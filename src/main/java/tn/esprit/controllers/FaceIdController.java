package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Ellipse;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.util.concurrent.CompletableFuture;

public class FaceIdController {

    public enum Mode { LOGIN, REGISTER }

    @FXML private Label       labelSubtitle;
    @FXML private ImageView   cameraView;
    @FXML private Ellipse     faceOverlay;
    @FXML private VBox        statusOverlay;
    @FXML private Label       labelCameraStatus;
    @FXML private ProgressBar progressBar;
    @FXML private Label       labelResult;
    @FXML private VBox        emailBox;
    @FXML private TextField   fieldEmail;
    @FXML private Button      btnStart;
    @FXML private Button      btnCancel;

    private Mode mode = Mode.LOGIN;
    private final UserService userService = new UserService();
    private Timeline dotAnimation;

    public void setMode(Mode mode) {
        this.mode = mode;
        Platform.runLater(() -> {
            if (mode == Mode.REGISTER) {
                labelSubtitle.setText("Enregistrez votre visage");
                btnStart.setText("Demarrer l enregistrement");
                emailBox.setVisible(false);
                emailBox.setManaged(false);
            } else {
                labelSubtitle.setText("Connectez-vous avec votre visage");
                btnStart.setText("Demarrer");
                emailBox.setVisible(true);
                emailBox.setManaged(true);
            }
        });
    }

    public void prefillEmail(String email) {
        Platform.runLater(() -> { if (fieldEmail != null) fieldEmail.setText(email); });
    }

    @FXML
    private void onStart() {
        btnStart.setDisable(true);

        // Check server
        if (!FaceIdService.isServerRunning()) {
            showResult("Serveur Face ID non demarre.\n\nLancez dans un terminal:\n  python faceid_server.py", false);
            btnStart.setDisable(false);
            return;
        }

        if (mode == Mode.LOGIN) doLogin();
        else doRegister();
    }

    private void doLogin() {
        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) { showResult("Veuillez entrer votre email.", false); btnStart.setDisable(false); return; }

        User user = userService.trouverParEmail(email);
        if (user == null) { showResult("Aucun compte trouve.", false); btnStart.setDisable(false); return; }
        if (!FaceIdService.hasFaceRegistered(user.getId())) {
            showResult("Aucun visage enregistre.\nActivez Face ID dans votre profil.", false);
            btnStart.setDisable(false); return;
        }
        if (user.isIsSuspended()) { showResult("Compte suspendu.", false); btnStart.setDisable(false); return; }

        setStatus("Regardez la camera...");
        startDotAnimation("Analyse du visage");

        final User finalUser = user;
        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.authenticateFace(finalUser.getId());
            Platform.runLater(() -> {
                stopDotAnimation();
                if (result.success()) {
                    showResult("Bienvenue " + finalUser.getPrenom() + " !", true);
                    SessionManager.login(finalUser);
                    ActivityApiClient.logAsync(finalUser.getId(), "user.login",
                        java.util.Map.of("method", "face_id", "email", finalUser.getEmail()));
                    new Timeline(new KeyFrame(Duration.millis(1500), e -> {
                        closeDialog();
                        try {
                            if ("ADMIN".equals(finalUser.getRole())) MainApp.showBackoffice();
                            else MainApp.showFrontoffice();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    })).play();
                } else {
                    showResult(result.message(), false);
                    btnStart.setDisable(false);
                }
            });
        });
    }

    private void doRegister() {
        User user = SessionManager.getCurrentUser();
        if (user == null) { showResult("Vous devez etre connecte.", false); btnStart.setDisable(false); return; }

        setStatus("Placez votre visage devant la camera...");
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);
        startDotAnimation("Enregistrement en cours");

        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.registerFace(
                user.getId(),
                pct -> Platform.runLater(() -> {
                    progressBar.setProgress(pct / 100.0);
                    setStatus("Enregistrement... " + pct + "%");
                })
            );
            Platform.runLater(() -> {
                stopDotAnimation();
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                if (result.success()) {
                    showResult("Face ID active avec succes !\nVous pouvez maintenant vous connecter avec votre visage.", true);
                    btnStart.setText("Fermer");
                    btnStart.setDisable(false);
                    btnStart.setOnAction(e -> closeDialog());
                } else {
                    showResult(result.message(), false);
                    btnStart.setDisable(false);
                }
            });
        });
    }

    private void startDotAnimation(String base) {
        final int[] dots = {0};
        dotAnimation = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            dots[0] = (dots[0] + 1) % 4;
            setStatus(base + ".".repeat(dots[0]));
        }));
        dotAnimation.setCycleCount(Timeline.INDEFINITE);
        dotAnimation.play();
    }

    private void stopDotAnimation() {
        if (dotAnimation != null) { dotAnimation.stop(); dotAnimation = null; }
    }

    @FXML
    private void onCancel() { stopDotAnimation(); closeDialog(); }

    private void closeDialog() {
        try { ((Stage) btnCancel.getScene().getWindow()).close(); }
        catch (Exception ignored) {}
    }

    private void setStatus(String t) {
        Platform.runLater(() -> { if (labelCameraStatus != null) labelCameraStatus.setText(t); });
    }

    private void showResult(String msg, boolean ok) {
        Platform.runLater(() -> {
            if (labelResult == null) return;
            labelResult.setText(msg);
            labelResult.setStyle(ok
                ? "-fx-font-size:12;-fx-font-weight:600;-fx-background-radius:8;-fx-padding:10 14 10 14;-fx-border-radius:8;-fx-border-width:1;-fx-text-fill:#34d399;-fx-background-color:rgba(5,150,105,0.15);-fx-border-color:rgba(5,150,105,0.3);"
                : "-fx-font-size:12;-fx-font-weight:600;-fx-background-radius:8;-fx-padding:10 14 10 14;-fx-border-radius:8;-fx-border-width:1;-fx-text-fill:#f85149;-fx-background-color:rgba(248,81,73,0.12);-fx-border-color:rgba(248,81,73,0.3);"
            );
            labelResult.setVisible(true);
            labelResult.setManaged(true);
        });
    }
}