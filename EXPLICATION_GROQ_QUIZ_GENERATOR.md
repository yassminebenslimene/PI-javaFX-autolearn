# Explication Complète — Génération de Quiz avec Groq IA
# Basé sur le vrai code du projet AutoLearn

---

## 🎯 Ce que fait cette fonctionnalité

L'admin clique sur un bouton → l'IA Groq lit le contenu d'un chapitre
→ génère automatiquement des questions QCM avec 4 options
→ les sauvegarde en base de données → le quiz est prêt à être utilisé.

```
Admin choisit un chapitre
        ↓
Groq lit le contenu du chapitre
        ↓
Groq génère N questions QCM en JSON
        ↓
Symfony crée Quiz + Questions + Options en BDD
        ↓
Admin révise et active le quiz
```

---

## 📁 Fichiers impliqués dans votre projet

```
src/Service/GrokQuizGeneratorService.php   ← Le cerveau — appelle l'API Groq
src/Controller/Backoffice/QuizController.php ← Les routes — reçoit les clics admin
config/services.yaml                        ← Injecte la clé API dans le service
.env / .env.local                           ← Stocke la clé API Groq
```

---

## 🔑 1. La clé API Groq — Comment elle circule

### Dans `.env.local` (déjà configuré dans votre projet)

```bash
GROQ_API_KEY=GROQ_API_KEY_REMOVED
```

### Dans `config/services.yaml` (déjà configuré)

```yaml
App\Service\GrokQuizGeneratorService:
    arguments:
        $grokApiKey: '%env(GROQ_API_KEY)%'
        #              ↑
        #   Symfony lit GROQ_API_KEY depuis .env.local
        #   et l'injecte dans le constructeur du service
```

### Dans `GrokQuizGeneratorService.php` — Le constructeur reçoit la clé

```php
public function __construct(
    private HttpClientInterface $httpClient,      // Client HTTP Symfony
    private EntityManagerInterface $entityManager, // Pour sauvegarder en BDD
    private LoggerInterface $logger,               // Pour les logs
    private string $grokApiKey                     // ← La clé API injectée ici
) {}
```

**Résumé du chemin de la clé :**
```
.env.local
  GROQ_API_KEY=gsk_xxx
        ↓
services.yaml
  $grokApiKey: '%env(GROQ_API_KEY)%'
        ↓
GrokQuizGeneratorService::__construct($grokApiKey)
        ↓
Utilisée dans les requêtes HTTP : 'Authorization' => 'Bearer ' . $this->grokApiKey
```

---

## 🌐 2. L'API Groq — Ce que c'est

Groq est une API compatible avec le format OpenAI.
Votre projet utilise le modèle **Llama 3.3 70B** (rapide et gratuit).

```php
// Dans GrokQuizGeneratorService.php
private const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
private const DEFAULT_MODEL = 'llama-3.3-70b-versatile';
private const MAX_QUESTIONS = 10;
```

**Pourquoi Groq et pas ChatGPT ?**
- Groq est **gratuit** (plan free généreux)
- Groq est **très rapide** (réponse en 1-2 secondes)
- Compatible avec le format OpenAI (même structure de requête)

---

## 🔄 3. Flux complet — Étape par étape

### ÉTAPE 1 — L'admin choisit un chapitre

**Route :** `GET /quiz/generate/select-chapitre`
**Contrôleur :** `QuizController::selectChapitre()`

```php
#[Route('/generate/select-chapitre', name: 'app_quiz_generate_select_chapitre')]
#[IsGranted('ROLE_ADMIN')]  // ← Seulement les admins
public function selectChapitre(ChapitreRepository $chapitreRepository): Response
{
    // Récupère tous les chapitres de la BDD
    $chapitres = $chapitreRepository->findAll();

    // Affiche la liste pour que l'admin choisisse
    return $this->render('backoffice/quiz/select_chapitre.html.twig', [
        'chapitres' => $chapitres,
    ]);
}
```

---

### ÉTAPE 2 — L'admin configure la génération

**Route :** `GET /quiz/generate/chapitre/{id}`
**Contrôleur :** `QuizController::generateFromChapitre()` (méthode GET)

L'admin voit un formulaire avec :
- Nombre de questions (1 à 10)
- Niveau de difficulté (facile / moyen / difficile)
- Titre du quiz
- Seuil de réussite (%)
- Durée maximale (minutes)

---

### ÉTAPE 3 — L'admin soumet le formulaire

**Route :** `POST /quiz/generate/chapitre/{id}`
**Contrôleur :** `QuizController::generateFromChapitre()` (méthode POST)

```php
if ($request->isMethod('POST')) {

    // Récupère les paramètres du formulaire
    $nombreQuestions = (int) $request->request->get('nombre_questions', 5);
    $difficulte      = $request->request->get('difficulte', 'moyen');

    $options = [
        'titre'          => $request->request->get('titre', 'Quiz - ' . $chapitre->getTitre()),
        'description'    => 'Quiz généré automatiquement par IA',
        'difficulte'     => $difficulte,
        'seuil_reussite' => (int) $request->request->get('seuil_reussite', 60),
        'max_tentatives' => null,
        'duree_max'      => null,
        'etat'           => 'brouillon',  // ← Toujours brouillon pour révision
    ];

    // ← Appelle le service IA
    $quiz = $grokService->genererQuizPourChapitre($chapitre, $nombreQuestions, $options);

    // Redirige vers l'édition du quiz créé
    return $this->redirectToRoute('app_quiz_edit', ['id' => $quiz->getId()]);
}
```

---

### ÉTAPE 4 — Le service crée le Quiz en mémoire

**Méthode :** `GrokQuizGeneratorService::genererQuizPourChapitre()`

```php
public function genererQuizPourChapitre(
    Chapitre $chapitre,
    int $nombreQuestions = 5,
    array $options = []
): Quiz {

    // ① Validation : entre 1 et 10 questions
    if ($nombreQuestions < 1 || $nombreQuestions > self::MAX_QUESTIONS) {
        throw new \InvalidArgumentException('Le nombre de questions doit être entre 1 et 10');
    }

    // ② Créer l'objet Quiz
    $quiz = new Quiz();
    $quiz->setTitre($options['titre'] ?? 'Quiz - ' . $chapitre->getTitre());
    $quiz->setDescription($options['description'] ?? 'Quiz généré automatiquement par IA');
    $quiz->setEtat('brouillon');          // ← Brouillon pour révision
    $quiz->setChapitre($chapitre);        // ← Lié au chapitre
    $quiz->setSeuilReussite(60);          // ← 60% pour réussir
    $quiz->setMaxTentatives(null);        // ← Illimité
    $quiz->setDureeMaxMinutes(null);      // ← Pas de limite de temps

    // ③ Appeler l'API Groq pour obtenir les questions
    $questionsData = $this->appellerApiGroq($chapitre, $nombreQuestions, $options);
    //                     ↑
    //         Retourne un tableau PHP comme :
    //         [
    //           ['texte' => 'Question 1?', 'points' => 10, 'options' => [...]],
    //           ['texte' => 'Question 2?', 'points' => 10, 'options' => [...]],
    //         ]

    // ④ Créer les entités Question et Option
    foreach ($questionsData as $questionData) {
        $question = new Question();
        $question->setTexteQuestion($questionData['texte']);
        $question->setPoint($questionData['points'] ?? 10);
        $question->setQuiz($quiz);

        foreach ($questionData['options'] as $optionData) {
            $option = new Option();
            $option->setTexteOption($optionData['texte']);
            $option->setEstCorrecte($optionData['correcte']); // true ou false
            $option->setQuestion($question);
            $question->addOption($option);
        }

        $quiz->addQuestion($question);
    }

    // ⑤ Sauvegarder en BDD
    $this->entityManager->persist($quiz);
    $this->entityManager->flush();

    return $quiz;
}
```

---

### ÉTAPE 5 — L'appel HTTP à l'API Groq

**Méthode :** `GrokQuizGeneratorService::appellerApiGroq()`

```php
private function appellerApiGroq(Chapitre $chapitre, int $nombreQuestions, array $options): array
{
    // ① Extraire le texte du chapitre (max 4000 caractères)
    $contenu = $this->extraireContenuChapitre($chapitre);

    // ② Construire le prompt (l'instruction pour l'IA)
    $prompt = $this->construirePrompt($contenu, $nombreQuestions, $options['difficulte'] ?? 'moyen');

    // ③ Envoyer la requête HTTP POST à Groq
    $response = $this->httpClient->request('POST', 'https://api.groq.com/openai/v1/chat/completions', [
        'headers' => [
            'Authorization' => 'Bearer ' . $this->grokApiKey,  // ← Clé API
            'Content-Type'  => 'application/json',
        ],
        'json' => [
            'model'       => 'llama-3.3-70b-versatile',  // ← Modèle IA
            'messages'    => [
                [
                    'role'    => 'system',
                    'content' => 'Tu es un expert pédagogique qui crée des quiz de qualité. Tu réponds uniquement en JSON valide.'
                ],
                [
                    'role'    => 'user',
                    'content' => $prompt  // ← Le prompt avec le contenu du chapitre
                ]
            ],
            'temperature'     => 0.7,                        // ← Créativité (0=rigide, 1=créatif)
            'max_tokens'      => 2000,                       // ← Longueur max de la réponse
            'response_format' => ['type' => 'json_object']  // ← Force le JSON
        ],
        'timeout'     => 60,    // ← 60 secondes max
        'max_retries' => 3,     // ← 3 tentatives si erreur
        'verify_peer' => false, // ← Désactive SSL (développement)
        'verify_host' => false,
    ]);

    // ④ Lire la réponse
    $data    = $response->toArray();
    $content = $data['choices'][0]['message']['content'];  // ← Le JSON généré par l'IA

    // ⑤ Décoder le JSON
    $questionsData = json_decode($content, true);

    // ⑥ Valider et retourner
    return $this->validerEtNormaliserQuestions($questionsData);
}
```

---

### ÉTAPE 6 — Le Prompt envoyé à l'IA

**Méthode :** `GrokQuizGeneratorService::construirePrompt()`

```php
private function construirePrompt(string $contenu, int $nombreQuestions, string $difficulte): string
{
    // Traduit le niveau de difficulté en texte
    $niveauDifficulte = match($difficulte) {
        'facile'    => 'faciles, adaptées aux débutants',
        'difficile' => 'difficiles, nécessitant une compréhension approfondie',
        default     => 'de difficulté moyenne'
    };

    return <<<PROMPT
Crée exactement {$nombreQuestions} questions à choix multiples (QCM) basées sur le contenu suivant.
Les questions doivent être {$niveauDifficulte}.

CONTENU DU CHAPITRE:
{$contenu}

INSTRUCTIONS:
1. Crée exactement {$nombreQuestions} questions pertinentes
2. Chaque question doit avoir exactement 4 options de réponse
3. Une seule option doit être correcte par question
4. Les questions doivent évaluer la compréhension du contenu
5. Utilise un langage clair et professionnel en français
6. Attribue 10 points par question

RÉPONDS UNIQUEMENT avec un objet JSON dans ce format exact:
{
  "questions": [
    {
      "texte": "Quelle est la question?",
      "points": 10,
      "options": [
        {"texte": "Option A", "correcte": false},
        {"texte": "Option B", "correcte": true},
        {"texte": "Option C", "correcte": false},
        {"texte": "Option D", "correcte": false}
      ]
    }
  ]
}
PROMPT;
}
```

---

### ÉTAPE 7 — Ce que Groq répond (exemple réel)

Groq reçoit le prompt et répond avec du JSON :

```json
{
  "questions": [
    {
      "texte": "Qu'est-ce qu'une variable en Java ?",
      "points": 10,
      "options": [
        {"texte": "Un espace mémoire pour stocker une valeur", "correcte": true},
        {"texte": "Une fonction qui retourne une valeur", "correcte": false},
        {"texte": "Un type de boucle", "correcte": false},
        {"texte": "Une classe abstraite", "correcte": false}
      ]
    },
    {
      "texte": "Quel mot-clé déclare une variable entière en Java ?",
      "points": 10,
      "options": [
        {"texte": "string", "correcte": false},
        {"texte": "int", "correcte": true},
        {"texte": "var", "correcte": false},
        {"texte": "number", "correcte": false}
      ]
    }
  ]
}
```

---

### ÉTAPE 8 — Validation du JSON reçu

**Méthode :** `GrokQuizGeneratorService::validerEtNormaliserQuestions()`

```php
private function validerEtNormaliserQuestions(array $data): array
{
    // Vérifier que la clé 'questions' existe
    if (!isset($data['questions']) || !is_array($data['questions'])) {
        throw new \RuntimeException('Format invalide: clé "questions" manquante');
    }

    $questions = [];

    foreach ($data['questions'] as $index => $questionData) {

        // Vérifier le texte de la question
        if (!isset($questionData['texte']) || empty(trim($questionData['texte']))) {
            throw new \RuntimeException("Question #{$index}: texte manquant");
        }

        // Vérifier les options
        if (!isset($questionData['options']) || count($questionData['options']) < 2) {
            throw new \RuntimeException("Question #{$index}: au moins 2 options requises");
        }

        $hasCorrectAnswer = false;
        $options = [];

        foreach ($questionData['options'] as $optionIndex => $optionData) {
            if (!isset($optionData['texte']) || empty(trim($optionData['texte']))) {
                throw new \RuntimeException("Question #{$index}, Option #{$optionIndex}: texte manquant");
            }

            $isCorrect = $optionData['correcte'] ?? false;
            if ($isCorrect) $hasCorrectAnswer = true;

            $options[] = [
                'texte'   => trim($optionData['texte']),
                'correcte' => (bool) $isCorrect
            ];
        }

        // Vérifier qu'il y a au moins une bonne réponse
        if (!$hasCorrectAnswer) {
            throw new \RuntimeException("Question #{$index}: aucune réponse correcte");
        }

        $questions[] = [
            'texte'   => trim($questionData['texte']),
            'points'  => $questionData['points'] ?? 10,
            'options' => $options
        ];
    }

    if (empty($questions)) {
        throw new \RuntimeException('Aucune question valide générée');
    }

    return $questions;
}
```

---

### ÉTAPE 9 — Résultat en base de données

Après `$this->entityManager->flush()`, voici ce qui est créé en BDD :

```
TABLE quiz
  id=42, titre="Quiz Java Variables", etat="brouillon",
  chapitre_id=5, seuil_reussite=60

TABLE question
  id=101, texte_question="Qu'est-ce qu'une variable en Java ?", point=10, quiz_id=42
  id=102, texte_question="Quel mot-clé déclare une variable entière ?", point=10, quiz_id=42

TABLE option
  id=201, texte_option="Un espace mémoire...", est_correcte=1, question_id=101
  id=202, texte_option="Une fonction...",      est_correcte=0, question_id=101
  id=203, texte_option="Un type de boucle",    est_correcte=0, question_id=101
  id=204, texte_option="Une classe abstraite", est_correcte=0, question_id=101
  id=205, texte_option="string",               est_correcte=0, question_id=102
  id=206, texte_option="int",                  est_correcte=1, question_id=102
  ...
```

---

## 🔁 Régénération des questions

Si l'admin n'est pas satisfait, il peut régénérer :

**Route :** `POST /quiz/{id}/regenerate`

```php
public function regenerate(Request $request, Quiz $quiz, GrokQuizGeneratorService $grokService): Response
{
    // ① Supprimer toutes les questions existantes
    foreach ($quiz->getQuestions() as $question) {
        $quiz->removeQuestion($question);
        $this->entityManager->remove($question);
    }
    $this->entityManager->flush();

    // ② Générer de nouvelles questions via Groq
    $questionsData = $this->appellerApiGroq($chapitre, $nombreQuestions, $options);

    // ③ Créer les nouvelles entités
    foreach ($questionsData as $questionData) {
        // ... même logique que genererQuizPourChapitre()
    }

    $this->entityManager->flush();
    return $quiz;
}
```

---

## 🗺️ Schéma visuel complet

```
ADMIN
  │
  │ GET /quiz/generate/select-chapitre
  ▼
[Liste des chapitres]
  │
  │ Clique sur "Chapitre Java - Variables"
  ▼
[Formulaire de génération]
  - Nombre de questions : 5
  - Difficulté : moyen
  - Seuil de réussite : 60%
  │
  │ POST /quiz/generate/chapitre/5
  ▼
QuizController::generateFromChapitre()
  │
  │ Appelle grokService->genererQuizPourChapitre()
  ▼
GrokQuizGeneratorService
  │
  ├─ extraireContenuChapitre()
  │    → strip_tags(chapitre->getContenu())
  │    → Tronque à 4000 caractères
  │
  ├─ construirePrompt()
  │    → "Crée 5 questions QCM basées sur ce contenu..."
  │    → Inclut le contenu du chapitre
  │    → Demande le format JSON exact
  │
  ├─ HTTP POST → https://api.groq.com/openai/v1/chat/completions
  │    Headers: Authorization: Bearer gsk_xxx
  │    Body: { model: "llama-3.3-70b-versatile", messages: [...] }
  │
  ├─ Groq répond avec JSON
  │    { "questions": [ {...}, {...}, {...}, {...}, {...} ] }
  │
  ├─ validerEtNormaliserQuestions()
  │    → Vérifie structure JSON
  │    → Vérifie au moins 1 bonne réponse par question
  │
  └─ Crée Quiz + Questions + Options en BDD
       entityManager->persist(quiz)
       entityManager->flush()
  │
  │ Redirige vers /quiz/42/edit
  ▼
[Admin révise le quiz en brouillon]
  │
  │ Change l'état de "brouillon" à "actif"
  ▼
[Quiz disponible pour les étudiants]
```

---

## ⚙️ Configuration complète dans votre projet

### Variables d'environnement (`.env.local`)

```bash
# Déjà configuré dans votre projet
GROQ_API_KEY=GROQ_API_KEY_REMOVED
```

### `config/services.yaml` (déjà configuré)

```yaml
App\Service\GrokQuizGeneratorService:
    arguments:
        $grokApiKey: '%env(GROQ_API_KEY)%'
```

### Modèle utilisé

```php
private const DEFAULT_MODEL = 'llama-3.3-70b-versatile';
```

Ce modèle est :
- **Gratuit** sur Groq (plan free)
- **Rapide** : réponse en 1-3 secondes
- **Performant** : 70 milliards de paramètres

---

## 🐛 Erreurs possibles et solutions

### "Impossible de se connecter à l'API Groq"
```
Cause  : Pas de connexion internet ou Groq est down
Solution : Vérifier la connexion, réessayer plus tard
```

### "L'API Groq a retourné une erreur (Code: 401)"
```
Cause  : Clé API invalide ou expirée
Solution : Aller sur https://console.groq.com/keys
           Créer une nouvelle clé
           Mettre à jour GROQ_API_KEY dans .env.local
```

### "L'API Groq a retourné une erreur (Code: 429)"
```
Cause  : Trop de requêtes (rate limit dépassé)
Solution : Attendre quelques secondes et réessayer
           Le plan free a une limite de requêtes par minute
```

### "Format de réponse invalide: clé questions manquante"
```
Cause  : L'IA n'a pas respecté le format JSON demandé
Solution : Réessayer (l'IA peut parfois dévier du format)
           Augmenter max_tokens si le JSON est tronqué
```

### "Aucune question valide générée"
```
Cause  : Le contenu du chapitre est vide ou trop court
Solution : Vérifier que le chapitre a du contenu
           Chapitre->getContenu() ne doit pas être vide
```

---

## 📊 Résumé des méthodes du service

| Méthode | Rôle | Appelée par |
|---------|------|-------------|
| `genererQuizPourChapitre()` | Crée un quiz complet | `QuizController::generateFromChapitre()` |
| `regenererQuestions()` | Remplace les questions | `QuizController::regenerate()` |
| `appellerApiGroq()` | Fait la requête HTTP | `genererQuizPourChapitre()` et `regenererQuestions()` |
| `extraireContenuChapitre()` | Nettoie le texte du chapitre | `appellerApiGroq()` |
| `construirePrompt()` | Construit l'instruction pour l'IA | `appellerApiGroq()` |
| `validerEtNormaliserQuestions()` | Vérifie le JSON reçu | `appellerApiGroq()` |

---

## 🔗 Obtenir une clé API Groq

```
1. Aller sur https://console.groq.com
2. Créer un compte (gratuit)
3. Menu → "API Keys"
4. Cliquer "Create API Key"
5. Copier la clé (commence par gsk_)
6. Mettre dans .env.local :
   GROQ_API_KEY=gsk_ta_cle_ici
```
