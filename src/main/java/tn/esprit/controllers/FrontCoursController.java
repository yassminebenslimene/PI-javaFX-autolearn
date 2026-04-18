package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import tn.esprit.entities.Cours;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCours;
import tn.esprit.session.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * FrontCoursController — page catalogue des cours (frontoffice).
 * Affiche tous les cours en grille de cartes colorées avec recherche et filtres par niveau.
 */
public class FrontCoursController {

    @FXML private FlowPane cardsContainer;
    @FXML private Label    labelTotalCours;
    @FXML private Label    labelTotalChapitres;
    @FXML private Label    labelEmpty;
    @FXML private TextField searchField;
    @FXML private Button   btnAll, btnDebutant, btnInter, btnAvance;

    private final ServiceCours          serviceCours    = new ServiceCours();
    private final ServiceChapitre       serviceChapitre = new ServiceChapitre();
    private final CourseProgressService progressService = new CourseProgressService();

    private Consumer<Cours> onVoirChapitres;
    private List<Cours>     allCours;
    private Map<Integer, Integer> countByCours = new HashMap<>();

    // Palette de couleurs — chaque cours a une couleur différente
    private static final String[][] PALETTES = {
        {"#7a6ad8", "#f0eeff", "rgba(122,106,216,0.15)"},  // violet
        {"#059669", "#ecfdf5", "rgba(5,150,105,0.15)"},    // vert
        {"#e94560", "#fff1f3", "rgba(233,69,96,0.15)"},    // rouge
        {"#f59e0b", "#fffbeb", "rgba(245,158,11,0.15)"},   // orange
        {"#0ea5e9", "#f0f9ff", "rgba(14,165,233,0.15)"},   // bleu
        {"#8b5cf6", "#f5f3ff", "rgba(139,92,246,0.15)"},   // indigo
        {"#10b981", "#ecfdf5", "rgba(16,185,129,0.15)"},   // emeraude
        {"#f43f5e", "#fff1f2", "rgba(244,63,94,0.15)"},    // rose
    };

    private static final String[] ICONS = {"📚", "💻", "🔬", "🎯", "🤖", "🏆", "⚡", "🌐"};

    public void setOnVoirChapitres(Consumer<Cours> callback) {
        this.onVoirChapitres = callback;
    }

    public void loadData() {
        allCours = serviceCours.consulter();
        countByCours = new HashMap<>();
        serviceChapitre.consulter().forEach(ch -> countByCours.merge(ch.getCoursId(), 1, Integer::sum));

        if (labelTotalCours     != null) labelTotalCours.setText(String.valueOf(allCours.size()));
        if (labelTotalChapitres != null) labelTotalChapitres.setText(
            String.valueOf(countByCours.values().stream().mapToInt(Integer::intValue).sum()));

        afficher(allCours);
    }

    private void afficher(List<Cours> liste) {
        cardsContainer.getChildren().clear();
        if (liste.isEmpty()) {
            labelEmpty.setVisible(true); labelEmpty.setManaged(true); return;
        }
        labelEmpty.setVisible(false); labelEmpty.setManaged(false);

        for (int i = 0; i < liste.size(); i++) {
            VBox card = buildCard(liste.get(i), i);
            // Animation fade-in + slide-up décalée
            card.setOpacity(0);
            card.setTranslateY(40);
            cardsContainer.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(450), card);
            fade.setFromValue(0); fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(450), card);
            slide.setFromY(40); slide.setToY(0);

            ParallelTransition anim = new ParallelTransition(fade, slide);
            anim.setDelay(Duration.millis(70 * i));
            anim.play();
        }
    }

    private VBox buildCard(Cours cours, int index) {
        String[] palette = PALETTES[index % PALETTES.length];
        String accent  = palette[0]; // couleur principale
        String bgLight = palette[1]; // fond clair
        String accentA = palette[2]; // couleur avec alpha
        String icon    = ICONS[index % ICONS.length];
        int nbChap     = countByCours.getOrDefault(cours.getId(), 0);

        // ── Bandeau coloré en haut ──
        HBox banner = new HBox();
        banner.setPrefHeight(90);
        banner.setStyle("-fx-background-color:" + accentA + "; -fx-background-radius:16 16 0 0; -fx-padding:18 20 18 20;");
        banner.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:38; -fx-background-color:" + accent +
                         "; -fx-background-radius:14; -fx-padding:8 12 8 12;");

        VBox bannerInfo = new VBox(4);
        bannerInfo.setPadding(new Insets(0, 0, 0, 14));

        // Badge niveau
        String niveauColor = switch (cours.getNiveau() == null ? "" : cours.getNiveau().toUpperCase()) {
            case "AVANCE", "AVANCÉ" -> "#e94560";
            case "INTERMEDIAIRE", "INTERMÉDIAIRE" -> "#f59e0b";
            default -> "#059669";
        };
        Label niveauBadge = new Label(cours.getNiveau() != null ? cours.getNiveau() : "DEBUTANT");
        niveauBadge.setStyle("-fx-font-size:10; -fx-font-weight:700; -fx-text-fill:" + niveauColor +
                             "; -fx-background-color:white; -fx-background-radius:20; -fx-padding:3 10 3 10;");

        Label dureeLabel = new Label("⏱  " + cours.getDuree() + "h  •  📖  " + nbChap + " chapitres");
        dureeLabel.setStyle("-fx-font-size:11; -fx-text-fill:" + accent + "; -fx-font-weight:600;");

        bannerInfo.getChildren().addAll(niveauBadge, dureeLabel);
        banner.getChildren().addAll(iconLbl, bannerInfo);

        // ── Contenu ──
        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 18, 18, 18));

        Label titre = new Label(cours.getTitre());
        titre.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        titre.setWrapText(true);

        Label matiere = new Label("📂  " + (cours.getMatiere() != null ? cours.getMatiere() : ""));
        matiere.setStyle("-fx-font-size:11; -fx-text-fill:#999; -fx-font-weight:600;");

        String desc = cours.getDescription() != null ? cours.getDescription() : "";
        if (desc.length() > 85) desc = desc.substring(0, 85) + "...";
        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size:12; -fx-text-fill:#666; -fx-line-spacing:3;");

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#f0f0f0;");

        // Barre de progression
        int userId = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
        int progress = userId > 0 ? progressService.getCourseProgress(userId, cours.getId()) : 0;

        // Couleur selon progression : rouge < 50%, orange 50-79%, vert >= 80%
        String progressColor = progress >= 80 ? "#059669" : progress >= 50 ? "#f59e0b" : "#e94560";
        String progressBgColor = progress >= 80 ? "rgba(5,150,105,0.1)" : progress >= 50 ? "rgba(245,158,11,0.1)" : "rgba(233,69,96,0.1)";

        Label progressLabel = new Label("Progression : " + progress + "%");
        progressLabel.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:" + progressColor + ";");

        // Barre de progression
        Region progressFill = new Region();
        progressFill.setPrefHeight(8);
        double fillRatio = Math.max(0, Math.min(progress, 100)) / 100.0;
        progressFill.setPrefWidth(fillRatio * 252); // largeur max ~252px
        progressFill.setStyle("-fx-background-color:" + progressColor + "; -fx-background-radius:4;");

        HBox progressBarBox = new HBox(progressFill);
        progressBarBox.setPrefHeight(8);
        progressBarBox.setStyle("-fx-background-color:#f0f0f0; -fx-background-radius:4;");

        VBox progressBox = new VBox(5, progressLabel, progressBarBox);
        progressBox.setStyle("-fx-background-color:" + progressBgColor + "; -fx-background-radius:8; -fx-padding:8 10 8 10;");

        // Bouton
        Button btn = new Button("Voir les chapitres  →");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color:linear-gradient(to right," + accent + "," + accent + ");" +
                     "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;" +
                     "-fx-padding:11 0 11 0; -fx-background-radius:12; -fx-cursor:hand; -fx-border-width:0;" +
                     "-fx-effect:dropshadow(gaussian," + accentA + ",8,0,0,3);");
        btn.setOnAction(e -> { if (onVoirChapitres != null) onVoirChapitres.accept(cours); });

        // Hover effect
        btn.setOnMouseEntered(e -> btn.setOpacity(0.88));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));

        content.getChildren().addAll(titre, matiere, descLabel, sep, progressBox, btn);

        // ── Assembler la carte ──
        VBox card = new VBox(0, banner, content);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                      "-fx-border-color:#eeeeee; -fx-border-radius:16;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),14,0,0,4);");

        // Clip pour arrondir les coins du bandeau
        Rectangle clip = new Rectangle(300, 400);
        clip.setArcWidth(16); clip.setArcHeight(16);
        card.setClip(clip);

        // Hover animé sur la carte
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.03); st.setToY(1.03); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:" + accent + "; -fx-border-radius:16;"
                + "-fx-effect:dropshadow(gaussian," + accentA + ",22,0,0,8);");
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#eeeeee; -fx-border-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),14,0,0,4);");
        });

        return card;
    }

    // ── Recherche ──────────────────────────────────────────────────────────────
    @FXML private void onSearch() {
        String q = searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) { afficher(allCours); return; }
        afficher(allCours.stream().filter(c ->
            c.getTitre().toLowerCase().contains(q) ||
            (c.getMatiere() != null && c.getMatiere().toLowerCase().contains(q)) ||
            (c.getNiveau()  != null && c.getNiveau().toLowerCase().contains(q))
        ).toList());
    }

    @FXML private void onClearSearch() { searchField.clear(); afficher(allCours); }

    // ── Filtres par niveau ─────────────────────────────────────────────────────
    @FXML private void onFilterAll()      { setActiveFilter(btnAll);      afficher(allCours); }
    @FXML private void onFilterDebutant() { setActiveFilter(btnDebutant); filterByNiveau("DEBUTANT"); }
    @FXML private void onFilterInter()    { setActiveFilter(btnInter);    filterByNiveau("INTERMEDIAIRE"); }
    @FXML private void onFilterAvance()   { setActiveFilter(btnAvance);   filterByNiveau("AVANCE"); }

    private void filterByNiveau(String niveau) {
        afficher(allCours.stream().filter(c ->
            c.getNiveau() != null && c.getNiveau().toUpperCase().contains(niveau)
        ).toList());
    }

    private void setActiveFilter(Button active) {
        String activeStyle = "-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700; -fx-padding:6 18 6 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;";
        String inactiveBase = "-fx-background-color:white; -fx-font-size:12; -fx-font-weight:600; -fx-padding:6 18 6 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:1; -fx-border-radius:20;";
        if (btnAll      != null) btnAll.setStyle(btnAll      == active ? activeStyle : inactiveBase + "-fx-text-fill:#7a6ad8; -fx-border-color:#7a6ad8;");
        if (btnDebutant != null) btnDebutant.setStyle(btnDebutant == active ? activeStyle : inactiveBase + "-fx-text-fill:#059669; -fx-border-color:#059669;");
        if (btnInter    != null) btnInter.setStyle(btnInter    == active ? activeStyle : inactiveBase + "-fx-text-fill:#f59e0b; -fx-border-color:#f59e0b;");
        if (btnAvance   != null) btnAvance.setStyle(btnAvance   == active ? activeStyle : inactiveBase + "-fx-text-fill:#e94560; -fx-border-color:#e94560;");
    }
}
