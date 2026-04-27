package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Quiz;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.GroqQuizGeneratorService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceQuiz;
import tn.esprit.session.SessionManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * QuizFormController — formulaire de création et modification d'un quiz.
 * Mode création si quizAModifier == null, sinon mode modification.
 */
public class QuizFormController {

    // ── Champs FXML ───────────────────────────────────────────────────────────
    @FXML private Label pageTitle;
    @FXML private Label cardTitle;
    @FXML private Label cardSubtitle;
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> etatCombo;
    @FXML private ComboBox<Chapitre> chapitreCombo; // obligatoire
    @FXML private Label chapitreErrorLabel;
    @FXML private TextField dureeField;
    @FXML private TextField seuilField;
    @FXML private TextField tentativesField;
    @FXML private Label messageLabel;
    @FXML private Button btnSauvegarder;
    @FXML private Button btnGenererIA;

    // ── Image ─────────────────────────────────────────────────────────────────
    @FXML private StackPane imagePreviewPane;
    @FXML private ImageView imagePreview;
    @FXML private Label imagePreviewLabel;
    @FXML private Label imageInfoLabel;
    @FXML private Button btnSupprimerImage;

    private File selectedImageFile = null;   // image choisie par l'utilisateur
    private String existingImageName = null; // image déjà enregistrée (mode modification)

    // Style normal d'un champ
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

    private final ServiceQuiz serviceQuiz = new ServiceQuiz();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    private Quiz quizAModifier = null; // null = création, sinon = modification

    // Initialisation : remplit les ComboBox et attache les listeners de validation
    @FXML
    public void initialize() {
        // Remplir la liste déroulante états
        etatCombo.setItems(FXCollections.observableArrayList(
            "actif", "inactif", "brouillon", "archive"
        ));
        etatCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                setStyle("-fx-text-fill:#f5f5f4; -fx-background-color:#1a2e1f; -fx-padding:8 14 8 14;");
            }
        });
        etatCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez un état" : item);
                setStyle("-fx-text-fill:#f5f5f4;");
            }
        });

        // Remplir la ComboBox chapitres
        List<Chapitre> chapitres = serviceChapitre.consulter();
        chapitreCombo.getItems().addAll(chapitres);
        chapitreCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Chapitre item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
                setStyle("-fx-text-fill:#f5f5f4; -fx-background-color:#1a2e1f; -fx-padding:8 14 8 14;");
            }
        });
        chapitreCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Chapitre item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionnez un chapitre obligatoirement" : item.getTitre());
                setStyle("-fx-text-fill:#f5f5f4;");
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

            // Forcer le fond sombre sur le popup ListView des ComboBox
            styleComboPopup(etatCombo);
            styleComboPopup(chapitreCombo);
        });

        // Effacer les erreurs dès que l'utilisateur commence à taper
        titreField.textProperty().addListener((o, ov, nv) -> resetField(titreField));
        descriptionField.textProperty().addListener((o, ov, nv) -> resetField(descriptionField));
        dureeField.textProperty().addListener((o, ov, nv) -> resetField(dureeField));
        seuilField.textProperty().addListener((o, ov, nv) -> resetField(seuilField));
        tentativesField.textProperty().addListener((o, ov, nv) -> resetField(tentativesField));
    }

    // Pré-remplit le formulaire avec les données du quiz à modifier
    public void initEdit(Quiz quiz) {
        this.quizAModifier = quiz;
        pageTitle.setText("Modifier le Quiz");
        cardTitle.setText("Modifier le Quiz");
        cardSubtitle.setText("Mettez à jour les informations");
        btnSauvegarder.setText("✓ Mettre à jour");
        titreField.setText(quiz.getTitre());
        descriptionField.setText(quiz.getDescription());
        etatCombo.setValue(quiz.getEtat());
        if (quiz.getDureeMaxMinutes() != null) dureeField.setText(String.valueOf(quiz.getDureeMaxMinutes()));
        if (quiz.getSeuilReussite() != null)   seuilField.setText(String.valueOf(quiz.getSeuilReussite()));
        if (quiz.getMaxTentatives() != null)   tentativesField.setText(String.valueOf(quiz.getMaxTentatives()));
        // Pré-sélectionner le chapitre
        if (quiz.getChapitreId() != null) {
            chapitreCombo.getItems().stream()
                .filter(c -> c.getId() == quiz.getChapitreId())
                .findFirst()
                .ifPresent(chapitreCombo::setValue);
        }
        // Pré-charger l'image existante
        if (quiz.getImageName() != null && !quiz.getImageName().isBlank()) {
            existingImageName = quiz.getImageName();
            afficherImageExistante(quiz.getImageName(), quiz.getImageSize());
        }
    }

    // Valide les champs et sauvegarde le quiz (création ou modification)
    @FXML
    public void sauvegarder() {
        resetAll(); // effacer les erreurs précédentes
        boolean valid = true;

        // Récupérer les valeurs saisies (trim() enlève les espaces inutiles)
        String titre = titreField.getText() == null ? "" : titreField.getText().trim();
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        String etat = etatCombo.getValue();
        String dureeStr = dureeField.getText() == null ? "" : dureeField.getText().trim();
        String seuilStr = seuilField.getText() == null ? "" : seuilField.getText().trim();
        String tentStr  = tentativesField.getText() == null ? "" : tentativesField.getText().trim();

        // ── Validation du titre ──
        if (titre.isEmpty()) {
            markError(titreField, "⚠ Le titre du quiz est obligatoire.");
            valid = false;
        } else if (titre.length() < 3) {
            markError(titreField, "⚠ Le titre est trop court — minimum 3 caractères (actuellement " + titre.length() + ").");
            valid = false;
        } else if (titre.length() > 255) {
            markError(titreField, "⚠ Le titre est trop long — maximum 255 caractères (actuellement " + titre.length() + ").");
            valid = false;
        }

        // ── Validation de la description ──
        if (valid) {
            if (description.isEmpty()) {
                markError(descriptionField, "⚠ La description est obligatoire.");
                valid = false;
            } else if (description.length() < 10) {
                markError(descriptionField, "⚠ Description trop courte — minimum 10 caractères (actuellement " + description.length() + ").");
                valid = false;
            } else if (description.length() > 2000) {
                markError(descriptionField, "⚠ Description trop longue — maximum 2000 caractères (actuellement " + description.length() + ").");
                valid = false;
            }
        }

        // ── Validation de l'état (doit être dans la liste) ──
        if (valid && (etat == null || !List.of("actif","inactif","brouillon","archive").contains(etat))) {
            showError("⚠ Veuillez sélectionner un état parmi : Actif, Inactif, Brouillon, Archive.");
            valid = false;
        }

        // ── Validation du chapitre (OBLIGATOIRE) ──
        Chapitre chapitreSelectionne = chapitreCombo.getValue();
        if (valid && chapitreSelectionne == null) {
            chapitreCombo.setStyle(FIELD_ERROR);
            if (chapitreErrorLabel != null) {
                chapitreErrorLabel.setText("🔒 OBLIGATOIRE : Sélectionnez un chapitre");
                chapitreErrorLabel.setVisible(true);
                chapitreErrorLabel.setManaged(true);
            }
            showError("🔒 Un quiz doit obligatoirement appartenir à un chapitre.");
            valid = false;
        }

        // ── Validation de la durée (optionnelle, entier positif max 600) ──
        Integer duree = null;
        if (valid && !dureeStr.isEmpty()) {
            try {
                duree = Integer.parseInt(dureeStr);
                if (duree <= 0) {
                    markError(dureeField, "⚠ La durée doit être un nombre entier positif (ex: 30).");
                    valid = false;
                } else if (duree > 600) {
                    markError(dureeField, "⚠ La durée maximale est 600 minutes (10 heures).");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(dureeField, "⚠ La durée doit être un nombre entier (ex: 30), pas \"" + dureeStr + "\".");
                valid = false;
            }
        }

        // ── Validation du seuil de réussite (optionnel, entre 0 et 100) ──
        Integer seuil = null;
        if (valid && !seuilStr.isEmpty()) {
            try {
                seuil = Integer.parseInt(seuilStr);
                if (seuil < 0 || seuil > 100) {
                    markError(seuilField, "⚠ Le seuil doit être un pourcentage entre 0 et 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(seuilField, "⚠ Le seuil doit être un entier entre 0 et 100 (ex: 50), pas \"" + seuilStr + "\".");
                valid = false;
            }
        }

        // ── Validation du nombre de tentatives (optionnel, entier positif max 100) ──
        Integer tentatives = null;
        if (valid && !tentStr.isEmpty()) {
            try {
                tentatives = Integer.parseInt(tentStr);
                if (tentatives <= 0) {
                    markError(tentativesField, "⚠ Le nombre de tentatives doit être un entier positif (ex: 3).");
                    valid = false;
                } else if (tentatives > 100) {
                    markError(tentativesField, "⚠ Le nombre de tentatives ne peut pas dépasser 100.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                markError(tentativesField, "⚠ Le nombre de tentatives doit être un entier (ex: 3), pas \"" + tentStr + "\".");
                valid = false;
            }
        }

        // Si une validation a échoué, on arrête ici
        if (!valid) return;

        // ── Sauvegarde ────────────────────────────────────────────────────────────
        // Copier l'image si une nouvelle a été sélectionnée
        String imageName = existingImageName;
        Integer imageSize = null;
        if (selectedImageFile != null) {
            String[] saved = saveImageFile(selectedImageFile);
            if (saved != null) {
                imageName = saved[0];
                imageSize = Integer.parseInt(saved[1]);
            }
        } else if (quizAModifier != null) {
            imageSize = quizAModifier.getImageSize();
        }

        boolean ok;
        if (quizAModifier == null) {
            Quiz newQuiz = new Quiz(titre, description, etat, duree, seuil, tentatives, imageName, imageSize, null, chapitreSelectionne.getId());
            ok = serviceQuiz.ajouter(newQuiz);
            if (!ok) {
                showError("❌ Échec de l'ajout — vérifiez que la table 'quiz' existe et que le chapitre_id est valide.");
                return;
            }
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_quiz",
                java.util.Map.of("titre", titre));
            showAlert(true, "Quiz ajouté avec succès !", "");
        } else {
            quizAModifier.setTitre(titre);
            quizAModifier.setDescription(description);
            quizAModifier.setEtat(etat);
            quizAModifier.setDureeMaxMinutes(duree);
            quizAModifier.setSeuilReussite(seuil);
            quizAModifier.setMaxTentatives(tentatives);
            quizAModifier.setImageName(imageName);
            quizAModifier.setImageSize(imageSize);
            quizAModifier.setChapitreId(chapitreSelectionne.getId());
            ok = serviceQuiz.modifier(quizAModifier);
            if (ok) {
                var admin = SessionManager.getCurrentUser();
                if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_quiz",
                    java.util.Map.of("titre", titre));
            }
            showAlert(ok, "Quiz modifié avec succès !", "Échec de la modification du quiz.");
        }
        // Si succès → retourner à la liste des quiz
        if (ok) navigateToList();
    }

    // Lance la génération IA depuis le formulaire (nécessite un chapitre sélectionné)
    @FXML
    public void genererAvecIA() {
        // Vérifier qu'un chapitre est sélectionné
        Chapitre chapitre = chapitreCombo.getValue();
        if (chapitre == null) {
            chapitreCombo.setStyle(FIELD_ERROR);
            if (chapitreErrorLabel != null) {
                chapitreErrorLabel.setText("🔒 Sélectionnez un chapitre avant de générer");
                chapitreErrorLabel.setVisible(true);
                chapitreErrorLabel.setManaged(true);
            }
            showError("🤖 Sélectionnez d'abord un chapitre pour que l'IA puisse générer les questions.");
            return;
        }

        // Boîte de dialogue de configuration
        javafx.scene.control.Dialog<String[]> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("🤖 Générer avec IA Groq");
        dialog.setHeaderText("Configurer la génération automatique\nChapitre : " + chapitre.getTitre());

        javafx.scene.control.ButtonType btnGenerer = new javafx.scene.control.ButtonType(
            "🚀 Générer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType btnAnnuler = new javafx.scene.control.ButtonType(
            "Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGenerer, btnAnnuler);

        // Formulaire de configuration
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(12);
        content.setPadding(new javafx.geometry.Insets(16));
        content.setStyle("-fx-background-color:#1a2e1f;");

        // Nombre de questions
        javafx.scene.control.Label lblNb = new javafx.scene.control.Label("Nombre de questions (1-10) :");
        lblNb.setStyle("-fx-text-fill:#f5f5f4; -fx-font-size:13;");
        javafx.scene.control.Spinner<Integer> spinnerNb = new javafx.scene.control.Spinner<>(1, 10, 5);
        spinnerNb.setEditable(true);
        spinnerNb.setStyle("-fx-background-color:#0f1a14; -fx-text-fill:#f5f5f4;");

        // Difficulté
        javafx.scene.control.Label lblDiff = new javafx.scene.control.Label("Niveau de difficulté :");
        lblDiff.setStyle("-fx-text-fill:#f5f5f4; -fx-font-size:13;");
        javafx.scene.control.ComboBox<String> comboDiff = new javafx.scene.control.ComboBox<>();
        comboDiff.getItems().addAll("facile", "moyen", "difficile");
        comboDiff.setValue("moyen");
        comboDiff.setMaxWidth(Double.MAX_VALUE);
        comboDiff.setStyle("-fx-background-color:#0f1a14; -fx-text-fill:#f5f5f4;");

        content.getChildren().addAll(lblNb, spinnerNb, lblDiff, comboDiff);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#0f1a14;");

        dialog.setResultConverter(btn -> {
            if (btn == btnGenerer) {
                return new String[]{
                    String.valueOf(spinnerNb.getValue()),
                    comboDiff.getValue()
                };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(params -> {
            int nb = Integer.parseInt(params[0]);
            String diff = params[1];

            // Désactiver le bouton pendant la génération
            if (btnGenererIA != null) {
                btnGenererIA.setDisable(true);
                btnGenererIA.setText("⏳ Génération en cours...");
            }
            showError(""); // effacer erreurs

            // Afficher message de chargement
            messageLabel.setText("🤖 L'IA génère " + nb + " questions... (peut prendre 5-15 secondes)");
            messageLabel.setStyle("-fx-text-fill:#a5b4fc; -fx-font-size:13px; -fx-font-weight:bold;");

            // Titre du quiz depuis le champ titre (ou auto)
            String titre = titreField.getText() == null || titreField.getText().isBlank()
                ? null : titreField.getText().trim();

            // Appel asynchrone
            GroqQuizGeneratorService groqService = new GroqQuizGeneratorService();
            groqService.genererQuizAsync(chapitre, nb, diff, titre, chapitre.getId())
                .thenAccept(quiz -> {
                    javafx.application.Platform.runLater(() -> {
                        if (btnGenererIA != null) {
                            btnGenererIA.setDisable(false);
                            btnGenererIA.setText("🤖 Générer avec IA");
                        }
                        if (quiz != null) {
                            messageLabel.setText("✅ Quiz généré avec succès ! " + nb + " questions créées en BDD.");
                            messageLabel.setStyle("-fx-text-fill:#6ee7b7; -fx-font-size:13px; -fx-font-weight:bold;");
                            // Naviguer vers la liste pour voir le quiz créé
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                            alert.setTitle("✅ Génération réussie");
                            alert.setHeaderText(null);
                            alert.setContentText("Quiz \"" + quiz.getTitre() + "\" créé avec " + nb
                                + " questions !\n\nÉtat : brouillon — Révisez et activez-le.");
                            alert.showAndWait();
                            navigateToList();
                        }
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        if (btnGenererIA != null) {
                            btnGenererIA.setDisable(false);
                            btnGenererIA.setText("🤖 Générer avec IA");
                        }
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        showError("❌ Erreur IA : " + msg);
                    });
                    return null;
                });
        });
    }

    // Retourne à la liste sans sauvegarder
    @FXML
    public void retour() { navigateToList(); }

    // Ouvre le sélecteur de fichier pour choisir une image
    @FXML
    public void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour le quiz");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp")
        );
        File file = fileChooser.showOpenDialog(titreField.getScene().getWindow());
        if (file == null) return;

        // Vérifier la taille (max 5 Mo)
        long sizeBytes = file.length();
        if (sizeBytes > 5 * 1024 * 1024) {
            showError("⚠ L'image est trop grande — maximum 5 Mo (fichier : " + (sizeBytes / 1024 / 1024) + " Mo).");
            return;
        }

        selectedImageFile = file;
        // Afficher la prévisualisation
        try {
            Image img = new Image(file.toURI().toString());
            imagePreview.setImage(img);
            imagePreview.setVisible(true);
            imagePreview.setManaged(true);
            imagePreviewLabel.setVisible(false);
            imagePreviewLabel.setManaged(false);
            btnSupprimerImage.setVisible(true);
            btnSupprimerImage.setManaged(true);
            String sizeStr = sizeBytes < 1024 ? sizeBytes + " o"
                           : sizeBytes < 1024 * 1024 ? (sizeBytes / 1024) + " Ko"
                           : String.format("%.1f Mo", sizeBytes / 1024.0 / 1024.0);
            imageInfoLabel.setText("📄 " + file.getName() + "  •  " + sizeStr);
        } catch (Exception e) {
            showError("⚠ Impossible de charger l'image : " + e.getMessage());
        }
    }

    // Supprime l'image sélectionnée et réinitialise la prévisualisation
    @FXML
    public void supprimerImage() {
        selectedImageFile = null;
        existingImageName = null;
        imagePreview.setImage(null);
        imagePreview.setVisible(false);
        imagePreview.setManaged(false);
        imagePreviewLabel.setVisible(true);
        imagePreviewLabel.setManaged(true);
        btnSupprimerImage.setVisible(false);
        btnSupprimerImage.setManaged(false);
        imageInfoLabel.setText("");
    }

    // Affiche l'image existante dans la prévisualisation (mode modification)
    private void afficherImageExistante(String imageName, Integer imageSize) {
        try {
            // Chercher dans le dossier quiz_images
            Path imgPath = Paths.get("src/main/resources/images/quiz", imageName);
            if (Files.exists(imgPath)) {
                Image img = new Image(imgPath.toUri().toString());
                imagePreview.setImage(img);
                imagePreview.setVisible(true);
                imagePreview.setManaged(true);
                imagePreviewLabel.setVisible(false);
                imagePreviewLabel.setManaged(false);
                btnSupprimerImage.setVisible(true);
                btnSupprimerImage.setManaged(true);
                String sizeStr = imageSize == null ? "" :
                    imageSize < 1024 ? imageSize + " o" :
                    imageSize < 1024 * 1024 ? (imageSize / 1024) + " Ko" :
                    String.format("%.1f Mo", imageSize / 1024.0 / 1024.0);
                imageInfoLabel.setText("📄 " + imageName + (sizeStr.isEmpty() ? "" : "  •  " + sizeStr));
            }
        } catch (Exception e) {
            System.err.println("[QuizForm] Image existante introuvable : " + imageName);
        }
    }

    // Copie l'image dans le dossier ressources et retourne [nomFichier, taille]
    private String[] saveImageFile(File file) {
        try {
            Path destDir = Paths.get("src/main/resources/images/quiz");
            Files.createDirectories(destDir);
            // Nom unique : timestamp + nom original
            String uniqueName = System.currentTimeMillis() + "_" + file.getName()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
            Path dest = destDir.resolve(uniqueName);
            Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return new String[]{uniqueName, String.valueOf(file.length())};
        } catch (IOException e) {
            System.err.println("[QuizForm] Erreur copie image : " + e.getMessage());
            showError("⚠ Impossible de sauvegarder l'image : " + e.getMessage());
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    // Remet un champ à son style normal et efface le message d'erreur
    private void resetField(Control field) {
        field.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
    }

    // Applique un fond sombre au popup ListView d'un ComboBox
    private <T> void styleComboPopup(ComboBox<T> combo) {
        combo.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                javafx.scene.control.skin.ComboBoxListViewSkin<?> skin =
                    (javafx.scene.control.skin.ComboBoxListViewSkin<?>) combo.getSkin();
                if (skin != null) {
                    javafx.scene.Node popup = skin.getPopupContent();
                    if (popup != null) {
                        popup.setStyle(
                            "-fx-background-color:#1a2e1f;" +
                            "-fx-border-color:rgba(255,255,255,0.1);" +
                            "-fx-border-radius:8;" +
                            "-fx-background-radius:8;"
                        );
                    }
                }
            }
        });
    }

    // Remet tous les champs à leur style normal
    private void resetAll() {
        titreField.setStyle(FIELD_NORMAL);
        descriptionField.setStyle(FIELD_NORMAL);
        dureeField.setStyle(FIELD_NORMAL);
        seuilField.setStyle(FIELD_NORMAL);
        tentativesField.setStyle(FIELD_NORMAL);
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    // Navigue vers la liste des quiz dans la zone de contenu principale
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

    // Affiche une alerte de succès ou d'échec
    private void showAlert(boolean success, String msgOk, String msgEchec) {
        Alert alert = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setTitle(success ? "✅ Succès" : "❌ Échec");
        alert.setContentText(success ? msgOk : msgEchec);
        alert.showAndWait(); // bloque jusqu'à ce que l'utilisateur ferme l'alerte
    }
}
