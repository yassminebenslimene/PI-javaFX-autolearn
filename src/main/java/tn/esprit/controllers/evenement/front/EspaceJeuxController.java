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

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Espace Jeux — hub de sélection des mini-jeux.
 * Propose : Memory Cards (TheCatAPI) + Candy Crush (Numbers API).
 * Animations colorées, sons, interface festive.
 */
public class EspaceJeuxController {

    private static final float SR = 44100f;

    public static void show(Window owner) {
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color:rgba(0,0,0,0.68);");
        root.setAlignment(Pos.CENTER);

        VBox modal = buildModal(dialog, winH);
        modal.setPrefWidth(660);
        modal.setMaxWidth(660);
        root.getChildren().add(modal);

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

        FadeTransition fi = new FadeTransition(Duration.millis(230), root);
        fi.setFromValue(0); fi.setToValue(1);
        TranslateTransition su = new TranslateTransition(Duration.millis(280), modal);
        su.setFromY(45); su.setToY(0); su.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, su).play();

        playOpenSound();
    }

    private static VBox buildModal(Stage dialog, double winH) {
        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:#0a001a; -fx-background-radius:24;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),40,0,0,10);");

        // Header arc-en-ciel
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#c44dff,#ff6b9d,#f7b731);"
                + "-fx-background-radius:24 24 0 0;");

        Label icon = new Label("🎮");
        icon.setStyle("-fx-font-size:38;");
        // Bounce continu
        TranslateTransition iconBounce = new TranslateTransition(Duration.millis(700), icon);
        iconBounce.setFromY(0); iconBounce.setToY(-8);
        iconBounce.setAutoReverse(true); iconBounce.setCycleCount(Animation.INDEFINITE);
        iconBounce.setInterpolator(Interpolator.EASE_BOTH); iconBounce.play();

        VBox headerText = new VBox(3);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        Label titre = new Label("🎮  Espace Jeux");
        titre.setStyle("-fx-font-size:22; -fx-font-weight:800; -fx-text-fill:white;");
        Label sub = new Label("Choisissez votre jeu et amusez-vous ! 🎉");
        sub.setStyle("-fx-font-size:12; -fx-text-fill:rgba(255,255,255,0.9);");
        headerText.getChildren().addAll(titre, sub);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.2); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(icon, headerText, closeBtn);

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(24, 24, 24, 24));
        body.setStyle("-fx-background-color:#0a001a;");

        Label subtitle = new Label("🕹️  Sélectionnez un jeu");
        subtitle.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#c44dff;");
        body.getChildren().add(subtitle);

        // Card Memory
        VBox cardMemory = buildGameCard(
            "🃏", "Memory Cards",
            "Jeu de mémoire",
            "Retournez les cartes et trouvez les paires !\nImages de chats via TheCatAPI 🐱",
            "#7c3aed", "#f3e5f5", "#faf5ff",
            new String[]{"🐱","🃏","🧠","⭐"},
            "TheCatAPI + Numbers API",
            () -> { dialog.close(); MemoryGameController.show(dialog.getOwner()); }
        );

        // Card Candy
        VBox cardCandy = buildGameCard(
            "🍬", "Candy Crush",
            "Match-3 coloré",
            "Échangez les bonbons pour aligner 3 identiques !\nFun facts via Numbers API 🔢",
            "#f7b731", "#fff8e1", "#fffde7",
            new String[]{"🍬","🍭","🍫","🎉"},
            "Numbers API",
            () -> { dialog.close(); CandyGameController.show(dialog.getOwner()); }
        );

        animateCard(cardMemory, 0);
        animateCard(cardCandy, 150);
        body.getChildren().addAll(cardMemory, cardCandy);

        // Décorations flottantes
        HBox decoRow = new HBox(12);
        decoRow.setAlignment(Pos.CENTER);
        decoRow.setPadding(new Insets(8, 0, 0, 0));
        for (String s : new String[]{"🎮","⭐","🎯","🏆","🎊","💫","🎮","⭐","🎯"}) {
            Label dl = new Label(s);
            dl.setStyle("-fx-font-size:18;");
            decoRow.getChildren().add(dl);
        }
        FadeTransition decoFade = new FadeTransition(Duration.millis(2000), decoRow);
        decoFade.setFromValue(0.4); decoFade.setToValue(1.0);
        decoFade.setAutoReverse(true); decoFade.setCycleCount(Animation.INDEFINITE); decoFade.play();
        body.getChildren().add(decoRow);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setStyle("-fx-background-color:#050010; -fx-background-radius:0 0 24 24;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:#1a0033; -fx-text-fill:#c44dff; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25;"
                + "-fx-border-color:#c44dff; -fx-border-radius:25; -fx-border-width:1.5; -fx-cursor:hand;");
        fermerBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, body, footer);
        return modal;
    }

    private static VBox buildGameCard(String mainEmoji, String titre, String sousTitre,
                                       String desc, String accent, String bgLight, String bgCard,
                                       String[] miniEmojis, String apiTag, Runnable onClick) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:" + bgCard + "; -fx-background-radius:20;"
                + "-fx-border-color:" + accent + "44; -fx-border-radius:20; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian," + accent + "33,12,0,0,4); -fx-cursor:hand;");

        HBox topBand = new HBox(16);
        topBand.setAlignment(Pos.CENTER_LEFT);
        topBand.setPadding(new Insets(18, 20, 18, 20));
        topBand.setStyle("-fx-background-color:linear-gradient(to right," + accent + "22," + bgLight + ");"
                + "-fx-background-radius:18 18 0 0;");

        Label mainEmojiLbl = new Label(mainEmoji);
        mainEmojiLbl.setStyle("-fx-font-size:44; -fx-background-color:" + accent + ";"
                + "-fx-background-radius:50%; -fx-padding:14 16 14 16;"
                + "-fx-min-width:80; -fx-min-height:80; -fx-alignment:CENTER;");
        // Pulse sur l'emoji
        ScaleTransition emojiPulse = new ScaleTransition(Duration.millis(1000), mainEmojiLbl);
        emojiPulse.setFromX(1.0); emojiPulse.setToX(1.1);
        emojiPulse.setFromY(1.0); emojiPulse.setToY(1.1);
        emojiPulse.setAutoReverse(true); emojiPulse.setCycleCount(Animation.INDEFINITE); emojiPulse.play();

        VBox textBox = new VBox(4);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-font-size:17; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        Label sousTitreLbl = new Label(sousTitre);
        sousTitreLbl.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:" + accent + ";");
        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:11; -fx-text-fill:#6b7280;");
        descLbl.setWrapText(true);
        // Badge API
        Label apiBadge = new Label("🔗 " + apiTag);
        apiBadge.setStyle("-fx-background-color:" + accent + "22; -fx-text-fill:" + accent + ";"
                + "-fx-font-size:10; -fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 8 3 8;");
        textBox.getChildren().addAll(titreLbl, sousTitreLbl, descLbl, apiBadge);

        Label arrow = new Label("▶");
        arrow.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:" + accent + ";");
        topBand.getChildren().addAll(mainEmojiLbl, textBox, arrow);

        // Mini emojis
        HBox miniRow = new HBox(8);
        miniRow.setAlignment(Pos.CENTER_LEFT);
        miniRow.setPadding(new Insets(10, 20, 10, 20));
        miniRow.setStyle("-fx-background-color:" + bgLight + "; -fx-background-radius:0 0 18 18;");
        for (String e : miniEmojis) {
            Label ml = new Label(e);
            ml.setStyle("-fx-font-size:22; -fx-background-color:white; -fx-background-radius:50%;"
                    + "-fx-padding:6 8 6 8; -fx-min-width:38; -fx-min-height:38; -fx-alignment:CENTER;"
                    + "-fx-effect:dropshadow(gaussian," + accent + "33,6,0,0,2);");
            miniRow.getChildren().add(ml);
        }
        Label playLbl = new Label("Jouer maintenant →");
        playLbl.setStyle("-fx-font-size:11; -fx-text-fill:" + accent + "; -fx-font-weight:700;");
        miniRow.getChildren().add(playLbl);

        card.getChildren().addAll(topBand, miniRow);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color:" + bgLight + "; -fx-background-radius:20;"
                    + "-fx-border-color:" + accent + "; -fx-border-radius:20; -fx-border-width:2.5;"
                    + "-fx-effect:dropshadow(gaussian," + accent + "66,18,0,0,6); -fx-cursor:hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
            st.setToX(1.02); st.setToY(1.02); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color:" + bgCard + "; -fx-background-radius:20;"
                    + "-fx-border-color:" + accent + "44; -fx-border-radius:20; -fx-border-width:2;"
                    + "-fx-effect:dropshadow(gaussian," + accent + "33,12,0,0,4); -fx-cursor:hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(140), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });
        card.setOnMouseClicked(e -> {
            playClickSound();
            ScaleTransition st = new ScaleTransition(Duration.millis(80), card);
            st.setToX(0.97); st.setToY(0.97);
            st.setOnFinished(ev -> {
                ScaleTransition st2 = new ScaleTransition(Duration.millis(80), card);
                st2.setToX(1.0); st2.setToY(1.0);
                st2.setOnFinished(ev2 -> onClick.run());
                st2.play();
            });
            st.play();
        });
        return card;
    }

    private static void animateCard(VBox card, int delayMs) {
        card.setOpacity(0); card.setTranslateY(30);
        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
        tt.setFromY(30); tt.setToY(0); tt.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs)); pt.play();
    }

    // ── Sons ─────────────────────────────────────────────────────

    private static void playOpenSound() {
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (double[] n : new double[][]{{523,80},{659,80},{784,80},{1047,150}})
                    baos.write(genTone((int)n[0], (int)n[1], 0.35f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        }, "jeux-open-sound");
        t.setDaemon(true); t.start();
    }

    private static void playClickSound() {
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(SR, 16, 1, true, false);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                baos.write(genTone(880, 60, 0.3f));
                baos.write(genTone(1100, 80, 0.35f));
                playBytes(baos.toByteArray(), fmt);
            } catch (Exception ignored) {}
        }, "jeux-click-sound");
        t.setDaemon(true); t.start();
    }

    private static byte[] genTone(int freq, int ms, float vol) {
        int n = (int)(SR * ms / 1000.0);
        byte[] buf = new byte[n * 2];
        int fi = Math.max(1, n/8), fo = Math.max(1, n/5);
        for (int i = 0; i < n; i++) {
            double t = i / SR;
            double w = (Math.sin(2*Math.PI*freq*t) + 0.3*Math.sin(4*Math.PI*freq*t)) / 1.3;
            double env = i < fi ? (double)i/fi : i > n-fo ? (double)(n-i)/fo : 1.0;
            short s = (short)(w * env * vol * Short.MAX_VALUE);
            buf[i*2]=(byte)(s&0xFF); buf[i*2+1]=(byte)((s>>8)&0xFF);
        }
        return buf;
    }

    private static void playBytes(byte[] data, AudioFormat fmt) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        AudioInputStream ais = new AudioInputStream(bais, fmt, data.length / fmt.getFrameSize());
        DataLine.Info info = new DataLine.Info(Clip.class, fmt);
        if (!AudioSystem.isLineSupported(info)) return;
        Clip clip = (Clip) AudioSystem.getLine(info);
        clip.open(ais); clip.start();
        Thread.sleep(clip.getMicrosecondLength() / 1000 + 50);
        clip.close();
    }
}
