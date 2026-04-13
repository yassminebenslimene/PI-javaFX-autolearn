# Documentation Fonctionnalité Quiz - Réimplémentation JavaFX

## Vue d'ensemble

Cette fonctionnalité permet à un étudiant de passer un quiz interactif style **Kahoot**.
Le flux complet se déroule en **4 écrans** :

1. **Liste des quiz** → carte avec infos + bouton "Commencer le quiz"
2. **Écran de chargement** → animation 2 secondes
3. **Passage du quiz** → questions une par une avec chronomètre
4. **Résultats** → score, pourcentage, statistiques + boutons d'action

---

## ÉCRAN 1 — Liste des Quiz (`/chapitre/{id}/quiz`)

### Ce que l'utilisateur voit
- Fond violet foncé `#46178F` avec motif diagonal
- Bouton "← Retour aux chapitres" en haut à gauche
- Une **carte blanche** par quiz avec :
  - Barre colorée en haut (rouge → bleu → orange → vert)
  - Icône colorée (cerveau, ampoule, diplôme...)
  - Titre du quiz (ex: "Quiz - Loops and Iterations")
  - Description courte
  - 3 badges d'info : **Questions / Points / Minutes**
  - Bouton vert **"Commencer le quiz"** (ou orange "Nouvelle tentative" si déjà tenté)

### Données affichées sur la carte
```
Quiz.titre          → "Quiz - Loops and Iterations"
Quiz.description    → "Quiz généré automatiquement..."
Quiz.questions.size → 5  (QUESTIONS)
somme(question.point) → 50 (POINTS)
Quiz.dureeMaxMinutes  → 3  (MINUTES)
```

### Logique du bouton
| Condition | Bouton affiché |
|-----------|---------------|
| Jamais tenté | 🟢 "Commencer le quiz" |
| Tenté, pas réussi | 🟠 "Nouvelle tentative" |
| Réussi | 🔵 "Refaire le quiz" |
| Max tentatives atteint | ⚫ "Tentatives épuisées" (désactivé) |

### Effets sonores (Web Audio API)
- Hover sur carte → son court 600Hz
- Clic sur bouton → montée C5 → E5 → G5

---

## ÉCRAN 2 — Chargement (`/quiz/{id}/start`)

### Ce que l'utilisateur voit
- Fond dégradé `#667eea → #764ba2`
- Logo animé : 4 carrés colorés (rouge, bleu, orange, vert) qui tournent
- Texte "Chargement du Quiz..."
- Sous-titre : nom du quiz
- Durée : **2 secondes** puis disparition avec `fadeOut`

### Ce qui se passe côté serveur (Controller `start()`)
```
1. Vérifier que l'utilisateur est ROLE_ETUDIANT
2. Vérifier que le quiz est "actif"
3. Vérifier qu'il n'y a pas de tentative en cours (session)
4. Mélanger les questions (shuffle)
5. Mélanger les options de chaque question (shuffle)
6. Stocker en session :
   - etudiant_id
   - quiz_id
   - date_debut (Y-m-d H:i:s)
   - timestamp_debut
   - quiz_data (questions + options mélangées, SANS les bonnes réponses)
7. Rendre le template passage.html.twig
```

**Important sécurité** : les `isEstCorrecte` ne sont PAS envoyés au frontend.

---

## ÉCRAN 3 — Passage du Quiz (`/quiz/{id}/start` - même URL)

### Layout général
```
┌─────────────────────────────────────────────────────┐
│  [← Prev]  Quiz - Loops and Iterations  [Next →]   🔊  ⏱ 2:33  │
├─────────────────────────────────────────────────────┤
│                                                     │
│              Question 1 / 5                         │
│                                                     │
│   ┌─────────────────────────────────────────────┐   │
│   │  Qu'est-ce qui se passe si la condition...  │   │
│   └─────────────────────────────────────────────┘   │
│                  ⭐ 10 points                        │
│                                                     │
│   ┌──────────────┐  ┌──────────────┐               │
│   │ ▲  Option A  │  │ ◆  Option B  │               │
│   │   (ROUGE)    │  │   (BLEU)     │               │
│   └──────────────┘  └──────────────┘               │
│   ┌──────────────┐  ┌──────────────┐               │
│   │ ●  Option C  │  │ ■  Option D  │               │
│   │  (ORANGE)    │  │   (VERT)     │               │
│   └──────────────┘  └──────────────┘               │
│                                                     │
├─────────────────────────────────────────────────────┤
│  [←]  [→]                    [✓ Soumettre le Quiz] │
├─────────────────────────────────────────────────────┤
│  0 / 5 questions répondues  ████░░░░░░░░░░░░░░░░░  │
└─────────────────────────────────────────────────────┘
```

### Composants détaillés

#### Header fixe
- Titre du quiz à gauche
- Bouton 🔊 (toggle son, sauvegardé en localStorage)
- Chronomètre `⏱ MM:SS` à droite (fond blanc → orange si <30s → rouge si <10s)

#### Carte de question
- Badge "Question X / N" (fond blanc, texte violet)
- Texte de la question (fond blanc, grande police 32px, centré)
- Badge points "⭐ X points"

#### Grille des options (2×2)
| Position | Couleur | Icône |
|----------|---------|-------|
| Haut-gauche | Rouge `#e74c3c` | ▲ |
| Haut-droite | Bleu `#3498db` | ◆ |
| Bas-gauche | Orange `#f39c12` | ● |
| Bas-droite | Vert `#2ecc71` | ■ |

- Clic sur option → bordure blanche + scale(1.08) + son de sélection
- `<input type="radio" name="answers[questionId]" value="optionId">`

#### Navigation bas
- Boutons `[←]` `[→]` ronds (fond blanc, icône violette)
- Bouton `[✓ Soumettre le Quiz]` vert en bas à droite (fixe)
- Barre de progression verte en bas

### Chronomètre JavaScript
```javascript
// Calcul du temps restant
tempsRestant = dureeMaxSecondes - tempsEcoule
affichage = MM:SS

// États visuels
> 30s  → fond blanc, texte violet (normal)
≤ 30s  → fond orange, animation pulse 1s
≤ 10s  → fond rouge, animation pulse 0.5s
= 0s   → soumission automatique du formulaire
```

### Navigation entre questions
```javascript
function showQuestion(index) {
    // Cacher toutes les .question-card
    // Afficher celle avec data-question-index == index
    // Mettre à jour prev/next buttons (disabled si premier/dernier)
}
```

### Compteur de progression
```javascript
function updateProgress() {
    // Compter les radio buttons cochés
    // Mettre à jour "X / N questions répondues"
    // Mettre à jour la largeur de la barre verte
}
```

---

## ÉCRAN 4 — Résultats (`/quiz/{id}/submit` POST)

### Ce qui se passe côté serveur (Controller `submit()`)
```
1. Récupérer la tentative depuis la session
2. Calculer la durée réelle (dateDebut → dateFin)
3. Récupérer les réponses POST : answers[questionId] = optionId
4. Calculer le score :
   - Pour chaque question : comparer optionId sélectionné avec isEstCorrecte
   - score += question.point si correct
   - percentage = (score / totalPoints) * 100
5. Déterminer statut : percentage >= seuilReussite (50%) → "VALIDÉ" sinon "ÉCHEC"
6. Si VALIDÉ → marquer le chapitre comme complété (CourseProgressService)
7. Supprimer la tentative de la session
8. Générer explications IA (QuizCorrectorAIService)
9. Rendre result_with_ai.html.twig
```

### Layout résultats

#### Section Score principal
```
┌─────────────────────────────────────────────────────┐
│              📊 Résultats du Quiz                   │
│           Quiz - Loops and Iterations               │
│                                                     │
│    20              ⭕ 40%           50              │
│  POINTS            RÉUSSITE       POINTS            │
│  OBTENUS                           TOTAL            │
│                                                     │
│              [🏆 Peut mieux faire]                  │
└─────────────────────────────────────────────────────┘
```

#### Cercle de pourcentage
- Cercle SVG avec `conic-gradient`
- Couleur dynamique selon score :
  - ≥ 80% → vert `#2ecc71`
  - ≥ 60% → bleu `#3498db`
  - ≥ 40% → orange `#f39c12`
  - < 40% → rouge `#e74c3c`

#### Badge de performance
| Score | Badge | Icône |
|-------|-------|-------|
| ≥ 80% | "Excellent !" | 🏆 |
| ≥ 60% | "Bien joué !" | 👍 |
| ≥ 40% | "Peut mieux faire" | 📈 |
| < 40% | "À revoir" | 📚 |

#### Section Statistiques tentatives
```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  ▶  1        │  │  🟠 40%      │  │  🔄 OUI      │
│  TENTATIVE/3 │  │  MEILLEUR    │  │  PEUT        │
│              │  │  SCORE       │  │  RECOMMENCER │
└──────────────┘  └──────────────┘  └──────────────┘
```

#### Boutons d'action
```
[🔄 Refaire le quiz]  [← Autres quiz]  [🏠 Accueil]
     (BLEU)               (BLANC)          (VERT)
```

#### Bouton flottant IA
- Position fixe bas-droite
- Animation `float` (monte/descend)
- Ouvre un chat avec le tuteur IA

---

## Modèle de données

### Entité Quiz
```java
// Équivalent JavaFX
class Quiz {
    int id
    String titre           // "Quiz - Loops and Iterations"
    String description     // Description longue
    String etat            // "actif" | "inactif" | "brouillon" | "archive"
    Integer dureeMaxMinutes // 3 (null = pas de limite)
    Integer seuilReussite  // 50 (défaut 50%)
    Integer maxTentatives  // 3 (null = illimité)
    String imageName       // nom du fichier image (optionnel)
    List<Question> questions
}
```

### Entité Question
```java
class Question {
    int id
    String texteQuestion   // "Qu'est-ce qui se passe si..."
    int point              // 10
    String imageName       // optionnel
    List<Option> options
}
```

### Entité Option
```java
class Option {
    int id
    String texteOption     // "La boucle entre dans une boucle infinie"
    boolean estCorrecte    // true/false (NE PAS afficher pendant le quiz !)
}
```

### Résultat calculé
```java
class QuizResult {
    int score              // 20
    int totalPoints        // 50
    double percentage      // 40.0
    Map<Integer, DetailQuestion> details
    // details[questionId] = {question, selectedOptionId, correctOptionId, isCorrect, points}
}
```

---

## Architecture JavaFX recommandée

### Structure des fichiers
```
src/
├── model/
│   ├── Quiz.java
│   ├── Question.java
│   ├── Option.java
│   └── QuizResult.java
├── service/
│   ├── QuizService.java        // logique métier (calcul score, validation)
│   └── QuizSessionService.java // gestion état en cours
├── controller/
│   ├── QuizListController.java
│   ├── QuizLoadingController.java
│   ├── QuizPassageController.java
│   └── QuizResultController.java
└── view/
    ├── quiz-list.fxml
    ├── quiz-loading.fxml
    ├── quiz-passage.fxml
    └── quiz-result.fxml
```

### Navigation entre scènes
```
QuizListController
    → clic "Commencer le quiz"
    → passer Quiz en paramètre
    → afficher QuizLoadingController (2 secondes)
    → afficher QuizPassageController

QuizPassageController
    → clic "Soumettre le Quiz" ou timer = 0
    → calculer score via QuizService
    → afficher QuizResultController

QuizResultController
    → clic "Refaire le quiz" → retour QuizLoadingController
    → clic "Autres quiz"    → retour QuizListController
    → clic "Accueil"        → retour HomeController
```

---

## Design JavaFX — Couleurs et styles CSS

### Palette principale
```css
/* Fond principal */
-fx-background-color: #46178F;

/* Options de réponse */
option-rouge:  #e74c3c → #c0392b
option-bleu:   #3498db → #2980b9
option-orange: #f39c12 → #e67e22
option-vert:   #2ecc71 → #27ae60

/* Texte */
texte-principal: #1f2937
texte-secondaire: #6b7280
texte-blanc: #ffffff

/* Accents */
violet-accent: #46178F
violet-clair:  #764ba2
```

### Fichier `quiz-styles.css` JavaFX
```css
.quiz-background {
    -fx-background-color: #46178F;
}

.question-card {
    -fx-background-color: white;
    -fx-background-radius: 20;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 10);
    -fx-padding: 40 60 40 60;
}

.question-text {
    -fx-font-size: 24px;
    -fx-font-weight: bold;
    -fx-text-fill: #1f2937;
    -fx-wrap-text: true;
    -fx-text-alignment: center;
}

.option-rouge {
    -fx-background-color: linear-gradient(to bottom right, #e74c3c, #c0392b);
    -fx-background-radius: 15;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-cursor: hand;
    -fx-min-height: 100;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 5);
}

.option-bleu {
    -fx-background-color: linear-gradient(to bottom right, #3498db, #2980b9);
    -fx-background-radius: 15;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-cursor: hand;
    -fx-min-height: 100;
}

.option-orange {
    -fx-background-color: linear-gradient(to bottom right, #f39c12, #e67e22);
    -fx-background-radius: 15;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-cursor: hand;
    -fx-min-height: 100;
}

.option-vert {
    -fx-background-color: linear-gradient(to bottom right, #2ecc71, #27ae60);
    -fx-background-radius: 15;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-cursor: hand;
    -fx-min-height: 100;
}

.option-selected {
    -fx-border-color: white;
    -fx-border-width: 4;
    -fx-border-radius: 15;
    -fx-scale-x: 1.05;
    -fx-scale-y: 1.05;
}

.timer-normal {
    -fx-background-color: white;
    -fx-background-radius: 50;
    -fx-text-fill: #46178F;
    -fx-font-weight: bold;
    -fx-font-size: 20px;
    -fx-padding: 10 25 10 25;
}

.timer-warning {
    -fx-background-color: #f39c12;
    -fx-text-fill: white;
}

.timer-danger {
    -fx-background-color: #e74c3c;
    -fx-text-fill: white;
}

.btn-submit {
    -fx-background-color: linear-gradient(to bottom right, #2ecc71, #27ae60);
    -fx-background-radius: 50;
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 16px;
    -fx-padding: 15 35 15 35;
    -fx-cursor: hand;
}

.progress-bar .track {
    -fx-background-color: rgba(255,255,255,0.2);
    -fx-background-radius: 10;
}

.progress-bar .bar {
    -fx-background-color: linear-gradient(to right, #2ecc71, #27ae60);
    -fx-background-radius: 10;
}
```

---

## Animations JavaFX

### Écran de chargement — Logo rotatif
```java
// 4 rectangles qui tournent ensemble
RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), logoGroup);
rotate.setByAngle(360);
rotate.setCycleCount(Animation.INDEFINITE);
rotate.setInterpolator(Interpolator.EASE_BOTH);
rotate.play();

// Après 2 secondes → transition vers quiz
PauseTransition pause = new PauseTransition(Duration.seconds(2));
pause.setOnFinished(e -> showQuizPassage());
pause.play();
```

### Transition entre questions
```java
FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentCard);
fadeOut.setToValue(0);
fadeOut.setOnFinished(e -> {
    showNextCard();
    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), nextCard);
    fadeIn.setFromValue(0);
    fadeIn.setToValue(1);
    fadeIn.play();
});
fadeOut.play();
```

### Sélection d'une option
```java
ScaleTransition scale = new ScaleTransition(Duration.millis(150), optionButton);
scale.setToX(1.05);
scale.setToY(1.05);
scale.play();
```

### Chronomètre
```java
Timeline timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
    secondsLeft--;
    updateTimerDisplay();
    if (secondsLeft <= 30) timerLabel.getStyleClass().add("timer-warning");
    if (secondsLeft <= 10) timerLabel.getStyleClass().add("timer-danger");
    if (secondsLeft <= 0) submitQuiz();
}));
timer.setCycleCount(totalSeconds);
timer.play();
```

---

## Logique métier JavaFX (QuizService.java)

```java
public class QuizService {

    // Mélanger les questions
    public List<Question> shuffleQuestions(Quiz quiz) {
        List<Question> questions = new ArrayList<>(quiz.getQuestions());
        Collections.shuffle(questions);
        return questions;
    }

    // Mélanger les options d'une question
    public List<Option> shuffleOptions(Question question) {
        List<Option> options = new ArrayList<>(question.getOptions());
        Collections.shuffle(options);
        return options;
    }

    // Calculer le score
    public QuizResult calculateScore(Quiz quiz, Map<Integer, Integer> answers) {
        // answers = Map<questionId, selectedOptionId>
        int score = 0;
        int totalPoints = 0;
        Map<Integer, DetailQuestion> details = new HashMap<>();

        for (Question question : quiz.getQuestions()) {
            totalPoints += question.getPoint();
            Integer selectedOptionId = answers.get(question.getId());
            boolean isCorrect = false;
            Integer correctOptionId = null;

            for (Option option : question.getOptions()) {
                if (option.isEstCorrecte()) {
                    correctOptionId = option.getId();
                    if (option.getId().equals(selectedOptionId)) {
                        isCorrect = true;
                        score += question.getPoint();
                    }
                    break;
                }
            }

            details.put(question.getId(), new DetailQuestion(
                question, selectedOptionId, correctOptionId, isCorrect,
                isCorrect ? question.getPoint() : 0
            ));
        }

        double percentage = totalPoints > 0 ? (score * 100.0 / totalPoints) : 0;
        return new QuizResult(score, totalPoints, percentage, details);
    }

    // Déterminer le statut
    public String getStatut(Quiz quiz, double percentage) {
        int seuil = quiz.getSeuilReussite() != null ? quiz.getSeuilReussite() : 50;
        return percentage >= seuil ? "VALIDÉ" : "ÉCHEC";
    }

    // Badge de performance
    public String getPerformanceBadge(double percentage) {
        if (percentage >= 80) return "Excellent !";
        if (percentage >= 60) return "Bien joué !";
        if (percentage >= 40) return "Peut mieux faire";
        return "À revoir";
    }
}
```

---

## Flux complet résumé

```
[Page Chapitre]
      │
      ▼
[QuizListController] ──── GET /chapitre/{id}/quiz
      │  Affiche les cartes quiz avec stats
      │  Clic "Commencer le quiz"
      ▼
[QuizLoadingController] ── Appel API GET /quiz/{id}/start
      │  Animation 2s (logo rotatif)
      │  Reçoit quizData (questions mélangées, sans réponses)
      ▼
[QuizPassageController]
      │  Affiche question par question
      │  Chronomètre décompte
      │  Étudiant sélectionne réponses
      │  Clic "Soumettre" ou timer = 0
      ▼
[Calcul score local ou POST /quiz/{id}/submit]
      │  score, percentage, statut
      ▼
[QuizResultController]
      │  Affiche score / cercle % / badge
      │  Affiche statistiques tentatives
      │  Boutons : Refaire / Autres quiz / Accueil
      ▼
[Fin]
```
