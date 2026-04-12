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

    @FXML private tn.esprit.controllers.NavbarController navbarController;
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
            }

    private void loadJoinOptions() {
        joinTeamsContainer.getChildren().clear();
        List<Equipe> equipes = equipeService.getByEvenement(evenement.getId());
        List<Equipe> available = equipes.stream()
                .filter(eq -> equipeService.countMembres(eq.getId()) < 6)
                .toList();

        if (available.isEmpty()) {
            labelJoinStatus.setText("Aucune equipe disponible avec des places libres");
        } else {
            labelJoinStatus.setText("");
            Label title = new Label("Equipes disponibles (" + available.size() + ")");
            title.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:#1e1e1e; -fx-padding:8 0 8 0;");
            joinTeamsContainer.getChildren().add(title);

            // Cards grid
            javafx.scene.layout.FlowPane grid = new javafx.scene.layout.FlowPane(16, 16);
            for (Equipe eq : available) {
                int membres = equipeService.countMembres(eq.getId());
                int spots = 6 - membres;
                javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(8);
                card.setPadding(new javafx.geometry.Insets(16));
                card.setPrefWidth(200);
                card.setStyle("-fx-background-color:white; -fx-background-radius:12;"
                        + "-fx-border-color:#eeeeee; -fx-border-radius:12; -fx-border-width:1;"
                        + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

                Label nomLbl = new Label(eq.getNom());
                nomLbl.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
                Label membresLbl = new Label("\uD83D\uDC65 " + membres + " / 6 membres");
                membresLbl.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
                Label spotsLbl = new Label("\u2713 " + spots + " places disponibles");
                spotsLbl.setStyle("-fx-font-size:11; -fx-text-fill:#059669; -fx-font-weight:600;");

                Button joinBtn = new Button("Rejoindre cette equipe");
                joinBtn.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:12;"
                        + "-fx-font-weight:700; -fx-padding:8 16 8 16; -fx-background-radius:8;"
                        + "-fx-cursor:hand; -fx-border-width:0;");
                final Equipe selected = eq;
                joinBtn.setOnAction(e -> joinTeam(selected));

                card.getChildren().addAll(nomLbl, membresLbl, spotsLbl, joinBtn);
                grid.getChildren().add(card);
            }
            joinTeamsContainer.getChildren().add(grid);
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
