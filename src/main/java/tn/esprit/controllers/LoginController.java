package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.PasswordUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private TextField     fieldPasswordVisible;
    @FXML private Button        btnTogglePassword;
    @FXML private CheckBox      checkRememberMe;
    @FXML private Label         errorLabel;

    private boolean passwordVisible = false;

    private final UserService service = new UserService();
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    // email -> mot de passe (seulement ceux avec Remember Me)
    private final Map<String, String> savedCredentials = new LinkedHashMap<>();
    // tous les emails utilisés (historique)
    private final List<String> emailHistory = new ArrayList<>();

    private final ContextMenu suggestionMenu = new ContextMenu();

    @FXML
    public void initialize() {
        loadHistory();

        // Pré-remplir si Remember Me était coché
        String savedEmail = prefs.get("remembered_email", "");
        String savedPass  = prefs.get("remembered_pass", "");
        if (!savedEmail.isEmpty()) {
            fieldEmail.setText(savedEmail);
            fieldPassword.setText(savedPass);
            checkRememberMe.setSelected(true);
        }

        // Autocomplete en temps réel
        fieldEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                suggestionMenu.hide();
                return;
            }
            showSuggestions(newVal.trim());
        });

        // Quand on sélectionne via clavier dans le menu, fermer proprement
        fieldEmail.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) suggestionMenu.hide();
        });
    }

    private void showSuggestions(String typed) {
        String lower = typed.toLowerCase();
        List<String> matches = emailHistory.stream()
            .filter(e -> e.toLowerCase().startsWith(lower))
            .toList();

        if (matches.isEmpty()) {
            suggestionMenu.hide();
            return;
        }

        suggestionMenu.getItems().clear();
        for (String email : matches) {
            MenuItem item = new MenuItem(email);
            item.setStyle("-fx-font-size:13; -fx-padding:8 14 8 14;");
            item.setOnAction(e -> {
                // Bloquer le listener le temps de setter le texte
                fieldEmail.setText(email);
                fieldEmail.positionCaret(email.length());
                suggestionMenu.hide();

                // Auto-remplir le mot de passe si sauvegardé
                String pass = savedCredentials.get(email);
                if (pass != null && !pass.isEmpty()) {
                    fieldPassword.setText(pass);
                    checkRememberMe.setSelected(true);
                } else {
                    fieldPassword.clear();
                    checkRememberMe.setSelected(false);
                }
            });
            suggestionMenu.getItems().add(item);
        }

        suggestionMenu.show(fieldEmail,
            javafx.geometry.Side.BOTTOM, 0, 0);
    }

    /** Toggle show/hide password */
    @FXML
    private void onTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            fieldPasswordVisible.setText(fieldPassword.getText());
            fieldPasswordVisible.setVisible(true);  fieldPasswordVisible.setManaged(true);
            fieldPassword.setVisible(false);         fieldPassword.setManaged(false);
            btnTogglePassword.setText("\uD83D\uDE48"); // 🙈
        } else {
            fieldPassword.setText(fieldPasswordVisible.getText());
            fieldPassword.setVisible(true);          fieldPassword.setManaged(true);
            fieldPasswordVisible.setVisible(false);  fieldPasswordVisible.setManaged(false);
            btnTogglePassword.setText("\uD83D\uDC41"); // 👁
        }
    }

    /** Returns the current password regardless of which field is active */
    private String getPassword() {
        return passwordVisible ? fieldPasswordVisible.getText() : fieldPassword.getText();
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String email    = fieldEmail.getText().trim();
        String password = getPassword();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User found = service.trouverParEmail(email);
        if (found == null) {
            showError("Aucun compte trouve avec cet email.");
            return;
        }
        if (!PasswordUtil.verify(password, found.getPassword())) {
            showError("Mot de passe incorrect.");
            return;
        }

        // ── Inactivity check: auto-suspend after 60 days ──────────────────────
        if (!found.isIsSuspended()) {
            java.util.Date lastActivityDate = found.getLastLoginAt() != null
                ? found.getLastLoginAt()
                : found.getCreatedAt();

            LocalDateTime lastActivity = lastActivityDate != null
                ? lastActivityDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
                : LocalDateTime.now();

            long daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastActivity, LocalDateTime.now());
            if (daysSince >= 60) {
                found.setIsSuspended(true);
                found.setSuspendedAt(new java.util.Date());
                found.setSuspensionReason("Inactivite prolongee (plus de 60 jours sans connexion)");
                service.modifier(found);
                EmailService.sendSuspensionNotification(found.getEmail(), found.getPrenom(),
                    "Votre compte n'a pas ete utilise depuis plus de 60 jours.");
            }
        }

        // ── Block suspended users ─────────────────────────────────────────────
        if (found.isIsSuspended()) {
            showError("Compte suspendu : " +
                (found.getSuspensionReason() != null ? found.getSuspensionReason() : "") +
                "\nContactez autolearn66@gmail.com pour plus d'informations.");
            return;
        }

        // ── Update lastLoginAt ────────────────────────────────────────────────
        found.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now()));
        service.modifier(found);

        // Sauvegarder dans l'historique
        addToHistory(email);

        if (checkRememberMe.isSelected()) {
            prefs.put("remembered_email", email);
            prefs.put("remembered_pass", password);
            savedCredentials.put(email, password);
            saveCredentials();
        } else {
            prefs.remove("remembered_email");
            prefs.remove("remembered_pass");
            savedCredentials.remove(email);
            saveCredentials();
        }

        SessionManager.login(found);
        try {
            if ("ADMIN".equals(found.getRole())) MainApp.showBackoffice();
            else                                  MainApp.showFrontoffice();
        } catch (Exception e) {
            showError("Erreur de navigation: " + e.getMessage());
        }
    }

    // ── Persistance ───────────────────────────────────────────────────────────

    private void loadHistory() {
        // Historique emails
        String history = prefs.get("email_history", "");
        if (!history.isEmpty())
            emailHistory.addAll(Arrays.asList(history.split("\\|")));

        // Credentials sauvegardés (Remember Me)
        String creds = prefs.get("saved_credentials", "");
        if (!creds.isEmpty()) {
            for (String entry : creds.split("\\|")) {
                String[] parts = entry.split(":::", 2);
                if (parts.length == 2)
                    savedCredentials.put(parts[0], parts[1]);
            }
        }
    }

    private void addToHistory(String email) {
        emailHistory.remove(email);
        emailHistory.add(0, email);
        if (emailHistory.size() > 10) emailHistory.subList(10, emailHistory.size()).clear();
        prefs.put("email_history", String.join("|", emailHistory));
    }

    private void saveCredentials() {
        StringBuilder sb = new StringBuilder();
        savedCredentials.forEach((email, pass) -> {
            if (sb.length() > 0) sb.append("|");
            sb.append(email).append(":::").append(pass);
        });
        prefs.put("saved_credentials", sb.toString());
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML private void onGoToRegister() {
        try { MainApp.showRegister(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onBackToLanding() {
        try { MainApp.showLanding(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onForgotPassword() {
        try { MainApp.showResetPassword(); } catch (Exception e) { e.printStackTrace(); }
    }
}
