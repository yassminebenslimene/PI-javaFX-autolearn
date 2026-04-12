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

import java.io.IOException;

public class FrontofficeController {

    @FXML private Label welcomeLabel;
    @FXML private Label labelNiveauStat;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private NavbarController navbarController;

    private javafx.scene.Node originalCenter;

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;

        if (labelCurrentUser != null) labelCurrentUser.setText(u.getPrenom() + " " + u.getNom());
        if (welcomeLabel != null) welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Pret a apprendre aujourd'hui ?");
        if (labelAvatarNav != null) {
            String initials = u.getPrenom().substring(0,1).toUpperCase()
                            + u.getNom().substring(0,1).toUpperCase();
            labelAvatarNav.setText(initials);
        }
        if (u instanceof Etudiant e && e.getNiveau() != null) {
            if (labelNiveauUser != null) labelNiveauUser.setText("Niveau : " + e.getNiveau());
            if (labelNiveauStat != null) labelNiveauStat.setText(e.getNiveau());
        }
        if (navbarController != null) navbarController.setActive("Accueil");

        javafx.application.Platform.runLater(() -> {
            if (labelCurrentUser != null && labelCurrentUser.getScene() != null) {
                BorderPane root = (BorderPane) labelCurrentUser.getScene().getRoot();
                originalCenter = root.getCenter();
            }
        });
    }

    @FXML
    public void onHome() {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        if (originalCenter != null) root.setCenter(originalCenter);
    }

    @FXML
    public void onCours() { naviguerVersCours(); }

    private void naviguerVersCours() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/cours/index.fxml"));
            Parent view = loader.load();
            FrontCoursController ctrl = loader.getController();
            ctrl.setOnVoirChapitres(cours -> {
                try {
                    FXMLLoader chapLoader = new FXMLLoader(getClass().getResource("/views/frontoffice/chapitre/index.fxml"));
                    Parent chapView = chapLoader.load();
                    FrontChapitreController chapCtrl = chapLoader.getController();
                    chapCtrl.setOnLireChapitre((c, chapitre) -> {
                        try {
                            FXMLLoader detailLoader = new FXMLLoader(getClass().getResource("/views/frontoffice/chapitre/detail.fxml"));
                            Parent detailView = detailLoader.load();
                            FrontChapitreDetailController detailCtrl = detailLoader.getController();
                            detailCtrl.setChapitre(c, chapitre, () -> setCenter(chapView));
                            setCenter(detailView);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                    chapCtrl.setOnPasserQuiz(chapitre -> {
                        try {
                            FXMLLoader quizLoader = new FXMLLoader(getClass().getResource("/views/frontoffice/quiz/intro.fxml"));
                            Parent quizView = quizLoader.load();
                            FrontQuizController quizCtrl = quizLoader.getController();
                            quizCtrl.setChapitre(chapitre, () -> setCenter(chapView));
                            setCenterDirect(quizView);
                            javafx.application.Platform.runLater(() -> quizCtrl.setSceneRef(labelCurrentUser));
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                    chapCtrl.setCours(cours);
                    setCenter(chapView);
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            ctrl.loadData();
            setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onEvenements() {
        try { MainApp.showEvenementsFront(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onChallenges() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void onViewCourses() { naviguerVersCours(); }

    @FXML
    public void onProfile() {
        try { MainApp.showProfile(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    private void setCenter(Parent view) {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        boolean isQuizView = view.getStyle() != null &&
            (view.getStyle().contains("6b21a8") || view.getStyle().contains("667eea") || view.getStyle().contains("4c1d95"));
        if (isQuizView) {
            root.setCenter(view);
        } else {
            ScrollPane sp = new ScrollPane(view);
            sp.setFitToWidth(true);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");
            root.setCenter(sp);
        }
    }

    private void setCenterDirect(Parent view) {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        ((BorderPane) scene.getRoot()).setCenter(view);
    }
}