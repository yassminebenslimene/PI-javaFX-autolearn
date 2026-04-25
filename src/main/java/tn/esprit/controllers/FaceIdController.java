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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for Face ID dialog.
 *
 * Modes:
 *  - LOGIN:    User enters email → camera opens → face compared → login if match
 *  - REGISTER: User already logged in → camera opens → captures 20 photos → trains model
 */
public class FaceIdController {

    public enum Mode { LOGIN, REGISTER }

    @FXML private Label         labelSubtitle;
    @FXML private ImageView     cameraView;
    @FXML private Ellipse       faceOverlay;
    @FXML private VBox          statusOverlay;
    @FXML private Label         labelCameraStatus;
    @FXML private ProgressBar   progressBar;
    @FXML private Label         labelResult;
    @FXML private VBox          emailBox;
    @FXML private TextField     fieldEmail;
    @FXML private Button        btnStart;
    @FXML private Button        btnCancel;

    private Mode mode = Mode.LOGIN;
    private VideoCapture camera;
    private Timeline cameraTimeline;
    private boolean running = false;
    private final UserService userService = new UserService();

    // Callback when login succeeds
    private Runnable onLoginSuccess;

    // ── Setup ─────────────────────────────────────────────────────────────────

    public void setMode(Mode mode) {
        this.mode = mode;
        Platform.runLater(() -> {
            if (mode == Mode.REGISTER) {
                labelSubtitle.setText("Enregistrez votre visage pour une connexion rapide");
                btnStart.setText("Enregistrer mon visage");
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

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    public void prefillEmail(String email) {
        Platform.runLater(() -> {
            if (fieldEmail != null) fieldEmail.setText(email);
        });
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    @FXML
    private void onStart() {
        if (!FaceIdService.initOpenCV()) {
            showResult("OpenCV non disponible sur ce systeme.", false);
            return;
        }

        btnStart.setDisable(true);

        if (mode == Mode.LOGIN) {
            startLogin();
        } else {
            startRegister();
        }
    }

    // ── LOGIN flow ────────────────────────────────────────────────────────────

    private void startLogin() {
        String email = fieldEmail.getText().trim();
        if (email.isEmpty()) {
            showResult("Veuillez entrer votre adresse email.", false);
            btnStart.setDisable(false);
            return;
        }

        // Find user by email
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

        // Check if suspended
        if (user.isIsSuspended()) {
            showResult("Compte suspendu. Contactez autolearn66@gmail.com", false);
            btnStart.setDisable(false);
            return;
        }

        setStatus("Regardez la camera...");
        startCameraPreview();

        // Run face auth in background
        final User finalUser = user;
        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.authenticateFace(finalUser.getId());

            Platform.runLater(() -> {
                stopCameraPreview();
                if (result.success()) {
                    showResult("Bienvenue " + finalUser.getPrenom() + " ! Connexion reussie.", true);

                    // Log activity
                    ActivityApiClient.logAsync(finalUser.getId(), "user.login",
                        java.util.Map.of("method", "face_id", "email", finalUser.getEmail()));

                    // Login and navigate after 1.5s
                    SessionManager.login(finalUser);
                    Timeline delay = new Timeline(new KeyFrame(Duration.millis(1500), e -> {
                        closeDialog();
                        try {
                            if ("ADMIN".equals(finalUser.getRole())) MainApp.showBackoffice();
                            else MainApp.showFrontoffice();
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }));
                    delay.play();
                } else {
                    showResult(result.message(), false);
                    btnStart.setDisable(false);
                }
            });
        });
    }

    // ── REGISTER flow ─────────────────────────────────────────────────────────

    private void startRegister() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            showResult("Vous devez etre connecte pour enregistrer votre visage.", false);
            btnStart.setDisable(false);
            return;
        }

        setStatus("Placez votre visage dans le cadre...");
        startCameraPreview();

        // Show progress bar
        Platform.runLater(() -> {
            progressBar.setVisible(true);
            progressBar.setManaged(true);
            progressBar.setProgress(0);
        });

        CompletableFuture.runAsync(() -> {
            FaceIdService.FaceResult result = FaceIdService.registerFace(
                user.getId(),
                progress -> Platform.runLater(() -> {
                    progressBar.setProgress(progress / 100.0);
                    setStatus("Capture en cours... " + progress + "%");
                })
            );

            Platform.runLater(() -> {
                stopCameraPreview();
                progressBar.setVisible(false);
                progressBar.setManaged(false);

                if (result.success()) {
                    showResult("Visage enregistre avec succes ! Vous pouvez maintenant utiliser Face ID.", true);
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

    // ── Camera preview ────────────────────────────────────────────────────────

    private void startCameraPreview() {
        try {
            camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                setStatus("Webcam non disponible");
                return;
            }

            camera.set(3, 640);
            camera.set(4, 480);

            faceOverlay.setVisible(true);

            cameraTimeline = new Timeline(new KeyFrame(Duration.millis(40), e -> {
                if (camera != null && camera.isOpened()) {
                    Mat frame = new Mat();
                    if (camera.read(frame) && !frame.empty()) {
                        // Draw face detection rectangle on preview
                        Mat display = drawFaceOverlay(frame);
                        Platform.runLater(() -> cameraView.setImage(matToImage(display)));
                    }
                }
            }));
            cameraTimeline.setCycleCount(Timeline.INDEFINITE);
            cameraTimeline.play();
            running = true;

        } catch (Exception e) {
            System.err.println("[FaceID] Camera preview error: " + e.getMessage());
        }
    }

    /** Draw green rectangle around detected face in preview */
    private Mat drawFaceOverlay(Mat frame) {
        try {
            Mat gray = new Mat();
            org.opencv.imgproc.Imgproc.cvtColor(frame, gray, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY);
            org.opencv.imgproc.Imgproc.equalizeHist(gray, gray);

            org.opencv.objdetect.CascadeClassifier det = FaceIdService.getDetector();
            if (det != null && !det.empty()) {
                MatOfRect faces = new MatOfRect();
                det.detectMultiScale(gray, faces, 1.05, 4, 0,
                    new Size(60, 60), new Size(400, 400));

                Mat display = frame.clone();
                for (Rect r : faces.toArray()) {
                    // Green rectangle around face
                    org.opencv.imgproc.Imgproc.rectangle(display,
                        new Point(r.x, r.y),
                        new Point(r.x + r.width, r.y + r.height),
                        new Scalar(0, 255, 0), 2);
                    // Label
                    org.opencv.imgproc.Imgproc.putText(display, "Visage detecte",
                        new Point(r.x, r.y - 8),
                        org.opencv.imgproc.Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.5, new Scalar(0, 255, 0), 1);
                }
                return display;
            }
        } catch (Exception ignored) {}
        return frame;
    }

    private void stopCameraPreview() {
        running = false;
        if (cameraTimeline != null) {
            cameraTimeline.stop();
            cameraTimeline = null;
        }
        if (camera != null) {
            camera.release();
            camera = null;
        }
        faceOverlay.setVisible(false);
        Platform.runLater(() -> cameraView.setImage(null));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @FXML
    private void onCancel() {
        stopCameraPreview();
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setStatus(String text) {
        Platform.runLater(() -> labelCameraStatus.setText(text));
    }

    private void showResult(String message, boolean success) {
        Platform.runLater(() -> {
            labelResult.setText(message);
            labelResult.setStyle(
                "-fx-font-size:13; -fx-font-weight:600; -fx-background-radius:8;" +
                "-fx-padding:10 14 10 14; -fx-border-radius:8; -fx-border-width:1;" +
                (success
                    ? "-fx-text-fill:#065f46; -fx-background-color:#d1fae5; -fx-border-color:#86efac;"
                    : "-fx-text-fill:#991b1b; -fx-background-color:#fee2e2; -fx-border-color:#fca5a5;")
            );
            labelResult.setVisible(true);
            labelResult.setManaged(true);
            setStatus(success ? "Authentification reussie !" : "Echec de l'authentification");
        });
    }

    /**
     * Converts OpenCV Mat to JavaFX Image for display.
     */
    private Image matToImage(Mat mat) {
        try {
            Mat rgb = new Mat();
            Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_BGR2RGB);

            int width  = rgb.cols();
            int height = rgb.rows();
            int channels = rgb.channels();
            byte[] data = new byte[width * height * channels];
            rgb.get(0, 0, data);

            WritableImage image = new WritableImage(width, height);
            PixelWriter pw = image.getPixelWriter();
            pw.setPixels(0, 0, width, height,
                PixelFormat.getByteRgbInstance(), data, 0, width * channels);
            return image;
        } catch (Exception e) {
            return null;
        }
    }
}
