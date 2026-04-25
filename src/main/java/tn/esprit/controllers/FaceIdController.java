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
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Face ID Controller — Guided UX with real-time feedback.
 *
 * Registration flow:
 *  1. Camera opens → shows live preview
 *  2. Detects face → shows green overlay + "Visage détecté"
 *  3. 3-second countdown while face is held still
 *  4. Captures 30 photos automatically
 *  5. Shows progress bar + count
 *
 * Login flow:
 *  1. User enters email
 *  2. Camera opens → detects face
 *  3. Compares with stored data
 *  4. Shows result
 */
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
    private VideoCapture camera;
    private Timeline cameraTimeline;
    private final UserService userService = new UserService();

    // State for guided registration
    private final AtomicBoolean faceDetected   = new AtomicBoolean(false);
    private final AtomicBoolean capturing      = new AtomicBoolean(false);
    private final AtomicInteger countdownValue = new AtomicInteger(0);
    private Timeline countdownTimeline;

    // ── Setup ─────────────────────────────────────────────────────────────────

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

    // ── Start ─────────────────────────────────────────────────────────────────

    @FXML
    private void onStart() {
        if (!FaceIdService.initOpenCV()) {
            showResult("OpenCV non disponible sur ce systeme.", false);
            return;
        }
        btnStart.setDisable(true);
        setStatus("Ouverture de la camera...");

        // Run camera init in background to avoid UI freeze
        CompletableFuture.runAsync(() -> {
            if (mode == Mode.LOGIN) {
                // Validate email first on UI thread
                Platform.runLater(() -> startLogin());
            } else {
                Platform.runLater(() -> startRegisterGuided());
            }
        });
    }

    // ── LOGIN flow ────────────────────────────────────────────────────────────

    private void startLogin() {
        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) {
            showResult("Veuillez entrer votre adresse email.", false);
            btnStart.setDisable(false);
            return;
        }

        User user = userService.trouverParEmail(email);
        if (user == null) {
            showResult("Aucun compte trouve avec cet email.", false);
            btnStart.setDisable(false);
            return;
        }
        if (!FaceIdService.hasFaceRegistered(user.getId())) {
            showResult("Aucun visage enregistre pour ce compte.\nActivez Face ID dans votre profil.", false);
            btnStart.setDisable(false);
            return;
        }
        if (user.isIsSuspended()) {
            showResult("Compte suspendu. Contactez autolearn66@gmail.com", false);
            btnStart.setDisable(false);
            return;
        }

        setStatus("Regardez la camera...");

        final User finalUser = user;
        CompletableFuture.runAsync(() -> {
            startCameraPreview(false); // opens camera in background
            FaceIdService.FaceResult result = FaceIdService.authenticateFace(finalUser.getId());
            Platform.runLater(() -> {
                stopCameraPreview();
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

    // ── REGISTER flow — Guided ────────────────────────────────────────────────

    private void startRegisterGuided() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showResult("Vous devez etre connecte.", false);
            btnStart.setDisable(false);
            return;
        }

        faceDetected.set(false);
        capturing.set(false);

        setStatus("Ouverture de la camera...");

        // Open camera in background thread
        CompletableFuture.runAsync(() -> {
            startCameraPreview(true);
            // Start monitoring after camera is ready
            Platform.runLater(() -> {
                setStatus("Placez votre visage dans le cercle blanc");
                monitorFaceForRegistration(user);
            });
        });
    }

    /**
     * Monitors the camera feed for a stable face.
     * When face is detected for 1 second → starts 3-second countdown.
     * When countdown ends → starts capturing.
     */
    private void monitorFaceForRegistration(User user) {
        AtomicInteger stableFrames = new AtomicInteger(0);
        AtomicBoolean countdownStarted = new AtomicBoolean(false);

        // Check every 200ms if face is detected
        Timeline monitor = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            if (capturing.get()) return; // already capturing

            if (faceDetected.get()) {
                int stable = stableFrames.incrementAndGet();

                if (stable >= 5 && !countdownStarted.get()) {
                    // Face stable for 1 second → start countdown
                    countdownStarted.set(true);
                    startCountdown(user);
                }
            } else {
                // Face lost → reset
                stableFrames.set(0);
                if (!countdownStarted.get()) {
                    setStatus("Placez votre visage dans le cercle blanc");
                } else if (!capturing.get()) {
                    // Countdown was running but face lost
                    countdownStarted.set(false);
                    stopCountdown();
                    setStatus("Visage perdu — replacez-vous dans le cercle");
                }
            }
        }));
        monitor.setCycleCount(Timeline.INDEFINITE);
        monitor.play();

        // Store reference to stop later
        this.countdownTimeline = monitor;
    }

    private void startCountdown(User user) {
        countdownValue.set(3);
        setStatus("Restez immobile... 3");

        Timeline countdown = new Timeline(
            new KeyFrame(Duration.millis(1000), e -> {
                int val = countdownValue.decrementAndGet();
                if (val > 0) {
                    setStatus("Restez immobile... " + val);
                } else {
                    // Countdown done → start capture
                    if (faceDetected.get()) {
                        startCapture(user);
                    } else {
                        setStatus("Visage perdu — replacez-vous dans le cercle");
                    }
                }
            })
        );
        countdown.setCycleCount(3);
        countdown.play();
    }

    private void stopCountdown() {
        countdownValue.set(0);
    }

    private void startCapture(User user) {
        capturing.set(true);
        stopCountdown();

        Platform.runLater(() -> {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
            setStatus("Enregistrement en cours...");
        });

        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.registerFace(
                user.getId(),
                progress -> Platform.runLater(() -> {
                    progressBar.setProgress(progress / 100.0);
                    setStatus("Enregistrement... " + progress + "%");
                })
            );

            Platform.runLater(() -> {
                stopCameraPreview();
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

    // ── Camera preview ────────────────────────────────────────────────────────

    private void startCameraPreview(boolean withGuidance) {
        try {
            // Hide the status overlay once camera starts
            Platform.runLater(() -> {
                if (statusOverlay != null) {
                    statusOverlay.setVisible(false);
                    statusOverlay.setManaged(false);
                }
            });

            camera = new VideoCapture(0);

            // Wait up to 3 seconds for camera to initialize
            int waitMs = 0;
            while (!camera.isOpened() && waitMs < 3000) {
                Thread.sleep(200);
                waitMs += 200;
                if (!camera.isOpened()) {
                    camera.release();
                    camera = new VideoCapture(0);
                }
            }

            if (!camera.isOpened()) {
                setStatus("Webcam non disponible — verifiez qu elle n est pas utilisee par une autre application");
                Platform.runLater(() -> {
                    if (statusOverlay != null) {
                        statusOverlay.setVisible(true);
                        statusOverlay.setManaged(true);
                    }
                    btnStart.setDisable(false);
                });
                return;
            }

            camera.set(3, 640);
            camera.set(4, 480);

            // Warm up — read a few frames before showing
            Mat warmup = new Mat();
            for (int i = 0; i < 5; i++) {
                camera.read(warmup);
                Thread.sleep(100);
            }

            faceOverlay.setVisible(false); // we draw circle in OpenCV

            CascadeClassifier det = withGuidance ? FaceIdService.getDetector() : null;

            cameraTimeline = new Timeline(new KeyFrame(Duration.millis(66), e -> {
                if (camera != null && camera.isOpened()) {
                    Mat frame = new Mat();
                    if (camera.read(frame) && !frame.empty()) {
                        Mat display = withGuidance
                            ? drawGuidedOverlay(frame, det)
                            : drawSimpleOverlay(frame, det);
                        Image img = matToImage(display);
                        if (img != null) Platform.runLater(() -> cameraView.setImage(img));
                    }
                }
            }));
            cameraTimeline.setCycleCount(Timeline.INDEFINITE);
            cameraTimeline.play();

        } catch (Exception e) {
            System.err.println("[FaceID] Camera error: " + e.getMessage());
            setStatus("Erreur camera: " + e.getMessage());
            Platform.runLater(() -> btnStart.setDisable(false));
        }
    }

    /**
     * Guided overlay for registration:
     * - Green circle when face is inside the guide area
     * - Red circle when face is not detected or outside
     * - Updates faceDetected flag
     */
    private Mat drawGuidedOverlay(Mat frame, CascadeClassifier det) {
        try {
            Mat display = frame.clone();
            int cx = frame.cols() / 2;
            int cy = frame.rows() / 2;
            int rx = 110; // guide ellipse radius X
            int ry = 135; // guide ellipse radius Y

            boolean detected = false;

            if (det != null && !det.empty()) {
                Mat gray = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
                MatOfRect faces = new MatOfRect();
                det.detectMultiScale(gray, faces, 1.05, 4, 0, new Size(60, 60), new Size(400, 400));

                for (Rect r : faces.toArray()) {
                    int faceCx = r.x + r.width / 2;
                    int faceCy = r.y + r.height / 2;

                    // Check if face center is inside the guide ellipse
                    double dx = (double)(faceCx - cx) / rx;
                    double dy = (double)(faceCy - cy) / ry;
                    boolean inside = (dx * dx + dy * dy) <= 1.0;

                    if (inside) {
                        detected = true;
                        // Draw green face rectangle
                        Imgproc.rectangle(display,
                            new Point(r.x, r.y),
                            new Point(r.x + r.width, r.y + r.height),
                            new Scalar(0, 220, 100), 2);
                    }
                }
            }

            faceDetected.set(detected);

            // Draw guide ellipse — green if face detected, white if not
            Scalar ellipseColor = detected
                ? new Scalar(0, 220, 100)   // green
                : new Scalar(200, 200, 200); // white

            Imgproc.ellipse(display,
                new Point(cx, cy),
                new Size(rx, ry),
                0, 0, 360,
                ellipseColor, 2);

            // Draw instruction text at bottom
            String instruction = detected
                ? (capturing.get() ? "Enregistrement..." : "Parfait ! Restez immobile")
                : "Centrez votre visage dans le cercle";

            Scalar textColor = detected
                ? new Scalar(0, 220, 100)
                : new Scalar(200, 200, 200);

            // Background for text
            int textY = frame.rows() - 15;
            Imgproc.rectangle(display,
                new Point(0, textY - 20),
                new Point(frame.cols(), frame.rows()),
                new Scalar(0, 0, 0), -1);

            Imgproc.putText(display, instruction,
                new Point(10, textY),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.55, textColor, 1);

            return display;

        } catch (Exception e) {
            return frame;
        }
    }

    /**
     * Simple overlay for login — just shows face detection rectangle.
     */
    private Mat drawSimpleOverlay(Mat frame, CascadeClassifier det) {
        try {
            if (det == null || det.empty()) return frame;
            Mat display = frame.clone();
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfRect faces = new MatOfRect();
            det.detectMultiScale(gray, faces, 1.05, 4, 0, new Size(60, 60), new Size(400, 400));
            for (Rect r : faces.toArray()) {
                Imgproc.rectangle(display,
                    new Point(r.x, r.y),
                    new Point(r.x + r.width, r.y + r.height),
                    new Scalar(0, 220, 100), 2);
            }
            return display;
        } catch (Exception e) {
            return frame;
        }
    }

    private void stopCameraPreview() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        if (cameraTimeline != null) {
            cameraTimeline.stop();
            cameraTimeline = null;
        }
        if (camera != null) {
            camera.release();
            camera = null;
        }
        faceDetected.set(false);
        Platform.runLater(() -> {
            cameraView.setImage(null);
            faceOverlay.setVisible(false);
        });
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @FXML
    private void onCancel() {
        stopCameraPreview();
        closeDialog();
    }

    private void closeDialog() {
        try {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            stage.close();
        } catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String text) {
        Platform.runLater(() -> {
            if (labelCameraStatus != null) labelCameraStatus.setText(text);
        });
    }

    private void showResult(String message, boolean success) {
        Platform.runLater(() -> {
            if (labelResult == null) return;
            labelResult.setText(message);
            labelResult.setStyle(success
                ? "-fx-font-size:12; -fx-font-weight:600; -fx-background-radius:8;" +
                  "-fx-padding:10 14 10 14; -fx-border-radius:8; -fx-border-width:1;" +
                  "-fx-text-fill:#34d399; -fx-background-color:rgba(5,150,105,0.15);" +
                  "-fx-border-color:rgba(5,150,105,0.3);"
                : "-fx-font-size:12; -fx-font-weight:600; -fx-background-radius:8;" +
                  "-fx-padding:10 14 10 14; -fx-border-radius:8; -fx-border-width:1;" +
                  "-fx-text-fill:#f85149; -fx-background-color:rgba(248,81,73,0.12);" +
                  "-fx-border-color:rgba(248,81,73,0.3);"
            );
            labelResult.setVisible(true);
            labelResult.setManaged(true);
        });
    }

    private Image matToImage(Mat mat) {
        try {
            Mat rgb = new Mat();
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);
            int w = rgb.cols(), h = rgb.rows(), ch = rgb.channels();
            byte[] data = new byte[w * h * ch];
            rgb.get(0, 0, data);
            WritableImage img = new WritableImage(w, h);
            img.getPixelWriter().setPixels(0, 0, w, h,
                PixelFormat.getByteRgbInstance(), data, 0, w * ch);
            return img;
        } catch (Exception e) { return null; }
    }
}
