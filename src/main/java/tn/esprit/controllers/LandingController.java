package tn.esprit.controllers;

import javafx.fxml.FXML;
import tn.esprit.MainApp;

public class LandingController {

    @FXML
    public void onLogin() {
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onRegister() {
        try { MainApp.showRegister(); } catch (Exception e) { e.printStackTrace(); }
    }
}
