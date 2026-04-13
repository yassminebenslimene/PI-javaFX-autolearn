package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Participation;
import tn.esprit.services.EquipeService;
import tn.esprit.services.EvenementService;
import tn.esprit.services.ParticipationService;
import tn.esprit.session.SessionManager;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MesParticipationsController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private VBox participationsContainer;
    @FXML private VBox successBannerContainer;
    @FXML private Label labelSuccessBanner;

    private final ParticipationService participationService = new ParticipationService();
    private final EquipeService equipeService = new EquipeService();
    private final EvenementService evenementService = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static String pendingSuccessMsg = null;

    public static void setPendingSuccess(String msg) { pendingSuccessMsg = msg; }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);

        if (pendingSuccessMsg != null && !pendingSuccessMsg.isEmpty()) {
            labelSuccessBanner.setText(pendingSuccessMsg);
            labelSuccessBanner.setVisible(true);
            labelSuccessBanner.setManaged(true);
            successBannerContainer.setVisible(true);
            successBannerContainer.setManaged(true);
            pendingSuccessMsg = null;
        } else {
            labelSuccessBanner.setVisible(false);
            labelSuccessBanner.setManaged(false);
            successBannerContainer.setVisible(false);
            successBannerContainer.setManaged(false);
        }

        loadParticipations();
    }

    private void loadParticipations() {
        participationsContainer.getChildren().clear();
        var user = SessionManager.getCurrentUser();
        if (!(user instanceof Etudiant etudiant)) return;

        List<Participation> participations = participationService.getByEtudiant(etudiant.getId());
        if (participations.isEmpty()) {
            Label empty = new Label("Vous n'avez pas encore de participations.");
            empty.setStyle("-fx-font-size:14; -fx-text-fill:#888; -fx-padding:32;");
            participationsContainer.getChildren().add(empty);
            return;
        }

        for (Participation p : participations) {
            Equipe eq = equipeService.getById(p.getEquipeId());
            Evenement ev = evenementService.getById(p.getEvenementId());
            if (eq == null || ev == null) continue;
            participationsContainer.getChildren().add(buildParticipationCard(p, eq, ev));
        }
    }

    private VBox buildParticipationCard(Participation p, Equipe eq, Evenement ev) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#eeeeee; -fx-border-radius:16; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);"
                + "-fx-max-width:480;");

        // Status badge
        Label statusBadge = new Label("\u2713 Accepte");
        statusBadge.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#065f46; -fx-font-size:11;"
                + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:4 12 4 12;");

        Label titre = new Label(ev.getTitre());
        titre.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");

        HBox meta1 = new HBox(24);
        meta1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        meta1.getChildren().addAll(
                metaLabel("\uD83D\uDC65 Equipe: " + eq.getNom()),
                metaLabel("\uD83D\uDCCD Lieu: " + (ev.getLieu() != null ? ev.getLieu() : ""))
        );
        HBox meta2 = new HBox();
        if (ev.getDateDebut() != null)
            meta2.getChildren().add(metaLabel("\uD83D\uDCC5 Date: " + ev.getDateDebut().format(FMT)));

        Button viewBtn = new Button("\uD83C\uDFAF  Voir les details");
        viewBtn.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12;"
                + "-fx-font-weight:700; -fx-padding:9 20 9 20; -fx-background-radius:10;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        viewBtn.setOnAction(e -> {
            try { MainApp.showParticipationDetails(p, eq, ev); } catch (Exception ex) { ex.printStackTrace(); }
        });

        card.getChildren().addAll(statusBadge, titre, meta1, meta2);

        // Feedback button if event is past
        boolean isPast = ev.getDateFin() != null && java.time.LocalDateTime.now().isAfter(ev.getDateFin());
        if (isPast) {
            boolean hasFeedback = p.getFeedbacks() != null && !p.getFeedbacks().isBlank();
            Button feedbackBtn = new Button(hasFeedback ? "\u2713  Modifier mon feedback" : "Donner mon feedback");
            feedbackBtn.setStyle("-fx-background-color:" + (hasFeedback ? "#059669" : "#f59e0b") + ";"
                    + "-fx-text-fill:white; -fx-font-size:12;"
                    + "-fx-font-weight:700; -fx-padding:9 20 9 20; -fx-background-radius:10;"
                    + "-fx-cursor:hand; -fx-border-width:0;");
            feedbackBtn.setOnAction(e -> {
                try { MainApp.showFeedback(p, ev); } catch (Exception ex) { ex.printStackTrace(); }
            });
            HBox btnRow = new HBox(10, viewBtn, feedbackBtn);
            card.getChildren().add(btnRow);
        } else {
            card.getChildren().add(viewBtn);
        }
        return card;
    }

    private Label metaLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#666;");
        return l;
    }

    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onEvenements() { FrontNavHelper.goEvenements(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
