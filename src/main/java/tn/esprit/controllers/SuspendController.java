package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.util.Date;

public class SuspendController {

    @FXML private Label            labelTitle;
    @FXML private Label            labelSubtitle;
    @FXML private ComboBox<String> comboReason;
    @FXML private TextField        fieldCustomReason;
    @FXML private Label            labelCustomReason;
    @FXML private Label            errorReason;
    @FXML private Button           btnAction;
    @FXML private Label            labelCurrentStatus;

    private static final String AUTRE = "Autre (préciser)";
    private static final java.util.List<String> REASONS = java.util.List.of(
        "Comportement inapproprié",
        "Violation des règles de la plateforme",
        "Contenu frauduleux ou triche",
        "Inactivité prolongée",
        "Demande de l'étudiant",
        AUTRE
    );

    private User user;
    private final UserService service = new UserService();

    @FXML
    public void initialize() {
        comboReason.setItems(FXCollections.observableArrayList(REASONS));
        comboReason.valueProperty().addListener((obs, o, val) -> {
            boolean isAutre = AUTRE.equals(val);
            labelCustomReason.setVisible(isAutre); labelCustomReason.setManaged(isAutre);
            fieldCustomReason.setVisible(isAutre); fieldCustomReason.setManaged(isAutre);
        });
    }

    public void setUser(User user) {
        this.user = user;
        labelSubtitle.setText(user.getPrenom() + " " + user.getNom() + " — " + user.getEmail());

        if (user.isIsSuspended()) {
            labelTitle.setText("Lever la suspension");
            labelCurrentStatus.setText("Statut actuel : ⛔ Suspendu");
            labelCurrentStatus.setStyle("-fx-text-fill:#f87171; -fx-font-weight:bold;");
            btnAction.setText("✔  Lever la suspension");
            btnAction.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10 24 10 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
            comboReason.setDisable(true);
            comboReason.setPromptText("—");
            if (user.getSuspensionReason() != null) comboReason.setValue(user.getSuspensionReason());
        } else {
            labelTitle.setText("Suspendre l'étudiant");
            labelCurrentStatus.setText("Statut actuel : ✅ Actif");
            labelCurrentStatus.setStyle("-fx-text-fill:#4ade80; -fx-font-weight:bold;");
            btnAction.setText("⚠  Suspendre");
            btnAction.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10 24 10 24; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        }
    }

    @FXML
    private void onAction() {
        errorReason.setText("");

        if (!user.isIsSuspended()) {
            String selected = comboReason.getValue();
            if (selected == null || selected.isBlank()) {
                errorReason.setText("Veuillez sélectionner une raison.");
                return;
            }
            String reason = AUTRE.equals(selected)
                    ? fieldCustomReason.getText().trim()
                    : selected;
            if (reason.isBlank()) {
                errorReason.setText("Veuillez préciser la raison.");
                return;
            }
            user.setIsSuspended(true);
            user.setSuspendedAt(new Date());
            user.setSuspensionReason(reason);
            user.setSuspendedBy(SessionManager.getCurrentUser().getId());
        } else {
            user.setIsSuspended(false);
            user.setSuspendedAt(null);
            user.setSuspensionReason(null);
            user.setSuspendedBy(null);
        }

        service.modifier(user);

        // ── Log under ADMIN's ID ──────────────────────────────────────────────
        var admin = SessionManager.getCurrentUser();
        int logId = (admin != null) ? admin.getId() : user.getId();
        if (user.isIsSuspended()) {
            ActivityApiClient.logAsync(logId, "admin.suspended_student",
                java.util.Map.of("student_email", user.getEmail(),
                                 "student_name", user.getPrenom() + " " + user.getNom(),
                                 "reason", user.getSuspensionReason() != null ? user.getSuspensionReason() : ""));
        } else {
            ActivityApiClient.logAsync(logId, "admin.reactivated_student",
                java.util.Map.of("student_email", user.getEmail(),
                                 "student_name", user.getPrenom() + " " + user.getNom()));
        }

        // Send email notification (async)
        if (user.isIsSuspended()) {
            EmailService.sendSuspensionNotification(user.getEmail(), user.getPrenom(),
                user.getSuspensionReason() != null ? user.getSuspensionReason() : "Non précisée");
        } else {
            EmailService.sendReactivationNotification(user.getEmail(), user.getPrenom());
        }

        ((Stage) btnAction.getScene().getWindow()).close();
    }

    @FXML private void onCancel() {
        ((Stage) btnAction.getScene().getWindow()).close();
    }
}
