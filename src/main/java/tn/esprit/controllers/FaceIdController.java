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
import tn.esprit.session.JwtManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    private Timeline previewTimeline;
    private Timeline monitorTimeline;
    private final UserService userService = new UserService();

    private final AtomicBoolean faceDetected = new AtomicBoolean(false);
    private final AtomicBoolean capturing    = new AtomicBoolean(false);
    private final AtomicInteger countdown    = new AtomicInteger(0);

    public void setMode(Mode mode) {
        this.mode = mode;
        Platform.runLater(() -> {
            if (mode == Mode.REGISTER) {
                labelSubtitle.setText("Placez votre visage dans le cercle");
                btnStart.setText("Ouvrir la camera");
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
        if (!FaceIdService.isServerRunning()) {
            showResult("Serveur Face ID non demarre.\nLancez: python faceid_server.py", false);
            return;
        }
        btnStart.setDisable(true);
        if (statusOverlay != null) { statusOverlay.setVisible(false); statusOverlay.setManaged(false); }
        startPreview();
        if (mode == Mode.LOGIN) doLogin();
        else { setStatus("Placez votre visage dans le cercle"); monitorForRegistration(); }
    }

    // ── Preview — fetches frames from Python server ───────────────────────────

    private void startPreview() {
        previewTimeline = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            // Fetch frame in background to avoid blocking FX thread
            CompletableFuture.supplyAsync(() -> FaceIdService.getFrame())
                .thenAccept(result -> {
                    if (result != null) {
                        faceDetected.set(result.faceDetected());
                        Platform.runLater(() -> cameraView.setImage(result.image()));
                    }
                });
        }));
        previewTimeline.setCycleCount(Timeline.INDEFINITE);
        previewTimeline.play();
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    private void doLogin() {
        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) { showResult("Veuillez entrer votre email.", false); stopPreview(); btnStart.setDisable(false); return; }
        User user = userService.trouverParEmail(email);
        if (user == null) { showResult("Aucun compte trouve.", false); stopPreview(); btnStart.setDisable(false); return; }
        if (!FaceIdService.hasFaceRegistered(user.getId())) {
            showResult("Aucun visage enregistre. Activez Face ID dans votre profil.", false);
            stopPreview(); btnStart.setDisable(false); return;
        }
        if (user.isIsSuspended()) { showResult("Compte suspendu.", false); stopPreview(); btnStart.setDisable(false); return; }

        setStatus("Regardez la camera...");
        final User finalUser = user;
        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.authenticateFace(finalUser.getId());
            Platform.runLater(() -> {
                stopPreview();
                if (result.success()) {
                    showResult("Bienvenue " + finalUser.getPrenom() + " !", true);
                    JwtManager.login(finalUser);
                    ActivityApiClient.logAsync(finalUser.getId(), "user.login",
                        java.util.Map.of("method", "face_id", "email", finalUser.getEmail()));
                    new Timeline(new KeyFrame(Duration.millis(1500), ev -> {
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

    // ── REGISTER ──────────────────────────────────────────────────────────────

    private void monitorForRegistration() {
        AtomicInteger stable = new AtomicInteger(0);
        AtomicBoolean started = new AtomicBoolean(false);

        monitorTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            if (capturing.get()) return;
            if (faceDetected.get()) {
                if (stable.incrementAndGet() >= 5 && !started.get()) {
                    started.set(true);
                    runCountdown();
                }
            } else {
                stable.set(0);
                if (!started.get()) setStatus("Placez votre visage dans le cercle");
                else if (!capturing.get()) { started.set(false); setStatus("Visage perdu - replacez-vous"); }
            }
        }));
        monitorTimeline.setCycleCount(Timeline.INDEFINITE);
        monitorTimeline.play();
    }

    private void runCountdown() {
        countdown.set(3);
        setStatus("Restez immobile... 3");
        Timeline t = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            int v = countdown.decrementAndGet();
            if (v > 0) setStatus("Restez immobile... " + v);
            else { if (faceDetected.get()) doCapture(); else setStatus("Visage perdu - replacez-vous"); }
        }));
        t.setCycleCount(3);
        t.play();
    }

    private void doCapture() {
        capturing.set(true);
        stopPreview();
        if (monitorTimeline != null) { monitorTimeline.stop(); monitorTimeline = null; }
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);
        setStatus("Enregistrement en cours...");

        User user = JwtManager.getCurrentUser();
        if (user == null) { showResult("Session perdue.", false); return; }

        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.registerFace(user.getId(),
                pct -> Platform.runLater(() -> {
                    progressBar.setProgress(pct / 100.0);
                    setStatus("Enregistrement... " + pct + "%");
                })
            );
            Platform.runLater(() -> {
                progressBar.setVisible(false);
                progressBar.setManaged(false);
                if (result.success()) {
                    showResult("Face ID active avec succes !\nVous pouvez maintenant vous connecter avec votre visage.", true);
                    btnStart.setText("Fermer");
                    btnStart.setDisable(false);
                    btnStart.setOnAction(ev -> closeDialog());
                } else {
                    showResult(result.message(), false);
                    btnStart.setDisable(false);
                    capturing.set(false);
                }
            });
        });
    }

    private void stopPreview() {
        if (monitorTimeline != null) { monitorTimeline.stop(); monitorTimeline = null; }
        if (previewTimeline != null) { previewTimeline.stop(); previewTimeline = null; }
        faceDetected.set(false);
        Platform.runLater(() -> { cameraView.setImage(null); if (faceOverlay != null) faceOverlay.setVisible(false); });
        // Release the persistent camera on the Python server
        CompletableFuture.runAsync(FaceIdService::releaseCamera);
    }

    @FXML private void onCancel() { stopPreview(); closeDialog(); }
    private void closeDialog() { try { ((Stage) btnCancel.getScene().getWindow()).close(); } catch (Exception ignored) {} }
    private void setStatus(String t) { Platform.runLater(() -> { if (labelCameraStatus != null) labelCameraStatus.setText(t); }); }

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