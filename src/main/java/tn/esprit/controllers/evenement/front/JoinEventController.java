package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EquipeService;
import tn.esprit.session.SessionManager;

import java.util.List;

public class JoinEventController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private Label labelEventName;
    @FXML private Label labelJoinStatus;
    @FXML private VBox joinTeamsContainer;

    private final EquipeService equipeService = new EquipeService();
    private Evenement evenement;

    public void setEvenement(Evenement ev) {
        this.evenement = ev;
        if (labelEventName != null) labelEventName.setText(ev.getTitre());
        loadJoinOptions();
    }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);
    }

    private void loadJoinOptions() {
        joinTeamsContainer.getChildren().clear();
        List<Equipe> equipes = equipeService.getByEvenement(evenement.getId());
        // Filter teams with < 6 members
        List<Equipe> available = equipes.stream()
                .filter(eq -> equipeService.countMembres(eq.getId()) < 6)
                .toList();

        if (available.isEmpty()) {
            labelJoinStatus.setText("No teams available with open spots");
        } else {
            labelJoinStatus.setText("");
            for (Equipe eq : available) {
                int membres = equipeService.countMembres(eq.getId());
                Button btn = new Button("Join \"" + eq.getNom() + "\" (" + membres + "/6 members)");
                btn.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12;"
                        + "-fx-font-weight:600; -fx-padding:8 20 8 20; -fx-background-radius:8;"
                        + "-fx-cursor:hand; -fx-border-width:0;");
                final Equipe selected = eq;
                btn.setOnAction(e -> joinTeam(selected));
                joinTeamsContainer.getChildren().add(btn);
            }
        }
    }

    private void joinTeam(Equipe eq) {
        var user = SessionManager.getCurrentUser();
        if (!(user instanceof Etudiant etudiant)) return;
        equipeService.ajouterEtudiantEquipe(eq.getId(), etudiant.getId());
        // Create participation
        tn.esprit.entities.Participation p = new tn.esprit.entities.Participation(eq.getId(), evenement.getId());
        p.setStatut("Accepté");
        new tn.esprit.services.ParticipationService().ajouter(p);
        FrontNavHelper.goMesParticipations("✓ Participation acceptée avec succès ! Votre équipe \""
                + eq.getNom() + "\" est inscrite à l'événement \"" + evenement.getTitre() + "\".");
    }

    @FXML private void onCreateTeam() {
        try {
            MainApp.showCreateTeam(evenement);
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Erreur: " + e.getMessage() + (e.getCause() != null ? "\n" + e.getCause().getMessage() : ""));
            alert.setTitle("Erreur navigation");
            alert.showAndWait();
        }
    }

    @FXML private void onBack() { FrontNavHelper.goEvenements(); }
    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onEvenements() { FrontNavHelper.goEvenements(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
