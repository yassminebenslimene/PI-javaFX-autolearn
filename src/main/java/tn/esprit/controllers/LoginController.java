package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.PasswordUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private ComboBox<String> fieldEmail;
    @FXML private PasswordField    fieldPassword;
    @FXML private CheckBox         checkRememberMe;
    @FXML private Label            errorLabel;

    private final UserService service = new UserService();
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    public void initialize() {
        // Charger l'historique des emails
        String history = prefs.get("email_history", "");
        List<String> emails = new ArrayList<>();
        if (!history.isEmpty()) {
            emails.addAll(Arrays.asList(history.split("\\|")));
        }
        fieldEmail.getItems().addAll(emails);

        // Pré-remplir le dernier email connecté avec "Remember Me"
        String savedEmail = prefs.get("remembered_email", "");
        String savedPass  = prefs.get("remembered_pass", "");
        if (!savedEmail.isEmpty()) {
            fieldEmail.setValue(savedEmail);
            fieldPassword.setText(savedPass);
            checkRememberMe.setSelected(true);
        }

        // Autocomplete : filtrer les suggestions en tapant
        fieldEmail.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                fieldEmail.hide();
                return;
            }
            String typed = newVal.toLowerCase();
            List<String> filtered = emails.stream()
                .filter(e -> e.toLowerCase().startsWith(typed))
                .toList();
            if (!filtered.isEmpty()) {
                fieldEmail.getItems().setAll(filtered);
                fieldEmail.show();
            } else {
                fieldEmail.hide();
            }
        });
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String email    = fieldEmail.getEditor().getText().trim();
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

        // Sauvegarder dans l'historique
        addToHistory(email);

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

    /** Ajoute l'email à l'historique (max 10, pas de doublons) */
    private void addToHistory(String email) {
        String history = prefs.get("email_history", "");
        List<String> list = new ArrayList<>();
        if (!history.isEmpty()) list.addAll(Arrays.asList(history.split("\\|")));
        list.remove(email); // supprimer si déjà présent
        list.add(0, email); // ajouter en premier
        if (list.size() > 10) list = list.subList(0, 10);
        prefs.put("email_history", String.join("|", list));
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
