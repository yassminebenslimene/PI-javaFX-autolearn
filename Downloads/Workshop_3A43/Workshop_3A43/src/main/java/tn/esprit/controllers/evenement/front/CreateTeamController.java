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
import tn.esprit.session.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class CreateTeamController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelForEvent;
    @FXML private TextField fieldNomEquipe;
    @FXML private VBox studentsContainer;
    @FXML private Label labelCount;
    @FXML private Label labelError;

    private final EquipeService equipeService = new EquipeService();
    private Evenement evenement;
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private int currentUserId;

    public void setEvenement(Evenement ev) {
        this.evenement = ev;
        if (labelForEvent != null) labelForEvent.setText("Pour l'evenement : " + ev.getTitre());
        loadStudents();
    }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, null);
        var user = SessionManager.getCurrentUser();
        if (user != null) currentUserId = user.getId();
    }

    private void loadStudents() {
        studentsContainer.getChildren().clear();
        checkBoxes.clear();
        List<Etudiant> etudiants = equipeService.getAllEtudiants();
        for (Etudiant et : etudiants) {
            boolean isCurrentUser = et.getId() == currentUserId;
            CheckBox cb = new CheckBox();
            cb.setSelected(isCurrentUser);
            cb.setDisable(isCurrentUser);
            cb.setStyle("-fx-cursor:hand;");

            String label = et.getPrenom() + " " + et.getNom() + " - "
                    + (et.getNiveau() != null ? et.getNiveau().toUpperCase() : "")
                    + (isCurrentUser ? " (Vous - Obligatoire)" : "");

            Label lbl = new Label(label);
            lbl.setStyle("-fx-font-size:13; -fx-text-fill:" + (isCurrentUser ? "#7a6ad8" : "#333") + ";"
                    + (isCurrentUser ? "-fx-font-weight:700;" : ""));

            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 16, 10, 16));
            row.setStyle("-fx-background-color:" + (isCurrentUser ? "#f5f3ff" : "white") + ";"
                    + "-fx-border-color:#f0f0f0; -fx-border-width:0 0 1 0;");
            row.getChildren().addAll(cb, lbl);

            // Store etudiant id in cb's user data
            cb.setUserData(et.getId());
            cb.setOnAction(e -> updateCount());
            checkBoxes.add(cb);
            studentsContainer.getChildren().add(row);
        }
        updateCount();
    }

    private void updateCount() {
        long count = checkBoxes.stream().filter(CheckBox::isSelected).count();
        if (labelCount != null) labelCount.setText(count + " selectionne(s)");
    }

    @FXML
    private void onCreateTeam() {
        labelError.setText("");
        String nom = fieldNomEquipe.getText().trim();
        if (nom.isEmpty()) {
            labelError.setText("Le nom de l'équipe est obligatoire.");
            return;
        }
        List<Integer> selectedIds = checkBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .toList();
        if (selectedIds.size() < 4 || selectedIds.size() > 6) {
            labelError.setText("Vous devez sélectionner entre 4 et 6 étudiants.");
            return;
        }

        // Validation 1 : aucun membre ne doit déjà être dans une équipe pour cet événement
        for (int etId : selectedIds) {
            if (equipeService.etudiantDejaInscritEvenement(etId, evenement.getId())) {
                // Find student name
                String name = checkBoxes.stream()
                        .filter(cb -> cb.getUserData().equals(etId))
                        .findFirst()
                        .map(cb -> {
                            int idx = checkBoxes.indexOf(cb);
                            if (idx >= 0 && idx < studentsContainer.getChildren().size()) {
                                javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) studentsContainer.getChildren().get(idx);
                                return ((javafx.scene.control.Label) row.getChildren().get(1)).getText().split(" - ")[0];
                            }
                            return "Un étudiant";
                        }).orElse("Un étudiant");
                labelError.setText(name + " participe déjà à cet événement avec une autre équipe.");
                return;
            }
        }

        // Validation 2 : conflit horaire — aucun membre ne doit participer à un événement en même temps
        if (evenement.getDateDebut() != null && evenement.getDateFin() != null) {
            for (int etId : selectedIds) {
                if (equipeService.etudiantConflitHoraire(etId, evenement.getDateDebut(), evenement.getDateFin())) {
                    String name = checkBoxes.stream()
                            .filter(cb -> cb.getUserData().equals(etId))
                            .findFirst()
                            .map(cb -> {
                                int idx = checkBoxes.indexOf(cb);
                                if (idx >= 0 && idx < studentsContainer.getChildren().size()) {
                                    javafx.scene.layout.HBox row = (javafx.scene.layout.HBox) studentsContainer.getChildren().get(idx);
                                    return ((javafx.scene.control.Label) row.getChildren().get(1)).getText().split(" - ")[0];
                                }
                                return "Un étudiant";
                            }).orElse("Un étudiant");
                    labelError.setText(name + " participe déjà à un autre événement qui se déroule en même temps.");
                    return;
                }
            }
        }

        Equipe equipe = new Equipe(nom, evenement.getId());
        int equipeId = equipeService.ajouterEtRetournerId(equipe);
        if (equipeId < 0) {
            labelError.setText("Erreur lors de la création de l'équipe.");
            return;
        }
        equipe.setId(equipeId);
        for (int etId : selectedIds) {
            equipeService.ajouterEtudiantEquipe(equipeId, etId);
        }

        try { MainApp.showTeamDetails(equipe, evenement, true); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onCancel() { FrontNavHelper.goEvenements(); }
    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onCours() { FrontNavHelper.goCours(); }
    @FXML private void onChallenges() { FrontNavHelper.goChallenges(); }
    @FXML private void onEvenements() { FrontNavHelper.goEvenements(); }
    @FXML private void onCommunaute() { FrontNavHelper.goCommunaute(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
