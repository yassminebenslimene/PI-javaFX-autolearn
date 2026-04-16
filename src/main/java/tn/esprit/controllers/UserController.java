package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Admin;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

public class UserController {

    // ── Index ─────────────────────────────────────────────────────────────────
    @FXML private TableView<User>            tableUsers;
    @FXML private TableColumn<User, User>    colUser;     // combined avatar+name+email
    @FXML private TableColumn<User, String>  colNiveau;
    @FXML private TableColumn<User, String>  colStatut;
    @FXML private TableColumn<User, String>  colCreated;
    @FXML private TableColumn<User, Void>    colActions;
    @FXML private TextField                  searchField;
    @FXML private Label                      labelTotalUsers;
    @FXML private Label                      labelTotalAdmins;
    @FXML private Label                      labelTotalEtudiants;
    @FXML private HBox                       adminToolbarNew;

    // ── Form ──────────────────────────────────────────────────────────────────
    @FXML private Label         formTitle;
    @FXML private Label         formSubtitle;
    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private Label         labelPasswordHint;
    @FXML private ComboBox<String> comboRole;
    @FXML private Label         labelNiveau;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private Label         errorNom;
    @FXML private Label         errorPrenom;
    @FXML private Label         errorEmail;
    @FXML private Label         errorPassword;
    @FXML private Label         errorRole;
    @FXML private Label         errorNiveau;

    private final UserService service    = new UserService();
    private User              editingUser = null;
    private boolean           isEditMode  = false;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    @FXML
    public void initialize() {
        if (tableUsers != null) initTable();
        if (comboRole  != null) initForm();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TABLE
    // ─────────────────────────────────────────────────────────────────────────
    private void initTable() {
        // Force dark background on the TableView itself
        tableUsers.setStyle(
            "-fx-background-color:#0f1a14; -fx-border-width:0;" +
            "-fx-table-cell-border-color:rgba(255,255,255,0.06);"
        );

        // Dark scrollbar + dark header — applied once skin is ready
        tableUsers.skinProperty().addListener((obs, o, skin) -> {
            javafx.application.Platform.runLater(() -> applyTableDarkTheme());
        });
        // Also apply when scene is set (fallback)
        tableUsers.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) javafx.application.Platform.runLater(() -> applyTableDarkTheme());
        });

        // Force dark row background — overrides JavaFX default white
        tableUsers.setRowFactory(tv -> {
            javafx.scene.control.TableRow<User> row = new javafx.scene.control.TableRow<>();
            row.setStyle("-fx-background-color:#0f1a14;");
            row.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                row.setStyle(isSelected
                    ? "-fx-background-color:rgba(5,150,105,0.18);"
                    : "-fx-background-color:#0f1a14;")
            );
            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (!row.isSelected())
                    row.setStyle(isHover
                        ? "-fx-background-color:rgba(255,255,255,0.04);"
                        : "-fx-background-color:#0f1a14;");
            });
            return row;
        });

        // ── User column ──
        colUser.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue()));
        colUser.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;");
                if (empty || user == null) { setGraphic(null); return; }
                String initials = user.getPrenom().substring(0,1).toUpperCase()
                                + user.getNom().substring(0,1).toUpperCase();
                Label avatar = new Label(initials);
                avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#34d399,#059669);" +
                                "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;" +
                                "-fx-background-radius:50%; -fx-min-width:38; -fx-min-height:38;" +
                                "-fx-max-width:38; -fx-max-height:38; -fx-alignment:CENTER;");
                Label name  = new Label(user.getPrenom() + " " + user.getNom());
                name.setStyle("-fx-text-fill:white; -fx-font-weight:600; -fx-font-size:13;");
                Label email = new Label(user.getEmail());
                email.setStyle("-fx-text-fill:rgba(245,245,244,0.45); -fx-font-size:11;");
                VBox info = new VBox(2, name, email);
                HBox cell = new HBox(10, avatar, info);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setStyle("-fx-padding:3 0 3 4;");
                setGraphic(cell);
            }
        });

        // ── Level badge ──
        colNiveau.setCellValueFactory(data -> {
            User u = data.getValue();
            return new SimpleStringProperty((u instanceof Etudiant e && e.getNiveau() != null) ? e.getNiveau() : "—");
        });
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                if (empty || item == null || "—".equals(item)) { setText("—"); setGraphic(null); return; }
                Label badge = new Label(item);
                String base = "-fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20; -fx-text-fill:white;";
                switch (item) {
                    case "DEBUTANT"      -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#34d399,#059669);");
                    case "INTERMEDIAIRE" -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#fbbf24,#f59e0b); -fx-text-fill:#1a1a1a;");
                    case "AVANCE"        -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#f87171,#dc2626);");
                    default              -> badge.setStyle(base + "-fx-background-color:rgba(255,255,255,0.15);");
                }
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        // ── Status badge ──
        colStatut.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().isIsSuspended() ? "Suspendu" : "Actif"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                if ("Suspendu".equals(item))
                    badge.setStyle("-fx-background-color:linear-gradient(to right,#dc2626,#b91c1c);" +
                                   "-fx-text-fill:white; -fx-font-size:11; -fx-font-weight:700;" +
                                   "-fx-padding:4 12 4 12; -fx-background-radius:20;");
                else
                    badge.setStyle("-fx-background-color:linear-gradient(to right,#34d399,#059669);" +
                                   "-fx-text-fill:white; -fx-font-size:11; -fx-font-weight:700;" +
                                   "-fx-padding:4 12 4 12; -fx-background-radius:20;");
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        // ── Joined date ──
        colCreated.setCellValueFactory(data -> {
            User u = data.getValue();
            return new SimpleStringProperty(u.getCreatedAt() != null ? SDF.format(u.getCreatedAt()) : "—");
        });
        colCreated.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12;");
                setText(empty || item == null ? null : item);
            }
        });

        // ── Actions ──
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnView    = new Button("View");
            private final Button btnEdit    = new Button("Edit");
            private final Button btnSuspend = new Button("Suspend");
            private final HBox   box        = new HBox(6, btnView, btnEdit, btnSuspend);
            {
                String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10;" +
                              "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                btnView.setStyle(base + "-fx-background-color:rgba(14,165,233,0.25); -fx-text-fill:#38bdf8;");
                btnEdit.setStyle(base + "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;");
                btnSuspend.setStyle(base + "-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24;");
                box.setAlignment(Pos.CENTER);
                btnView.setOnAction(e -> openDetailWindow(getTableView().getItems().get(getIndex())));
                btnEdit.setOnAction(e -> { onEditUser(getTableView().getItems().get(getIndex())); loadTable(); });
                btnSuspend.setOnAction(e -> { onSuspendUser(getTableView().getItems().get(getIndex())); loadTable(); });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;");
                if (empty) { setGraphic(null); return; }
                User u = getTableView().getItems().get(getIndex());
                if (u != null) {
                    String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10;" +
                                  "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                    if (u.isIsSuspended()) {
                        btnSuspend.setStyle(base + "-fx-background-color:rgba(52,211,153,0.25); -fx-text-fill:#34d399;");
                        btnSuspend.setText("Réactiver");
                    } else {
                        btnSuspend.setStyle(base + "-fx-background-color:rgba(251,191,36,0.25); -fx-text-fill:#fbbf24;");
                        btnSuspend.setText("Suspend");
                    }
                }
                setGraphic(box);
            }
        });

        // New user button visibility
        if (adminToolbarNew != null) {
            adminToolbarNew.setVisible(SessionManager.isAdmin());
            adminToolbarNew.setManaged(SessionManager.isAdmin());
        }

        try { loadTable(); } catch (Exception e) {
            System.err.println("Erreur chargement: " + e.getMessage());
        }
    }

    private void applyTableDarkTheme() {
        // Header row
        javafx.scene.Node header = tableUsers.lookup("TableHeaderRow");
        if (header != null)
            header.setStyle("-fx-background-color:#0d1710; -fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        tableUsers.lookupAll(".column-header").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710; -fx-border-width:0;"));
        tableUsers.lookupAll(".column-header .label").forEach(n ->
            ((javafx.scene.control.Label) n).setStyle(
                "-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-font-weight:700;"));
        tableUsers.lookupAll(".filler").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710;"));
        // Scrollbars
        tableUsers.lookupAll(".scroll-bar").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
        tableUsers.lookupAll(".scroll-bar .track").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;"));
        tableUsers.lookupAll(".scroll-bar .thumb").forEach(n ->
            n.setStyle("-fx-background-color:rgba(52,211,153,0.22); -fx-background-radius:4;"));
        tableUsers.lookupAll(".increment-button, .decrement-button").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-pref-height:0; -fx-pref-width:0;"));
        tableUsers.lookupAll(".corner").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
    }

    private void loadTable() {
        List<User> all = service.afficher();

        // ADMIN sees only students (not other admins), ETUDIANT sees only etudiants
        List<User> displayed = all.stream().filter(u -> u instanceof Etudiant).toList();

        tableUsers.setItems(FXCollections.observableArrayList(displayed));

        // Stats — always count all
        if (labelTotalUsers    != null) labelTotalUsers.setText(String.valueOf(all.size()));
        if (labelTotalAdmins   != null) labelTotalAdmins.setText(String.valueOf(all.stream().filter(u -> u instanceof Admin).count()));
        if (labelTotalEtudiants!= null) labelTotalEtudiants.setText(String.valueOf(all.stream().filter(u -> u instanceof Etudiant).count()));
    }

    @FXML private void onSearch() {
        String q = searchField.getText().toLowerCase().trim();
        List<User> base = service.afficher().stream().filter(u -> u instanceof Etudiant).toList();
        tableUsers.setItems(FXCollections.observableArrayList(
            base.stream().filter(u ->
                u.getNom().toLowerCase().contains(q) ||
                u.getPrenom().toLowerCase().contains(q) ||
                u.getEmail().toLowerCase().contains(q) ||
                u.getRole().toLowerCase().contains(q)
            ).toList()
        ));
    }

    @FXML private void onClearSearch() { searchField.clear(); loadTable(); }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD actions
    // ─────────────────────────────────────────────────────────────────────────
    @FXML private void onNewUser() {
        if (!SessionManager.isAdmin()) { showAlert(Alert.AlertType.WARNING, "Accès refusé", "Réservé aux administrateurs."); return; }
        openFormWindow(null);
    }

    private void onEditUser(User sel) {
        if (!SessionManager.isAdmin()) { showAlert(Alert.AlertType.WARNING, "Accès refusé", "Réservé aux administrateurs."); return; }
        openFormWindow(sel);
    }

    private void onSuspendUser(User sel) {
        if (!SessionManager.isAdmin()) { showAlert(Alert.AlertType.WARNING, "Accès refusé", "Réservé aux administrateurs."); return; }
        if (!(sel instanceof tn.esprit.entities.Etudiant)) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Seul un étudiant peut être suspendu."); return;
        }
        openSuspendWindow(sel);
    }

    private void openDetailWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/user/show.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails — " + user.getPrenom() + " " + user.getNom());
            stage.setResizable(false);
            // Cap height to 90% of screen height
            double maxH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.88;
            stage.setScene(new Scene(loader.load(), 500, Math.min(580, maxH)));
            ShowUserController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openSuspendWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/user/suspend.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(user.isIsSuspended() ? "Lever la suspension" : "Suspendre — " + user.getPrenom());
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 480, 380));
            SuspendController ctrl = loader.getController();
            ctrl.setUser(user);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void openFormWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/user/form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(user == null ? "Nouvel utilisateur" : "Modifier — " + user.getPrenom() + " " + user.getNom());
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 520, 600));
            UserController ctrl = loader.getController();
            ctrl.setEditingUser(user);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onDeleteUser() {
        showAlert(Alert.AlertType.WARNING, "Action non autorisée", "L'administrateur ne peut pas supprimer un utilisateur. Utilisez la suspension.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FORM
    // ─────────────────────────────────────────────────────────────────────────
    private void initForm() {
        // Admin can only create students — no role selection needed
        comboNiveau.setItems(FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
        comboNiveau.setVisible(true); comboNiveau.setManaged(true);
        labelNiveau.setVisible(true); labelNiveau.setManaged(true);
        errorNiveau.setVisible(true); errorNiveau.setManaged(true);
    }

    public void setEditingUser(User user) {
        this.editingUser = user;
        this.isEditMode  = (user != null);

        if (isEditMode) {
            formTitle.setText("Modifier étudiant");
            if (formSubtitle != null) formSubtitle.setText(user.getPrenom() + " " + user.getNom());
            fieldNom.setText(user.getNom());
            fieldPrenom.setText(user.getPrenom());
            fieldEmail.setText(user.getEmail());
            if (user instanceof Etudiant e) comboNiveau.setValue(e.getNiveau());
            if (labelPasswordHint != null)
                labelPasswordHint.setText("Laisser vide pour conserver le mot de passe actuel");
        } else {
            formTitle.setText("Nouvel étudiant");
            if (formSubtitle != null) formSubtitle.setText("Remplissez les informations ci-dessous");
        }
    }

    @FXML private void onSave() {
        if (!validateForm()) return;

        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText().trim();
        String niveau   = comboNiveau.getValue();

        if (!isEditMode) {
            // Admin always creates students
            String plainPassword = password; // keep plain copy before hashing for email
            User newUser = new Etudiant(nom, prenom, email, tn.esprit.tools.PasswordUtil.hash(password), comboNiveau.getValue());
            service.ajouter(newUser);
            // Notify the new student by email
            EmailService.sendAdminCreatedAccount(email, prenom, nom, plainPassword);
            // Log to Symfony ActivityAPI
            ActivityApiClient.logAsync(newUser.getId(), "user.created",
                java.util.Map.of("email", email, "niveau", niveau != null ? niveau : ""));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Étudiant créé avec succès. Un email lui a été envoyé.");
        } else {
            editingUser.setNom(nom);
            editingUser.setPrenom(prenom);
            editingUser.setEmail(email);
            if (!password.isEmpty()) editingUser.setPassword(tn.esprit.tools.PasswordUtil.hash(password));
            if (editingUser instanceof Etudiant e && niveau != null) e.setNiveau(niveau);
            service.modifier(editingUser);
            // Log to Symfony ActivityAPI
            ActivityApiClient.logAsync(editingUser.getId(), "user.updated",
                java.util.Map.of("email", email));
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur modifié avec succès.");
        }
        ((Stage) fieldNom.getScene().getWindow()).close();
    }

    @FXML private void onCancel() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────
    private boolean validateForm() {
        clearErrors();
        boolean valid = true;
        String nom      = fieldNom.getText().trim();
        String prenom   = fieldPrenom.getText().trim();
        String email    = fieldEmail.getText().trim();
        String password = fieldPassword.getText().trim();
        String niveau   = comboNiveau.getValue();

        if (nom.isEmpty()) { errorNom.setText("Le nom est obligatoire"); valid = false; }
        else if (nom.length() < 2 || nom.length() > 50) { errorNom.setText("Entre 2 et 50 caractères"); valid = false; }
        else if (!nom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorNom.setText("Lettres uniquement"); valid = false; }

        if (prenom.isEmpty()) { errorPrenom.setText("Le prénom est obligatoire"); valid = false; }
        else if (prenom.length() < 2 || prenom.length() > 50) { errorPrenom.setText("Entre 2 et 50 caractères"); valid = false; }
        else if (!prenom.matches("^[a-zA-ZÀ-ÿ\\s'-]+$")) { errorPrenom.setText("Lettres uniquement"); valid = false; }

        if (email.isEmpty()) { errorEmail.setText("L'email est obligatoire"); valid = false; }
        else if (!email.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            errorEmail.setText("Format invalide. Ex: nom@domaine.com"); valid = false;
        }

        if (!isEditMode) {
            if (password.isEmpty()) { errorPassword.setText("Le mot de passe est obligatoire"); valid = false; }
            else if (password.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
            else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
                errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
            }
        } else if (!password.isEmpty()) {
            if (password.length() < 6) { errorPassword.setText("Minimum 6 caractères"); valid = false; }
            else if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$")) {
                errorPassword.setText("Maj + min + chiffre + spécial requis"); valid = false;
            }
        }

        if (niveau == null || niveau.isEmpty()) {
            errorNiveau.setText("Le niveau est obligatoire"); valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        errorNom.setText(""); errorPrenom.setText(""); errorEmail.setText("");
        errorPassword.setText(""); errorRole.setText(""); errorNiveau.setText("");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
