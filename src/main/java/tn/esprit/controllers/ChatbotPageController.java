package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import tn.esprit.components.AvatarView;
import tn.esprit.models.AvatarCustomization;
import tn.esprit.services.StudentAssistantExecutor;
import tn.esprit.services.StudentAssistantService;
import tn.esprit.session.JwtManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated Chatbot Page Controller
 * Full-page chat interface with large avatar and customization
 */
public class ChatbotPageController {

    @FXML private StackPane largeAvatarContainer;
    @FXML private StackPane smallAvatarContainer;
    @FXML private Label labelAvatarStatus;
    @FXML private Label labelChatStatus;
    @FXML private ScrollPane scrollPane;
    @FXML private VBox messagesBox;
    @FXML private TextField inputField;
    @FXML private Button btnSend;

    private final List<StudentAssistantService.ChatMessage> history = new ArrayList<>();
    private final StudentAssistantExecutor executor = new StudentAssistantExecutor();
    private boolean welcomeShown = false;
    
    private AvatarView largeAvatar;
    private AvatarView smallAvatar;
    private AvatarCustomization avatarCustomization;

    private java.util.function.Consumer<String> onNavigate;
    public void setOnNavigate(java.util.function.Consumer<String> cb) { this.onNavigate = cb; }

    @FXML
    public void initialize() {
        // Initialize avatar customization
        avatarCustomization = new AvatarCustomization();
        
        // Create large avatar (180x180)
        largeAvatar = new AvatarView(180);
        largeAvatar.setCustomization(avatarCustomization);
        if (largeAvatarContainer != null) {
            largeAvatarContainer.getChildren().add(largeAvatar);
        }
        
        // Create small avatar (48x48)
        smallAvatar = new AvatarView(48);
        smallAvatar.setCustomization(avatarCustomization);
        if (smallAvatarContainer != null) {
            smallAvatarContainer.getChildren().add(smallAvatar);
        }

        // Enter key to send
        if (inputField != null) {
            inputField.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) onSend();
            });
        }

        // Auto-scroll when messages change
        if (messagesBox != null) {
            messagesBox.heightProperty().addListener((obs, oldH, newH) -> scrollToBottom());
        }
        
        // Show welcome message
        Platform.runLater(this::showWelcome);
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

        // Add quick actions
        addQuickActions();
        
        // Start avatar idle animation
        if (largeAvatar != null) {
            largeAvatar.playAnimation(AvatarView.AnimationType.IDLE);
        }
        if (smallAvatar != null) {
            smallAvatar.playAnimation(AvatarView.AnimationType.IDLE);
        }
    }

    @FXML
    public void onSend() {
        if (inputField == null) return;
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        inputField.clear();
        if (btnSend != null) btnSend.setDisable(true);
        addUserMessage(text);
        history.add(new StudentAssistantService.ChatMessage("user", text));

        // Avatar thinking animation
        if (largeAvatar != null) {
            largeAvatar.playAnimation(AvatarView.AnimationType.THINKING);
        }
        if (smallAvatar != null) {
            smallAvatar.playAnimation(AvatarView.AnimationType.THINKING);
        }

        // Typing indicator
        VBox typingIndicator = addTypingIndicator();
        if (labelChatStatus != null) {
            labelChatStatus.setText("⏳ Réflexion...");
        }

        StudentAssistantService.sendMessage(text, history).thenAccept(response -> {
            Platform.runLater(() -> {
                if (messagesBox != null) {
                    messagesBox.getChildren().remove(typingIndicator);
                }
                if (labelChatStatus != null) {
                    labelChatStatus.setText("Prêt à vous aider");
                }
                
                // Avatar talking animation
                if (largeAvatar != null) {
                    largeAvatar.playAnimation(AvatarView.AnimationType.TALKING);
                }
                if (smallAvatar != null) {
                    smallAvatar.playAnimation(AvatarView.AnimationType.TALKING);
                }

                addBotMessage(response.message());
                history.add(new StudentAssistantService.ChatMessage("assistant", response.message()));

                if (!"CHAT".equals(response.intent())) {
                    executeAction(response.intent(), response.params());
                }

                if (btnSend != null) btnSend.setDisable(false);
                
                // Return to idle after talking
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(2000);
                        if (largeAvatar != null) {
                            largeAvatar.playAnimation(AvatarView.AnimationType.IDLE);
                        }
                        if (smallAvatar != null) {
                            smallAvatar.playAnimation(AvatarView.AnimationType.IDLE);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            });
        });
    }
    
    @FXML
    public void onCustomize() {
        openCustomizationDialog();
    }

    @FXML
    public void onClear() {
        if (messagesBox != null) {
            messagesBox.getChildren().clear();
        }
        history.clear();
        welcomeShown = false;
        showWelcome();
    }

    private void executeAction(String intent, com.google.gson.JsonObject params) {
        StudentAssistantExecutor.ActionResult result = executor.execute(intent, params);

        if (result.message() != null && !result.message().isEmpty()) {
            addBotMessage(result.message());
            history.add(new StudentAssistantService.ChatMessage("assistant", result.message()));
        }

        if (result.navigateTo() != null && onNavigate != null && intent.startsWith("NAVIGATE_")) {
            Platform.runLater(() -> onNavigate.accept(result.navigateTo()));
        }
    }

    private void addBotMessage(String text) {
        if (messagesBox == null) return;
        
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
        bubble.setMaxWidth(400);
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
            lbl.setMaxWidth(380);
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
        if (messagesBox == null) return;
        
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setStyle("-fx-padding:2 0 2 0;");

        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(400);
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
        if (messagesBox == null) return new VBox();
        
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

    private void addQuickActions() {
        if (messagesBox == null) return;
        
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
                if (inputField != null) {
                    inputField.setText(action[1]);
                    onSend();
                }
            });
            actionsBox.getChildren().add(chip);
        }

        messagesBox.getChildren().add(actionsBox);
    }

    private void scrollToBottom() {
        if (scrollPane == null) return;
        Platform.runLater(() -> Platform.runLater(() -> scrollPane.setVvalue(1.0)));
    }
    
    private void openCustomizationDialog() {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("✨ Personnaliser mon assistant");
        dialog.setResizable(false);
        
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#faf8ff;");
        
        // Header
        HBox header = new HBox(12);
        header.setStyle("-fx-background-color:linear-gradient(to right,#6d28d9,#7c3aed); -fx-padding:20 24;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("✨ Personnaliser mon assistant");
        title.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:white;");
        header.getChildren().add(title);
        
        // Preview
        AvatarView previewAvatar = new AvatarView(120);
        previewAvatar.setCustomization(avatarCustomization);
        StackPane previewPane = new StackPane(previewAvatar);
        previewPane.setStyle("-fx-padding:24; -fx-alignment:center;");
        
        // Options
        VBox options = new VBox(16);
        options.setStyle("-fx-padding:0 24 24 24;");
        
        // Hair Style
        options.getChildren().add(createOptionSection("💇 Coiffure", 
            new String[]{"short", "long", "curly", "ponytail", "bun"},
            new String[]{"Court", "Long", "Bouclé", "Queue", "Chignon"},
            avatarCustomization.getHairStyle(),
            value -> {
                avatarCustomization.setHairStyle(value);
                previewAvatar.setCustomization(avatarCustomization);
            }
        ));
        
        // Hair Color
        options.getChildren().add(createColorSection("🎨 Couleur cheveux",
            new String[]{"#7c3aed", "#1e40af", "#059669", "#f59e0b", "#dc2626", "#ec4899", "#1e1b4b"},
            avatarCustomization.getHairColor(),
            color -> {
                avatarCustomization.setHairColor(color);
                previewAvatar.setCustomization(avatarCustomization);
            }
        ));
        
        // Skin Tone
        options.getChildren().add(createOptionSection("👤 Teint",
            new String[]{"light", "medium", "tan", "dark"},
            new String[]{"Clair", "Moyen", "Bronzé", "Foncé"},
            avatarCustomization.getSkinTone(),
            value -> {
                avatarCustomization.setSkinTone(value);
                previewAvatar.setCustomization(avatarCustomization);
            }
        ));
        
        // Outfit
        options.getChildren().add(createOptionSection("👔 Tenue",
            new String[]{"casual", "professional", "sporty", "academic"},
            new String[]{"Décontracté", "Professionnel", "Sportif", "Académique"},
            avatarCustomization.getOutfit(),
            value -> {
                avatarCustomization.setOutfit(value);
                previewAvatar.setCustomization(avatarCustomization);
            }
        ));
        
        // Accessory
        options.getChildren().add(createOptionSection("🎭 Accessoire",
            new String[]{"none", "glasses", "hat", "headphones"},
            new String[]{"Aucun", "Lunettes", "Chapeau", "Casque"},
            avatarCustomization.getAccessory(),
            value -> {
                avatarCustomization.setAccessory(value);
                previewAvatar.setCustomization(avatarCustomization);
            }
        ));
        
        // Save button
        Button btnSave = new Button("💾 Enregistrer");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white; " +
                        "-fx-font-size:14; -fx-font-weight:700; -fx-padding:12; -fx-background-radius:10; -fx-cursor:hand;");
        btnSave.setOnAction(e -> {
            if (largeAvatar != null) {
                largeAvatar.setCustomization(avatarCustomization);
                largeAvatar.playAnimation(AvatarView.AnimationType.CELEBRATING);
            }
            if (smallAvatar != null) {
                smallAvatar.setCustomization(avatarCustomization);
                smallAvatar.playAnimation(AvatarView.AnimationType.CELEBRATING);
            }
            dialog.close();
        });
        options.getChildren().add(btnSave);
        
        ScrollPane scroll = new ScrollPane(options);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#faf8ff; -fx-background:#faf8ff;");
        
        root.getChildren().addAll(header, previewPane, scroll);
        
        javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 600);
        dialog.setScene(scene);
        dialog.show();
    }
    
    private VBox createOptionSection(String title, String[] values, String[] labels, String current, 
                                     java.util.function.Consumer<String> onChange) {
        VBox section = new VBox(8);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1b4b;");
        
        javafx.scene.layout.FlowPane buttons = new javafx.scene.layout.FlowPane(8, 8);
        javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
        
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            String label = labels[i];
            javafx.scene.control.ToggleButton btn = new javafx.scene.control.ToggleButton(label);
            btn.setToggleGroup(group);
            btn.setSelected(value.equals(current));
            btn.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed; -fx-font-size:11; " +
                        "-fx-font-weight:600; -fx-padding:6 12; -fx-background-radius:20; -fx-cursor:hand;");
            btn.selectedProperty().addListener((obs, old, selected) -> {
                if (selected) {
                    btn.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white; " +
                                "-fx-font-size:11; -fx-font-weight:700; -fx-padding:6 12; -fx-background-radius:20; -fx-cursor:hand;");
                    onChange.accept(value);
                } else {
                    btn.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed; -fx-font-size:11; " +
                                "-fx-font-weight:600; -fx-padding:6 12; -fx-background-radius:20; -fx-cursor:hand;");
                }
            });
            buttons.getChildren().add(btn);
        }
        
        section.getChildren().addAll(titleLabel, buttons);
        return section;
    }
    
    private VBox createColorSection(String title, String[] colors, String current, 
                                   java.util.function.Consumer<String> onChange) {
        VBox section = new VBox(8);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1b4b;");
        
        HBox colorButtons = new HBox(8);
        javafx.scene.control.ToggleGroup group = new javafx.scene.control.ToggleGroup();
        
        for (String color : colors) {
            javafx.scene.control.ToggleButton btn = new javafx.scene.control.ToggleButton();
            btn.setToggleGroup(group);
            btn.setSelected(color.equals(current));
            btn.setMinSize(36, 36);
            btn.setMaxSize(36, 36);
            String baseStyle = "-fx-background-color:" + color + "; -fx-background-radius:50%; -fx-cursor:hand; " +
                              "-fx-border-width:2; -fx-border-radius:50%;";
            btn.setStyle(baseStyle + "-fx-border-color:" + (color.equals(current) ? "#1e1b4b" : "transparent") + ";");
            btn.selectedProperty().addListener((obs, old, selected) -> {
                btn.setStyle(baseStyle + "-fx-border-color:" + (selected ? "#1e1b4b" : "transparent") + ";");
                if (selected) onChange.accept(color);
            });
            colorButtons.getChildren().add(btn);
        }
        
        section.getChildren().addAll(titleLabel, colorButtons);
        return section;
    }
}
