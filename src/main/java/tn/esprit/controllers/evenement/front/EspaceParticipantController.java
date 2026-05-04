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
        modal.setPrefWidth(620);
        modal.setMaxWidth(620);

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
        scene.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) close.run(); });
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
        modal.setStyle("-fx-background-color:white; -fx-background-radius:24;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2); -fx-background-radius:24 24 0 0;");
        VBox headerInfo = new VBox(4);
        HBox.setHgrow(headerInfo, Priority.ALWAYS);
        Label titreLbl = new Label("\uD83C\uDFAF  Espace Participant");
        titreLbl.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        Label evLbl = new Label(ev.getTitre());
        evLbl.setStyle("-fx-font-size:13; -fx-text-fill:rgba(255,255,255,0.85); -fx-font-weight:500;");
        evLbl.setWrapText(true); evLbl.setMaxWidth(440);
        headerInfo.getChildren().addAll(titreLbl, evLbl);
        Button closeBtn = new Button("\u2715");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white; -fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%; -fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34; -fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(headerInfo, closeBtn);

        // Body — 3 grandes cards illustrées
        VBox body = new VBox(14);
        body.setPadding(new Insets(22, 22, 22, 22));
        body.setStyle("-fx-background-color:linear-gradient(to bottom,#f8f7ff,#f0ebff);");

        Label subtitle = new Label("Choisissez votre service \uD83D\uDC47");
        subtitle.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#5b21b6;");
        body.getChildren().add(subtitle);

        // Card 1 — Vending Machine (rose/magenta)
        VBox card1 = buildBigCard(
            "\uD83C\uDFB0",
            "Vending Machine",
            "Boissons & snacks",
            "Choisissez parmi nos produits frais !",
            "#ff6b9d", "#fce7f3", "#fff0f7",
            new String[]{"\uD83E\uDD64","\u2615","\uD83C\uDF4A","\uD83C\uDF6B"},
            () -> VendingMachineController.show(ev, dialog.getOwner())
        );

        // Card 2 — Menu Dejeuner (violet/bleu)
        VBox card2 = buildBigCard(
            "\uD83C\uDF7D\uFE0F",
            "Menu & Pause Cafe",
            "Dejeuner et snacks",
            "Decouvrez le menu du jour !",
            "#667eea", "#ede9fe", "#f5f3ff",
            new String[]{"\uD83E\uDD57","\uD83C\uDF55","\u2615","\uD83E\uDDC1"},
            () -> MenuDejeunerController.show(dialog.getOwner())
        );

        // Card 3 — Emprunt Materiel (vert/teal)
        VBox card3 = buildBigCard(
            "\uD83D\uDD0C",
            "Emprunt de Materiel",
            "Equipements disponibles",
            "Empruntez ce dont vous avez besoin !",
            "#10b981", "#d1fae5", "#f0fdf4",
            new String[]{"\uD83D\uDCBB","\uD83D\uDCF1","\uD83C\uDFA7","\uD83D\uDCF7"},
            () -> EmpruntMaterielController.show(ev, dialog.getOwner())
        );

        // Card 4 — Coin Café (rouge/orange chaud)
        VBox card4 = buildBigCard(
            "\u2615",
            "Coin Caf\u00e9",
            "Machine \u00e0 caf\u00e9 virtuelle",
            "Pr\u00e9parez votre caf\u00e9 pr\u00e9f\u00e9r\u00e9 ! \u2615\u2728",
            "#c0392b", "#fff3e0", "#fff8f0",
            new String[]{"\u2615","\uD83E\uDD5B","\uD83C\uDF6B","\u2728"},
            () -> CoinCafeController.show(dialog.getOwner())
        );

        // Card 5 — Espace Jeux (violet/rose festif)
        VBox card5 = buildBigCard(
            "\uD83C\uDFAE",
            "Espace Jeux",
            "Mini-jeux amusants",
            "Memory Cards & Candy Crush ! \uD83C\uDF89",
            "#7c3aed", "#f3e5f5", "#faf5ff",
            new String[]{"\uD83C\uDFAE","\uD83C\uDFB4","\uD83C\uDDE8","\uD83C\uDFC6"},
            () -> EspaceJeuxController.show(dialog.getOwner())
        );

        animateCard(card1, 0);
        animateCard(card2, 100);
        animateCard(card3, 200);
        animateCard(card4, 300);
        animateCard(card5, 400);

        // Card 6 — Brainstorming IA (violet foncé)
        VBox card6 = buildBigCard(
            "💡",
            "Brainstorming IA",
            "Hackathon & Innovation",
            "Générez des idées créatives avec l'IA !",
            "#7c3aed", "#ede9fe", "#f5f3ff",
            new String[]{"💡","🚀","🤖","🌱"},
            () -> BrainstormingController.show(dialog.getOwner())
        );
        animateCard(card6, 500);

        body.getChildren().addAll(card1, card2, card3, card4, card5, card6);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 18, 24));
        footer.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:0 0 24 24; -fx-border-color:#eeeeee; -fx-border-width:1 0 0 0;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:13; -fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25; -fx-border-color:#d0d0d0; -fx-border-radius:25; -fx-border-width:1.5; -fx-cursor:hand;");
        fermerBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, body, footer);
        return modal;
    }

    private static VBox buildBigCard(String mainEmoji, String titre, String sousTitre,
                                      String desc, String accent, String bgLight, String bgCard,
                                      String[] miniEmojis, Runnable onClick) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:" + bgCard + "; -fx-background-radius:20;"
                + "-fx-border-color:" + accent + "44; -fx-border-radius:20; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian," + accent + "33,12,0,0,4); -fx-cursor:hand;");

        // Bande coloree du haut avec illustration
        HBox topBand = new HBox(16);
        topBand.setAlignment(Pos.CENTER_LEFT);
        topBand.setPadding(new Insets(18, 20, 18, 20));
        topBand.setStyle("-fx-background-color:linear-gradient(to right," + accent + "22," + bgLight + "); -fx-background-radius:18 18 0 0;");

        // Grand emoji principal dans cercle avec animation
        Label mainEmojiLbl = new Label(mainEmoji);
        mainEmojiLbl.setStyle("-fx-font-size:44; -fx-background-color:" + accent + ";"
                + "-fx-background-radius:50%; -fx-padding:14 16 14 16;"
                + "-fx-min-width:80; -fx-min-height:80; -fx-alignment:CENTER;");
        
        // Animation de bounce sur l'emoji
        ScaleTransition bounce = new ScaleTransition(Duration.millis(1200), mainEmojiLbl);
        bounce.setFromX(1); bounce.setToX(1.1);
        bounce.setFromY(1); bounce.setToY(1.1);
        bounce.setAutoReverse(true);
        bounce.setCycleCount(Animation.INDEFINITE);
        bounce.setInterpolator(Interpolator.EASE_BOTH);
        bounce.play();

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-font-size:17; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        Label sousTitreLbl = new Label(sousTitre);
        sousTitreLbl.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:" + accent + ";");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:11; -fx-text-fill:#6b7280;");
        descLbl.setWrapText(true);
        textBox.getChildren().addAll(titreLbl, sousTitreLbl, descLbl);

        // Fleche animée
        Label arrow = new Label("\u2192");
        arrow.setStyle("-fx-font-size:22; -fx-font-weight:800; -fx-text-fill:" + accent + ";");
        TranslateTransition arrowMove = new TranslateTransition(Duration.millis(800), arrow);
        arrowMove.setFromX(0); arrowMove.setToX(8);
        arrowMove.setAutoReverse(true);
        arrowMove.setCycleCount(Animation.INDEFINITE);
        arrowMove.play();

        topBand.getChildren().addAll(mainEmojiLbl, textBox, arrow);

        // Bande mini-emojis en bas avec animations
        HBox miniRow = new HBox(8);
        miniRow.setAlignment(Pos.CENTER_LEFT);
        miniRow.setPadding(new Insets(10, 20, 10, 20));
        miniRow.setStyle("-fx-background-color:" + bgLight + "; -fx-background-radius:0 0 18 18;");
        for (int i = 0; i < miniEmojis.length; i++) {
            Label ml = new Label(miniEmojis[i]);
            ml.setStyle("-fx-font-size:22; -fx-background-color:white; -fx-background-radius:50%;"
                    + "-fx-padding:6 8 6 8; -fx-min-width:38; -fx-min-height:38; -fx-alignment:CENTER;"
                    + "-fx-effect:dropshadow(gaussian," + accent + "33,6,0,0,2);");
            
            // Animation de rotation pour chaque mini-emoji
            final int idx = i;
            RotateTransition rt = new RotateTransition(Duration.millis(2000 + idx * 300), ml);
            rt.setByAngle(360);
            rt.setCycleCount(Animation.INDEFINITE);
            rt.setInterpolator(Interpolator.LINEAR);
            rt.play();
            
            miniRow.getChildren().add(ml);
        }
        Label plusLbl = new Label("et plus...");
        plusLbl.setStyle("-fx-font-size:11; -fx-text-fill:" + accent + "; -fx-font-weight:600;");
        miniRow.getChildren().add(plusLbl);

        card.getChildren().addAll(topBand, miniRow);

        // Hover + animation
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color:" + bgLight + "; -fx-background-radius:20;"
                    + "-fx-border-color:" + accent + "; -fx-border-radius:20; -fx-border-width:2.5;"
                    + "-fx-effect:dropshadow(gaussian," + accent + "66,18,0,0,6); -fx-cursor:hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.05); st.setToY(1.05); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color:" + bgCard + "; -fx-background-radius:20;"
                    + "-fx-border-color:" + accent + "44; -fx-border-radius:20; -fx-border-width:2;"
                    + "-fx-effect:dropshadow(gaussian," + accent + "33,12,0,0,4); -fx-cursor:hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        card.setOnMouseClicked(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(80), card);
            st.setToX(0.97); st.setToY(0.97);
            st.setOnFinished(ev2 -> {
                ScaleTransition st2 = new ScaleTransition(Duration.millis(80), card);
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
        card.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setFromY(30); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }
}