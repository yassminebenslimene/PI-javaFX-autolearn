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

        // ═══════════════════════════════════════════════════════════════════════════
        // HEADER SECTION - Professionnel et accueillant
        // ═══════════════════════════════════════════════════════════════════════════
        VBox headerBox = new VBox(12);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(40, 30, 35, 30));
        headerBox.setStyle(
            "-fx-background-color:#4a3db8; " +
            "-fx-background-radius:0 0 30 30;"
        );
        
        // Titre principal avec animation
        Label titleLabel = new Label("✨ Espace Participant ✨");
        titleLabel.setStyle(
            "-fx-font-size:36; " +
            "-fx-font-weight:bold; " +
            "-fx-text-fill:white; " +
            "-fx-effect:dropshadow(gaussian, rgba(102,126,234,0.2), 8, 0, 0, 2);"
        );
        titleLabel.setAlignment(Pos.CENTER);
        
        // Sous-titre accueillant
        Label subtitleLabel = new Label("Bienvenue dans ton espace personnel ! 🎉");
        subtitleLabel.setStyle(
            "-fx-font-size:14; " +
            "-fx-text-fill:white; " +
            "-fx-font-style:italic; " +
            "-fx-font-weight:500;"
        );
        subtitleLabel.setAlignment(Pos.CENTER);
        
        // Description courte
        Label descLabel = new Label("Explore les activités, joue, détends-toi et profite de chaque moment");
        descLabel.setStyle(
            "-fx-font-size:12; " +
            "-fx-text-fill:rgba(255,255,255,0.85);"
        );
        descLabel.setAlignment(Pos.CENTER);
        
        headerBox.getChildren().addAll(titleLabel, subtitleLabel, descLabel);
        containerBox.getChildren().add(headerBox);

        // ═══════════════════════════════════════════════════════════════════════════
        // MAIN CONTENT - Grille 2x3 avec design moderne
        // ═══════════════════════════════════════════════════════════════════════════
        VBox contentBox = new VBox(0);
        contentBox.setPadding(new Insets(35, 25, 40, 25));
        contentBox.setStyle("-fx-background-color:#5b4fcf;");
        
        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(22);
        grid.setAlignment(Pos.TOP_CENTER);

        // 2 colonnes égales
        for (int i = 0; i < 2; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setHgrow(Priority.ALWAYS);
            col.setPercentWidth(50);
            grid.getColumnConstraints().add(col);
        }

        // Conteneur 1 : Coin Café ☕
        VBox cafeContainer = buildContainer(
            "☕", "Coin Café",
            "Votre pause café virtuelle",
            "#c0392b", "#e74c3c", "#fff3e0",
            () -> CoinCafeController.show(containerBox.getScene().getWindow())
        );
        grid.add(cafeContainer, 0, 0);

        // Conteneur 2 : Espace Jeux 🎮
        VBox jeuxContainer = buildContainer(
            "🎮", "Espace Jeux",
            "Jeux amusants et relaxants",
            "#7c3aed", "#c44dff", "#f3e5f5",
            () -> EspaceJeuxController.show(containerBox.getScene().getWindow())
        );
        grid.add(jeuxContainer, 1, 0);

        // Conteneur 3 : Menu Déjeuner 🍽️
        VBox menuContainer = buildContainer(
            "🍽️", "Menu Déjeuner",
            "Découvrez nos plats délicieux",
            "#f39c12", "#f1c40f", "#fff8e1",
            () -> MenuDejeunerController.show(containerBox.getScene().getWindow())
        );
        grid.add(menuContainer, 0, 1);

        // Conteneur 4 : Vending Machine 🛒
        VBox vendingContainer = buildContainer(
            "🛒", "Vending Machine",
            "Snacks et boissons variés",
            "#e74c3c", "#ff6b6b", "#ffebee",
            () -> VendingMachineController.show(evenement, containerBox.getScene().getWindow())
        );
        grid.add(vendingContainer, 1, 1);

        // Conteneur 5 : Emprunt Matériel 🔧
        VBox empruntContainer = buildContainer(
            "🔧", "Emprunt Matériel",
            "Emprunter du matériel facilement",
            "#22c55e", "#4ade80", "#e8f5e9",
            () -> EmpruntMaterielController.show(evenement, containerBox.getScene().getWindow())
        );
        grid.add(empruntContainer, 0, 2);

        // Conteneur 6 : Réservation Salle 📍 (optionnel)
        VBox reservationContainer = buildContainer(
            "📍", "Réservation Salle",
            "Réserve ta place facilement",
            "#2196f3", "#03a9f4", "#e3f2fd",
            () -> FrontNavHelper.goSalleReservation(evenement)
        );
        grid.add(reservationContainer, 1, 2);

        // Conteneur 7 : Brainstorming IA 💡
        VBox brainstormingContainer = buildContainer(
            "💡", "Espace de Brainstorming",
            "Hackathon & idées innovantes",
            "#7c3aed", "#4f46e5", "#ede9fe",
            () -> BrainstormingController.show(containerBox.getScene().getWindow())
        );
        grid.add(brainstormingContainer, 0, 3);

        contentBox.getChildren().add(grid);
        containerBox.getChildren().add(contentBox);

        // Animer l'apparition
        animateContainers();
    }

    private VBox buildContainer(String emoji, String title, String subtitle, String colorDark, String colorLight, String bgLight, Runnable onAction) {
        VBox container = new VBox(14);
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(24, 20, 24, 20));
        
        // Assombrir légèrement les couleurs de fond
        String darkerBg = darkenColor(bgLight, 0.08);
        
        container.setStyle(
            "-fx-background-color:" + darkerBg + "; " +
            "-fx-background-radius:18; " +
            "-fx-border-color:" + colorLight + "88; " +
            "-fx-border-radius:18; " +
            "-fx-border-width:2; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),12,0,0,4);"
        );
        container.setPrefHeight(240);
        container.setCursor(javafx.scene.Cursor.HAND);

        // Emoji grand et coloré
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle(
            "-fx-font-size:48; " +
            "-fx-background-color:" + colorDark + "; " +
            "-fx-background-radius:50%; " +
            "-fx-padding:16 18 16 18; " +
            "-fx-min-width:80; " +
            "-fx-min-height:80; " +
            "-fx-alignment:CENTER; " +
            "-fx-effect:dropshadow(gaussian," + colorDark + "88,10,0,0,3);"
        );

        // Titre
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size:18; " +
            "-fx-font-weight:bold; " +
            "-fx-text-fill:#1a1a1a;"
        );
        titleLabel.setAlignment(Pos.CENTER);

        // Sous-titre
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle(
            "-fx-font-size:12; " +
            "-fx-text-fill:#666666; " +
            "-fx-font-style:italic;"
        );
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setMaxWidth(180);

        // Spacer pour pousser le bouton en bas
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Bouton d'action - design moderne
        Button actionBtn = new Button("Accéder →");
        actionBtn.setStyle(
            "-fx-background-color:#7a6ad8; " +
            "-fx-text-fill:white; " +
            "-fx-font-size:13; " +
            "-fx-font-weight:bold; " +
            "-fx-padding:11 26 11 26; " +
            "-fx-background-radius:22; " +
            "-fx-cursor:hand; " +
            "-fx-border-width:0; " +
            "-fx-effect:dropshadow(gaussian,rgba(122,106,216,0.5),10,0,0,3);"
        );
        actionBtn.setOnAction(e -> onAction.run());

        container.getChildren().addAll(emojiLabel, titleLabel, subtitleLabel, spacer, actionBtn);

        // Animations au survol
        container.setOnMouseEntered(e -> {
            // Agrandissement du conteneur
            ScaleTransition st = new ScaleTransition(Duration.millis(200), container);
            st.setToX(1.04);
            st.setToY(1.04);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();

            // Changement de style
            container.setStyle(
                "-fx-background-color:" + colorLight + "15; " +
                "-fx-background-radius:18; " +
                "-fx-border-color:" + colorLight + "; " +
                "-fx-border-radius:18; " +
                "-fx-border-width:2.5; " +
                "-fx-effect:dropshadow(gaussian," + colorLight + "99,16,0,0,6);"
            );

            // Animation de l'emoji
            ScaleTransition emojiScale = new ScaleTransition(Duration.millis(200), emojiLabel);
            emojiScale.setToX(1.12);
            emojiScale.setToY(1.12);
            emojiScale.setInterpolator(Interpolator.EASE_OUT);
            emojiScale.play();
        });

        container.setOnMouseExited(e -> {
            // Retour à la taille normale
            ScaleTransition st = new ScaleTransition(Duration.millis(200), container);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();

            // Retour au style normal
            container.setStyle(
                "-fx-background-color:" + darkerBg + "; " +
                "-fx-background-radius:18; " +
                "-fx-border-color:" + colorLight + "88; " +
                "-fx-border-radius:18; " +
                "-fx-border-width:2; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),12,0,0,4);"
            );

            // Retour de l'emoji
            ScaleTransition emojiScale = new ScaleTransition(Duration.millis(200), emojiLabel);
            emojiScale.setToX(1.0);
            emojiScale.setToY(1.0);
            emojiScale.setInterpolator(Interpolator.EASE_OUT);
            emojiScale.play();
        });

        // Animation au clic
        container.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), container);
            st.setToX(0.98);
            st.setToY(0.98);
            st.play();
        });

        container.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), container);
            st.setToX(1.04);
            st.setToY(1.04);
            st.play();
        });

        return container;
    }

    // Utilitaire pour assombrir les couleurs
    private static String darkenColor(String hexColor, double factor) {
        try {
            String hex = hexColor.replace("#", "");
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            
            r = Math.max(0, (int)(r * (1 - factor)));
            g = Math.max(0, (int)(g * (1 - factor)));
            b = Math.max(0, (int)(b * (1 - factor)));
            
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
    }

    private void animateContainers() {
        for (int i = 0; i < containerBox.getChildren().size(); i++) {
            javafx.scene.Node node = containerBox.getChildren().get(i);
            if (node instanceof GridPane) {
                int delay = 0;
                for (javafx.scene.Node child : ((GridPane) node).getChildren()) {
                    child.setOpacity(0);
                    child.setTranslateY(40);

                    FadeTransition ft = new FadeTransition(Duration.millis(500), child);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.setDelay(Duration.millis(delay));
                    ft.setInterpolator(Interpolator.EASE_OUT);

                    TranslateTransition tt = new TranslateTransition(Duration.millis(500), child);
                    tt.setFromY(40);
                    tt.setToY(0);
                    tt.setDelay(Duration.millis(delay));
                    tt.setInterpolator(Interpolator.EASE_OUT);

                    new ParallelTransition(ft, tt).play();
                    delay += 80;
                }
            }
        }
    }
}
