package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

/**
 * Menu Déjeuner & Pause Café — modal in-memory, aucune DB.
 * Données hardcodées, animations séquentielles, palette violet/rose.
 */
public class MenuDejeunerController {

    // ── Données hardcodées ───────────────────────────────────────

    public static List<MenuItem> getDejeunerItems() {
        return List.of(
                new MenuItem("Salade César", "🥗", "Salade fraîche, croûtons dorés, parmesan râpé", "dejeuner"),
                new MenuItem("Pizza Margherita", "🍕", "Tomate fraîche, mozzarella fondante, basilic", "dejeuner"),
                new MenuItem("Wrap Poulet", "🥙", "Poulet grillé, légumes croquants, sauce yaourt", "dejeuner"),
                new MenuItem("Pasta Carbonara", "🍝", "Pâtes al dente, lardons fumés, crème onctueuse", "dejeuner"),
                new MenuItem("Sandwich Club", "🥪", "Jambon, fromage, tomate, laitue, mayo", "dejeuner"),
                new MenuItem("Bento Végétarien", "🍱", "Riz basmati, légumes sautés, tofu mariné", "dejeuner")
        );
    }

    public static List<MenuItem> getCafeItems() {
        return List.of(
                new MenuItem("Café Espresso", "☕", "Arabica sélectionné, torréfaction artisanale", "cafe"),
                new MenuItem("Thé à la Menthe", "🍵", "Menthe fraîche, sucre de canne, tradition", "cafe"),
                new MenuItem("Muffin Chocolat", "🧁", "Fondant au cœur, pépites de chocolat noir", "cafe"),
                new MenuItem("Croissant Beurre", "🥐", "Feuilleté croustillant, doré au four", "cafe"),
                new MenuItem("Jus d'Orange", "🍊", "Pressé frais, sans sucre ajouté", "cafe")
        );
    }

    // ── Point d'entrée ───────────────────────────────────────────

    public static void show(Window owner) {
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        VBox modal = buildModal(dialog, winH);
        modal.setPrefWidth(580);
        modal.setMaxWidth(580);
        modal.setPrefHeight(winH * 0.88);
        modal.setMaxHeight(winH * 0.88);

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

        // Animation entrée
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

    // ── Construction du modal ────────────────────────────────────

    private static VBox buildModal(Stage dialog, double winH) {
        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:white; -fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
                + "-fx-background-radius:20 20 0 0;");

        Label titre = new Label("🍽️  Menu & Pause Café");
        titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        HBox.setHgrow(titre, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180),
                    (javafx.scene.Node) closeBtn.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(ev -> dialog.close());
            ft.play();
        });
        header.getChildren().addAll(titre, closeBtn);

        // Body scrollable
        VBox body = new VBox(20);
        body.setPadding(new Insets(24, 28, 24, 28));
        body.setStyle("-fx-background-color:white;");

        // Section Déjeuner
        body.getChildren().add(buildSectionHeader("🍽️  Menu Déjeuner", "#667eea", "#764ba2"));
        VBox dejeunerGrid = new VBox(12);
        List<MenuItem> dejeunerItems = getDejeunerItems();
        for (int i = 0; i < dejeunerItems.size(); i++) {
            HBox card = buildMenuCard(dejeunerItems.get(i));
            card.setOpacity(0);
            card.setTranslateY(20);
            dejeunerGrid.getChildren().add(card);
            animateCard(card, i * 60);
        }
        body.getChildren().add(dejeunerGrid);

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:linear-gradient(to right,transparent,#c4b5fd,transparent);");
        body.getChildren().add(sep);

        // Section Pause Café
        body.getChildren().add(buildSectionHeader("☕  Pause Café & Snacks", "#f59e0b", "#d97706"));
        VBox cafeGrid = new VBox(12);
        List<MenuItem> cafeItems = getCafeItems();
        for (int i = 0; i < cafeItems.size(); i++) {
            HBox card = buildMenuCard(cafeItems.get(i));
            card.setOpacity(0);
            card.setTranslateY(20);
            cafeGrid.getChildren().add(card);
            animateCard(card, (dejeunerItems.size() + i) * 60);
        }
        body.getChildren().add(cafeGrid);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-width:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

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
        fermerBtn.setOnAction(e -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180),
                    (javafx.scene.Node) fermerBtn.getScene().getRoot());
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(ev -> dialog.close());
            ft.play();
        });
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, scroll, footer);
        return modal;
    }

    // ── Helpers UI ───────────────────────────────────────────────

    private static HBox buildSectionHeader(String text, String color1, String color2) {
        HBox box = new HBox();
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setStyle("-fx-background-color:linear-gradient(to right," + color1 + "22," + color2 + "11);"
                + "-fx-background-radius:12; -fx-border-color:" + color1 + "44;"
                + "-fx-border-radius:12; -fx-border-width:0 0 0 4;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:" + color1 + ";");
        box.getChildren().add(lbl);
        return box;
    }

    private static HBox buildMenuCard(MenuItem item) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-border-color:#f0ebff; -fx-border-radius:14; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.08),8,0,0,2);");

        // Emoji dans un cercle
        Label emojiLbl = new Label(item.emoji());
        emojiLbl.setStyle("-fx-font-size:28; -fx-background-color:#f5f3ff;"
                + "-fx-background-radius:50%; -fx-padding:10 12 10 12;"
                + "-fx-min-width:52; -fx-min-height:52; -fx-alignment:CENTER;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nomLbl = new Label(item.nom());
        nomLbl.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        Label descLbl = new Label(item.description());
        descLbl.setStyle("-fx-font-size:12; -fx-text-fill:#6b7280;");
        descLbl.setWrapText(true);
        info.getChildren().addAll(nomLbl, descLbl);

        // Badge catégorie
        String badgeColor = "dejeuner".equals(item.categorie()) ? "#667eea" : "#f59e0b";
        String badgeBg = "dejeuner".equals(item.categorie()) ? "#ede9fe" : "#fef3c7";
        Label badge = new Label("dejeuner".equals(item.categorie()) ? "🍽 Déjeuner" : "☕ Café");
        badge.setStyle("-fx-background-color:" + badgeBg + "; -fx-text-fill:" + badgeColor + ";"
                + "-fx-font-size:10; -fx-font-weight:700; -fx-background-radius:20;"
                + "-fx-padding:4 10 4 10;");

        card.getChildren().addAll(emojiLbl, info, badge);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#faf5ff; -fx-background-radius:14;"
                + "-fx-border-color:#c4b5fd; -fx-border-radius:14; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.18),12,0,0,4);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:white; -fx-background-radius:14;"
                + "-fx-border-color:#f0ebff; -fx-border-radius:14; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.08),8,0,0,2);"));

        return card;
    }

    private static void animateCard(HBox card, int delayMs) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), card);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), card);
        tt.setFromY(20); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(ft, tt);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }
}
