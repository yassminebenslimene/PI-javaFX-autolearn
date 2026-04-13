package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.scene.control.*;
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

public class EditParticipationController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private ComboBox<String> comboEquipe;
    @FXML private ComboBox<String> comboEvenement;
    @FXML private Label labelError;

    private final EquipeService equipeService = new EquipeService();
    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Participation participation;
    private List<Equipe> equipes;
    private List<Evenement> evenements;

    public void setParticipation(Participation p) {
        this.participation = p;
        loadDropdowns();
    }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);
    }

    private void loadDropdowns() {
        var user = SessionManager.getCurrentUser();
        if (!(user instanceof Etudiant etudiant)) return;

        // Load equipes of this student
        equipes = equipeService.getEquipesByEtudiant(etudiant.getId());
        comboEquipe.getItems().clear();
        for (Equipe eq : equipes) comboEquipe.getItems().add(eq.getNom());
        // Select current
        for (int i = 0; i < equipes.size(); i++) {
            if (equipes.get(i).getId() == participation.getEquipeId()) {
                comboEquipe.getSelectionModel().select(i);
                break;
            }
        }

        // Load available evenements
        evenements = evenementService.getAll().stream()
                .filter(ev -> !ev.isIsCanceled() && !"Annulé".equals(ev.getStatus()))
                .toList();
        comboEvenement.getItems().clear();
        for (Evenement ev : evenements) {
            String label = ev.getTitre() + " - " + (ev.getLieu() != null ? ev.getLieu() : "")
                    + (ev.getDateDebut() != null ? " (" + ev.getDateDebut().format(FMT) + ")" : "");
            comboEvenement.getItems().add(label);
        }
        // Select current
        for (int i = 0; i < evenements.size(); i++) {
            if (evenements.get(i).getId() == participation.getEvenementId()) {
                comboEvenement.getSelectionModel().select(i);
                break;
            }
        }
    }

    @FXML
    private void onUpdate() {
        labelError.setText("");
        int equipeIdx = comboEquipe.getSelectionModel().getSelectedIndex();
        int evenementIdx = comboEvenement.getSelectionModel().getSelectedIndex();
        if (equipeIdx < 0) { labelError.setText("Sélectionnez une équipe."); return; }
        if (evenementIdx < 0) { labelError.setText("Sélectionnez un événement."); return; }

        participation.setEquipeId(equipes.get(equipeIdx).getId());
        participation.setEvenementId(evenements.get(evenementIdx).getId());
        participationService.modifierComplet(participation);

        Equipe eq = equipes.get(equipeIdx);
        Evenement ev = evenements.get(evenementIdx);
        try { MainApp.showParticipationDetails(participation, eq, ev); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onCancel() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
