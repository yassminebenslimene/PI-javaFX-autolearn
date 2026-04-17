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
import javafx.animation.*;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
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
    @FXML private MenuButton menuUser;
    @FXML private Label labelCoursCount;
    @FXML private Label labelChallengesCount;
    @FXML private Label labelEtudiantsCount;

    // Slider icons (set in Java to avoid encoding issues)
    @FXML private Label slide1Icon, slide2Icon, slide3Icon;
    @FXML private Label aboutIcon1, aboutIcon2, aboutIcon3, aboutIcon4;
    // Slider
    @FXML private HBox slide1, slide2, slide3;
    @FXML private Label dot1, dot2, dot3;
    @FXML private Label labelCoursIllus;

    // Contact
    @FXML private javafx.scene.control.TextField contactNom;
    @FXML private javafx.scene.control.TextField contactEmail;
    @FXML private javafx.scene.control.TextField contactSujet;
    @FXML private javafx.scene.control.TextArea   contactMessage;
    @FXML private Label contactStatus;

    // Container dynamique pour les cartes de cours
    @FXML private HBox coursCardsContainer;
    @FXML private ScrollPane mainScrollPane;

    private javafx.scene.Node homeCenter;
    private int currentSlide = 0;
    private Timeline sliderTimeline;

    private final ServiceCours serviceCours = new ServiceCours();
    private final ChallengeService challengeService = new ChallengeService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        var u = SessionManager.getCurrentUser();
        if (u == null) return;

        String name = u.getPrenom() + " " + u.getNom();
        String initials = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
        if (labelCurrentUser != null) labelCurrentUser.setText(name);
        if (labelAvatarNav   != null) labelAvatarNav.setText(initials);
        if (menuUser         != null) menuUser.setText(initials + " \u25be");
        if (welcomeLabel     != null) welcomeLabel.setText("Bienvenue, " + u.getPrenom() + " ! Pret a apprendre aujourd'hui !");
        if (u instanceof Etudiant e && e.getNiveau() != null)
            if (labelNiveauUser != null) labelNiveauUser.setText("Niveau : " + e.getNiveau());

        // Emojis dans les slides
        if (slide1Icon != null) slide1Icon.setText("\uD83D\uDCDA");
        if (slide2Icon != null) slide2Icon.setText("\uD83C\uDFC6");
        if (slide3Icon != null) slide3Icon.setText("\uD83D\uDC65");
        if (aboutIcon1 != null) aboutIcon1.setText("\uD83E\uDD16");
        if (aboutIcon2 != null) aboutIcon2.setText("\uD83D\uDCCA");
        if (aboutIcon3 != null) aboutIcon3.setText("\uD83C\uDFC6");
        if (aboutIcon4 != null) aboutIcon4.setText("\uD83D\uDC65");

        javafx.application.Platform.runLater(() -> {
            // Sauvegarder le center accueil
            if (mainScrollPane != null) homeCenter = mainScrollPane;
            else if (labelCurrentUser != null && labelCurrentUser.getScene() != null)
                homeCenter = ((BorderPane) labelCurrentUser.getScene().getRoot()).getCenter();

            try {
                int nbCours = serviceCours.consulter().size();
                int nbChallenges = challengeService.getAll().size();
                long nbEtudiants = userService.afficher().stream()
                    .filter(usr -> usr instanceof Etudiant).count();
                String niveau = (u instanceof Etudiant e && e.getNiveau() != null) ? e.getNiveau() : "—";

                if (labelNiveauStat      != null) labelNiveauStat.setText(niveau);
                if (labelCoursCount      != null) labelCoursCount.setText(String.valueOf(nbCours));
                if (labelCoursIllus     != null) labelCoursIllus.setText(nbCours + " cours disponibles");
                if (labelChallengesCount != null) labelChallengesCount.setText(String.valueOf(nbChallenges));
                if (labelEtudiantsCount  != null) labelEtudiantsCount.setText(nbEtudiants + "+");

                // Charger les vraies cartes de cours
                if (coursCardsContainer != null) loadCoursCards();

                // Démarrer le slider automatique
                startSlider();

                // Animation fade-in sur les cartes
                if (coursCardsContainer != null) {
                    javafx.application.Platform.runLater(() -> {
                        int i = 0;
                        for (javafx.scene.Node card : coursCardsContainer.getChildren()) {
                            card.setOpacity(0);
                            FadeTransition f = new FadeTransition(Duration.millis(500), card);
                            f.setFromValue(0); f.setToValue(1);
                            f.setDelay(Duration.millis(300 + i * 150));
                            f.play();
                            i++;
                        }
                    });
                }
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
        // Image du cours depuis les resources
        javafx.scene.image.ImageView imgView = null;
        try {
            String[] imgs = {"/images/course1.jpg", "/images/course2.jpg", "/images/course3.jpg",
                             "/images/course4.jpg", "/images/course5.jpg", "/images/course6.jpg"};
            int idx = Math.abs(c.getTitre().hashCode()) % imgs.length;
            var url = getClass().getResource(imgs[idx]);
            if (url != null) {
                imgView = new javafx.scene.image.ImageView(new javafx.scene.image.Image(url.toExternalForm()));
                imgView.setFitWidth(300); imgView.setFitHeight(140);
                imgView.setPreserveRatio(false);
            }
        } catch (Exception ignored) {}

        Label titre = new Label(c.getTitre());
        titre.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        titre.setWrapText(true);

        String niveauColor = switch (c.getNiveau() == null ? "" : c.getNiveau().toLowerCase()) {
            case "avancé", "avance" -> "#e94560";
            case "intermédiaire", "intermediaire" -> "#f59e0b";
            default -> "#059669";
        };
        Label niveauBadge = new Label(c.getNiveau() != null ? c.getNiveau() : "Debutant");
        niveauBadge.setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-text-fill:" + niveauColor +
                             "; -fx-background-color:" + niveauColor.replace(")", ",0.1)").replace("#", "rgba(") +
                             "; -fx-background-radius:10; -fx-padding:2 8 2 8;");

        Label matiereLabel = new Label(c.getMatiere() != null ? c.getMatiere() : "");
        matiereLabel.setStyle("-fx-font-size:11; -fx-text-fill:#999;");

        String desc = c.getDescription() != null ? c.getDescription() : "Cours de " + c.getMatiere();
        if (desc.length() > 90) desc = desc.substring(0, 90) + "...";
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size:12; -fx-text-fill:#666; -fx-line-spacing:3;");

        Button btn = new Button("Voir le cours  →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:linear-gradient(to right,#7a6ad8,#4e3b9c);" +
                     "-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700;" +
                     "-fx-padding:10 20 10 20; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btn.setOnAction(e -> naviguerVersCours());

        VBox content = new VBox(10, titre, niveauBadge, matiereLabel, descLabel, btn);
        content.setPadding(new Insets(16));

        VBox card = imgView != null
            ? new VBox(0, imgView, content)
            : new VBox(0, content);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                      "-fx-border-color:#eeeeee; -fx-border-radius:16;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4);");
        if (imgView != null) {
            // Clip image corners
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 140);
            clip.setArcWidth(16); clip.setArcHeight(16);
            imgView.setClip(clip);
        }
        return card;
    }

    // ── Navigation — seul le center change, la navbar reste fixe ──────────────

    @FXML public void onHome() {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        if (homeCenter != null) root.setCenter(homeCenter);
        else if (mainScrollPane != null) root.setCenter(mainScrollPane);
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/evenements.fxml"));
            BorderPane bp = loader.load();
            setCenter((Parent) bp.getCenter());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onCommunaute() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/communaute/index.fxml"));
            javafx.scene.Node root = loader.load();
            if (root instanceof BorderPane bp && bp.getCenter() != null)
                setCenter((Parent) bp.getCenter());
            else
                setCenter((Parent) root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onChallenges() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            BorderPane bp = loader.load();
            // Extraire seulement le center pour éviter la double navbar
            setCenter((Parent) bp.getCenter());
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onProfile() {
        try { MainApp.showProfile(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onMesParticipations() {
        try { MainApp.showMesParticipations(null); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onMesEquipes() {
        try { MainApp.showMesEquipes(null); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void onLogout() {
        SessionManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // ── Slider automatique ────────────────────────────────────────────────────

    @FXML private StackPane heroSlider;

    private void startSlider() {
        if (slide1 == null) return;
        // Appliquer l'image de fond initiale
        applySliderBackground(0);
        sliderTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            currentSlide = (currentSlide + 1) % 3;
            showSlide(currentSlide);
            applySliderBackground(currentSlide);
        }));
        sliderTimeline.setCycleCount(Timeline.INDEFINITE);
        sliderTimeline.play();
    }

    private void applySliderBackground(int index) {
        if (heroSlider == null) return;
        String[] bgImages = {"/images/banner1.jpg", "/images/banner2.jpg", "/images/banner3.jpg"};
        try {
            var url = getClass().getResource(bgImages[index]);
            if (url != null) {
                heroSlider.setStyle(
                    "-fx-background-image: url('" + url.toExternalForm() + "');" +
                    "-fx-background-size: cover; -fx-background-position: center;" +
                    "-fx-background-color: rgba(78,59,156,0.7);"
                );
            }
        } catch (Exception ignored) {
            heroSlider.setStyle("-fx-background-color:linear-gradient(to bottom right,#7a6ad8,#4e3b9c);");
        }
    }

    private void showSlide(int index) {
        HBox[] slides = {slide1, slide2, slide3};
        Label[] dots  = {dot1, dot2, dot3};
        for (int i = 0; i < slides.length; i++) {
            if (slides[i] == null) continue;
            boolean active = (i == index);
            if (active) {
                slides[i].setVisible(true);
                slides[i].setManaged(true);
                FadeTransition ft = new FadeTransition(Duration.millis(600), slides[i]);
                ft.setFromValue(0); ft.setToValue(1); ft.play();
            } else {
                slides[i].setVisible(false);
                slides[i].setManaged(false);
            }
            if (dots[i] != null)
                dots[i].setStyle("-fx-font-size:10; -fx-text-fill:" + (active ? "white" : "rgba(255,255,255,0.4)") + ";");
        }
    }

    // ── Contact ───────────────────────────────────────────────────────────────

    @FXML public void onContactSend() {
        if (contactNom == null) return;
        String nom  = contactNom.getText().trim();
        String mail = contactEmail.getText().trim();
        String msg  = contactMessage.getText().trim();
        if (nom.isEmpty() || mail.isEmpty() || msg.isEmpty()) {
            contactStatus.setText("Veuillez remplir tous les champs obligatoires.");
            contactStatus.setStyle("-fx-text-fill:#e74c3c; -fx-font-size:12;");
            contactStatus.setVisible(true); contactStatus.setManaged(true);
            return;
        }
        // Simuler l'envoi
        contactNom.clear(); contactEmail.clear();
        if (contactSujet != null) contactSujet.clear();
        contactMessage.clear();
        contactStatus.setText("Message envoyé avec succès ! Nous vous répondrons sous 24h.");
        contactStatus.setStyle("-fx-text-fill:#059669; -fx-font-size:12;");
        contactStatus.setVisible(true); contactStatus.setManaged(true);
    }

    private void setCenter(Parent view) {
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        // Si la vue est déjà un ScrollPane, la mettre directement
        if (view instanceof ScrollPane) {
            ((ScrollPane) view).setFitToWidth(true);
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
