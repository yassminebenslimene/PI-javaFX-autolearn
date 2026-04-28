package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import tn.esprit.services.StudentAssistantExecutor;
import tn.esprit.services.StudentAssistantService;
import tn.esprit.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Student AI Assistant Controller
 * Floating chat bubble in the frontoffice for student actions.
 */
public class StudentAssistantController {

    @FXML private VBox      chatPanel;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox      messagesBox;
    @FXML private TextField inputField;
    @FXML private Button    btnSend;
    @FXML private Button    btnToggle;
    @FXML private Label     labelStatus;
    @FXML private HBox      suggestionsBox;

    private final List<StudentAssistantService.ChatMessage> history = new ArrayList<>();
    private final StudentAssistantExecutor executor = new StudentAssistantExecutor();
    private boolean isOpen = false;

    // Callback to FrontofficeController for navigation
    private java.util.function.Consumer<String> onNavigate;

    public void setOnNavigate(java.util.function.Consumer<String> cb) {
        this.onNavigate = cb;
    }

    @FXML
    public void initialize() {
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);

        // Show welcome on first open
        String name = SessionManager.getCurrentUser() != null
            ? SessionManager.getCurrentUser().getPrenom() : "étudiant";

        addBotMessage(
            "Bonjour **" + name + "** ! 👋 Je suis votre assistant AutoLearn.\n\n" +
            "Je peux vous aider à :\n" +
            "📚 Voir et accéder aux **cours**\n" +
            "🎉 S'inscrire aux **événements**\n" +
            "🏆 Rejoindre des **challenges**\n" +
            "👥 Rejoindre des **communautés**\n" +
            "👫 Créer ou rejoindre des **équipes**\n\n" +
            "Que souhaitez-vous faire ?"
        );

        // Quick suggestion chips
        addSuggestions(new String[]{
            "📚 Voir les cours",
            "🎉 Voir les événements",
            "👥 Voir les communautés",
            "🏆 Challenges"
        });

        // Send on Enter
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onSend();
        });
    }

    // ── Toggle ────────────────────────────────────────────────────────────────

    @FXML
    public void onToggle() {
        isOpen = !isOpen;
        chatPanel.setVisible(isOpen);
        chatPanel.setManaged(isOpen);
        btnToggle.setText(isOpen ? "✕" : "💬");
        if (isOpen) {
            Platform.runLater(() -> inputField.requestFocus());
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

        labelStatus.setText("⏳ En train de réfléchir...");

        StudentAssistantService.sendMessage(text, history).thenAccept(response -> {
            Platform.runLater(() -> {
                labelStatus.setText("🟢 En ligne");
                addBotMessage(response.message());
                history.add(new StudentAssistantService.ChatMessage("assistant", response.message()));

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
        initialize();
    }

    // ── Execute action ────────────────────────────────────────────────────────

    private void executeAction(String intent, com.google.gson.JsonObject params) {
        StudentAssistantExecutor.ActionResult result = executor.execute(intent, params);

        // Show result message if any
        if (result.message() != null && !result.message().isEmpty()) {
            addBotMessage(result.message());
            history.add(new StudentAssistantService.ChatMessage("assistant", result.message()));
        }

        // Navigate if needed
        if (result.navigateTo() != null && onNavigate != null) {
            Platform.runLater(() -> onNavigate.accept(result.navigateTo()));
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void addBotMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        StackPane avatar = new StackPane();
        Circle circle = new Circle(18);
        circle.setStyle("-fx-fill:linear-gradient(to bottom right,#7c3aed,#4f46e5);");
        Label avatarLabel = new Label("AI");
        avatarLabel.setStyle("-fx-font-size:9; -fx-font-weight:900; -fx-text-fill:white;");
        avatar.getChildren().addAll(circle, avatarLabel);
        avatar.setMinWidth(36);
        avatar.setMaxWidth(36);
        avatar.setAlignment(Pos.CENTER);

        // Bubble
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(280);
        bubble.setStyle(
            "-fx-background-color:white;" +
            "-fx-background-radius:18 18 18 4;" +
            "-fx-padding:12 16 12 16;" +
            "-fx-border-color:#ede9fe; -fx-border-radius:18 18 18 4; -fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.1),8,0,0,3);"
        );

        // Parse markdown-like bold (**text**)
        for (String line : text.split("\n")) {
            if (line.isBlank()) {
                Region spacer = new Region();
                spacer.setPrefHeight(4);
                bubble.getChildren().add(spacer);
                continue;
            }
            Label lbl = new Label(line.replaceAll("\\*\\*([^*]+)\\*\\*", "$1"));
            lbl.setWrapText(true);
            lbl.setMaxWidth(260);
            if (line.contains("**")) {
                lbl.setStyle("-fx-font-size:12; -fx-text-fill:#1e1b4b; -fx-font-weight:700;");
            } else {
                lbl.setStyle("-fx-font-size:12; -fx-text-fill:#374151;");
            }
            bubble.getChildren().add(lbl);
        }

        row.getChildren().addAll(avatar, bubble);
        messagesBox.getChildren().add(row);
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(260);
        bubble.setStyle(
            "-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
            "-fx-text-fill:white; -fx-font-size:12;" +
            "-fx-background-radius:18 18 4 18;" +
            "-fx-padding:12 16 12 16;" +
            "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.35),10,0,0,3);"
        );

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
    }

    private void addSuggestions(String[] suggestions) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:4 0 4 46;");

        for (String s : suggestions) {
            Button chip = new Button(s);
            chip.setStyle(
                "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                "-fx-font-size:11; -fx-font-weight:700;" +
                "-fx-padding:6 12; -fx-background-radius:20;" +
                "-fx-cursor:hand; -fx-border-width:1;" +
                "-fx-border-color:#ede9fe; -fx-border-radius:20;"
            );
            chip.setOnMouseEntered(e -> chip.setStyle(
                "-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white;" +
                "-fx-font-size:11; -fx-font-weight:700;" +
                "-fx-padding:6 12; -fx-background-radius:20;" +
                "-fx-cursor:hand; -fx-border-width:0;"
            ));
            chip.setOnMouseExited(e -> chip.setStyle(
                "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                "-fx-font-size:11; -fx-font-weight:700;" +
                "-fx-padding:6 12; -fx-background-radius:20;" +
                "-fx-cursor:hand; -fx-border-width:1;" +
                "-fx-border-color:#ede9fe; -fx-border-radius:20;"
            ));
            chip.setOnAction(e -> {
                inputField.setText(s.replaceAll("[📚🎉👥🏆👫]\\s*", ""));
                onSend();
            });
            row.getChildren().add(chip);
        }

        messagesBox.getChildren().add(row);
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }
}
