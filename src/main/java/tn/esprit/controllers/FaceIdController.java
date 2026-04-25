package tn.esprit.controllers;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
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
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.awt.image.BufferedImage;
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
    private Webcam webcam;
    private Timeline previewTimeline;
    private Timeline monitorTimeline;
    private final UserService userService = new UserService();
    private CascadeClassifier detector;

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
        if (!FaceIdService.initOpenCV()) {
            showResult("OpenCV non disponible.", false);
            return;
        }
        btnStart.setDisable(true);
        detector = FaceIdService.getDetector();
        setStatus("Ouverture de la camera...");

        CompletableFuture.runAsync(() -> {
            webcam = FaceIdService.openWebcam();
            Platform.runLater(() -> {
                if (webcam == null || !webcam.isOpen()) {
                    showResult("Webcam non disponible.", false);
                    btnStart.setDisable(false);
                    return;
                }
                if (statusOverlay != null) {
                    statusOverlay.setVisible(false);
                    statusOverlay.setManaged(false);
                }
                startPreview();
                if (mode == Mode.LOGIN) doLogin();
                else {
                    setStatus("Placez votre visage dans le cercle");
                    monitorForRegistration();
                }
            });
        });
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    private void startPreview() {
        previewTimeline = new Timeline(new KeyFrame(Duration.millis(80), e -> {
            if (webcam == null || !webcam.isOpen()) return;
            BufferedImage img = webcam.getImage();
            if (img == null) return;

            // Convert to OpenCV for face detection overlay
            Mat frame = FaceIdService.bufferedImageToMat(img);
            if (frame != null && !frame.empty() && detector != null && !detector.empty()) {
                Mat display = drawOverlay(frame);
                javafx.scene.image.Image fxImg = matToFxImage(display);
                if (fxImg != null) cameraView.setImage(fxImg);
            } else {
                javafx.scene.image.Image fxImg = FaceIdService.bufferedImageToFxImage(img);
                if (fxImg != null) cameraView.setImage(fxImg);
            }
        }));
        previewTimeline.setCycleCount(Timeline.INDEFINITE);
        previewTimeline.play();
    }

    private Mat drawOverlay(Mat frame) {
        try {
            Mat display = frame.clone();
            int cx = frame.cols() / 2, cy = frame.rows() / 2;
            int rx = 110, ry = 135;

            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfRect faces = new MatOfRect();
            detector.detectMultiScale(gray, faces, 1.05, 4, 0, new Size(60, 60), new Size());

            boolean detected = false;
            for (Rect r : faces.toArray()) {
                int fcx = r.x + r.width / 2, fcy = r.y + r.height / 2;
                double dx = (double)(fcx - cx) / rx, dy = (double)(fcy - cy) / ry;
                if (dx * dx + dy * dy <= 1.0) {
                    detected = true;
                    Imgproc.rectangle(display, new Point(r.x, r.y),
                        new Point(r.x + r.width, r.y + r.height), new Scalar(0, 220, 100), 2);
                }
            }
            faceDetected.set(detected);

            Scalar color = detected ? new Scalar(0, 220, 100) : new Scalar(200, 200, 200);
            Imgproc.ellipse(display, new Point(cx, cy), new Size(rx, ry), 0, 0, 360, color, 2);

            String msg = detected
                ? (capturing.get() ? "Enregistrement..." : "Parfait ! Restez immobile")
                : "Centrez votre visage dans le cercle";
            int ty = frame.rows() - 12;
            Imgproc.rectangle(display, new Point(0, ty - 22), new Point(frame.cols(), frame.rows()), new Scalar(0, 0, 0), -1);
            Imgproc.putText(display, msg, new Point(10, ty), Imgproc.FONT_HERSHEY_SIMPLEX, 0.55, color, 1);
            return display;
        } catch (Exception e) { return frame; }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

    private void doLogin() {
        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) { showResult("Veuillez entrer votre email.", false); stopCamera(); btnStart.setDisable(false); return; }
        User user = userService.trouverParEmail(email);
        if (user == null) { showResult("Aucun compte trouve.", false); stopCamera(); btnStart.setDisable(false); return; }
        if (!FaceIdService.hasFaceRegistered(user.getId())) {
            showResult("Aucun visage enregistre. Activez Face ID dans votre profil.", false);
            stopCamera(); btnStart.setDisable(false); return;
        }
        if (user.isIsSuspended()) { showResult("Compte suspendu.", false); stopCamera(); btnStart.setDisable(false); return; }

        setStatus("Regardez la camera...");
        final User finalUser = user;
        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.authenticateFace(finalUser.getId());
            Platform.runLater(() -> {
                stopCamera();
                if (result.success()) {
                    showResult("Bienvenue " + finalUser.getPrenom() + " !", true);
                    SessionManager.login(finalUser);
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
        if (monitorTimeline != null) { monitorTimeline.stop(); monitorTimeline = null; }
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        progressBar.setProgress(0);
        setStatus("Enregistrement en cours...");

        User user = SessionManager.getCurrentUser();
        if (user == null) { showResult("Session perdue.", false); return; }

        // Stop preview so webcam is free for registration
        if (previewTimeline != null) { previewTimeline.stop(); previewTimeline = null; }
        if (webcam != null) { webcam.close(); webcam = null; }

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

    // ── Stop ──────────────────────────────────────────────────────────────────

    private void stopCamera() {
        if (monitorTimeline != null) { monitorTimeline.stop(); monitorTimeline = null; }
        if (previewTimeline != null) { previewTimeline.stop(); previewTimeline = null; }
        if (webcam != null) { webcam.close(); webcam = null; }
        faceDetected.set(false);
        Platform.runLater(() -> { cameraView.setImage(null); if (faceOverlay != null) faceOverlay.setVisible(false); });
    }

    @FXML private void onCancel() { stopCamera(); closeDialog(); }
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

    private javafx.scene.image.Image matToFxImage(Mat mat) {
        try {
            Mat rgb = new Mat();
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);
            int w = rgb.cols(), h = rgb.rows(), ch = rgb.channels();
            byte[] data = new byte[w * h * ch];
            rgb.get(0, 0, data);
            javafx.scene.image.WritableImage img = new javafx.scene.image.WritableImage(w, h);
            img.getPixelWriter().setPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getByteRgbInstance(), data, 0, w * ch);
            return img;
        } catch (Exception e) { return null; }
    }
}