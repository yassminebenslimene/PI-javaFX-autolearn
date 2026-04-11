# Guide Complet : Transfert CRUD Quiz (Symfony → JavaFX)

## Contexte du Projet

Ce document explique comment reproduire le CRUD Quiz/Question/Option du projet Symfony dans JavaFX.
Le projet JavaFX utilise JDBC direct (pas d'ORM), avec une architecture MVC manuelle.

---

## Structure de la Base de Données

### Tables concernées (issues des entités Symfony)

```sql
-- Table quiz
CREATE TABLE quiz (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    etat VARCHAR(50) NOT NULL  -- valeurs: 'actif', 'inactif', 'brouillon', 'archive'
);

-- Table question
CREATE TABLE question (
    id INT AUTO_INCREMENT PRIMARY KEY,
    texte_question TEXT NOT NULL,
    point INT NOT NULL,          -- entre 1 et 100
    quiz_id INT NOT NULL,
    FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE
);

-- Table option (attention: mot réservé SQL, utiliser backticks)
CREATE TABLE `option` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    texte_option VARCHAR(255) NOT NULL,
    est_correcte TINYINT(1) NOT NULL,  -- 0 ou 1
    question_id INT NOT NULL,
    FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE
);
```

---

## Architecture JavaFX à Créer

```
src/main/java/tn/esprit/
├── entities/
│   ├── Quiz.java
│   ├── Question.java
│   └── Option.java
├── services/
│   ├── IService.java          (interface générique)
│   ├── ServiceQuiz.java
│   ├── ServiceQuestion.java
│   └── ServiceOption.java
├── controllers/
│   ├── QuizController.java
│   ├── QuestionController.java
│   └── OptionController.java
└── tools/
    └── MyConnection.java

src/main/resources/com/quiz/
├── QuizView.fxml
├── QuestionView.fxml
├── OptionView.fxml
└── css/
    └── style.css
```

---

## Étape 1 : Entités Java

### Quiz.java

Correspond à `src/Entity/Quiz.php` dans Symfony.

Champs Symfony → Java :
- `$id` (int, auto) → `private int id`
- `$titre` (string 255, NotBlank, min 3, max 255) → `private String titre`
- `$description` (text, NotBlank, min 10, max 2000) → `private String description`
- `$etat` (string 50, Choice: actif/inactif/brouillon/archive) → `private String etat`

```java
package tn.esprit.entities;

public class Quiz {
    private int id;
    private String titre;
    private String description;
    private String etat;  // "actif", "inactif", "brouillon", "archive"

    public Quiz() {}

    public Quiz(String titre, String description, String etat) {
        this.titre = titre;
        this.description = description;
        this.etat = etat;
    }

    public Quiz(int id, String titre, String description, String etat) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.etat = etat;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    @Override
    public String toString() {
        return titre;  // utile pour les ComboBox
    }
}
```

### Question.java

Correspond à `src/Entity/Question.php` dans Symfony.

Champs Symfony → Java :
- `$texteQuestion` (text, min 10, max 1000) → `private String texteQuestion`
- `$point` (int, entre 1 et 100) → `private int point`
- `$quiz` (ManyToOne → quiz_id) → `private int quizId`

```java
package tn.esprit.entities;

public class Question {
    private int id;
    private String texteQuestion;
    private int point;
    private int quizId;

    public Question() {}

    public Question(String texteQuestion, int point, int quizId) {
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.quizId = quizId;
    }

    public Question(int id, String texteQuestion, int point, int quizId) {
        this.id = id;
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.quizId = quizId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTexteQuestion() { return texteQuestion; }
    public void setTexteQuestion(String texteQuestion) { this.texteQuestion = texteQuestion; }
    public int getPoint() { return point; }
    public void setPoint(int point) { this.point = point; }
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    @Override
    public String toString() {
        return texteQuestion;
    }
}
```

### Option.java

Correspond à `src/Entity/Option.php` dans Symfony.

Champs Symfony → Java :
- `$texteOption` (string 255) → `private String texteOption`
- `$estCorrecte` (bool) → `private boolean estCorrecte`
- `$question` (ManyToOne → question_id) → `private int questionId`

```java
package tn.esprit.entities;

public class Option {
    private int id;
    private String texteOption;
    private boolean estCorrecte;
    private int questionId;

    public Option() {}

    public Option(String texteOption, boolean estCorrecte, int questionId) {
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    public Option(int id, String texteOption, boolean estCorrecte, int questionId) {
        this.id = id;
        this.texteOption = texteOption;
        this.estCorrecte = estCorrecte;
        this.questionId = questionId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTexteOption() { return texteOption; }
    public void setTexteOption(String texteOption) { this.texteOption = texteOption; }
    public boolean isEstCorrecte() { return estCorrecte; }
    public void setEstCorrecte(boolean estCorrecte) { this.estCorrecte = estCorrecte; }
    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    @Override
    public String toString() {
        return texteOption + (estCorrecte ? " ✓" : "");
    }
}
```

---

## Étape 2 : Interface IService

Correspond au pattern Repository de Symfony. Chaque service implémente ces 4 méthodes CRUD.

```java
package tn.esprit.services;

import java.util.List;

public interface IService<T> {
    void ajouter(T t);
    void supprimer(T t);
    void modifier(T t);
    List<T> afficher();
}
```

---

## Étape 3 : ServiceQuiz.java

Correspond à ce que fait `QuizController.php` + `QuizRepository.php` dans Symfony.
Chaque méthode = une requête SQL directe via JDBC.

```java
package tn.esprit.services;

import tn.esprit.entities.Quiz;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuiz implements IService<Quiz> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    // CREATE → équivalent de entityManager.persist($quiz) dans Symfony
    @Override
    public void ajouter(Quiz quiz) {
        String req = "INSERT INTO quiz (titre, description, etat) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.executeUpdate();
            System.out.println("Quiz ajouté avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur ajout quiz : " + e.getMessage());
        }
    }

    // DELETE → équivalent de entityManager.remove($quiz) dans Symfony
    @Override
    public void supprimer(Quiz quiz) {
        String req = "DELETE FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, quiz.getId());
            statement.executeUpdate();
            System.out.println("Quiz supprimé avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression quiz : " + e.getMessage());
        }
    }

    // UPDATE → équivalent de entityManager.flush() après modification dans Symfony
    @Override
    public void modifier(Quiz quiz) {
        String req = "UPDATE quiz SET titre = ?, description = ?, etat = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setInt(4, quiz.getId());
            statement.executeUpdate();
            System.out.println("Quiz modifié avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur modification quiz : " + e.getMessage());
        }
    }

    // READ ALL → équivalent de quizRepository.findAll() dans Symfony
    @Override
    public List<Quiz> afficher() {
        List<Quiz> quizzes = new ArrayList<>();
        String req = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                quizzes.add(new Quiz(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("etat")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage quiz : " + e.getMessage());
        }
        return quizzes;
    }

    // READ ONE → équivalent de quizRepository.find($id) dans Symfony
    public Quiz findById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return new Quiz(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getString("etat")
                );
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById quiz : " + e.getMessage());
        }
        return null;
    }
}
```

---

## Étape 4 : ServiceQuestion.java

```java
package tn.esprit.services;

import tn.esprit.entities.Question;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceQuestion implements IService<Question> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Question question) {
        String req = "INSERT INTO question (texte_question, point, quiz_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            statement.setInt(3, question.getQuizId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur ajout question : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Question question) {
        String req = "DELETE FROM question WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, question.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression question : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Question question) {
        String req = "UPDATE question SET texte_question = ?, point = ?, quiz_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            statement.setInt(3, question.getQuizId());
            statement.setInt(4, question.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur modification question : " + e.getMessage());
        }
    }

    @Override
    public List<Question> afficher() {
        List<Question> questions = new ArrayList<>();
        String req = "SELECT * FROM question";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                questions.add(new Question(
                    rs.getInt("id"),
                    rs.getString("texte_question"),
                    rs.getInt("point"),
                    rs.getInt("quiz_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage questions : " + e.getMessage());
        }
        return questions;
    }

    // Récupérer les questions d'un quiz spécifique
    // Équivalent de quiz.getQuestions() dans Symfony (relation OneToMany)
    public List<Question> findByQuizId(int quizId) {
        List<Question> questions = new ArrayList<>();
        String req = "SELECT * FROM question WHERE quiz_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, quizId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                questions.add(new Question(
                    rs.getInt("id"),
                    rs.getString("texte_question"),
                    rs.getInt("point"),
                    rs.getInt("quiz_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByQuizId : " + e.getMessage());
        }
        return questions;
    }
}
```

---

## Étape 5 : ServiceOption.java

```java
package tn.esprit.services;

import tn.esprit.entities.Option;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceOption implements IService<Option> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Option option) {
        String req = "INSERT INTO `option` (texte_option, est_correcte, question_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte());
            statement.setInt(3, option.getQuestionId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur ajout option : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Option option) {
        String req = "DELETE FROM `option` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, option.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression option : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Option option) {
        String req = "UPDATE `option` SET texte_option = ?, est_correcte = ?, question_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte());
            statement.setInt(3, option.getQuestionId());
            statement.setInt(4, option.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur modification option : " + e.getMessage());
        }
    }

    @Override
    public List<Option> afficher() {
        List<Option> options = new ArrayList<>();
        String req = "SELECT * FROM `option`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                options.add(new Option(
                    rs.getInt("id"),
                    rs.getString("texte_option"),
                    rs.getBoolean("est_correcte"),
                    rs.getInt("question_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage options : " + e.getMessage());
        }
        return options;
    }

    // Équivalent de question.getOptions() dans Symfony
    public List<Option> findByQuestionId(int questionId) {
        List<Option> options = new ArrayList<>();
        String req = "SELECT * FROM `option` WHERE question_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, questionId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                options.add(new Option(
                    rs.getInt("id"),
                    rs.getString("texte_option"),
                    rs.getBoolean("est_correcte"),
                    rs.getInt("question_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByQuestionId : " + e.getMessage());
        }
        return options;
    }
}
```

---

## Étape 6 : Validations (équivalent des @Assert Symfony)

Dans Symfony, les validations sont dans les entités avec des annotations `@Assert`.
Dans JavaFX, on les fait manuellement dans le Controller avant d'appeler le service.

### Règles de validation à reproduire :

**Quiz :**
- `titre` : non vide, entre 3 et 255 caractères
- `description` : non vide, entre 10 et 2000 caractères
- `etat` : doit être parmi `actif`, `inactif`, `brouillon`, `archive`

**Question :**
- `texteQuestion` : non vide, entre 10 et 1000 caractères
- `point` : entier, entre 1 et 100
- `quizId` : doit exister (non nul)

**Option :**
- `texteOption` : non vide, max 255 caractères
- `estCorrecte` : boolean (checkbox)
- `questionId` : doit exister (non nul)

### Exemple de validation dans le Controller JavaFX :

```java
private String validerQuiz(String titre, String description, String etat) {
    if (titre == null || titre.trim().isEmpty())
        return "Le titre est obligatoire.";
    if (titre.length() < 3 || titre.length() > 255)
        return "Le titre doit contenir entre 3 et 255 caractères.";
    if (description == null || description.trim().isEmpty())
        return "La description est obligatoire.";
    if (description.length() < 10 || description.length() > 2000)
        return "La description doit contenir entre 10 et 2000 caractères.";
    List<String> etatsValides = List.of("actif", "inactif", "brouillon", "archive");
    if (!etatsValides.contains(etat))
        return "L'état doit être: actif, inactif, brouillon ou archive.";
    return null; // null = pas d'erreur
}
```

---

## Étape 7 : QuizController.java (JavaFX)

Correspond à `QuizController.php` dans Symfony mais gère l'UI JavaFX.

```java
package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

public class QuizController {

    // Composants FXML (équivalent des champs de formulaire Twig)
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private TableView<Quiz> quizTable;
    @FXML private TableColumn<Quiz, Integer> idCol;
    @FXML private TableColumn<Quiz, String> titreCol;
    @FXML private TableColumn<Quiz, String> descriptionCol;
    @FXML private TableColumn<Quiz, String> etatCol;
    @FXML private Label messageLabel;

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Quiz quizSelectionne = null;

    @FXML
    public void initialize() {
        // Initialiser les colonnes du tableau
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titreCol.setCellValueFactory(new PropertyValueFactory<>("titre"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        etatCol.setCellValueFactory(new PropertyValueFactory<>("etat"));

        // Remplir le ComboBox des états (équivalent ChoiceType dans QuizType.php)
        etatCombo.setItems(FXCollections.observableArrayList(
            "actif", "inactif", "brouillon", "archive"
        ));

        // Charger les données
        chargerQuizzes();

        // Sélection dans le tableau → remplir le formulaire
        quizTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    quizSelectionne = newVal;
                    titreField.setText(newVal.getTitre());
                    descriptionField.setText(newVal.getDescription());
                    etatCombo.setValue(newVal.getEtat());
                }
            }
        );
    }

    // Équivalent de QuizController::new() dans Symfony
    @FXML
    public void ajouterQuiz() {
        String erreur = validerQuiz(
            titreField.getText(),
            descriptionField.getText(),
            etatCombo.getValue()
        );
        if (erreur != null) {
            messageLabel.setText(erreur);
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        Quiz quiz = new Quiz(
            titreField.getText().trim(),
            descriptionField.getText().trim(),
            etatCombo.getValue()
        );
        serviceQuiz.ajouter(quiz);
        messageLabel.setText("Quiz ajouté avec succès !");
        messageLabel.setStyle("-fx-text-fill: green;");
        viderFormulaire();
        chargerQuizzes();
    }

    // Équivalent de QuizController::edit() dans Symfony
    @FXML
    public void modifierQuiz() {
        if (quizSelectionne == null) {
            messageLabel.setText("Sélectionnez un quiz à modifier.");
            return;
        }
        String erreur = validerQuiz(
            titreField.getText(),
            descriptionField.getText(),
            etatCombo.getValue()
        );
        if (erreur != null) {
            messageLabel.setText(erreur);
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        quizSelectionne.setTitre(titreField.getText().trim());
        quizSelectionne.setDescription(descriptionField.getText().trim());
        quizSelectionne.setEtat(etatCombo.getValue());
        serviceQuiz.modifier(quizSelectionne);
        messageLabel.setText("Quiz modifié avec succès !");
        messageLabel.setStyle("-fx-text-fill: green;");
        viderFormulaire();
        chargerQuizzes();
    }

    // Équivalent de QuizController::delete() dans Symfony
    @FXML
    public void supprimerQuiz() {
        if (quizSelectionne == null) {
            messageLabel.setText("Sélectionnez un quiz à supprimer.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Êtes-vous sûr de vouloir supprimer ce quiz ?",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                serviceQuiz.supprimer(quizSelectionne);
                messageLabel.setText("Quiz supprimé.");
                viderFormulaire();
                chargerQuizzes();
            }
        });
    }

    // Équivalent de QuizController::index() dans Symfony
    private void chargerQuizzes() {
        List<Quiz> quizzes = serviceQuiz.afficher();
        ObservableList<Quiz> data = FXCollections.observableArrayList(quizzes);
        quizTable.setItems(data);
        quizSelectionne = null;
    }

    private void viderFormulaire() {
        titreField.clear();
        descriptionField.clear();
        etatCombo.setValue(null);
        quizSelectionne = null;
        quizTable.getSelectionModel().clearSelection();
    }

    private String validerQuiz(String titre, String description, String etat) {
        if (titre == null || titre.trim().isEmpty()) return "Le titre est obligatoire.";
        if (titre.length() < 3 || titre.length() > 255) return "Le titre doit contenir entre 3 et 255 caractères.";
        if (description == null || description.trim().isEmpty()) return "La description est obligatoire.";
        if (description.length() < 10 || description.length() > 2000) return "La description doit contenir entre 10 et 2000 caractères.";
        if (etat == null) return "L'état est obligatoire.";
        return null;
    }
}
```

---

## Étape 8 : QuizView.fxml

Correspond aux templates Twig `quiz/index.html.twig`, `quiz/new.html.twig`, `quiz/edit.html.twig` dans Symfony.
Tout est regroupé dans un seul fichier FXML (liste + formulaire sur la même vue).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="tn.esprit.controllers.QuizController"
            prefWidth="900" prefHeight="600">

    <!-- HAUT : Titre (équivalent navbar backoffice) -->
    <top>
        <Label text="Gestion des Quiz" style="-fx-font-size: 20px; -fx-font-weight: bold; -fx-padding: 15px;"/>
    </top>

    <!-- GAUCHE : Formulaire (équivalent quiz/_form.html.twig) -->
    <left>
        <VBox spacing="10" style="-fx-padding: 15px; -fx-min-width: 300px;">
            <Label text="Titre :"/>
            <TextField fx:id="titreField" promptText="Entrez le titre du quiz"/>

            <Label text="Description :"/>
            <TextArea fx:id="descriptionField" promptText="Décrivez le contenu du quiz"
                      prefRowCount="5"/>

            <Label text="État :"/>
            <!-- Équivalent ChoiceType dans QuizType.php -->
            <ComboBox fx:id="etatCombo" promptText="Sélectionnez un état" maxWidth="Infinity"/>

            <!-- Boutons CRUD (équivalent boutons Twig) -->
            <HBox spacing="10">
                <Button text="Ajouter" onAction="#ajouterQuiz"
                        style="-fx-background-color: #10b981; -fx-text-fill: white;"/>
                <Button text="Modifier" onAction="#modifierQuiz"
                        style="-fx-background-color: #f59e0b; -fx-text-fill: white;"/>
                <Button text="Supprimer" onAction="#supprimerQuiz"
                        style="-fx-background-color: #ef4444; -fx-text-fill: white;"/>
            </HBox>

            <!-- Message d'erreur/succès (équivalent flash messages Symfony) -->
            <Label fx:id="messageLabel" wrapText="true"/>
        </VBox>
    </left>

    <!-- CENTRE : Tableau (équivalent quiz/index.html.twig) -->
    <center>
        <TableView fx:id="quizTable" style="-fx-padding: 15px;">
            <columns>
                <TableColumn fx:id="idCol" text="ID" prefWidth="50"/>
                <TableColumn fx:id="titreCol" text="Titre" prefWidth="200"/>
                <TableColumn fx:id="descriptionCol" text="Description" prefWidth="300"/>
                <TableColumn fx:id="etatCol" text="État" prefWidth="100"/>
            </columns>
        </TableView>
    </center>

</BorderPane>
```

---

## Correspondance Symfony ↔ JavaFX (Résumé)

| Symfony | JavaFX |
|---------|--------|
| `src/Entity/Quiz.php` | `entities/Quiz.java` |
| `src/Entity/Question.php` | `entities/Question.java` |
| `src/Entity/Option.php` | `entities/Option.java` |
| `QuizRepository::findAll()` | `ServiceQuiz::afficher()` |
| `QuizRepository::find($id)` | `ServiceQuiz::findById(id)` |
| `entityManager->persist($quiz)` | `ServiceQuiz::ajouter(quiz)` |
| `entityManager->flush()` | `ServiceQuiz::modifier(quiz)` |
| `entityManager->remove($quiz)` | `ServiceQuiz::supprimer(quiz)` |
| `quiz.getQuestions()` (relation) | `ServiceQuestion::findByQuizId(quizId)` |
| `question.getOptions()` (relation) | `ServiceOption::findByQuestionId(questionId)` |
| `QuizType.php` (formulaire) | Champs FXML + ComboBox dans QuizView.fxml |
| `@Assert\NotBlank` | Validation manuelle dans le Controller |
| `@Assert\Length(min=3, max=255)` | `if (titre.length() < 3 \|\| titre.length() > 255)` |
| `@Assert\Choice(choices=[...])` | `List.of(...).contains(etat)` |
| Flash messages Symfony | `messageLabel.setText(...)` |
| Confirmation suppression JS | `Alert(AlertType.CONFIRMATION)` |

---

## Points Importants à Ne Pas Oublier

1. **La table `option` est un mot réservé SQL** → toujours utiliser des backticks : `` `option` ``

2. **MyConnection** doit être un Singleton (pattern déjà utilisé dans le projet) :
   ```java
   private final Connection connection = MyConnection.getInstance().getConnection();
   ```

3. **Les relations OneToMany** de Symfony (quiz → questions → options) sont gérées par des méthodes `findByXxxId()` dans les services JavaFX.

4. **L'ordre de suppression** : supprimer d'abord les options, puis les questions, puis le quiz (à cause des foreign keys). Ou utiliser `ON DELETE CASCADE` dans la BDD.

5. **Le ComboBox des états** doit contenir exactement : `actif`, `inactif`, `brouillon`, `archive` (mêmes valeurs que dans `QuizType.php`).

6. **Pour charger le FXML** dans MainApp.java :
   ```java
   FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/quiz/QuizView.fxml"));
   Parent root = loader.load();
   ```

---

## Étape 9 : Charte Graphique — CSS JavaFX (style.css)

Le backoffice Symfony utilise un style **Glassmorphism** (effet verre sombre).
Voici comment reproduire exactement les mêmes couleurs et styles dans JavaFX via un fichier CSS.

### Fichier : `src/main/resources/com/quiz/css/style.css`

```css
/* ============================================
   CHARTE GRAPHIQUE - QUIZ JAVAFX
   Basée sur le backoffice Symfony Glassmorphism
============================================ */

/* ============================================
   COULEURS PRINCIPALES (variables Symfony → JavaFX)
   --emerald:        #059669
   --emerald-light:  #34d399
   --gold:           #d4a574
   --gold-light:     #e8c9a0
   --amber:          #b45309
   --coral:          #e07a5f
   --bg-dark:        #0a0f0d
   --text-primary:   #f5f5f4
   --text-secondary: rgba(245,245,244,0.7)
   --glass-bg:       rgba(255,255,255,0.05)
   --glass-border:   rgba(255,255,255,0.1)
   --success:        #22c55e
   --warning:        #eab308
   --danger:         #dc2626
============================================ */

/* FOND PRINCIPAL */
.root {
    -fx-background-color: #0a0f0d;
    -fx-font-family: "Segoe UI", Arial, sans-serif;
    -fx-font-size: 14px;
}

/* ============================================
   GLASS CARD (équivalent .glass-card CSS)
============================================ */
.glass-card {
    -fx-background-color: rgba(255, 255, 255, 0.05);
    -fx-border-color: rgba(255, 255, 255, 0.1);
    -fx-border-radius: 20px;
    -fx-background-radius: 20px;
    -fx-border-width: 1px;
    -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 25, 0, 0, 8);
    -fx-padding: 24px;
}

/* ============================================
   TITRE DE PAGE (équivalent .page-title)
============================================ */
.page-title {
    -fx-font-size: 24px;
    -fx-font-weight: bold;
    -fx-text-fill: #f5f5f4;
    -fx-padding: 0 0 16px 0;
}

/* ============================================
   CARD TITLE (équivalent .card-title)
============================================ */
.card-title {
    -fx-font-size: 18px;
    -fx-font-weight: bold;
    -fx-text-fill: #f5f5f4;
}

.card-subtitle {
    -fx-font-size: 13px;
    -fx-text-fill: rgba(245, 245, 244, 0.4);
}

/* ============================================
   LABELS DE FORMULAIRE (équivalent label dans custom-forms.css)
============================================ */
.form-label {
    -fx-font-size: 13px;
    -fx-font-weight: bold;
    -fx-text-fill: #f5f5f4;
}

/* ============================================
   INPUTS (équivalent input[type="text"] dans custom-forms.css)
============================================ */
.text-field, .text-area {
    -fx-background-color: rgba(255, 255, 255, 0.05);
    -fx-border-color: rgba(255, 255, 255, 0.1);
    -fx-border-radius: 8px;
    -fx-background-radius: 8px;
    -fx-border-width: 1px;
    -fx-text-fill: #f5f5f4;
    -fx-prompt-text-fill: rgba(245, 245, 244, 0.4);
    -fx-padding: 10px 14px;
    -fx-font-size: 14px;
}

.text-field:focused, .text-area:focused {
    -fx-border-color: #34d399;
    -fx-background-color: rgba(255, 255, 255, 0.08);
    -fx-effect: dropshadow(gaussian, rgba(52, 211, 153, 0.2), 10, 0, 0, 0);
}

/* INPUT INVALIDE (équivalent .is-invalid) */
.text-field-error, .text-area-error {
    -fx-border-color: rgba(239, 68, 68, 0.6);
    -fx-background-color: rgba(239, 68, 68, 0.05);
}

/* ============================================
   COMBOBOX (équivalent select dans custom-forms.css)
============================================ */
.combo-box {
    -fx-background-color: rgba(255, 255, 255, 0.05);
    -fx-border-color: rgba(255, 255, 255, 0.1);
    -fx-border-radius: 8px;
    -fx-background-radius: 8px;
    -fx-border-width: 1px;
    -fx-text-fill: #f5f5f4;
    -fx-padding: 4px;
}

.combo-box:focused {
    -fx-border-color: #34d399;
}

.combo-box .list-cell {
    -fx-background-color: #0d1a14;
    -fx-text-fill: #f5f5f4;
    -fx-padding: 8px 14px;
}

.combo-box-popup .list-view {
    -fx-background-color: #0d1a14;
    -fx-border-color: rgba(255, 255, 255, 0.1);
}

.combo-box-popup .list-cell:hover {
    -fx-background-color: rgba(255, 255, 255, 0.08);
}

/* ============================================
   BOUTON AJOUTER - VERT (équivalent bouton Nouveau avec --emerald)
============================================ */
.btn-add {
    -fx-background-color: linear-gradient(to bottom right, #34d399, #059669);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
    -fx-padding: 10px 20px;
    -fx-background-radius: 8px;
    -fx-border-radius: 8px;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(5, 150, 105, 0.4), 12, 0, 0, 4);
}

.btn-add:hover {
    -fx-background-color: linear-gradient(to bottom right, #6ee7b7, #34d399);
    -fx-effect: dropshadow(gaussian, rgba(52, 211, 153, 0.5), 16, 0, 0, 6);
}

/* ============================================
   BOUTON MODIFIER - OR (équivalent bouton Modifier avec --gold)
============================================ */
.btn-edit {
    -fx-background-color: linear-gradient(to bottom right, #e8c9a0, #d4a574);
    -fx-text-fill: #0a0f0d;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
    -fx-padding: 10px 20px;
    -fx-background-radius: 8px;
    -fx-border-radius: 8px;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(212, 165, 116, 0.4), 12, 0, 0, 4);
}

.btn-edit:hover {
    -fx-background-color: linear-gradient(to bottom right, #fde8c8, #e8c9a0);
    -fx-effect: dropshadow(gaussian, rgba(212, 165, 116, 0.5), 16, 0, 0, 6);
}

/* ============================================
   BOUTON SUPPRIMER - ROUGE (équivalent bouton Supprimer #ef4444)
============================================ */
.btn-delete {
    -fx-background-color: linear-gradient(to bottom right, #f87171, #dc2626);
    -fx-text-fill: white;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
    -fx-padding: 10px 20px;
    -fx-background-radius: 8px;
    -fx-border-radius: 8px;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(220, 38, 38, 0.4), 12, 0, 0, 4);
}

.btn-delete:hover {
    -fx-background-color: linear-gradient(to bottom right, #fca5a5, #f87171);
    -fx-effect: dropshadow(gaussian, rgba(239, 68, 68, 0.5), 16, 0, 0, 6);
}

/* ============================================
   BOUTON NEUTRE (équivalent .card-btn)
============================================ */
.btn-neutral {
    -fx-background-color: rgba(255, 255, 255, 0.05);
    -fx-border-color: rgba(255, 255, 255, 0.1);
    -fx-border-width: 1px;
    -fx-border-radius: 8px;
    -fx-background-radius: 8px;
    -fx-text-fill: rgba(245, 245, 244, 0.7);
    -fx-font-size: 13px;
    -fx-padding: 10px 20px;
    -fx-cursor: hand;
}

.btn-neutral:hover {
    -fx-background-color: rgba(255, 255, 255, 0.08);
    -fx-border-color: #34d399;
    -fx-text-fill: #f5f5f4;
}

/* ============================================
   TABLEAU (équivalent .data-table)
============================================ */
.table-view {
    -fx-background-color: transparent;
    -fx-border-color: transparent;
    -fx-table-cell-border-color: rgba(255, 255, 255, 0.03);
}

.table-view .column-header-background {
    -fx-background-color: transparent;
}

.table-view .column-header {
    -fx-background-color: transparent;
    -fx-border-color: transparent transparent rgba(255,255,255,0.1) transparent;
    -fx-border-width: 0 0 1px 0;
}

.table-view .column-header .label {
    -fx-text-fill: rgba(245, 245, 244, 0.4);
    -fx-font-size: 11px;
    -fx-font-weight: bold;
}

.table-view .table-row-cell {
    -fx-background-color: transparent;
    -fx-border-color: transparent transparent rgba(255,255,255,0.03) transparent;
    -fx-border-width: 0 0 1px 0;
    -fx-text-fill: rgba(245, 245, 244, 0.7);
}

.table-view .table-row-cell:hover {
    -fx-background-color: rgba(255, 255, 255, 0.08);
}

.table-view .table-row-cell:selected {
    -fx-background-color: rgba(52, 211, 153, 0.15);
    -fx-text-fill: #f5f5f4;
}

.table-view .table-cell {
    -fx-text-fill: rgba(245, 245, 244, 0.7);
    -fx-padding: 12px 16px;
    -fx-font-size: 14px;
}

/* ============================================
   MESSAGES (succès / erreur)
   Équivalent flash messages Symfony
============================================ */
.msg-success {
    -fx-text-fill: #22c55e;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
}

.msg-error {
    -fx-text-fill: #fca5a5;
    -fx-font-size: 13px;
    -fx-font-weight: bold;
}

/* ============================================
   BADGE ÉTAT (équivalent .status-badge)
============================================ */
/* actif → completed (vert) */
.badge-actif {
    -fx-background-color: rgba(16, 185, 129, 0.15);
    -fx-text-fill: #22c55e;
    -fx-background-radius: 20px;
    -fx-padding: 4px 12px;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
}

/* inactif → pending (orange) */
.badge-inactif {
    -fx-background-color: rgba(245, 158, 11, 0.15);
    -fx-text-fill: #eab308;
    -fx-background-radius: 20px;
    -fx-padding: 4px 12px;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
}

/* brouillon → processing (bleu) */
.badge-brouillon {
    -fx-background-color: rgba(59, 130, 246, 0.15);
    -fx-text-fill: #0ea5e9;
    -fx-background-radius: 20px;
    -fx-padding: 4px 12px;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
}

/* archive → gris */
.badge-archive {
    -fx-background-color: rgba(71, 85, 105, 0.3);
    -fx-text-fill: rgba(245, 245, 244, 0.5);
    -fx-background-radius: 20px;
    -fx-padding: 4px 12px;
    -fx-font-size: 12px;
    -fx-font-weight: bold;
}

/* ============================================
   SCROLLBAR (équivalent scrollbar CSS backoffice)
============================================ */
.scroll-bar:vertical .thumb,
.scroll-bar:horizontal .thumb {
    -fx-background-color: linear-gradient(to bottom, #34d399, #d4a574);
    -fx-background-radius: 4px;
}

.scroll-bar .track {
    -fx-background-color: rgba(255, 255, 255, 0.05);
    -fx-background-radius: 4px;
}

/* ============================================
   SEPARATOR
============================================ */
.separator .line {
    -fx-border-color: rgba(255, 255, 255, 0.1);
    -fx-border-width: 1px;
}
```

---

## Comment appliquer le CSS dans JavaFX

### Dans le Controller (initialize) :

```java
@FXML
public void initialize() {
    // Appliquer les styles CSS aux boutons
    btnAjouter.getStyleClass().add("btn-add");
    btnModifier.getStyleClass().add("btn-edit");
    btnSupprimer.getStyleClass().add("btn-delete");

    // Appliquer le style glass-card au panneau principal
    mainPane.getStyleClass().add("glass-card");

    // Appliquer les styles aux champs
    titreField.getStyleClass().add("text-field");
    descriptionField.getStyleClass().add("text-area");
    etatCombo.getStyleClass().add("combo-box");

    // Appliquer le style au tableau
    quizTable.getStyleClass().add("table-view");
}
```

### Dans le FXML (méthode directe) :

```xml
<Button text="Ajouter" styleClass="btn-add" onAction="#ajouterQuiz"/>
<Button text="Modifier" styleClass="btn-edit" onAction="#modifierQuiz"/>
<Button text="Supprimer" styleClass="btn-delete" onAction="#supprimerQuiz"/>
<TextField fx:id="titreField" styleClass="text-field" promptText="Titre du quiz"/>
<TableView fx:id="quizTable" styleClass="table-view"/>
```

### Charger le CSS dans MainApp.java :

```java
Scene scene = new Scene(root, 900, 600);
scene.getStylesheets().add(
    getClass().getResource("/com/quiz/css/style.css").toExternalForm()
);
// Fond sombre obligatoire
scene.setFill(javafx.scene.paint.Color.web("#0a0f0d"));
primaryStage.setScene(scene);
```

---

## Résumé Charte Graphique

| Élément | Couleur Symfony | Classe JavaFX CSS |
|---------|----------------|-------------------|
| Fond principal | `#0a0f0d` | `.root` |
| Carte verre | `rgba(255,255,255,0.05)` | `.glass-card` |
| Bouton Ajouter | `#34d399 → #059669` | `.btn-add` |
| Bouton Modifier | `#e8c9a0 → #d4a574` | `.btn-edit` |
| Bouton Supprimer | `#f87171 → #dc2626` | `.btn-delete` |
| Texte principal | `#f5f5f4` | `-fx-text-fill` |
| Badge actif | vert `#22c55e` | `.badge-actif` |
| Badge inactif | orange `#eab308` | `.badge-inactif` |
| Badge brouillon | bleu `#0ea5e9` | `.badge-brouillon` |
| Badge archive | gris | `.badge-archive` |
| Input focus | `#34d399` (emerald-light) | `.text-field:focused` |
| Tableau sélection | `rgba(52,211,153,0.15)` | `.table-row-cell:selected` |

---

## Étape 10 : Interface Exacte — Structure Visuelle (d'après la capture d'écran)

L'interface que tu dois reproduire dans JavaFX ressemble à ceci :

```
┌─────────────────────────────────────────────────────────────────┐
│  SIDEBAR (gauche)          │  CONTENU PRINCIPAL (droite)        │
│                            │                                     │
│  G  GlassDash              │  Dashboard                          │
│                            │  ─────────────────────────────────  │
│  MAIN MENU                 │  Gestion des Quiz                   │
│  □ Dashboard               │                                     │
│  □ Analytics               │  ┌─────────────────────────────┐   │
│                            │  │ 📋 Liste des Quiz  [+Nouveau]│   │
│  GESTION                   │  │ Cliquez sur "Sélectionner"   │   │
│  □ Gestion Quiz ← actif    │  │ pour afficher les questions  │   │
│                            │  ├─────────────────────────────┤   │
│  SYSTÈME                   │  │ #1  react  [Actif]           │   │
│  □ Users                   │  │ hjhhhhhhhhhhhh...            │   │
│  □ Settings                │  │ [Sélectionner][Voir]         │   │
│                            │  │ [Modifier][Supprimer]        │   │
│  ACCOUNT                   │  ├─────────────────────────────┤   │
│  □ Logout                  │  │ #2  Quiz-Loops...  [Actif]   │   │
│                            │  │ Quiz généré automatiquement  │   │
│                            │  │ [Sélectionner][Voir]         │   │
│                            │  │ [Modifier][Supprimer]        │   │
│                            │  └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Comportement "Sélectionner" (hiérarchique) :

Quand on clique sur **Sélectionner** d'un quiz → les questions apparaissent en dessous.
Quand on clique sur **Sélectionner** d'une question → les options apparaissent en dessous.

```
Quiz #1 react [Actif]
  └── [Sélectionner cliqué]
      ├── Question #1 : "Qu'est-ce que React ?" (5 pts)
      │     └── [Sélectionner cliqué]
      │         ├── Option #1 : "Une bibliothèque JS" ✓ Correcte
      │         ├── Option #2 : "Un framework CSS"   ✗ Incorrecte
      │         └── [+ Nouvelle Option]
      ├── Question #2 : "Qu'est-ce que JSX ?" (3 pts)
      └── [+ Nouvelle Question]
```

---

## Étape 11 : QuizView.fxml — Interface Complète et Exacte

Voici le FXML qui reproduit exactement l'interface de la capture d'écran :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.geometry.Insets?>

<BorderPane xmlns:fx="http://javafx.c

---

## Étape 10 : QuestionController.java (JavaFX)

Correspond à `QuestionController.php` dans Symfony.
La question est liée à un quiz → le ComboBox charge la liste des quiz.

```java
package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

public class QuestionController {

    @FXML private TextArea texteQuestionField;
    @FXML private TextField pointField;
    @FXML private ComboBox<Quiz> quizCombo;   // équivalent EntityType Quiz dans QuestionType.php
    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, Integer> idCol;
    @FXML private TableColumn<Question, String> texteCol;
    @FXML private TableColumn<Question, Integer> pointCol;
    @FXML private TableColumn<Question, Integer> quizIdCol;
    @FXML private Label messageLabel;

    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Question questionSelectionnee = null;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        texteCol.setCellValueFactory(new PropertyValueFactory<>("texteQuestion"));
        pointCol.setCellValueFactory(new PropertyValueFactory<>("point"));
        quizIdCol.setCellValueFactory(new PropertyValueFactory<>("quizId"));

        // Charger les quiz dans le ComboBox (équivalent EntityType dans QuestionType.php)
        quizCombo.setItems(FXCollections.observableArrayList(serviceQuiz.afficher()));

        chargerQuestions();

        questionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    questionSelectionnee = newVal;
                    texteQuestionField.setText(newVal.getTexteQuestion());
                    pointField.setText(String.valueOf(newVal.getPoint()));
                    // Sélectionner le quiz correspondant dans le ComboBox
                    quizCombo.getItems().stream()
                        .filter(q -> q.getId() == newVal.getQuizId())
                        .findFirst()
                        .ifPresent(quizCombo::setValue);
                }
            }
        );
    }

    @FXML
    public void ajouterQuestion() {
        String erreur = validerQuestion();
        if (erreur != null) {
            afficherErreur(erreur);
            return;
        }
        Question q = new Question(
            texteQuestionField.getText().trim(),
            Integer.parseInt(pointField.getText().trim()),
            quizCombo.getValue().getId()
        );
        serviceQuestion.ajouter(q);
        afficherSucces("Question ajoutée avec succès !");
        viderFormulaire();
        chargerQuestions();
    }

    @FXML
    public void modifierQuestion() {
        if (questionSelectionnee == null) { afficherErreur("Sélectionnez une question."); return; }
        String erreur = validerQuestion();
        if (erreur != null) { afficherErreur(erreur); return; }
        questionSelectionnee.setTexteQuestion(texteQuestionField.getText().trim());
        questionSelectionnee.setPoint(Integer.parseInt(pointField.getText().trim()));
        questionSelectionnee.setQuizId(quizCombo.getValue().getId());
        serviceQuestion.modifier(questionSelectionnee);
        afficherSucces("Question modifiée avec succès !");
        viderFormulaire();
        chargerQuestions();
    }

    @FXML
    public void supprimerQuestion() {
        if (questionSelectionnee == null) { afficherErreur("Sélectionnez une question."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Êtes-vous sûr de vouloir supprimer cette question ?",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceQuestion.supprimer(questionSelectionnee);
                afficherSucces("Question supprimée.");
                viderFormulaire();
                chargerQuestions();
            }
        });
    }

    private void chargerQuestions() {
        questionTable.setItems(FXCollections.observableArrayList(serviceQuestion.afficher()));
        questionSelectionnee = null;
    }

    private void viderFormulaire() {
        texteQuestionField.clear();
        pointField.clear();
        quizCombo.setValue(null);
        questionSelectionnee = null;
        questionTable.getSelectionModel().clearSelection();
    }

    private String validerQuestion() {
        String texte = texteQuestionField.getText();
        String pointStr = pointField.getText();
        if (texte == null || texte.trim().isEmpty()) return "Le texte de la question est obligatoire.";
        if (texte.length() < 10 || texte.length() > 1000) return "La question doit contenir entre 10 et 1000 caractères.";
        if (pointStr == null || pointStr.trim().isEmpty()) return "Le nombre de points est obligatoire.";
        try {
            int point = Integer.parseInt(pointStr.trim());
            if (point < 1 || point > 100) return "Les points doivent être entre 1 et 100.";
        } catch (NumberFormatException e) {
            return "Le nombre de points doit être un entier.";
        }
        if (quizCombo.getValue() == null) return "Sélectionnez un quiz.";
        return null;
    }

    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("msg-success");
        messageLabel.getStyleClass().add("msg-error");
    }

    private void afficherSucces(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("msg-error");
        messageLabel.getStyleClass().add("msg-success");
    }
}
```

---

## Étape 11 : OptionController.java (JavaFX)

Correspond à `OptionController.php` dans Symfony.
L'option est liée à une question → ComboBox charge la liste des questions.

```java
package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;

public class OptionController {

    @FXML private TextField texteOptionField;
    @FXML private CheckBox estCorrecteCheck;    // équivalent CheckboxType dans OptionType.php
    @FXML private ComboBox<Question> questionCombo; // équivalent EntityType Question dans OptionType.php
    @FXML private TableView<Option> optionTable;
    @FXML private TableColumn<Option, Integer> idCol;
    @FXML private TableColumn<Option, String> texteCol;
    @FXML private TableColumn<Option, Boolean> correcteCol;
    @FXML private TableColumn<Option, Integer> questionIdCol;
    @FXML private Label messageLabel;

    private final ServiceOption serviceOption = new ServiceOption();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private Option optionSelectionnee = null;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        texteCol.setCellValueFactory(new PropertyValueFactory<>("texteOption"));
        correcteCol.setCellValueFactory(new PropertyValueFactory<>("estCorrecte"));
        questionIdCol.setCellValueFactory(new PropertyValueFactory<>("questionId"));

        // Charger les questions dans le ComboBox (équivalent EntityType dans OptionType.php)
        questionCombo.setItems(FXCollections.observableArrayList(serviceQuestion.afficher()));

        chargerOptions();

        optionTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    optionSelectionnee = newVal;
                    texteOptionField.setText(newVal.getTexteOption());
                    estCorrecteCheck.setSelected(newVal.isEstCorrecte());
                    questionCombo.getItems().stream()
                        .filter(q -> q.getId() == newVal.getQuestionId())
                        .findFirst()
                        .ifPresent(questionCombo::setValue);
                }
            }
        );
    }

    @FXML
    public void ajouterOption() {
        String erreur = validerOption();
        if (erreur != null) { afficherErreur(erreur); return; }
        Option o = new Option(
            texteOptionField.getText().trim(),
            estCorrecteCheck.isSelected(),
            questionCombo.getValue().getId()
        );
        serviceOption.ajouter(o);
        afficherSucces("Option ajoutée avec succès !");
        viderFormulaire();
        chargerOptions();
    }

    @FXML
    public void modifierOption() {
        if (optionSelectionnee == null) { afficherErreur("Sélectionnez une option."); return; }
        String erreur = validerOption();
        if (erreur != null) { afficherErreur(erreur); return; }
        optionSelectionnee.setTexteOption(texteOptionField.getText().trim());
        optionSelectionnee.setEstCorrecte(estCorrecteCheck.isSelected());
        optionSelectionnee.setQuestionId(questionCombo.getValue().getId());
        serviceOption.modifier(optionSelectionnee);
        afficherSucces("Option modifiée avec succès !");
        viderFormulaire();
        chargerOptions();
    }

    @FXML
    public void supprimerOption() {
        if (optionSelectionnee == null) { afficherErreur("Sélectionnez une option."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Êtes-vous sûr de vouloir supprimer cette option ?",
            ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                serviceOption.supprimer(optionSelectionnee);
                afficherSucces("Option supprimée.");
                viderFormulaire();
                chargerOptions();
            }
        });
    }

    private void chargerOptions() {
        optionTable.setItems(FXCollections.observableArrayList(serviceOption.afficher()));
        optionSelectionnee = null;
    }

    private void viderFormulaire() {
        texteOptionField.clear();
        estCorrecteCheck.setSelected(false);
        questionCombo.setValue(null);
        optionSelectionnee = null;
        optionTable.getSelectionModel().clearSelection();
    }

    private String validerOption() {
        String texte = texteOptionField.getText();
        if (texte == null || texte.trim().isEmpty()) return "Le texte de l'option est obligatoire.";
        if (texte.length() > 255) return "L'option ne peut pas dépasser 255 caractères.";
        if (questionCombo.getValue() == null) return "Sélectionnez une question.";
        return null;
    }

    private void afficherErreur(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("msg-success");
        messageLabel.getStyleClass().add("msg-error");
    }

    private void afficherSucces(String msg) {
        messageLabel.setText(msg);
        messageLabel.getStyleClass().removeAll("msg-error");
        messageLabel.getStyleClass().add("msg-success");
    }
}
```

---

## Étape 12 : QuestionView.fxml

Correspond aux templates `question/index.html.twig` + `question/_form.html.twig`.
Bouton "Nouveau Question" en couleur OR (comme dans le Twig : `var(--gold), var(--amber)`).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="tn.esprit.controllers.QuestionController"
            prefWidth="950" prefHeight="620"
            styleClass="root">

    <top>
        <Label text="Gestion des Questions" styleClass="page-title"
               style="-fx-padding: 15px 20px;"/>
    </top>

    <left>
        <VBox spacing="12" styleClass="glass-card"
              style="-fx-padding: 20px; -fx-min-width: 320px; -fx-max-width: 320px;">

            <Label text="Texte de la question :" styleClass="form-label"/>
            <!-- Équivalent TextareaType dans QuestionType.php (min 10, max 1000 chars) -->
            <TextArea fx:id="texteQuestionField"
                      promptText="Entrez votre question (min 10 caractères)"
                      prefRowCount="5" styleClass="text-area"/>

            <Label text="Points :" styleClass="form-label"/>
            <!-- Équivalent IntegerType dans QuestionType.php (entre 1 et 100) -->
            <TextField fx:id="pointField"
                       promptText="Entre 1 et 100"
                       styleClass="text-field"/>

            <Label text="Quiz associé :" styleClass="form-label"/>
            <!-- Équivalent EntityType Quiz dans QuestionType.php -->
            <ComboBox fx:id="quizCombo"
                      promptText="Sélectionnez un quiz"
                      maxWidth="Infinity" styleClass="combo-box"/>

            <HBox spacing="8">
                <!-- Bouton OR (couleur gold comme dans question/_form.html.twig) -->
                <Button text="Ajouter" onAction="#ajouterQuestion" styleClass="btn-edit"/>
                <Button text="Modifier" onAction="#modifierQuestion" styleClass="btn-neutral"/>
                <Button text="Supprimer" onAction="#supprimerQuestion" styleClass="btn-delete"/>
            </HBox>

            <Label fx:id="messageLabel" wrapText="true"/>
        </VBox>
    </left>

    <center>
        <TableView fx:id="questionTable" styleClass="table-view"
                   style="-fx-padding: 15px;">
            <columns>
                <TableColumn fx:id="idCol" text="ID" prefWidth="50"/>
                <TableColumn fx:id="texteCol" text="Question" prefWidth="350"/>
                <TableColumn fx:id="pointCol" text="Points" prefWidth="80"/>
                <TableColumn fx:id="quizIdCol" text="Quiz ID" prefWidth="80"/>
            </columns>
            <placeholder>
                <Label text="Aucune question trouvée" styleClass="card-subtitle"/>
            </placeholder>
        </TableView>
    </center>

</BorderPane>
```

---

## Étape 13 : OptionView.fxml

Correspond aux templates `option/index.html.twig` + `option/_form.html.twig`.
Bouton "Nouvelle Option" en couleur CORAL (comme dans le Twig : `var(--coral), var(--gold)`).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="tn.esprit.controllers.OptionController"
            prefWidth="950" prefHeight="620"
            styleClass="root">

    <top>
        <Label text="Gestion des Options" styleClass="page-title"
               style="-fx-padding: 15px 20px;"/>
    </top>

    <left>
        <VBox spacing="12" styleClass="glass-card"
              style="-fx-padding: 20px; -fx-min-width: 320px; -fx-max-width: 320px;">

            <Label text="Texte de l'option :" styleClass="form-label"/>
            <!-- Équivalent TextType dans OptionType.php (max 255 chars) -->
            <TextField fx:id="texteOptionField"
                       promptText="Entrez le texte de l'option"
                       styleClass="text-field"/>

            <!-- Équivalent CheckboxType dans OptionType.php -->
            <HBox spacing="10" alignment="CENTER_LEFT"
                  style="-fx-padding: 10px; -fx-background-color: rgba(255,255,255,0.05);
                         -fx-background-radius: 8px; -fx-border-color: rgba(255,255,255,0.1);
                         -fx-border-radius: 8px; -fx-border-width: 1px;">
                <CheckBox fx:id="estCorrecteCheck"/>
                <Label text="Cette option est correcte ?" styleClass="form-label"
                       style="-fx-padding: 0;"/>
            </HBox>

            <Label text="Question associée :" styleClass="form-label"/>
            <!-- Équivalent EntityType Question dans OptionType.php -->
            <ComboBox fx:id="questionCombo"
                      promptText="Sélectionnez une question"
                      maxWidth="Infinity" styleClass="combo-box"/>

            <HBox spacing="8">
                <!-- Bouton CORAL (couleur coral comme dans option/_form.html.twig) -->
                <Button text="Ajouter" onAction="#ajouterOption"
                        style="-fx-background-color: linear-gradient(to right, #e07a5f, #d4a574);
                               -fx-text-fill: white; -fx-font-weight: bold;
                               -fx-background-radius: 8px; -fx-padding: 10px 18px;"/>
                <Button text="Modifier" onAction="#modifierOption" styleClass="btn-neutral"/>
                <Button text="Supprimer" onAction="#supprimerOption" styleClass="btn-delete"/>
            </HBox>

            <Label fx:id="messageLabel" wrapText="true"/>
        </VBox>
    </left>

    <center>
        <TableView fx:id="optionTable" styleClass="table-view"
                   style="-fx-padding: 15px;">
            <columns>
                <TableColumn fx:id="idCol" text="ID" prefWidth="50"/>
                <TableColumn fx:id="texteCol" text="Option" prefWidth="300"/>
                <!-- Équivalent badge ✓ Oui / ✗ Non dans option/index.html.twig -->
                <TableColumn fx:id="correcteCol" text="Correcte ?" prefWidth="100"/>
                <TableColumn fx:id="questionIdCol" text="Question ID" prefWidth="100"/>
            </columns>
            <placeholder>
                <Label text="Aucune option trouvée" styleClass="card-subtitle"/>
            </placeholder>
        </TableView>
    </center>

</BorderPane>
```

---

## Étape 14 : Vue Hiérarchique (Quiz → Questions → Options)

Dans Symfony, la page `backoffice/quiz_management.html.twig` affiche une vue hiérarchique :
- Quiz → clic "Sélectionner" → affiche ses Questions (via AJAX `/quiz/api/{id}/questions`)
- Question → clic "Sélectionner" → affiche ses Options (via AJAX `/question/api/{id}/options`)

Dans JavaFX, on reproduit ça avec un `TreeView` ou avec des `TitledPane` imbriqués.

### QuizManagementController.java

```java
package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceOption;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

public class QuizManagementController {

    @FXML private VBox quizListContainer;  // conteneur principal

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceOption serviceOption = new ServiceOption();

    @FXML
    public void initialize() {
        chargerTousLesQuiz();
    }

    // Équivalent de la boucle {% for quiz in quizzes %} dans quiz_management.html.twig
    private void chargerTousLesQuiz() {
        quizListContainer.getChildren().clear();
        List<Quiz> quizzes = serviceQuiz.afficher();

        for (Quiz quiz : quizzes) {
            // Créer un TitledPane pour chaque quiz (équivalent .quiz-item)
            TitledPane quizPane = creerQuizPane(quiz);
            quizListContainer.getChildren().add(quizPane);
        }
    }

    private TitledPane creerQuizPane(Quiz quiz) {
        // Titre du panneau : #ID - Titre [badge état]
        String titre = "#" + quiz.getId() + " — " + quiz.getTitre() + "  [" + quiz.getEtat().toUpperCase() + "]";
        TitledPane pane = new TitledPane();
        pane.setText(titre);
        pane.setExpanded(false);  // fermé par défaut (équivalent display:none)

        // Contenu : questions chargées au clic (lazy loading comme le AJAX Symfony)
        pane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && pane.getContent() == null) {
                pane.setContent(creerContenuQuestions(quiz));
            }
        });

        return pane;
    }

    // Équivalent de fetch(`/quiz/api/${quizId}/questions`) dans quiz_management.html.twig
    private VBox creerContenuQuestions(Quiz quiz) {
        VBox container = new VBox(8);
        container.setStyle("-fx-padding: 10px 10px 10px 30px;");

        List<Question> questions = serviceQuestion.findByQuizId(quiz.getId());

        if (questions.isEmpty()) {
            container.getChildren().add(new Label("Aucune question pour ce quiz."));
        } else {
            for (Question question : questions) {
                TitledPane questionPane = creerQuestionPane(question);
                container.getChildren().add(questionPane);
            }
        }

        return container;
    }

    private TitledPane creerQuestionPane(Question question) {
        String titre = "#" + question.getId() + " — " + question.getTexteQuestion() + "  (" + question.getPoint() + " pts)";
        TitledPane pane = new TitledPane();
        pane.setText(titre.length() > 80 ? titre.substring(0, 80) + "..." : titre);
        pane.setExpanded(false);

        // Charger les options au clic (équivalent fetch(`/question/api/${questionId}/options`))
        pane.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && pane.getContent() == null) {
                pane.setContent(creerContenuOptions(question));
            }
        });

        return pane;
    }

    // Équivalent de fetch(`/question/api/${questionId}/options`) dans quiz_management.html.twig
    private VBox creerContenuOptions(Question question) {
        VBox container = new VBox(6);
        container.setStyle("-fx-padding: 8px 8px 8px 30px;");

        List<Option> options = serviceOption.findByQuestionId(question.getId());

        if (options.isEmpty()) {
            container.getChildren().add(new Label("Aucune option pour cette question."));
        } else {
            for (Option option : options) {
                // Équivalent .option-item correct/incorrect dans quiz_management.html.twig
                String badge = option.isEstCorrecte() ? "✓ Correcte" : "✗ Incorrecte";
                String couleur = option.isEstCorrecte() ? "-fx-text-fill: #4ade80;" : "-fx-text-fill: #f87171;";
                Label optionLabel = new Label("#" + option.getId() + "  " + option.getTexteOption() + "  → " + badge);
                optionLabel.setStyle(couleur + " -fx-padding: 6px 10px;");
                container.getChildren().add(optionLabel);
            }
        }

        return container;
    }
}
```

### QuizManagementView.fxml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="tn.esprit.controllers.QuizManagementController"
            prefWidth="1000" prefHeight="700"
            styleClass="root">

    <top>
        <Label text="Gestion des Quiz — Vue Hiérarchique" styleClass="page-title"
               style="-fx-padding: 15px 20px;"/>
    </top>

    <center>
        <!-- ScrollPane pour la liste hiérarchique Quiz → Questions → Options -->
        <ScrollPane fitToWidth="true" style="-fx-background-color: transparent; -fx-padding: 15px;">
            <content>
                <!-- VBox remplie dynamiquement par le controller -->
                <VBox fx:id="quizListContainer" spacing="10"
                      style="-fx-padding: 10px;"/>
            </content>
        </ScrollPane>
    </center>

</BorderPane>
```

---

## Étape 15 : Navigation entre les vues (MainApp.java)

Pour naviguer entre QuizView, QuestionView, OptionView et QuizManagementView.

```java
package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Charger la vue principale (vue hiérarchique comme quiz_management.html.twig)
        Parent root = FXMLLoader.load(
            getClass().getResource("/com/quiz/QuizManagementView.fxml")
        );

        Scene scene = new Scene(root, 1000, 700);

        // Charger le CSS (charte graphique glassmorphism)
        scene.getStylesheets().add(
            getClass().getResource("/com/quiz/css/style.css").toExternalForm()
        );

        // Fond sombre obligatoire (équivalent --bg-dark: #0a0f0d)
        scene.setFill(Color.web("#0a0f0d"));

        primaryStage.setTitle("Gestion des Quiz");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Méthode utilitaire pour changer de vue depuis n'importe quel controller
    public static void changerVue(Stage stage, String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(
            MainApp.class.getResource(fxmlPath)
        );
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            MainApp.class.getResource("/com/quiz/css/style.css").toExternalForm()
        );
        scene.setFill(Color.web("#0a0f0d"));
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

## Récapitulatif Complet — Tous les Fichiers à Créer

```
src/main/java/tn/esprit/
├── entities/
│   ├── Quiz.java              ← Étape 1
│   ├── Question.java          ← Étape 1
│   └── Option.java            ← Étape 1
├── services/
│   ├── IService.java          ← Étape 2
│   ├── ServiceQuiz.java       ← Étape 3
│   ├── ServiceQuestion.java   ← Étape 4
│   └── ServiceOption.java     ← Étape 5
├── controllers/
│   ├── QuizController.java          ← Étape 7
│   ├── QuestionController.java      ← Étape 10
│   ├── OptionController.java        ← Étape 11
│   └── QuizManagementController.java ← Étape 14
└── MainApp.java               ← Étape 15

src/main/resources/com/quiz/
├── QuizView.fxml              ← Étape 8
├── QuestionView.fxml          ← Étape 12
├── OptionView.fxml            ← Étape 13
├── QuizManagementView.fxml    ← Étape 14
└── css/
    └── style.css              ← Étape 9
```

## Ordre de Création Recommandé

1. `MyConnection.java` (connexion BDD — déjà dans le projet)
2. Les 3 entités : `Quiz.java`, `Question.java`, `Option.java`
3. `IService.java`
4. Les 3 services : `ServiceQuiz`, `ServiceQuestion`, `ServiceOption`
5. `style.css`
6. Les 4 controllers + leurs FXML
7. `MainApp.java`
