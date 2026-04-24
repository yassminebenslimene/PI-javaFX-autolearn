package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.services.ChatbotActionExecutor;
import tn.esprit.services.ChatbotService;
import tn.esprit.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the AI Chatbot interface.
 *
 * Handles:
 * - Sending messages to Hugging Face API
 * - Displaying chat bubbles
 * - Executing CRUD actions based on AI response
 * - Navigating to relevant pages
 */
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
        // Welcome message
        String userName = "";
        if (SessionManager.getCurrentUser() != null) {
            userName = SessionManager.getCurrentUser().getPrenom();
        }

        addBotMessage(
            "Bonjour " + userName + " ! Je suis votre assistant AutoLearn.\n\n" +
            "Je peux vous aider a :\n" +
            "• Lister, creer, modifier ou supprimer des cours\n" +
            "• Gerer les utilisateurs et etudiants\n" +
            "• Creer des evenements et challenges\n" +
            "• Naviguer dans l'application\n\n" +
            "Que souhaitez-vous faire ?"
        );
    }

    // ── Send message ──────────────────────────────────────────────────────────

    @FXML
    private void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        btnSend.setDisable(true);
        suggestionsBox.setVisible(false);
        suggestionsBox.setManaged(false);

        // Show user message
        addUserMessage(text);

        // Show typing indicator
        VBox typingBubble = addTypingIndicator();

        // Add to history
        history.add(new ChatbotService.ChatMessage("user", text));

        // Send to AI
        ChatbotService.sendMessage(text, history).thenAccept(response -> {
            Platform.runLater(() -> {
                // Remove typing indicator
                messagesBox.getChildren().remove(typingBubble);

                // Show AI message
                addBotMessage(response.message());

                // Add to history
                history.add(new ChatbotService.ChatMessage("assistant", response.message()));

                // Execute action if needed
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
        Button btn = (Button) e.getSource();
        inputField.setText(btn.getText());
        onSend();
    }

    @FXML
    private void onClear() {
        messagesBox.getChildren().clear();
        history.clear();
        suggestionsBox.setVisible(true);
        suggestionsBox.setManaged(true);
        initialize();
    }

    // ── Execute action ────────────────────────────────────────────────────────

    private void executeAction(String intent, com.google.gson.JsonObject params) {
        ChatbotActionExecutor.ActionResult result = executor.execute(intent, params);

        // If there's additional data to show (list result)
        if (result.data() != null && result.message() != null && !result.message().isEmpty()) {
            // Message already shown by addBotMessage above
        }

        // Navigate if needed
        if (result.navigateTo() != null && !result.navigateTo().isEmpty()) {
            try {
                MainApp.showBackofficeView(result.navigateTo(), "");
            } catch (Exception e) {
                System.err.println("[Chatbot] Navigation error: " + e.getMessage());
            }
        }

        // Show action result if different from AI message
        if (!result.success()) {
            addErrorMessage(result.message());
        } else if (result.data() != null) {
            // Show success confirmation
            addSuccessMessage("Action effectuee avec succes !");
        }
    }

    // ── UI Builders ───────────────────────────────────────────────────────────

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(420);
        bubble.setStyle(
            "-fx-background-color:#7a6ad8; -fx-text-fill:white;" +
            "-fx-font-size:13; -fx-padding:10 16 10 16;" +
            "-fx-background-radius:18 18 4 18;"
        );

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Bot avatar
        Label avatar = new Label("AI");
        avatar.setStyle(
            "-fx-background-color:#059669; -fx-text-fill:white;" +
            "-fx-font-size:10; -fx-font-weight:700;" +
            "-fx-background-radius:50%; -fx-padding:6 8 6 8;" +
            "-fx-min-width:32; -fx-min-height:32;" +
            "-fx-alignment:CENTER;"
        );

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(460);
        bubble.setStyle(
            "-fx-background-color:#1e2130; -fx-text-fill:rgba(255,255,255,0.9);" +
            "-fx-font-size:13; -fx-padding:10 16 10 16;" +
            "-fx-background-radius:4 18 18 18;" +
            "-fx-border-color:rgba(255,255,255,0.08); -fx-border-width:1;" +
            "-fx-border-radius:4 18 18 18;"
        );

        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addSuccessMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);

        Label badge = new Label("✓  " + text);
        badge.setStyle(
            "-fx-background-color:rgba(16,185,129,0.15);" +
            "-fx-text-fill:#10b981; -fx-font-size:12; -fx-font-weight:700;" +
            "-fx-padding:6 16 6 16; -fx-background-radius:20;" +
            "-fx-border-color:rgba(16,185,129,0.3); -fx-border-width:1; -fx-border-radius:20;"
        );

        row.getChildren().add(badge);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void addErrorMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);

        Label badge = new Label("✗  " + text);
        badge.setStyle(
            "-fx-background-color:rgba(239,68,68,0.15);" +
            "-fx-text-fill:#ef4444; -fx-font-size:12; -fx-font-weight:700;" +
            "-fx-padding:6 16 6 16; -fx-background-radius:20;" +
            "-fx-border-color:rgba(239,68,68,0.3); -fx-border-width:1; -fx-border-radius:20;"
        );

        row.getChildren().add(badge);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private VBox addTypingIndicator() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("AI");
        avatar.setStyle(
            "-fx-background-color:#059669; -fx-text-fill:white;" +
            "-fx-font-size:10; -fx-font-weight:700;" +
            "-fx-background-radius:50%; -fx-padding:6 8 6 8;" +
            "-fx-min-width:32; -fx-min-height:32; -fx-alignment:CENTER;"
        );

        VBox dots = new VBox();
        dots.setStyle(
            "-fx-background-color:#1e2130; -fx-padding:14 20 14 20;" +
            "-fx-background-radius:4 18 18 18;" +
            "-fx-border-color:rgba(255,255,255,0.08); -fx-border-width:1;" +
            "-fx-border-radius:4 18 18 18;"
        );

        Label typing = new Label("En train d'ecrire...");
        typing.setStyle("-fx-text-fill:rgba(255,255,255,0.4); -fx-font-size:12;");
        dots.getChildren().add(typing);

        // Animate dots
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(500), e -> {
                String t = typing.getText();
                if (t.endsWith("...")) typing.setText("En train d'ecrire");
                else typing.setText(t + ".");
            })
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
        dots.setUserData(timeline);

        row.getChildren().addAll(avatar, dots);
        messagesBox.getChildren().add(row);
        scrollToBottom();
        return dots;
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
