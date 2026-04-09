package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.MainApp;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EquipeService;
import tn.esprit.services.EvenementService;
import tn.esprit.session.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class EvenementFrontController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private VBox eventsContainer;

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);
        loadEvenements();
    }

    private void loadEvenements() {
        eventsContainer.getChildren().clear();
        List<Evenement> list = evenementService.getAll();
        for (Evenement ev : list) {
            eventsContainer.getChildren().add(buildEventCard(ev));
        }
    }

    private VBox buildEventCard(Evenement ev) {
        boolean isCancelled = ev.isIsCanceled() || "Annulé".equals(ev.getStatus());
        boolean isPast = ev.getDateFin() != null && LocalDateTime.now().isAfter(ev.getDateFin());

        // Outer card
        VBox card = new VBox();
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:" + (isCancelled ? "#fca5a5" : "#eeeeee") + ";"
                + "-fx-border-radius:16; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        // Header row (always visible)
        HBox header = new HBox(16);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 24));

        // Image placeholder
        Label imgPlaceholder = new Label("🖼");
        imgPlaceholder.setStyle("-fx-font-size:36; -fx-background-color:#e8e4ff; -fx-background-radius:10;"
                + "-fx-padding:10 14 10 14; -fx-min-width:80; -fx-min-height:60;");

        // Info
        VBox info = new VBox(6);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Type + cancelled badge
        HBox badges = new HBox(8);
        badges.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label typeBadge = new Label(ev.getType() != null ? ev.getType().toUpperCase() : "");
        typeBadge.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:10;"
                + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 10 3 10;");
        badges.getChildren().add(typeBadge);
        if (isCancelled) {
            Label cancelBadge = new Label("✗  CANCELLED");
            cancelBadge.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626; -fx-font-size:10;"
                    + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 10 3 10;");
            badges.getChildren().add(cancelBadge);
        }

        Label titre = new Label(ev.getTitre());
        titre.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:" + (isCancelled ? "#999" : "#1e1e1e") + ";"
                + (isCancelled ? "-fx-strikethrough:true;" : ""));

        HBox meta = new HBox(20);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (ev.getDateDebut() != null) {
            meta.getChildren().add(metaLabel("📅 " + ev.getDateDebut().format(FMT)));
            meta.getChildren().add(metaLabel("🕐 " + ev.getDateDebut().format(FMT_TIME)));
        }
        if (ev.getLieu() != null) meta.getChildren().add(metaLabel("📍 " + ev.getLieu()));
        int nbEquipes = equipeService.countByEvenement(ev.getId());
        meta.getChildren().add(metaLabel("👥 " + nbEquipes + " / " + ev.getNbMax() + " teams"));

        info.getChildren().addAll(badges, titre, meta);

        // TIME LEFT box
        VBox timeBox = buildTimeBox(ev);

        // Expand button
        Button expandBtn = new Button("∨");
        expandBtn.setStyle("-fx-background-color:#f0f0f8; -fx-text-fill:#7a6ad8; -fx-font-size:16;"
                + "-fx-font-weight:700; -fx-background-radius:50%; -fx-padding:8 12 8 12;"
                + "-fx-cursor:hand; -fx-border-width:0; -fx-min-width:40; -fx-min-height:40;");

        header.getChildren().addAll(imgPlaceholder, info, timeBox, expandBtn);

        // Details panel (hidden by default)
        VBox details = new VBox(16);
        details.setPadding(new Insets(0, 24, 20, 24));
        details.setVisible(false);
        details.setManaged(false);
        buildDetails(details, ev, isCancelled, isPast, nbEquipes);

        expandBtn.setOnAction(e -> {
            boolean showing = details.isVisible();
            details.setVisible(!showing);
            details.setManaged(!showing);
            expandBtn.setText(showing ? "∨" : "∧");
        });

        card.getChildren().addAll(header, details);
        return card;
    }

    private void buildDetails(VBox details, Evenement ev, boolean isCancelled, boolean isPast, int nbEquipes) {
        // About
        Label aboutTitle = new Label("ℹ  About This Event");
        aboutTitle.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#7a6ad8;");
        Label desc = new Label(ev.getDescription() != null ? ev.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:12; -fx-text-fill:#555;");
        details.getChildren().addAll(aboutTitle, desc);

        // Available spots
        int spots = ev.getNbMax() - nbEquipes;
        Label spotsTitle = new Label("⊙  Available Spots");
        spotsTitle.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#7a6ad8;");
        HBox spotsBox = new HBox();
        spotsBox.setStyle("-fx-background-color:#eff6ff; -fx-background-radius:8;"
                + "-fx-border-color:#bfdbfe; -fx-border-radius:8; -fx-border-width:0 0 0 4;"
                + "-fx-border-color:#7a6ad8; -fx-padding:12;");
        Label spotsLabel = new Label(spots + " spots remaining out of " + ev.getNbMax() + " total");
        spotsLabel.setStyle("-fx-font-size:13; -fx-text-fill:#1e4d8c;");
        spotsBox.getChildren().add(spotsLabel);
        details.getChildren().addAll(spotsTitle, spotsBox);

        // Participating teams
        List<Equipe> equipes = equipeService.getByEvenement(ev.getId());
        Label teamsTitle = new Label("👥  Participating Teams (" + equipes.size() + ")");
        teamsTitle.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#7a6ad8;");
        details.getChildren().add(teamsTitle);

        if (equipes.isEmpty()) {
            Label noTeams = new Label("No teams registered yet. Be the first to participate!");
            noTeams.setStyle("-fx-font-size:12; -fx-text-fill:#888;");
            details.getChildren().add(noTeams);
        } else {
            HBox teamsRow = new HBox(12);
            for (Equipe eq : equipes) {
                VBox teamCard = new VBox(8);
                teamCard.setAlignment(javafx.geometry.Pos.CENTER);
                teamCard.setStyle("-fx-background-color:white; -fx-background-radius:12;"
                        + "-fx-border-color:#eeeeee; -fx-border-radius:12; -fx-border-width:1;"
                        + "-fx-padding:16; -fx-min-width:120;");
                Label avatar = new Label("👤");
                avatar.setStyle("-fx-font-size:28; -fx-background-color:#e8e4ff; -fx-background-radius:50%;"
                        + "-fx-padding:10 12 10 12;");
                Label typeBadge = new Label(ev.getType() != null ? ev.getType() : "");
                typeBadge.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:9;"
                        + "-fx-background-radius:20; -fx-padding:2 8 2 8;");
                Label nomEq = new Label(eq.getNom());
                nomEq.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
                int membres = equipeService.countMembres(eq.getId());
                Label membresLabel = new Label("👥 " + membres + " members");
                membresLabel.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
                teamCard.getChildren().addAll(avatar, typeBadge, nomEq, membresLabel);
                teamsRow.getChildren().add(teamCard);
            }
            details.getChildren().add(teamsRow);
        }

        // Status banner or Participate button
        if (isCancelled) {
            details.getChildren().add(buildCancelledBanner());
        } else if (isPast) {
            details.getChildren().add(buildCompletedBanner());
        } else if (spots > 0) {
            Button participateBtn = new Button("🎯  Participate in This Event");
            participateBtn.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:13;"
                    + "-fx-font-weight:700; -fx-padding:12 32 12 32; -fx-background-radius:30;"
                    + "-fx-cursor:hand; -fx-border-width:0;"
                    + "-fx-effect:dropshadow(gaussian,rgba(5,150,105,0.3),8,0,0,2);");
            participateBtn.setOnAction(e -> {
                try {
                    MainApp.showJoinEvent(ev);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            "Erreur: " + ex.getMessage() + (ex.getCause() != null ? "\n" + ex.getCause().getMessage() : ""));
                    alert.setTitle("Erreur navigation");
                    alert.showAndWait();
                }
            });
            HBox btnBox = new HBox(participateBtn);
            btnBox.setAlignment(javafx.geometry.Pos.CENTER);
            details.getChildren().add(btnBox);
        }
    }

    private VBox buildTimeBox(Evenement ev) {
        VBox box = new VBox(4);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.setStyle("-fx-background-color:#fffbeb; -fx-background-radius:12;"
                + "-fx-border-color:#f59e0b; -fx-border-radius:12; -fx-border-width:2;"
                + "-fx-padding:12 16 12 16; -fx-min-width:100;");
        Label title = new Label("TIME LEFT");
        title.setStyle("-fx-font-size:10; -fx-font-weight:700; -fx-text-fill:#92400e;");
        long days = ev.getDateDebut() != null
                ? Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), ev.getDateDebut()))
                : 0;
        Label daysLabel = new Label(days + " days");
        daysLabel.setStyle("-fx-font-size:18; -fx-font-weight:800; -fx-text-fill:#f59e0b;");
        box.getChildren().addAll(title, daysLabel);
        return box;
    }

    private HBox buildCancelledBanner() {
        HBox banner = new HBox(12);
        banner.setAlignment(javafx.geometry.Pos.CENTER);
        banner.setStyle("-fx-background-color:#fee2e2; -fx-background-radius:12;"
                + "-fx-border-color:#fca5a5; -fx-border-radius:12; -fx-border-width:1; -fx-padding:20;");
        VBox content = new VBox(6);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        Label icon = new Label("✕");
        icon.setStyle("-fx-font-size:24; -fx-text-fill:#dc2626;");
        Label title = new Label("Event Cancelled");
        title.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#dc2626;");
        Label msg = new Label("This event has been cancelled. No registrations are accepted.");
        msg.setStyle("-fx-font-size:12; -fx-text-fill:#666;");
        content.getChildren().addAll(icon, title, msg);
        banner.getChildren().add(content);
        return banner;
    }

    private HBox buildCompletedBanner() {
        HBox banner = new HBox(12);
        banner.setAlignment(javafx.geometry.Pos.CENTER);
        banner.setStyle("-fx-background-color:#e8eaf6; -fx-background-radius:12;"
                + "-fx-border-color:#c5cae9; -fx-border-radius:12; -fx-border-width:1; -fx-padding:20;");
        VBox content = new VBox(6);
        content.setAlignment(javafx.geometry.Pos.CENTER);
        Label icon = new Label("🏁");
        icon.setStyle("-fx-font-size:24;");
        Label title = new Label("Event Completed");
        title.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#3949ab;");
        Label msg = new Label("This event has ended. Registrations are now closed.");
        msg.setStyle("-fx-font-size:12; -fx-text-fill:#666;");
        content.getChildren().addAll(icon, title, msg);
        banner.getChildren().add(content);
        return banner;
    }

    private Label metaLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#666;");
        return l;
    }

    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
