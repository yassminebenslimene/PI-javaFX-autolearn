# Relations : Cours → Chapitre → Quiz (Pour JavaFX)

---

## 1. Schéma global des relations

```
COURS
 │
 │  OneToMany (1 cours a plusieurs chapitres)
 │  cascade = supprime les chapitres si le cours est supprimé
 │
 ▼
CHAPITRE
 │  ← contient FK : cours_id
 │
 │  OneToMany (1 chapitre a plusieurs quiz)
 │  cascade = supprime les quiz si le chapitre est supprimé
 │
 ▼
QUIZ
 │  ← contient FK : chapitre_id
 │
 │  OneToMany (1 quiz a plusieurs questions)
 │  cascade = supprime les questions si le quiz est supprimé
 │
 ▼
QUESTION
 │  ← contient FK : quiz_id
 │
 │  OneToMany (1 question a plusieurs options)
 │  cascade = supprime les options si la question est supprimée
 │
 ▼
OPTION
    ← contient FK : question_id
    ← contient : est_correcte (boolean)
```

---

## 2. Détail de chaque relation

### Cours ↔ Chapitre

```
COURS (1) ──────────────────── (N) CHAPITRE
```

- Un **Cours** peut avoir **plusieurs Chapitres**
- Un **Chapitre** appartient à **un seul Cours**
- La clé étrangère `cours_id` est dans la table **chapitre**
- Si on supprime un Cours → tous ses Chapitres sont supprimés automatiquement

```java
// Dans Cours.java
List<Chapitre> chapitres;   // "je possède plusieurs chapitres"

// Dans Chapitre.java
Cours cours;                // "j'appartiens à un cours"  ← FK cours_id
```

---

### Chapitre ↔ Quiz

```
CHAPITRE (1) ───────────────── (N) QUIZ
```

- Un **Chapitre** peut avoir **plusieurs Quiz**
- Un **Quiz** appartient à **un seul Chapitre**
- La clé étrangère `chapitre_id` est dans la table **quiz**
- Si on supprime un Chapitre → tous ses Quiz sont supprimés automatiquement

```java
// Dans Chapitre.java
List<Quiz> quizzes;         // "je possède plusieurs quiz"

// Dans Quiz.java
Chapitre chapitre;          // "j'appartiens à un chapitre"  ← FK chapitre_id
```

---

### Quiz ↔ Question

```
QUIZ (1) ───────────────────── (N) QUESTION
```

- Un **Quiz** peut avoir **plusieurs Questions**
- Une **Question** appartient à **un seul Quiz**
- La clé étrangère `quiz_id` est dans la table **question**

```java
// Dans Quiz.java
List<Question> questions;   // "je possède plusieurs questions"

// Dans Question.java
Quiz quiz;                  // "j'appartiens à un quiz"  ← FK quiz_id
```

---

### Question ↔ Option

```
QUESTION (1) ───────────────── (N) OPTION
```

- Une **Question** peut avoir **plusieurs Options** (minimum 2)
- Une **Option** appartient à **une seule Question**
- La clé étrangère `question_id` est dans la table **option**
- Une option a un champ `estCorrecte` (true/false)
- **Règle** : au moins 1 option doit avoir `estCorrecte = true`

```java
// Dans Question.java
List<Option> options;       // "je possède plusieurs options"

// Dans Option.java
Question question;          // "j'appartiens à une question"  ← FK question_id
boolean estCorrecte;        // ⚠️ NE PAS envoyer au frontend pendant le quiz !
```

---

## 3. Tables SQL complètes

```sql
-- ① Table cours (racine)
CREATE TABLE cours (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    titre           VARCHAR(255) NOT NULL,
    description     TEXT,
    matiere         VARCHAR(255) NOT NULL,
    niveau          VARCHAR(50)  NOT NULL,
    duree           INT          NOT NULL,
    created_at      DATETIME     NOT NULL
);

-- ② Table chapitre (FK → cours)
CREATE TABLE chapitre (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    titre           VARCHAR(255) NOT NULL,
    contenu         TEXT         NOT NULL,
    ordre           INT          NOT NULL,
    ressources      VARCHAR(255),
    cours_id        INT          NOT NULL,
    FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE
);

-- ③ Table quiz (FK → chapitre)
CREATE TABLE quiz (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    titre               VARCHAR(255) NOT NULL,
    description         TEXT         NOT NULL,
    etat                VARCHAR(50)  NOT NULL DEFAULT 'brouillon',
    duree_max_minutes   INT,
    seuil_reussite      INT          DEFAULT 50,
    max_tentatives      INT,
    chapitre_id         INT,
    FOREIGN KEY (chapitre_id) REFERENCES chapitre(id) ON DELETE CASCADE
);

-- ④ Table question (FK → quiz)
CREATE TABLE question (
    id                  INT PRIMARY KEY AUTO_INCREMENT,
    texte_question      TEXT         NOT NULL,
    point               INT          NOT NULL DEFAULT 10,
    quiz_id             INT          NOT NULL,
    FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
);

-- ⑤ Table option (FK → question)
CREATE TABLE option_reponse (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    texte_option    TEXT         NOT NULL,
    est_correcte    BOOLEAN      NOT NULL DEFAULT FALSE,
    question_id     INT          NOT NULL,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);
```

---

## 4. Classes Java complètes pour JavaFX

### Cours.java
```java
public class Cours {
    private int id;
    private String titre;
    private String description;
    private String matiere;
    private String niveau;
    private int duree;
    private LocalDateTime createdAt;

    // Relation OneToMany vers Chapitre
    private List<Chapitre> chapitres = new ArrayList<>();

    // Getters / Setters
    public List<Chapitre> getChapitres() { return chapitres; }
    public void addChapitre(Chapitre c) {
        chapitres.add(c);
        c.setCours(this);   // lier les deux côtés
    }
}
```

### Chapitre.java
```java
public class Chapitre {
    private int id;
    private String titre;
    private String contenu;
    private int ordre;
    private String ressources;

    // Relation ManyToOne vers Cours  ← FK cours_id
    private Cours cours;

    // Relation OneToMany vers Quiz
    private List<Quiz> quizzes = new ArrayList<>();

    // Getters / Setters
    public Cours getCours() { return cours; }
    public void setCours(Cours cours) { this.cours = cours; }

    public List<Quiz> getQuizzes() { return quizzes; }
    public void addQuiz(Quiz q) {
        quizzes.add(q);
        q.setChapitre(this);  // lier les deux côtés
    }
}
```

### Quiz.java
```java
public class Quiz {
    private int id;
    private String titre;
    private String description;
    private String etat;           // "actif" | "inactif" | "brouillon" | "archive"
    private Integer dureeMaxMinutes;
    private Integer seuilReussite; // défaut 50
    private Integer maxTentatives;

    // Relation ManyToOne vers Chapitre  ← FK chapitre_id
    private Chapitre chapitre;

    // Relation OneToMany vers Question
    private List<Question> questions = new ArrayList<>();

    // Getters / Setters
    public Chapitre getChapitre() { return chapitre; }
    public void setChapitre(Chapitre chapitre) { this.chapitre = chapitre; }

    public List<Question> getQuestions() { return questions; }
    public void addQuestion(Question q) {
        questions.add(q);
        q.setQuiz(this);
    }

    // Accéder au cours parent depuis le quiz
    public Cours getCours() {
        return chapitre != null ? chapitre.getCours() : null;
    }
}
```

### Question.java
```java
public class Question {
    private int id;
    private String texteQuestion;
    private int point;

    // Relation ManyToOne vers Quiz  ← FK quiz_id
    private Quiz quiz;

    // Relation OneToMany vers Option
    private List<Option> options = new ArrayList<>();

    public List<Option> getOptions() { return options; }
    public void addOption(Option o) {
        options.add(o);
        o.setQuestion(this);
    }
}
```

### Option.java
```java
public class Option {
    private int id;
    private String texteOption;
    private boolean estCorrecte;  // ⚠️ ne jamais envoyer au frontend !

    // Relation ManyToOne vers Question  ← FK question_id
    private Question question;

    public boolean isEstCorrecte() { return estCorrecte; }
    public void setEstCorrecte(boolean estCorrecte) { this.estCorrecte = estCorrecte; }
}
```

---

## 5. Comment utiliser les relations dans le code JavaFX

### Charger les quiz d'un chapitre
```java
// Depuis un chapitre, récupérer ses quiz actifs
Chapitre chapitre = chapitreService.findById(21);

List<Quiz> quizActifs = chapitre.getQuizzes()
    .stream()
    .filter(q -> "actif".equals(q.getEtat()))
    .collect(Collectors.toList());
```

### Remonter du Quiz vers le Cours
```java
Quiz quiz = quizService.findById(2);

Chapitre chapitre = quiz.getChapitre();   // 1 niveau au-dessus
Cours cours       = chapitre.getCours();  // 2 niveaux au-dessus

System.out.println("Ce quiz appartient au cours : " + cours.getTitre());
```

### Calculer le total des points d'un quiz
```java
int totalPoints = quiz.getQuestions()
    .stream()
    .mapToInt(Question::getPoint)
    .sum();
```

### Trouver la bonne réponse d'une question
```java
Option bonneReponse = question.getOptions()
    .stream()
    .filter(Option::isEstCorrecte)
    .findFirst()
    .orElse(null);
```

### Construire un quiz complet (exemple de données)
```java
// Créer le cours
Cours cours = new Cours();
cours.setTitre("Java Programming");
cours.setMatiere("Informatique");
cours.setNiveau("Débutant");

// Créer le chapitre
Chapitre chapitre = new Chapitre();
chapitre.setTitre("Loops and Iterations");
chapitre.setOrdre(2);
cours.addChapitre(chapitre);  // lie chapitre.cours = cours

// Créer le quiz
Quiz quiz = new Quiz();
quiz.setTitre("Quiz - Loops and Iterations");
quiz.setEtat("actif");
quiz.setDureeMaxMinutes(3);
quiz.setSeuilReussite(50);
chapitre.addQuiz(quiz);       // lie quiz.chapitre = chapitre

// Créer une question
Question q1 = new Question();
q1.setTexteQuestion("Qu'est-ce qui se passe si la condition d'une boucle while est toujours vraie ?");
q1.setPoint(10);
quiz.addQuestion(q1);         // lie q1.quiz = quiz

// Créer les options
Option o1 = new Option();
o1.setTexteOption("La boucle entre dans une boucle infinie");
o1.setEstCorrecte(true);      // ✅ bonne réponse
q1.addOption(o1);

Option o2 = new Option();
o2.setTexteOption("La boucle est ignorée par le programme");
o2.setEstCorrecte(false);
q1.addOption(o2);
```

---

## 6. Résumé en une ligne

```
Cours (1) → (N) Chapitre (1) → (N) Quiz (1) → (N) Question (1) → (N) Option
```

Chaque flèche = **OneToMany** du côté gauche, **ManyToOne** du côté droit.
La **clé étrangère** est toujours dans la table de **droite** (le côté "Many").
