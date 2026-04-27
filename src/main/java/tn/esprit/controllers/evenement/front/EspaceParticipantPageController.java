package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import tn.esprit.entities.Evenement;
import tn.esprit.session.SessionManager;

/**
 * Interface dédiée à l'Espace Participant.
 * Affiche les fonctionnalités (Café, Jeux, Réservation, Emprunt) comme containers animés.
 */
public class EspaceParticipantPageController {

    @FXML private VBox containerBox;
    private Evenement evenement;

    @FXML
    public void initialize() {
        // Sera appelé après le chargement du FXML
    }

    public void setData(Evenement ev) {
        this.evenement = ev;
        buildContainers();
    }

    private void buildContainers() {
        containerBox.getChildren().clear();

        // Titre principal
        Label titleLabel = new Label("✨ Espace Participant ✨");
        titleLabel.setStyle(
            "-fx-font-size:28; " +
            "-fx-font-weight:bold; " +
            "-fx-text-fill:#D4A96A; " +
            "-fx-effect:dropshadow(gaussian, rgba(212,169,106,0.5), 15, 0, 0, 3);"
        );
        titleLabel.setAlignment(Pos.CENTER);
        containerBox.getChildren().add(titleLabel);

        // Grille 2x2 pour les 4 fonctionnalités
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setAlignment(Pos.CENTER);

        // Colonne 1 et 2 avec même largeur
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Conteneur 1 : Coin Café ☕
        VBox cafeContainer = buildContainer(
            "☕ Coin Café",
            "Votre pause café virtuelle",
            "#c0392b",
            "#e74c3c",
            () -> CoinCafeController.show(containerBox.getScene().getWindow())
        );
        grid.add(cafeContainer, 0, 0);

        // Conteneur 2 : Espace Jeux 🎮
        VBox jeuxContainer = buildContainer(
            "🎮 Espace Jeux",
            "Jeux amusants et relaxants",
            "#7c3aed",
            "#c44dff",
            () -> EspaceJeuxController.show(containerBox.getScene().getWindow())
        );
        grid.add(jeuxContainer, 1, 0);

        // Conteneur 3 : Réservation de Tables 🪑
        VBox reservationContainer = buildContainer(
            "🪑 Réservation Tables",
            "Réservez votre place",
            "#8B6614",
            "#D4A96A",
            () -> {
                try { tn.esprit.MainApp.showSalleReservation(evenement, null); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        );
        grid.add(reservationContainer, 0, 1);

        // Conteneur 4 : Emprunt Matériel 🔧
        VBox empruntContainer = buildContainer(
            "🔧 Emprunt Matériel",
            "Emprunter du matériel",
            "#22c55e",
            "#4ade80",
            () -> EmpruntMaterielController.show(evenement, containerBox.getScene().getWindow())
        );
        grid.add(empruntContainer, 1, 1);

        containerBox.getChildren().add(grid);

        // Animer l'apparition des containers
        animateContainers();
    }

    private VBox buildContainer(String title, String subtitle, String colorDark, String colorLight, Runnable onAction) {
        VBox container = new VBox(12);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(24));
        container.setStyle(
            "-fx-background-color:linear-gradient(to bottom right," + colorDark + "," + colorLight + "); " +
            "-fx-background-radius:20; " +
            "-fx-border-color:#ffffff44; " +
            "-fx-border-radius:20; " +
            "-fx-border-width:2; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),20,0,0,8); " +
            "-fx-cursor:hand;"
        );
        container.setPrefHeight(200);

        // Titre
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size:20; " +
            "-fx-font-weight:bold; " +
            "-fx-text-fill:white;"
        );

        // Sous-titre
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle(
            "-fx-font-size:13; " +
            "-fx-text-fill:rgba(255,255,255,0.85); " +
            "-fx-font-style:italic;"
        );

        // Bouton d'action
        Button actionBtn = new Button("Accéder →");
        actionBtn.setStyle(
            "-fx-background-color:rgba(255,255,255,0.25); " +
            "-fx-text-fill:white; " +
            "-fx-font-size:12; " +
            "-fx-font-weight:bold; " +
            "-fx-padding:8 20 8 20; " +
            "-fx-background-radius:20; " +
            "-fx-cursor:hand; " +
            "-fx-border-width:0;"
        );
        actionBtn.setOnAction(e -> onAction.run());

        container.getChildren().addAll(titleLabel, subtitleLabel, actionBtn);

        // Hover effect
        container.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), container);
            st.setToX(1.05);
            st.setToY(1.05);
            st.play();
        });

        container.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), container);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });

        return container;
    }

    private void animateContainers() {
        for (int i = 0; i < containerBox.getChildren().size(); i++) {
            javafx.scene.Node node = containerBox.getChildren().get(i);
            if (node instanceof GridPane) {
                for (javafx.scene.Node child : ((GridPane) node).getChildren()) {
                    child.setOpacity(0);
                    child.setTranslateY(30);

                    FadeTransition ft = new FadeTransition(Duration.millis(400), child);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.setDelay(Duration.millis(GridPane.getRowIndex(child) * 150 + GridPane.getColumnIndex(child) * 100));

                    TranslateTransition tt = new TranslateTransition(Duration.millis(400), child);
                    tt.setFromY(30);
                    tt.setToY(0);
                    tt.setDelay(Duration.millis(GridPane.getRowIndex(child) * 150 + GridPane.getColumnIndex(child) * 100));
                    tt.setInterpolator(Interpolator.EASE_OUT);

                    new ParallelTransition(ft, tt).play();
                }
            }
        }
    }
}
