package tn.esprit.controllers.evenement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class EvenementFormController implements Initializable {

    @FXML private Label labelFormTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField fieldNbMax;
    @FXML private TextField fieldDateDebut;
    @FXML private TextField fieldDateFin;
    @FXML private TextField fieldLieu;
    @FXML private Label labelError;
    @FXML private Button btnSubmit;

    private final EvenementService service = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private Evenement evenementToEdit = null; // null = mode ajout

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboType.getItems().addAll("Hackathon", "Conference", "Workshop");
        comboType.setValue("Conference");

        // Listeners pour effacer l'erreur en temps réel
        fieldTitre.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldTitre));
        fieldDescription.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldDescription));
        fieldLieu.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldLieu));
        fieldNbMax.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldNbMax));
        fieldDateDebut.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldDateDebut));
        fieldDateFin.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldDateFin));

        // Forcer uniquement des chiffres dans nbMax
        fieldNbMax.textProperty().addListener((o, ov, nv) -> {
            if (!nv.matches("\\d*")) fieldNbMax.setText(nv.replaceAll("[^\\d]", ""));
        });
    }

    /** Appelé depuis EvenementIndexController pour pré-remplir en mode édition */
    public void setEvenement(Evenement e) {
        this.evenementToEdit = e;
        labelFormTitle.setText("Modifier l'Événement: " + e.getTitre());
        btnSubmit.setText("💾  Enregistrer");

        fieldTitre.setText(e.getTitre());
        fieldDescription.setText(e.getDescription());
        comboType.setValue(e.getType());
        fieldNbMax.setText(String.valueOf(e.getNbMax()));
        fieldLieu.setText(e.getLieu());
        if (e.getDateDebut() != null) fieldDateDebut.setText(e.getDateDebut().format(FMT));
        if (e.getDateFin() != null)   fieldDateFin.setText(e.getDateFin().format(FMT));
    }

    @FXML
    private void onSubmit() {
        labelError.setText("");
        resetFieldStyles();

        // ── Validation ──────────────────────────────────────────────
        boolean valid = true;

        // Titre : obligatoire, 5–255 caractères
        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) {
            setFieldError(fieldTitre, "Le titre est obligatoire.");
            valid = false;
        } else if (titre.length() < 5) {
            setFieldError(fieldTitre, "Le titre doit contenir au moins 5 caractères.");
            valid = false;
        } else if (titre.length() > 255) {
            setFieldError(fieldTitre, "Le titre ne peut pas dépasser 255 caractères.");
            valid = false;
        }

        // Description : obligatoire, 10–2000 caractères
        String description = fieldDescription.getText().trim();
        if (description.isEmpty()) {
            setFieldError(fieldDescription, "La description est obligatoire.");
            valid = false;
        } else if (description.length() < 10) {
            setFieldError(fieldDescription, "La description doit contenir au moins 10 caractères.");
            valid = false;
        } else if (description.length() > 2000) {
            setFieldError(fieldDescription, "La description ne peut pas dépasser 2000 caractères.");
            valid = false;
        }

        // Lieu : obligatoire, 2–255 caractères
        String lieu = fieldLieu.getText().trim();
        if (lieu.isEmpty()) {
            setFieldError(fieldLieu, "Le lieu est obligatoire.");
            valid = false;
        } else if (lieu.length() < 2) {
            setFieldError(fieldLieu, "Le lieu doit contenir au moins 2 caractères.");
            valid = false;
        }

        // Type : obligatoire
        if (comboType.getValue() == null) {
            labelError.setText("Veuillez sélectionner un type d'événement.");
            valid = false;
        }

        // NbMax : obligatoire, entier entre 1 et 100
        int nbMax = 0;
        String nbMaxStr = fieldNbMax.getText().trim();
        if (nbMaxStr.isEmpty()) {
            setFieldError(fieldNbMax, "Le nombre maximum d'équipes est obligatoire.");
            valid = false;
        } else {
            try {
                nbMax = Integer.parseInt(nbMaxStr);
                if (nbMax < 1 || nbMax > 100) {
                    setFieldError(fieldNbMax, "Le nombre d'équipes doit être entre 1 et 100.");
                    valid = false;
                }
            } catch (NumberFormatException ex) {
                setFieldError(fieldNbMax, "Veuillez entrer un nombre valide.");
                valid = false;
            }
        }

        // Date début : obligatoire, format valide, doit être dans le futur (mode ajout)
        LocalDateTime dateDebut = null;
        String dateDebutStr = fieldDateDebut.getText().trim();
        if (dateDebutStr.isEmpty()) {
            setFieldError(fieldDateDebut, "La date de début est obligatoire.");
            valid = false;
        } else {
            try {
                dateDebut = LocalDateTime.parse(dateDebutStr, FMT);
                if (evenementToEdit == null && dateDebut.isBefore(LocalDateTime.now())) {
                    setFieldError(fieldDateDebut, "La date de début doit être dans le futur.");
                    valid = false;
                }
            } catch (DateTimeParseException ex) {
                setFieldError(fieldDateDebut, "Format invalide. Utilisez jj/MM/aaaa HH:mm");
                valid = false;
            }
        }

        // Date fin : obligatoire, format valide, doit être après date début
        LocalDateTime dateFin = null;
        String dateFinStr = fieldDateFin.getText().trim();
        if (dateFinStr.isEmpty()) {
            setFieldError(fieldDateFin, "La date de fin est obligatoire.");
            valid = false;
        } else {
            try {
                dateFin = LocalDateTime.parse(dateFinStr, FMT);
                if (dateDebut != null && dateFin.isBefore(dateDebut)) {
                    setFieldError(fieldDateFin, "La date de fin doit être après la date de début.");
                    valid = false;
                }
            } catch (DateTimeParseException ex) {
                setFieldError(fieldDateFin, "Format invalide. Utilisez jj/MM/aaaa HH:mm");
                valid = false;
            }
        }

        if (!valid) return;

        // ── Sauvegarde ──────────────────────────────────────────────
        if (evenementToEdit == null) {
            // Mode ajout
            Evenement e = new Evenement(titre, lieu, description, comboType.getValue(), dateDebut, dateFin, nbMax);
            service.ajouter(e);
        } else {
            // Mode modification
            evenementToEdit.setTitre(titre);
            evenementToEdit.setLieu(lieu);
            evenementToEdit.setDescription(description);
            evenementToEdit.setType(comboType.getValue());
            evenementToEdit.setDateDebut(dateDebut);
            evenementToEdit.setDateFin(dateFin);
            evenementToEdit.setNbMax(nbMax);
            // Recalculer le statut
            String newStatus = evenementToEdit.computeStatus();
            evenementToEdit.setStatus(newStatus);
            // workflowStatus mapping
            String wf = switch (newStatus) {
                case "En cours"  -> "en_cours";
                case "Passé"     -> "termine";
                case "Annulé"    -> "annule";
                default          -> "planifie";
            };
            evenementToEdit.setWorkflowStatus(wf);
            service.modifier(evenementToEdit);
        }

        retourListe();
    }

    @FXML
    private void onAnnuler() {
        retourListe();
    }

    private void retourListe() {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/index.fxml");
            Parent view = FXMLLoader.load(resource);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ── Helpers visuels ─────────────────────────────────────────────

    private void setFieldError(Control field, String message) {
        field.setStyle(field.getStyle()
                + "-fx-border-color:#f87171 !important; -fx-border-width:1.5;");
        labelError.setText(message);
    }

    private void clearFieldError(Control field) {
        // Réinitialise la bordure rouge si l'utilisateur corrige
        String style = field.getStyle().replaceAll("-fx-border-color:#f87171 !important;", "")
                                       .replaceAll("-fx-border-width:1\\.5;", "");
        field.setStyle(style);
        if (labelError.getText() != null && !labelError.getText().isEmpty()) {
            labelError.setText("");
        }
    }

    private void resetFieldStyles() {
        for (Control c : new Control[]{fieldTitre, fieldLieu, fieldNbMax, fieldDateDebut, fieldDateFin}) {
            clearFieldError(c);
        }
        clearFieldError(fieldDescription);
    }

    private StackPane getContentArea() {
        return (StackPane) fieldTitre.getScene().lookup("#contentArea");
    }
}
