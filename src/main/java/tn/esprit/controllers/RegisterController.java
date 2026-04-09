package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Admin;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.PasswordUtil;

import java.util.List;

public class RegisterController {

    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldConfirmPassword;
    @FXML private ComboBox<String> comboRole;
    @FXML private Label         labelNiveau;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private Label         errorNom;
    @FXML private Label         errorPrenom;
    @FXML private Label         errorEmail;
    @FXML private Label         errorPassword;
    @FXML private Label         errorConfirm;
    @FXML private Label         errorRole;
    @FXML private Label         errorNiveau;
    @FXML private Label         errorGeneral;

    private final UserService service = new UserService();

    @FXML
    public void initialize() {
        comboRole.setItems(FXCollections.observableArrayList("ADMIN", "ETUDIANT"));
        comboNiveau.setItems(FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE"));

        // Show niveau only for ETUDIANT
        comboRole.valueProperty().addListener((obs, o, newVal) -> {
            boolean isEtudiant = "ETUDIANT".equals(newVal);
            labelNiveau.setVisible(isEtudiant); labelNiveau.setManaged(isEtudiant);
            comboNiveau.setVisible(isEtudiant); comboNiveau.setManaged(isEtudiant);
            errorNiveau.setVisible(isEtudiant); errorNiveau.setManaged(isEtudiant);
            if (!isEtudiant) comboNiveau.setValue(null);
        });
    }

    @FXML
    private void onRegister() {
        if (!validate()) return;

        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String password = PasswordUtil.hash(fieldPassword.getText().trim());
        String role     = comboRole.getValue();
        String niveau   = comboNiveau.getValue();

        User newUser = "ADMIN".equals(role)
                ? new Admin(nom, prenom, email, password)
                : new Etudiant(nom, prenom, email, password, niveau);

        service.ajouter(newUser);

        try {
            if ("ADMIN".equals(role)) MainApp.showBackoffice();
            else                      MainApp.showFrontoffice();
        } catch (Exception e) {
            errorGeneral.setText("Erreur lors de la navigation.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onGoToLogin() {
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean validate() {
        clearErrors();
        boolean valid = true;

        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText().trim();
        String confirm  = fieldConfirmPassword.getText().trim();
        String role     = comboRole.getValue();
        String niveau   = comboNiveau.getValue();

        // Nom
        if (nom.isEmpty()) { errorNom.setText("Le nom est obligatoire"); valid = false; }
        else if (nom.length() < 2 || nom.length() > 50) { errorNom.setText("Entre 2 et 50 caractères"); valid = false; }
        else if (!nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorNom.setText("Lettres uniquement"); valid = false; }

        // Prénom
        if (prenom.isEmpty()) { errorPrenom.setText("Le prénom est obligatoire"); valid = false; }
        else if (prenom.length() < 2 || prenom.length() > 50) { errorPrenom.setText("Entre 2 et 50 caractères"); valid = false; }
        else if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorPrenom.setText("Lettres uniquement"); valid = false; }

        // Email
        if (email.isEmpty()) { errorEmail.setText("L'email est obligatoire"); valid = false; }
        else if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorEmail.setText("Format invalide. Ex: nom@domaine.com"); valid = false;
        } else {
            // Check email uniqueness
            List<User> all = service.afficher();
            boolean exists = all.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
            if (exists) { errorEmail.setText("Cet email est déjà utilisé"); valid = false; }
        }

        // Password
        if (password.isEmpty()) { errorPassword.setText("Le mot de passe est obligatoire"); valid = false; }
        else if (password.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
        else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
            errorPassword.setText("Maj + min + chiffre + spécial (@$!%*?&)"); valid = false;
        }

        // Confirm password
        if (confirm.isEmpty()) { errorConfirm.setText("Veuillez confirmer le mot de passe"); valid = false; }
        else if (!confirm.equals(fieldPassword.getText().trim())) {
            errorConfirm.setText("Les mots de passe ne correspondent pas"); valid = false;
        }

        // Rôle
        if (role == null) { errorRole.setText("Le rôle est obligatoire"); valid = false; }

        // Niveau
        if ("ETUDIANT".equals(role) && (niveau == null || niveau.isEmpty())) {
            errorNiveau.setText("Le niveau est obligatoire pour un étudiant"); valid = false;
        }

        return valid;
    }

    private void clearErrors() {
        errorNom.setText(""); errorPrenom.setText(""); errorEmail.setText("");
        errorPassword.setText(""); errorConfirm.setText(""); errorRole.setText("");
        errorNiveau.setText(""); errorGeneral.setText("");
    }
}
