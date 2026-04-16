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

public class ExerciceController {

    @FXML private VBox exercicesContainer;
    @FXML private TextField searchField;
    @FXML private Label emptyLabel;
    @FXML private Label successLabel;

    private ExerciceService exerciceService;
    private ObservableList<Exercice> masterExercicesList;
    private Exercice selectedExercice;

    @FXML
    public void initialize() {
        exerciceService = new ExerciceService();
        loadExercices();
        setupSearchListener();
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

            TextField txtQuestion = (TextField) dialogPane.lookup("#txtQuestion");
            TextField txtReponse = (TextField) dialogPane.lookup("#txtReponse");
            TextField txtPoints = (TextField) dialogPane.lookup("#txtPoints");
            Label dialogTitle = (Label) dialogPane.lookup("#dialogTitle");

            if (isEdit && exercice != null) {
                dialogTitle.setText("Modifier l'exercice");
                txtQuestion.setText(exercice.getQuestion());
                txtReponse.setText(exercice.getReponse());
                txtPoints.setText(String.valueOf(exercice.getPoints()));
            } else {
                dialogTitle.setText("Ajouter un exercice");
            }

            ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(isEdit ? "Modifier l'exercice" : "Ajouter un exercice");
            dialog.initOwner(exercicesContainer.getScene().getWindow());
            dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

            dialog.getDialogPane().lookupButton(saveButton).setStyle(
                    "-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            );
            dialog.getDialogPane().lookupButton(cancelButton).setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            );

            Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButton);
            cancelBtn.setOnMouseEntered(e -> cancelBtn.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.15); -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            ));
            cancelBtn.setOnMouseExited(e -> cancelBtn.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            ));

            dialog.showAndWait().ifPresent(response -> {
                if (response == saveButton) {
                    String question = txtQuestion.getText().trim();
                    String reponse = txtReponse.getText().trim();
                    String pointsStr = txtPoints.getText().trim();

                    if (question.isEmpty() || reponse.isEmpty() || pointsStr.isEmpty()) {
                        showAlert(Alert.AlertType.WARNING, "Attention", "Tous les champs sont obligatoires");
                        return;
                    }

                    try {
                        int points = Integer.parseInt(pointsStr);
                        if (points <= 0) {
                            showAlert(Alert.AlertType.WARNING, "Attention", "Les points doivent être positifs");
                            return;
                        }

                        if (isEdit && exercice != null) {
                            exercice.setQuestion(question);
                            exercice.setReponse(reponse);
                            exercice.setPoints(points);
                            exerciceService.update(exercice);
                            showSuccessMessage("Exercice modifié avec succès !");
                        } else {
                            Exercice newExercice = new Exercice(question, reponse, points);
                            exerciceService.add(newExercice);
                            showSuccessMessage("Exercice ajouté avec succès !");
                        }
                        loadExercices();

                    } catch (NumberFormatException e) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Les points doivent être un nombre valide");
                    }
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}