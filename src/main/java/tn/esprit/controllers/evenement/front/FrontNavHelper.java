package tn.esprit.controllers.evenement.front;

import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

/**
 * Shared navbar initialization + navigation helpers for all front controllers.
 */
public class FrontNavHelper {

    public static void initNavbar(Label labelAvatarNav, Label labelCurrentUser, MenuButton menuUser) {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;
        String name = u.getPrenom() + " " + u.getNom();
        if (labelCurrentUser != null) labelCurrentUser.setText(name);
        String initials = u.getPrenom().substring(0, 1).toUpperCase()
                        + u.getNom().substring(0, 1).toUpperCase();
        if (labelAvatarNav != null) labelAvatarNav.setText(initials);
        if (menuUser != null) menuUser.setText(initials + " ▾");
    }

    public static void goHome() {
        try { MainApp.showFrontoffice(); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void goEvenements() {
        try { MainApp.showEvenementsFront(); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void goMesParticipations(String successMsg) {
        try { MainApp.showMesParticipations(successMsg); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void goMesEquipes(String successMsg) {
        try { MainApp.showMesEquipes(successMsg); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void goSelectEvent() {
        try { MainApp.showSelectEvent(); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void goProfile() {
        try { MainApp.showProfile(); } catch (Exception e) { e.printStackTrace(); }
    }

    public static void goLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }
}
