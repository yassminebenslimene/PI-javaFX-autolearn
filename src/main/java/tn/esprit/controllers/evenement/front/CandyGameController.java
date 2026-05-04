package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Candy Crush-like — match-3 simplifié.
 * Grille 6x6 de bonbons colorés, cliquer 2 adjacents pour les échanger.
 * Si 3+ identiques alignés → explosion + score.
 * API : Numbers API pour fun fact sur le score final.
 * Sons : swap, match, explosion — javax.sound.
 */
public class CandyGameController {

    private static final float SR = 44100f;
    private static final int GRID = 6;
    private static final String[] CANDIES = {"🍬","🍭","🍫","🍡","🧁","🍰"};
    private static final String[] CANDY_COLORS = {
        "#ff6b9d","#c44dff","#f7b731","#26de81","#fd9644","#4ecdc4"
    };
    private static final String[] CANDY_BG = {
        "#fce4ec","#f3e5f5","#fff8e1","#e8f5e9","#fff3e0","#e0f7fa"
    };

    private static int[][] grid = new int[GRID][GRID];
    private static Label[][] cells = new Label[GRID][GRID];
    private static int score = 0;
    private static int selectedRow = -1, selectedCol = -1;
    private static Label scoreLabel;
    private static GridPane gameGrid;
    private static VBox gameBody;
    private static Stage currentDialog;
    private static int movesLeft = 20;
    private static Label movesLabel;

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
        modal.setPrefWidth(600);
        modal.setMaxWidth(600);
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
        initGrid();

        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:#0a001a; -fx-background-radius:24;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),40,0,0,10);");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#f7b731,#fd9644,#ff6b9d);"
                + "-fx-background-radius:24 24 0 0;");

        Label icon = new Label("🍬");
        icon.setStyle("-fx-font-size:32;");
        RotateTransition iconRot = new RotateTransition(Duration.millis(2000), icon);
        iconRot.setByAngle(360); iconRot.setCycleCount(Animation.INDEFINITE);
        iconRot.setInterpolator(Interpolator.LINEAR); iconRot.play();

        VBox headerText = new VBox(2);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        Label titre = new Label("🍬  Candy Crush");
        titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        Label sub = new Label("Match 3 bonbons pour marquer des points !");
        sub.setStyle("-fx-font-size:11; -fx-text-fill:rgba(255,255,255,0.9);");
        headerText.getChildren().addAll(titre, sub);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.2); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> currentDialog.close());
        header.getChildren().addAll(icon, headerText, closeBtn);

        // Stats
        HBox statsBar = new HBox(24);
        statsBar.setAlignment(Pos.CENTER);
        statsBar.setPadding(new Insets(12, 24, 12, 24));
        statsBar.setStyle("-fx-background-color:#1a0033;");

        scoreLabel = new Label("⭐  Score : 0");
        scoreLabel.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#f7b731;");
        movesLabel = new Label("🎯  Coups : 20");
        movesLabel.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:#fd9644;");

        Button newGameBtn = new Button("🔄  Nouveau");
        newGameBtn.setStyle("-fx-background-color:#f7b731; -fx-text-fill:#1a0033; -fx-font-size:12;"
                + "-fx-font-weight:700; -fx-padding:8 18 8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
        newGameBtn.setOnAction(e -> restartGame());
        statsBar.getChildren().addAll(scoreLabel, movesLabel, newGameBtn);

        // Game body
        gameBody = new VBox(10);
        gameBody.setAlignment(Pos.CENTER);
        gameBody.setPadding(new Insets(14, 20, 14, 20));
        gameBody.setStyle("-fx-background-color:#0a001a;");
        VBox.setVgrow(gameBody, Priority.ALWAYS);

        // Instructions
        Label instrLbl = new Label("👆  Cliquez 2 bonbons adjacents pour les échanger  •  3+ identiques = points !");
        instrLbl.setStyle("-fx-font-size:11; -fx-text-fill:#aaaaaa; -fx-font-style:italic;");
        instrLbl.setWrapText(true); instrLbl.setAlignment(Pos.CENTER);

        gameGrid = buildGameGrid();
        gameBody.getChildren().addAll(instrLbl, gameGrid);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setStyle("-fx-background-color:#050010; -fx-background-radius:0 0 24 24;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:#1a0033; -fx-text-fill:#f7b731; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25;"
                + "-fx-border-color:#f7b731; -fx-border-radius:25; -fx-border-width:1.5; -fx-cursor:hand;");
        fermerBtn.setOnAction(e -> currentDialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, statsBar, gameBody, footer);
        return modal;
    }

    private static void initGrid() {
        score = 0; movesLeft = 20; selectedRow = -1; selectedCol = -1;
        Random rnd = new Random();
        for (int r = 0; r < GRID; r++)
            for (int c = 0; c < GRID; c++)
                grid[r][c] = rnd.nextInt(CANDIES.length);
    }

    private static void restartGame() {
        initGrid();
        if (scoreLabel != null) scoreLabel.setText("⭐  Score : 0");
        if (movesLabel != null) movesLabel.setText("🎯  Coups : 20");
        if (gameBody != null) {
            gameBody.getChildren().clear();
            Label instrLbl = new Label("👆  Cliquez 2 bonbons adjacents pour les échanger  •  3+ identiques = points !");
            instrLbl.setStyle("-fx-font-size:11; -fx-text-fill:#aaaaaa; -fx-font-style:italic;");
            instrLbl.setWrapText(true); instrLbl.setAlignment(Pos.CENTER);
            gameGrid = buildGameGrid();
            gameBody.getChildren().addAll(instrLbl, gameGrid);
        }
    }

    private static GridPane buildGameGrid() {
        GridPane gp = new GridPane();
        gp.setHgap(6); gp.setVgap(6);
        gp.setAlignment(Pos.CENTER);
        gp.setStyle("-fx-background-color:#1a0033; -fx-background-radius:16; -fx-padding:12;");

        for (int r = 0; r < GRID; r++) {
            for (int c = 0; c < GRID; c++) {
                Label cell = buildCandyCell(r, c);
                cells[r][c] = cell;
                gp.add(cell, c, r);
                // Entrée animée
                cell.setOpacity(0);
                FadeTransition ft = new FadeTransition(Duration.millis(300), cell);
                ft.setFromValue(0); ft.setToValue(1);
                ft.setDelay(Duration.millis((r * GRID + c) * 15));
                ft.play();
            }
        }
        return gp;
    }

    private static Label buildCandyCell(int row, int col) {
        int type = grid[row][col];
        Label cell = new Label(CANDIES[type]);
        cell.setPrefSize(68, 68);
        cell.setMaxSize(68, 68);
        cell.setAlignment(Pos.CENTER);
        cell.setStyle("-fx-font-size:30; -fx-background-color:" + CANDY_BG[type] + ";"
                + "-fx-background-radius:14; -fx-border-color:" + CANDY_COLORS[type] + "66;"
                + "-fx-border-radius:14; -fx-border-width:2; -fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian," + CANDY_COLORS[type] + "55,8,0,0,2);");

        cell.setOnMouseEntered(e -> {
            if (selectedRow == row && selectedCol == col) return;
            cell.setStyle("-fx-font-size:30; -fx-background-color:" + CANDY_COLORS[type] + "33;"
                    + "-fx-background-radius:14; -fx-border-color:" + CANDY_COLORS[type] + ";"
                    + "-fx-border-radius:14; -fx-border-width:2.5; -fx-cursor:hand;"
                    + "-fx-effect:dropshadow(gaussian," + CANDY_COLORS[type] + "88,12,0,0,4);");
        });
        cell.setOnMouseExited(e -> {
            if (selectedRow == row && selectedCol == col) return;
            cell.setStyle("-fx-font-size:30; -fx-background-color:" + CANDY_BG[type] + ";"
                    + "-fx-background-radius:14; -fx-border-color:" + CANDY_COLORS[type] + "66;"
                    + "-fx-border-radius:14; -fx-border-width:2; -fx-cursor:hand;"
                    + "-fx-effect:dropshadow(gaussian," + CANDY_COLORS[type] + "55,8,0,0,2);");
        });
        
        // Drag-and-drop pour swapper
        cell.setOnDragDetected(e -> {
            if (selectedRow == -1) {
                selectedRow = row;
                selectedCol = col;
                highlightSelected(row, col, true);
                playSelectSound();
            }
        });
        
        cell.setOnDragOver(e -> {
            if (selectedRow != -1 && (selectedRow != row || selectedCol != col)) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.MOVE);
            }
        });
        
        cell.setOnDragDropped(e -> {
            if (selectedRow != -1 && isAdjacent(selectedRow, selectedCol, row, col)) {
                int prevRow = selectedRow, prevCol = selectedCol;
                highlightSelected(prevRow, prevCol, false);
                selectedRow = -1;
                selectedCol = -1;
                swapAndCheck(prevRow, prevCol, row, col);
                e.setDropCompleted(true);
            }
        });
        
        cell.setOnMouseClicked(e -> onCellClick(row, col));
        return cell;
    }

    private static void onCellClick(int row, int col) {
        if (movesLeft <= 0) return;

        if (selectedRow == -1) {
            // Première sélection
            selectedRow = row; selectedCol = col;
            highlightSelected(row, col, true);
            playSelectSound();
        } else if (selectedRow == row && selectedCol == col) {
            // Désélectionner
            highlightSelected(row, col, false);
            selectedRow = -1; selectedCol = -1;
        } else if (isAdjacent(selectedRow, selectedCol, row, col)) {
            // Échanger
            int prevRow = selectedRow, prevCol = selectedCol;
            highlightSelected(prevRow, prevCol, false);
            selectedRow = -1; selectedCol = -1;
            swapAndCheck(prevRow, prevCol, row, col);
        } else {
            // Nouvelle sélection
            highlightSelected(selectedRow, selectedCol, false);
            selectedRow = row; selectedCol = col;
            highlightSelected(row, col, true);
            playSelectSound();
        }
    }

    private static boolean isAdjacent(int r1, int c1, int r2, int c2) {
        return (Math.abs(r1 - r2) == 1 && c1 == c2) || (Math.abs(c1 - c2) == 1 && r1 == r2);
    }

    private static void highlightSelected(int row, int col, boolean selected) {
        int type = grid[row][col];
        Label cell = cells[row][col];
        if (selected) {
            cell.setStyle("-fx-font-size:30; -fx-background-color:" + CANDY_COLORS[type] + "55;"
                    + "-fx-background-radius:14; -fx-border-color:" + CANDY_COLORS[type] + ";"
                    + "-fx-border-radius:14; -fx-border-width:3; -fx-cursor:hand;"
                    + "-fx-effect:dropshadow(gaussian," + CANDY_COLORS[type] + ",16,0.3,0,0);");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), cell);
            st.setToX(1.12); st.setToY(1.12); st.play();
        } else {
            cell.setStyle("-fx-font-size:30; -fx-background-color:" + CANDY_BG[type] + ";"
                    + "-fx-background-radius:14; -fx-border-color:" + CANDY_COLORS[type] + "66;"
                    + "-fx-border-radius:14; -fx-border-width:2; -fx-cursor:hand;"
                    + "-fx-effect:dropshadow(gaussian," + CANDY_COLORS[type] + "55,8,0,0,2);");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), cell);
            st.setToX(1.0); st.setToY(1.0); st.play();
        }
    }

    private static void swapAndCheck(int r1, int c1, int r2, int c2) {
        // Swap
        int tmp = grid[r1][c1]; grid[r1][c1] = grid[r2][c2]; grid[r2][c2] = tmp;
        playSwapSound();

        // Animer le swap
        animateSwap(r1, c1, r2, c2, () -> {
            // Mettre à jour les cellules
            updateCell(r1, c1); updateCell(r2, c2);

            // Chercher les matches
            List<int[]> matches = findMatches();
            if (!matches.isEmpty()) {
                movesLeft--;
                movesLabel.setText("🎯  Coups : " + movesLeft);
                explodeMatches(matches);
            } else {
                // Annuler le swap si pas de match
                int tmp2 = grid[r1][c1]; grid[r1][c1] = grid[r2][c2]; grid[r2][c2] = tmp2;
                updateCell(r1, c1); updateCell(r2, c2);
            }

            if (movesLeft <= 0) {
                PauseTransition pt = new PauseTransition(Duration.millis(500));
                pt.setOnFinished(e -> showGameOver());
                pt.play();
            }
        });
    }

    private static void animateSwap(int r1, int c1, int r2, int c2, Runnable onDone) {
        Label cell1 = cells[r1][c1], cell2 = cells[r2][c2];
        ScaleTransition st1 = new ScaleTransition(Duration.millis(100), cell1);
        st1.setToX(0.8); st1.setToY(0.8);
        ScaleTransition st2 = new ScaleTransition(Duration.millis(100), cell2);
        st2.setToX(0.8); st2.setToY(0.8);
        ParallelTransition pt = new ParallelTransition(st1, st2);
        pt.setOnFinished(e -> {
            ScaleTransition back1 = new ScaleTransition(Duration.millis(100), cell1);
            back1.setToX(1.0); back1.setToY(1.0);
            ScaleTransition back2 = new ScaleTransition(Duration.millis(100), cell2);
            back2.setToX(1.0); back2.setToY(1.0);
            ParallelTransition pt2 = new ParallelTransition(back1, back2);
            pt2.setOnFinished(ev -> onDone.run());
            pt2.play();
        });
        pt.play();
    }

    private static void updateCell(int row, int col) {
        int type = grid[row][col];
        Label cell = cells[row][col];
        cell.setText(CANDIES[type]);
        cell.setStyle("-fx-font-size:30; -fx-background-color:" + CANDY_BG[type] + ";"
                + "-fx-background-radius:14; -fx-border-color:" + CANDY_COLORS[type] + "66;"
                + "-fx-border-radius:14; -fx-border-width:2; -fx-cursor:hand;"
                + "-fx-effect:dropshadow(gaussian," + CANDY_COLORS[type] + "55,8,0,0,2);");
    }

    private static List<int[]> findMatches() {
        Set<String> matchSet = new HashSet<>();
        // Horizontal
        for (int r = 0; r < GRID; r++)
            for (int c = 0; c <= GRID - 3; c++)
                if (grid[r][c] == grid[r][c+1] && grid[r][c] == grid[r][c+2])
                    for (int k = c; k < c+3; k++) matchSet.add(r+","+k);
        // Vertical
        for (int c = 0; c < GRID; c++)
            for (int r = 0; r <= GRID - 3; r++)
                if (grid[r][c] == grid[r+1][c] && grid[r][c] == grid[r+2][c])
                    for (int k = r; k < r+3; k++) matchSet.add(k+","+c);

        List<int[]> result = new ArrayList<>();
        for (String key : matchSet) {
            String[] parts = key.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    private static void explodeMatches(List<int[]> matches) {
        SoundGenerator.playCandyMatch();
        playExplosionSound();
        int points = matches.size() * 10;
        score += points;
        scoreLabel.setText("⭐  Score : " + score);

        // Animer l'explosion
        for (int[] pos : matches) {
            Label cell = cells[pos[0]][pos[1]];
            ScaleTransition st = new ScaleTransition(Duration.millis(200), cell);
            st.setToX(1.4); st.setToY(1.4);
            FadeTransition ft = new FadeTransition(Duration.millis(200), cell);
            ft.setToValue(0);
            ParallelTransition pt = new ParallelTransition(st, ft);
            pt.setOnFinished(e -> {
                // Remplir avec nouveau bonbon
                grid[pos[0]][pos[1]] = new Random().nextInt(CANDIES.length);
                updateCell(pos[0], pos[1]);
                cell.setOpacity(1); cell.setScaleX(1); cell.setScaleY(1);
                FadeTransition ft2 = new FadeTransition(Duration.millis(200), cell);
                ft2.setFromValue(0); ft2.setToValue(1); ft2.play();
            });
            pt.play();
        }

        // Score popup
        showScorePopup("+" + points + " pts !");
    }

    private static void showScorePopup(String text) {
        Label popup = new Label(text);
        popup.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:#f7b731;"
                + "-fx-effect:dropshadow(gaussian,rgba(247,183,49,0.8),10,0,0,0);");
        popup.setMouseTransparent(true);
        gameBody.getChildren().add(popup);

        FadeTransition ft = new FadeTransition(Duration.millis(800), popup);
        ft.setFromValue(1); ft.setToValue(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(800), popup);
        tt.setFromY(0); tt.setToY(-40);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setOnFinished(e -> gameBody.getChildren().remove(popup));
        pt.play();
    }

    private static void showGameOver() {
        gameBody.getChildren().clear();

        VBox gameOverBox = new VBox(16);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setPadding(new Insets(40));
        gameOverBox.setStyle("-fx-background-color:#0a001a;");

        Label icon = new Label(score >= 100 ? "🏆" : "🎮");
        icon.setStyle("-fx-font-size:70;");
        ScaleTransition bounce = new ScaleTransition(Duration.millis(400), icon);
        bounce.setFromX(0.5); bounce.setToX(1.0);
        bounce.setFromY(0.5); bounce.setToY(1.0);
        bounce.setInterpolator(Interpolator.EASE_OUT); bounce.play();

        Label titleLbl = new Label(score >= 100 ? "🎉  Excellent !" : "🎮  Partie terminée !");
        titleLbl.setStyle("-fx-font-size:24; -fx-font-weight:800; -fx-text-fill:#f7b731;");

        Label scoreFinalLbl = new Label("Score final : " + score + " pts");
        scoreFinalLbl.setStyle("-fx-font-size:18; -fx-text-fill:white;");

        // Confettis si bon score
        HBox confetti = new HBox(8);
        confetti.setAlignment(Pos.CENTER);
        if (score >= 50) {
            for (String s : new String[]{"🍬","🍭","⭐","🎉","🍫","✨","🍬","🎊"}) {
                Label cl = new Label(s); cl.setStyle("-fx-font-size:22;");
                confetti.getChildren().add(cl);
            }
            for (int i = 0; i < confetti.getChildren().size(); i++) {
                javafx.scene.Node n = confetti.getChildren().get(i);
                TranslateTransition tt = new TranslateTransition(Duration.millis(500 + i*60), n);
                tt.setFromY(0); tt.setToY(-12);
                tt.setAutoReverse(true); tt.setCycleCount(Animation.INDEFINITE);
                tt.setDelay(Duration.millis(i * 80)); tt.play();
            }
        }

        // Fun fact via Numbers API
        Label funFactLbl = new Label("⏳  Chargement d'un fun fact...");
        funFactLbl.setStyle("-fx-font-size:12; -fx-text-fill:#fd9644; -fx-font-style:italic;");
        funFactLbl.setWrapText(true); funFactLbl.setMaxWidth(480);

        Button playAgainBtn = new Button("🔄  Rejouer");
        playAgainBtn.setStyle("-fx-background-color:linear-gradient(to right,#f7b731,#fd9644);"
                + "-fx-text-fill:#1a0033; -fx-font-size:14; -fx-font-weight:800;"
                + "-fx-padding:12 32 12 32; -fx-background-radius:25; -fx-cursor:hand; -fx-border-width:0;");
        playAgainBtn.setOnAction(e -> restartGame());

        gameOverBox.getChildren().addAll(icon, titleLbl, scoreFinalLbl, confetti, funFactLbl, playAgainBtn);
        gameBody.getChildren().add(gameOverBox);

        if (score >= 50) playVictorySound();
        fetchNumberFact(score, funFactLbl);
    }

    // ── API : Numbers API ────────────────────────────────────────

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
                        int idx = json.indexOf("\"text\":\"");
                        if (idx >= 0) {
                            int start = idx + 8;
                            int end = json.indexOf('"', start);
                            if (end > start) return "🔢  Fun fact : " + json.substring(start, end);
                        }
                    }
                } catch (Exception ignored) {}
                return "🔢  Fun fact : " + number + " — votre score, continuez comme ça !";
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            targetLabel.setText(task.getValue());
            targetLabel.setStyle("-fx-font-size:12; -fx-text-fill:#fd9644;");
        }));
        task.setOnFailed(e -> Platform.runLater(() ->
            targetLabel.setText("🔢  Score : " + number + " pts — bien joué !")));
        Thread t = new Thread(task, "numbers-api-candy"); t.setDaemon(true); t.start();
    }

    // ── Sons ─────────────────────────────────────────────────────

    private static void playSelectSound() {
        SoundGenerator.playCandySwap();
    }
    private static void playSwapSound() {
        SoundGenerator.playCandySwap();
    }
    private static void playExplosionSound() {
        SoundGenerator.playCandyExplosion();
    }
    private static void playVictorySound() {
        SoundGenerator.playCandyVictory();
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
