package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import tn.esprit.MainApp;
import tn.esprit.services.ChatbotActionExecutor;
import tn.esprit.services.ChatbotService;
import tn.esprit.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class ChatbotController {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox       messagesBox;
    @FXML private TextField  inputField;
    @FXML private Button     btnSend;
    @FXML private Label      labelStatus;
    @FXML private HBox       suggestionsBox;

    private final List<ChatbotService.ChatMessage> history = new ArrayList<>();
    private final ChatbotActionExecutor executor = new ChatbotActionExecutor();

    // ── Initialize ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        String name = SessionManager.getCurrentUser() != null
            ? SessionManager.getCurrentUser().getPrenom() : "";

        addBotMessage(
            "Bonjour **" + name + "** ! Je suis votre assistant AutoLearn.\n\n" +
            "Je peux vous aider à :\n" +
            "• **Lister** les cours, étudiants, événements, challenges\n" +
            "• **Créer** des cours, événements, challenges, étudiants\n" +
            "• **Supprimer** des éléments par ID\n" +
            "• **Naviguer** dans l'application\n\n" +
            "Que souhaitez-vous faire ?"
        );
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        btnSend.setDisable(true);

        addUserMessage(text);
        history.add(new ChatbotService.ChatMessage("user", text));

        // Show subtle loading indicator in status bar
        Platform.runLater(() -> labelStatus.setText("En train de réfléchir..."));

        ChatbotService.sendMessage(text, history).thenAccept(response -> {
            Platform.runLater(() -> {
                labelStatus.setText("En ligne");

                // Show AI response
                addBotMessage(response.message());
                history.add(new ChatbotService.ChatMessage("assistant", response.message()));

                // Execute action
                if (!"CHAT".equals(response.intent())) {
                    executeAction(response.intent(), response.params());
                }

                btnSend.setDisable(false);
                scrollToBottom();
            });
        });
    }

    @FXML
    private void onSuggestion(javafx.event.ActionEvent e) {
        inputField.setText(((Button) e.getSource()).getText());
        onSend();
    }

    @FXML
    private void onClear() {
        messagesBox.getChildren().clear();
        history.clear();
        initialize();
    }

    // ── Execute action ────────────────────────────────────────────────────────

    private void executeAction(String intent, com.google.gson.JsonObject params) {
        ChatbotActionExecutor.ActionResult result = executor.execute(intent, params);

        // Navigate
        if (result.navigateTo() != null && !result.navigateTo().isEmpty()) {
            try {
                MainApp.showBackofficeView(result.navigateTo(), "");
            } catch (Exception e) {
                System.err.println("[Chatbot] Nav error: " + e.getMessage());
            }
        }

        if (!result.success()) {
            // Asking for missing info → show as bot message (conversational)
            if (result.message() != null && !result.message().isEmpty()) {
                addBotMessage(result.message());
                history.add(new ChatbotService.ChatMessage("assistant", result.message()));
            }
        } else if (result.data() != null && result.navigateTo() == null) {
            // LIST or CREATE result
            if (result.message() != null && !result.message().isEmpty()) {
                addBotMessage(result.message());
                history.add(new ChatbotService.ChatMessage("assistant", result.message()));
            }
        } else if (result.data() != null) {
            addSuccessMessage(result.message() != null ? result.message() : "Action effectuée avec succès !");
        }
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setStyle("-fx-padding:2 0 2 60;");

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(500);
        bubble.setStyle(
            "-fx-background-color:#7a6ad8;" +
            "-fx-text-fill:white;" +
            "-fx-font-size:13;" +
            "-fx-padding:10 14 10 14;" +
            "-fx-background-radius:18 18 4 18;"
        );

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-padding:2 60 2 0;");

        // Avatar
        StackPane avatar = new StackPane();
        avatar.setMinWidth(32); avatar.setMinHeight(32);
        avatar.setPrefWidth(32); avatar.setPrefHeight(32);
        Circle circle = new Circle(16);
        circle.setStyle("-fx-fill:linear-gradient(to bottom right,#7a6ad8,#059669);");
        Label avatarLabel = new Label("AI");
        avatarLabel.setStyle("-fx-font-size:9; -fx-font-weight:900; -fx-text-fill:white;");
        avatar.getChildren().addAll(circle, avatarLabel);

        // Render markdown-like formatting
        Label bubble = new Label(renderText(text));
        bubble.setWrapText(true);
        bubble.setMaxWidth(520);
        bubble.setStyle(
            "-fx-background-color:#161b22;" +
            "-fx-text-fill:#e6edf3;" +
            "-fx-font-size:13;" +
            "-fx-padding:10 14 10 14;" +
            "-fx-background-radius:4 18 18 18;" +
            "-fx-border-color:#30363d;" +
            "-fx-border-width:1;" +
            "-fx-border-radius:4 18 18 18;"
        );

        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addSuccessMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setStyle("-fx-padding:4 0 4 0;");

        Label badge = new Label("✓  " + text);
        badge.setStyle(
            "-fx-background-color:rgba(16,185,129,0.12);" +
            "-fx-text-fill:#10b981;" +
            "-fx-font-size:12; -fx-font-weight:700;" +
            "-fx-padding:6 16 6 16; -fx-background-radius:20;" +
            "-fx-border-color:rgba(16,185,129,0.25);" +
            "-fx-border-width:1; -fx-border-radius:20;"
        );

        row.getChildren().add(badge);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addErrorMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setStyle("-fx-padding:4 0 4 0;");

        Label badge = new Label("✗  " + text);
        badge.setStyle(
            "-fx-background-color:rgba(239,68,68,0.12);" +
            "-fx-text-fill:#f85149;" +
            "-fx-font-size:12; -fx-font-weight:700;" +
            "-fx-padding:6 16 6 16; -fx-background-radius:20;" +
            "-fx-border-color:rgba(239,68,68,0.25);" +
            "-fx-border-width:1; -fx-border-radius:20;"
        );

        row.getChildren().add(badge);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    // ── Text rendering ────────────────────────────────────────────────────────

    /**
     * Converts **bold** markdown to plain text (JavaFX Label doesn't support HTML).
     * Keeps the text readable without ugly asterisks.
     */
    private String renderText(String text) {
        if (text == null) return "";
        // Remove **bold** markers but keep the text
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                   .replaceAll("\\*(.+?)\\*", "$1")
                   .replaceAll("`(.+?)`", "$1");
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
