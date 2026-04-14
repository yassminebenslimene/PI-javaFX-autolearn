package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.PasswordUtil;

import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private CheckBox      checkRememberMe;
    @FXML private Label         errorLabel;

    private final UserService service = new UserService();
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    public void initialize() {
        String savedEmail = prefs.get("remembered_email", "");
        String savedPass  = prefs.get("remembered_pass", "");
        if (!savedEmail.isEmpty()) {
            fieldEmail.setText(savedEmail);
            fieldPassword.setText(savedPass);
            checkRememberMe.setSelected(true);
        }
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User found = service.trouverParEmail(email);

        if (found == null) {
            showError("Aucun compte trouvé avec cet email.");
            return;
        }

        if (!PasswordUtil.verify(password, found.getPassword())) {
            showError("Mot de passe incorrect.");
            return;
        }

        if (found.isIsSuspended()) {
            String reason = found.getSuspensionReason();
            showError("Compte suspendu" + (reason != null ? " : " + reason : "."));
            return;
        }

        if (checkRememberMe.isSelected()) {
            prefs.put("remembered_email", email);
            prefs.put("remembered_pass", password);
        } else {
            prefs.remove("remembered_email");
            prefs.remove("remembered_pass");
        }

        SessionManager.login(found);

        try {
            if ("ADMIN".equals(found.getRole())) MainApp.showBackoffice();
            else                                  MainApp.showFrontoffice();
        } catch (Exception e) {
            showError("Erreur de navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML private void onGoToRegister() {
        try { MainApp.showRegister(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onForgotPassword() {
        try { MainApp.showResetPassword(); } catch (Exception e) { e.printStackTrace(); }
    }
}
