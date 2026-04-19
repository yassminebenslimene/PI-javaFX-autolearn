package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Exercice;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ExerciceService;
import tn.esprit.services.ServiceQuiz;

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
    @FXML private FlowPane quizsContainer;

    @FXML private Label errorTitre;
    @FXML private Label errorDescription;
    @FXML private Label errorDateDebut;
    @FXML private Label errorDateFin;
    @FXML private Label errorNiveau;
    @FXML private Label errorDuree;
    @FXML private Label dialogTitle;

    private Challenge challenge;
    private ExerciceService exerciceService;
    private ServiceQuiz quizService;
    private List<ExerciceCard> exerciceCards = new ArrayList<>();
    private List<QuizCard> quizCards = new ArrayList<>();
    private boolean isEditMode = false;

    // Styles pour les champs
    private final String DEFAULT_STYLE = "-fx-background-color:rgba(255,255,255,0.08); " +
            "-fx-border-color:rgba(255,255,255,0.15); " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:10 14 10 14; -fx-font-size:13; " +
            "-fx-text-fill:white;";

    private final String ERROR_STYLE = "-fx-background-color:rgba(239,68,68,0.08); " +
            "-fx-border-color:#ef4444; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:10 14 10 14; -fx-font-size:13; " +
            "-fx-text-fill:white;";

    private final String DATE_STYLE = "-fx-border-color:rgba(255,255,255,0.15); " +
            "-fx-background-color:rgba(255,255,255,0.08); " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-text-fill:white;";

    private final String DATE_ERROR_STYLE = "-fx-border-color:#ef4444; " +
            "-fx-background-color:rgba(239,68,68,0.08); " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-text-fill:white;";

    // Classe interne pour les cartes d'exercices
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

        public boolean isSelected() { return selected; }
        public Exercice getExercice() { return exercice; }
    }

    // Classe interne pour les cartes de quiz
    private class QuizCard extends VBox {
        private Quiz quiz;
        private boolean selected = false;
        private Label titreLabel;
        private Label etatLabel;

        public QuizCard(Quiz quiz) {
            this.quiz = quiz;

            setPrefWidth(180);
            setMinWidth(160);
            setMaxWidth(200);
            setStyle("-fx-background-color:rgba(255,255,255,0.05); -fx-border-color:rgba(255,255,255,0.15); " +
                    "-fx-border-radius:10; -fx-background-radius:10; -fx-padding:12; -fx-cursor:hand;");

            titreLabel = new Label(quiz.getTitre());
            titreLabel.setWrapText(true);
            titreLabel.setStyle("-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold;");

            etatLabel = new Label("📋 " + quiz.getEtat());
            String etatColor = quiz.getEtat().equals("actif") ? "#34d399" :
                    (quiz.getEtat().equals("inactif") ? "#f87171" : "#fbbf24");
            etatLabel.setStyle("-fx-text-fill:" + etatColor + "; -fx-font-size:11;");

            VBox.setMargin(titreLabel, new Insets(0, 0, 8, 0));

            getChildren().addAll(titreLabel, etatLabel);
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

        public boolean isSelected() { return selected; }
        public Quiz getQuiz() { return quiz; }
    }

    @FXML
    public void initialize() {
        // Initialiser les niveaux
        comboNiveau.setItems(FXCollections.observableArrayList("Débutant", "Intermédiaire", "Avancé"));

        // Listeners pour réinitialiser les styles
        txtTitre.textProperty().addListener((obs, oldVal, newVal) -> {
            errorTitre.setText("");
            txtTitre.setStyle(DEFAULT_STYLE);
        });

        txtDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            errorDescription.setText("");
            txtDescription.setStyle(DEFAULT_STYLE);
        });

        txtDuree.textProperty().addListener((obs, oldVal, newVal) -> {
            errorDuree.setText("");
            txtDuree.setStyle(DEFAULT_STYLE);
        });

        dateDebut.valueProperty().addListener((obs, oldVal, newVal) -> {
            errorDateDebut.setText("");
            dateDebut.setStyle(DATE_STYLE);
        });

        dateFin.valueProperty().addListener((obs, oldVal, newVal) -> {
            errorDateFin.setText("");
            dateFin.setStyle(DATE_STYLE);
        });

        comboNiveau.valueProperty().addListener((obs, oldVal, newVal) -> {
            errorNiveau.setText("");
            comboNiveau.setStyle(DEFAULT_STYLE);
        });

        // Style du ComboBox
        comboNiveau.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill:white;");
                }
            }
        });

        comboNiveau.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else {
                    setText(item);
                    setStyle("-fx-text-fill:white; -fx-background-color:#1a1a2e;");
                }
            }
        });
    }

    public void setExerciceService(ExerciceService service) {
        this.exerciceService = service;
        loadExercices();
    }

    public void setQuizService(ServiceQuiz service) {
        this.quizService = service;
        loadQuizs();
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

    private void loadQuizs() {
        if (quizService != null) {
            List<Quiz> allQuizs = quizService.afficher();  // Utilisez la méthode appropriée
            quizsContainer.getChildren().clear();
            quizCards.clear();

            for (Quiz q : allQuizs) {
                QuizCard card = new QuizCard(q);
                quizCards.add(card);
                quizsContainer.getChildren().add(card);
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

            // Sélectionner les quiz déjà associés
            for (Integer quizId : challenge.getQuizIds()) {
                for (QuizCard card : quizCards) {
                    if (card.getQuiz().getId() == quizId) {
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

        // Reset errors et styles
        errorTitre.setText("");
        errorDescription.setText("");
        errorDateDebut.setText("");
        errorDateFin.setText("");
        errorNiveau.setText("");
        errorDuree.setText("");

        txtTitre.setStyle(DEFAULT_STYLE);
        txtDescription.setStyle(DEFAULT_STYLE);
        txtDuree.setStyle(DEFAULT_STYLE);
        dateDebut.setStyle(DATE_STYLE);
        dateFin.setStyle(DATE_STYLE);
        comboNiveau.setStyle(DEFAULT_STYLE);

        // Validation Titre
        String titre = txtTitre.getText().trim();
        if (titre.isEmpty()) {
            errorTitre.setText("⚠ Le titre ne peut pas être vide");
            txtTitre.setStyle(ERROR_STYLE);
            isValid = false;
        } else if (titre.length() < 3) {
            errorTitre.setText("⚠ Le titre doit contenir au moins 3 caractères");
            txtTitre.setStyle(ERROR_STYLE);
            isValid = false;
        } else if (titre.length() > 100) {
            errorTitre.setText("⚠ Le titre ne peut pas dépasser 100 caractères");
            txtTitre.setStyle(ERROR_STYLE);
            isValid = false;
        }

        // Validation Description
        String description = txtDescription.getText().trim();
        if (description.isEmpty()) {
            errorDescription.setText("⚠ La description ne peut pas être vide");
            txtDescription.setStyle(ERROR_STYLE);
            isValid = false;
        } else if (description.length() < 10) {
            errorDescription.setText("⚠ La description doit contenir au moins 10 caractères");
            txtDescription.setStyle(ERROR_STYLE);
            isValid = false;
        } else if (description.length() > 500) {
            errorDescription.setText("⚠ La description ne peut pas dépasser 500 caractères");
            txtDescription.setStyle(ERROR_STYLE);
            isValid = false;
        }

        // Validation Date Début
        LocalDate debut = dateDebut.getValue();
        if (debut == null) {
            errorDateDebut.setText("⚠ La date de début est obligatoire");
            dateDebut.setStyle(DATE_ERROR_STYLE);
            isValid = false;
        } else if (debut.isBefore(LocalDate.now())) {
            errorDateDebut.setText("⚠ La date de début ne peut pas être dans le passé");
            dateDebut.setStyle(DATE_ERROR_STYLE);
            isValid = false;
        }

        // Validation Date Fin
        LocalDate fin = dateFin.getValue();
        if (fin == null) {
            errorDateFin.setText("⚠ La date de fin est obligatoire");
            dateFin.setStyle(DATE_ERROR_STYLE);
            isValid = false;
        } else if (debut != null && fin.isBefore(debut)) {
            errorDateFin.setText("⚠ La date de fin doit être après la date de début");
            dateFin.setStyle(DATE_ERROR_STYLE);
            isValid = false;
        }

        // Validation Niveau
        String niveau = comboNiveau.getValue();
        if (niveau == null || niveau.isEmpty()) {
            errorNiveau.setText("⚠ Veuillez sélectionner un niveau");
            comboNiveau.setStyle(ERROR_STYLE);
            isValid = false;
        } else if (!niveau.equals("Débutant") && !niveau.equals("Intermédiaire") && !niveau.equals("Avancé")) {
            errorNiveau.setText("⚠ Le niveau doit être: Débutant, Intermédiaire ou Avancé");
            comboNiveau.setStyle(ERROR_STYLE);
            isValid = false;
        }

        // Validation Durée
        String dureeStr = txtDuree.getText().trim();
        if (dureeStr.isEmpty()) {
            errorDuree.setText("⚠ La durée est obligatoire");
            txtDuree.setStyle(ERROR_STYLE);
            isValid = false;
        } else {
            try {
                int duree = Integer.parseInt(dureeStr);
                if (duree <= 0) {
                    errorDuree.setText("⚠ La durée doit être positive");
                    txtDuree.setStyle(ERROR_STYLE);
                    isValid = false;
                } else if (duree > 600) {
                    errorDuree.setText("⚠ La durée ne peut pas dépasser 600 minutes (10 heures)");
                    txtDuree.setStyle(ERROR_STYLE);
                    isValid = false;
                } else {
                    challenge.setDuree(duree);
                }
            } catch (NumberFormatException e) {
                errorDuree.setText("⚠ La durée doit être un nombre valide");
                txtDuree.setStyle(ERROR_STYLE);
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

            // Récupérer les IDs des quiz sélectionnés
            List<Integer> quizIds = new ArrayList<>();
            for (QuizCard card : quizCards) {
                if (card.isSelected()) {
                    quizIds.add(card.getQuiz().getId());
                }
            }
            challenge.setQuizIds(quizIds);
        }

        return isValid;
    }
}