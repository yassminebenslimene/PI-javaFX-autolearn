package tn.esprit.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.session.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiConsumer;

/**
 * FrontChapitreController — affiche les chapitres d'un cours côté étudiant.
 * Chaque carte chapitre a 3 boutons : Lire / PDF (téléchargement) / Quiz.
 */
public class FrontChapitreController {

    @FXML private Label labelCourseTitle;
    @FXML private Label labelCourseMeta;
    @FXML private Label labelEmpty;
    @FXML private VBox  chaptersContainer;

    private final ServiceChapitre       serviceChapitre = new ServiceChapitre();
    private final CourseProgressService progressService  = new CourseProgressService();

    private BiConsumer<Cours, Chapitre>           onLireChapitre;
    private java.util.function.Consumer<Chapitre> onPasserQuiz;
    private Runnable                              onRetourCours;
    private Set<Integer>                          completedIds = new HashSet<>();

    public void setOnLireChapitre(BiConsumer<Cours, Chapitre> callback) { this.onLireChapitre = callback; }
    public BiConsumer<Cours, Chapitre> getOnLireChapitre() { return onLireChapitre; }
    public void setOnPasserQuiz(java.util.function.Consumer<Chapitre> callback) { this.onPasserQuiz = callback; }
    public void setOnRetourCours(Runnable callback) { this.onRetourCours = callback; }
    public java.util.function.Consumer<Chapitre> getOnPasserQuiz() { return onPasserQuiz; }

    /** Appelé depuis le FXML via onAction="#onRetourCours" */
    @FXML
    private void onRetourCours() {
        if (onRetourCours != null) onRetourCours.run();
    }

    public void setCours(Cours cours) {
        labelCourseTitle.setText(cours.getTitre());
        labelCourseMeta.setText("Matière: " + cours.getMatiere()
            + "  |  Niveau: " + cours.getNiveau()
            + "  |  Durée: " + cours.getDuree() + "h");

        // Charger les chapitres complétés par l'étudiant
        if (SessionManager.getCurrentUser() != null) {
            completedIds = new HashSet<>(
                progressService.getCompletedChapitreIds(
                    SessionManager.getCurrentUser().getId(), cours.getId()));
        }

        List<Chapitre> chapitres = serviceChapitre.consulterParCoursId(cours.getId());
        chaptersContainer.getChildren().clear();

        if (chapitres.isEmpty()) {
            labelEmpty.setVisible(true); labelEmpty.setManaged(true); return;
        }
        labelEmpty.setVisible(false); labelEmpty.setManaged(false);

        FlowPane grid = new FlowPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPrefWrapLength(1000);

        int i = 0;
        for (Chapitre chapitre : chapitres) {
            VBox card = buildChapitreCard(cours, chapitre);
            // Animation : fade-in + slide-up décalée par carte
            card.setOpacity(0);
            card.setTranslateY(30);
            grid.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(400), card);
            fade.setFromValue(0); fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(400), card);
            slide.setFromY(30); slide.setToY(0);

            ParallelTransition anim = new ParallelTransition(fade, slide);
            anim.setDelay(Duration.millis(80 * i));
            anim.play();
            i++;
        }
        chaptersContainer.getChildren().add(grid);
    }

    private VBox buildChapitreCard(Cours cours, Chapitre chapitre) {
        // Icône
        Label icon = new Label("📖");
        icon.setStyle("-fx-font-size:32; -fx-background-color:linear-gradient(to bottom right,#a5f3fc,#818cf8);"
            + "-fx-background-radius:16; -fx-padding:14 18 14 18;");
        HBox iconBox = new HBox(icon);
        iconBox.setAlignment(Pos.CENTER);

        // Titre
        Label titre = new Label(chapitre.getTitre());
        titre.setWrapText(true);
        titre.setAlignment(Pos.CENTER);
        titre.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#0f172a; -fx-alignment:CENTER;");

        // Contenu aperçu
        String contenuText = chapitre.getContenu() == null ? "" : chapitre.getContenu();
        if (contenuText.length() > 80) contenuText = contenuText.substring(0, 80) + "...";
        Label contenu = new Label(contenuText);
        contenu.setWrapText(true);
        contenu.setAlignment(Pos.CENTER);
        contenu.setStyle("-fx-font-size:12; -fx-text-fill:#64748b; -fx-alignment:CENTER;");

        // Badge
        Label badge = new Label("📌  Chapitre " + chapitre.getOrdre());
        badge.setStyle("-fx-background-color:linear-gradient(to right,#34d399,#059669);"
            + "-fx-text-fill:white; -fx-font-size:11; -fx-font-weight:700;"
            + "-fx-padding:5 14 5 14; -fx-background-radius:999;");
        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.CENTER);

        // Badge "✓ Complété" si le chapitre est réussi
        boolean completed = completedIds.contains(chapitre.getId());
        Label completedBadge = new Label("✓  Complété");
        completedBadge.setStyle("-fx-background-color:rgba(5,150,105,0.12);"
            + "-fx-text-fill:#059669; -fx-font-size:11; -fx-font-weight:700;"
            + "-fx-padding:4 12 4 12; -fx-background-radius:999;");
        completedBadge.setVisible(completed); completedBadge.setManaged(completed);
        HBox completedBox = new HBox(completedBadge);
        completedBox.setAlignment(Pos.CENTER);

        // Cours parent
        Label coursLabel = new Label("🎓  " + cours.getTitre());
        coursLabel.setStyle("-fx-font-size:11; -fx-text-fill:#94a3b8;");
        HBox coursBox = new HBox(coursLabel);
        coursBox.setAlignment(Pos.CENTER);

        // Bouton Lire
        Button btnLire = new Button("📖   Lire le chapitre");
        btnLire.setMaxWidth(Double.MAX_VALUE);
        btnLire.setStyle("-fx-background-color:linear-gradient(to right,#06b6d4,#0ea5e9);"
            + "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;"
            + "-fx-padding:11 0 11 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btnLire.setOnAction(e -> { if (onLireChapitre != null) onLireChapitre.accept(cours, chapitre); });

        // Bouton PDF — télécharge le chapitre en PDF
        Button btnPdf = new Button("📄   PDF");
        btnPdf.setMaxWidth(Double.MAX_VALUE);
        btnPdf.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9);"
            + "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;"
            + "-fx-padding:11 0 11 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btnPdf.setOnAction(e -> telechargerPdf(cours, chapitre));

        // Bouton Quiz
        Button btnQuiz = new Button("❓   Passer le quiz");
        btnQuiz.setMaxWidth(Double.MAX_VALUE);
        btnQuiz.setStyle("-fx-background-color:linear-gradient(to right,#f59e0b,#d97706);"
            + "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;"
            + "-fx-padding:11 0 11 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btnQuiz.setOnAction(e -> { if (onPasserQuiz != null) onPasserQuiz.accept(chapitre); });

        VBox buttons = new VBox(8, btnLire, btnPdf, btnQuiz);

        VBox card = new VBox(12, iconBox, titre, contenu, badgeBox, completedBox, coursBox, buttons);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.setStyle("-fx-background-color:white; -fx-background-radius:18;"
            + "-fx-border-color:#e2e8f0; -fx-border-radius:18;"
            + "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.09),14,0,0,4);");

        // Couleur douce au hover (pas de scale, juste fond légèrement coloré)
        card.setOnMouseEntered(e ->
            card.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:18;"
                + "-fx-border-color:#c4b5fd; -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian,rgba(129,140,248,0.18),16,0,0,5);"));
        card.setOnMouseExited(e ->
            card.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-border-color:#e2e8f0; -fx-border-radius:18;"
                + "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.09),14,0,0,4);"));

        return card;
    }

    // ── GÉNÉRATION PDF ────────────────────────────────────────────────────────
    /**
     * Génère un PDF du chapitre avec iText et ouvre un FileChooser pour sauvegarder.
     * Le PDF contient : titre du cours, titre du chapitre, contenu, ressource.
     */
    private void telechargerPdf(Cours cours, Chapitre chapitre) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le chapitre en PDF");
        chooser.setInitialFileName("Chapitre_" + chapitre.getOrdre() + "_" +
            chapitre.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File file = chooser.showSaveDialog(chaptersContainer.getScene().getWindow());
        if (file == null) return; // annulé

        try {
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // Polices
            Font fontTitreCours = new Font(Font.FontFamily.HELVETICA, 11, Font.ITALIC, BaseColor.GRAY);
            Font fontTitre      = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(74, 58, 156));
            Font fontSousTitre  = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.DARK_GRAY);
            Font fontBody       = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            Font fontLabel      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(100, 100, 100));

            // En-tête
            Paragraph header = new Paragraph("AutoLearn  —  " + cours.getTitre(), fontTitreCours);
            header.setAlignment(Element.ALIGN_RIGHT);
            doc.add(header);
            doc.add(new Paragraph(" "));

            // Titre chapitre
            Paragraph titrePara = new Paragraph("Chapitre " + chapitre.getOrdre() + " : " + chapitre.getTitre(), fontTitre);
            titrePara.setSpacingAfter(10);
            doc.add(titrePara);

            // Ligne séparatrice
            com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
            line.setLineColor(new BaseColor(122, 106, 216));
            doc.add(new Chunk(line));
            doc.add(new Paragraph(" "));

            // Infos cours
            doc.add(new Paragraph("Cours : " + cours.getTitre(), fontLabel));
            doc.add(new Paragraph("Matière : " + cours.getMatiere() + "   |   Niveau : " + cours.getNiveau()
                + "   |   Durée : " + cours.getDuree() + "h", fontLabel));
            doc.add(new Paragraph(" "));

            // Contenu
            doc.add(new Paragraph("Contenu", fontSousTitre));
            doc.add(new Paragraph(" "));
            String contenu = chapitre.getContenu() == null ? "Aucun contenu." : chapitre.getContenu();
            Paragraph contenuPara = new Paragraph(contenu, fontBody);
            contenuPara.setLeading(16);
            doc.add(contenuPara);

            // Ressource
            if (chapitre.getRessources() != null && !chapitre.getRessources().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Ressource", fontSousTitre));
                doc.add(new Paragraph(chapitre.getRessources(), fontBody));
            }

            // Pied de page
            doc.add(new Paragraph(" "));
            doc.add(new Chunk(line));
            Paragraph footer = new Paragraph("Généré par AutoLearn", fontTitreCours);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();

            // Confirmation
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF téléchargé");
            alert.setHeaderText(null);
            alert.setContentText("Le chapitre a été sauvegardé :\n" + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur PDF");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de générer le PDF : " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
