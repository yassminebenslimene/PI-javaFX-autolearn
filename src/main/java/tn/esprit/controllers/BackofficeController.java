package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.MainApp;
import tn.esprit.session.SessionManager;

/**
 * Controller principal du backoffice (layout.fxml).
 * Gère la sidebar de navigation et charge les différentes vues dans la zone de contenu.
 * C'est le "chef d'orchestre" de l'interface backoffice.
 */
public class BackofficeController {

    // ── Zone de contenu principale (change selon la page sélectionnée) ────────
    @FXML private StackPane contentArea;

    // ── Labels de la sidebar (infos de l'utilisateur connecté) ───────────────
    @FXML private Label labelCurrentUser;    // nom complet de l'utilisateur
    @FXML private Label labelCurrentRole;    // rôle (Admin, Etudiant...)
    @FXML private Label labelAvatarSidebar;  // initiales de l'utilisateur (ex: "TY")
    @FXML private Label labelPageTitle;      // titre affiché dans la topbar

    // ── Boutons de navigation dans la sidebar ─────────────────────────────────
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnQuiz;
    @FXML private Button btnCours;  // bouton Gestion Cours
    @FXML private Button btnProfile;

    // Style d'un bouton actif (page actuellement affichée) — fond vert
    private static final String ACTIVE_STYLE =
        "-fx-background-color:rgba(5,150,105,0.2); -fx-text-fill:#34d399; -fx-font-weight:bold;" +
        "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";

    // Style d'un bouton inactif (page non sélectionnée) — transparent
    private static final String INACTIVE_STYLE =
        "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-weight:normal;" +
        "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";

    // ── Initialisation : appelée automatiquement au chargement du layout ──────
    @FXML
    public void initialize() {
        // Afficher les infos de l'utilisateur connecté dans la sidebar
        if (SessionManager.getCurrentUser() != null) {
            String name = SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom();
            labelCurrentUser.setText(name);
            labelCurrentRole.setText(SessionManager.getCurrentUser().getRole());
            // Créer les initiales (ex: "Tayssir Yousfi" → "TY")
            String initials = SessionManager.getCurrentUser().getPrenom().substring(0,1).toUpperCase()
                            + SessionManager.getCurrentUser().getNom().substring(0,1).toUpperCase();
            labelAvatarSidebar.setText(initials);
        }
        // Afficher la page Utilisateurs par défaut au démarrage
        navigateToUsers();
    }

    // ── Navigation vers le Dashboard ──────────────────────────────────────────
    @FXML public void navigateToDashboard() {
        setActive(btnDashboard);
        labelPageTitle.setText("Dashboard");
        loadView("/views/backoffice/user/index.fxml");
    }

    // ── Navigation vers la gestion des utilisateurs ───────────────────────────
    @FXML public void navigateToUsers() {
        setActive(btnUsers);
        labelPageTitle.setText("Gestion des Utilisateurs");
        loadView("/views/backoffice/user/index.fxml");
    }

    // ── Navigation vers la gestion des quiz ───────────────────────────────────
    @FXML public void navigateToQuiz() {
        setActive(btnQuiz);
        labelPageTitle.setText("Gestion des Quiz");
        loadView("/views/backoffice/quiz/index.fxml");
    }

    // ── Navigation vers la gestion des cours ──────────────────────────────────
    @FXML public void navigateToCours() {
        setActive(btnCours);
        labelPageTitle.setText("Gestion des Cours");
        loadView("/views/backoffice/cours/index.fxml");
    }

    // ── Navigation vers le profil ─────────────────────────────────────────────
    @FXML public void navigateToProfile() {
        setActive(btnProfile);
        labelPageTitle.setText("Mon Profil");
        loadView("/views/backoffice/profile.fxml");
    }

    // ── Déconnexion : vider la session et retourner à la page de login ────────
    @FXML
    public void onLogout() {
        SessionManager.logout(); // effacer l'utilisateur connecté
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Marquer un bouton comme actif et les autres comme inactifs ────────────
    private void setActive(Button active) {
        for (Button b : new Button[]{btnDashboard, btnUsers, btnQuiz, btnCours, btnProfile}) {
            if (b != null) b.setStyle(b == active ? ACTIVE_STYLE : INACTIVE_STYLE);
        }
    }

    // ── Charger une vue FXML dans la zone de contenu principale ──────────────
    // path = chemin vers le fichier FXML (ex: "/views/backoffice/quiz/index.fxml")
    public void loadView(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            contentArea.getChildren().clear();          // vider la zone de contenu
            contentArea.getChildren().add(loader.load()); // charger la nouvelle vue
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Impossible de charger : " + path + " — " + e.getMessage());
        }
    }
}
