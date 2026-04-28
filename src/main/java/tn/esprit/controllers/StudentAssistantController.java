package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import tn.esprit.services.StudentAssistantExecutor;
import tn.esprit.services.StudentAssistantService;
import tn.esprit.session.JwtManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Student AI Assistant Controller
 * Floating chat bubble - always visible on all frontoffice pages.
 * - Shows data in chat (LIST actions)
 * - Navigates ONLY when user explicitly asks to go somewhere
 * - Vertical layout for suggestions
 */
public class StudentAssistantController {

    @FXML private VBox       chatPanel;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox       messagesBox;
    @FXML private TextField  inputField;
    @FXML private Button     btnSend;
    @FXML private Button     btnToggle;
    @FXML private Label      labelStatus;

    private final List<StudentAssistantService.ChatMessage> history = new ArrayList<>();
    private final StudentAssistantExecutor executor = new StudentAssistantExecutor();
    private boolean isOpen = false;
    private boolean welcomeShown = false;

    private java.util.function.Consumer<String> onNavigate;
    public void setOnNavigate(java.util.function.Consumer<String> cb) { this.onNavigate = cb; }

    @FXML
    public void initialize() {
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);

        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onSend();
        });
    }

    private void showWelcome() {
        if (welcomeShown) return;
        welcomeShown = true;

        String name = JwtManager.getCurrentUser() != null
            ? JwtManager.getCurrentUser().getPrenom() : "étudiant";

        addBotMessage(
            "Bonjour **" + name + "** ! 👋\n\n" +
            "Je suis votre assistant AutoLearn.\n" +
            "Posez-moi n'importe quelle question !"
        );

        // Vertical quick actions
        addQuickActions();
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    @FXML
    public void onToggle() {
        isOpen = !isOpen;
        chatPanel.setVisible(isOpen);
        chatPanel.setManaged(isOpen);
        btnToggle.setText(isOpen ? "✕" : "💬");
        btnToggle.setStyle(
            (isOpen
                ? "-fx-background-color:rgba(109,40,217,0.15); -fx-text-fill:#7c3aed;"
                : "-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5); -fx-text-fill:white;") +
            "-fx-font-size:" + (isOpen ? "14" : "20") + ";" +
            "-fx-min-width:54; -fx-min-height:54;" +
            "-fx-max-width:54; -fx-max-height:54;" +
            "-fx-background-radius:50%;" +
            "-fx-cursor:hand; -fx-border-width:0;" +
            "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.5),14,0,0,4);"
        );
        if (isOpen) {
            showWelcome();
            Platform.runLater(() -> {
                inputField.requestFocus();
                scrollToBottom();
            });
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    @FXML
    public void onSend() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        btnSend.setDisable(true);
        addUserMessage(text);
        history.add(new StudentAssistantService.ChatMessage("user", text));

        // Typing indicator
        VBox typingIndicator = addTypingIndicator();
        labelStatus.setText("⏳ Réflexion...");

        StudentAssistantService.sendMessage(text, history).thenAccept(response -> {
            Platform.runLater(() -> {
                // Remove typing indicator
                messagesBox.getChildren().remove(typingIndicator);
                labelStatus.setText("● En ligne");

                // Always show message in chat
                addBotMessage(response.message());
                history.add(new StudentAssistantService.ChatMessage("assistant", response.message()));

                // Execute action (LIST shows in chat, NAVIGATE goes to page)
                if (!"CHAT".equals(response.intent())) {
                    executeAction(response.intent(), response.params());
                }

                btnSend.setDisable(false);
                scrollToBottom();
            });
        });
    }

    @FXML
    public void onClear() {
        messagesBox.getChildren().clear();
        history.clear();
        welcomeShown = false;
        showWelcome();
    }

    // ── Execute action ────────────────────────────────────────────────────────

    private void executeAction(String intent, com.google.gson.JsonObject params) {
        StudentAssistantExecutor.ActionResult result = executor.execute(intent, params);

        // Show result message in chat if different from AI message
        if (result.message() != null && !result.message().isEmpty()) {
            addBotMessage(result.message());
            history.add(new StudentAssistantService.ChatMessage("assistant", result.message()));
        }

        // Navigate ONLY for NAVIGATE_* intents (not LIST_*)
        if (result.navigateTo() != null && onNavigate != null && intent.startsWith("NAVIGATE_")) {
            Platform.runLater(() -> onNavigate.accept(result.navigateTo()));
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void addBotMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-padding:2 0 2 0;");

        // Avatar circle
        StackPane avatar = new StackPane();
        Circle bg = new Circle(16);
        bg.setStyle("-fx-fill:linear-gradient(to bottom right,#7c3aed,#4f46e5);");
        Label ai = new Label("AI");
        ai.setStyle("-fx-font-size:8; -fx-font-weight:900; -fx-text-fill:white;");
        avatar.getChildren().addAll(bg, ai);
        avatar.setMinWidth(32); avatar.setMaxWidth(32);
        avatar.setMinHeight(32); avatar.setMaxHeight(32);
        avatar.setAlignment(Pos.CENTER);

        // Message bubble
        VBox bubble = new VBox(3);
        bubble.setMaxWidth(240);
        bubble.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:4 16 16 16;" +
            "-fx-padding:10 14 10 14;" +
            "-fx-border-color:#ede9fe; -fx-border-radius:4 16 16 16; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.08),6,0,0,2);"
        );

        for (String line : text.split("\n")) {
            if (line.isBlank()) {
                Region sp = new Region(); sp.setPrefHeight(3);
                bubble.getChildren().add(sp);
                continue;
            }
            String clean = line.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
            Label lbl = new Label(clean);
            lbl.setWrapText(true);
            lbl.setMaxWidth(220);
            boolean isBold = line.contains("**");
            lbl.setStyle(isBold
                ? "-fx-font-size:12; -fx-text-fill:#1e1b4b; -fx-font-weight:700;"
                : "-fx-font-size:12; -fx-text-fill:#374151; -fx-line-spacing:2;");
            bubble.getChildren().add(lbl);
        }

        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setStyle("-fx-padding:2 0 2 0;");

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(230);
        bubble.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
            "-fx-text-fill:white; -fx-font-size:12;" +
            "-fx-background-radius:16 4 16 16;" +
            "-fx-padding:10 14 10 14;" +
            "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.3),8,0,0,2);"
        );

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
    }

    private VBox addTypingIndicator() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        StackPane avatar = new StackPane();
        Circle bg = new Circle(16);
        bg.setStyle("-fx-fill:linear-gradient(to bottom right,#7c3aed,#4f46e5);");
        Label ai = new Label("AI");
        ai.setStyle("-fx-font-size:8; -fx-font-weight:900; -fx-text-fill:white;");
        avatar.getChildren().addAll(bg, ai);
        avatar.setMinWidth(32); avatar.setMaxWidth(32);

        Label dots = new Label("● ● ●");
        dots.setStyle(
            "-fx-background-color:white; -fx-text-fill:#c4b5fd;" +
            "-fx-font-size:14; -fx-background-radius:4 16 16 16;" +
            "-fx-padding:10 14 10 14;" +
            "-fx-border-color:#ede9fe; -fx-border-radius:4 16 16 16; -fx-border-width:1;"
        );

        row.getChildren().addAll(avatar, dots);

        VBox wrapper = new VBox(row);
        messagesBox.getChildren().add(wrapper);
        return wrapper;
    }

    /** Vertical quick action chips */
    private void addQuickActions() {
        VBox actionsBox = new VBox(6);
        actionsBox.setStyle("-fx-padding:4 0 4 42;");

        String[][] actions = {
            {"📚 Voir les cours",        "liste les cours"},
            {"🎉 Voir les événements",   "liste les événements"},
            {"🏆 Voir les challenges",   "liste les challenges"},
            {"👥 Voir les communautés",  "liste les communautés"},
            {"🧭 Aller aux cours",       "aller aux cours"},
        };

        for (String[] action : actions) {
            Button chip = new Button(action[0]);
            String baseStyle =
                "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                "-fx-font-size:11; -fx-font-weight:600;" +
                "-fx-padding:7 14; -fx-background-radius:20;" +
                "-fx-cursor:hand; -fx-border-width:1;" +
                "-fx-border-color:#ddd6fe; -fx-border-radius:20;" +
                "-fx-alignment:CENTER_LEFT;";
            String hoverStyle =
                "-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white;" +
                "-fx-font-size:11; -fx-font-weight:600;" +
                "-fx-padding:7 14; -fx-background-radius:20;" +
                "-fx-cursor:hand; -fx-border-width:0;" +
                "-fx-alignment:CENTER_LEFT;";
            chip.setStyle(baseStyle);
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setOnMouseEntered(e -> chip.setStyle(hoverStyle));
            chip.setOnMouseExited(e -> chip.setStyle(baseStyle));
            chip.setOnAction(e -> {
                inputField.setText(action[1]);
                onSend();
            });
            actionsBox.getChildren().add(chip);
        }

        messagesBox.getChildren().add(actionsBox);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
