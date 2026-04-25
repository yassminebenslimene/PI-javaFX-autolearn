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
import org.opencv.videoio.Videoio;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

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
    private VideoCapture camera;
    private Timeline cameraTimeline;
    private final UserService userService = new UserService();

    private final AtomicBoolean faceDetected   = new AtomicBoolean(false);
    private final AtomicBoolean capturing      = new AtomicBoolean(false);
    private final AtomicInteger countdownValue = new AtomicInteger(0);
    private Timeline monitorTimeline;

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
            showResult("OpenCV non disponible.", false);
            return;
        }
        btnStart.setDisable(true);
        setStatus("Ouverture de la camera...");

        if (mode == Mode.LOGIN) {
            startLogin();
        } else {
            startRegisterGuided();
        }
    }

    // ── LOGIN ─────────────────────────────────────────────────────────────────

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
            showResult("Aucun visage enregistre. Activez Face ID dans votre profil.", false);
            btnStart.setDisable(false);
            return;
        }
        if (user.isIsSuspended()) {
            showResult("Compte suspendu.", false);
            btnStart.setDisable(false);
            return;
        }

        final User finalUser = user;
        CompletableFuture.runAsync(() -> {
            openCamera(false);
            FaceIdService.FaceResult result = FaceIdService.authenticateFace(finalUser.getId());
            Platform.runLater(() -> {
                stopCamera();
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

    // ── REGISTER ──────────────────────────────────────────────────────────────

    private void startRegisterGuided() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showResult("Vous devez etre connecte.", false);
            btnStart.setDisable(false);
            return;
        }

        faceDetected.set(false);
        capturing.set(false);

        CompletableFuture.runAsync(() -> {
            openCamera(true);
            Platform.runLater(() -> {
                setStatus("Placez votre visage dans le cercle blanc");
                monitorFaceForRegistration(user);
            });
        });
    }

    private void monitorFaceForRegistration(User user) {
        AtomicInteger stableFrames = new AtomicInteger(0);
        AtomicBoolean countdownStarted = new AtomicBoolean(false);

        monitorTimeline = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            if (capturing.get()) return;

            if (faceDetected.get()) {
                int stable = stableFrames.incrementAndGet();
                if (stable >= 5 && !countdownStarted.get()) {
                    countdownStarted.set(true);
                    startCountdown(user);
                }
            } else {
                stableFrames.set(0);
                if (!countdownStarted.get()) {
                    setStatus("Placez votre visage dans le cercle blanc");
                } else if (!capturing.get()) {
                    countdownStarted.set(false);
                    setStatus("Visage perdu - replacez-vous dans le cercle");
                }
            }
        }));
        monitorTimeline.setCycleCount(Timeline.INDEFINITE);
        monitorTimeline.play();
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
                    if (faceDetected.get()) {
                        startCapture(user);
                    } else {
                        setStatus("Visage perdu - replacez-vous dans le cercle");
                    }
                }
            })
        );
        countdown.setCycleCount(3);
        countdown.play();
    }

    private void startCapture(User user) {
        capturing.set(true);
        if (monitorTimeline != null) monitorTimeline.stop();

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
                stopCamera();
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

    // ── Camera ────────────────────────────────────────────────────────────────

    private void openCamera(boolean withGuidance) {
        try {
            // Hide status overlay
            Platform.runLater(() -> {
                if (statusOverlay != null) {
                    statusOverlay.setVisible(false);
                    statusOverlay.setManaged(false);
                }
            });

            // Try DirectShow first (more reliable on Windows, avoids MSMF issues)
            camera = new VideoCapture(0 + Videoio.CAP_DSHOW);
            if (!camera.isOpened()) {
                camera.release();
                camera = new VideoCapture(0);
            }

            if (!camera.isOpened()) {
                setStatus("Webcam non disponible");
                Platform.runLater(() -> {
                    if (statusOverlay != null) {
                        statusOverlay.setVisible(true);
                        statusOverlay.setManaged(true);
                    }
                    btnStart.setDisable(false);
                });
                return;
            }

            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);

            // Small stabilization delay
            Thread.sleep(300);

            CascadeClassifier det = withGuidance ? FaceIdService.getDetector() : null;

            // Start preview on JavaFX timeline
            Platform.runLater(() -> {
                cameraTimeline = new Timeline(new KeyFrame(Duration.millis(66), e -> {
                    if (camera != null && camera.isOpened()) {
                        Mat frame = new Mat();
                        if (camera.read(frame) && !frame.empty()) {
                            Mat display = withGuidance
                                ? drawGuidedOverlay(frame, det)
                                : drawSimpleOverlay(frame, det);
                            Image img = matToImage(display);
                            if (img != null) cameraView.setImage(img);
                        }
                    }
                }));
                cameraTimeline.setCycleCount(Timeline.INDEFINITE);
                cameraTimeline.play();
            });

        } catch (Exception e) {
            System.err.println("[FaceID] Camera error: " + e.getMessage());
            setStatus("Erreur camera: " + e.getMessage());
            Platform.runLater(() -> btnStart.setDisable(false));
        }
    }

    private Mat drawGuidedOverlay(Mat frame, CascadeClassifier det) {
        try {
            Mat display = frame.clone();
            int cx = frame.cols() / 2;
            int cy = frame.rows() / 2;
            int rx = 110;
            int ry = 135;

            boolean detected = false;

            if (det != null && !det.empty()) {
                Mat gray = new Mat();
                Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
                MatOfRect faces = new MatOfRect();
                det.detectMultiScale(gray, faces, 1.05, 4, 0, new Size(60, 60), new Size(400, 400));

                for (Rect r : faces.toArray()) {
                    int faceCx = r.x + r.width / 2;
                    int faceCy = r.y + r.height / 2;
                    double dx = (double)(faceCx - cx) / rx;
                    double dy = (double)(faceCy - cy) / ry;
                    if (dx * dx + dy * dy <= 1.0) {
                        detected = true;
                        Imgproc.rectangle(display,
                            new Point(r.x, r.y),
                            new Point(r.x + r.width, r.y + r.height),
                            new Scalar(0, 220, 100), 2);
                    }
                }
            }

            faceDetected.set(detected);

            // Draw guide ellipse
            Scalar color = detected ? new Scalar(0, 220, 100) : new Scalar(200, 200, 200);
            Imgproc.ellipse(display, new Point(cx, cy), new Size(rx, ry), 0, 0, 360, color, 2);

            // Instruction text at bottom
            String msg = detected
                ? (capturing.get() ? "Enregistrement..." : "Parfait ! Restez immobile")
                : "Centrez votre visage dans le cercle";

            int textY = frame.rows() - 12;
            Imgproc.rectangle(display, new Point(0, textY - 22), new Point(frame.cols(), frame.rows()), new Scalar(0, 0, 0), -1);
            Imgproc.putText(display, msg, new Point(10, textY), Imgproc.FONT_HERSHEY_SIMPLEX, 0.55, color, 1);

            return display;
        } catch (Exception e) { return frame; }
    }

    private Mat drawSimpleOverlay(Mat frame, CascadeClassifier det) {
        try {
            if (det == null || det.empty()) return frame;
            Mat display = frame.clone();
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfRect faces = new MatOfRect();
            det.detectMultiScale(gray, faces, 1.05, 4, 0, new Size(60, 60), new Size(400, 400));
            for (Rect r : faces.toArray()) {
                Imgproc.rectangle(display, new Point(r.x, r.y), new Point(r.x + r.width, r.y + r.height), new Scalar(0, 220, 100), 2);
            }
            return display;
        } catch (Exception e) { return frame; }
    }

    private void stopCamera() {
        if (monitorTimeline != null) { monitorTimeline.stop(); monitorTimeline = null; }
        if (cameraTimeline != null) { cameraTimeline.stop(); cameraTimeline = null; }
        if (camera != null) { camera.release(); camera = null; }
        faceDetected.set(false);
        Platform.runLater(() -> {
            cameraView.setImage(null);
            if (faceOverlay != null) faceOverlay.setVisible(false);
        });
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @FXML
    private void onCancel() {
        stopCamera();
        closeDialog();
    }

    private void closeDialog() {
        try { ((Stage) btnCancel.getScene().getWindow()).close(); }
        catch (Exception ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String text) {
        Platform.runLater(() -> { if (labelCameraStatus != null) labelCameraStatus.setText(text); });
    }

    private void showResult(String message, boolean success) {
        Platform.runLater(() -> {
            if (labelResult == null) return;
            labelResult.setText(message);
            labelResult.setStyle(success
                ? "-fx-font-size:12;-fx-font-weight:600;-fx-background-radius:8;-fx-padding:10 14 10 14;-fx-border-radius:8;-fx-border-width:1;-fx-text-fill:#34d399;-fx-background-color:rgba(5,150,105,0.15);-fx-border-color:rgba(5,150,105,0.3);"
                : "-fx-font-size:12;-fx-font-weight:600;-fx-background-radius:8;-fx-padding:10 14 10 14;-fx-border-radius:8;-fx-border-width:1;-fx-text-fill:#f85149;-fx-background-color:rgba(248,81,73,0.12);-fx-border-color:rgba(248,81,73,0.3);"
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
            img.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getByteRgbInstance(), data, 0, w * ch);
            return img;
        } catch (Exception e) { return null; }
    }
}