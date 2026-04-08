package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;
import javafx.scene.layout.VBox;

import tn.esprit.tools.PasswordUtil;

public class ProfileController {

    // Header
    @FXML private Label labelInitials;
    @FXML private Label labelFullName;
    @FXML private Label labelFullName2;   // inside the white card
    @FXML private Label labelEmailHeader;
    @FXML private Label labelRole;
    @FXML private Label labelNiveauBadge;

    // Form fields
    @FXML private TextField        fieldNom;
    @FXML private TextField        fieldPrenom;
    @FXML private TextField        fieldEmail;
    @FXML private PasswordField    fieldPassword;
    @FXML private PasswordField    fieldConfirmPassword;
    @FXML private Label            labelRoleDisplay;  // read-only role in form body
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

    @FXML
    public void initialize() {
        User u = SessionManager.getCurrentUser();
        if (u == null) {
            System.err.println("ProfileController: no session user");
            return;
        }
        System.out.println("ProfileController: loading profile for id=" + u.getId() + " name=" + u.getNom());

        // Reload fresh from DB if we have a valid id
        if (u.getId() > 0) {
            User fresh = service.trouver(u.getId());
            if (fresh != null) { SessionManager.login(fresh); u = fresh; }
        }

        populateView(u);
    }

    private void populateView(User u) {
        // Avatar initials
        String initials = u.getPrenom().substring(0, 1).toUpperCase()
                        + u.getNom().substring(0, 1).toUpperCase();
        if (labelInitials != null) labelInitials.setText(initials);
        if (labelFullName != null) labelFullName.setText(u.getPrenom() + " " + u.getNom());
        if (labelFullName2 != null) labelFullName2.setText(u.getPrenom() + " " + u.getNom());
        if (labelEmailHeader != null) labelEmailHeader.setText(u.getEmail());
        if (labelRole != null) labelRole.setText(u.getRole());

        // Pre-fill form with current data
        if (fieldNom != null) fieldNom.setText(u.getNom());
        if (fieldPrenom != null) fieldPrenom.setText(u.getPrenom());
        if (fieldEmail != null) fieldEmail.setText(u.getEmail());
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

    @FXML
    private void onBack() {
        try {
            User u = SessionManager.getCurrentUser();
            if (u == null) { MainApp.showLogin(); return; }
            if ("ADMIN".equals(u.getRole())) MainApp.showBackoffice();
            else MainApp.showFrontoffice();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onSave() {
        clearErrors();
        if (!validate()) return;

        User u = SessionManager.getCurrentUser();
        u.setNom(fieldNom.getText().trim());
        u.setPrenom(fieldPrenom.getText().trim());
        u.setEmail(fieldEmail.getText().trim());

        String pwd = fieldPassword.getText().trim();
        if (!pwd.isEmpty()) u.setPassword(PasswordUtil.hash(pwd));

        if (u instanceof Etudiant e && comboNiveau != null && comboNiveau.getValue() != null)
            e.setNiveau(comboNiveau.getValue());

        service.modifier(u);
        SessionManager.login(u);

        // Refresh header
        populateView(u);

        successLabel.setText("✔  Profil mis à jour avec succès !");
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private boolean validate() {
        boolean valid = true;
        String nom    = fieldNom.getText().trim();
        String prenom = fieldPrenom.getText().trim();
        String email  = fieldEmail.getText().trim();
        String pwd    = fieldPassword.getText().trim();
        String confirm = fieldConfirmPassword != null ? fieldConfirmPassword.getText().trim() : "";

        if (nom.isEmpty() || nom.length() < 2) { errorNom.setText("Nom invalide (min 2 car.)"); valid = false; }
        else if (!nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorNom.setText("Lettres uniquement"); valid = false; }

        if (prenom.isEmpty() || prenom.length() < 2) { errorPrenom.setText("Prénom invalide (min 2 car.)"); valid = false; }
        else if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorPrenom.setText("Lettres uniquement"); valid = false; }

        if (email.isEmpty() || !email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorEmail.setText("Email invalide"); valid = false;
        }

        if (!pwd.isEmpty()) {
            if (pwd.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
            else if (!pwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
                errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
            } else if (!pwd.equals(confirm)) {
                if (errorConfirmPassword != null) errorConfirmPassword.setText("Les mots de passe ne correspondent pas");
                valid = false;
            }
        }
        return valid;
    }

    private void clearErrors() {
        if (errorNom != null) errorNom.setText("");
        if (errorPrenom != null) errorPrenom.setText("");
        if (errorEmail != null) errorEmail.setText("");
        if (errorPassword != null) errorPassword.setText("");
        if (errorConfirmPassword != null) errorConfirmPassword.setText("");
        if (successLabel != null) { successLabel.setText(""); successLabel.setVisible(false); successLabel.setManaged(false); }
    }
}
