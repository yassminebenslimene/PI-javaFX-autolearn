package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceQuiz;

import java.util.List;

/**
 * Controller du formulaire Quiz (quiz_form.fxml).
 * G├¿re ├á la fois la cr├®ation d'un nouveau quiz et la modification d'un quiz existant.
 * Si quizAModifier == null ÔåÆ mode cr├®ation, sinon ÔåÆ mode modification.
 */
public class QuizFormController {

    // ÔöÇÔöÇ Composants FXML (li├®s aux ├®l├®ments de quiz_form.fxml) ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML private Label pageTitle;        // titre en haut de la page
    @FXML private Label cardTitle;        // titre de la carte
    @FXML private Label cardSubtitle;     // sous-titre de la carte
    @FXML private TextField titreField;   // champ texte pour le titre
    @FXML private TextArea descriptionField; // zone de texte pour la description
    @FXML private ComboBox<String> etatCombo;  // liste d├®roulante pour l'├®tat
    @FXML private ComboBox<Chapitre> chapitreCombo; // liste d├®roulante chapitre (OBLIGATOIRE)
    @FXML private Label chapitreErrorLabel;    // message d'erreur chapitre
    @FXML private TextField dureeField;   // champ pour la dur├®e max (optionnel)
    @FXML private TextField seuilField;   // champ pour le seuil de r├®ussite (optionnel)
    @FXML private TextField tentativesField; // champ pour le nb de tentatives (optionnel)
    @FXML private Label messageLabel;     // affiche les messages d'erreur
    @FXML private Button btnSauvegarder;  // bouton Enregistrer / Mettre ├á jour

    // Style normal d'un champ de saisie
    private static final String FIELD_NORMAL =
        "-fx-background-color:rgba(255,255,255,0.05);" +
        "-fx-border-color:rgba(255,255,255,0.1); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

    // Style d'un champ en erreur (bordure rouge)
    private static final String FIELD_ERROR =
        "-fx-background-color:rgba(239,68,68,0.08);" +
        "-fx-border-color:rgba(239,68,68,0.6); -fx-border-radius:8px;" +
        "-fx-background-radius:8px; -fx-border-width:1.5px;" +
        "-fx-text-fill:#f5f5f4; -fx-prompt-text-fill:rgba(245,245,244,0.35);" +
        "-fx-padding:9px 13px; -fx-font-size:13px;";

    // Service pour les op├®rations BDD sur les quiz
    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // Le quiz ├á modifier (null si on est en mode cr├®ation)
    private Quiz quizAModifier = null;

    // ÔöÇÔöÇ Initialisation : appel├®e automatiquement au chargement du FXML ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void initialize() {
        // Remplir la liste d├®roulante ├®tats
        etatCombo.setItems(FXCollections.observableArrayList(
            "actif", "inactif", "brouillon", "archive"
        ));

        // Remplir la ComboBox chapitres
        List<Chapitre> chapitres = serviceChapitre.consulter();
        chapitreCombo.getItems().addAll(chapitres);
        chapitreCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Chapitre item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
        chapitreCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Chapitre item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "S├®lectionnez un chapitre obligatoirement" : item.getTitre());
            }
        });
        chapitreCombo.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) {
                chapitreCombo.setStyle(FIELD_NORMAL);
                if (chapitreErrorLabel != null) {
                    chapitreErrorLabel.setVisible(false);
                    chapitreErrorLabel.setManaged(false);
                }
            }
        });

        // Forcer le fond sombre sur le TextArea (le style inline ne suffit pas)
        javafx.application.Platform.runLater(() -> {
            javafx.scene.Node content = descriptionField.lookup(".content");
            if (content != null) content.setStyle("-fx-background-color:#1a2e1f;");
        });

        // Effacer les erreurs d├¿s que l'utilisateur commence ├á taper
        titreField.textProperty().addListener((o, ov, nv) -> resetField(titreField));
        descriptionField.textProperty().addListener((o, ov, nv) -> resetField(descriptionField));
        dureeField.textProperty().addListener((o, ov, nv) -> resetField(dureeField));
        seuilField.textProperty().addListener((o, ov, nv) -> resetField(seuilField));
        tentativesField.textProperty().addListener((o, ov, nv) -> resetField(tentativesField));
    }

    // ÔöÇÔöÇ Mode modification : pr├®-remplir le formulaire avec les donn├®es du quiz ÔöÇ
    public void initEdit(Quiz quiz) {
        this.quizAModifier = quiz;
        pageTitle.setText("Modifier le Quiz");
        cardTitle.setText("Modifier le Quiz");
        cardSubtitle.setText("Mettez ├á jour les informations");
        btnSauvegarder.setText("Ô£ô Mettre ├á jour");
        titreField.setText(quiz.getTitre());
        descriptionField.setText(quiz.getDescription());
        etatCombo.setValue(quiz.getEtat());
        if (quiz.getDureeMaxMinutes() != null) dureeField.setText(String.valueOf(quiz.getDureeMaxMinutes()));
        if (quiz.getSeuilReussite() != null)   seuilField.setText(String.valueOf(quiz.getSeuilReussite()));
        if (quiz.getMaxTentatives() != null)   tentativesField.setText(String.valueOf(quiz.getMaxTentatives()));
        // Pr├®-s├®lectionner le chapitre
        if (quiz.getChapitreId() != null) {
            chapitreCombo.getItems().stream()
                .filter(c -> c.getId() == quiz.getChapitreId())
                .findFirst()
                .ifPresent(chapitreCombo::setValue);
        }
    }

    // ÔöÇÔöÇ Sauvegarder : appel├® quand on clique sur le bouton Enregistrer ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void sauvegarder() {
        resetAll(); // effacer les erreurs pr├®c├®dentes
        boolean valid = true;

        // R├®cup├®rer les valeurs saisies (trim() enl├¿ve les espaces inutiles)
        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        String etat = etatCombo.getValue();
        String dureeStr = dureeField.getText() == null ? "" : dureeField.getText().trim();
        String seuilStr = seuilField.getText() == null ? "" : seuilField.getText().trim();
        String tentStr  = tentativesField.getText() == null ? "" : tentativesField.getText().trim();

        // ÔöÇÔöÇ Validation du titre ÔöÇÔöÇ
        if (titre.isEmpty()) {
            markError(titreField, "ÔÜá Le titre du quiz est obligatoire.");
            valid = false;
        } else if (titre.length() < 3) {
            markError(titreField, "ÔÜá Le titre est trop court ÔÇö minimum 3 caract├¿res (actuellement " + titre.length() + ").");
            valid = false;
        } else if (titre.length() > 255) {
            markError(titreField, "ÔÜá Le titre est trop long ÔÇö maximum 255 caract├¿res (actuellement " + titre.length() + ").");
            valid = false;
        }

        // ÔöÇÔöÇ Validation de la description ÔöÇÔöÇ
        if (valid) {
            if (description.isEmpty()) {
                markError(descriptionField, "ÔÜá La description est obligatoire.");
                valid = false;
            } else if (description.length() < 10) {
                markError(descriptionField, "ÔÜá Description trop courte ÔÇö minimum 10 caract├¿res (actuellement " + description.length() + ").");
                valid = false;
            } else if (description.length() > 2000) {
                markError(descriptionField, "ÔÜá Description trop longue ÔÇö maximum 2000 caract├¿res (actuellement " + description.length() + ").");
                valid = false;
            }
        }

        // ÔöÇÔöÇ Validation de l'├®tat (doit ├¬tre dans la liste) ÔöÇÔöÇ
        if (valid && (etat == null || !List.of("actif","inactif","brouillon","archive").contains(etat))) {
            showError("ÔÜá Veuillez s├®lectionner un ├®tat parmi : Actif, Inactif, Brouillon, Archive.");
            valid = false;
        }

        // ÔöÇÔöÇ Validation du chapitre (OBLIGATOIRE) ÔöÇÔöÇ
        Chapitre chapitreSelectionne = chapitreCombo.getValue();
        if (valid && chapitreSelectionne == null) {
            chapitreCombo.setStyle(FIELD_ERROR);
            if (chapitreErrorLabel != null) {
                chapitreErrorLabel.setText("­ƒöÆ OBLIGATOIRE : S├®lectionnez un chapitre");
                chapitreErrorLabel.setVisible(true);
                chapitreErrorLabel.setManaged(true);
            }
            showError("­ƒöÆ Un quiz doit obligatoirement appartenir ├á un chapitre.");
            valid = false;
        }

        // ÔöÇÔöÇ Validation de la dur├®e (optionnelle, entier positif max 600) ÔöÇÔöÇ
        Integer duree = null;
        if (valid && !dureeStr.isEmpty()) {
            try {
                duree = Integer.parseInt(dureeStr);
                if (duree <= 0) {
                    markError(dureeField, "ÔÜá La dur├®e doit ├¬tre un nombre entier positif (ex: 30).");
                    valid = false;
                } else if (duree > 600) {
                    markError(dureeField, "ÔÜá La dur├®e maximale est 600 minutes (10 heures).");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(dureeField, "ÔÜá La dur├®e doit ├¬tre un nombre entier (ex: 30), pas \"" + dureeStr + "\".");
                valid = false;
            }
        }

        // ÔöÇÔöÇ Validation du seuil de r├®ussite (optionnel, entre 0 et 100) ÔöÇÔöÇ
        Integer seuil = null;
        if (valid && !seuilStr.isEmpty()) {
            try {
                seuil = Integer.parseInt(seuilStr);
                if (seuil < 0 || seuil > 100) {
                    markError(seuilField, "ÔÜá Le seuil doit ├¬tre un pourcentage entre 0 et 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(seuilField, "ÔÜá Le seuil doit ├¬tre un entier entre 0 et 100 (ex: 50), pas \"" + seuilStr + "\".");
                valid = false;
            }
        }

        // ÔöÇÔöÇ Validation du nombre de tentatives (optionnel, entier positif max 100) ÔöÇÔöÇ
        Integer tentatives = null;
        if (valid && !tentStr.isEmpty()) {
            try {
                tentatives = Integer.parseInt(tentStr);
                if (tentatives <= 0) {
                    markError(tentativesField, "ÔÜá Le nombre de tentatives doit ├¬tre un entier positif (ex: 3).");
                    valid = false;
                } else if (tentatives > 100) {
                    markError(tentativesField, "ÔÜá Le nombre de tentatives ne peut pas d├®passer 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(tentativesField, "ÔÜá Le nombre de tentatives doit ├¬tre un entier (ex: 3), pas \"" + tentStr + "\".");
                valid = false;
            }
        }

        // Si une validation a ├®chou├®, on arr├¬te ici
        if (!valid) return;

        // ÔöÇÔöÇ Sauvegarde en BDD ÔöÇÔöÇ
        boolean ok;
        if (quizAModifier == null) {
            Quiz newQuiz = new Quiz(titre, description, etat, duree, seuil, tentatives, null, null, null, chapitreSelectionne.getId());
            ok = serviceQuiz.ajouter(newQuiz);
            if (!ok) {
                showError("ÔØî ├ëchec de l'ajout ÔÇö v├®rifiez que la table 'quiz' existe et que le chapitre_id est valide.");
                return;
            }
            showAlert(true, "Quiz ajout├® avec succ├¿s !", "");
        } else {
            quizAModifier.setTitre(titre);
            quizAModifier.setDescription(description);
            quizAModifier.setEtat(etat);
            quizAModifier.setDureeMaxMinutes(duree);
            quizAModifier.setSeuilReussite(seuil);
            quizAModifier.setMaxTentatives(tentatives);
            quizAModifier.setChapitreId(chapitreSelectionne.getId());
            ok = serviceQuiz.modifier(quizAModifier);
            showAlert(ok, "Quiz modifi├® avec succ├¿s !", "├ëchec de la modification du quiz.");
        }
        // Si succ├¿s ÔåÆ retourner ├á la liste des quiz
        if (ok) navigateToList();
    }

    // ÔöÇÔöÇ Retour : revenir ├á la liste sans sauvegarder ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    @FXML
    public void retour() { navigateToList(); }

    // ÔöÇÔöÇ Helpers ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

    // Marque un champ en erreur et affiche le message
    private void markError(Control field, String msg) {
        field.setStyle(FIELD_ERROR);
        showError(msg);
    }

    // Affiche un message d'erreur en rouge sous le formulaire
    private void showError(String msg) {
        messageLabel.setText(msg);
        messageLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:13px; -fx-font-weight:bold;" +
            "-fx-background-color:rgba(239,68,68,0.08); -fx-background-radius:8;" +
            "-fx-padding:8 12; -fx-border-color:rgba(239,68,68,0.3);" +
            "-fx-border-radius:8; -fx-border-width:1;");
    }

    // Remet un champ ├á son style normal et efface le message d'erreur
    private void resetField(Control field) {
        field.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    // Remet tous les champs ├á leur style normal
    private void resetAll() {
        titreField.setStyle(FIELD_NORMAL);
        descriptionField.setStyle(FIELD_NORMAL);
        dureeField.setStyle(FIELD_NORMAL);
        seuilField.setStyle(FIELD_NORMAL);
        tentativesField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    // Navigue vers la liste des quiz (index.fxml) dans la zone de contenu principale
    private void navigateToList() {
        try {
            StackPane contentArea = (StackPane) titreField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/quiz/index.fxml"));
                contentArea.getChildren().clear();
                contentArea.getChildren().add(loader.load());
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Affiche une alerte de succ├¿s (vert) ou d'├®chec (rouge) selon le r├®sultat
    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "Ô£à Succ├¿s" : "ÔØî ├ëchec");
        alert.setContentText(success ? msgOk : msgEchec);
        alert.showAndWait(); // bloque jusqu'├á ce que l'utilisateur ferme l'alerte
    }
}