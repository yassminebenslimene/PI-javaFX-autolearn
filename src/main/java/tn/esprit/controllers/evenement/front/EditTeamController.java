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

public class EditTeamController {

    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private MenuButton menuUser;
    @FXML private TextField fieldNomEquipe;
    @FXML private VBox studentsContainer;
    @FXML private Label labelCount;
    @FXML private Label labelError;

    private final EquipeService equipeService = new EquipeService();
    private Equipe equipe;
    private Evenement evenement;
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private int currentUserId;
    private List<Integer> currentMemberIds = new ArrayList<>();

    public void setData(Equipe eq, Evenement ev) {
        this.equipe = eq;
        this.evenement = ev;
        if (fieldNomEquipe != null) fieldNomEquipe.setText(eq.getNom());
        currentMemberIds = equipeService.getEtudiantsByEquipe(eq.getId())
                .stream().map(Etudiant::getId).toList();
        loadStudents();
    }

    @FXML
    public void initialize() {
        FrontNavHelper.initNavbar(labelAvatarNav, labelCurrentUser, menuUser);
        var user = SessionManager.getCurrentUser();
        if (user != null) currentUserId = user.getId();
    }

    private void loadStudents() {
        studentsContainer.getChildren().clear();
        checkBoxes.clear();
        List<Etudiant> etudiants = equipeService.getAllEtudiants();
        for (Etudiant et : etudiants) {
            boolean isCurrentUser = et.getId() == currentUserId;
            boolean isMember = currentMemberIds.contains(et.getId());
            CheckBox cb = new CheckBox();
            cb.setSelected(isMember || isCurrentUser);
            cb.setDisable(isCurrentUser);
            cb.setUserData(et.getId());
            cb.setOnAction(e -> updateCount());

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
    private void onUpdateTeam() {
        labelError.setText("");
        String nom = fieldNomEquipe.getText().trim();
        if (nom.isEmpty()) { labelError.setText("Le nom est obligatoire."); return; }
        List<Integer> selectedIds = checkBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> (Integer) cb.getUserData())
                .toList();
        if (selectedIds.size() < 4 || selectedIds.size() > 6) {
            labelError.setText("Vous devez sélectionner entre 4 et 6 étudiants.");
            return;
        }
        equipe.setNom(nom);
        equipeService.modifier(equipe);
        equipeService.supprimerEtudiantsEquipe(equipe.getId());
        for (int etId : selectedIds) equipeService.ajouterEtudiantEquipe(equipe.getId(), etId);

        try { MainApp.showMesEquipes("Equipe modifiee avec succes !"); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onCancel() {
        try { MainApp.showTeamDetails(equipe, evenement, false); } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
