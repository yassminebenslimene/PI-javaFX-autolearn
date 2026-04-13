package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceQuestion;
import tn.esprit.services.ServiceQuiz;

/**
 * Controller du formulaire Question (question_form.fxml).
 * G├¿re la cr├®ation d'une nouvelle question et la modification d'une question existante.
 * Si questionAModifier == null ÔåÆ mode cr├®ation, sinon ÔåÆ mode modification.
 */
public class QuestionController {

    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private Label cardSubtitle;
    @FXML private TextArea texteField;
    @FXML private TextField pointField;
    @FXML private ComboBox<Quiz> quizCombo;
    @FXML private Label quizErrorLabel;
    @FXML private Label messageLabel;

    private static final String FIELD_NORMAL =
        "-fx-background-color:rgba(255,255,255,0.05);" +
        "-fx-border-color:rgba(255,255,255,0.1); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

    private static final String FIELD_ERROR =
        "-fx-background-color:rgba(239,68,68,0.08);" +
        "-fx-border-color:rgba(239,68,68,0.6); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1.5px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private Question questionAModifier = null;
    private int quizId;

    @FXML
    public void initialize() {
        // Remplir la ComboBox quiz
        quizCombo.getItems().addAll(serviceQuiz.afficher());
        quizCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Quiz item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
        quizCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Quiz item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Choisissez le quiz auquel appartient cette question" : item.getTitre());
            }
        });
        quizCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                quizCombo.setStyle(FIELD_NORMAL);
                if (quizErrorLabel != null) { quizErrorLabel.setVisible(false); quizErrorLabel.setManaged(false); }
            }
        });

        // Forcer le fond sombre sur le TextArea
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node content = texteField.lookup(".content");
            if (content != null) content.setStyle("-fx-background-color:#1a2e1f;");
        });

        texteField.textProperty().addListener((o, ov, nv) -> resetField(texteField));
        pointField.textProperty().addListener((o, ov, nv) -> resetField(pointField));
    }

    public void initNouvelle(int quizId) {
        this.quizId = quizId;
        // Pr├®-s├®lectionner le quiz dans la ComboBox
        quizCombo.getItems().stream().filter(q -> q.getId() == quizId).findFirst().ifPresent(quizCombo::setValue);
    }

    public void initModifier(Question question) {
        this.questionAModifier = question;
        this.quizId = question.getQuizId();
        pageTitle.setText("Modifier la Question");
        cardTitle.setText("Modifier la Question");
        cardSubtitle.setText("Mettez ├á jour les informations");
        texteField.setText(question.getTexteQuestion());
        pointField.setText(String.valueOf(question.getPoint()));
        // Pr├®-s├®lectionner le quiz
        quizCombo.getItems().stream().filter(q -> q.getId() == question.getQuizId()).findFirst().ifPresent(quizCombo::setValue);
    }

    // ÔöÇÔöÇ Sauvegarder : appel├® quand on clique sur le bouton Enregistrer ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void sauvegarder() {
        resetAll(); // effacer les erreurs pr├®c├®dentes
        boolean valid = true;

        String texte = texteField.getText() == null ? "" : texteField.getText().trim();
        String pointStr = pointField.getText() == null ? "" : pointField.getText().trim();

        // ÔöÇÔöÇ Validation du texte de la question ÔöÇÔöÇ
        if (texte.isEmpty()) {
            markError(texteField, "ÔÜá Le texte de la question est obligatoire.");
            valid = false;
        } else if (texte.length() < 10) {
            markError(texteField,
                "ÔÜá Question trop courte ÔÇö minimum 10 caract├¿res (actuellement " + texte.length() + ").\n" +
                "   Formulez une question compl├¿te et compr├®hensible.");
            valid = false;
        } else if (texte.length() > 1000) {
            markError(texteField,
                "ÔÜá Question trop longue ÔÇö maximum 1000 caract├¿res (actuellement " + texte.length() + ").\n" +
                "   Simplifiez l'├®nonc├® de la question.");
            valid = false;
        } else if (!texte.contains(" ")) {
            // Une question doit contenir au moins 2 mots
            markError(texteField,
                "ÔÜá La question doit contenir au moins deux mots.\n" +
                "   Exemple : ┬½ Qu'est-ce que la programmation orient├®e objet ? ┬╗");
            valid = false;
        }

        // ÔöÇÔöÇ Validation des points ÔöÇÔöÇ
        if (valid) {
            if (pointStr.isEmpty()) {
                markError(pointField, "ÔÜá Le nombre de points est obligatoire.");
                valid = false;
            } else {
                try {
                    int point = Integer.parseInt(pointStr);
                    if (point < 1) {
                        markError(pointField, "ÔÜá Les points doivent ├¬tre au minimum 1 (valeur saisie : " + point + ").");
                        valid = false;
                    } else if (point > 100) {
                        markError(pointField, "ÔÜá Les points ne peuvent pas d├®passer 100 (valeur saisie : " + point + ").");
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    // L'utilisateur a saisi du texte au lieu d'un nombre
                    markError(pointField,
                        "ÔÜá ┬½ " + pointStr + " ┬╗ n'est pas un nombre valide.\n" +
                        "   Entrez un entier entre 1 et 100 (ex: 5, 10, 20).");
                    valid = false;
                }
            }
        }

        // Si une validation a ├®chou├®, on arr├¬te ici
        if (!valid) return;

        // ÔöÇÔöÇ Validation du quiz associ├® ÔöÇÔöÇ
        Quiz quizSelectionne = quizCombo.getValue();
        if (quizSelectionne == null) {
            quizCombo.setStyle(FIELD_ERROR);
            if (quizErrorLabel != null) { quizErrorLabel.setVisible(true); quizErrorLabel.setManaged(true); }
            showError("ÔÜá Veuillez choisir le quiz auquel appartient cette question.");
            return;
        }
        quizId = quizSelectionne.getId();

        // ÔöÇÔöÇ Sauvegarde en BDD ÔöÇÔöÇ
        int point = Integer.parseInt(pointStr);
        boolean ok;
        if (questionAModifier == null) {
            // Mode cr├®ation : ins├®rer une nouvelle question li├®e au quiz
            ok = serviceQuestion.ajouter(new Question(texte, point, null, quizId));
            showAlert(ok, "Question ajout├®e avec succ├¿s !", "├ëchec de l'ajout de la question.");
        } else {
            // Mode modification : mettre ├á jour la question existante
            questionAModifier.setTexteQuestion(texte);
            questionAModifier.setPoint(point);
            ok = serviceQuestion.modifier(questionAModifier);
            showAlert(ok, "Question modifi├®e avec succ├¿s !", "├ëchec de la modification de la question.");
        }
        // Si succ├¿s ÔåÆ retourner ├á la liste des quiz
        if (ok) retour();
    }

    // ÔöÇÔöÇ Retour : revenir ├á la liste sans sauvegarder ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void retour() {
        try {
            StackPane contentArea = (StackPane) texteField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Affiche une alerte de succ├¿s ou d'├®chec
    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "Ô£à Succ├¿s" : "ÔØî ├ëchec");
        alert.setContentText(success ? msgOk : msgEchec);
        alert.showAndWait();
    }

    // ÔöÇÔöÇ Helpers ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    private void markError(Control field, String msg) {
        field.setStyle(FIELD_ERROR);
        showError(msg);
    }

    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-color:rgba(239,68,68,0.08); -fx-background-radius:8;" +
            "-fx-padding:8 12; -fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8; -fx-border-width:1; -fx-wrap-text:true;");
    }

    // Remet un champ ├á son style normal
    private void resetField(Control field) {
        field.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    // Remet tous les champs ├á leur style normal
    private void resetAll() {
        texteField.setStyle(FIELD_NORMAL);
        pointField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }
}