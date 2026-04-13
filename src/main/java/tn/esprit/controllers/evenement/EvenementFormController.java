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
import java.util.ResourceBundle;

public class EvenementFormController implements Initializable {

    @FXML private Label labelFormTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField fieldNbMax;
    @FXML private javafx.scene.control.DatePicker pickerDateDebut;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerHeureDebut;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerMinDebut;
    @FXML private javafx.scene.control.DatePicker pickerDateFin;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerHeureFin;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerMinFin;
    @FXML private TextField fieldLieu;
    @FXML private Label labelError;
    @FXML private Button btnSubmit;
    // Inline error labels
    @FXML private Label errTitre;
    @FXML private Label errDescription;
    @FXML private Label errType;
    @FXML private Label errNbMax;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;
    @FXML private Label errLieu;

    private final EvenementService service = new EvenementService();
    private Evenement evenementToEdit = null; // null = mode ajout

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboType.getItems().addAll("Hackathon", "Conference", "Workshop");
        comboType.setValue("Conference");

        // Format DatePicker en dd/MM/yyyy
        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        javafx.util.StringConverter<java.time.LocalDate> converter = new javafx.util.StringConverter<>() {
            public String toString(java.time.LocalDate d) { return d != null ? d.format(dateFmt) : ""; }
            public java.time.LocalDate fromString(String s) {
                try { return s != null && !s.isBlank() ? java.time.LocalDate.parse(s, dateFmt) : null; }
                catch (Exception e) { return null; }
            }
        };
        pickerDateDebut.setConverter(converter);
        pickerDateFin.setConverter(converter);

        // Listeners pour effacer les erreurs
        fieldTitre.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldTitre));
        fieldDescription.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldDescription));
        fieldLieu.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldLieu));
        fieldNbMax.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldNbMax));
        pickerDateDebut.valueProperty().addListener((o, ov, nv) -> { if (errDateDebut != null) errDateDebut.setText(""); });
        pickerDateFin.valueProperty().addListener((o, ov, nv) -> { if (errDateFin != null) errDateFin.setText(""); });

        // Forcer uniquement des chiffres dans nbMax
        fieldNbMax.textProperty().addListener((o, ov, nv) -> {
            if (!nv.matches("\\d*")) fieldNbMax.setText(nv.replaceAll("[^\\d]", ""));
        });
    }

    public void setEvenement(Evenement e) {
        this.evenementToEdit = e;
        labelFormTitle.setText("Modifier l'Evenement: " + e.getTitre());
        btnSubmit.setText("Enregistrer");

        fieldTitre.setText(e.getTitre());
        fieldDescription.setText(e.getDescription());
        comboType.setValue(e.getType());
        fieldNbMax.setText(String.valueOf(e.getNbMax()));
        fieldLieu.setText(e.getLieu());
        if (e.getDateDebut() != null) {
            pickerDateDebut.setValue(e.getDateDebut().toLocalDate());
            spinnerHeureDebut.getValueFactory().setValue(e.getDateDebut().getHour());
            spinnerMinDebut.getValueFactory().setValue(e.getDateDebut().getMinute());
        }
        if (e.getDateFin() != null) {
            pickerDateFin.setValue(e.getDateFin().toLocalDate());
            spinnerHeureFin.getValueFactory().setValue(e.getDateFin().getHour());
            spinnerMinFin.getValueFactory().setValue(e.getDateFin().getMinute());
        }
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
            if (errType != null) errType.setText("Veuillez selectionner un type d'evenement.");
            else labelError.setText("Veuillez selectionner un type d'evenement.");
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

        // Date debut : obligatoire, ne peut pas être dans le passé (heure incluse)
        // Force commit si l'utilisateur a tapé la date sans passer par le calendrier
        if (pickerDateDebut.getValue() == null && pickerDateDebut.getEditor().getText() != null
                && !pickerDateDebut.getEditor().getText().trim().isEmpty()) {
            pickerDateDebut.getEditor().commitValue();
        }
        LocalDateTime dateDebut = null;
        if (pickerDateDebut.getValue() == null) {
            if (errDateDebut != null) errDateDebut.setText("La date de début est obligatoire.");
            valid = false;
        } else {
            int h = spinnerHeureDebut.getValue();
            int m = spinnerMinDebut.getValue();
            dateDebut = pickerDateDebut.getValue().atTime(h, m);
            if (evenementToEdit == null) {
                LocalDateTime now = LocalDateTime.now();
                if (dateDebut.isBefore(now)) {
                    if (pickerDateDebut.getValue().isBefore(java.time.LocalDate.now())) {
                        if (errDateDebut != null) errDateDebut.setText("Impossible de planifier un événement dans le passé.");
                    } else {
                        // même jour mais heure passée
                        if (errDateDebut != null) errDateDebut.setText("L'heure de début doit être supérieure à l'heure actuelle (" +
                            String.format("%02d:%02d", now.getHour(), now.getMinute()) + ").");
                    }
                    valid = false;
                }
            }
        }

        // Date fin : obligatoire, doit être strictement après date début
        // Force commit si l'utilisateur a tapé la date sans passer par le calendrier
        if (pickerDateFin.getValue() == null && pickerDateFin.getEditor().getText() != null
                && !pickerDateFin.getEditor().getText().trim().isEmpty()) {
            pickerDateFin.getEditor().commitValue();
        }
        LocalDateTime dateFin = null;
        if (pickerDateFin.getValue() == null) {
            if (errDateFin != null) errDateFin.setText("La date de fin est obligatoire.");
            valid = false;
        } else {
            int h = spinnerHeureFin.getValue();
            int m = spinnerMinFin.getValue();
            dateFin = pickerDateFin.getValue().atTime(h, m);
            if (dateDebut != null && !dateFin.isAfter(dateDebut)) {
                if (dateFin.toLocalDate().isBefore(dateDebut.toLocalDate())) {
                    if (errDateFin != null) errDateFin.setText("La date de fin ne peut pas être antérieure à la date de début.");
                } else {
                    // même jour, heure inférieure ou égale
                    if (errDateFin != null) errDateFin.setText("L'heure de fin doit être supérieure à l'heure de début (" +
                        String.format("%02d:%02d", dateDebut.getHour(), dateDebut.getMinute()) + ").");
                }
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
        // Show inline label
        Label errLabel = getErrLabel(field);
        if (errLabel != null) errLabel.setText(message);
        else labelError.setText(message);
    }

    private Label getErrLabel(Control field) {
        if (field == fieldTitre) return errTitre;
        if (field == fieldDescription) return errDescription;
        if (field == fieldNbMax) return errNbMax;
        if (field == fieldLieu) return errLieu;
        return null;
    }

    private void clearFieldError(Control field) {
        String style = field.getStyle().replaceAll("-fx-border-color:#f87171 !important;", "")
                                       .replaceAll("-fx-border-width:1\\.5;", "");
        field.setStyle(style);
        Label errLabel = getErrLabel(field);
        if (errLabel != null) errLabel.setText("");
        if (labelError != null) labelError.setText("");
    }

    private void resetFieldStyles() {
        for (Control c : new Control[]{fieldTitre, fieldLieu, fieldNbMax}) {
            clearFieldError(c);
        }
        clearFieldError(fieldDescription);
        if (errDateDebut != null) errDateDebut.setText("");
        if (errDateFin != null) errDateFin.setText("");
        if (errType != null) errType.setText("");
        if (labelError != null) labelError.setText("");
    }

    private StackPane getContentArea() {
        return (StackPane) fieldTitre.getScene().lookup("#contentArea");
    }
}
