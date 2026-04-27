# Explication Complète — Correction de Quiz par IA
# Basé sur le vrai code du projet AutoLearn

---

## 🎯 Ce que fait la correction IA

Quand un étudiant soumet un quiz, l'IA Groq analyse **chaque réponse**
et génère deux choses :

```
1. Une explication personnalisée pour CHAQUE question
   → Pourquoi c'est correct ou incorrect
   → Un conseil pratique
   → Des ressources pour aller plus loin

2. Un résumé pédagogique GLOBAL
   → Message général adapté au score
   → Points forts identifiés
   → Points à améliorer
   → Conseils de révision
   → Message d'encouragement final
```

---

## 📁 Fichiers impliqués

```
src/Service/QuizCorrectorAIService.php        ← Le cerveau de la correction IA
src/Controller/FrontOffice/QuizPassageController.php ← Appelle le service après soumission
templates/frontoffice/quiz/result_with_ai.html.twig  ← Affiche les explications
config/services.yaml                          ← Injecte la clé API
.env.local                                    ← GROQ_API_KEY
```

---

## 🔄 Flux complet — De la soumission à l'affichage

```
Étudiant clique "Soumettre le Quiz"
        │
        ▼
QuizPassageController::submit()
  ① Récupère les réponses du formulaire
  ② calculateScore() → calcule le score
  ③ Détermine VALIDÉ ou ÉCHEC
  ④ markChapterAsCompleted() si VALIDÉ
  ⑤ session->remove(tentativeKey)
        │
        ▼
correctorAI->genererExplicationsPersonnalisees($result['details'])
  → Pour chaque question → appel API Groq → explication JSON
        │
        ▼
correctorAI->genererResumePedagogique($result['details'], $percentage)
  → 1 seul appel API Groq → résumé global JSON
        │
        ▼
render('result_with_ai.html.twig', [
    'explications'     => $explications,
    'resumePedagogique'=> $resumePedagogique,
    ...
])
        │
        ▼
Template affiche les explications sous chaque question
```

---

## 🧠 1. Le Service QuizCorrectorAIService

### Modèle IA utilisé

```php
private const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
private const DEFAULT_MODEL = 'meta-llama/llama-4-scout-17b-16e-instruct';
//                             ↑ Llama 4 Scout — optimisé pour les explications pédagogiques
```

### Constructeur

```php
public function __construct(
    private HttpClientInterface $httpClient,  // Client HTTP Symfony
    private LoggerInterface $logger,           // Logs
    private string $grokApiKey                 // Clé API depuis .env.local
) {}
```

---

## 📝 2. Méthode principale — `genererExplicationsPersonnalisees()`

C'est la méthode appelée depuis le contrôleur.
Elle reçoit le tableau `$result['details']` qui contient pour chaque question :

```php
// Format de $resultDetails reçu depuis calculateScore()
$resultDetails = [
    10 => [                              // questionId = 10
        'question'       => $question,   // Objet Question
        'selectedOption' => 101,         // ID de l'option choisie par l'étudiant
        'correctOption'  => 101,         // ID de la bonne réponse
        'isCorrect'      => true,        // true ou false
        'points'         => 10           // Points gagnés
    ],
    11 => [
        'question'       => $question2,
        'selectedOption' => 107,         // L'étudiant a choisi 107
        'correctOption'  => 106,         // Mais la bonne réponse était 106
        'isCorrect'      => false,
        'points'         => 0
    ],
    // ...
];
```

### Code de la méthode

```php
public function genererExplicationsPersonnalisees(array $resultDetails): array
{
    $explications = [];

    // Boucle sur chaque question
    foreach ($resultDetails as $questionId => $detail) {
        try {
            // Génère une explication pour cette question
            $explication = $this->genererExplicationPourQuestion(
                $detail['question'],       // Objet Question
                $detail['selectedOption'], // Ce que l'étudiant a choisi
                $detail['isCorrect']       // Correct ou non
            );

            $explications[$questionId] = $explication;

        } catch (\Exception $e) {
            // Si l'API échoue → explication par défaut (sans IA)
            $this->logger->error('Erreur explication', ['question_id' => $questionId]);
            $explications[$questionId] = $this->genererExplicationParDefaut($detail);
        }
    }

    return $explications;
    // Retourne : [10 => [...], 11 => [...], 12 => [...]]
}
```

---

## 🔍 3. Méthode privée — `genererExplicationPourQuestion()`

Pour chaque question, cette méthode :
1. Trouve l'option sélectionnée et la bonne réponse
2. Construit un prompt adapté (correct ou incorrect)
3. Appelle l'API Groq
4. Retourne un tableau structuré

```php
private function genererExplicationPourQuestion(
    Question $question,
    ?int $selectedOptionId,
    bool $isCorrect
): array {

    // ① Trouver l'option sélectionnée et la bonne réponse
    $selectedOption = null;
    $correctOption  = null;

    foreach ($question->getOptions() as $option) {
        if ($option->getId() === $selectedOptionId) {
            $selectedOption = $option;  // Ce que l'étudiant a choisi
        }
        if ($option->isEstCorrecte()) {
            $correctOption = $option;   // La vraie bonne réponse
        }
    }

    // ② Construire le prompt adapté
    $prompt = $this->construirePromptExplication(
        $question, $selectedOption, $correctOption, $isCorrect
    );

    // ③ Appel HTTP à l'API Groq
    $response = $this->httpClient->request('POST', self::GROQ_API_URL, [
        'headers' => [
            'Authorization' => 'Bearer ' . $this->grokApiKey,
            'Content-Type'  => 'application/json',
        ],
        'json' => [
            'model'    => self::DEFAULT_MODEL,
            'messages' => [
                [
                    'role'    => 'system',
                    'content' => 'Tu es un professeur bienveillant et pédagogue qui aide
                                  les étudiants à comprendre leurs erreurs. Tu expliques
                                  de manière claire, encourageante et constructive.
                                  Tu réponds en JSON valide.'
                ],
                [
                    'role'    => 'user',
                    'content' => $prompt
                ]
            ],
            'temperature'     => 0.7,
            'max_tokens'      => 500,
            'response_format' => ['type' => 'json_object']
        ],
        'timeout' => 15  // 15 secondes max par question
    ]);

    // ④ Lire et décoder la réponse JSON
    $data       = $response->toArray();
    $content    = $data['choices'][0]['message']['content'] ?? '{}';
    $explication = json_decode($content, true);

    // ⑤ Retourner un tableau structuré
    return [
        'message'           => $explication['message']           ?? 'Explication non disponible',
        'conseil'           => $explication['conseil']           ?? '',
        'pourquoi_incorrect'=> $explication['pourquoi_incorrect'] ?? '',
        'pourquoi_correct'  => $explication['pourquoi_correct']  ?? '',
        'ressources'        => $explication['ressources']        ?? [],
        'tone'              => $isCorrect ? 'success' : 'error'
    ];
}
```

---

## 📋 4. Les deux Prompts envoyés à l'IA

### Prompt pour une BONNE réponse ✅

```
L'étudiant a CORRECTEMENT répondu à cette question:

QUESTION: Qu'est-ce qu'une variable en Java ?
RÉPONSE DE L'ÉTUDIANT: Un espace mémoire pour stocker une valeur
BONNE RÉPONSE: Un espace mémoire pour stocker une valeur

Génère un message d'encouragement et une explication pédagogique qui:
1. Félicite l'étudiant pour sa bonne réponse
2. Explique POURQUOI cette réponse est correcte
3. Approfondit le concept pour renforcer la compréhension
4. Suggère des ressources pour aller plus loin (optionnel)

RÉPONDS en JSON avec ce format:
{
  "message": "Message d'encouragement positif et bref",
  "pourquoi_correct": "Explication détaillée de pourquoi c'est correct",
  "conseil": "Conseil pour approfondir ou conseil d'application pratique",
  "ressources": ["Suggestion 1", "Suggestion 2"]
}
```

### Prompt pour une MAUVAISE réponse ❌

```
L'étudiant a INCORRECTEMENT répondu à cette question:

QUESTION: Quel mot-clé déclare une variable entière en Java ?
RÉPONSE DE L'ÉTUDIANT: string
BONNE RÉPONSE: int

Génère une explication pédagogique bienveillante qui:
1. Explique POURQUOI la réponse de l'étudiant est incorrecte (sans être négatif)
2. Explique POURQUOI la bonne réponse est correcte
3. Donne un conseil pratique pour éviter cette erreur à l'avenir
4. Encourage l'étudiant à continuer d'apprendre

Ton: Bienveillant, constructif, pédagogique, encourageant

RÉPONDS en JSON avec ce format:
{
  "message": "Message d'encouragement bref et positif",
  "pourquoi_incorrect": "Explication claire de l'erreur commise",
  "pourquoi_correct": "Explication de la bonne réponse",
  "conseil": "Conseil pratique pour progresser",
  "ressources": ["Suggestion de révision 1", "Suggestion 2"]
}
```

### Ce que Groq répond (exemple réel)

**Pour une bonne réponse :**
```json
{
  "message": "🎉 Excellent ! Vous avez parfaitement compris ce concept !",
  "pourquoi_correct": "Une variable est effectivement un espace mémoire nommé qui permet de stocker et manipuler des données. En Java, chaque variable a un type qui détermine la nature des données qu'elle peut contenir.",
  "conseil": "Pour approfondir, explorez les différents types de variables en Java : primitifs (int, double, boolean) et objets (String, ArrayList).",
  "ressources": ["Documentation Oracle Java - Variables", "Tutoriel Java sur les types de données"]
}
```

**Pour une mauvaise réponse :**
```json
{
  "message": "Ne vous découragez pas, cette confusion est très courante !",
  "pourquoi_incorrect": "Vous avez confondu 'string' (type de texte en JavaScript/C#) avec le type entier Java. En Java, 'string' n'est pas un mot-clé valide pour les entiers.",
  "pourquoi_correct": "'int' est le mot-clé Java pour déclarer un entier (nombre sans décimale). Exemple : int age = 25;",
  "conseil": "Mémorisez les 8 types primitifs Java : byte, short, int, long, float, double, boolean, char.",
  "ressources": ["Tableau des types primitifs Java", "Exercices sur les déclarations de variables"]
}
```

---

## 📊 5. Méthode — `genererResumePedagogique()`

Génère **un seul appel API** pour le bilan global.

```php
public function genererResumePedagogique(array $resultDetails, float $percentage): array
{
    // Calcule les statistiques
    $nombreQuestions = count($resultDetails);
    $nombreCorrect   = array_reduce(
        $resultDetails,
        fn($carry, $detail) => $carry + ($detail['isCorrect'] ? 1 : 0),
        0
    );
    $nombreIncorrect = $nombreQuestions - $nombreCorrect;

    // Filtre les questions incorrectes pour les inclure dans le prompt
    $questionsIncorrectes = array_filter(
        $resultDetails,
        fn($detail) => !$detail['isCorrect']
    );

    // Construit le prompt du résumé
    $prompt = $this->construirePromptResume(
        $nombreQuestions, $nombreCorrect, $nombreIncorrect,
        $percentage, $questionsIncorrectes
    );

    // Appel API Groq (même structure que pour les explications)
    $response = $this->httpClient->request('POST', self::GROQ_API_URL, [
        'headers' => ['Authorization' => 'Bearer ' . $this->grokApiKey, ...],
        'json' => [
            'model'       => self::DEFAULT_MODEL,
            'messages'    => [
                ['role' => 'system', 'content' => 'Tu es un professeur qui fait un bilan pédagogique personnalisé...'],
                ['role' => 'user',   'content' => $prompt]
            ],
            'temperature'     => 0.7,
            'max_tokens'      => 400,
            'response_format' => ['type' => 'json_object']
        ],
        'timeout' => 15
    ]);

    $resume = json_decode($data['choices'][0]['message']['content'], true);

    return [
        'message_general'     => $resume['message_general']     ?? 'Bon travail !',
        'points_forts'        => $resume['points_forts']        ?? [],
        'points_amelioration' => $resume['points_amelioration'] ?? [],
        'conseils_revision'   => $resume['conseils_revision']   ?? [],
        'encouragement'       => $resume['encouragement']       ?? 'Continuez vos efforts !'
    ];
}
```

### Prompt du résumé global

```
Génère un bilan pédagogique personnalisé pour un étudiant qui vient de terminer un quiz:

STATISTIQUES:
- Total de questions: 5
- Réponses correctes: 3
- Réponses incorrectes: 2
- Score: 60%

QUESTIONS MANQUÉES:
- Quel mot-clé déclare une variable entière en Java ?
- Quelle est la différence entre == et .equals() en Java ?

Génère un bilan qui:
1. Donne un message général adapté au score (encourageant même si faible)
2. Identifie les points forts (ce qui a été réussi)
3. Identifie les points à améliorer (sans être négatif)
4. Donne 2-3 conseils concrets de révision
5. Termine par un encouragement motivant

RÉPONDS en JSON avec ce format:
{
  "message_general": "Message d'ouverture adapté au score",
  "points_forts": ["Point fort 1", "Point fort 2"],
  "points_amelioration": ["Domaine à revoir 1", "Domaine 2"],
  "conseils_revision": ["Conseil pratique 1", "Conseil 2", "Conseil 3"],
  "encouragement": "Message final motivant"
}
```

### Exemple de réponse Groq

```json
{
  "message_general": "Bon travail ! Vous avez obtenu 60%, ce qui montre de bonnes bases en Java.",
  "points_forts": [
    "Bonne compréhension des variables et de leur rôle",
    "Maîtrise des concepts de base de la POO"
  ],
  "points_amelioration": [
    "Les types primitifs Java (int, String, double...)",
    "La comparaison d'objets avec == vs .equals()"
  ],
  "conseils_revision": [
    "Révisez le tableau des 8 types primitifs Java",
    "Pratiquez la comparaison de chaînes avec .equals()",
    "Faites des exercices sur les déclarations de variables"
  ],
  "encouragement": "Vous êtes sur la bonne voie ! Avec un peu de révision, vous maîtriserez ces concepts rapidement. 💪"
}
```

---

## 🛡️ 6. Système de secours — `genererExplicationParDefaut()`

Si l'API Groq échoue (timeout, erreur réseau, rate limit),
le service génère une explication **sans IA** :

```php
private function genererExplicationParDefaut(array $detail): array
{
    $isCorrect = $detail['isCorrect'];

    // Trouver la bonne réponse dans les options
    $correctOption = null;
    foreach ($detail['question']->getOptions() as $option) {
        if ($option->isEstCorrecte()) {
            $correctOption = $option;
            break;
        }
    }

    if ($isCorrect) {
        return [
            'message'          => '✅ Excellente réponse !',
            'pourquoi_correct' => 'Votre réponse est correcte. Continuez ainsi !',
            'conseil'          => 'Continuez à approfondir vos connaissances sur ce sujet.',
            'ressources'       => [],
            'tone'             => 'success'
        ];
    } else {
        return [
            'message'            => '❌ Ce n\'est pas la bonne réponse',
            'pourquoi_incorrect' => 'Votre réponse n\'est pas correcte.',
            'pourquoi_correct'   => 'La bonne réponse est : ' . $correctOption?->getTexteOption(),
            'conseil'            => 'Révisez ce concept et réessayez.',
            'ressources'         => ['Relisez le chapitre correspondant'],
            'tone'               => 'error'
        ];
    }
}
```

---

## 🎮 7. Dans le Contrôleur — Comment c'est appelé

```php
// src/Controller/FrontOffice/QuizPassageController.php

public function submit(Quiz $quiz, Request $request, SessionInterface $session): Response
{
    // ... calcul du score ...

    // ① Appel 1 : Explications par question
    $explications = [];
    $resumePedagogique = [];

    try {
        // Génère N explications (une par question)
        // N appels API Groq en séquence
        $explications = $this->correctorAI->genererExplicationsPersonnalisees(
            $result['details']
        );

        // Génère 1 résumé global
        // 1 appel API Groq
        $resumePedagogique = $this->correctorAI->genererResumePedagogique(
            $result['details'],
            $result['percentage']
        );

    } catch (\Exception $e) {
        // Si tout échoue → continuer sans IA
        $this->addFlash('warning', 'Les explications IA ne sont pas disponibles pour le moment.');
    }

    // ② Passer au template
    return $this->render('frontoffice/quiz/result_with_ai.html.twig', [
        'quiz'             => $quiz,
        'result'           => $result,
        'statut'           => $statut,
        'explications'     => $explications,      // ← Explications par question
        'resumePedagogique'=> $resumePedagogique, // ← Résumé global
        'statistiques'     => $statistiques,
        // ...
    ]);
}
```

---

## 🖥️ 8. Dans le Template — Comment c'est affiché

### Résumé pédagogique global (en haut de la page)

```twig
{% if resumePedagogique is defined and resumePedagogique %}
<div class="ai-summary-card">
    <div class="ai-summary-header">
        <i class="fa fa-robot"></i>
        <h3>Analyse pédagogique par IA</h3>
    </div>

    {# Message général #}
    <p class="summary-message">{{ resumePedagogique.message_general }}</p>

    {# Points forts en vert #}
    {% if resumePedagogique.points_forts %}
        <h4>✅ Points forts</h4>
        <ul class="summary-list success">
            {% for point in resumePedagogique.points_forts %}
                <li>{{ point }}</li>
            {% endfor %}
        </ul>
    {% endif %}

    {# Points à améliorer en orange #}
    {% if resumePedagogique.points_amelioration %}
        <h4>📈 Points à améliorer</h4>
        <ul class="summary-list warning">
            {% for point in resumePedagogique.points_amelioration %}
                <li>{{ point }}</li>
            {% endfor %}
        </ul>
    {% endif %}

    {# Conseils de révision en bleu #}
    {% if resumePedagogique.conseils_revision %}
        <h4>💡 Conseils de révision</h4>
        <ul class="summary-list info">
            {% for conseil in resumePedagogique.conseils_revision %}
                <li>{{ conseil }}</li>
            {% endfor %}
        </ul>
    {% endif %}

    {# Encouragement final en violet #}
    <div class="summary-encouragement">
        <i class="fa fa-heart"></i>
        {{ resumePedagogique.encouragement }}
    </div>
</div>
{% endif %}
```

### Explication IA sous chaque question

```twig
{% for questionId, detail in result.details %}
    <div class="result-card {% if detail.isCorrect %}correct{% else %}incorrect{% endif %}">

        {# ... affichage de la question et des options ... #}

        {# Explication IA pour cette question #}
        {% if explications is defined and explications[questionId] is defined %}
            {% set explication = explications[questionId] %}

            <div class="ai-explanation">
                <div class="ai-explanation-header">
                    <i class="fa fa-robot"></i>
                    <span>Explication de votre professeur IA</span>
                </div>

                {# Message principal #}
                <p class="ai-message">{{ explication.message }}</p>

                {# Pourquoi c'est incorrect (rouge) #}
                {% if explication.pourquoi_incorrect %}
                    <div class="ai-detail error">
                        <strong>❌ Pourquoi c'est incorrect :</strong>
                        <p>{{ explication.pourquoi_incorrect }}</p>
                    </div>
                {% endif %}

                {# Explication de la bonne réponse (vert) #}
                {% if explication.pourquoi_correct %}
                    <div class="ai-detail success">
                        <strong>✅ Explication :</strong>
                        <p>{{ explication.pourquoi_correct }}</p>
                    </div>
                {% endif %}

                {# Conseil pratique (bleu) #}
                {% if explication.conseil %}
                    <div class="ai-detail info">
                        <strong>💡 Conseil :</strong>
                        <p>{{ explication.conseil }}</p>
                    </div>
                {% endif %}

                {# Ressources (orange) #}
                {% if explication.ressources %}
                    <div class="ai-resources">
                        <strong>📚 Pour aller plus loin :</strong>
                        <ul>
                            {% for ressource in explication.ressources %}
                                <li>{{ ressource }}</li>
                            {% endfor %}
                        </ul>
                    </div>
                {% endif %}
            </div>
        {% endif %}
    </div>
{% endfor %}
```

---

## 📊 9. Résumé — Nombre d'appels API par soumission

```
Quiz avec 5 questions :
  → 5 appels API pour les explications (1 par question)
  → 1 appel API pour le résumé global
  ─────────────────────────────────────
  → 6 appels API Groq au total

Quiz avec 10 questions :
  → 10 appels API pour les explications
  → 1 appel API pour le résumé global
  ─────────────────────────────────────
  → 11 appels API Groq au total

Timeout par appel : 15 secondes
Temps total max   : 11 × 15s = 165s (mais en pratique 5-15s total)
```

---

## 🗺️ Schéma visuel complet

```
ÉTUDIANT soumet le quiz
        │
        ▼
QuizPassageController::submit()
        │
        ├─ calculateScore() → result['details']
        │
        ├─ correctorAI->genererExplicationsPersonnalisees(details)
        │       │
        │       ├─ Question 1 (correct) → prompt "féliciter" → Groq → JSON
        │       ├─ Question 2 (incorrect) → prompt "expliquer erreur" → Groq → JSON
        │       ├─ Question 3 (incorrect) → prompt "expliquer erreur" → Groq → JSON
        │       ├─ Question 4 (correct) → prompt "féliciter" → Groq → JSON
        │       └─ Question 5 (correct) → prompt "féliciter" → Groq → JSON
        │
        ├─ correctorAI->genererResumePedagogique(details, 60%)
        │       │
        │       └─ 1 prompt avec stats globales → Groq → JSON résumé
        │
        └─ render('result_with_ai.html.twig', [
               'explications'      → affiché sous chaque question
               'resumePedagogique' → affiché en haut de la page
           ])
```

---

## 📋 Tableau des méthodes du service

| Méthode | Publique/Privée | Rôle | Appels API |
|---------|-----------------|------|------------|
| `genererExplicationsPersonnalisees()` | Publique | Boucle sur toutes les questions | N appels |
| `genererExplicationPourQuestion()` | Privée | Explication pour 1 question | 1 appel |
| `construirePromptExplication()` | Privée | Construit le prompt (correct/incorrect) | 0 |
| `genererExplicationParDefaut()` | Privée | Secours si API échoue | 0 |
| `genererResumePedagogique()` | Publique | Bilan global | 1 appel |
| `construirePromptResume()` | Privée | Construit le prompt du résumé | 0 |
| `genererResumeParDefaut()` | Privée | Secours si API échoue | 0 |
