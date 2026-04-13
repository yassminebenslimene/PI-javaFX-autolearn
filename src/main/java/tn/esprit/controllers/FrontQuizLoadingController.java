package tn.esprit.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Écran de chargement affiché entre "Commencer le quiz" et les questions.
 * Logo 4 carrés animé + texte "Chargement du Quiz..." pendant 2 secondes.
 */
public class FrontQuizLoadingController {

    @FXML private StackPane rootPane;
    @FXML private StackPane logoContainer;
    @FXML private Rectangle squareBlue;
    @FXML private Rectangle squareGreen;
    @FXML private Rectangle squareRed;
    @FXML private Rectangle squareOrange;
    @FXML private Label loadingLabel;
    @FXML private Label quizNameLabel;

    // Callback appelé après la fin du chargement (2s + fondu)
    private Runnable onFinished;

    @FXML
    public void initialize() {
        startLogoAnimation();
        startTextAnimation();
    }

    public void start(String quizTitre, Runnable onFinished) {
        this.onFinished = onFinished;
        quizNameLabel.setText(quizTitre);
        scheduleTransition();
    }

    // ── Logo : rotation + pulse sur chaque carré ──────────────────────────────
    private void startLogoAnimation() {
        RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), logoContainer);
        rotate.setByAngle(360);
        rotate.setCycleCount(Animation.INDEFINITE);
        rotate.setInterpolator(Interpolator.EASE_BOTH);
        rotate.play();

        animateSquare(squareBlue,   0.0);
        animateSquare(squareGreen,  0.2);
        animateSquare(squareRed,    0.4);
        animateSquare(squareOrange, 0.6);
    }

    private void animateSquare(Rectangle square, double delaySeconds) {
        ScaleTransition scale = new ScaleTransition(Duration.seconds(0.75), square);
        scale.setFromX(1.0); scale.setToX(1.15);
        scale.setFromY(1.0); scale.setToY(1.15);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);
        scale.setDelay(Duration.seconds(delaySeconds));
        scale.play();
    }

    // ── Texte : clignotement ──────────────────────────────────────────────────
    private void startTextAnimation() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1.2), loadingLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.4);
        fade.setCycleCount(Animation.INDEFINITE);
        fade.setAutoReverse(true);
        fade.play();
    }

    // ── Transition : 2s puis fondu de sortie ─────────────────────────────────
    private void scheduleTransition() {
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(400), rootPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> {
                if (onFinished != null) onFinished.run();
            });
            fadeOut.play();
        });
        pause.play();
    }
}
