package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;

import java.text.SimpleDateFormat;

public class ShowUserController {

    @FXML private Label labelInitials;
    @FXML private Label labelFullName;
    @FXML private Label labelRoleBadge;
    @FXML private Label valNom;
    @FXML private Label valPrenom;
    @FXML private Label valEmail;
    @FXML private Label valNiveau;
    @FXML private HBox  rowNiveau;      // HBox in show.fxml
    @FXML private Label valStatut;
    @FXML private Label valCreated;
    @FXML private Label valSuspendedAt;
    @FXML private Label valSuspendedReason;
    @FXML private VBox  rowSuspension;  // VBox in show.fxml

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public void setUser(User user) {
        String initials = user.getPrenom().substring(0,1).toUpperCase()
                        + user.getNom().substring(0,1).toUpperCase();
        labelInitials.setText(initials);
        labelFullName.setText(user.getPrenom() + " " + user.getNom());
        labelRoleBadge.setText(user.getRole());
        labelRoleBadge.setStyle("ADMIN".equals(user.getRole())
            ? "-fx-background-color:#1a4a8a; -fx-text-fill:#60a5fa; -fx-background-radius:20; -fx-padding:4 14 4 14; -fx-font-weight:bold; -fx-font-size:12;"
            : "-fx-background-color:#1a4a2a; -fx-text-fill:#4ade80; -fx-background-radius:20; -fx-padding:4 14 4 14; -fx-font-weight:bold; -fx-font-size:12;");

        valNom.setText(user.getNom());
        valPrenom.setText(user.getPrenom());
        valEmail.setText(user.getEmail());
        valCreated.setText(user.getCreatedAt() != null ? SDF.format(user.getCreatedAt()) : "—");

        boolean isEtudiant = user instanceof Etudiant;
        rowNiveau.setVisible(isEtudiant); rowNiveau.setManaged(isEtudiant);
        if (isEtudiant) valNiveau.setText(((Etudiant) user).getNiveau() != null ? ((Etudiant) user).getNiveau() : "—");

        if (user.isIsSuspended()) {
            valStatut.setText("⛔  Suspendu");
            valStatut.setStyle("-fx-text-fill:#f87171; -fx-font-weight:bold;");
            rowSuspension.setVisible(true); rowSuspension.setManaged(true);
            valSuspendedAt.setText(user.getSuspendedAt() != null ? SDF.format(user.getSuspendedAt()) : "—");
            valSuspendedReason.setText(user.getSuspensionReason() != null ? user.getSuspensionReason() : "—");
        } else {
            // Active: check if last login was within 7 days
            boolean recentLogin = user.getLastLoginAt() != null &&
                (System.currentTimeMillis() - user.getLastLoginAt().getTime()) < 7L * 24 * 60 * 60 * 1000;
            if (recentLogin) {
                valStatut.setText("✅  Actif  •  Connecté récemment");
                valStatut.setStyle("-fx-text-fill:#4ade80; -fx-font-weight:bold;");
            } else {
                valStatut.setText("✅  Actif");
                valStatut.setStyle("-fx-text-fill:#4ade80; -fx-font-weight:bold;");
            }
            rowSuspension.setVisible(false); rowSuspension.setManaged(false);
        }
    }

    @FXML private void onClose() {
        ((Stage) labelFullName.getScene().getWindow()).close();
    }
}
