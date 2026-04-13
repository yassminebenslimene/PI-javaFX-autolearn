# Calcul de Score du Quiz — Explication Détaillée

## Le code original Symfony (QuizManagementService.php)

```php
public function calculateScore(Quiz $quiz, array $reponses): array
{
    $score = 0;
    $totalPoints = 0;
    $details = [];

    foreach ($quiz->getQuestions() as $question) {
        $totalPoints += $question->getPoint();
        $questionId = $question->getId();
        $isCorrect = false;
        $correctOptionId = null;

        foreach ($question->getOptions() as $option) {
            if ($option->isEstCorrecte()) {
                $correctOptionId = $option->getId();

                if (isset($reponses[$questionId]) && $reponses[$questionId] == $option->getId()) {
                    $isCorrect = true;
                    $score += $question->getPoint();
                }
                break;
            }
        }

        $details[$questionId] = [
            'question'       => $question,
            'selectedOption' => $reponses[$questionId] ?? null,
            'correctOption'  => $correctOptionId,
            'isCorrect'      => $isCorrect,
            'points'         => $isCorrect ? $question->getPoint() : 0
        ];
    }

    $percentage = $totalPoints > 0 ? ($score / $totalPoints) * 100 : 0;

    return [
        'score'       => $score,
        'totalPoints' => $totalPoints,
        'percentage'  => round($percentage, 2),
        'details'     => $details
    ];
}
```

---

## Explication étape par étape

### Données d'entrée

```
Quiz "Loops and Iterations" contient 5 questions :

Question 1 (id=10) → 10 points
  Option A (id=101) estCorrecte=true  ← bonne réponse
  Option B (id=102) estCorrecte=false
  Option C (id=103) estCorrecte=false
  Option D (id=104) estCorrecte=false

Question 2 (id=11) → 10 points
  Option A (id=105) estCorrecte=false
  Option B (id=106) estCorrecte=true  ← bonne réponse
  Option C (id=107) estCorrecte=false
  Option D (id=108) estCorrecte=false

Question 3 (id=12) → 10 points
  ...

Question 4 (id=13) → 10 points
  ...

Question 5 (id=14) → 10 points
  ...

Réponses de l'étudiant (POST du formulaire) :
answers = {
  10 → 101,   ← Q1 : a choisi option 101 (CORRECT ✅)
  11 → 107,   ← Q2 : a choisi option 107 (FAUX ❌)
  12 → null,  ← Q3 : n'a pas répondu     (FAUX ❌)
  13 → ...,   ← Q4 : ...
  14 → ...    ← Q5 : ...
}
```

---

### Étape 1 — Initialisation des compteurs

```
score       = 0    ← points gagnés par l'étudiant
totalPoints = 0    ← total possible du quiz
details     = {}   ← détail question par question
```

---

### Étape 2 — Boucle sur chaque question

Pour chaque question du quiz :

```
┌─────────────────────────────────────────────────────────┐
│  QUESTION 1 (id=10, point=10)                           │
│                                                         │
│  totalPoints += 10  →  totalPoints = 10                 │
│                                                         │
│  Chercher la bonne réponse dans les options :           │
│    Option 101 → estCorrecte = TRUE  ← trouvée !         │
│    correctOptionId = 101                                │
│                                                         │
│  L'étudiant a répondu quoi ?                            │
│    reponses[10] = 101                                   │
│                                                         │
│  Comparer : 101 == 101 → OUI ✅                         │
│    isCorrect = true                                     │
│    score += 10  →  score = 10                           │
│                                                         │
│  Stocker dans details[10] :                             │
│    selectedOption = 101                                 │
│    correctOption  = 101                                 │
│    isCorrect      = true                                │
│    points         = 10                                  │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  QUESTION 2 (id=11, point=10)                           │
│                                                         │
│  totalPoints += 10  →  totalPoints = 20                 │
│                                                         │
│  Chercher la bonne réponse :                            │
│    Option 105 → estCorrecte = false                     │
│    Option 106 → estCorrecte = TRUE  ← trouvée !         │
│    correctOptionId = 106                                │
│                                                         │
│  L'étudiant a répondu quoi ?                            │
│    reponses[11] = 107                                   │
│                                                         │
│  Comparer : 107 == 106 → NON ❌                         │
│    isCorrect = false                                    │
│    score reste à 10 (pas de points ajoutés)             │
│                                                         │
│  Stocker dans details[11] :                             │
│    selectedOption = 107  ← ce qu'il a choisi            │
│    correctOption  = 106  ← la vraie bonne réponse       │
│    isCorrect      = false                               │
│    points         = 0                                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  QUESTION 3 (id=12, point=10)                           │
│                                                         │
│  totalPoints += 10  →  totalPoints = 30                 │
│                                                         │
│  L'étudiant a répondu quoi ?                            │
│    reponses[12] = null  ← pas de réponse !              │
│                                                         │
│  isset(reponses[12]) → FALSE                            │
│    isCorrect = false                                    │
│    score reste à 10                                     │
│                                                         │
│  Stocker dans details[12] :                             │
│    selectedOption = null  ← rien sélectionné            │
│    correctOption  = ...   ← la vraie bonne réponse      │
│    isCorrect      = false                               │
│    points         = 0                                   │
└─────────────────────────────────────────────────────────┘

... (idem pour Q4 et Q5)

Résultat final après les 5 questions :
  score       = 20   (Q1=10, Q2=0, Q3=0, Q4=5, Q5=5 par exemple)
  totalPoints = 50
```

---

### Étape 3 — Calcul du pourcentage

```
percentage = (score / totalPoints) * 100
           = (20 / 50) * 100
           = 0.4 * 100
           = 40.0 %

round(40.0, 2) = 40.0
```

**Cas particulier : totalPoints = 0**
```
Si le quiz n'a aucune question → totalPoints = 0
→ division par zéro impossible !
→ le code vérifie : totalPoints > 0 ? calcul : 0
→ percentage = 0
```

---

### Étape 4 — Déterminer le statut (VALIDÉ ou ÉCHEC)

```php
// Dans QuizPassageController.php
$seuilReussite = $quiz->getSeuilReussite() ?? 50;  // défaut 50%

$statut = $result['percentage'] >= $seuilReussite ? 'VALIDÉ' : 'ÉCHEC';
```

```
percentage = 40%
seuil      = 50%

40 >= 50 → FALSE → statut = "ÉCHEC"
```

---

### Résultat final retourné

```
{
  score:       20,
  totalPoints: 50,
  percentage:  40.0,
  details: {
    10: { selectedOption: 101, correctOption: 101, isCorrect: true,  points: 10 },
    11: { selectedOption: 107, correctOption: 106, isCorrect: false, points: 0  },
    12: { selectedOption: null,correctOption: ..., isCorrect: false, points: 0  },
    13: { ... },
    14: { ... }
  }
}
```

---

## Traduction complète en JavaFX

### Modèles nécessaires

```java
// DetailQuestion.java — résultat pour une question
public class DetailQuestion {
    private Question question;
    private Integer  selectedOptionId;  // ce que l'étudiant a choisi (null si pas répondu)
    private Integer  correctOptionId;   // la vraie bonne réponse
    private boolean  isCorrect;         // true si bonne réponse
    private int      points;            // points gagnés (0 ou question.getPoint())

    public DetailQuestion(Question question, Integer selectedOptionId,
                          Integer correctOptionId, boolean isCorrect, int points) {
        this.question         = question;
        this.selectedOptionId = selectedOptionId;
        this.correctOptionId  = correctOptionId;
        this.isCorrect        = isCorrect;
        this.points           = points;
    }

    // Getters
    public Question getQuestion()          { return question; }
    public Integer  getSelectedOptionId()  { return selectedOptionId; }
    public Integer  getCorrectOptionId()   { return correctOptionId; }
    public boolean  isCorrect()            { return isCorrect; }
    public int      getPoints()            { return points; }
}
```

```java
// QuizResult.java — résultat global du quiz
public class QuizResult {
    private int    score;        // points obtenus
    private int    totalPoints;  // points total possible
    private double percentage;   // pourcentage de réussite
    private Map<Integer, DetailQuestion> details; // détail par questionId

    public QuizResult(int score, int totalPoints, double percentage,
                      Map<Integer, DetailQuestion> details) {
        this.score       = score;
        this.totalPoints = totalPoints;
        this.percentage  = percentage;
        this.details     = details;
    }

    // Getters
    public int    getScore()       { return score; }
    public int    getTotalPoints() { return totalPoints; }
    public double getPercentage()  { return percentage; }
    public Map<Integer, DetailQuestion> getDetails() { return details; }
}
```

---

### Le service de calcul — QuizService.java

```java
public class QuizService {

    /**
     * Calcule le score d'un étudiant pour un quiz
     *
     * @param quiz     Le quiz passé
     * @param reponses Map<questionId, optionId> — les réponses de l'étudiant
     * @return QuizResult avec score, totalPoints, percentage, details
     */
    public QuizResult calculateScore(Quiz quiz, Map<Integer, Integer> reponses) {

        int score       = 0;   // points gagnés
        int totalPoints = 0;   // total possible
        Map<Integer, DetailQuestion> details = new HashMap<>();

        // ── Boucle sur chaque question du quiz ──────────────────
        for (Question question : quiz.getQuestions()) {

            // Ajouter les points de cette question au total
            totalPoints += question.getPoint();

            int     questionId      = question.getId();
            boolean isCorrect       = false;
            Integer correctOptionId = null;

            // ── Chercher la bonne réponse parmi les options ─────
            for (Option option : question.getOptions()) {

                if (option.isEstCorrecte()) {
                    correctOptionId = option.getId();

                    // L'étudiant a-t-il choisi cette option ?
                    Integer selectedId = reponses.get(questionId);

                    if (selectedId != null && selectedId.equals(option.getId())) {
                        // ✅ Bonne réponse !
                        isCorrect = true;
                        score += question.getPoint();
                    }

                    break; // une seule bonne réponse → on arrête
                }
            }

            // ── Stocker le détail de cette question ─────────────
            details.put(questionId, new DetailQuestion(
                question,
                reponses.get(questionId),  // ce que l'étudiant a choisi (null si rien)
                correctOptionId,           // la vraie bonne réponse
                isCorrect,
                isCorrect ? question.getPoint() : 0
            ));
        }

        // ── Calcul du pourcentage ────────────────────────────────
        // Éviter la division par zéro si le quiz n'a pas de questions
        double percentage = totalPoints > 0
            ? Math.round((score * 100.0 / totalPoints) * 100.0) / 100.0
            : 0.0;

        return new QuizResult(score, totalPoints, percentage, details);
    }

    /**
     * Détermine si l'étudiant a réussi le quiz
     */
    public String getStatut(Quiz quiz, double percentage) {
        int seuil = quiz.getSeuilReussite() != null
            ? quiz.getSeuilReussite()
            : 50; // défaut 50%

        return percentage >= seuil ? "VALIDÉ" : "ÉCHEC";
    }

    /**
     * Retourne le badge de performance selon le score
     */
    public String getPerformanceBadge(double percentage) {
        if (percentage >= 80) return "🏆 Excellent !";
        if (percentage >= 60) return "👍 Bien joué !";
        if (percentage >= 40) return "📈 Peut mieux faire";
        return "📚 À revoir";
    }
}
```

---

### Utilisation dans le Controller JavaFX

```java
// Dans QuizPassageController.java
// Quand l'étudiant clique "Soumettre le Quiz"

@FXML
private void handleSubmit() {

    // 1. Récupérer les réponses sélectionnées
    //    Map<questionId, optionId sélectionné>
    Map<Integer, Integer> reponses = collectReponses();

    // 2. Calculer le score
    QuizService quizService = new QuizService();
    QuizResult result = quizService.calculateScore(quiz, reponses);

    // 3. Déterminer le statut
    String statut = quizService.getStatut(quiz, result.getPercentage());

    // 4. Afficher l'écran de résultats
    showResultScreen(result, statut);
}

/**
 * Collecte toutes les réponses sélectionnées par l'étudiant
 * depuis les boutons radio de l'interface
 */
private Map<Integer, Integer> collectReponses() {
    Map<Integer, Integer> reponses = new HashMap<>();

    // Pour chaque question affichée
    for (QuestionPane questionPane : questionPanes) {
        int questionId = questionPane.getQuestion().getId();

        // Récupérer l'option sélectionnée (null si rien)
        Integer selectedOptionId = questionPane.getSelectedOptionId();

        if (selectedOptionId != null) {
            reponses.put(questionId, selectedOptionId);
        }
        // Si null → pas mis dans la map → reponses.get(questionId) = null
    }

    return reponses;
}
```

---

## Exemple concret avec des chiffres

```
Quiz : 5 questions × 10 points = 50 points total
Seuil de réussite : 50%

Réponses de l'étudiant :
  Q1 (10pts) → bonne réponse  ✅ → +10
  Q2 (10pts) → mauvaise       ❌ → +0
  Q3 (10pts) → pas répondu    ❌ → +0
  Q4 (10pts) → bonne réponse  ✅ → +10
  Q5 (10pts) → mauvaise       ❌ → +0

Calcul :
  score       = 10 + 0 + 0 + 10 + 0 = 20
  totalPoints = 10 + 10 + 10 + 10 + 10 = 50
  percentage  = (20 / 50) × 100 = 40%

Statut :
  40% >= 50% → FALSE → "ÉCHEC"

Badge :
  40% >= 40% → "📈 Peut mieux faire"

Affichage résultats :
  ┌──────────────────────────────────────────┐
  │   20          ⭕ 40%          50         │
  │ POINTS       RÉUSSITE      POINTS        │
  │ OBTENUS                     TOTAL        │
  │                                          │
  │         [📈 Peut mieux faire]            │
  └──────────────────────────────────────────┘
```

---

## Résumé de la logique en 4 lignes

```
1. Pour chaque question → chercher l'option avec estCorrecte=true
2. Comparer avec ce que l'étudiant a choisi
3. Si identique → score += question.point
4. percentage = (score / totalPoints) × 100
```
