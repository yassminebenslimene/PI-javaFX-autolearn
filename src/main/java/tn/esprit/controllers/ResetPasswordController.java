package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.PasswordUtil;

import java.util.List;

public class ResetPasswordController {

    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldNewPassword;
    @FXML private PasswordField fieldConfirmPassword;
    @FXML private Label         errorEmail;
    @FXML private Label         errorPassword;
    @FXML private Label         errorConfirm;
    @FXML private Label         successLabel;

    private final UserService service = new UserService();

    @FXML
    private void onReset() {
        clearErrors();
        boolean valid = true;

        String email    = fieldEmail.getText().trim();
        String password = fieldNewPassword.getText().trim();
        String confirm  = fieldConfirmPassword.getText().trim();

        if (email.isEmpty()) { errorEmail.setText("L'email est obligatoire"); valid = false; }
        else {
            List<User> all = service.afficher();
            User found = all.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
            if (found == null) { errorEmail.setText("Aucun compte trouvé avec cet email"); valid = false; }
        }

        if (password.isEmpty()) { errorPassword.setText("Le mot de passe est obligatoire"); valid = false; }
        else if (password.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
        else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
            errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
        }

        if (confirm.isEmpty()) { errorConfirm.setText("Veuillez confirmer le mot de passe"); valid = false; }
        else if (!confirm.equals(password)) { errorConfirm.setText("Les mots de passe ne correspondent pas"); valid = false; }

        if (!valid) return;

        // Find and update
        List<User> all = service.afficher();
        User found = all.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst().orElse(null);
        if (found != null) {
            found.setPassword(PasswordUtil.hash(password));
            service.modifier(found);
            successLabel.setText("Mot de passe réinitialisé avec succès !");
            successLabel.setStyle("-fx-text-fill:#27ae60; -fx-font-weight:bold;");
        }
    }

    @FXML private void onBackToLogin() {
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void clearErrors() {
        errorEmail.setText(""); errorPassword.setText("");
        errorConfirm.setText(""); successLabel.setText("");
    }
}
