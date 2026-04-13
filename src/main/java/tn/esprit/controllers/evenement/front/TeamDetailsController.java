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
import tn.esprit.services.ParticipationService;

import java.util.List;

public class TeamDetailsController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private HBox successBanner;
    @FXML private Label labelTeamName;
    @FXML private Label labelMembersCount;
    @FXML private Label labelMembersTitle;
    @FXML private VBox membersContainer;
    @FXML private Button btnParticipate;

    private final EquipeService equipeService = new EquipeService();
    private final ParticipationService participationService = new ParticipationService();
    private Equipe equipe;
    private Evenement evenement;
    private boolean showSuccess;

    public void setData(Equipe eq, Evenement ev, boolean showSuccess) {
        this.equipe = eq;
        this.evenement = ev;
        this.showSuccess = showSuccess;
        refresh();
    }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);
    }

    private void refresh() {
        if (successBanner != null) {
            successBanner.setVisible(showSuccess);
            successBanner.setManaged(showSuccess);
        }
        if (labelTeamName != null) labelTeamName.setText(equipe.getNom());

        List<Etudiant> membres = equipeService.getEtudiantsByEquipe(equipe.getId());
        if (labelMembersCount != null) labelMembersCount.setText(membres.size() + " Membres");
        if (labelMembersTitle != null) labelMembersTitle.setText("Membres de l'equipe (" + membres.size() + ")");

        if (membersContainer != null) {
            membersContainer.getChildren().clear();
            for (Etudiant et : membres) {
                membersContainer.getChildren().add(buildMemberRow(et));
            }
        }

        if (btnParticipate != null) {
            btnParticipate.setText("\uD83C\uDFAF  Participer a " + evenement.getTitre());
        }
    }

    private HBox buildMemberRow(Etudiant et) {
        HBox row = new HBox(14);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color:#f8f8ff; -fx-background-radius:10;"
                + "-fx-border-color:#eeeeee; -fx-border-radius:10; -fx-border-width:1;");

        String initials = et.getPrenom().substring(0, 1).toUpperCase()
                + et.getNom().substring(0, 1).toUpperCase();
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-weight:700;"
                + "-fx-font-size:13; -fx-background-radius:50%; -fx-padding:8 10 8 10;");

        VBox info = new VBox(2);
        Label name = new Label(et.getPrenom() + " " + et.getNom());
        name.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        Label emailLvl = new Label(et.getEmail() + " \u2022 Niveau: " + (et.getNiveau() != null ? et.getNiveau().toUpperCase() : ""));
        emailLvl.setStyle("-fx-font-size:11; -fx-text-fill:#888;");
        info.getChildren().addAll(name, emailLvl);

        row.getChildren().addAll(avatar, info);
        return row;
    }

    @FXML
    private void onParticipate() {
        Participation p = new Participation(equipe.getId(), evenement.getId());
        p.setStatut("Accepté");
        participationService.ajouter(p);
        FrontNavHelper.goMesParticipations("✓ Participation acceptée avec succès ! Votre équipe \""
                + equipe.getNom() + "\" est inscrite à l'événement \"" + evenement.getTitre() + "\".");
    }

    @FXML
    private void onEditTeam() {
        try { MainApp.showEditTeam(equipe, evenement); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onDeleteTeam() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer l'equipe \"" + equipe.getNom() + "\" et ses participations ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                equipeService.supprimerAvecParticipations(equipe.getId());
                FrontNavHelper.goEvenements();
            }
        });
    }

    @FXML private void onBackToEvents() { FrontNavHelper.goEvenements(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
