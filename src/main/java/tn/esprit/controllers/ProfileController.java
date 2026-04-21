package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ApiService;
import tn.esprit.services.BlockchainService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.PasswordUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProfileController {

    // Header
    @FXML private Label labelInitials;
    @FXML private javafx.scene.image.ImageView avatarImageView;
    @FXML private Label labelFullName;
    @FXML private Label labelFullName2;
    @FXML private Label labelEmailHeader;
    @FXML private Label labelRole;
    @FXML private Label labelNiveauBadge;

    // Security panel (new)
    @FXML private Label labelSecurityScore;
    @FXML private Label labelSecurityText;
    @FXML private Label labelChainStatus;

    // Geo location (new)
    @FXML private Label labelGeoLoading;
    @FXML private VBox  geoInfoBox;
    @FXML private Label labelGeoCity;
    @FXML private Label labelGeoCountry;
    @FXML private Label labelGeoIp;
    @FXML private Label labelGeoIsp;
    @FXML private Label labelGeoError;

    // Blockchain history (new)
    @FXML private VBox blockchainHistoryBox;

    // Form fields
    @FXML private TextField        fieldNom;
    @FXML private TextField        fieldPrenom;
    @FXML private TextField        fieldEmail;
    @FXML private PasswordField    fieldPassword;
    @FXML private PasswordField    fieldConfirmPassword;
    @FXML private Label            labelRoleDisplay;
    @FXML private VBox             labelNiveauRow;
    @FXML private ComboBox<String> comboNiveau;

    // Errors + success
    @FXML private Label errorNom;
    @FXML private Label errorPrenom;
    @FXML private Label errorEmail;
    @FXML private Label errorPassword;
    @FXML private Label errorConfirmPassword;
    @FXML private Label successLabel;

    private final UserService service = new UserService();

    // ── Initialize ────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        User u = SessionManager.getCurrentUser();
        if (u == null) return;

        if (u.getId() > 0) {
            User fresh = service.trouver(u.getId());
            if (fresh != null) { SessionManager.login(fresh); u = fresh; }
        }

        // Create genesis block if first time (writes to file, not DB)
        BlockchainService.createGenesisBlock(u.getId());

        populateView(u);
        loadBlockchainHistory(u.getId());
        updateSecurityPanel(u.getId());
        loadGeoLocation();
    }

    // ── Populate view ─────────────────────────────────────────────────────────

    private void populateView(User u) {
        String initials = u.getPrenom().substring(0, 1).toUpperCase()
                        + u.getNom().substring(0, 1).toUpperCase();
        if (labelInitials    != null) labelInitials.setText(initials);
        if (labelFullName    != null) labelFullName.setText(u.getPrenom() + " " + u.getNom());
        if (labelFullName2   != null) labelFullName2.setText(u.getPrenom() + " " + u.getNom());
        if (labelEmailHeader != null) labelEmailHeader.setText(u.getEmail());
        if (labelRole        != null) labelRole.setText(u.getRole());

        // Gravatar async
        ApiService.fetchGravatarBytes(u.getEmail(), 100).thenAccept(bytes -> {
            if (bytes != null && bytes.length > 0) {
                javafx.application.Platform.runLater(() -> {
                    try {
                        var img = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes));
                        if (!img.isError() && avatarImageView != null) {
                            avatarImageView.setImage(img);
                            avatarImageView.setVisible(true);
                            avatarImageView.setManaged(true);
                            if (labelInitials != null) {
                                labelInitials.setVisible(false);
                                labelInitials.setManaged(false);
                            }
                        }
                    } catch (Exception ignored) {}
                });
            }
        });

        if (fieldNom    != null) fieldNom.setText(u.getNom());
        if (fieldPrenom != null) fieldPrenom.setText(u.getPrenom());
        if (fieldEmail  != null) fieldEmail.setText(u.getEmail());
        if (labelRoleDisplay != null) labelRoleDisplay.setText(u.getRole());

        boolean isEtudiant = u instanceof Etudiant;
        if (labelNiveauBadge != null) {
            labelNiveauBadge.setVisible(isEtudiant);
            labelNiveauBadge.setManaged(isEtudiant);
            if (isEtudiant && ((Etudiant) u).getNiveau() != null)
                labelNiveauBadge.setText("Niveau : " + ((Etudiant) u).getNiveau());
        }
        if (comboNiveau != null) {
            comboNiveau.setItems(FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
            if (isEtudiant) comboNiveau.setValue(((Etudiant) u).getNiveau());
        }
        if (labelNiveauRow != null) {
            labelNiveauRow.setVisible(isEtudiant);
            labelNiveauRow.setManaged(isEtudiant);
        }
    }

    // ── Geo Location ──────────────────────────────────────────────────────────

    private void loadGeoLocation() {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            
            System.out.println("[Profile] Loading geo location...");
            ApiService.GeoInfo geo = ApiService.getMyGeoInfo();
            System.out.println("[Profile] Geo result: " + geo);
            
            javafx.application.Platform.runLater(() -> {
                if (labelGeoLoading != null) {
                    labelGeoLoading.setVisible(false);
                    labelGeoLoading.setManaged(false);
                }
                if (geo != null) {
                    if (labelGeoCity    != null) labelGeoCity.setText(geo.city());
                    if (labelGeoCountry != null) labelGeoCountry.setText(geo.country());
                    if (labelGeoIp      != null) labelGeoIp.setText(geo.ip());
                    if (labelGeoIsp     != null) labelGeoIsp.setText(geo.isp());
                    if (geoInfoBox      != null) {
                        geoInfoBox.setVisible(true);
                        geoInfoBox.setManaged(true);
                    }
                } else {
                    if (labelGeoError != null) {
                        labelGeoError.setText("Localisation non disponible (verifiez votre connexion).");
                        labelGeoError.setVisible(true);
                        labelGeoError.setManaged(true);
                    }
                }
            });
        });
    }

    // ── Security panel ────────────────────────────────────────────────────────

    private void updateSecurityPanel(int userId) {
        int score = BlockchainService.computeSecurityScore(userId);
        BlockchainService.ValidationResult v = BlockchainService.validateChain(userId);

        if (labelSecurityScore != null) {
            labelSecurityScore.setText(score + "/100");
            String color = score >= 70 ? "#ffffff" : score >= 40 ? "#fef3c7" : "#fee2e2";
            labelSecurityScore.setStyle(
                "-fx-font-size:26; -fx-font-weight:900; -fx-text-fill:" + color + ";");
        }

        if (labelSecurityText != null) {
            String text = score >= 70 ? "Bon niveau"
                        : score >= 40 ? "A ameliorer"
                        :               "Faible";
            labelSecurityText.setText(text);
        }

        if (labelChainStatus != null) {
            labelChainStatus.setText(v.message());
            labelChainStatus.setStyle(
                "-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:" +
                (v.isValid() ? "#16a34a" : "#dc2626") + ";");
        }
    }

    // ── Blockchain history ────────────────────────────────────────────────────

    private void loadBlockchainHistory(int userId) {
        if (blockchainHistoryBox == null) return;
        blockchainHistoryBox.getChildren().clear();

        List<BlockchainService.Block> chain = BlockchainService.getChain(userId);

        if (chain.isEmpty()) {
            Label empty = new Label("Aucun historique disponible");
            empty.setStyle("-fx-text-fill:#aaa; -fx-font-size:12;");
            blockchainHistoryBox.getChildren().add(empty);
            return;
        }

        // Most recent first, skip genesis
        for (int i = chain.size() - 1; i >= 0; i--) {
            blockchainHistoryBox.getChildren().add(buildBlockCard(chain.get(i)));
        }
    }

    private VBox buildBlockCard(BlockchainService.Block block) {
        boolean valid = block.isValid();
        boolean isGenesis = "GENESIS".equals(block.action);

        VBox card = new VBox(4);
        card.setStyle(
            "-fx-background-color:" + (isGenesis ? "#f5f3ff" : "#f9fafb") + ";" +
            "-fx-border-color:"     + (valid ? "#e5e7eb" : "#fca5a5") + ";" +
            "-fx-border-radius:8; -fx-background-radius:8;" +
            "-fx-border-width:1; -fx-padding:10 14 10 14;"
        );

        // Action label + date on same row
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Colored dot
        Label dot = new Label(isGenesis ? "●" : "●");
        dot.setStyle("-fx-font-size:8; -fx-text-fill:"
            + (isGenesis ? "#7a6ad8" : (valid ? "#10b981" : "#ef4444")) + ";");

        Label actionLbl = new Label(block.actionLabel());
        actionLbl.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#111827;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Format date nicely: "21 avr. 2026 a 23:12"
        Label timeLbl = new Label(formatDate(block.timestamp));
        timeLbl.setStyle("-fx-font-size:10; -fx-text-fill:#9ca3af;");

        row.getChildren().addAll(dot, actionLbl, spacer, timeLbl);
        card.getChildren().add(row);

        // Show what changed in plain French (no hashes)
        if (!isGenesis && block.data != null && !block.data.isEmpty()) {
            Label detailLbl = new Label(parseDataToFrench(block.data));
            detailLbl.setStyle("-fx-font-size:11; -fx-text-fill:#6b7280;");
            detailLbl.setWrapText(true);
            card.getChildren().add(detailLbl);
        }

        return card;
    }

    /** Converts JSON data to plain French text */
    private String parseDataToFrench(String data) {
        try {
            // Simple parsing without full JSON library
            if (data.contains("email_new")) {
                String newVal = extractValue(data, "email_new");
                return "Nouvel email : " + newVal;
            }
            if (data.contains("password_changed")) {
                return "Mot de passe mis a jour";
            }
            if (data.contains("niveau_new")) {
                String newVal = extractValue(data, "niveau_new");
                return "Nouveau niveau : " + newVal;
            }
            if (data.contains("nom_new") || data.contains("prenom_new")) {
                return "Nom ou prenom modifie";
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String extractValue(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) return "";
            int colon = json.indexOf(":", idx);
            int start = json.indexOf("\"", colon) + 1;
            int end   = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) { return ""; }
    }

    private String formatDate(String timestamp) {
        try {
            // Input: "2026-04-21 23:12:57"
            String[] parts = timestamp.split(" ");
            String[] dateParts = parts[0].split("-");
            String[] months = {"", "jan.", "fev.", "mar.", "avr.", "mai", "juin",
                               "juil.", "aout", "sep.", "oct.", "nov.", "dec."};
            int month = Integer.parseInt(dateParts[1]);
            String time = parts[1].substring(0, 5); // HH:mm
            return dateParts[2] + " " + months[month] + " " + dateParts[0] + "  " + time;
        } catch (Exception e) {
            return timestamp;
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void onSave() {
        clearErrors();
        if (!validate()) return;

        User u = SessionManager.getCurrentUser();

        // Detect what changed → for blockchain block
        Map<String, Object> changes = new LinkedHashMap<>();
        String action = "PROFILE_UPDATE";

        if (!u.getNom().equals(fieldNom.getText().trim())) {
            changes.put("nom_old", u.getNom());
            changes.put("nom_new", fieldNom.getText().trim());
        }
        if (!u.getPrenom().equals(fieldPrenom.getText().trim())) {
            changes.put("prenom_old", u.getPrenom());
            changes.put("prenom_new", fieldPrenom.getText().trim());
        }
        if (!u.getEmail().equals(fieldEmail.getText().trim())) {
            changes.put("email_old", u.getEmail());
            changes.put("email_new", fieldEmail.getText().trim());
            action = "EMAIL_CHANGE";
        }

        String pwd = fieldPassword.getText().trim();
        if (!pwd.isEmpty()) {
            changes.put("password_changed", "true");
            action = "PASSWORD_CHANGE";
        }

        if (u instanceof Etudiant e && comboNiveau != null && comboNiveau.getValue() != null) {
            if (e.getNiveau() != null && !comboNiveau.getValue().equals(e.getNiveau())) {
                changes.put("niveau_old", e.getNiveau());
                changes.put("niveau_new", comboNiveau.getValue());
                if ("PROFILE_UPDATE".equals(action)) action = "NIVEAU_CHANGE";
            }
        }

        // Apply changes to user
        u.setNom(fieldNom.getText().trim());
        u.setPrenom(fieldPrenom.getText().trim());
        u.setEmail(fieldEmail.getText().trim());
        if (!pwd.isEmpty()) u.setPassword(PasswordUtil.hash(pwd));
        if (u instanceof Etudiant e && comboNiveau != null && comboNiveau.getValue() != null)
            e.setNiveau(comboNiveau.getValue());

        service.modifier(u);
        SessionManager.login(u);

        // Add blockchain block if something actually changed
        if (!changes.isEmpty()) {
            BlockchainService.addBlock(u.getId(), action, changes);
        }

        // Log activity
        ActivityApiClient.logAsync(u.getId(), "user.update_profile",
            Map.of("email", u.getEmail(), "role", u.getRole()));

        // Refresh UI
        populateView(u);
        loadBlockchainHistory(u.getId());
        updateSecurityPanel(u.getId());

        successLabel.setText("✔  Profil mis à jour avec succès !");
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    @FXML
    private void onBack() {
        try {
            User u = SessionManager.getCurrentUser();
            if (u == null) { MainApp.showLogin(); return; }
            if ("ADMIN".equals(u.getRole())) MainApp.showBackoffice();
            else MainApp.showFrontoffice();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private boolean validate() {
        boolean valid = true;
        String nom     = fieldNom.getText().trim();
        String prenom  = fieldPrenom.getText().trim();
        String email   = fieldEmail.getText().trim();
        String pwd     = fieldPassword.getText().trim();
        String confirm = fieldConfirmPassword != null
                       ? fieldConfirmPassword.getText().trim() : "";

        if (nom.isEmpty() || nom.length() < 2)
            { errorNom.setText("Nom invalide (min 2 car.)"); valid = false; }
        else if (!nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$"))
            { errorNom.setText("Lettres uniquement"); valid = false; }

        if (prenom.isEmpty() || prenom.length() < 2)
            { errorPrenom.setText("Prénom invalide (min 2 car.)"); valid = false; }
        else if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$"))
            { errorPrenom.setText("Lettres uniquement"); valid = false; }

        if (email.isEmpty() || !email.matches(
                "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"))
            { errorEmail.setText("Email invalide"); valid = false; }

        if (!pwd.isEmpty()) {
            if (pwd.length() < 6)
                { errorPassword.setText("Minimum 6 caractères"); valid = false; }
            else if (!pwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$"))
                { errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false; }
            else if (!pwd.equals(confirm)) {
                if (errorConfirmPassword != null)
                    errorConfirmPassword.setText("Les mots de passe ne correspondent pas");
                valid = false;
            }
        }
        return valid;
    }

    private void clearErrors() {
        if (errorNom            != null) errorNom.setText("");
        if (errorPrenom         != null) errorPrenom.setText("");
        if (errorEmail          != null) errorEmail.setText("");
        if (errorPassword       != null) errorPassword.setText("");
        if (errorConfirmPassword!= null) errorConfirmPassword.setText("");
        if (successLabel        != null) {
            successLabel.setText("");
            successLabel.setVisible(false);
            successLabel.setManaged(false);
        }
    }
}
