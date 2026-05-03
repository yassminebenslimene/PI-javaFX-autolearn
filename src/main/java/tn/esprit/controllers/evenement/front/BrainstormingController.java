package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import tn.esprit.services.GroqService;

import java.util.ArrayList;
import java.util.List;

/**
 * Espace Brainstorming IA — Hackathon
 * L'utilisateur décrit sa problématique et ses contraintes,
 * Groq génère des idées créatives et détaillées.
 */
public class BrainstormingController {

    private static final String ACCENT   = "#7c3aed";
    private static final String ACCENT2  = "#4f46e5";
    private static final String BG_DARK  = "#0f0a1e";
    private static final String BG_CARD  = "#1a1040";
    private static final String BG_INPUT = "#1e1550";

    // Tags de contraintes prédéfinis
    private static final String[][] TAGS = {
        {"🤖", "Intelligence Artificielle"},
        {"🌱", "Écologique / Durable"},
        {"👶", "Accessible aux enfants"},
        {"♿", "Inclusif / Accessibilité"},
        {"🌍", "Aligné avec les ODD"},
        {"📱", "Mobile First"},
        {"🔒", "Sécurité & Confidentialité"},
        {"💰", "Faible coût"},
        {"⚡", "Temps réel"},
        {"🤝", "Collaboratif"},
        {"🎓", "Éducatif"},
        {"🏥", "Santé & Bien-être"},
    };

    public static void show(Window owner) {
        double winW = owner.getWidth(), winH = owner.getHeight();
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        VBox modal = buildModal(dialog, winH);
        modal.setPrefWidth(720); modal.setMaxWidth(720);
        modal.setPrefHeight(winH * 0.92); modal.setMaxHeight(winH * 0.92);

        StackPane root = new StackPane(modal);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:rgba(0,0,0,0.72);");

        Runnable close = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> dialog.close()); ft.play();
        };
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });

        Scene scene = new Scene(root, winW, winH);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) close.run(); });
        dialog.setScene(scene);
        dialog.setX(owner.getX()); dialog.setY(owner.getY());

        root.setOpacity(0); modal.setTranslateY(50);
        dialog.show();

        FadeTransition fi = new FadeTransition(Duration.millis(250), root);
        fi.setFromValue(0); fi.setToValue(1);
        TranslateTransition su = new TranslateTransition(Duration.millis(300), modal);
        su.setFromY(50); su.setToY(0); su.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fi, su).play();
    }

    private static VBox buildModal(Stage dialog, double winH) {
        VBox modal = new VBox(0);
        modal.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background-radius:24;-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.5),40,0,0,10);");

        // Header
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 20, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + ACCENT + "," + ACCENT2 + ");-fx-background-radius:24 24 0 0;");
        Label icon = new Label("💡");
        icon.setStyle("-fx-font-size:32;");
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), icon);
        pulse.setFromX(1); pulse.setToX(1.15); pulse.setFromY(1); pulse.setToY(1.15);
        pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE); pulse.play();
        VBox ht = new VBox(3); HBox.setHgrow(ht, Priority.ALWAYS);
        Label t1 = new Label("💡  Espace Brainstorming IA");
        t1.setStyle("-fx-font-size:20;-fx-font-weight:800;-fx-text-fill:white;");
        Label t2 = new Label("Décrivez votre problématique — l'IA génère des idées innovantes  •  Powered by Groq");
        t2.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.8);");
        ht.getChildren().addAll(t1, t2);
        Button cb = new Button("✕");
        cb.setStyle("-fx-background-color:rgba(255,255,255,0.2);-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:700;-fx-background-radius:50%;-fx-min-width:34;-fx-min-height:34;-fx-max-width:34;-fx-max-height:34;-fx-cursor:hand;-fx-border-width:0;");
        cb.setOnAction(e -> dialog.close());
        header.getChildren().addAll(icon, ht, cb);

        // Body scrollable
        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:" + BG_DARK + ";");

        // Section 1: Thématique
        Label lblTheme = new Label("🎯  Thématique / Problématique du Hackathon");
        lblTheme.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#a78bfa;");
        TextArea fieldTheme = new TextArea();
        fieldTheme.setPromptText("Ex: Améliorer l'accès à l'éducation dans les zones rurales grâce à la technologie...");
        fieldTheme.setPrefRowCount(3);
        fieldTheme.setWrapText(true);
        fieldTheme.setStyle("-fx-control-inner-background:" + BG_INPUT + ";-fx-text-fill:white;-fx-prompt-text-fill:rgba(255,255,255,0.3);-fx-background-radius:10;-fx-border-color:" + ACCENT + "55;-fx-border-radius:10;-fx-border-width:1;-fx-font-size:13;-fx-padding:10;");

        // Section 2: Points à inclure
        Label lblPoints = new Label("✨  Points & Contraintes à inclure dans la solution");
        lblPoints.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#a78bfa;");
        Label lblPointsHint = new Label("Décrivez librement les caractéristiques souhaitées pour votre solution :");
        lblPointsHint.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.5);");
        TextArea fieldPoints = new TextArea();
        fieldPoints.setPromptText("Ex: Solution avec IA, écologique, utilisable par les enfants, alignée avec les ODD, mobile first, faible coût...");
        fieldPoints.setPrefRowCount(3);
        fieldPoints.setWrapText(true);
        fieldPoints.setStyle("-fx-control-inner-background:" + BG_INPUT + ";-fx-text-fill:white;-fx-prompt-text-fill:rgba(255,255,255,0.3);-fx-background-radius:10;-fx-border-color:" + ACCENT + "55;-fx-border-radius:10;-fx-border-width:1;-fx-font-size:13;-fx-padding:10;");

        // Section 3: Tags rapides
        Label lblTags = new Label("🏷️  Tags rapides (cliquez pour ajouter)");
        lblTags.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#a78bfa;");
        FlowPane tagsPane = new FlowPane(8, 8);
        List<String> selectedTags = new ArrayList<>();
        for (String[] tag : TAGS) {
            Button tagBtn = new Button(tag[0] + " " + tag[1]);
            tagBtn.setStyle("-fx-background-color:" + BG_INPUT + ";-fx-text-fill:rgba(255,255,255,0.7);-fx-font-size:11;-fx-padding:5 12 5 12;-fx-background-radius:20;-fx-border-color:" + ACCENT + "55;-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
            tagBtn.setOnAction(e -> {
                if (selectedTags.contains(tag[1])) {
                    selectedTags.remove(tag[1]);
                    tagBtn.setStyle("-fx-background-color:" + BG_INPUT + ";-fx-text-fill:rgba(255,255,255,0.7);-fx-font-size:11;-fx-padding:5 12 5 12;-fx-background-radius:20;-fx-border-color:" + ACCENT + "55;-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
                } else {
                    selectedTags.add(tag[1]);
                    tagBtn.setStyle("-fx-background-color:" + ACCENT + ";-fx-text-fill:white;-fx-font-size:11;-fx-font-weight:700;-fx-padding:5 12 5 12;-fx-background-radius:20;-fx-border-color:" + ACCENT + ";-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
                    // Ajouter au champ points
                    String current = fieldPoints.getText();
                    if (!current.contains(tag[1])) {
                        fieldPoints.setText(current.isEmpty() ? tag[1] : current + ", " + tag[1]);
                    }
                }
            });
            tagsPane.getChildren().add(tagBtn);
        }

        // Nombre d'idées
        Label lblNb = new Label("📊  Nombre d'idées à générer");
        lblNb.setStyle("-fx-font-size:13;-fx-font-weight:700;-fx-text-fill:#a78bfa;");
        HBox nbBox = new HBox(10); nbBox.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup tg = new ToggleGroup();
        for (int n : new int[]{3, 5, 7}) {
            ToggleButton tb = new ToggleButton(n + " idées");
            tb.setToggleGroup(tg);
            tb.setUserData(n);
            tb.setStyle("-fx-background-color:" + BG_INPUT + ";-fx-text-fill:rgba(255,255,255,0.7);-fx-font-size:12;-fx-padding:7 18 7 18;-fx-background-radius:20;-fx-border-color:" + ACCENT + "55;-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
            tb.selectedProperty().addListener((obs, ov, nv) -> {
                if (nv) tb.setStyle("-fx-background-color:" + ACCENT + ";-fx-text-fill:white;-fx-font-size:12;-fx-font-weight:700;-fx-padding:7 18 7 18;-fx-background-radius:20;-fx-border-color:" + ACCENT + ";-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
                else tb.setStyle("-fx-background-color:" + BG_INPUT + ";-fx-text-fill:rgba(255,255,255,0.7);-fx-font-size:12;-fx-padding:7 18 7 18;-fx-background-radius:20;-fx-border-color:" + ACCENT + "55;-fx-border-radius:20;-fx-border-width:1;-fx-cursor:hand;");
            });
            if (n == 5) tb.setSelected(true);
            nbBox.getChildren().add(tb);
        }

        // Bouton générer
        Button btnGenerer = new Button("🚀  Générer des idées avec l'IA");
        btnGenerer.setMaxWidth(Double.MAX_VALUE);
        btnGenerer.setStyle("-fx-background-color:linear-gradient(to right," + ACCENT + "," + ACCENT2 + ");-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:800;-fx-padding:14 0 14 0;-fx-background-radius:12;-fx-cursor:hand;-fx-border-width:0;-fx-effect:dropshadow(gaussian," + ACCENT + "88,12,0,0,4);");

        // Zone résultats
        VBox resultsBox = new VBox(12);
        resultsBox.setVisible(false);
        resultsBox.setManaged(false);

        // Label erreur
        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill:#f87171;-fx-font-size:12;");
        errLabel.setVisible(false);

        btnGenerer.setOnAction(e -> {
            String theme = fieldTheme.getText().trim();
            String points = fieldPoints.getText().trim();
            if (theme.isEmpty()) {
                errLabel.setText("⚠ Veuillez décrire la thématique / problématique.");
                errLabel.setVisible(true);
                return;
            }
            errLabel.setVisible(false);
            int nbIdees = tg.getSelectedToggle() != null ? (int) tg.getSelectedToggle().getUserData() : 5;
            generateIdeas(theme, points, nbIdees, btnGenerer, resultsBox);
        });

        body.getChildren().addAll(
            lblTheme, fieldTheme,
            lblPoints, lblPointsHint, fieldPoints,
            lblTags, tagsPane,
            lblNb, nbBox,
            errLabel, btnGenerer,
            resultsBox
        );

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:" + BG_DARK + ";-fx-background:" + BG_DARK + ";-fx-border-width:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(12, 24, 16, 24));
        footer.setStyle("-fx-background-color:#0a0618;-fx-background-radius:0 0 24 24;");
        Button fb = new Button("Fermer");
        fb.setStyle("-fx-background-color:" + BG_CARD + ";-fx-text-fill:#a78bfa;-fx-font-size:13;-fx-font-weight:600;-fx-padding:10 28 10 28;-fx-background-radius:25;-fx-border-color:" + ACCENT + ";-fx-border-radius:25;-fx-border-width:1.5;-fx-cursor:hand;");
        fb.setOnAction(e -> dialog.close());
        footer.getChildren().add(fb);

        modal.getChildren().addAll(header, scroll, footer);
        return modal;
    }

    private static void generateIdeas(String theme, String points, int nbIdees,
                                       Button btnGenerer, VBox resultsBox) {
        btnGenerer.setDisable(true);
        btnGenerer.setText("⏳  Génération en cours...");
        resultsBox.setVisible(false);
        resultsBox.setManaged(false);
        resultsBox.getChildren().clear();

        String systemPrompt = "Tu es un expert en innovation et en hackathons académiques. " +
            "Tu génères des idées de projets créatives, détaillées et réalisables. " +
            "Réponds UNIQUEMENT en JSON valide sans markdown avec cette structure exacte: " +
            "{\"idees\":[{\"titre\":\"...\",\"description\":\"...\",\"outils\":[\"...\"],\"impact\":\"...\",\"difficulte\":\"Facile|Moyen|Avancé\",\"emoji\":\"...\"}]}";

        String userPrompt = "Génère exactement " + nbIdees + " idées de projets innovants pour ce hackathon.\n\n" +
            "PROBLÉMATIQUE: " + theme + "\n\n" +
            (points.isEmpty() ? "" : "CONTRAINTES ET POINTS À INCLURE: " + points + "\n\n") +
            "Pour chaque idée, fournis:\n" +
            "- titre: nom accrocheur du projet (max 8 mots)\n" +
            "- description: explication claire et détaillée (3-4 phrases)\n" +
            "- outils: liste de 3-5 technologies/outils spécifiques à utiliser\n" +
            "- impact: bénéfice concret pour les utilisateurs (1-2 phrases)\n" +
            "- difficulte: Facile, Moyen ou Avancé\n" +
            "- emoji: un emoji représentatif\n\n" +
            "Sois créatif, pratique et aligné avec les contraintes demandées.";

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                GroqService groq = new GroqService();
                return groq.ask(systemPrompt, userPrompt);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            btnGenerer.setDisable(false);
            btnGenerer.setText("🚀  Générer des idées avec l'IA");
            String response = task.getValue();
            if (response != null && !response.isBlank()) {
                renderIdeas(response, resultsBox);
            } else {
                showError(resultsBox, "Erreur lors de la génération. Vérifiez votre connexion.");
            }
            resultsBox.setVisible(true);
            resultsBox.setManaged(true);
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            btnGenerer.setDisable(false);
            btnGenerer.setText("🚀  Générer des idées avec l'IA");
            showError(resultsBox, "Erreur de connexion à l'IA. Réessayez.");
            resultsBox.setVisible(true);
            resultsBox.setManaged(true);
        }));

        new Thread(task, "brainstorming-ai").start();
    }

    private static void renderIdeas(String json, VBox resultsBox) {
        try {
            // Clean JSON
            String clean = json.trim();
            if (clean.startsWith("\uFEFF")) clean = clean.substring(1);
            clean = clean.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            int start = clean.indexOf('{'); int end = clean.lastIndexOf('}');
            if (start >= 0 && end > start) clean = clean.substring(start, end + 1);

            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(clean).getAsJsonObject();
            com.google.gson.JsonArray idees = obj.getAsJsonArray("idees");

            // Titre section
            Label titre = new Label("✨  Idées générées par l'IA");
            titre.setStyle("-fx-font-size:15;-fx-font-weight:800;-fx-text-fill:#a78bfa;-fx-padding:0 0 4 0;");
            resultsBox.getChildren().add(titre);

            String[] colors = {ACCENT, "#4f46e5", "#0891b2", "#059669", "#d97706", "#dc2626", "#7c3aed"};

            for (int i = 0; i < idees.size(); i++) {
                com.google.gson.JsonObject idee = idees.get(i).getAsJsonObject();
                String emojiI  = idee.has("emoji")       ? idee.get("emoji").getAsString()       : "💡";
                String titreI  = idee.has("titre")        ? idee.get("titre").getAsString()        : "Idée " + (i+1);
                String descI   = idee.has("description")  ? idee.get("description").getAsString()  : "";
                String impactI = idee.has("impact")       ? idee.get("impact").getAsString()       : "";
                String diffI   = idee.has("difficulte")   ? idee.get("difficulte").getAsString()   : "Moyen";
                String color   = colors[i % colors.length];

                // Difficulté couleur
                String diffColor = switch (diffI.toLowerCase()) {
                    case "facile" -> "#10b981";
                    case "avancé", "avance" -> "#ef4444";
                    default -> "#f59e0b";
                };

                VBox card = new VBox(10);
                card.setPadding(new Insets(16, 18, 16, 18));
                card.setStyle("-fx-background-color:" + BG_CARD + ";-fx-background-radius:14;-fx-border-color:" + color + "66;-fx-border-radius:14;-fx-border-width:1.5;-fx-effect:dropshadow(gaussian," + color + "44,10,0,0,3);");

                // Header de la carte
                HBox cardHeader = new HBox(10); cardHeader.setAlignment(Pos.CENTER_LEFT);
                Label numLbl = new Label((i+1) + "");
                numLbl.setStyle("-fx-font-size:11;-fx-font-weight:800;-fx-text-fill:" + color + ";-fx-background-color:" + color + "22;-fx-background-radius:50%;-fx-padding:3 8 3 8;");
                Label emojiLbl = new Label(emojiI);
                emojiLbl.setStyle("-fx-font-size:22;");
                Label titreLbl = new Label(titreI);
                titreLbl.setStyle("-fx-font-size:14;-fx-font-weight:800;-fx-text-fill:white;");
                titreLbl.setWrapText(true); HBox.setHgrow(titreLbl, Priority.ALWAYS);
                Label diffLbl = new Label(diffI);
                diffLbl.setStyle("-fx-font-size:10;-fx-font-weight:700;-fx-text-fill:" + diffColor + ";-fx-background-color:" + diffColor + "22;-fx-background-radius:20;-fx-padding:3 10 3 10;");
                cardHeader.getChildren().addAll(numLbl, emojiLbl, titreLbl, diffLbl);

                // Description
                Label descLbl = new Label(descI);
                descLbl.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.85);-fx-line-spacing:3;");
                descLbl.setWrapText(true);

                // Outils
                HBox outilsRow = new HBox(6); outilsRow.setAlignment(Pos.CENTER_LEFT);
                Label outilsTitle = new Label("🛠 Outils:");
                outilsTitle.setStyle("-fx-font-size:11;-fx-font-weight:700;-fx-text-fill:" + color + ";");
                outilsRow.getChildren().add(outilsTitle);
                if (idee.has("outils") && idee.get("outils").isJsonArray()) {
                    for (com.google.gson.JsonElement outil : idee.getAsJsonArray("outils")) {
                        Label tag = new Label(outil.getAsString());
                        tag.setStyle("-fx-font-size:10;-fx-text-fill:white;-fx-background-color:" + color + "44;-fx-background-radius:20;-fx-padding:2 8 2 8;-fx-border-color:" + color + "66;-fx-border-radius:20;-fx-border-width:1;");
                        outilsRow.getChildren().add(tag);
                    }
                }

                // Impact
                HBox impactBox = new HBox(6); impactBox.setAlignment(Pos.CENTER_LEFT);
                impactBox.setPadding(new Insets(8, 12, 8, 12));
                impactBox.setStyle("-fx-background-color:" + color + "18;-fx-background-radius:8;-fx-border-color:" + color + "33;-fx-border-radius:8;-fx-border-width:1;");
                Label impactIcon = new Label("🎯 Impact:");
                impactIcon.setStyle("-fx-font-size:11;-fx-font-weight:700;-fx-text-fill:" + color + ";");
                Label impactLbl = new Label(impactI);
                impactLbl.setStyle("-fx-font-size:11;-fx-text-fill:rgba(255,255,255,0.75);");
                impactLbl.setWrapText(true); HBox.setHgrow(impactLbl, Priority.ALWAYS);
                impactBox.getChildren().addAll(impactIcon, impactLbl);

                card.getChildren().addAll(cardHeader, descLbl, outilsRow, impactBox);

                // Animation d'entrée
                card.setOpacity(0); card.setTranslateY(20);
                resultsBox.getChildren().add(card);
                FadeTransition ft = new FadeTransition(Duration.millis(400), card);
                ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(i * 100));
                TranslateTransition tt = new TranslateTransition(Duration.millis(400), card);
                tt.setFromY(20); tt.setToY(0); tt.setDelay(Duration.millis(i * 100));
                tt.setInterpolator(Interpolator.EASE_OUT);
                new ParallelTransition(ft, tt).play();
            }
        } catch (Exception e) {
            showError(resultsBox, "Erreur d'analyse de la réponse IA: " + e.getMessage());
        }
    }

    private static void showError(VBox box, String msg) {
        Label err = new Label("⚠ " + msg);
        err.setStyle("-fx-font-size:12;-fx-text-fill:#f87171;-fx-background-color:#2d0a0a;-fx-background-radius:10;-fx-padding:12 16 12 16;");
        err.setWrapText(true);
        box.getChildren().add(err);
    }
}
