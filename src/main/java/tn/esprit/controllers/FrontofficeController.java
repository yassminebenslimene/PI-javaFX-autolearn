package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import tn.esprit.MainApp;
import tn.esprit.entities.Etudiant;
import tn.esprit.session.SessionManager;

public class FrontofficeController {

    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelNiveauStat;
    @FXML private Label welcomeLabel;

    // Référence au BorderPane principal pour changer le contenu
    private BorderPane mainRoot;

    // Sauvegarde du contenu d'accueil original
    private javafx.scene.Node originalCenter;

    @FXML
    public void initialize() {
        if (SessionManager.getCurrentUser() == null) return;

        var u = SessionManager.getCurrentUser();
        String name = u.getPrenom() + " " + u.getNom();
        if (labelCurrentUser != null) labelCurrentUser.setText(name);
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Prêt à apprendre aujourd'hui ?");

        String initials = u.getPrenom().substring(0,1).toUpperCase()
                        + u.getNom().substring(0,1).toUpperCase();
        if (labelAvatarNav != null) labelAvatarNav.setText(initials);

        if (u instanceof Etudiant e && e.getNiveau() != null) {
            labelNiveauUser.setText("Niveau : " + e.getNiveau());
            if (labelNiveauStat != null) labelNiveauStat.setText(e.getNiveau());
        } else {
            labelNiveauUser.setText("");
            if (labelNiveauStat != null) labelNiveauStat.setText("—");
        }

        // Sauvegarder le center original (hero + stats + cards) après le rendu
        javafx.application.Platform.runLater(() -> {
            if (labelCurrentUser.getScene() != null) {
                BorderPane root = (BorderPane) labelCurrentUser.getScene().getRoot();
                originalCenter = root.getCenter();
            }
        });
    }

    // ── Navigation vers la liste des cours ───────────────────────────────────
    @FXML
    public void onHome() {
        // Restaurer le contenu d'accueil original (hero + stats + cards)
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        if (originalCenter != null) {
            root.setCenter(originalCenter);
        }
    }

    @FXML
    public void onCours() {
        naviguerVersCours();
    }

    private void naviguerVersCours() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/frontoffice/cours/index.fxml"));
            Parent view = loader.load();
            FrontCoursController ctrl = loader.getController();

            ctrl.setOnVoirChapitres(cours -> {
                try {
                    FXMLLoader chapLoader = new FXMLLoader(
                        getClass().getResource("/views/frontoffice/chapitre/index.fxml"));
                    Parent chapView = chapLoader.load();
                    FrontChapitreController chapCtrl = chapLoader.getController();

                    chapCtrl.setOnLireChapitre((c, chapitre) -> {
                        try {
                            FXMLLoader detailLoader = new FXMLLoader(
                                getClass().getResource("/views/frontoffice/chapitre/detail.fxml"));
                            Parent detailView = detailLoader.load();
                            FrontChapitreDetailController detailCtrl = detailLoader.getController();
                            detailCtrl.setChapitre(c, chapitre, () -> setCenter(chapView));
                            setCenter(detailView);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    chapCtrl.setCours(cours);
                    setCenter(chapView);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            ctrl.loadData();
            setCenter(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Remplace le contenu central du BorderPane ─────────────────────────────
    private void setCenter(Parent view) {
        // Chercher le BorderPane racine via le label (qui est dans le layout)
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        // Remplacer le ScrollPane central par la nouvelle vue
        ScrollPane sp = new ScrollPane(view);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");
        root.setCenter(sp);
    }

    @FXML
    private void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onProfile() {
        try {
            MainApp.showProfile();
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Erreur profil");
            alert.setHeaderText(e.getMessage());
            alert.showAndWait();
        }
    }
}
