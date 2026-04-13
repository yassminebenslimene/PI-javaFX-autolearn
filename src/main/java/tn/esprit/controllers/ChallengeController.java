package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Exercice;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.ExerciceService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ChallengeController {

    @FXML private VBox challengesContainer;
    @FXML private TextField searchField;
    @FXML private Label emptyLabel;
    @FXML private Label successLabel;

    private ChallengeService challengeService;
    private ExerciceService exerciceService;
    private ObservableList<Challenge> masterChallengesList;
    private Challenge selectedChallenge;

    @FXML
    public void initialize() {
        challengeService = new ChallengeService();
        exerciceService = new ExerciceService();
        loadChallenges();
        setupSearchListener();
    }

    private void loadChallenges() {
        masterChallengesList = FXCollections.observableArrayList(challengeService.getAll());
        displayChallenges(masterChallengesList);
    }

    private void displayChallenges(List<Challenge> challenges) {
        challengesContainer.getChildren().clear();

        if (challenges.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (Challenge c : challenges) {
            HBox row = createChallengeRow(c);
            challengesContainer.getChildren().add(row);
        }
    }

    private HBox createChallengeRow(Challenge challenge) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;");
        row.setPrefHeight(50);

        row.setOnMouseEntered(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:rgba(255,255,255,0.03);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;"));

        // Titre
        Label titreLabel = new Label(challenge.getTitre());
        titreLabel.setStyle("-fx-text-fill:white; -fx-font-size:13;");
        titreLabel.setPrefWidth(200);
        titreLabel.setWrapText(true);

        // Description
        Label descriptionLabel = new Label(challenge.getDescription());
        descriptionLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:13;");
        descriptionLabel.setPrefWidth(250);
        descriptionLabel.setWrapText(true);

        // Niveau
        Label niveauLabel = new Label(challenge.getNiveau());
        String niveauColor = challenge.getNiveau().equals("Débutant") ? "#34d399" :
                (challenge.getNiveau().equals("Intermédiaire") ? "#fbbf24" : "#f87171");
        niveauLabel.setStyle("-fx-text-fill:" + niveauColor + "; -fx-font-size:13; -fx-font-weight:bold;");
        niveauLabel.setPrefWidth(100);

        // Durée
        Label dureeLabel = new Label(challenge.getDuree() + " min");
        dureeLabel.setStyle("-fx-text-fill:#34d399; -fx-font-size:13; -fx-font-weight:bold;");
        dureeLabel.setPrefWidth(80);

        // Actions
        HBox actionsBox = new HBox(12);
        actionsBox.setPrefWidth(150);

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#fbbf24; -fx-font-size:12; -fx-cursor:hand;");
        editBtn.setOnAction(e -> openChallengeForm(challenge, true));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#f87171; -fx-font-size:12; -fx-cursor:hand;");
        deleteBtn.setOnAction(e -> {
            selectedChallenge = challenge;
            deleteChallenge();
        });

        actionsBox.getChildren().addAll(editBtn, deleteBtn);
        row.getChildren().addAll(titreLabel, descriptionLabel, niveauLabel, dureeLabel, actionsBox);

        return row;
    }

    private void openChallengeForm(Challenge challenge, boolean isEdit) {
        try {
            String fxmlPath = "/views/backoffice/challenge/challenge_form_dialog.fxml";
            java.net.URL fxmlUrl = getClass().getResource(fxmlPath);

            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire non trouvé");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            DialogPane dialogPane = loader.load();
            ChallengeFormController formController = loader.getController();
            formController.setExerciceService(exerciceService);  // charger les exercices D'ABORD
            formController.setChallenge(challenge);               // puis pré-remplir le formulaire

            ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(isEdit ? "Modifier le challenge" : "Ajouter un challenge");
            dialog.initOwner(challengesContainer.getScene().getWindow());
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

            dialog.showAndWait().ifPresent(response -> {
                if (response == saveButton && formController.validateFields()) {
                    Challenge updatedChallenge = formController.getChallenge();
                    updatedChallenge.setCreatedBy(SessionManager.getCurrentUser().getId());

                    if (isEdit) {
                        challengeService.update(updatedChallenge);
                        showSuccessMessage("Challenge modifié avec succès !");
                    } else {
                        challengeService.add(updatedChallenge);
                        showSuccessMessage("Challenge ajouté avec succès !");
                    }
                    loadChallenges();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    @FXML
    public void addChallenge() {
        openChallengeForm(null, false);
    }

    @FXML
    public void deleteChallenge() {
        if (selectedChallenge == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un challenge à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le challenge");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer ce challenge ?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                challengeService.delete(selectedChallenge.getId());
                loadChallenges();
                selectedChallenge = null;
                showSuccessMessage("Challenge supprimé avec succès !");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    public void refreshChallenges() {
        loadChallenges();
        showSuccessMessage("La liste des challenges a été mise à jour.");
    }

    @FXML
    public void filterChallenges() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            displayChallenges(masterChallengesList);
        } else {
            List<Challenge> filtered = masterChallengesList.stream()
                    .filter(c -> c.getTitre().toLowerCase().contains(searchText) ||
                            c.getDescription().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
            displayChallenges(filtered);
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterChallenges());
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}