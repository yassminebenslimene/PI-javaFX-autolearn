package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Exercice;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.ExerciceService;
import tn.esprit.session.JwtManager;
import tn.esprit.services.ServiceQuiz;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChallengeController {

    @FXML private VBox challengesContainer;
    @FXML private TextField searchField;
    @FXML private Label emptyLabel;
    @FXML private Label successLabel;

    // Composants de recherche avancée
    @FXML private VBox advancedSearchPanel;
    @FXML private Button toggleSearchBtn;
    @FXML private TextField advancedTitreField;
    @FXML private ComboBox<String> advancedNiveauCombo;
    @FXML private ComboBox<String> advancedCreatedByCombo;

    private ChallengeService challengeService;
    private ExerciceService exerciceService;
    private ObservableList<Challenge> masterChallengesList;
    private Challenge selectedChallenge;
    private boolean isAdvancedSearchVisible = false;
    private Map<Integer, String> userNames = new HashMap<>();

    @FXML
    public void initialize() {
        challengeService = new ChallengeService();
        exerciceService = new ExerciceService();
        loadChallenges();
        initAdvancedSearch();
        setupSearchListener();
    }

    private void initAdvancedSearch() {
        // Initialiser les niveaux
        advancedNiveauCombo.setItems(FXCollections.observableArrayList("Tous", "Débutant", "Intermédiaire", "Avancé"));
        advancedNiveauCombo.setValue("Tous");

        // Appliquer le style sombre avec texte blanc
        String comboStyle = "-fx-background-color:rgba(255,255,255,0.08); " +
                "-fx-border-color:rgba(255,255,255,0.15); " +
                "-fx-border-radius:8; -fx-background-radius:8; " +
                "-fx-padding:5 10 5 10; -fx-font-size:13; " +
                "-fx-text-fill:white;";

        advancedNiveauCombo.setStyle(comboStyle);
        advancedCreatedByCombo.setStyle(comboStyle);

        // Pour que les items dans la liste aient aussi fond sombre et texte blanc
        advancedNiveauCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:white; -fx-background-color:#1a1a2e; -fx-padding:5 10 5 10;");
                }
            }
        });

        advancedCreatedByCombo.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:white; -fx-background-color:#1a1a2e; -fx-padding:5 10 5 10;");
                }
            }
        });

        // Pour le texte sélectionné affiché
        advancedNiveauCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:white;");
                }
            }
        });

        advancedCreatedByCombo.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill:white;");
                }
            }
        });

        // Charger la liste des créateurs
        loadCreators();

        // Panneau caché par défaut
        advancedSearchPanel.setVisible(false);
        advancedSearchPanel.setManaged(false);
    }

    private void loadCreators() {
        List<Challenge> allChallenges = challengeService.getAll();

        List<Integer> creatorIds = allChallenges.stream()
                .map(Challenge::getCreatedBy)
                .distinct()
                .collect(Collectors.toList());

        userNames.clear();
        for (Integer id : creatorIds) {
            String userName = getUserNameById(id);
            userNames.put(id, userName);
        }

        ObservableList<String> creatorNames = FXCollections.observableArrayList();
        creatorNames.add("Tous");
        creatorNames.addAll(userNames.values().stream().sorted().collect(Collectors.toList()));
        advancedCreatedByCombo.setItems(creatorNames);
        advancedCreatedByCombo.setValue("Tous");
    }

    private String getUserNameById(int userId) {
        try {
            // À remplacer par votre service utilisateur
            // UserService userService = new UserService();
            // User user = userService.getById(userId);
            // return user.getPrenom() + " " + user.getNom();
            return "Utilisateur " + userId;
        } catch (Exception e) {
            return "Inconnu";
        }
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

        // Créé par
        String creatorName = userNames.getOrDefault(challenge.getCreatedBy(), "Inconnu");
        Label creatorLabel = new Label(creatorName);
        creatorLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12;");
        creatorLabel.setPrefWidth(120);

        // Actions
        HBox actionsBox = new HBox(12);
        actionsBox.setPrefWidth(150);
        actionsBox.setAlignment(Pos.CENTER);

        Button btnView = new Button("View");
        btnView.setStyle("-fx-background-color:rgba(59,130,246,0.25); -fx-text-fill:#60a5fa; " +
                "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnView.setOnAction(e -> viewChallenge(challenge));

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24; " +
                "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        editBtn.setOnAction(e -> openChallengeForm(challenge, true));

        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af; " +
                "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        deleteBtn.setOnAction(e -> {
            selectedChallenge = challenge;
            deleteChallenge();
        });

        actionsBox.getChildren().addAll(btnView, editBtn, deleteBtn);
        row.getChildren().addAll(titreLabel, descriptionLabel, niveauLabel, dureeLabel, creatorLabel, actionsBox);

        return row;
    }

    @FXML
    private void toggleAdvancedSearch() {
        isAdvancedSearchVisible = !isAdvancedSearchVisible;
        advancedSearchPanel.setVisible(isAdvancedSearchVisible);
        advancedSearchPanel.setManaged(isAdvancedSearchVisible);

        if (isAdvancedSearchVisible) {
            toggleSearchBtn.setText("✖ Fermer recherche");
        } else {
            toggleSearchBtn.setText("🔍 Recherche avancée");
            clearAdvancedSearch();
            filterChallenges();
        }
    }

    @FXML
    private void applyAdvancedSearch() {
        String titre = advancedTitreField.getText().trim().toLowerCase();
        String niveau = advancedNiveauCombo.getValue();
        String createdByName = advancedCreatedByCombo.getValue();

        Integer createdById = null;
        if (createdByName != null && !createdByName.equals("Tous")) {
            for (Map.Entry<Integer, String> entry : userNames.entrySet()) {
                if (entry.getValue().equals(createdByName)) {
                    createdById = entry.getKey();
                    break;
                }
            }
        }

        final Integer finalCreatedById = createdById;
        final String finalNiveau = niveau != null && !niveau.equals("Tous") ? niveau : null;

        List<Challenge> filtered = masterChallengesList.stream()
                .filter(c -> {
                    if (!titre.isEmpty() && !c.getTitre().toLowerCase().contains(titre)) {
                        return false;
                    }
                    if (finalNiveau != null && !c.getNiveau().equals(finalNiveau)) {
                        return false;
                    }
                    if (finalCreatedById != null && c.getCreatedBy() != finalCreatedById) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        displayChallenges(filtered);

        if (filtered.isEmpty()) {
            showSuccessMessage("Aucun challenge trouvé pour ces critères.");
        } else {
            showSuccessMessage(filtered.size() + " challenge(s) trouvé(s).");
        }
    }

    @FXML
    private void clearAdvancedSearch() {
        advancedTitreField.clear();
        advancedNiveauCombo.setValue("Tous");
        advancedCreatedByCombo.setValue("Tous");
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

            // Ajouter les services
            formController.setExerciceService(exerciceService);
            formController.setQuizService(new ServiceQuiz());  // ← AJOUTER CETTE LIGNE

            // Puis charger le challenge
            formController.setChallenge(challenge);

            ButtonType saveButton = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(isEdit ? "Modifier le challenge" : "Ajouter un challenge");
            dialog.initOwner(challengesContainer.getScene().getWindow());
            dialog.getDialogPane().getButtonTypes().addAll(saveButton, cancelButton);

            dialog.getDialogPane().lookupButton(saveButton).setStyle(
                    "-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            );
            dialog.getDialogPane().lookupButton(cancelButton).setStyle(
                    "-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold; " +
                            "-fx-padding:11 24 11 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"
            );

            Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveButton);
            saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!formController.validateFields()) {
                    event.consume();
                }
            });

            dialog.showAndWait().ifPresent(response -> {
                if (response == saveButton) {
                    Challenge updatedChallenge = formController.getChallenge();
                    updatedChallenge.setCreatedBy(JwtManager.getCurrentUser().getId());

                    if (isEdit) {
                        challengeService.update(updatedChallenge);
                        var admin = JwtManager.getCurrentUser();
                        if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_challenge",
                            java.util.Map.of("titre", updatedChallenge.getTitre() != null ? updatedChallenge.getTitre() : ""));
                        showSuccessMessage("Challenge modifié avec succès !");
                    } else {
                        challengeService.add(updatedChallenge);
                        var admin = JwtManager.getCurrentUser();
                        if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_challenge",
                            java.util.Map.of("titre", updatedChallenge.getTitre() != null ? updatedChallenge.getTitre() : ""));
                        showSuccessMessage("Challenge ajouté avec succès !");
                    }
                    loadChallenges();
                    loadCreators();
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
    public void generateChallengeWithAI() {
        // Dialog to select existing exercises and quizzes
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("🤖 Générer un Challenge");
        dialog.setHeaderText("Créer un challenge en utilisant les exercices et quiz existants");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(16);
        content.setStyle("-fx-padding:24; -fx-background-color:#0a0f0d;");

        // Challenge info
        javafx.scene.control.TextField titreField = new javafx.scene.control.TextField();
        titreField.setPromptText("Titre du challenge");
        titreField.setStyle("-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.2); -fx-border-radius:8; -fx-background-radius:8; -fx-padding:10;");

        javafx.scene.control.TextArea descriptionArea = new javafx.scene.control.TextArea();
        descriptionArea.setPromptText("Description du challenge");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setStyle("-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.2); -fx-border-radius:8; -fx-background-radius:8; -fx-padding:10;");

        javafx.scene.control.ComboBox<String> niveauCombo = new javafx.scene.control.ComboBox<>();
        niveauCombo.getItems().addAll("Débutant", "Intermédiaire", "Avancé");
        niveauCombo.setValue("Intermédiaire");
        niveauCombo.setStyle("-fx-background-color:rgba(255,255,255,0.08); -fx-text-fill:white; -fx-border-color:rgba(255,255,255,0.2); -fx-border-radius:8; -fx-background-radius:8; -fx-padding:8;");

        javafx.scene.control.Spinner<Integer> dureeSpinner = new javafx.scene.control.Spinner<>(10, 180, 60, 10);
        dureeSpinner.setStyle("-fx-background-color:rgba(255,255,255,0.08);");

        // Load existing exercises and quizzes
        List<Exercice> allExercices = exerciceService.getAll();
        ServiceQuiz quizService = new ServiceQuiz();
        List<tn.esprit.entities.Quiz> allQuizzes = quizService.afficher();

        // Exercices selection
        javafx.scene.control.Label exercicesLabel = styledLabel("Sélectionner les exercices (" + allExercices.size() + " disponibles) :");
        javafx.scene.layout.FlowPane exercicesPane = new javafx.scene.layout.FlowPane(8, 8);
        exercicesPane.setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-padding:12; -fx-background-radius:8;");
        exercicesPane.setMaxHeight(150);
        
        javafx.scene.control.ScrollPane exercicesScroll = new javafx.scene.control.ScrollPane(exercicesPane);
        exercicesScroll.setFitToWidth(true);
        exercicesScroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        exercicesScroll.setPrefHeight(150);

        List<javafx.scene.control.CheckBox> exerciceCheckBoxes = new java.util.ArrayList<>();
        for (Exercice ex : allExercices) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(
                "Ex #" + ex.getId() + ": " + (ex.getQuestion().length() > 40 ? 
                    ex.getQuestion().substring(0, 37) + "..." : ex.getQuestion())
            );
            cb.setStyle("-fx-text-fill:white; -fx-font-size:12;");
            cb.setUserData(ex);
            exerciceCheckBoxes.add(cb);
            exercicesPane.getChildren().add(cb);
        }

        // Quizzes selection
        javafx.scene.control.Label quizzesLabel = styledLabel("Sélectionner les quiz (" + allQuizzes.size() + " disponibles) :");
        javafx.scene.layout.FlowPane quizzesPane = new javafx.scene.layout.FlowPane(8, 8);
        quizzesPane.setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-padding:12; -fx-background-radius:8;");
        quizzesPane.setMaxHeight(150);
        
        javafx.scene.control.ScrollPane quizzesScroll = new javafx.scene.control.ScrollPane(quizzesPane);
        quizzesScroll.setFitToWidth(true);
        quizzesScroll.setStyle("-fx-background:transparent; -fx-background-color:transparent;");
        quizzesScroll.setPrefHeight(150);

        List<javafx.scene.control.CheckBox> quizCheckBoxes = new java.util.ArrayList<>();
        for (tn.esprit.entities.Quiz quiz : allQuizzes) {
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox(
                "Quiz #" + quiz.getId() + ": " + (quiz.getTitre().length() > 40 ? 
                    quiz.getTitre().substring(0, 37) + "..." : quiz.getTitre())
            );
            cb.setStyle("-fx-text-fill:white; -fx-font-size:12;");
            cb.setUserData(quiz);
            quizCheckBoxes.add(cb);
            quizzesPane.getChildren().add(cb);
        }

        content.getChildren().addAll(
            styledLabel("Titre du challenge :"),
            titreField,
            styledLabel("Description :"),
            descriptionArea,
            styledLabel("Niveau :"),
            niveauCombo,
            styledLabel("Durée (minutes) :"),
            dureeSpinner,
            new javafx.scene.control.Separator(),
            exercicesLabel,
            exercicesScroll,
            quizzesLabel,
            quizzesScroll
        );

        javafx.scene.control.ScrollPane mainScroll = new javafx.scene.control.ScrollPane(content);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background:transparent; -fx-background-color:#0a0f0d;");
        mainScroll.setPrefHeight(600);

        dialog.getDialogPane().setContent(mainScroll);
        dialog.getDialogPane().setStyle("-fx-background-color:#0a0f0d;");
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().getButtonTypes().addAll(
            new javafx.scene.control.ButtonType("✅ Créer Challenge", javafx.scene.control.ButtonBar.ButtonData.OK_DONE),
            new javafx.scene.control.ButtonType("Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        dialog.showAndWait().ifPresent(response -> {
            if (response.getButtonData() == javafx.scene.control.ButtonBar.ButtonData.OK_DONE) {
                String titre = titreField.getText().trim();
                String description = descriptionArea.getText().trim();
                
                if (titre.isEmpty()) {
                    showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Attention", "Veuillez entrer un titre.");
                    return;
                }

                // Collect selected exercises
                List<Integer> selectedExerciceIds = new java.util.ArrayList<>();
                for (javafx.scene.control.CheckBox cb : exerciceCheckBoxes) {
                    if (cb.isSelected()) {
                        Exercice ex = (Exercice) cb.getUserData();
                        selectedExerciceIds.add(ex.getId());
                    }
                }

                // Collect selected quizzes
                List<Integer> selectedQuizIds = new java.util.ArrayList<>();
                for (javafx.scene.control.CheckBox cb : quizCheckBoxes) {
                    if (cb.isSelected()) {
                        tn.esprit.entities.Quiz quiz = (tn.esprit.entities.Quiz) cb.getUserData();
                        selectedQuizIds.add(quiz.getId());
                    }
                }

                if (selectedExerciceIds.isEmpty() && selectedQuizIds.isEmpty()) {
                    showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Attention", 
                        "Veuillez sélectionner au moins un exercice ou un quiz.");
                    return;
                }

                // Create the challenge
                Challenge newChallenge = new Challenge();
                newChallenge.setTitre(titre);
                newChallenge.setDescription(description.isEmpty() ? "Challenge créé avec exercices et quiz existants" : description);
                newChallenge.setNiveau(niveauCombo.getValue());
                newChallenge.setDuree(dureeSpinner.getValue());
                newChallenge.setCreatedBy(JwtManager.getCurrentUser().getId());
                newChallenge.setExerciceIds(selectedExerciceIds);
                newChallenge.setQuizIds(selectedQuizIds);

                try {
                    challengeService.add(newChallenge);
                    var admin = JwtManager.getCurrentUser();
                    if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_challenge",
                        java.util.Map.of("titre", newChallenge.getTitre()));
                    
                    loadChallenges();
                    loadCreators();
                    showSuccessMessage("✅ Challenge \"" + titre + "\" créé avec " + 
                        selectedExerciceIds.size() + " exercice(s) et " + 
                        selectedQuizIds.size() + " quiz !");
                } catch (Exception e) {
                    showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Erreur", 
                        "Impossible de créer le challenge : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private javafx.scene.control.Label styledLabel(String text) {
        javafx.scene.control.Label l = new javafx.scene.control.Label(text);
        l.setStyle("-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12; -fx-font-weight:600;");
        return l;
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
                var admin = JwtManager.getCurrentUser();
                if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.deleted_challenge",
                    java.util.Map.of("titre", selectedChallenge.getTitre() != null ? selectedChallenge.getTitre() : ""));
                challengeService.delete(selectedChallenge.getId());
                loadChallenges();
                selectedChallenge = null;
                showSuccessMessage("Challenge supprimé avec succès !");
                loadCreators();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    public void refreshChallenges() {
        loadChallenges();
        loadCreators();
        showSuccessMessage("La liste des challenges a été mise à jour.");
    }

    @FXML
    public void filterChallenges() {
        if (isAdvancedSearchVisible) {
            return;
        }

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

    @FXML
    private void viewChallenge(Challenge challenge) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/challenge/challenge_detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails du Challenge - " + challenge.getTitre());
            stage.setScene(new Scene(loader.load(), 600, 500));
            stage.setResizable(false);

            ChallengeDetailsController controller = loader.getController();
            controller.setChallenge(challenge);

            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails du challenge");
        }
    }
}