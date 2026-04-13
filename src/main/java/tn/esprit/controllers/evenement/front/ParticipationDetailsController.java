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

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ParticipationDetailsController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private Label labelStatut;
    @FXML private Label labelEventName;
    @FXML private Label labelLieu;
    @FXML private Label labelType;
    @FXML private Label labelDateDebut;
    @FXML private Label labelDateFin;
    @FXML private Label labelNbMax;
    @FXML private Label labelTeamTitle;
    @FXML private VBox membersContainer;

    private final EquipeService equipeService = new EquipeService();
    private final ParticipationService participationService = new ParticipationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Participation participation;
    private Equipe equipe;
    private Evenement evenement;

    public void setData(Participation p, Equipe eq, Evenement ev) {
        this.participation = p;
        this.equipe = eq;
        this.evenement = ev;
        refresh();
    }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);
    }

    private void refresh() {
        if (labelEventName != null) labelEventName.setText(evenement.getTitre());
        if (labelLieu != null) labelLieu.setText(evenement.getLieu() != null ? evenement.getLieu() : "");
        if (labelType != null) labelType.setText(evenement.getType() != null ? evenement.getType() : "");
        if (labelDateDebut != null && evenement.getDateDebut() != null)
            labelDateDebut.setText(evenement.getDateDebut().format(FMT));
        if (labelDateFin != null && evenement.getDateFin() != null)
            labelDateFin.setText(evenement.getDateFin().format(FMT));
        if (labelNbMax != null) labelNbMax.setText(String.valueOf(evenement.getNbMax()));
        if (labelTeamTitle != null) labelTeamTitle.setText("Equipe: " + equipe.getNom());

        List<Etudiant> membres = equipeService.getEtudiantsByEquipe(equipe.getId());
        if (membersContainer != null) {
            membersContainer.getChildren().clear();
            for (Etudiant et : membres) {
                membersContainer.getChildren().add(buildMemberRow(et));
            }
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
        Label email = new Label(et.getEmail() != null ? et.getEmail() : "");
        email.setStyle("-fx-font-size:11; -fx-text-fill:#888;");
        info.getChildren().addAll(name, email);
        row.getChildren().addAll(avatar, info);
        return row;
    }

    @FXML
    private void onEditParticipation() {
        try { MainApp.showEditParticipation(participation); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void onDeleteParticipation() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette participation et l'equipe associee ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer la suppression");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                participationService.supprimerAvecEquipe(participation.getId());
                FrontNavHelper.goMesParticipations(null);
            }
        });
    }

    @FXML private void onBack() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
