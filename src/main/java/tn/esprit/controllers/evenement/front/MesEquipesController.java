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

import java.time.format.DateTimeFormatter;
import java.util.List;
public class MesEquipesController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private VBox teamsContainer;
    @FXML private VBox successBannerContainer;
    @FXML private Label labelSuccessBanner;

    private final EquipeService equipeService = new EquipeService();
    private final EvenementService evenementService = new EvenementService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static String pendingSuccessMsg = null;

    public static void setPendingSuccess(String msg) { pendingSuccessMsg = msg; }

    @FXML
    public void initialize() {
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

        loadTeams();
    }

    private void loadTeams() {
        teamsContainer.getChildren().clear();
        var user = SessionManager.getCurrentUser();
        if (!(user instanceof Etudiant etudiant)) return;

        List<Equipe> equipes = equipeService.getEquipesByEtudiant(etudiant.getId());
        if (equipes.isEmpty()) {
            Label empty = new Label("Vous n'avez pas encore d'equipes.");
            empty.setStyle("-fx-font-size:14; -fx-text-fill:#888; -fx-padding:32;");
            teamsContainer.getChildren().add(empty);
            return;
        }

        // Grid: 2 columns
        int col = 0;
        HBox row = null;
        for (Equipe eq : equipes) {
            if (col % 2 == 0) {
                row = new HBox(24);
                teamsContainer.getChildren().add(row);
            }
            Evenement ev = evenementService.getById(eq.getEvenementId());
            VBox card = buildTeamCard(eq, ev);
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(card);
            col++;
        }
        // Fill last row if odd
        if (col % 2 != 0 && row != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().add(spacer);
        }
    }

    private VBox buildTeamCard(Equipe eq, Evenement ev) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:#eeeeee; -fx-border-radius:16; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        // Header: name + members badge
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label nomLabel = new Label(eq.getNom());
        nomLabel.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        HBox.setHgrow(nomLabel, Priority.ALWAYS);
        int membres = equipeService.countMembres(eq.getId());
        Label membersBadge = new Label(membres + " membres");
        membersBadge.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:11;"
                + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:4 12 4 12;");
        header.getChildren().addAll(nomLabel, membersBadge);

        // Event info
        VBox info = new VBox(6);
        if (ev != null) {
            info.getChildren().add(metaLabel("\uD83D\uDCC5 Evenement: " + ev.getTitre()));
            info.getChildren().add(metaLabel("\uD83D\uDCCD Lieu: " + (ev.getLieu() != null ? ev.getLieu() : "")));
            if (ev.getDateDebut() != null)
                info.getChildren().add(metaLabel("\uD83D\uDD50 Date: " + ev.getDateDebut().format(FMT)));
        }

        // Buttons
        HBox buttons = new HBox(8);
        buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Button viewBtn = new Button("Voir les details");
        viewBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:11;"
                + "-fx-font-weight:600; -fx-padding:7 14 7 14; -fx-background-radius:8;"
                + "-fx-cursor:hand; -fx-border-color:#ddd; -fx-border-width:1; -fx-border-radius:8;");
        viewBtn.setOnAction(e -> {
            if (ev != null) {
                try { MainApp.showTeamDetails(eq, ev, false); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color:#fff7ed; -fx-text-fill:#ea580c; -fx-font-size:11;"
                + "-fx-font-weight:600; -fx-padding:7 14 7 14; -fx-background-radius:8;"
                + "-fx-cursor:hand; -fx-border-color:#fed7aa; -fx-border-width:1; -fx-border-radius:8;");
        editBtn.setOnAction(e -> {
            if (ev != null) {
                try { MainApp.showEditTeam(eq, ev); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        Button participateBtn = new Button("Participer a l'evenement");
        participateBtn.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:11;"
                + "-fx-font-weight:700; -fx-padding:7 14 7 14; -fx-background-radius:8;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        participateBtn.setOnAction(e -> {
            if (ev != null) {
                try { MainApp.showTeamDetails(eq, ev, false); } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        buttons.getChildren().addAll(viewBtn, editBtn, participateBtn);
        card.getChildren().addAll(header, info, buttons);
        return card;
    }

    private Label metaLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#555;");
        return l;
    }

    @FXML
    private void onCreateNewTeam() {
        try { MainApp.showSelectEvent(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onEvenements() { FrontNavHelper.goEvenements(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
