package tn.esprit.controllers;

import javafx.fxml.FXML;

/**
 * Frontoffice home page controller.
 * Navbar is handled by NavbarController (via fx:include).
 *
 * Access the navbar controller with:
 *   @FXML private NavbarController navbarController;
 * Then call: navbarController.setActive("Accueil");
 */
public class FrontofficeController {

    @FXML private NavbarController navbarController; // injected from fx:include fx:id="navbar"

    @FXML
    public void initialize() {
        if (navbarController != null)
            navbarController.setActive("Accueil");
    }
}
