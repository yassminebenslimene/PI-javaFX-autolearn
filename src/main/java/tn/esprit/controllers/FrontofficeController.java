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
import javafx.scene.control.MenuButton;import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Cours;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ChallengeService;
import tn.esprit.services.EvenementService;
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
    @FXML private Button btnHome;
    @FXML private Button btnNavCours;
    @FXML private Button btnNavChallenges;
    @FXML private Button btnNavEvenements;
    @FXML private Button btnNavCommunaute;

    // Sections
    @FXML private VBox sectionCours;
    @FXML private VBox sectionFeatures;
    @FXML private HBox featureCardsContainer;
    @FXML private StackPane sectionEvenementsHome;
    @FXML private HBox evenementsHomeContainer;
    @FXML private HBox sectionChallenges;
    @FXML private HBox coursCardsContainer;

    private static final String NAV_ACTIVE =
        "-fx-background-color:rgba(255,255,255,0.2); -fx-text-fill:white; -fx-font-size:13;" +
        "-fx-font-weight:700; -fx-cursor:hand; -fx-padding:7 16 7 16; -fx-border-width:0; -fx-background-radius:8;";
    private static final String NAV_INACTIVE =
        "-fx-background-color:transparent; -fx-text-fill:rgba(255,255,255,0.85); -fx-font-size:13;" +
        "-fx-cursor:hand; -fx-padding:7 16 7 16; -fx-border-width:0;";

    private void setActiveNav(Button active) {
        for (Button b : new Button[]{btnHome, btnNavCours, btnNavChallenges, btnNavEvenements, btnNavCommunaute}) {
            if (b != null) b.setStyle(b == active ? NAV_ACTIVE : NAV_INACTIVE);
        }
    }

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

    // Container dynamique pour les cartes de challenges — carousel
    @FXML private StackPane challengeCarouselPane;
    @FXML private HBox challengeDots;
    private List<VBox> challengeCardsList = new java.util.ArrayList<>();
    private int currentChallengeIdx = 0;
    private Timeline challengeCarouselTimeline;
    @FXML private ScrollPane mainScrollPane;

    private javafx.scene.Node homeCenter;
    private int currentSlide = 0;
    private Timeline sliderTimeline;

    private final ServiceCours serviceCours = new ServiceCours();
    private final ChallengeService challengeService = new ChallengeService();
    private final EvenementService evenementService = new EvenementService();
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

                // Charger les cartes de challenges
                if (challengeCarouselPane != null) loadChallengeCarousel();

                // Charger les evenements
                if (evenementsHomeContainer != null) loadEvenementsHome();

                // Construire les feature cards
                if (featureCardsContainer != null) buildFeatureCards();

                // Démarrer le slider automatique
                startSlider();

                // Animations d'entree sur les sections
                animateSlideIn(sectionFeatures,       0);
                animateSlideIn(sectionCours,        150);
                animateSlideIn(sectionChallenges,   300);
                animateSlideIn(sectionEvenementsHome, 450);

            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void loadCoursCards() {
        coursCardsContainer.getChildren().clear();
        List<Cours> cours = serviceCours.consulter();
        String[] imgs = {"/images/course1.jpg", "/images/course2.jpg", "/images/course3.jpg",
                         "/images/course4.jpg", "/images/course5.jpg", "/images/course6.jpg"};
        int max = Math.min(cours.size(), 4);
        for (int i = 0; i < max; i++) {
            VBox card = buildCoursImageCard(cours.get(i), imgs[i % imgs.length]);
            card.setOpacity(0);
            coursCardsContainer.getChildren().add(card);
            final int idx = i;
            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(100 + idx * 120));
            ft.play();
        }
        if (cours.isEmpty()) {
            Label empty = new Label("Aucun cours disponible pour le moment.");
            empty.setStyle("-fx-text-fill:#aaa; -fx-font-size:13;");
            coursCardsContainer.getChildren().add(empty);
        }
    }

    /** Image card with colored gradient overlay — rich visual style */
    private VBox buildCoursImageCard(Cours c, String imgPath) {
        // Image with gradient overlay
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(150);
        imgPane.setMinHeight(150);

        String[] gradients = {
            "linear-gradient(to bottom,rgba(122,106,216,0.0) 30%,rgba(122,106,216,0.85) 100%)",
            "linear-gradient(to bottom,rgba(16,185,129,0.0) 30%,rgba(16,185,129,0.85) 100%)",
            "linear-gradient(to bottom,rgba(245,158,11,0.0) 30%,rgba(245,158,11,0.85) 100%)",
            "linear-gradient(to bottom,rgba(99,102,241,0.0) 30%,rgba(99,102,241,0.85) 100%)",
            "linear-gradient(to bottom,rgba(236,72,153,0.0) 30%,rgba(236,72,153,0.85) 100%)",
            "linear-gradient(to bottom,rgba(14,165,233,0.0) 30%,rgba(14,165,233,0.85) 100%)",
        };
        int gradIdx = Math.abs((c.getTitre() != null ? c.getTitre().hashCode() : 0)) % gradients.length;

        try {
            var url = getClass().getResource(imgPath);
            if (url != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(url.toExternalForm()));
                iv.setFitWidth(240); iv.setFitHeight(150);
                iv.setPreserveRatio(false);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(240, 150);
                clip.setArcWidth(12); clip.setArcHeight(12);
                iv.setClip(clip);
                imgPane.getChildren().add(iv);
            }
        } catch (Exception ignored) {}

        // Gradient overlay
        javafx.scene.shape.Rectangle grad = new javafx.scene.shape.Rectangle(240, 150);
        grad.setStyle("-fx-fill:" + gradients[gradIdx] + "; -fx-arc-width:12; -fx-arc-height:12;");
        imgPane.getChildren().add(grad);

        // Niveau badge on image
        String niveauColor = switch (c.getNiveau() == null ? "" : c.getNiveau().toLowerCase()) {
            case "avancé", "avance"               -> "#ef4444";
            case "intermédiaire", "intermediaire" -> "#f59e0b";
            default                               -> "#10b981";
        };
        Label niveauBadge = new Label(c.getNiveau() != null ? c.getNiveau() : "Debutant");
        niveauBadge.setStyle("-fx-font-size:10; -fx-font-weight:700; -fx-text-fill:white;" +
                             "-fx-background-color:" + niveauColor + ";" +
                             "-fx-background-radius:6; -fx-padding:2 8 2 8;");
        StackPane.setAlignment(niveauBadge, javafx.geometry.Pos.TOP_LEFT);
        niveauBadge.setTranslateX(10); niveauBadge.setTranslateY(10);
        imgPane.getChildren().add(niveauBadge);

        Label titre = new Label(c.getTitre());
        titre.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:#1a1a2e;");
        titre.setWrapText(true);
        titre.setMaxWidth(210);

        Label matiere = new Label(c.getMatiere() != null ? c.getMatiere() : "");
        matiere.setStyle("-fx-font-size:11; -fx-text-fill:#888;");

        Button btn = new Button("Voir le cours →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12;" +
                     "-fx-font-weight:700; -fx-padding:9 0 9 0; -fx-background-radius:8;" +
                     "-fx-cursor:hand; -fx-border-width:0;");
        btn.setOnAction(e -> naviguerVersCours());

        VBox info = new VBox(8, titre, matiere, btn);
        info.setPadding(new Insets(14));

        VBox card = new VBox(0, imgPane, info);
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                      "-fx-border-color:#eeeeee; -fx-border-radius:12;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                          "-fx-border-color:#7a6ad8; -fx-border-radius:12;" +
                          "-fx-effect:dropshadow(gaussian,rgba(122,106,216,0.22),14,0,0,5); -fx-cursor:hand;");
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                          "-fx-border-color:#eeeeee; -fx-border-radius:12;" +
                          "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");
        });
        card.setOnMouseClicked(e -> naviguerVersCours());
        return card;
    }

    // ── Feature Cards (OpenClassrooms illustrated style) ──────────────────────

    private void buildFeatureCards() {
        featureCardsContainer.getChildren().clear();
        String[][] features = {
            {"\uD83D\uDCDA", "Apprenez a votre rythme",
             "Acces illimite a tous les cours, chapitres et quiz. Progressez quand vous voulez, ou que vous soyez.",
             "#ede9ff", "#7a6ad8"},
            {"\uD83E\uDD16", "Correction par IA",
             "Soumettez vos exercices et obtenez un feedback instantane et personnalise grace a notre IA integree.",
             "#dcfce7", "#10b981"},
            {"\uD83C\uDFC6", "Challenges & Badges",
             "Relevez des defis stimulants, grimpez dans le classement et collectionnez des badges de competences.",
             "#fef3c7", "#f59e0b"},
        };
        for (int i = 0; i < features.length; i++) {
            VBox card = buildFeatureCard(features[i][0], features[i][1], features[i][2],
                                         features[i][3], features[i][4]);
            card.setOpacity(0);
            featureCardsContainer.getChildren().add(card);
            final int idx = i;
            FadeTransition ft = new FadeTransition(Duration.millis(600), card);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(idx * 150));
            TranslateTransition tt = new TranslateTransition(Duration.millis(600), card);
            tt.setFromY(20); tt.setToY(0);
            tt.setDelay(Duration.millis(idx * 150));
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            ft.play(); tt.play();
        }
    }

    private VBox buildFeatureCard(String emoji, String title, String desc, String bgColor, String accentColor) {
        // Large emoji in a colored circle (OpenClassrooms illustration style)
        StackPane iconCircle = new StackPane();
        iconCircle.setPrefSize(100, 100);
        iconCircle.setMaxSize(100, 100);
        javafx.scene.shape.Circle bg = new javafx.scene.shape.Circle(50);
        bg.setStyle("-fx-fill:" + bgColor + ";");
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:44;");
        iconCircle.getChildren().addAll(bg, emojiLbl);
        iconCircle.setAlignment(javafx.geometry.Pos.CENTER);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:#1a1a2e;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(240);
        titleLbl.setAlignment(javafx.geometry.Pos.CENTER);

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:13; -fx-text-fill:#666; -fx-line-spacing:3;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(240);
        descLbl.setAlignment(javafx.geometry.Pos.CENTER);

        VBox card = new VBox(16, iconCircle, titleLbl, descLbl);
        card.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setPadding(new Insets(32, 24, 32, 24));
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                      "-fx-border-color:#f0f0f0; -fx-border-radius:16;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),12,0,0,4);");
        return card;
    }

    // ── Challenge Carousel ────────────────────────────────────────────────────

    private void loadChallengeCarousel() {
        challengeCardsList.clear();
        if (challengeCarouselPane != null) challengeCarouselPane.getChildren().clear();
        if (challengeDots != null) challengeDots.getChildren().clear();

        List<Challenge> challenges = challengeService.getAll();
        if (challenges.isEmpty()) {
            Label empty = new Label("Aucun challenge disponible.");
            empty.setStyle("-fx-text-fill:rgba(255,255,255,0.6); -fx-font-size:14;");
            if (challengeCarouselPane != null) challengeCarouselPane.getChildren().add(empty);
            return;
        }

        // Color palette per card
        String[][] palette = {
            {"#7a6ad8", "#ede9ff", "/images/course1.jpg"},
            {"#10b981", "#dcfce7", "/images/course2.jpg"},
            {"#f59e0b", "#fef3c7", "/images/course3.jpg"},
            {"#6366f1", "#e0e7ff", "/images/course4.jpg"},
            {"#ec4899", "#fce7f3", "/images/course5.jpg"},
            {"#0ea5e9", "#e0f2fe", "/images/course6.jpg"},
        };
        String[] icons = {"\uD83C\uDFC6", "\uD83D\uDD25", "\u26A1", "\uD83E\uDDE0", "\uD83C\uDFAF", "\uD83D\uDE80"};

        int max = Math.min(challenges.size(), 6);
        for (int i = 0; i < max; i++) {
            String[] p = palette[i % palette.length];
            VBox card = buildChallengeCard(challenges.get(i), icons[i % icons.length], p[0], p[1], p[2]);
            card.setOpacity(i == 0 ? 1 : 0);
            card.setTranslateX(i == 0 ? 0 : 40);
            challengeCardsList.add(card);
            if (challengeCarouselPane != null) challengeCarouselPane.getChildren().add(card);
        }

        // Dot indicators
        if (challengeDots != null) {
            for (int i = 0; i < max; i++) {
                Label dot = new Label("●");
                dot.setStyle("-fx-font-size:8; -fx-text-fill:" + (i == 0 ? "white" : "rgba(255,255,255,0.3)") + ";");
                challengeDots.getChildren().add(dot);
            }
        }

        // Hover on carousel pane → advance to next card
        if (challengeCarouselPane != null) {
            challengeCarouselPane.setOnMouseEntered(e -> advanceChallengeCard());
        }

        // Auto-rotate every 3.5 seconds
        challengeCarouselTimeline = new Timeline(
            new KeyFrame(Duration.seconds(3.5), e -> advanceChallengeCard())
        );
        challengeCarouselTimeline.setCycleCount(Timeline.INDEFINITE);
        challengeCarouselTimeline.play();
    }

    private void advanceChallengeCard() {
        if (challengeCardsList.isEmpty()) return;
        int prev = currentChallengeIdx;
        currentChallengeIdx = (currentChallengeIdx + 1) % challengeCardsList.size();

        VBox outCard = challengeCardsList.get(prev);
        VBox inCard  = challengeCardsList.get(currentChallengeIdx);

        // Slide out current
        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), outCard);
        fadeOut.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(Duration.millis(350), outCard);
        slideOut.setToX(-40);
        slideOut.setInterpolator(javafx.animation.Interpolator.EASE_IN);

        // Slide in next (from right)
        inCard.setTranslateX(40);
        inCard.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), inCard);
        fadeIn.setToValue(1);
        fadeIn.setDelay(Duration.millis(200));
        TranslateTransition slideIn = new TranslateTransition(Duration.millis(400), inCard);
        slideIn.setFromX(40); slideIn.setToX(0);
        slideIn.setDelay(Duration.millis(200));
        slideIn.setInterpolator(javafx.animation.Interpolator.EASE_OUT);

        fadeOut.play(); slideOut.play();
        fadeIn.play();  slideIn.play();

        // Update dots
        if (challengeDots != null) {
            for (int i = 0; i < challengeDots.getChildren().size(); i++) {
                challengeDots.getChildren().get(i).setStyle(
                    "-fx-font-size:8; -fx-text-fill:" +
                    (i == currentChallengeIdx ? "white" : "rgba(255,255,255,0.3)") + ";");
            }
        }
    }

    private VBox buildChallengeCard(Challenge c, String icon, String accent, String lightBg, String imgPath) {
        // Background image with colored overlay
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(120);
        imgPane.setMinHeight(120);
        imgPane.setMaxHeight(120);

        // Try to load image
        try {
            var url = getClass().getResource(imgPath);
            if (url != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(url.toExternalForm()));
                iv.setFitWidth(380); iv.setFitHeight(120);
                iv.setPreserveRatio(false);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(380, 120);
                clip.setArcWidth(12); clip.setArcHeight(12);
                iv.setClip(clip);
                imgPane.getChildren().add(iv);
            }
        } catch (Exception ignored) {}

        // Colored overlay on image
        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(380, 120);
        overlay.setStyle("-fx-fill:" + accent + "; -fx-opacity:0.72; -fx-arc-width:12; -fx-arc-height:12;");
        imgPane.getChildren().add(overlay);

        // Icon + title on image
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:36;");
        Label titleOnImg = new Label(c.getTitre());
        titleOnImg.setStyle("-fx-font-size:16; -fx-font-weight:900; -fx-text-fill:white;");
        titleOnImg.setWrapText(true);
        titleOnImg.setMaxWidth(300);
        VBox imgContent = new VBox(6, iconLbl, titleOnImg);
        imgContent.setPadding(new Insets(16));
        imgContent.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        imgPane.getChildren().add(imgContent);
        StackPane.setAlignment(imgContent, javafx.geometry.Pos.BOTTOM_LEFT);

        // Niveau badge
        String niveauColor = switch (c.getNiveau() == null ? "" : c.getNiveau().toLowerCase()) {
            case "avancé", "avance"               -> "#ef4444";
            case "intermédiaire", "intermediaire" -> "#f59e0b";
            default                               -> "#10b981";
        };
        Label niveauBadge = new Label(c.getNiveau() != null ? c.getNiveau() : "Debutant");
        niveauBadge.setStyle("-fx-font-size:10; -fx-font-weight:700; -fx-text-fill:" + niveauColor + ";" +
                             "-fx-background-color:" + niveauColor + "18;" +
                             "-fx-background-radius:6; -fx-padding:2 8 2 8;");

        String desc = c.getDescription() != null ? c.getDescription() : "Relevez ce challenge et progressez.";
        if (desc.length() > 80) desc = desc.substring(0, 80) + "...";
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size:12; -fx-text-fill:#555; -fx-line-spacing:2;");

        HBox meta = new HBox(14);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (c.getDuree() > 0) {
            Label dur = new Label("\u23F1 " + c.getDuree() + " min");
            dur.setStyle("-fx-font-size:11; -fx-text-fill:#aaa;");
            meta.getChildren().add(dur);
        }
        if (c.getDateFin() != null) {
            Label fin = new Label("\uD83D\uDCC5 " + c.getDateFin().toString());
            fin.setStyle("-fx-font-size:11; -fx-text-fill:#aaa;");
            meta.getChildren().add(fin);
        }

        Button btn = new Button("Relever le challenge →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:" + accent + "; -fx-text-fill:white;" +
                     "-fx-font-size:12; -fx-font-weight:700;" +
                     "-fx-padding:10 0 10 0; -fx-background-radius:8;" +
                     "-fx-cursor:hand; -fx-border-width:0;");
        btn.setOnAction(e -> onChallenges());

        VBox info = new VBox(10, niveauBadge, descLabel, meta, btn);
        info.setPadding(new Insets(16));

        VBox card = new VBox(0, imgPane, info);
        card.setPrefWidth(380);
        card.setMaxWidth(380);
        card.setStyle("-fx-background-color:white; -fx-background-radius:14;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),16,0,0,6);");

        // Hover scale effect
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.03); st.setToY(1.03);
            st.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
            st.setToX(1.0); st.setToY(1.0);
            st.play();
        });
        card.setOnMouseClicked(e -> onChallenges());
        return card;
    }

    // ── Auth guard ────────────────────────────────────────────────────────────

    /** Returns true if user is logged in, otherwise redirects to login and returns false */
    private boolean requireLogin() {
        if (SessionManager.isLoggedIn()) return true;
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    // ── Navigation — seul le center change, la navbar reste fixe ──────────────
    @FXML public void onHome() {
        setActiveNav(btnHome);
        if (labelCurrentUser == null) return;
        var scene = labelCurrentUser.getScene();
        if (scene == null) return;
        BorderPane root = (BorderPane) scene.getRoot();
        if (homeCenter != null) root.setCenter(homeCenter);
        else if (mainScrollPane != null) root.setCenter(mainScrollPane);
    }

    @FXML public void onCours() { setActiveNav(btnNavCours); naviguerVersCours(); }

    @FXML public void onViewCourses() { naviguerVersCours(); }

    private void naviguerVersCours() {
        if (!requireLogin()) return;
        // Track student action
        var u = SessionManager.getCurrentUser();
        if (u != null) ActivityApiClient.logAsync(u.getId(), "user.view_cours",
            java.util.Map.of("email", u.getEmail()));
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
        if (!requireLogin()) return;
        setActiveNav(btnNavEvenements);
        // Track student action
        var u = SessionManager.getCurrentUser();
        if (u != null) ActivityApiClient.logAsync(u.getId(), "user.view_evenements",
            java.util.Map.of("email", u.getEmail()));
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/evenements.fxml"));
            Parent root = loader.load();
            if (root instanceof BorderPane bp && bp.getCenter() != null)
                setCenter((Parent) bp.getCenter());
            else
                setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback via MainApp
            try { MainApp.showEvenementsFront(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @FXML public void onCommunaute() {
        if (!requireLogin()) return;
        setActiveNav(btnNavCommunaute);
        var u = SessionManager.getCurrentUser();
        if (u != null) ActivityApiClient.logAsync(u.getId(), "user.view_communaute",
            java.util.Map.of("email", u.getEmail()));
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/communaute/index.fxml"));
            Parent root = loader.load();
            if (root instanceof BorderPane bp && bp.getCenter() != null)
                setCenter((Parent) bp.getCenter());
            else
                setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
            try { MainApp.showCommunauteFront(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @FXML public void onChallenges() {
        if (!requireLogin()) return;
        setActiveNav(btnNavChallenges);
        var u = SessionManager.getCurrentUser();
        if (u != null) ActivityApiClient.logAsync(u.getId(), "user.view_challenges",
            java.util.Map.of("email", u.getEmail()));
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            Parent root = loader.load();
            if (root instanceof BorderPane bp && bp.getCenter() != null)
                setCenter((Parent) bp.getCenter());
            else
                setCenter(root);
        } catch (Exception e) {
            e.printStackTrace();
            try { MainApp.showChallengesFront(); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @FXML public void onProfile() {
        try {
            var u = SessionManager.getCurrentUser();
            if (u != null) ActivityApiClient.logAsync(u.getId(), "user.view_profile",
                java.util.Map.of("email", u.getEmail()));
            MainApp.showProfile();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur chargement profil: " + e.getMessage());
        }
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

    // ── Animation helper ──────────────────────────────────────────────────────

    private void animateSlideIn(javafx.scene.Node node, double delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(28);
        FadeTransition ft = new FadeTransition(Duration.millis(550), node);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        TranslateTransition tt = new TranslateTransition(Duration.millis(550), node);
        tt.setFromY(28); tt.setToY(0);
        tt.setDelay(Duration.millis(delayMs));
        tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        ft.play(); tt.play();
    }

    // ── Evenements Sidebar ────────────────────────────────────────────────────

    private void loadEvenementsHome() {
        evenementsHomeContainer.getChildren().clear();

        if (sectionEvenementsHome != null) {
            addFloatingCircles(sectionEvenementsHome);
        }

        List<Evenement> all = evenementService.getAll();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        List<Evenement> upcoming = all.stream()
            .filter(e -> !e.isIsCanceled() && e.getDateFin() != null && e.getDateFin().isAfter(now))
            .sorted((a, b) -> a.getDateDebut().compareTo(b.getDateDebut()))
            .limit(4)
            .collect(java.util.stream.Collectors.toList());

        if (upcoming.isEmpty()) {
            upcoming = all.stream().limit(4).collect(java.util.stream.Collectors.toList());
        }

        for (int i = 0; i < upcoming.size(); i++) {
            VBox card = buildEvenementCard(upcoming.get(i));
            card.setOpacity(0);
            evenementsHomeContainer.getChildren().add(card);
            final int idx = i;
            FadeTransition ft = new FadeTransition(Duration.millis(500), card);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(100 + idx * 120));
            ft.play();
        }

        if (upcoming.isEmpty()) {
            Label empty = new Label("Aucun evenement a venir.");
            empty.setStyle("-fx-text-fill:#888; -fx-font-size:13;");
            evenementsHomeContainer.getChildren().add(empty);
        }
    }

    /** Rich colored event card with image background + overlay */
    private VBox buildEvenementCard(Evenement e) {
        String type = e.getType() != null ? e.getType() : "Evenement";

        // Per-type color scheme
        String[] colors = switch (type.toLowerCase()) {
            case "hackathon"   -> new String[]{"#7c3aed", "#a78bfa", "/images/event1.jpg"};
            case "workshop"    -> new String[]{"#059669", "#34d399", "/images/event2.jpg"};
            case "competition" -> new String[]{"#d97706", "#fbbf24", "/images/event3.jpg"};
            default            -> new String[]{"#2563eb", "#60a5fa", "/images/event1.jpg"};
        };
        String darkColor  = colors[0];
        String lightColor = colors[1];
        String imgPath    = colors[2];

        // Image pane with colored overlay
        StackPane imgPane = new StackPane();
        imgPane.setPrefHeight(110);
        imgPane.setMinHeight(110);

        try {
            var url = getClass().getResource(imgPath);
            if (url != null) {
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(url.toExternalForm()));
                iv.setFitWidth(260); iv.setFitHeight(110);
                iv.setPreserveRatio(false);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(260, 110);
                clip.setArcWidth(14); clip.setArcHeight(14);
                iv.setClip(clip);
                imgPane.getChildren().add(iv);
            }
        } catch (Exception ignored) {}

        // Strong colored overlay
        javafx.scene.shape.Rectangle overlay = new javafx.scene.shape.Rectangle(260, 110);
        overlay.setStyle("-fx-fill:" + darkColor + "; -fx-opacity:0.78; -fx-arc-width:14; -fx-arc-height:14;");
        imgPane.getChildren().add(overlay);

        // Type badge + date on image
        String dateStr = e.getDateDebut() != null
            ? e.getDateDebut().getDayOfMonth() + " " +
              e.getDateDebut().getMonth().getDisplayName(
                  java.time.format.TextStyle.SHORT, java.util.Locale.FRENCH)
            : "";

        Label typeBadge = new Label(type);
        typeBadge.setStyle("-fx-font-size:10; -fx-font-weight:700; -fx-text-fill:" + darkColor + ";" +
                           "-fx-background-color:white; -fx-background-radius:6; -fx-padding:2 8 2 8;");

        Label dateLbl = new Label(dateStr);
        dateLbl.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:rgba(255,255,255,0.85);");

        HBox topBadge = new HBox(8, typeBadge, dateLbl);
        topBadge.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        topBadge.setPadding(new Insets(10, 12, 0, 12));
        StackPane.setAlignment(topBadge, javafx.geometry.Pos.TOP_LEFT);
        imgPane.getChildren().add(topBadge);

        // Title on image bottom
        Label titleOnImg = new Label(e.getTitre());
        titleOnImg.setStyle("-fx-font-size:14; -fx-font-weight:900; -fx-text-fill:white;");
        titleOnImg.setWrapText(true);
        titleOnImg.setMaxWidth(230);
        VBox titleBox = new VBox(titleOnImg);
        titleBox.setPadding(new Insets(0, 12, 10, 12));
        titleBox.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        StackPane.setAlignment(titleBox, javafx.geometry.Pos.BOTTOM_LEFT);
        imgPane.getChildren().add(titleBox);

        // Info section below image
        String lieu = e.getLieu() != null ? "\uD83D\uDCCD  " + e.getLieu() : "";
        Label lieuLbl = new Label(lieu);
        lieuLbl.setStyle("-fx-font-size:11; -fx-text-fill:#666;");

        Button btn = new Button("S'inscrire →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:" + darkColor + "; -fx-text-fill:white;" +
                     "-fx-font-size:12; -fx-font-weight:700; -fx-padding:9 0 9 0;" +
                     "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btn.setOnAction(ev -> onEvenements());

        VBox info = new VBox(10, lieuLbl, btn);
        info.setPadding(new Insets(14, 14, 14, 14));

        VBox card = new VBox(0, imgPane, info);
        card.setPrefWidth(260);
        card.setMaxWidth(260);
        card.setStyle("-fx-background-color:white; -fx-background-radius:14;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),16,0,0,6);");

        // Hover: lift + colored border
        card.setOnMouseEntered(ev -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.04); st.setToY(1.04); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:14;" +
                          "-fx-border-color:" + lightColor + "; -fx-border-radius:14;" +
                          "-fx-effect:dropshadow(gaussian," + darkColor + "44,20,0,0,8); -fx-cursor:hand;");
        });
        card.setOnMouseExited(ev -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:14;" +
                          "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.22),16,0,0,6);");
        });
        card.setOnMouseClicked(ev -> onEvenements());
        return card;
    }

    /** Adds slow-pulsing translucent circles to a StackPane for a subtle animated background */
    private void addFloatingCircles(StackPane pane) {
        double[][] circles = {
            {90,  80, -60, 0.10},
            {60, -70,  80, 0.08},
            {45,  30,  90, 0.06},
            {70, -40, -70, 0.07},
        };
        for (double[] cfg : circles) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(cfg[0]);
            c.setStyle("-fx-fill:#6366f1;");  // indigo
            c.setTranslateX(cfg[1]);
            c.setTranslateY(cfg[2]);
            c.setOpacity(cfg[3]);
            c.setMouseTransparent(true);
            pane.getChildren().add(0, c);

            ScaleTransition st = new ScaleTransition(Duration.seconds(3 + cfg[0] / 30), c);
            st.setFromX(1.0); st.setToX(1.15);
            st.setFromY(1.0); st.setToY(1.15);
            st.setAutoReverse(true);
            st.setCycleCount(Timeline.INDEFINITE);
            st.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            st.setDelay(Duration.seconds(cfg[1] > 0 ? 0.5 : 1.2));
            st.play();
        }
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
        BorderPane root = (BorderPane) scene.getRoot();
        // Forcer la vue à prendre toute la hauteur du center
        if (view instanceof javafx.scene.layout.Region region) {
            region.prefHeightProperty().unbind();
            region.prefWidthProperty().unbind();
            region.setMaxHeight(Double.MAX_VALUE);
            region.setMaxWidth(Double.MAX_VALUE);
        }
        root.setCenter(view);
    }
}
