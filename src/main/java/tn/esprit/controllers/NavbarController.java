package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

public class NavbarController {

    @FXML private Button btnAccueil;
    @FXML private Button btnCours;
    @FXML private Button btnEvenements;
    @FXML private Button btnCommunaute;
    @FXML private Button btnChallenges;
    @FXML private Button btnDashboard;
    @FXML private Button btnUsers;
    @FXML private Button btnProfile;
    @FXML private MenuButton menuUser;
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
        "-fx-background-color:transparent; -fx-text-fill:rgba(245,245,244,0.7);" +
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
        String initials = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
        if (labelCurrentUser   != null) labelCurrentUser.setText(name);
        if (labelAvatarNav     != null) labelAvatarNav.setText(initials);
        if (labelAvatarSidebar != null) labelAvatarSidebar.setText(initials);
        if (labelCurrentRole   != null) labelCurrentRole.setText(u.getRole());
        if (menuUser           != null) menuUser.setText(initials + " v");
        if (labelNiveauUser    != null && u instanceof Etudiant e && e.getNiveau() != null)
            labelNiveauUser.setText("Niveau : " + e.getNiveau());
    }

    public void setActive(String page) {
        Button[] fb = {btnAccueil, btnCours, btnEvenements, btnCommunaute, btnChallenges};
        String[] fn = {"Accueil", "Cours", "Evenements", "Communaute", "Challenges"};
        for (int i = 0; i < fb.length; i++)
            if (fb[i] != null) fb[i].setStyle(fn[i].equals(page) ? ACTIVE_FRONT : INACTIVE_FRONT);
        Button[] bb = {btnDashboard, btnUsers, btnCours, btnEvenements, btnChallenges, btnCommunaute, btnProfile};
        String[] bn = {"Dashboard", "Utilisateurs", "Cours", "Evenements", "Challenges", "Communaute", "Profil"};
        for (int i = 0; i < bb.length; i++)
            if (bb[i] != null) bb[i].setStyle(bn[i].equals(page) ? ACTIVE : INACTIVE);
    }

    @FXML private void onAccueil() {
        if (SessionManager.isAdmin()) navigate(() -> MainApp.showBackoffice());
        else navigate(() -> MainApp.showFrontoffice());
    }
    @FXML private void onCours() {
        if (SessionManager.isAdmin()) navigate(() -> MainApp.showBackofficeView("/views/backoffice/cours/index.fxml", "Cours"));
        else navigate(() -> MainApp.showFrontoffice());
    }
    @FXML private void onEvenements() {
        if (SessionManager.isAdmin())
            navigate(() -> MainApp.showBackofficeView("/views/backoffice/evenement/index.fxml", "Evenements"));
        else navigate(() -> MainApp.showEvenementsFront());
    }
    @FXML private void onCommunaute() {
        if (SessionManager.isAdmin())
            navigate(() -> MainApp.showBackofficeView("/views/backoffice/communaute/index.fxml", "Communauté"));
        else navigate(() -> MainApp.showCommunauteFront());
    }
    @FXML private void onChallenges() {
        if (SessionManager.isAdmin())
            navigate(() -> MainApp.showBackofficeView("/views/backoffice/challenge/challenges.fxml", "Challenges"));
        else navigate(() -> MainApp.showChallengesFront());
    }
    @FXML private void onDashboard() { navigate(() -> MainApp.showBackoffice()); }
    @FXML private void onUsers()     { navigate(() -> MainApp.showBackoffice()); }
    @FXML private void onProfile() {
        if (SessionManager.isAdmin()) navigate(() -> MainApp.showBackofficeProfile());
        else navigate(() -> MainApp.showProfile());
    }
    @FXML private void onMesParticipations() {
        navigate(() -> MainApp.showMesParticipations(null));
    }
    @FXML private void onMesEquipes() {
        navigate(() -> MainApp.showMesEquipes(null));
    }
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