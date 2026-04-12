package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Question;
import tn.esprit.services.ServiceQuestion;

public class QuestionController {

    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private Label cardSubtitle;
    @FXML private TextArea texteField;
    @FXML private TextField pointField;
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
    private Question questionAModifier = null;
    private int quizId;

    @FXML
    public void initialize() {
        texteField.textProperty().addListener((o, ov, nv) -> resetField(texteField));
        pointField.textProperty().addListener((o, ov, nv) -> resetField(pointField));
    }

    public void initNouvelle(int quizId) {
        this.quizId = quizId;
    }

    public void initModifier(Question question) {
        this.questionAModifier = question;
        this.quizId = question.getQuizId();
        pageTitle.setText("Modifier la Question");
        cardTitle.setText("Modifier la Question");
        cardSubtitle.setText("Mettez à jour la question");
        texteField.setText(question.getTexteQuestion());
        pointField.setText(String.valueOf(question.getPoint()));
    }

    @FXML
    public void sauvegarder() {
        resetAll();
        boolean valid = true;

        String texte = texteField.getText() == null ? "" : texteField.getText().trim();
        String pointStr = pointField.getText() == null ? "" : pointField.getText().trim();

        // ── Texte de la question ──
        if (texte.isEmpty()) {
            markError(texteField, "⚠ Le texte de la question est obligatoire.");
            valid = false;
        } else if (texte.length() < 10) {
            markError(texteField,
                "⚠ Question trop courte — minimum 10 caractères (actuellement " + texte.length() + ").\n" +
                "   Formulez une question complète et compréhensible.");
            valid = false;
        } else if (texte.length() > 1000) {
            markError(texteField,
                "⚠ Question trop longue — maximum 1000 caractères (actuellement " + texte.length() + ").\n" +
                "   Simplifiez l'énoncé de la question.");
            valid = false;
        } else if (!texte.contains(" ")) {
            markError(texteField,
                "⚠ La question doit contenir au moins deux mots.\n" +
                "   Exemple : « Qu'est-ce que la programmation orientée objet ? »");
            valid = false;
        }

        // ── Points ──
        if (valid) {
            if (pointStr.isEmpty()) {
                markError(pointField, "⚠ Le nombre de points est obligatoire.");
                valid = false;
            } else {
                try {
                    int point = Integer.parseInt(pointStr);
                    if (point < 1) {
                        markError(pointField, "⚠ Les points doivent être au minimum 1 (valeur saisie : " + point + ").");
                        valid = false;
                    } else if (point > 100) {
                        markError(pointField, "⚠ Les points ne peuvent pas dépasser 100 (valeur saisie : " + point + ").");
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    markError(pointField,
                        "⚠ « " + pointStr + " » n'est pas un nombre valide.\n" +
                        "   Entrez un entier entre 1 et 100 (ex: 5, 10, 20).");
                    valid = false;
                }
            }
        }

        if (!valid) return;

        int point = Integer.parseInt(pointStr);
        if (questionAModifier == null) {
            serviceQuestion.ajouter(new Question(texte, point, null, quizId));
        } else {
            questionAModifier.setTexteQuestion(texte);
            questionAModifier.setPoint(point);
            serviceQuestion.modifier(questionAModifier);
        }
        retour();
    }

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

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void markError(Control field, String msg) {
        field.setStyle(FIELD_ERROR);
        messageLabel.setText(msg);
        messageLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-color:rgba(239,68,68,0.08); -fx-background-radius:8;" +
            "-fx-padding:8 12; -fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8; -fx-border-width:1; -fx-wrap-text:true;");
    }

    private void resetField(Control field) {
        field.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    private void resetAll() {
        texteField.setStyle(FIELD_NORMAL);
        pointField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }
}
