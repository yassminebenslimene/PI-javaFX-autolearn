package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Cours;
import tn.esprit.entities.Etudiant;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.ServiceCours;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.util.List;

public class FrontofficeController {

    // Navbar labels
    @FXML private Label welcomeLabel;
    @FXML private Label labelNiveauStat;
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private Label labelCoursCount;
    @FXML private Label labelChallengesCount;
    @FXML private Label labelEtudiantsCount;

    // Container dynamique pour les cartes de cours
    @FXML private HBox coursCardsContainer;

    // Zone centrale du BorderPane
    @FXML private ScrollPane mainScrollPane;

    private final ServiceCours serviceCours = new ServiceCours();
    private final ChallengeService challengeService = new ChallengeService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;

        // Infos utilisateur dans la navbar
        String name = u.getPrenom() + " " + u.getNom();
        String initials = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
        if (labelCurrentUser != null) labelCurrentUser.setText(name);
        if (labelAvatarNav   != null) labelAvatarNav.setText(initials);
        if (welcomeLabel     != null) welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Prêt à apprendre aujourd'hui ?");
        if (u instanceof Etudiant e && e.getNiveau() != null)
            if (labelNiveauUser != null) labelNiveauUser.setText("Niveau : " + e.getNiveau());

        // Stats réelles depuis la DB
        javafx.application.Platform.runLater(() -> {
            try {
                int nbCours = serviceCours.consulter().size();
                int nbChallenges = challengeService.getAll().size();
                long nbEtudiants = userService.afficher().stream()
                    .filter(usr -> usr instanceof Etudiant).count();
                String niveau = (u instanceof Etudiant e && e.getNiveau() != null) ? e.getNiveau() : "—";

                if (labelNiveauStat      != null) labelNiveauStat.setText(niveau);
                if (labelCoursCount      != null) labelCoursCount.setText(String.valueOf(nbCours));
                if (labelChallengesCount != null) labelChallengesCount.setText(String.valueOf(nbChallenges));
                if (labelEtudiantsCount  != null) labelEtudiantsCount.setText(nbEtudiants + "+");

                // Charger les vraies cartes de cours
                if (coursCardsContainer != null) loadCoursCards();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void loadCoursCards() {
        coursCardsContainer.getChildren().clear();
        List<Cours> cours = serviceCours.consulter();
        String[] icons = {"📚", "🤖", "🏆", "💡", "🔬", "🎯"};
        String[] colors = {"rgba(122,106,216,0.1)", "rgba(251,191,36,0.12)", "rgba(52,211,153,0.12)",
                           "rgba(239,68,68,0.1)", "rgba(59,130,246,0.1)", "rgba(168,85,247,0.1)"};
        int max = Math.min(cours.size(), 3);
        for (int i = 0; i < max; i++) {
            Cours c = cours.get(i);
            coursCardsContainer.getChildren().add(buildCoursCard(c, icons[i % icons.length], colors[i % colors.length]));
        }
        // Si moins de 3 cours, compléter avec des placeholders vides
        if (cours.isEmpty()) {
            Label empty = new Label("Aucun cours disponible pour le moment.");
            empty.setStyle("-fx-text-fill:#888; -fx-font-size:14;");
            coursCardsContainer.getChildren().add(empty);
        }
    }

    private VBox buildCoursCard(Cours c, String icon, String iconBg) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size:28; -fx-background-color:" + iconBg +
                           "; -fx-background-radius:12; -fx-padding:10 12 10 12;");

        Label titre = new Label(c.getTitre());
        titre.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        titre.setWrapText(true);

        String niveauColor = switch (c.getNiveau() == null ? "" : c.getNiveau().toLowerCase()) {
            case "avancé", "avance" -> "#e94560";
            case "intermédiaire", "intermediaire" -> "#f59e0b";
            default -> "#059669";
        };
        Label niveauBadge = new Label(c.getNiveau() != null ? c.getNiveau() : "Débutant");
        niveauBadge.setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-text-fill:" + niveauColor +
                             "; -fx-background-color:" + niveauColor.replace(")", ",0.1)").replace("#", "rgba(") + ";" +
                             " -fx-background-radius:10; -fx-padding:2 8 2 8;");

        HBox header = new HBox(14, iconLabel, new VBox(3, titre, niveauBadge));
        header.setAlignment(Pos.CENTER_LEFT);

        String desc = c.getDescription() != null ? c.getDescription() : "Cours de " + c.getMatiere();
        if (desc.length() > 100) desc = desc.substring(0, 100) + "...";
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size:12; -fx-text-fill:#666; -fx-line-spacing:3;");

        Button btn = new Button("Voir le cours  →");
        btn.setStyle("-fx-background-color:linear-gradient(to right,#7a6ad8,#4e3b9c);" +
                     "-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700;" +
                     "-fx-padding:9 20 9 20; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btn.setOnAction(e -> naviguerVersCours());

        VBox card = new VBox(16, header, descLabel, btn);
        card.setPrefWidth(320);
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                      "-fx-border-color:#eeeeee; -fx-border-radius:16;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4); -fx-padding:28;");
        return card;
    }

    // ── Navigation — seul le center change, la navbar reste fixe ──────────────

    @FXML public void onHome() {
        // Revenir au contenu d'accueil = remettre le ScrollPane original
        if (mainScrollPane == null) return;
        var scene = mainScrollPane.getScene();
        if (scene == null) return;
        ((BorderPane) scene.getRoot()).setCenter(mainScrollPane);
    }

    @FXML public void onCours() { naviguerVersCours(); }

    @FXML public void onViewCourses() { naviguerVersCours(); }

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
                            detailCtrl.setOnQuizCallback(() -> {
                                if (chapCtrl.getOnPasserQuiz() != null)
                                    chapCtrl.getOnPasserQuiz().accept(chapitre);
                            });
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

    @FXML public void onEvenements() {
        try { MainApp.showEvenementsFront(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onCommunaute() {
        try { MainApp.showCommunauteFront(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onChallenges() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            setCenter(loader.load());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onProfile() {
        try { MainApp.showProfile(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setCenter(Parent view) {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        ScrollPane sp = new ScrollPane(view);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent; -fx-background:transparent; -fx-border-width:0;");
        root.setCenter(sp);
    }

    private void setCenterDirect(Parent view) {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        ((BorderPane) scene.getRoot()).setCenter(view);
    }
}
