package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.tools.PasswordUtil;

import java.security.SecureRandom;
import java.util.List;

public class ResetPasswordController {

    @FXML private TextField     fieldEmail;
    @FXML private TextField     fieldCode;
    @FXML private PasswordField fieldNewPassword;
    @FXML private PasswordField fieldConfirmPassword;
    @FXML private Label         errorEmail;
    @FXML private Label         errorCode;
    @FXML private Label         errorPassword;
    @FXML private Label         errorConfirm;
    @FXML private Label         successLabel;

    // Step 1: email entry
    @FXML private javafx.scene.layout.VBox stepEmail;
    // Step 2: code + new password
    @FXML private javafx.scene.layout.VBox stepReset;

    private final UserService service = new UserService();
    private String pendingCode;
    private User   pendingUser;

    @FXML
    public void initialize() {
        // Start on step 1
        if (stepEmail != null) { stepEmail.setVisible(true);  stepEmail.setManaged(true); }
        if (stepReset != null) { stepReset.setVisible(false); stepReset.setManaged(false); }
    }

    /** Step 1 — validate email and send code */
    @FXML
    private void onSendCode() {
        if (errorEmail != null) errorEmail.setText("");
        if (successLabel != null) successLabel.setText("");

        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) {
            if (errorEmail != null) errorEmail.setText("L'email est obligatoire");
            return;
        }

        List<User> all = service.afficher();
        User found = all.stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(email))
            .findFirst().orElse(null);

        if (found == null) {
            if (errorEmail != null) errorEmail.setText("Aucun compte trouvé avec cet email");
            return;
        }

        // Generate 6-digit code
        pendingCode = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        pendingUser = found;

        // Send email (async)
        EmailService.sendPasswordReset(found.getEmail(), found.getPrenom(), pendingCode);

        // Move to step 2
        if (stepEmail != null) { stepEmail.setVisible(false); stepEmail.setManaged(false); }
        if (stepReset != null) { stepReset.setVisible(true);  stepReset.setManaged(true); }
        if (successLabel != null) {
            successLabel.setText("Un code a été envoyé à " + email);
            successLabel.setStyle("-fx-text-fill:#7a6ad8; -fx-font-size:12;");
        }
    }

    /** Step 2 — verify code and set new password */
    @FXML
    private void onReset() {
        clearErrors();
        boolean valid = true;

        String code     = fieldCode != null ? fieldCode.getText().trim() : "";
        String password = fieldNewPassword.getText().trim();
        String confirm  = fieldConfirmPassword.getText().trim();

        if (code.isEmpty()) {
            if (errorCode != null) errorCode.setText("Le code est obligatoire");
            valid = false;
        } else if (!code.equals(pendingCode)) {
            if (errorCode != null) errorCode.setText("Code incorrect");
            valid = false;
        }

        if (password.isEmpty()) { if (errorPassword != null) errorPassword.setText("Le mot de passe est obligatoire"); valid = false; }
        else if (password.length() < 6) { if (errorPassword != null) errorPassword.setText("Minimum 6 caractères"); valid = false; }
        else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
            if (errorPassword != null) errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
        }

        if (confirm.isEmpty()) { if (errorConfirm != null) errorConfirm.setText("Veuillez confirmer"); valid = false; }
        else if (!confirm.equals(password)) { if (errorConfirm != null) errorConfirm.setText("Les mots de passe ne correspondent pas"); valid = false; }

        if (!valid) return;

        pendingUser.setPassword(PasswordUtil.hash(password));
        service.modifier(pendingUser);

        if (successLabel != null) {
            successLabel.setText("Mot de passe réinitialisé avec succès !");
            successLabel.setStyle("-fx-text-fill:#059669; -fx-font-weight:bold;");
        }
        pendingCode = null;
        pendingUser = null;

        // Auto-navigate to login after 2 seconds
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
            javafx.util.Duration.seconds(2));
        pause.setOnFinished(e -> {
            try { MainApp.showLogin(); } catch (Exception ex) { ex.printStackTrace(); }
        });
        pause.play();
    }

    @FXML private void onBackToLogin() {
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void clearErrors() {
        if (errorCode     != null) errorCode.setText("");
        if (errorPassword != null) errorPassword.setText("");
        if (errorConfirm  != null) errorConfirm.setText("");
    }
}
