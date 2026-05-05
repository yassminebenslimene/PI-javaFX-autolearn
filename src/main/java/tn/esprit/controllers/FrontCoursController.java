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
import tn.esprit.entities.Communaute;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.LearningObjectiveService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCommunaute;
import tn.esprit.services.ServiceCours;
import tn.esprit.session.JwtManager;

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
    @FXML private HBox     banniereObjectif;
    @FXML private Label    labelObjectifActif;

    private final ServiceCours          serviceCours    = new ServiceCours();
    private final ServiceChapitre       serviceChapitre = new ServiceChapitre();
    private final ServiceCommunaute     serviceCommunaute = new ServiceCommunaute();
    private final CourseProgressService progressService = new CourseProgressService();
    private final LearningObjectiveService objectiveService = new LearningObjectiveService();

    private Consumer<Cours> onVoirChapitres;
    private Consumer<Communaute> onOuvrirCommunaute;
    private Runnable onNavigateGitHub;
    private Runnable onNavigateMaListe;
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
    
    public void setOnOuvrirCommunaute(Consumer<Communaute> callback) {
        this.onOuvrirCommunaute = callback;
    }
    
    public void setOnNavigateGitHub(Runnable callback) {
        this.onNavigateGitHub = callback;
    }
    
    public void setOnNavigateMaListe(Runnable callback) {
        this.onNavigateMaListe = callback;
    }

    public void loadData() {
        System.out.println("[FrontCoursController] loadData() appelé");
        try {
            allCours = serviceCours.consulter();
            System.out.println("[FrontCoursController] Nombre de cours récupérés: " + (allCours != null ? allCours.size() : "null"));
            
            if (allCours == null) {
                allCours = new java.util.ArrayList<>();
            }
            
            countByCours = new HashMap<>();
            serviceChapitre.consulter().forEach(ch -> countByCours.merge(ch.getCoursId(), 1, Integer::sum));

            if (labelTotalCours     != null) labelTotalCours.setText(String.valueOf(allCours.size()));
            if (labelTotalChapitres != null) labelTotalChapitres.setText(
                String.valueOf(countByCours.values().stream().mapToInt(Integer::intValue).sum()));

            System.out.println("[FrontCoursController] Affichage de " + allCours.size() + " cours");
            afficher(allCours);
        } catch (Exception e) {
            System.err.println("[FrontCoursController] ERREUR dans loadData(): " + e.getMessage());
            e.printStackTrace();
            allCours = new java.util.ArrayList<>();
            afficher(allCours);
        }
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
        int userId = JwtManager.getCurrentUser() != null ? JwtManager.getCurrentUser().getId() : 0;
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

        // Bouton Voir les chapitres
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

        // Bouton Communauté
        Button btnComm = new Button("💬 Communauté");
        btnComm.setMaxWidth(Double.MAX_VALUE);
        btnComm.setStyle("-fx-background-color:white; -fx-text-fill:" + accent + "; -fx-font-size:12; -fx-font-weight:700;" +
                         "-fx-padding:9 0 9 0; -fx-background-radius:12; -fx-cursor:hand;" +
                         "-fx-border-color:" + accent + "; -fx-border-width:1.5; -fx-border-radius:12;");
        btnComm.setOnAction(e -> {
            if (onOuvrirCommunaute != null) {
                // Passer un objet Communaute factice avec juste le coursId et le titre
                Communaute placeholder = new Communaute();
                placeholder.setCoursId(cours.getId());
                placeholder.setNom(cours.getTitre()); // utilisé pour la recherche par nom
                onOuvrirCommunaute.accept(placeholder);
            }
        });
        btnComm.setOnMouseEntered(e -> btnComm.setStyle("-fx-background-color:" + accentA + "; -fx-text-fill:" + accent + "; -fx-font-size:12; -fx-font-weight:700;" +
                                                         "-fx-padding:9 0 9 0; -fx-background-radius:12; -fx-cursor:hand;" +
                                                         "-fx-border-color:" + accent + "; -fx-border-width:1.5; -fx-border-radius:12;"));
        btnComm.setOnMouseExited(e -> btnComm.setStyle("-fx-background-color:white; -fx-text-fill:" + accent + "; -fx-font-size:12; -fx-font-weight:700;" +
                                                        "-fx-padding:9 0 9 0; -fx-background-radius:12; -fx-cursor:hand;" +
                                                        "-fx-border-color:" + accent + "; -fx-border-width:1.5; -fx-border-radius:12;"));

        content.getChildren().addAll(titre, matiere, descLabel, sep, progressBox, btn, btnComm);

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

    @FXML private void onGitHub() {
        try {
            System.out.println("[FrontCoursController] Navigation vers GitHub Examples");
            if (onNavigateGitHub != null) {
                // Utiliser le callback si disponible (reste dans le frontoffice)
                onNavigateGitHub.run();
            } else {
                // Fallback: changer toute la scène (comportement actuel)
                tn.esprit.MainApp.showGitHubExamples();
            }
        } catch (Exception e) {
            System.err.println("[FrontCoursController] Erreur navigation GitHub: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void onMaListe() {
        try {
            System.out.println("[FrontCoursController] Navigation vers Ma Liste");
            if (onNavigateMaListe != null) {
                // Utiliser le callback si disponible (reste dans le frontoffice)
                onNavigateMaListe.run();
            } else {
                // Fallback: changer toute la scène (comportement actuel)
                tn.esprit.MainApp.showTodoList();
            }
        } catch (Exception e) {
            System.err.println("[FrontCoursController] Erreur navigation Ma Liste: " + e.getMessage());
            e.printStackTrace();
        }
    }

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

    // ── Objectif d'apprentissage ───────────────────────────────────────────────

    @FXML
    private void onDefinirObjectif() {
        // Créer la popup
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("🎯 Définir mon objectif d'apprentissage");
        popup.setResizable(false);

        // ── Header ──
        VBox header = new VBox(4);
        header.setStyle("-fx-background-color:linear-gradient(to right,#f59e0b,#d97706); -fx-padding:20 24 20 24;");
        Label hTitle = new Label("🎯 Mon Objectif d'Apprentissage");
        hTitle.setStyle("-fx-font-size:17; -fx-font-weight:800; -fx-text-fill:white;");
        Label hSub = new Label("Choisissez un objectif pour voir les cours recommandés");
        hSub.setStyle("-fx-font-size:12; -fx-text-fill:rgba(255,255,255,0.85);");
        header.getChildren().addAll(hTitle, hSub);

        // ── Corps ──
        VBox body = new VBox(16);
        body.setStyle("-fx-padding:24; -fx-background-color:white;");

        // Objectifs prédéfinis
        Label lblPredefined = new Label("Objectifs prédéfinis :");
        lblPredefined.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#374151;");

        javafx.scene.layout.FlowPane predefinedPane = new javafx.scene.layout.FlowPane(8, 8);
        javafx.scene.control.ToggleGroup toggleGroup = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.TextField objectifField = new javafx.scene.control.TextField();

        for (String obj : LearningObjectiveService.PREDEFINED_OBJECTIVES.keySet()) {
            javafx.scene.control.ToggleButton btn = new javafx.scene.control.ToggleButton(obj);
            btn.setToggleGroup(toggleGroup);
            btn.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#4e3b9c; -fx-font-size:12; -fx-font-weight:600;" +
                         "-fx-padding:7 14 7 14; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
            btn.selectedProperty().addListener((obs, old, selected) -> {
                if (selected) {
                    btn.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700;" +
                                 "-fx-padding:7 14 7 14; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
                    objectifField.setText(obj); // remplir le champ texte
                } else {
                    btn.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#4e3b9c; -fx-font-size:12; -fx-font-weight:600;" +
                                 "-fx-padding:7 14 7 14; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
                }
            });
            predefinedPane.getChildren().add(btn);
        }

        // Objectif libre
        Label lblLibre = new Label("Ou écrivez votre propre objectif :");
        lblLibre.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#374151;");
        objectifField.setPromptText("Ex: Data Science, Cybersecurity, Mobile...");
        objectifField.setStyle("-fx-background-color:#f9fafb; -fx-border-color:#e5e7eb; -fx-border-radius:10;" +
                               "-fx-background-radius:10; -fx-padding:12 16; -fx-font-size:13;");

        // Niveau
        Label lblNiveau = new Label("Niveau :");
        lblNiveau.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#374151;");
        javafx.scene.control.ComboBox<String> niveauCombo = new javafx.scene.control.ComboBox<>();
        niveauCombo.getItems().addAll("Tous", "Débutant", "Intermédiaire", "Avancé");
        niveauCombo.setValue("Tous");
        niveauCombo.setStyle("-fx-background-color:#f9fafb; -fx-border-color:#e5e7eb; -fx-border-radius:10;" +
                             "-fx-background-radius:10; -fx-padding:8 12; -fx-font-size:13;");
        niveauCombo.setMaxWidth(Double.MAX_VALUE);

        // Bouton valider
        Button btnValider = new Button("✨ Appliquer le filtrage");
        btnValider.setMaxWidth(Double.MAX_VALUE);
        btnValider.setStyle("-fx-background-color:#f59e0b; -fx-text-fill:white; -fx-font-size:14; -fx-font-weight:700;" +
                            "-fx-padding:13 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");

        btnValider.setOnAction(e -> {
            String objectif = objectifField.getText().trim();
            String niveau   = niveauCombo.getValue();
            if (objectif.isEmpty()) {
                objectifField.setStyle("-fx-background-color:#fef2f2; -fx-border-color:#dc2626; -fx-border-radius:10;" +
                                       "-fx-background-radius:10; -fx-padding:12 16; -fx-font-size:13;");
                return;
            }
            // Appliquer le filtrage
            appliquerObjectif(objectif, niveau);
            popup.close();
        });

        body.getChildren().addAll(lblPredefined, predefinedPane, lblLibre, objectifField, lblNiveau, niveauCombo, btnValider);

        VBox root = new VBox(0, header, body);
        popup.setScene(new javafx.scene.Scene(root, 520, 480));
        popup.show();
    }

    /**
     * Applique le filtrage des cours selon l'objectif et le niveau.
     * Affiche la bannière "objectif actif" et les cours filtrés.
     */
    private void appliquerObjectif(String objectif, String niveau) {
        List<Cours> filtered = objectiveService.filterCoursByObjective(allCours, objectif, niveau);

        // Afficher la bannière avec l'objectif actif
        if (banniereObjectif != null) {
            banniereObjectif.setVisible(true);
            banniereObjectif.setManaged(true);
        }
        if (labelObjectifActif != null) {
            String niveauText = (niveau != null && !niveau.equals("Tous")) ? " • Niveau : " + niveau : "";
            labelObjectifActif.setText("Objectif : " + objectif + niveauText +
                                       " — " + filtered.size() + " cours trouvé(s)");
        }

        if (filtered.isEmpty()) {
            // Aucun cours trouvé → message avec suggestions
            cardsContainer.getChildren().clear();
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(40));
            Label icon = new Label("🔍");
            icon.setStyle("-fx-font-size:48;");
            Label msg = new Label("Aucun cours trouvé pour cet objectif.");
            msg.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#6b7280;");
            Label suggestion = new Label("Essayez : Web Developer, Java, Python, Data Science...");
            suggestion.setStyle("-fx-font-size:12; -fx-text-fill:#9ca3af;");
            emptyBox.getChildren().addAll(icon, msg, suggestion);
            cardsContainer.getChildren().add(emptyBox);
            if (labelEmpty != null) { labelEmpty.setVisible(false); labelEmpty.setManaged(false); }
        } else {
            afficher(filtered);
        }
    }

    @FXML
    private void onVoirTousLesCours() {
        // Masquer la bannière et afficher tous les cours
        if (banniereObjectif != null) {
            banniereObjectif.setVisible(false);
            banniereObjectif.setManaged(false);
        }
        afficher(allCours);
    }
}
