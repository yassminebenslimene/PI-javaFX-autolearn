package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Memory Card Game — jeu de mémoire avec cartes à retourner.
 * API intégrée : TheCatAPI (gratuite, no-key) pour les images des cartes.
 * Fallback : emojis kawaii si API indisponible.
 * Sons : flip, match, victoire — générés via javax.sound.
 */
public class MemoryGameController {

    private static final float SR = 44100f;
    private static final int COLS = 4, ROWS = 3; // 12 cartes = 6 paires
    
    // Types de cafés avec emojis et descriptions
    private static final String[] CAFE_TYPES = {
        "Espresso", "Cappuccino", "Latte", "Americano", "Mocha", "Iced Coffee"
    };
    private static final String[] CAFE_EMOJIS = {
        "\u2615", "\uD83E\uDD5B", "\uD83C\uDF75", "\uD83E\uDDCB", "\uD83C\uDF6B", "\uD83E\uDDCA"
    };
    private static final String[] CAFE_DESCRIPTIONS = {
        "Court, intense", "Mousse onctueuse", "Lait chaud", "Long, leger", "Chocolat & cafe", "Frais, glace"
    };
    private static final String[] CARD_COLORS = {
        "#6b3a2a","#c47c3a","#d4a96a","#3e2723","#4e342e","#1565c0"
    };

    // État du jeu
    private static final List<CardData> cards = new ArrayList<>();
    private static int flippedCount = 0;
    private static CardData firstFlipped = null;
    private static int matchesFound = 0;
    private static int moves = 0;
    private static boolean locked = false;
    private static Label movesLabel, matchesLabel;
    private static GridPane gameGrid;
    private static VBox gameBody;
    private static Stage currentDialog;

    public static void show(Window owner) {
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        currentDialog = new Stage();
        currentDialog.initModality(Modality.APPLICATION_MODAL);
        currentDialog.initStyle(StageStyle.TRANSPARENT);
        currentDialog.initOwner(owner);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color:rgba(0,0,0,0.7);");
        root.setAlignment(Pos.CENTER);

        VBox modal = buildModal(winH);
        modal.setPrefWidth(680);
        modal.setMaxWidth(680);
        modal.setPrefHeight(winH * 0.92);
        modal.setMaxHeight(winH * 0.92);
        root.getChildren().add(modal);

        Runnable close = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> currentDialog.close());
            ft.play();
        };
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });

        Scene scene = new Scene(root, winW, winH);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) close.run(); });
        currentDialog.setScene(scene);
        currentDialog.setX(owner.getX());
        currentDialog.setY(owner.getY());

        root.setOpacity(0);
        modal.setTranslateY(40);
        currentDialog.show();

        FadeTransition fi = new FadeTransition(Duration.millis(220), root);
        fi.setFromValue(0); fi.setToValue(1);
        TranslateTransition su = new TranslateTransition(Duration.millis(280), modal);
        su.setFromY(40); su.setToY(0); su.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, su).play();
    }

    private static VBox buildModal(double winH) {
        resetGame();

        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:#1a0033; -fx-background-radius:24;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),40,0,0,10);");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#c44dff,#ff6b9d);"
                + "-fx-background-radius:24 24 0 0;");

        Label icon = new Label("🃏");
        icon.setStyle("-fx-font-size:32;");
        ScaleTransition iconPulse = new ScaleTransition(Duration.millis(800), icon);
        iconPulse.setFromX(1.0); iconPulse.setToX(1.2);
        iconPulse.setFromY(1.0); iconPulse.setToY(1.2);
        iconPulse.setAutoReverse(true); iconPulse.setCycleCount(Animation.INDEFINITE); iconPulse.play();

        VBox headerText = new VBox(2);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        Label titre = new Label("☕  Memory Cafés");
        titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        Label sub = new Label("Trouvez toutes les paires de cafés !");
        sub.setStyle("-fx-font-size:11; -fx-text-fill:rgba(255,255,255,0.85);");
        headerText.getChildren().addAll(titre, sub);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.2); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> currentDialog.close());
        header.getChildren().addAll(icon, headerText, closeBtn);

        // Stats bar
        HBox statsBar = new HBox(20);
        statsBar.setAlignment(Pos.CENTER);
        statsBar.setPadding(new Insets(12, 24, 12, 24));
        statsBar.setStyle("-fx-background-color:#2d0050;");

        movesLabel = new Label("🎯  Coups : 0");
        movesLabel.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#c44dff;");
        matchesLabel = new Label("✅  Paires : 0 / 6");
        matchesLabel.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#4ecdc4;");

        Button newGameBtn = new Button("🔄  Nouvelle partie");
        newGameBtn.setStyle("-fx-background-color:#7c3aed; -fx-text-fill:white; -fx-font-size:12;"
                + "-fx-font-weight:700; -fx-padding:8 18 8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
        newGameBtn.setOnAction(e -> restartGame());

        statsBar.getChildren().addAll(movesLabel, matchesLabel, newGameBtn);

        // Loading label
        Label loadingLbl = new Label("⏳  Chargement des images depuis TheCatAPI...");
        loadingLbl.setStyle("-fx-font-size:13; -fx-text-fill:#c44dff; -fx-font-style:italic;");
        loadingLbl.setAlignment(Pos.CENTER);

        // Game body
        gameBody = new VBox(12);
        gameBody.setAlignment(Pos.CENTER);
        gameBody.setPadding(new Insets(16, 20, 16, 20));
        gameBody.setStyle("-fx-background-color:#1a0033;");
        gameBody.getChildren().add(loadingLbl);

        gameGrid = new GridPane();
        gameGrid.setHgap(10); gameGrid.setVgap(10);
        gameGrid.setAlignment(Pos.CENTER);

        VBox.setVgrow(gameBody, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setStyle("-fx-background-color:#0d0020; -fx-background-radius:0 0 24 24;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:#2d0050; -fx-text-fill:#c44dff; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25;"
                + "-fx-border-color:#c44dff; -fx-border-radius:25; -fx-border-width:1.5; -fx-cursor:hand;");
        fermerBtn.setOnAction(e -> currentDialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, statsBar, gameBody, footer);

        // Charger les images depuis TheCatAPI
        loadCatImages();
        return modal;
    }

    private static void resetGame() {
        cards.clear();
        flippedCount = 0; firstFlipped = null;
        matchesFound = 0; moves = 0; locked = false;
    }

    private static void restartGame() {
        resetGame();
        if (movesLabel != null) movesLabel.setText("🎯  Coups : 0");
        if (matchesLabel != null) matchesLabel.setText("✅  Paires : 0 / 6");
        if (gameBody != null) {
            gameBody.getChildren().clear();
            Label loadingLbl = new Label("⏳  Chargement des images...");
            loadingLbl.setStyle("-fx-font-size:13; -fx-text-fill:#c44dff; -fx-font-style:italic;");
            loadingLbl.setAlignment(Pos.CENTER);
            gameBody.getChildren().add(loadingLbl);
        }
        loadCatImages();
    }

    // ── Chargement des images de cafés ─────────────────

    private static void loadCatImages() {
        // Charger directement les images de cafés sans API externe
        buildGameGrid(null);
    }

    private static void buildGameGrid(List<Image> cafeImages) {
        gameBody.getChildren().clear();
        gameGrid.getChildren().clear();
        gameGrid.getColumnConstraints().clear();

        // Créer 6 paires de cafés
        List<Integer> pairIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) { pairIds.add(i); pairIds.add(i); }
        Collections.shuffle(pairIds);

        for (int i = 0; i < COLS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS); cc.setFillWidth(true);
            gameGrid.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < pairIds.size(); i++) {
            int pairId = pairIds.get(i);
            CardData card = new CardData(pairId, null, CARD_COLORS[pairId % CARD_COLORS.length],
                    CAFE_EMOJIS[pairId % CAFE_EMOJIS.length]);
            cards.add(card);
            StackPane cardNode = buildCardNode(card);
            card.node = cardNode;
            gameGrid.add(cardNode, i % COLS, i / COLS);
        }

        gameBody.getChildren().add(gameGrid);

        // Montrer brièvement les cartes au début
        showAllCardsTemporarily();
    }

    private static StackPane buildCardNode(CardData card) {
        StackPane stack = new StackPane();
        stack.setPrefSize(130, 110);
        stack.setMaxSize(130, 110);
        stack.setStyle("-fx-cursor:hand;");

        // Face cachée (dos de carte)
        VBox back = new VBox();
        back.setAlignment(Pos.CENTER);
        back.setPrefSize(130, 110);
        back.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#c44dff);"
                + "-fx-background-radius:14; -fx-border-color:#ffffff44; -fx-border-radius:14; -fx-border-width:2;"
                + "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),12,0,0,4);");
        Label backLabel = new Label("\u3010");
        backLabel.setStyle("-fx-font-size:36;");
        back.getChildren().add(backLabel);

        // Face visible avec type de café
        VBox front = new VBox(4);
        front.setAlignment(Pos.CENTER);
        front.setPrefSize(130, 110);
        front.setStyle("-fx-background-color:" + card.color + "22; -fx-background-radius:14;"
                + "-fx-border-color:" + card.color + "; -fx-border-radius:14; -fx-border-width:2.5;"
                + "-fx-effect:dropshadow(gaussian," + card.color + "88,14,0,0,4);");
        front.setVisible(false);

        // Emoji du café (grand)
        Label emojiLbl = new Label(card.fallbackEmoji);
        emojiLbl.setStyle("-fx-font-size:40;");
        
        // Nom du café
        Label nameLbl = new Label(CAFE_TYPES[card.pairId % CAFE_TYPES.length]);
        nameLbl.setStyle("-fx-font-size:11; -fx-font-weight:bold; -fx-text-fill:#1e1e1e;");
        nameLbl.setWrapText(true);
        nameLbl.setMaxWidth(100);
        nameLbl.setAlignment(Pos.CENTER);
        
        // Description
        Label descLbl = new Label(CAFE_DESCRIPTIONS[card.pairId % CAFE_DESCRIPTIONS.length]);
        descLbl.setStyle("-fx-font-size:9; -fx-text-fill:#6b7280; -fx-font-style:italic;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(100);
        descLbl.setAlignment(Pos.CENTER);
        
        front.getChildren().addAll(emojiLbl, nameLbl, descLbl);

        stack.getChildren().addAll(back, front);
        card.backNode = back; card.frontNode = front;

        stack.setOnMouseClicked(e -> onCardClick(card));
        return stack;
    }

    private static void showAllCardsTemporarily() {
        // Montrer toutes les cartes 2s puis les retourner avec animation
        for (int i = 0; i < cards.size(); i++) {
            CardData c = cards.get(i);
            c.backNode.setVisible(false);
            c.frontNode.setVisible(true);
            
            // Animation d'apparition
            c.node.setOpacity(0);
            c.node.setScaleX(0.7);
            c.node.setScaleY(0.7);
            FadeTransition ft = new FadeTransition(Duration.millis(300), c.node);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 40));
            ScaleTransition st = new ScaleTransition(Duration.millis(300), c.node);
            st.setFromX(0.7);
            st.setFromY(0.7);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setDelay(Duration.millis(i * 40));
            st.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(ft, st).play();
        }
        
        PauseTransition pause = new PauseTransition(Duration.millis(2000));
        pause.setOnFinished(e -> {
            for (int i = 0; i < cards.size(); i++) {
                CardData c = cards.get(i);
                int delay = i * 40;
                ScaleTransition st1 = new ScaleTransition(Duration.millis(150), c.node);
                st1.setFromX(1.0);
                st1.setToX(0.0);
                st1.setDelay(Duration.millis(delay));
                st1.setOnFinished(ev -> {
                    c.backNode.setVisible(true);
                    c.frontNode.setVisible(false);
                    ScaleTransition st2 = new ScaleTransition(Duration.millis(150), c.node);
                    st2.setFromX(0.0);
                    st2.setToX(1.0);
                    st2.play();
                });
                st1.play();
            }
        });
        pause.play();
    }

    // ── Logique du jeu ───────────────────────────────────────────

    private static void onCardClick(CardData card) {
        if (locked || card.isFlipped || card.isMatched) return;

        playFlipSound();
        flipCard(card, true);
        card.isFlipped = true;
        flippedCount++;

        if (flippedCount == 1) {
            firstFlipped = card;
        } else if (flippedCount == 2) {
            flippedCount = 0;
            moves++;
            movesLabel.setText("🎯  Coups : " + moves);
            locked = true;

            if (firstFlipped.pairId == card.pairId) {
                // Match !
                matchesFound++;
                matchesLabel.setText("✅  Paires : " + matchesFound + " / 6");
                playMatchSound();
                animateMatch(firstFlipped, card);
                firstFlipped = null;
                locked = false;
                if (matchesFound == 6) {
                    PauseTransition pt = new PauseTransition(Duration.millis(600));
                    pt.setOnFinished(e -> showVictory());
                    pt.play();
                }
            } else {
                // Pas de match — retourner après délai
                CardData second = card;
                PauseTransition pt = new PauseTransition(Duration.millis(900));
                pt.setOnFinished(e -> {
                    flipCard(firstFlipped, false);
                    flipCard(second, false);
                    firstFlipped.isFlipped = false;
                    second.isFlipped = false;
                    firstFlipped = null;
                    locked = false;
                });
                pt.play();
            }
        }
    }

    private static void flipCard(CardData card, boolean showFront) {
        ScaleTransition st1 = new ScaleTransition(Duration.millis(150), card.node);
        st1.setFromX(1.0); st1.setToX(0.0);
        st1.setOnFinished(e -> {
            card.backNode.setVisible(!showFront);
            card.frontNode.setVisible(showFront);
            ScaleTransition st2 = new ScaleTransition(Duration.millis(150), card.node);
            st2.setFromX(0.0); st2.setToX(1.0);
            st2.play();
        });
        st1.play();
    }

    private static void animateMatch(CardData c1, CardData c2) {
        c1.isMatched = true; c2.isMatched = true;
        for (CardData c : new CardData[]{c1, c2}) {
            c.frontNode.setStyle("-fx-background-color:" + c.color + "44; -fx-background-radius:14;"
                    + "-fx-border-color:#26de81; -fx-border-radius:14; -fx-border-width:3;"
                    + "-fx-effect:dropshadow(gaussian,#26de8188,18,0,0,6);");
            ScaleTransition bounce = new ScaleTransition(Duration.millis(200), c.node);
            bounce.setFromX(1.0); bounce.setToX(1.12);
            bounce.setFromY(1.0); bounce.setToY(1.12);
            bounce.setAutoReverse(true); bounce.setCycleCount(2); bounce.play();
        }
    }

    private static void showVictory() {
        playVictorySound();
        gameBody.getChildren().clear();

        VBox victoryBox = new VBox(16);
        victoryBox.setAlignment(Pos.CENTER);
        victoryBox.setPadding(new Insets(40));
        victoryBox.setStyle("-fx-background-color:#1a0033;");

        Label trophy = new Label("🏆");
        trophy.setStyle("-fx-font-size:80;");
        ScaleTransition trophyBounce = new ScaleTransition(Duration.millis(500), trophy);
        trophyBounce.setFromX(0.5); trophyBounce.setToX(1.0);
        trophyBounce.setFromY(0.5); trophyBounce.setToY(1.0);
        trophyBounce.setInterpolator(Interpolator.EASE_OUT); trophyBounce.play();

        Label winLbl = new Label("🎉  Félicitations !  🎉");
        winLbl.setStyle("-fx-font-size:26; -fx-font-weight:800; -fx-text-fill:#f7b731;");

        Label scoreLbl = new Label("Complété en " + moves + " coups !");
        scoreLbl.setStyle("-fx-font-size:16; -fx-text-fill:white;");

        // Confettis
        HBox confetti = new HBox(10);
        confetti.setAlignment(Pos.CENTER);
        for (String s : new String[]{"🎊","⭐","🌟","🎉","💫","✨","🎊","⭐"}) {
            Label cl = new Label(s); cl.setStyle("-fx-font-size:24;");
            confetti.getChildren().add(cl);
        }
        for (int i = 0; i < confetti.getChildren().size(); i++) {
            javafx.scene.Node n = confetti.getChildren().get(i);
            TranslateTransition tt = new TranslateTransition(Duration.millis(500 + i * 60), n);
            tt.setFromY(0); tt.setToY(-14);
            tt.setAutoReverse(true); tt.setCycleCount(Animation.INDEFINITE);
            tt.setDelay(Duration.millis(i * 80)); tt.play();
        }

        // Fun fact via Numbers API
        Label funFactLbl = new Label("⏳  Chargement d'un fun fact...");
        funFactLbl.setStyle("-fx-font-size:12; -fx-text-fill:#c44dff; -fx-font-style:italic;");
        funFactLbl.setWrapText(true); funFactLbl.setMaxWidth(500);

        Button playAgainBtn = new Button("🔄  Rejouer");
        playAgainBtn.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#c44dff);"
                + "-fx-text-fill:white; -fx-font-size:14; -fx-font-weight:700;"
                + "-fx-padding:12 32 12 32; -fx-background-radius:25; -fx-cursor:hand; -fx-border-width:0;");
        playAgainBtn.setOnAction(e -> restartGame());

        victoryBox.getChildren().addAll(trophy, winLbl, scoreLbl, confetti, funFactLbl, playAgainBtn);
        gameBody.getChildren().add(victoryBox);

        fetchNumberFact(moves, funFactLbl);
    }

    // ── API : Numbers API — fun fact sur le score ────────────────

    private static void fetchNumberFact(int number, Label targetLabel) {
        Task<String> task = new Task<>() {
            @Override protected String call() {
                try {
                    URL url = new URL("http://numbersapi.com/" + number + "/trivia?json");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    if (conn.getResponseCode() == 200) {
                        String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        String text = extractJsonValue(json, "text");
                        if (text != null && !text.isEmpty()) return "🔢  Fun fact : " + text;
                    }
                } catch (Exception ignored) {}
                return "🔢  Fun fact : " + number + " est le nombre de coups qu'il vous a fallu — excellent !";
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            targetLabel.setText(task.getValue());
            targetLabel.setStyle("-fx-font-size:12; -fx-text-fill:#c44dff;");
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
            targetLabel.setText("🔢  Fun fact : " + number + " coups — bien joué !")));
        Thread t = new Thread(task, "numbers-api");
        t.setDaemon(true); t.start();
    }

    // ── Sons ─────────────────────────────────────────────────────

    private static void playFlipSound() {
        SoundGenerator.playMemoryFlip();
    }

    private static void playMatchSound() {
        SoundGenerator.playMemoryMatch();
    }

    private static void playVictorySound() {
        SoundGenerator.playMemoryVictory();
    }

    // ── Utilitaires JSON ─────────────────────────────────────────

    private static String extractJsonValue(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int idx = json.indexOf(search);
            if (idx < 0) return null;
            int start = idx + search.length();
            while (start < json.length() && json.charAt(start) == ' ') start++;
            if (json.charAt(start) == '"') {
                int end = json.indexOf('"', start + 1);
                return json.substring(start + 1, end);
            } else {
                int end = start;
                while (end < json.length() && ",}\n".indexOf(json.charAt(end)) < 0) end++;
                return json.substring(start, end).trim();
            }
        } catch (Exception e) { return null; }
    }

    private static List<String> extractAllJsonValues(String json, String key) {
        List<String> results = new ArrayList<>();
        String search = "\"" + key + "\":\"";
        int idx = 0;
        while ((idx = json.indexOf(search, idx)) >= 0) {
            int start = idx + search.length();
            int end = json.indexOf('"', start);
            if (end > start) results.add(json.substring(start, end));
            idx = end + 1;
        }
        return results;
    }

    // ── CardData ─────────────────────────────────────────────────

    static class CardData {
        int pairId;
        Image image;
        String color, fallbackEmoji;
        boolean isFlipped = false, isMatched = false;
        StackPane node;
        VBox backNode, frontNode;

        CardData(int pairId, Image image, String color, String fallbackEmoji) {
            this.pairId = pairId; this.image = image;
            this.color = color; this.fallbackEmoji = fallbackEmoji;
        }
    }
}
