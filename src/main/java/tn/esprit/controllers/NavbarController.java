package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

/**
 * Shared controller for navbar_front.fxml and sidebar_back.fxml.
 * Every module includes one of these components via <fx:include>.
 *
 * HOW TO USE in a module layout:
 *
 *   <BorderPane>
 *     <top>
 *       <fx:include source="../components/navbar_front.fxml" fx:id="navbar"/>
 *     </top>
 *     <center> ... your content ... </center>
 *   </BorderPane>
 *
 * Then in your module controller, call:
 *   navbarController.setActive("Événements");
 */
public class NavbarController {

    // ── Frontoffice nav buttons ──
    @FXML private Button btnAccueil;
    @FXML private Button btnCours;
    @FXML private Button btnEvenements;
    @FXML private Button btnCommunaute;
    @FXML private Button btnChallenges;

    // ── Backoffice sidebar buttons ──
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnProfile;

    // ── User info labels (both navbars) ──
    @FXML private Label labelAvatarNav;
    @FXML private Label labelAvatarSidebar;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelCurrentRole;
    @FXML private Label labelNiveauUser;

    private static final String ACTIVE =
        "-fx-background-color:rgba(255,255,255,0.18); -fx-text-fill:white; -fx-font-weight:700;" +
        "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";
    private static final String INACTIVE =
        "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-weight:normal;" +
        "-fx-alignment:CENTER_LEFT; -fx-padding:11 12 11 16; -fx-background-radius:10;" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-border-width:0;";
    private static final String ACTIVE_FRONT =
        "-fx-background-color:rgba(255,255,255,0.2); -fx-text-fill:white; -fx-font-weight:700;" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-padding:8 16 8 16; -fx-border-width:0; -fx-background-radius:8;";
    private static final String INACTIVE_FRONT =
        "-fx-background-color:transparent; -fx-text-fill:rgba(255,255,255,0.75);" +
        "-fx-font-size:13; -fx-cursor:hand; -fx-padding:8 16 8 16; -fx-border-width:0;";

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;

        String name = u.getPrenom() + " " + u.getNom();
        String initials = u.getPrenom().substring(0,1).toUpperCase()
                        + u.getNom().substring(0,1).toUpperCase();

        if (labelCurrentUser    != null) labelCurrentUser.setText(name);
        if (labelAvatarNav      != null) labelAvatarNav.setText(initials);
        if (labelAvatarSidebar  != null) labelAvatarSidebar.setText(initials);
        if (labelCurrentRole    != null) labelCurrentRole.setText(u.getRole());
        if (labelNiveauUser     != null) {
            if (u instanceof Etudiant e && e.getNiveau() != null)
                labelNiveauUser.setText("Niveau : " + e.getNiveau());
        }
    }

    /** Call this from your module controller to highlight the correct nav item. */
    public void setActive(String page) {
        // Frontoffice
        Button[] frontBtns = {btnAccueil, btnCours, btnEvenements, btnCommunaute, btnChallenges};
        String[] frontNames = {"Accueil", "Cours", "Événements", "Communauté", "Challenges"};
        for (int i = 0; i < frontBtns.length; i++) {
            if (frontBtns[i] != null)
                frontBtns[i].setStyle(frontNames[i].equals(page) ? ACTIVE_FRONT : INACTIVE_FRONT);
        }
        // Backoffice
        Button[] backBtns = {btnDashboard, btnUsers, btnCours, btnEvenements, btnChallenges, btnCommunaute, btnProfile};
        String[] backNames = {"Dashboard", "Utilisateurs", "Cours", "Événements", "Challenges", "Communauté", "Profil"};
        for (int i = 0; i < backBtns.length; i++) {
            if (backBtns[i] != null)
                backBtns[i].setStyle(backNames[i].equals(page) ? ACTIVE : INACTIVE);
        }
    }

    // ── Navigation handlers ──
    @FXML private void onAccueil()    { navigate(() -> MainApp.showFrontoffice()); }
    @FXML private void onCours()      { navigate(() -> MainApp.showFrontoffice()); } // replace with showCours()
    @FXML private void onEvenements() { navigate(() -> MainApp.showEvenements()); }
    @FXML private void onCommunaute() { navigate(() -> MainApp.showFrontoffice()); }
    @FXML private void onChallenges() { navigate(() -> MainApp.showFrontoffice()); }
    @FXML private void onDashboard()  { navigate(() -> MainApp.showBackoffice()); }
    @FXML private void onUsers()      { navigate(() -> MainApp.showBackoffice()); }
    @FXML private void onProfile()    { navigate(() -> MainApp.showProfile()); }
    @FXML private void onLogout() {
        SessionManager.logout();
        navigate(() -> MainApp.showLogin());
    }

    private void navigate(ThrowingRunnable r) {
        try { r.run(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FunctionalInterface
    interface ThrowingRunnable { void run() throws Exception; }
}
