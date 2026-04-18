package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Exercice;
import tn.esprit.services.ExerciceService;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.Modality;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import tn.esprit.services.AIService;
public class ExerciceController {

    @FXML private VBox exercicesContainer;
    @FXML private TextField searchField;
    @FXML private Label emptyLabel;
    @FXML private Label successLabel;

    private ExerciceService exerciceService;
    private ObservableList<Exercice> masterExercicesList;
    private Exercice selectedExercice;
    @FXML private Button aiGenerateBtn;
    @FXML private VBox aiPanel;
    @FXML private TextField aiTopicField;
    @FXML private ComboBox<Integer> aiCountCombo;
    @FXML private ComboBox<String> aiDifficultyCombo;
    @FXML private ProgressIndicator aiProgress;
    @FXML private Label aiStatusLabel;

    private boolean isAIPanelVisible = false;

    @FXML
    public void initialize() {
        exerciceService = new ExerciceService();
        loadExercices();
        setupSearchListener();
        initAIPanel();
    }

    private void loadExercices() {
        masterExercicesList = FXCollections.observableArrayList(exerciceService.getAll());
        displayExercices(masterExercicesList);
    }

    private void displayExercices(List<Exercice> exercices) {
        exercicesContainer.getChildren().clear();

        if (exercices.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (Exercice e : exercices) {
            HBox row = createExerciseRow(e);
            exercicesContainer.getChildren().add(row);
        }
    }

    private HBox createExerciseRow(Exercice exercice) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;");
        row.setPrefHeight(50);

        row.setOnMouseEntered(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:rgba(255,255,255,0.03);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;"));

        Label questionLabel = new Label(exercice.getQuestion());
        questionLabel.setStyle("-fx-text-fill:white; -fx-font-size:13;");
        questionLabel.setPrefWidth(400);
        questionLabel.setWrapText(true);

        Label reponseLabel = new Label(exercice.getReponse());
        reponseLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:13;");
        reponseLabel.setPrefWidth(300);
        reponseLabel.setWrapText(true);

        Label pointsLabel = new Label(String.valueOf(exercice.getPoints()));
        pointsLabel.setStyle("-fx-text-fill:#34d399; -fx-font-size:13; -fx-font-weight:bold;");
        pointsLabel.setPrefWidth(80);

        HBox actionsBox = new HBox(12);
        actionsBox.setPrefWidth(150);
        actionsBox.setAlignment(Pos.CENTER);

        // Bouton View (AJOUTÉ)
        Button viewBtn = new Button("View");
        viewBtn.setStyle("-fx-background-color:rgba(59,130,246,0.25); -fx-text-fill:#60a5fa; " +
                "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        viewBtn.setOnAction(e -> viewExercice(exercice));

        // Bouton Edit
        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24; " +
                "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        editBtn.setOnAction(e -> openExerciceForm(exercice, true));

        // Bouton Delete
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af; " +
                "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        deleteBtn.setOnAction(e -> {
            selectedExercice = exercice;
            deleteExercice();
        });

        actionsBox.getChildren().addAll(viewBtn, editBtn, deleteBtn);
        row.getChildren().addAll(questionLabel, reponseLabel, pointsLabel, actionsBox);

        return row;
    }

    /**
     * Ouvre la fenêtre modale pour ajouter/modifier un exercice
     */
    private void openExerciceForm(Exercice exercice, boolean isEdit) {
        try {
            String fxmlPath = "/views/backoffice/exercice/exercice_form_dialog.fxml";
            java.net.URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire non trouvé");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            DialogPane dialogPane = loader.load();
            ExerciceFormController formController = loader.getController();
            formController.setExercice(exercice);

            ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(isEdit ? "Modifier l'exercice" : "Ajouter un exercice");
            dialog.initOwner(exercicesContainer.getScene().getWindow());
            dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

            // Style des boutons
            dialog.getDialogPane().lookupButton(saveButton).setStyle(
                    "-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            );
            dialog.getDialogPane().lookupButton(cancelButton).setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            );

            // Empêcher la fermeture automatique du dialog
            Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButton);
            saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!formController.validateFields()) {
                    event.consume(); // Empêche la fermeture du dialog
                }
            });

            dialog.showAndWait().ifPresent(response -> {
                if (response == saveButton) {
                    Exercice updatedExercice = formController.getExercice();
                    if (isEdit && exercice != null) {
                        exerciceService.update(updatedExercice);
                        showSuccessMessage("Exercice modifié avec succès !");
                    } else {
                        exerciceService.add(updatedExercice);
                        showSuccessMessage("Exercice ajouté avec succès !");
                    }
                    loadExercices();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    @FXML
    public void addExercice() {
        openExerciceForm(null, false);
    }

    @FXML
    public void deleteExercice() {
        if (selectedExercice == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un exercice à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'exercice");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cet exercice ?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                exerciceService.delete(selectedExercice.getId());
                loadExercices();
                selectedExercice = null;
                showSuccessMessage("Exercice supprimé avec succès !");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    public void refreshExercices() {
        loadExercices();
        showSuccessMessage("La liste des exercices a été mise à jour.");
    }

    @FXML
    public void filterExercises() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            displayExercices(masterExercicesList);
        } else {
            List<Exercice> filtered = masterExercicesList.stream()
                    .filter(e -> e.getQuestion().toLowerCase().contains(searchText) ||
                            e.getReponse().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            displayExercices(filtered);
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterExercises());
    }

    private void showSuccessMessage(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);

        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> {
                    successLabel.setVisible(false);
                    successLabel.setManaged(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void viewExercice(Exercice exercice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/exercice/exercice_detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails de l'exercice");
            stage.setScene(new Scene(loader.load(), 550, 400));
            stage.setResizable(false);

            ExerciceDetailController controller = loader.getController();
            controller.setExercice(exercice);

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails de l'exercice");
        }
    }
    private void initAIPanel() {
        // Initialiser le ComboBox pour le nombre d'exercices
        ObservableList<Integer> counts = FXCollections.observableArrayList(1, 2, 3, 5, 10, 15, 20);
        aiCountCombo.setItems(counts);
        aiCountCombo.setValue(5);

        // Initialiser le ComboBox pour la difficulté
        ObservableList<String> difficulties = FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé", "Expert");
        aiDifficultyCombo.setItems(difficulties);
        aiDifficultyCombo.setValue("Intermédiaire");

        // Style pour rendre le texte blanc dans les ComboBox
        String comboStyle = "-fx-background-color:rgba(255,255,255,0.08); " +
                "-fx-border-color:rgba(255,255,255,0.15); " +
                "-fx-border-radius:8; -fx-background-radius:8; " +
                "-fx-padding:5 10 5 10; -fx-font-size:13; " +
                "-fx-text-fill:white;";  // Texte blanc

        aiCountCombo.setStyle(comboStyle);
        aiDifficultyCombo.setStyle(comboStyle);

        // Style pour le texte sélectionné affiché (bouton)
        aiCountCombo.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill:white; -fx-background-color:transparent;");
                }
            }
        });

        aiDifficultyCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:white; -fx-background-color:transparent;");
                }
            }
        });

        // Style pour les items dans la liste déroulante
        aiCountCombo.setCellFactory(lv -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill:white; -fx-background-color:#1a1a2e;");
                }
            }
        });

        aiDifficultyCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:white; -fx-background-color:#1a1a2e;");
                }
            }
        });

        // Style du prompt text (placeholder)
        aiTopicField.setStyle("-fx-background-color:rgba(255,255,255,0.08); " +
                "-fx-border-color:rgba(255,255,255,0.15); " +
                "-fx-border-radius:8; -fx-background-radius:8; " +
                "-fx-padding:10 14 10 14; -fx-font-size:13; " +
                "-fx-text-fill:white; -fx-prompt-text-fill:rgba(255,255,255,0.4);");

        aiPanel.setVisible(false);
        aiPanel.setManaged(false);
    }

    @FXML
    private void generateAIExercises() {
        isAIPanelVisible = !isAIPanelVisible;
        aiPanel.setVisible(isAIPanelVisible);
        aiPanel.setManaged(isAIPanelVisible);
    }

    @FXML
    private void cancelAIGeneration() {
        aiPanel.setVisible(false);
        aiPanel.setManaged(false);
        isAIPanelVisible = false;
        aiTopicField.clear();
        aiStatusLabel.setText("");
    }

    @FXML
    private void executeAIGeneration() {
        String topic = aiTopicField.getText().trim();
        if (topic.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer un domaine/sujet.");
            return;
        }

        int count = aiCountCombo.getValue();
        String difficulty = aiDifficultyCombo.getValue();

        // Désactiver les boutons pendant la génération
        aiGenerateBtn.setDisable(true);
        aiProgress.setVisible(true);
        aiProgress.setManaged(true);
        aiStatusLabel.setText("🤖 Génération d'exercices IA en cours...");

        new Thread(() -> {
            AIService aiService = new AIService();
            List<AIService.AIExercise> generatedExercises = aiService.generateExercises(topic, count, difficulty);

            javafx.application.Platform.runLater(() -> {
                aiProgress.setVisible(false);
                aiProgress.setManaged(false);
                aiGenerateBtn.setDisable(false);
                aiStatusLabel.setText("");

                if (generatedExercises.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun exercice généré.");
                    return;
                }

                // Ajouter les exercices générés à la base de données
                int addedCount = 0;
                for (AIService.AIExercise aiEx : generatedExercises) {
                    Exercice exercice = new Exercice(aiEx.getQuestion(), aiEx.getAnswer(), aiEx.getPoints());
                    exerciceService.add(exercice);
                    addedCount++;
                }

                // Rafraîchir l'affichage
                loadExercices();

                // Fermer le panneau IA
                aiPanel.setVisible(false);
                aiPanel.setManaged(false);
                isAIPanelVisible = false;
                aiTopicField.clear();

                showSuccessMessage(addedCount + " exercices générés par IA avec succès !");
            });
        }).start();
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}