package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import tn.esprit.entities.Evenement;
import tn.esprit.session.SessionManager;

/**
 * Espace Participant Événement — hub principal.
 * Accessible uniquement quand computeStatus() == "En cours" et user connecté.
 * Contient 3 fonctionnalités : Vending Machine, Menu Déjeuner, Emprunt Matériel.
 * Zéro FXML, zéro DB, pattern identique à showDetailsModal().
 */
public class EspaceParticipantController {

    public static void show(Evenement ev, Window owner) {
        if (ev == null || owner == null) return;
        if (SessionManager.getCurrentUser() == null) return;

        double winW = owner.getWidth();
        double winH = owner.getHeight();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        VBox modal = buildHubModal(ev, dialog, winH);
        modal.setPrefWidth(580);
        modal.setMaxWidth(580);

        StackPane root = new StackPane(modal);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:rgba(0,0,0,0.62);");

        Runnable close = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> dialog.close());
            ft.play();
        };
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });

        Scene scene = new Scene(root, winW, winH);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) close.run();
        });
        dialog.setScene(scene);
        dialog.setX(owner.getX());
        dialog.setY(owner.getY());

        root.setOpacity(0);
        modal.setTranslateY(45);
        dialog.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(260), modal);
        slideUp.setFromY(45); slideUp.setToY(0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fadeIn, slideUp).play();
    }

    private static VBox buildHubModal(Evenement ev, Stage dialog, double winH) {
        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:white; -fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        // Header gradient violet
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
                + "-fx-background-radius:20 20 0 0;");

        VBox headerInfo = new VBox(4);
        HBox.setHgrow(headerInfo, Priority.ALWAYS);
        Label titreLbl = new Label("🎯  Espace Participant");
        titreLbl.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        Label evLbl = new Label(ev.getTitre());
        evLbl.setStyle("-fx-font-size:13; -fx-text-fill:rgba(255,255,255,0.85); -fx-font-weight:500;");
        evLbl.setWrapText(true);
        evLbl.setMaxWidth(420);
        headerInfo.getChildren().addAll(titreLbl, evLbl);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(headerInfo, closeBtn);

        // Body — 3 cards
        VBox body = new VBox(16);
        body.setPadding(new Insets(28, 28, 28, 28));
        body.setStyle("-fx-background-color:#f5f3ff;");

        Label subtitle = new Label("Que souhaitez-vous faire ?");
        subtitle.setStyle("-fx-font-size:14; -fx-font-weight:600; -fx-text-fill:#4a5568;");
        body.getChildren().add(subtitle);

        // Card 1 — Vending Machine
        VBox card1 = buildFeatureCard(
                "🎰", "Vending Machine",
                "Boissons & snacks gamifiés avec surprises",
                "#ff6b9d", "#fce7f3",
                () -> VendingMachineController.show(ev, dialog.getOwner())
        );

        // Card 2 — Menu Déjeuner
        VBox card2 = buildFeatureCard(
                "🍽️", "Menu & Pause Café",
                "Déjeuner et snacks de la pause café",
                "#667eea", "#ede9fe",
                () -> MenuDejeunerController.show(dialog.getOwner())
        );

        // Card 3 — Emprunt Matériel
        VBox card3 = buildFeatureCard(
                "🔌", "Emprunt de Matériel",
                "Chargeurs, câbles, projecteurs et plus",
                "#10b981", "#d1fae5",
                () -> EmpruntMaterielController.show(ev, dialog.getOwner())
        );

        // Animations séquentielles
        animateCard(card1, 0);
        animateCard(card2, 100);
        animateCard(card3, 200);

        body.getChildren().addAll(card1, card2, card3);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 18, 24));
        footer.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:0 0 20 20;"
                + "-fx-border-color:#eeeeee; -fx-border-width:1 0 0 0;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25;"
                + "-fx-border-color:#d0d0d0; -fx-border-radius:25; -fx-border-width:1.5;"
                + "-fx-cursor:hand;");
        fermerBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, body, footer);
        return modal;
    }

    private static VBox buildFeatureCard(String emoji, String titre, String desc,
                                          String accentColor, String bgColor, Runnable onClick) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20, 22, 20, 22));
        card.setStyle("-fx-background-color:white; -fx-background-radius:16;"
                + "-fx-border-color:" + bgColor + "; -fx-border-radius:16; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);"
                + "-fx-cursor:hand;");

        HBox topRow = new HBox(14);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Emoji dans cercle coloré
        Label emojiLbl = new Label(emoji);
        emojiLbl.setStyle("-fx-font-size:28; -fx-background-color:" + bgColor + ";"
                + "-fx-background-radius:50%; -fx-padding:12 14 12 14;"
                + "-fx-min-width:56; -fx-min-height:56; -fx-alignment:CENTER;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:12; -fx-text-fill:#6b7280;");
        descLbl.setWrapText(true);
        info.getChildren().addAll(titreLbl, descLbl);

        Label arrowLbl = new Label("→");
        arrowLbl.setStyle("-fx-font-size:20; -fx-font-weight:700; -fx-text-fill:" + accentColor + ";");

        topRow.getChildren().addAll(emojiLbl, info, arrowLbl);
        card.getChildren().add(topRow);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:" + bgColor + "22;"
                + "-fx-background-radius:16; -fx-border-color:" + accentColor + ";"
                + "-fx-border-radius:16; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian," + accentColor + "44,14,0,0,4);"
                + "-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:white;"
                + "-fx-background-radius:16; -fx-border-color:" + bgColor + ";"
                + "-fx-border-radius:16; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);"
                + "-fx-cursor:hand;"));

        card.setOnMouseClicked(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), card);
            st.setToX(0.97); st.setToY(0.97);
            st.setOnFinished(ev2 -> {
                ScaleTransition st2 = new ScaleTransition(Duration.millis(100), card);
                st2.setToX(1.0); st2.setToY(1.0);
                st2.setOnFinished(ev3 -> onClick.run());
                st2.play();
            });
            st.play();
        });

        return card;
    }

    private static void animateCard(VBox card, int delayMs) {
        card.setOpacity(0);
        card.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(350), card);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), card);
        tt.setFromY(20); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }
}
