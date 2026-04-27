package tn.esprit.controllers.evenement.front;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import tn.esprit.entities.Evenement;

import java.util.ArrayList;
import java.util.List;

public class VendingMachineController {

    public static void show(Evenement ev, Window owner) {
        if (ev == null || owner == null) return;
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        VBox modal = new VBox(0);
        modal.setPrefWidth(620);
        modal.setMaxWidth(620);
        modal.setPrefHeight(winH * 0.88);
        modal.setMaxHeight(winH * 0.88);
        modal.setStyle("-fx-background-color:white; -fx-background-radius:20; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#ff6b9d,#c44dff); -fx-background-radius:20 20 0 0;");
        Label titre = new Label("Vending Machine");
        titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        HBox.setHgrow(titre, Priority.ALWAYS);
        Button closeBtn = new Button("X");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white; -fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%; -fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34; -fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(titre, closeBtn);

        VBox body = new VBox(20);
        body.setPadding(new Insets(24, 28, 24, 28));
        body.setStyle("-fx-background-color:white;");

        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(60, 60);
        Label spinnerMsg = new Label("Chargement du menu...");
        spinnerMsg.setStyle("-fx-font-size:14; -fx-text-fill:#6b7280;");
        VBox spinnerBox = new VBox(12, spinner, spinnerMsg);
        spinnerBox.setAlignment(Pos.CENTER);
        spinnerBox.setPadding(new Insets(40, 0, 40, 0));
        body.getChildren().add(spinnerBox);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-width:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 18, 24));
        footer.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:0 0 20 20; -fx-border-color:#eeeeee; -fx-border-width:1 0 0 0;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:13; -fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25; -fx-border-color:#d0d0d0; -fx-border-radius:25; -fx-border-width:1.5; -fx-cursor:hand;");
        fermerBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, scroll, footer);

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

        loadItemsAsync(body);
    }

    private static void loadItemsAsync(VBox body) {
        Task<List<ItemVending>> task = new Task<List<ItemVending>>() {
            @Override
            protected List<ItemVending> call() {
                return getFallbackItems();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> {
            body.getChildren().clear();
            renderGrid(task.getValue(), body);
        }));
        task.setOnFailed(e -> Platform.runLater(() -> {
            body.getChildren().clear();
            renderGrid(getFallbackItems(), body);
        }));
        new Thread(task, "vending-loader").start();
    }

    private static void renderGrid(List<ItemVending> items, VBox body) {
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.setPadding(new Insets(0));
        int col = 0, row = 0;
        for (ItemVending item : items) {
            grid.add(createBadge(item, body), col, row);
            col++;
            if (col >= 2) { col = 0; row++; }
        }
        body.getChildren().add(grid);
    }

    private static VBox createBadge(ItemVending item, VBox parentBody) {
        VBox badge = new VBox(8);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(20, 16, 20, 16));
        badge.setStyle("-fx-background-color:white; -fx-background-radius:16; -fx-border-color:#fce7f3; -fx-border-radius:16; -fx-border-width:2; -fx-effect:dropshadow(gaussian,rgba(255,107,157,0.15),12,0,0,3); -fx-cursor:hand;");
        Label emojiLbl = new Label(item.emoji());
        emojiLbl.setStyle("-fx-font-size:48;");
        Label nomLbl = new Label(item.nom());
        nomLbl.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        nomLbl.setWrapText(true); nomLbl.setMaxWidth(120); nomLbl.setAlignment(Pos.CENTER);
        Label prixLbl = new Label(String.format("%.2f DT", item.prixTND()));
        prixLbl.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#ff6b9d;");
        badge.getChildren().addAll(emojiLbl, nomLbl, prixLbl);
        badge.setOnMouseEntered(e -> badge.setStyle("-fx-background-color:#fce7f322; -fx-background-radius:16; -fx-border-color:#ff6b9d; -fx-border-radius:16; -fx-border-width:2; -fx-effect:dropshadow(gaussian,rgba(255,107,157,0.3),16,0,0,6); -fx-cursor:hand;"));
        badge.setOnMouseExited(e -> badge.setStyle("-fx-background-color:white; -fx-background-radius:16; -fx-border-color:#fce7f3; -fx-border-radius:16; -fx-border-width:2; -fx-effect:dropshadow(gaussian,rgba(255,107,157,0.15),12,0,0,3); -fx-cursor:hand;"));
        badge.setOnMouseClicked(e -> showReveal(item, parentBody));
        return badge;
    }

    private static void showReveal(ItemVending item, VBox parentBody) {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setStyle("-fx-background-color:white;");
        Label emojiLbl = new Label(item.emoji());
        emojiLbl.setStyle("-fx-font-size:80;");
        emojiLbl.setOpacity(0); emojiLbl.setScaleX(0.5); emojiLbl.setScaleY(0.5);
        Label nomLbl = new Label(item.nom());
        nomLbl.setStyle("-fx-font-size:24; -fx-font-weight:800; -fx-text-fill:#1e1e1e;");
        nomLbl.setOpacity(0);
        Label prixLbl = new Label(String.format("%.2f DT", item.prixTND()));
        prixLbl.setStyle("-fx-font-size:18; -fx-font-weight:700; -fx-text-fill:#ff6b9d;");
        prixLbl.setOpacity(0);
        Button rejouBtn = new Button("Rejouer");
        rejouBtn.setStyle("-fx-background-color:linear-gradient(to right,#ff6b9d,#c44dff); -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700; -fx-padding:12 28 12 28; -fx-background-radius:25; -fx-cursor:hand; -fx-border-width:0;");
        rejouBtn.setOpacity(0);
        rejouBtn.setOnAction(e -> { parentBody.getChildren().clear(); loadItemsAsync(parentBody); });
        box.getChildren().addAll(emojiLbl, nomLbl, prixLbl, rejouBtn);
        parentBody.getChildren().clear();
        parentBody.getChildren().add(box);
        try { SoundGenerator.playRevelation(); } catch (Exception ignored) {}
        FadeTransition fe = new FadeTransition(Duration.millis(400), emojiLbl);
        fe.setFromValue(0); fe.setToValue(1);
        ScaleTransition se = new ScaleTransition(Duration.millis(400), emojiLbl);
        se.setFromX(0.5); se.setFromY(0.5); se.setToX(1.0); se.setToY(1.0);
        se.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fn = new FadeTransition(Duration.millis(300), nomLbl);
        fn.setFromValue(0); fn.setToValue(1); fn.setDelay(Duration.millis(200));
        FadeTransition fp = new FadeTransition(Duration.millis(300), prixLbl);
        fp.setFromValue(0); fp.setToValue(1); fp.setDelay(Duration.millis(400));
        FadeTransition fb = new FadeTransition(Duration.millis(300), rejouBtn);
        fb.setFromValue(0); fb.setToValue(1); fb.setDelay(Duration.millis(700));
        new ParallelTransition(new ParallelTransition(fe, se), fn, fp, fb).play();
    }

    private static List<ItemVending> getFallbackItems() {
        List<ItemVending> items = new ArrayList<>();
        items.add(new ItemVending("Cafe Espresso", "\u2615", 1.5));
        items.add(new ItemVending("Eau minerale", "\uD83D\uDCA7", 0.8));
        items.add(new ItemVending("Jus Orange", "\uD83C\uDF4A", 2.0));
        items.add(new ItemVending("Soda Cola", "\uD83E\uDD64", 1.8));
        items.add(new ItemVending("Chocolat", "\uD83C\uDF6B", 1.2));
        items.add(new ItemVending("Chips", "\uD83E\uDD54", 1.0));
        items.add(new ItemVending("Yaourt", "\uD83E\uDD5B", 1.5));
        items.add(new ItemVending("Biscuits", "\uD83C\uDF6A", 0.9));
        return items;
    }
}