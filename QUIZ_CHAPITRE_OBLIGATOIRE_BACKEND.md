# OBLIGATOIRE : Chaque Quiz doit appartenir à un Chapitre — Explication Backend

Cette règle est appliquée à **3 niveaux** dans le backend Symfony.
En JavaFX, il faut reproduire ces 3 mêmes niveaux.

---

## Niveau 1 — Base de données (SQL)

La contrainte la plus forte. Même si le code Java fait une erreur,
la base de données refuse d'enregistrer un quiz sans chapitre.

### Dans Symfony (Doctrine ORM)
```php
// src/Entity/Quiz.php

#[ORM\ManyToOne(inversedBy: 'quizzes')]
#[ORM\JoinColumn(nullable: true)]   // ← nullable: true = la FK peut être NULL en BDD
private ?Chapitre $chapitre = null;
```

> Note : dans ce projet la FK est `nullable: true` en BDD,
> mais la validation est faite au niveau du formulaire et du service.

### En JavaFX — Table SQL à créer
```sql
CREATE TABLE quiz (
    id              INT PRIMARY KEY AUTO_INCREMENT,
    titre           VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    etat            VARCHAR(50)  NOT NULL DEFAULT 'brouillon',
    duree_max_minutes INT,
    seuil_reussite  INT          DEFAULT 50,
    max_tentatives  INT,

    -- La clé étrangère vers chapitre
    chapitre_id     INT          NOT NULL,   -- ← NOT NULL = obligatoire !

    FOREIGN KEY (chapitre_id) REFERENCES chapitre(id) ON DELETE CASCADE
);
```

---

## Niveau 2 — Formulaire (Validation côté interface)

C'est ce que l'utilisateur voit dans le backoffice quand il crée un quiz.
Le champ "Chapitre" est marqué obligatoire avec `required: true`.

### Dans Symfony (QuizType.php)
```php
// src/Form/QuizType.php

->add('chapitre', EntityType::class, [
    'class'        => Chapitre::class,
    'choice_label' => 'titre',
    'label'        => 'Chapitre *',

    // Texte affiché par défaut dans le select (option vide)
    'placeholder'  => 'Sélectionnez un chapitre obligatoirement',

    // ← OBLIGATOIRE : le formulaire refuse la soumission si vide
    'required'     => true,

    // Message d'aide affiché sous le champ
    'help'         => '🔒 OBLIGATOIRE : Chaque quiz doit appartenir à un chapitre',

    'attr' => [
        'class' => 'required-field'
    ]
])
```

### Ce que ça donne visuellement (capture d'écran)
```
┌─────────────────────────────────────────────────────┐
│  Chapitre *                                         │
│  ┌─────────────────────────────────────────────┐   │
│  │  Sélectionnez un chapitre obligatoirement ▼ │   │
│  └─────────────────────────────────────────────┘   │
│  🔒 OBLIGATOIRE : Chaque quiz doit appartenir       │
│     à un chapitre                                   │
└─────────────────────────────────────────────────────┘
```

### En JavaFX — ComboBox équivalente
```java
// Dans QuizFormController.java

@FXML
private ComboBox<Chapitre> chapitreComboBox;

@FXML
private Label chapitreErrorLabel;

// Initialisation : charger tous les chapitres depuis la BDD
public void initialize() {
    List<Chapitre> chapitres = chapitreService.findAll();
    chapitreComboBox.getItems().addAll(chapitres);

    // Afficher le titre du chapitre dans la liste
    chapitreComboBox.setCellFactory(lv -> new ListCell<>() {
        @Override
        protected void updateItem(Chapitre item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getTitre());
        }
    });

    // Afficher le titre sélectionné dans le bouton
    chapitreComboBox.setButtonCell(new ListCell<>() {
        @Override
        protected void updateItem(Chapitre item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null
                ? "Sélectionnez un chapitre obligatoirement"
                : item.getTitre());
        }
    });
}

// Validation avant sauvegarde
private boolean validateForm() {
    if (chapitreComboBox.getValue() == null) {
        chapitreErrorLabel.setText("🔒 OBLIGATOIRE : Sélectionnez un chapitre");
        chapitreErrorLabel.setVisible(true);
        chapitreComboBox.setStyle("-fx-border-color: red; -fx-border-width: 2;");
        return false;
    }
    chapitreErrorLabel.setVisible(false);
    chapitreComboBox.setStyle("");
    return true;
}
```

---

## Niveau 3 — Service métier (Validation côté logique)

Même si quelqu'un contourne le formulaire (API, script...),
le service vérifie encore une fois la règle.

### Dans Symfony (QuizManagementService.php)
```php
// src/Service/QuizManagementService.php

public function validateQuizBusinessRules(Quiz $quiz): array
{
    $errors = [];

    // ← LA RÈGLE OBLIGATOIRE
    if ($quiz->getChapitre() === null) {
        $errors[] = '🔒 Un quiz doit obligatoirement appartenir à un chapitre.';
    }

    // Autres validations...
    if (empty($quiz->getTitre()) || strlen(trim($quiz->getTitre())) < 3) {
        $errors[] = 'Le titre du quiz doit contenir au moins 3 caractères.';
    }

    return [
        'valid'  => empty($errors),
        'errors' => $errors
    ];
}
```

### En JavaFX — Service équivalent
```java
// src/service/QuizService.java

public class QuizService {

    public ValidationResult validateQuiz(Quiz quiz) {
        List<String> errors = new ArrayList<>();

        // ← LA RÈGLE OBLIGATOIRE
        if (quiz.getChapitre() == null) {
            errors.add("🔒 Un quiz doit obligatoirement appartenir à un chapitre.");
        }

        // Validation du titre
        if (quiz.getTitre() == null || quiz.getTitre().trim().length() < 3) {
            errors.add("Le titre doit contenir au moins 3 caractères.");
        }

        // Validation de l'état
        List<String> etatsValides = List.of("actif", "inactif", "brouillon", "archive");
        if (!etatsValides.contains(quiz.getEtat())) {
            errors.add("L'état doit être : actif, inactif, brouillon ou archive.");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    public Quiz saveQuiz(Quiz quiz) {
        // Valider AVANT de sauvegarder
        ValidationResult result = validateQuiz(quiz);

        if (!result.isValid()) {
            throw new IllegalArgumentException(
                "Quiz invalide : " + String.join(", ", result.getErrors())
            );
        }

        return quizRepository.save(quiz);
    }
}

// Classe utilitaire pour le résultat de validation
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
}
```

---

## Résumé des 3 niveaux

```
┌─────────────────────────────────────────────────────────────────┐
│                    RÈGLE : Quiz → Chapitre obligatoire          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  NIVEAU 1 — BASE DE DONNÉES                                     │
│  ─────────────────────────                                      │
│  Symfony : @ORM\JoinColumn(nullable: true)                      │
│  JavaFX  : chapitre_id INT NOT NULL + FOREIGN KEY               │
│  → La BDD refuse INSERT si chapitre_id est NULL                 │
│                                                                 │
│  NIVEAU 2 — FORMULAIRE (Interface utilisateur)                  │
│  ─────────────────────────────────────────────                  │
│  Symfony : EntityType avec required: true + placeholder         │
│  JavaFX  : ComboBox + validation dans validateForm()            │
│  → L'utilisateur voit une erreur rouge si rien n'est choisi     │
│                                                                 │
│  NIVEAU 3 — SERVICE MÉTIER (Logique applicative)                │
│  ────────────────────────────────────────────────               │
│  Symfony : validateQuizBusinessRules() dans QuizManagementService│
│  JavaFX  : validateQuiz() dans QuizService                      │
│  → Exception levée si on essaie de sauvegarder sans chapitre    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Flux complet en JavaFX (du formulaire à la BDD)

```
[Utilisateur clique "Sauvegarder"]
         │
         ▼
[validateForm()]  ← Niveau 2
  chapitre == null ?
         │ OUI → afficher erreur rouge sur ComboBox → STOP
         │ NON ↓
         ▼
[QuizService.validateQuiz()]  ← Niveau 3
  chapitre == null ?
         │ OUI → lancer IllegalArgumentException → STOP
         │ NON ↓
         ▼
[QuizRepository.save(quiz)]  ← Niveau 1
  INSERT INTO quiz (chapitre_id, ...) VALUES (?, ...)
  chapitre_id NULL ?
         │ OUI → SQLException (NOT NULL constraint) → STOP
         │ NON ↓
         ▼
[Quiz sauvegardé avec succès ✅]
```

---

## Code FXML pour le formulaire JavaFX

```xml
<!-- quiz-form.fxml -->
<VBox spacing="10">

    <!-- Champ Titre -->
    <Label text="Titre du quiz" styleClass="form-label"/>
    <TextField fx:id="titreField" promptText="Entrez le titre du quiz"/>

    <!-- Champ État -->
    <Label text="État" styleClass="form-label"/>
    <ComboBox fx:id="etatComboBox"/>

    <!-- Champ Chapitre OBLIGATOIRE -->
    <Label text="Chapitre *" styleClass="form-label required-label"/>
    <ComboBox fx:id="chapitreComboBox"
              promptText="Sélectionnez un chapitre obligatoirement"
              maxWidth="Infinity"/>

    <!-- Message d'erreur (caché par défaut) -->
    <Label fx:id="chapitreErrorLabel"
           text="🔒 OBLIGATOIRE : Sélectionnez un chapitre"
           styleClass="error-label"
           visible="false"/>

    <!-- Texte d'aide -->
    <Label text="🔒 OBLIGATOIRE : Chaque quiz doit appartenir à un chapitre"
           styleClass="help-label"/>

    <!-- Bouton Sauvegarder -->
    <Button text="Sauvegarder" onAction="#handleSave"
            styleClass="btn-save"/>

</VBox>
```

```css
/* quiz-form.css */
.required-label {
    -fx-text-fill: #e74c3c;
    -fx-font-weight: bold;
}

.error-label {
    -fx-text-fill: #e74c3c;
    -fx-font-size: 13px;
}

.help-label {
    -fx-text-fill: #6b7280;
    -fx-font-size: 12px;
}

.combobox-error {
    -fx-border-color: #e74c3c;
    -fx-border-width: 2;
    -fx-border-radius: 5;
}
```
