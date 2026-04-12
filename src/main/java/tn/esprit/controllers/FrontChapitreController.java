package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FrontChapitreController {

    @FXML private Label labelCourseTitle;
    @FXML private Label labelCourseMeta;
    @FXML private Label labelEmpty;
    @FXML private VBox chaptersContainer;

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();
    private BiConsumer<Cours, Chapitre> onLireChapitre;
    private Consumer<Chapitre> onPasserQuiz;

    public void setOnLireChapitre(BiConsumer<Cours, Chapitre> callback) {
        this.onLireChapitre = callback;
    }

    public void setOnPasserQuiz(Consumer<Chapitre> callback) {
        this.onPasserQuiz = callback;
    }

    public void setCours(Cours cours) {
        labelCourseTitle.setText(cours.getTitre());
        labelCourseMeta.setText("Matière: " + cours.getMatiere() + "  |  Niveau: " + cours.getNiveau() + "  |  Durée: " + cours.getDuree() + "h");

        List<Chapitre> chapitres = serviceChapitre.consulterParCoursId(cours.getId());
        chaptersContainer.getChildren().clear();

        if (chapitres.isEmpty()) {
            labelEmpty.setVisible(true);
            labelEmpty.setManaged(true);
            return;
        }

        labelEmpty.setVisible(false);
        labelEmpty.setManaged(false);

        // Grille responsive avec FlowPane
        FlowPane grid = new FlowPane();
        grid.setHgap(18);
        grid.setVgap(18);
        grid.setPrefWrapLength(900);

        for (Chapitre chapitre : chapitres) {
            grid.getChildren().add(buildChapitreCard(cours, chapitre));
        }

        chaptersContainer.getChildren().add(grid);
    }

    private VBox buildChapitreCard(Cours cours, Chapitre chapitre) {
        String coursTitre = cours.getTitre();
        // Icône livre en haut
        Label icon = new Label("📖");
        icon.setStyle(
            "-fx-font-size:32; -fx-background-color:linear-gradient(to bottom right,#a5f3fc,#818cf8);" +
            "-fx-background-radius:16; -fx-padding:14 18 14 18;"
        );
        HBox iconBox = new HBox(icon);
        iconBox.setAlignment(Pos.CENTER);

        // Titre du chapitre
        Label titre = new Label(chapitre.getTitre());
        titre.setWrapText(true);
        titre.setAlignment(Pos.CENTER);
        titre.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#0f172a; -fx-alignment:CENTER;");

        // Contenu (tronqué à 80 chars)
        String contenuText = chapitre.getContenu() == null ? "" : chapitre.getContenu();
        if (contenuText.length() > 80) contenuText = contenuText.substring(0, 80) + "...";
        Label contenu = new Label(contenuText);
        contenu.setWrapText(true);
        contenu.setAlignment(Pos.CENTER);
        contenu.setStyle("-fx-font-size:12; -fx-text-fill:#64748b; -fx-alignment:CENTER;");

        // Badge "Chapitre N"
        Label badge = new Label("📌  Chapitre " + chapitre.getOrdre());
        badge.setStyle(
            "-fx-background-color:linear-gradient(to right,#34d399,#059669);" +
            "-fx-text-fill:white; -fx-font-size:11; -fx-font-weight:700;" +
            "-fx-padding:5 14 5 14; -fx-background-radius:999;"
        );
        HBox badgeBox = new HBox(badge);
        badgeBox.setAlignment(Pos.CENTER);

        // Nom du cours
        Label coursLabel = new Label("🎓  " + coursTitre);
        coursLabel.setStyle("-fx-font-size:11; -fx-text-fill:#94a3b8;");
        HBox coursBox = new HBox(coursLabel);
        coursBox.setAlignment(Pos.CENTER);

        // Bouton Lire le chapitre
        Button btnLire = new Button("📖   Lire le chapitre");
        btnLire.setMaxWidth(Double.MAX_VALUE);
        btnLire.setStyle(
            "-fx-background-color:linear-gradient(to right,#06b6d4,#0ea5e9);" +
            "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;" +
            "-fx-padding:11 0 11 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"
        );
        btnLire.setOnAction(e -> {
            if (onLireChapitre != null) onLireChapitre.accept(cours, chapitre);
        });

        // Bouton PDF
        Button btnPdf = new Button("📄   PDF");
        btnPdf.setMaxWidth(Double.MAX_VALUE);
        btnPdf.setStyle(
            "-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9);" +
            "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;" +
            "-fx-padding:11 0 11 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"
        );

        // Bouton Quiz
        Button btnQuiz = new Button("❓   Passer le quiz");
        btnQuiz.setMaxWidth(Double.MAX_VALUE);
        btnQuiz.setStyle(
            "-fx-background-color:linear-gradient(to right,#f59e0b,#d97706);" +
            "-fx-text-fill:white; -fx-font-weight:700; -fx-font-size:13;" +
            "-fx-padding:11 0 11 0; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;"
        );
        btnQuiz.setOnAction(e -> {
            if (onPasserQuiz != null) onPasserQuiz.accept(chapitre);
        });

        VBox buttons = new VBox(8, btnLire, btnPdf, btnQuiz);

        VBox card = new VBox(12, iconBox, titre, contenu, badgeBox, coursBox, buttons);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(240);
        card.setMaxWidth(240);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:18;" +
            "-fx-border-color:#e2e8f0; -fx-border-radius:18;" +
            "-fx-effect:dropshadow(gaussian,rgba(15,23,42,0.09),14,0,0,4);"
        );

        return card;
    }
}
