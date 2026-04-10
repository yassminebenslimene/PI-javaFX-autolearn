package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import tn.esprit.MainApp;
import tn.esprit.session.SessionManager;

import java.io.IOException;

public class BackofficeController {

    @FXML private StackPane        contentArea;
    @FXML private Label            labelPageTitle;
    @FXML private NavbarController sidebarController; // injected from fx:include fx:id="sidebar"

    @FXML
    public void initialize() {
        MainApp.setBackofficeController(this);
        navigateToUsers();
    }

    @FXML public void navigateToDashboard() {
        labelPageTitle.setText("Dashboard");
        if (sidebarController != null) sidebarController.setActive("Dashboard");
        loadView("/views/backoffice/user/index.fxml");
    }

    @FXML public void navigateToUsers() {
        labelPageTitle.setText("Gestion des Utilisateurs");
        if (sidebarController != null) sidebarController.setActive("Utilisateurs");
        loadView("/views/backoffice/user/index.fxml");
    }

    @FXML public void navigateToProfile() {
        labelPageTitle.setText("Mon Profil");
        if (sidebarController != null) sidebarController.setActive("Profil");
        loadView("/views/backoffice/profile.fxml");
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadView(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load: " + path + " — " + e.getMessage());
        }
    }
}
