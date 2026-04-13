# Écran de Chargement "Chargement du Quiz..." — JavaFX

## Ce qu'on voit dans la capture

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│                                                         │
│                  ┌──────┬──────┐                        │
│                  │ BLEU │ VERT │   ← Logo 4 carrés      │
│                  ├──────┼──────┤      animé             │
│                  │ROUGE │ORANGE│                        │
│                  └──────┴──────┘                        │
│                                                         │
│              Chargement du Quiz...                      │
│           Quiz - Loops and Iterations                   │
│                                                         │
│  Fond : dégradé violet-bleu (#667eea → #764ba2)         │
└─────────────────────────────────────────────────────────┘
```

---

## Structure des fichiers JavaFX à créer

```
src/
├── controller/
│   └── QuizLoadingController.java
├── view/
│   └── quiz-loading.fxml
└── styles/
    └── quiz-loading.css
```

---

## 1. Le fichier FXML — quiz-loading.fxml

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.layout.*?>
<?import javafx.scene.control.*?>
<?import javafx.scene.shape.*?>
<?import javafx.geometry.*?>

<StackPane xmlns:fx="http://javafx.com/fxml"
           fx:controller="controller.QuizLoadingController"
           fx:id="rootPane"
           styleClass="loading-root"
           stylesheets="@../styles/quiz-loading.css">

    <!-- Fond dégradé violet-bleu (géré en CSS) -->

    <VBox alignment="CENTER" spacing="20">

        <!-- Logo 4 carrés animés -->
        <StackPane fx:id="logoContainer" prefWidth="120" prefHeight="120">

            <!-- Carré BLEU — haut gauche -->
            <Rectangle fx:id="squareBlue"
                       width="55" height="55"
                       arcWidth="12" arcHeight="12"
                       fill="#3498db"
                       translateX="-28" translateY="-28"/>

            <!-- Carré VERT — haut droite -->
            <Rectangle fx:id="squareGreen"
                       width="55" height="55"
                       arcWidth="12" arcHeight="12"
                       fill="#2ecc71"
                       translateX="28" translateY="-28"/>

            <!-- Carré ROUGE — bas gauche -->
            <Rectangle fx:id="squareRed"
                       width="55" height="55"
                       arcWidth="12" arcHeight="12"
                       fill="#e74c3c"
                       translateX="-28" translateY="28"/>

            <!-- Carré ORANGE — bas droite -->
            <Rectangle fx:id="squareOrange"
                       width="55" height="55"
                       arcWidth="12" arcHeight="12"
                       fill="#f39c12"
                       translateX="28" translateY="28"/>

        </StackPane>

        <!-- Texte principal -->
        <Label fx:id="loadingLabel"
               text="Chargement du Quiz..."
               styleClass="loading-text"/>

        <!-- Sous-titre : nom du quiz -->
        <Label fx:id="quizNameLabel"
               text="Quiz - Loops and Iterations"
               styleClass="loading-subtitle"/>

    </VBox>

</StackPane>
```

---

## 2. Le CSS — quiz-loading.css

```css
/* Fond dégradé violet-bleu comme dans la capture */
.loading-root {
    -fx-background-color: linear-gradient(
        to bottom right,
        #667eea,   /* violet-bleu haut gauche */
        #764ba2    /* violet foncé bas droite */
    );
    -fx-min-width: 800px;
    -fx-min-height: 600px;
}

/* Texte principal blanc gras */
.loading-text {
    -fx-text-fill: white;
    -fx-font-size: 28px;
    -fx-font-weight: bold;
    -fx-font-family: "Inter", "Segoe UI", sans-serif;
}

/* Sous-titre blanc semi-transparent */
.loading-subtitle {
    -fx-text-fill: rgba(255, 255, 255, 0.8);
    -fx-font-size: 16px;
    -fx-font-family: "Inter", "Segoe UI", sans-serif;
}
```

---

## 3. Le Controller Java — QuizLoadingController.java

```java
package controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Quiz;

public class QuizLoadingController {

    // ── Éléments FXML ──────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private StackPane logoContainer;
    @FXML private Rectangle squareBlue;
    @FXML private Rectangle squareGreen;
    @FXML private Rectangle squareRed;
    @FXML private Rectangle squareOrange;
    @FXML private Label loadingLabel;
    @FXML private Label quizNameLabel;

    // Le quiz à passer (reçu depuis l'écran précédent)
    private Quiz quiz;

    // ── Initialisation automatique après chargement du FXML ────
    @FXML
    public void initialize() {
        startLogoAnimation();      // Lancer l'animation du logo
        startTextAnimation();      // Lancer l'animation du texte
        scheduleTransition();      // Après 2s → passer au quiz
    }

    // ── Recevoir le quiz depuis l'écran précédent ───────────────
    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        // Mettre à jour le sous-titre avec le vrai nom du quiz
        quizNameLabel.setText(quiz.getTitre());
    }

    // ══════════════════════════════════════════════════════════
    // ANIMATION 1 : Logo qui tourne (RotateTransition)
    // Les 4 carrés tournent ensemble en boucle infinie
    // ══════════════════════════════════════════════════════════
    private void startLogoAnimation() {

        // Rotation continue du groupe de 4 carrés
        RotateTransition rotate = new RotateTransition(
            Duration.seconds(1.5),  // 1 tour toutes les 1.5 secondes
            logoContainer           // sur le conteneur des 4 carrés
        );
        rotate.setByAngle(360);                    // tourner de 360°
        rotate.setCycleCount(Animation.INDEFINITE); // boucle infinie
        rotate.setInterpolator(Interpolator.EASE_BOTH); // accélération/décélération
        rotate.play();

        // Zoom pulsé sur chaque carré individuellement (effet "respiration")
        animateSquare(squareBlue,   0.0);   // commence immédiatement
        animateSquare(squareGreen,  0.2);   // décalé de 0.2s
        animateSquare(squareRed,    0.4);   // décalé de 0.4s
        animateSquare(squareOrange, 0.6);   // décalé de 0.6s
    }

    // Zoom pulsé sur un carré individuel
    private void animateSquare(Rectangle square, double delaySeconds) {
        ScaleTransition scale = new ScaleTransition(
            Duration.seconds(0.75),  // durée d'un pulse
            square
        );
        scale.setFromX(1.0);
        scale.setToX(1.15);          // agrandir de 15%
        scale.setFromY(1.0);
        scale.setToY(1.15);
        scale.setCycleCount(Animation.INDEFINITE);
        scale.setAutoReverse(true);  // revenir à la taille normale
        scale.setDelay(Duration.seconds(delaySeconds)); // décalage
        scale.play();
    }

    // ══════════════════════════════════════════════════════════
    // ANIMATION 2 : Texte qui clignote (FadeTransition)
    // "Chargement du Quiz..." apparaît/disparaît en boucle
    // ══════════════════════════════════════════════════════════
    private void startTextAnimation() {
        FadeTransition fade = new FadeTransition(
            Duration.seconds(1.5),  // durée d'un cycle
            loadingLabel
        );
        fade.setFromValue(1.0);                    // opaque
        fade.setToValue(0.5);                      // semi-transparent
        fade.setCycleCount(Animation.INDEFINITE);  // boucle infinie
        fade.setAutoReverse(true);                 // revenir à opaque
        fade.play();
    }

    // ══════════════════════════════════════════════════════════
    // TRANSITION : Après 2 secondes → passer à l'écran du quiz
    // ══════════════════════════════════════════════════════════
    private void scheduleTransition() {
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(event -> {
            // Fondu de sortie avant de changer d'écran
            FadeTransition fadeOut = new FadeTransition(
                Duration.millis(500),
                rootPane
            );
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> showQuizPassage()); // changer d'écran
            fadeOut.play();
        });
        pause.play();
    }

    // ── Charger l'écran de passage du quiz ─────────────────────
    private void showQuizPassage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/quiz-passage.fxml")
            );
            StackPane root = loader.load();

            // Passer le quiz au controller suivant
            QuizPassageController controller = loader.getController();
            controller.setQuiz(quiz);

            // Changer la scène
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 1024, 768);
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 4. Comment ouvrir cet écran depuis la liste des quiz

```java
// Dans QuizListController.java
// Quand l'utilisateur clique sur "Commencer le quiz"

@FXML
private void handleStartQuiz(Quiz quiz) {
    try {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/view/quiz-loading.fxml")
        );
        StackPane root = loader.load();

        // Passer le quiz sélectionné au controller de chargement
        QuizLoadingController controller = loader.getController();
        controller.setQuiz(quiz);  // ← transmettre le quiz

        // Afficher l'écran de chargement
        Stage stage = (Stage) startButton.getScene().getWindow();
        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.show();

    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

---

## 5. Résumé des animations utilisées

| Animation | Classe JavaFX | Effet visuel |
|-----------|--------------|--------------|
| Logo tourne | `RotateTransition` | 360° en 1.5s, boucle infinie |
| Carrés pulsent | `ScaleTransition` | zoom 1.0 → 1.15, autoReverse |
| Texte clignote | `FadeTransition` | opacité 1.0 → 0.5, autoReverse |
| Sortie de l'écran | `FadeTransition` | opacité 1.0 → 0.0 en 500ms |
| Délai 2 secondes | `PauseTransition` | attendre avant de changer d'écran |

---

## 6. Les couleurs exactes

| Élément | Couleur | Code HEX |
|---------|---------|----------|
| Fond haut-gauche | Violet-bleu | `#667eea` |
| Fond bas-droite | Violet foncé | `#764ba2` |
| Carré bleu | Bleu | `#3498db` |
| Carré vert | Vert | `#2ecc71` |
| Carré rouge | Rouge | `#e74c3c` |
| Carré orange | Orange | `#f39c12` |
| Texte principal | Blanc | `#ffffff` |
| Sous-titre | Blanc 80% | `rgba(255,255,255,0.8)` |
