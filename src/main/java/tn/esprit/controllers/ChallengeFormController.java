package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Exercice;
import tn.esprit.services.ExerciceService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChallengeFormController {

    @FXML private TextField txtTitre;
    @FXML private TextField txtDescription;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private TextField txtDuree;
    @FXML private TextField txtId;

    @FXML private FlowPane exercicesContainer;

    @FXML private Label errorTitre;
    @FXML private Label errorDescription;
    @FXML private Label errorDateDebut;
    @FXML private Label errorDateFin;
    @FXML private Label errorNiveau;
    @FXML private Label errorDuree;
    @FXML private Label dialogTitle;

    private Challenge challenge;
    private ExerciceService exerciceService;
    private List<ExerciceCard> exerciceCards = new ArrayList<>();
    private boolean isEditMode = false;

    // Classe interne pour gérer les cartes d'exercices
    private class ExerciceCard extends VBox {
        private Exercice exercice;
        private boolean selected = false;
        private Label questionLabel;
        private Label pointsLabel;

        public ExerciceCard(Exercice exercice) {
            this.exercice = exercice;

            setPrefWidth(180);
            setMinWidth(160);
            setMaxWidth(200);
            setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-border-color:rgba(255,255,255,0.15); " +
                    "-fx-border-radius:10; -fx-background-radius:10; -fx-padding:12; -fx-cursor:hand;");

            questionLabel = new Label(exercice.getQuestion());
            questionLabel.setWrapText(true);
            questionLabel.setStyle("-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold;");

            pointsLabel = new Label("⭐ " + exercice.getPoints() + " points");
            pointsLabel.setStyle("-fx-text-fill:#34d399; -fx-font-size:11;");

            VBox.setMargin(questionLabel, new Insets(0, 0, 8, 0));

            getChildren().addAll(questionLabel, pointsLabel);

            // Gestion du clic
            setOnMouseClicked(e -> toggleSelection());
        }

        public void toggleSelection() {
            selected = !selected;
            if (selected) {
                setStyle("-fx-background-color:rgba(5,150,105,0.2); -fx-border-color:#059669; " +
                        "-fx-border-radius:10; -fx-background-radius:10; -fx-padding:12; -fx-cursor:hand;");
            } else {
                setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-border-color:rgba(255,255,255,0.15); " +
                        "-fx-border-radius:10; -fx-background-radius:10; -fx-padding:12; -fx-cursor:hand;");
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                setStyle("-fx-background-color:rgba(5,150,105,0.2); -fx-border-color:#059669; " +
                        "-fx-border-radius:10; -fx-background-radius:10; -fx-padding:12; -fx-cursor:hand;");
            } else {
                setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-border-color:rgba(255,255,255,0.15); " +
                        "-fx-border-radius:10; -fx-background-radius:10; -fx-padding:12; -fx-cursor:hand;");
            }
        }

        public boolean isSelected() {
            return selected;
        }

        public Exercice getExercice() {
            return exercice;
        }
    }

    @FXML
    public void initialize() {
        // Initialiser les niveaux
        comboNiveau.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));

        // Style personnalisé pour le ComboBox
        comboNiveau.setButtonCell(new ListCell<String>() {
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

        comboNiveau.setCellFactory(lv -> new ListCell<String>() {
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

        // Validation de la durée
        txtDuree.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtDuree.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
    }

    public void setExerciceService(ExerciceService service) {
        this.exerciceService = service;
        loadExercices();
    }

    private void loadExercices() {
        if (exerciceService != null) {
            List<Exercice> allExercices = exerciceService.getAll();
            exercicesContainer.getChildren().clear();
            exerciceCards.clear();

            for (Exercice e : allExercices) {
                ExerciceCard card = new ExerciceCard(e);
                exerciceCards.add(card);
                exercicesContainer.getChildren().add(card);
            }
        }
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
        if (challenge != null) {
            isEditMode = true;
            dialogTitle.setText("Modifier le challenge");
            txtId.setText(String.valueOf(challenge.getId()));
            txtTitre.setText(challenge.getTitre());
            txtDescription.setText(challenge.getDescription());
            dateDebut.setValue(challenge.getDateDebut());
            dateFin.setValue(challenge.getDateFin());
            comboNiveau.setValue(challenge.getNiveau());
            txtDuree.setText(String.valueOf(challenge.getDuree()));

            // Sélectionner les exercices déjà associés
            for (Integer exerciceId : challenge.getExerciceIds()) {
                for (ExerciceCard card : exerciceCards) {
                    if (card.getExercice().getId() == exerciceId) {
                        card.setSelected(true);
                        break;
                    }
                }
            }
        } else {
            isEditMode = false;
            dialogTitle.setText("Ajouter un challenge");
            this.challenge = new Challenge();
        }
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public boolean validateFields() {
        boolean isValid = true;

        // Reset errors
        errorTitre.setText("");
        errorDescription.setText("");
        errorDateDebut.setText("");
        errorDateFin.setText("");
        errorNiveau.setText("");
        errorDuree.setText("");

        String titre = txtTitre.getText().trim();
        if (titre.isEmpty()) {
            errorTitre.setText("Le titre ne peut pas être vide");
            isValid = false;
        }

        String description = txtDescription.getText().trim();
        if (description.isEmpty()) {
            errorDescription.setText("La description ne peut pas être vide");
            isValid = false;
        }

        LocalDate debut = dateDebut.getValue();
        if (debut == null) {
            errorDateDebut.setText("La date de début est obligatoire");
            isValid = false;
        }

        LocalDate fin = dateFin.getValue();
        if (fin == null) {
            errorDateFin.setText("La date de fin est obligatoire");
            isValid = false;
        } else if (debut != null && fin.isBefore(debut)) {
            errorDateFin.setText("La date de fin doit être après la date de début");
            isValid = false;
        }

        String niveau = comboNiveau.getValue();
        if (niveau == null || niveau.isEmpty()) {
            errorNiveau.setText("Veuillez sélectionner un niveau");
            isValid = false;
        }

        String dureeStr = txtDuree.getText().trim();
        if (dureeStr.isEmpty()) {
            errorDuree.setText("La durée est obligatoire");
            isValid = false;
        } else {
            try {
                int duree = Integer.parseInt(dureeStr);
                if (duree <= 0) {
                    errorDuree.setText("La durée doit être positive");
                    isValid = false;
                } else {
                    challenge.setDuree(duree);
                }
            } catch (NumberFormatException e) {
                errorDuree.setText("La durée doit être un nombre valide");
                isValid = false;
            }
        }

        if (isValid) {
            challenge.setTitre(titre);
            challenge.setDescription(description);
            challenge.setDateDebut(debut);
            challenge.setDateFin(fin);
            challenge.setNiveau(niveau);

            // Récupérer les IDs des exercices sélectionnés
            List<Integer> exerciceIds = new ArrayList<>();
            for (ExerciceCard card : exerciceCards) {
                if (card.isSelected()) {
                    exerciceIds.add(card.getExercice().getId());
                }
            }
            challenge.setExerciceIds(exerciceIds);
        }

        return isValid;
    }
}