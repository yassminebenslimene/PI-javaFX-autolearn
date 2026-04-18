package tn.esprit.controllers.evenement.front;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;import tn.esprit.MainApp;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EquipeService;
import tn.esprit.services.EvenementService;
import tn.esprit.services.WeatherService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EvenementFrontController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private VBox eventsContainer;
    @FXML private HBox filterBar;
    @FXML private Label labelTotal;

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private final WeatherService weatherService = new WeatherService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private List<Evenement> allEvents;
    private String activeFilter = "Tous";

    // ── Couleurs par type ────────────────────────────────────────
    private static String typeColor(String type) {
        if (type == null) return "#7a6ad8";
        return switch (type) {
            case "Hackathon"  -> "#16a34a";   // vert foncé comme les cours
            case "Conference" -> "#4f46e5";   // bleu indigo comme les cours
            case "Workshop"   -> "#f59e0b";   // jaune
            default           -> "#7a6ad8";
        };
    }
    private static String typeBg(String type) {
        if (type == null) return "rgba(122,106,216,0.1)";
        return switch (type) {
            case "Hackathon"  -> "rgba(22,163,74,0.1)";
            case "Conference" -> "rgba(79,70,229,0.1)";
            case "Workshop"   -> "rgba(245,158,11,0.1)";
            default           -> "rgba(122,106,216,0.1)";
        };
    }
    private static String typeIcon(String type) {
        if (type == null) return "[E]";
        return switch (type) {
            case "Hackathon"  -> "[H]";
            case "Conference" -> "[C]";
            case "Workshop"   -> "[W]";
            default           -> "[E]";
        };
    }

    @FXML
    public void initialize() {
        allEvents = evenementService.getAll();
        buildFilterBar();
        renderGrid(allEvents);
    }

    // ── Filtres ──────────────────────────────────────────────────
    private void buildFilterBar() {
        filterBar.getChildren().clear();
        filterBar.setSpacing(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        String[] filters = {"Tous", "Hackathon", "Conference", "Workshop"};
        String[] colors  = {"#7a6ad8", "#16a34a", "#4f46e5", "#f59e0b"};

        for (int i = 0; i < filters.length; i++) {
            String f = filters[i];
            String c = colors[i];
            Button btn = new Button(f);
            boolean active = f.equals(activeFilter);
            btn.setStyle(active
                ? "-fx-background-color:" + c + "; -fx-text-fill:white; -fx-font-size:13;"
                  + "-fx-font-weight:700; -fx-padding:8 22 8 22; -fx-background-radius:25;"
                  + "-fx-cursor:hand; -fx-border-width:0;"
                : "-fx-background-color:transparent; -fx-text-fill:" + c + "; -fx-font-size:13;"
                  + "-fx-font-weight:600; -fx-padding:7 21 7 21; -fx-background-radius:25;"
                  + "-fx-cursor:hand; -fx-border-color:" + c + "; -fx-border-width:1.5;"
                  + "-fx-border-radius:25;");
            btn.setOnAction(e -> {
                activeFilter = f;
                buildFilterBar();
                List<Evenement> filtered = f.equals("Tous") ? allEvents
                        : allEvents.stream().filter(ev -> f.equals(ev.getType())).collect(Collectors.toList());
                renderGrid(filtered);
            });
            filterBar.getChildren().add(btn);
        }
    }

    // ── Grille de cards ──────────────────────────────────────────
    private void renderGrid(List<Evenement> list) {
        eventsContainer.getChildren().clear();

        if (labelTotal != null) {
            labelTotal.setText("  " + list.size() + " événement" + (list.size() > 1 ? "s" : ""));
            labelTotal.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:white;"
                    + "-fx-background-color:#7a6ad8; -fx-background-radius:20;"
                    + "-fx-padding:5 14 5 14;");
        }

        // GridPane 3 colonnes qui s'étale sur toute la largeur
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setStyle("-fx-padding:8 0 8 0;");

        // 3 colonnes égales
        for (int c = 0; c < 3; c++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        for (int i = 0; i < list.size(); i++) {
            VBox card = buildEventCard(list.get(i));
            card.setMaxWidth(Double.MAX_VALUE);
            card.setOpacity(0);
            card.setTranslateY(20);
            int delay = i * 55;
            FadeTransition ft = new FadeTransition(Duration.millis(320), card);
            ft.setFromValue(0); ft.setToValue(1);
            TranslateTransition tt = new TranslateTransition(Duration.millis(320), card);
            tt.setFromY(20); tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(Duration.millis(delay));
            pt.play();
            grid.add(card, i % 3, i / 3);
        }
        eventsContainer.getChildren().add(grid);
    }

    // ── Card individuelle ────────────────────────────────────────
    private VBox buildEventCard(Evenement ev) {
        boolean isCancelled = ev.isIsCanceled() || "Annulé".equals(ev.getStatus());
        boolean isPast = ev.getDateFin() != null && LocalDateTime.now().isAfter(ev.getDateFin());
        int nbEquipes = equipeService.countByEvenement(ev.getId());
        String color = typeColor(ev.getType());
        String bg    = typeBg(ev.getType());
        String icon  = typeIcon(ev.getType());

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                + "-fx-border-color:" + (isCancelled ? "#fca5a5" : "#eeeeee") + ";"
                + "-fx-border-radius:18; -fx-border-width:1;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4);");

        // Hover : scale + shadow
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.025); st.setToY(1.025); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                    + "-fx-border-color:" + color + ";"
                    + "-fx-border-radius:18; -fx-border-width:1.5;"
                    + "-fx-effect:dropshadow(gaussian," + color + "55,20,0,0,6);");
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
            card.setStyle("-fx-background-color:white; -fx-background-radius:18;"
                    + "-fx-border-color:" + (isCancelled ? "#fca5a5" : "#eeeeee") + ";"
                    + "-fx-border-radius:18; -fx-border-width:1;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,4);");
        });

        // ── Bande colorée en haut ────────────────────────────────
        HBox topBand = new HBox(10);
        topBand.setAlignment(Pos.CENTER_LEFT);
        topBand.setPadding(new Insets(14, 16, 14, 16));
        topBand.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:18 18 0 0;");

        // Icône type dans un cercle coloré — texte simple pour compatibilité
        String iconText = switch (ev.getType() != null ? ev.getType() : "") {
            case "Hackathon"  -> "H";
            case "Conference" -> "C";
            case "Workshop"   -> "W";
            default           -> "E";
        };
        Label iconLbl = new Label(iconText);
        iconLbl.setStyle("-fx-font-size:16; -fx-font-weight:900; -fx-background-color:" + color + ";"
                + "-fx-background-radius:50%; -fx-padding:10 13 10 13; -fx-text-fill:white;"
                + "-fx-min-width:44; -fx-min-height:44; -fx-alignment:CENTER;");

        VBox topInfo = new VBox(3);
        HBox.setHgrow(topInfo, Priority.ALWAYS);
        Label typeBadge = new Label(ev.getType() != null ? ev.getType().toUpperCase() : "");
        typeBadge.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-font-size:10;"
                + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 10 3 10;");
        if (isCancelled) {
            HBox badgeRow = new HBox(6, typeBadge);
            Label cancelBadge = new Label("X ANNULE");
            cancelBadge.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626; -fx-font-size:10;"
                    + "-fx-font-weight:700; -fx-background-radius:20; -fx-padding:3 10 3 10;");
            badgeRow.getChildren().add(cancelBadge);
            topInfo.getChildren().add(badgeRow);
        } else {
            topInfo.getChildren().add(typeBadge);
        }

        // Countdown
        long days = ev.getDateDebut() != null
                ? Math.max(0, ChronoUnit.DAYS.between(LocalDateTime.now(), ev.getDateDebut())) : 0;
        VBox timeBox = new VBox(2);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.setStyle("-fx-background-color:#fffbeb; -fx-background-radius:10;"
                + "-fx-border-color:#f59e0b; -fx-border-radius:10; -fx-border-width:1.5;"
                + "-fx-padding:6 12 6 12;");
        Label timeTitle = new Label("TEMPS");
        timeTitle.setStyle("-fx-font-size:9; -fx-font-weight:700; -fx-text-fill:#92400e;");
        Label timeDays = new Label(days + "j");
        timeDays.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:#f59e0b;");
        timeBox.getChildren().addAll(timeTitle, timeDays);

        topBand.getChildren().addAll(iconLbl, topInfo, timeBox);

        // ── Corps de la card ─────────────────────────────────────
        VBox body = new VBox(8);
        body.setPadding(new Insets(14, 16, 14, 16));

        Label titre = new Label(ev.getTitre());
        titre.setWrapText(true);
        titre.setStyle("-fx-font-size:15; -fx-font-weight:800; -fx-text-fill:"
                + (isCancelled ? "#999" : "#1e1e1e") + ";"
                + (isCancelled ? "-fx-strikethrough:true;" : ""));

        // Meta row avec icônes
        HBox meta = new HBox(0);
        meta.setAlignment(Pos.CENTER_LEFT);
        meta.setPadding(new Insets(10, 16, 12, 16));
        meta.setStyle("-fx-background-color:#fafafa; -fx-border-color:#f0f0f0;"
                + "-fx-border-width:1 0 0 0;");
        meta.setSpacing(0);

        if (ev.getDateDebut() != null) {
            meta.getChildren().add(metaItem("\uD83D\uDCC5", ev.getDateDebut().format(FMT), color));
            meta.getChildren().add(metaSep());
            meta.getChildren().add(metaItem("\uD83D\uDD50", ev.getDateDebut().format(FMT_TIME), color));
            meta.getChildren().add(metaSep());
        }
        if (ev.getLieu() != null) {
            meta.getChildren().add(metaItem("\uD83D\uDCCD", ev.getLieu(), color));
            meta.getChildren().add(metaSep());
        }
        meta.getChildren().add(metaItem("\uD83D\uDC65", nbEquipes + "/" + ev.getNbMax(), color));

        // Expand button → ouvre modal centrée
        Button expandBtn = new Button("▾  Voir les détails");
        expandBtn.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + color + ";"
                + "-fx-font-size:12; -fx-font-weight:700; -fx-background-radius:0 0 18 18;"
                + "-fx-padding:10 16 10 16; -fx-cursor:hand; -fx-border-width:0;"
                + "-fx-max-width:Infinity;");
        expandBtn.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(titre, meta, expandBtn);

        // ── Panneau détails (caché) ──────────────────────────────
        VBox details = new VBox(14);
        details.setPadding(new Insets(0, 16, 16, 16));
        details.setVisible(false);
        details.setManaged(false);
        buildDetails(details, ev, isCancelled, isPast, nbEquipes, color, bg);

        expandBtn.setOnAction(e -> showDetailsModal(ev, isCancelled, isPast, nbEquipes, color, bg));

        card.getChildren().addAll(topBand, body);
        return card;
    }

    private HBox metaChip(String icon, String text) {
        HBox chip = new HBox(4);
        chip.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size:11;");
        Label tx = new Label(text);
        tx.setStyle("-fx-font-size:11; -fx-text-fill:#666;");
        chip.getChildren().addAll(ic, tx);
        return chip;
    }

    /** Item meta avec emoji — police Segoe UI Emoji pour Windows */
    private VBox metaItem(String symbol, String text, String color) {
        VBox item = new VBox(4);
        item.setAlignment(Pos.CENTER);
        item.setPadding(new Insets(6, 10, 6, 10));
        HBox.setHgrow(item, Priority.ALWAYS);

        Label iconLbl = new Label(symbol);
        // Force la couleur noire pour que les emojis soient visibles (pas blancs)
        iconLbl.setStyle("-fx-font-family:'Segoe UI Emoji'; -fx-font-size:20; -fx-text-fill:#000000;");

        Label tx = new Label(text);
        tx.setStyle("-fx-font-size:11; -fx-font-weight:500; -fx-text-fill:#444;");
        tx.setTextOverrun(OverrunStyle.ELLIPSIS);
        tx.setMaxWidth(90);
        item.getChildren().addAll(iconLbl, tx);
        return item;
    }

    private Label metaSep() {
        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill:#ddd; -fx-font-size:16; -fx-padding:0 2 0 2;");
        return sep;
    }

    // ── Modal de détails (même style que calendrier) ─────────────
    private void showDetailsModal(Evenement ev, boolean isCancelled, boolean isPast,
                                  int nbEquipes, String color, String bg) {
        javafx.stage.Window owner = eventsContainer.getScene().getWindow();
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        VBox modal = new VBox(0);
        modal.setPrefWidth(560);
        modal.setMaxWidth(560);
        modal.setPrefHeight(winH * 0.85);
        modal.setMaxHeight(winH * 0.85);
        modal.setStyle("-fx-background-color:white; -fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        // Header gradient couleur du type
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right," + color + ",#764ba2);"
                + "-fx-background-radius:20 20 0 0;");
        Label titre = new Label(typeIcon(ev.getType()) + "  " + ev.getTitre());
        titre.setStyle("-fx-font-size:19; -fx-font-weight:800; -fx-text-fill:white;");
        titre.setWrapText(true);
        titre.setMaxWidth(440);
        HBox.setHgrow(titre, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        header.getChildren().addAll(titre, closeBtn);

        // Déclarer dialog avant buildDetails pour pouvoir le passer comme Runnable
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:white;");
        buildDetails(body, ev, isCancelled, isPast, nbEquipes, color, bg, dialog::close);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-width:0; -fx-padding:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

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
        closeBtn.setOnAction(e -> close.run());
        fermerBtn.setOnAction(e -> close.run());
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });

        javafx.scene.Scene scene = new javafx.scene.Scene(root, winW, winH);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
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

    // ── Détails expand ───────────────────────────────────────────
    private void buildDetails(VBox details, Evenement ev, boolean isCancelled,
                              boolean isPast, int nbEquipes, String color, String bg) {
        buildDetails(details, ev, isCancelled, isPast, nbEquipes, color, bg, null);
    }

    private void buildDetails(VBox details, Evenement ev, boolean isCancelled,
                              boolean isPast, int nbEquipes, String color, String bg,
                              Runnable onClose) {
        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#eeeeee;");
        details.getChildren().add(sep);

        // Description
        details.getChildren().add(sectionTitle(">> A propos", color));
        Label desc = new Label(ev.getDescription() != null ? ev.getDescription() : "Aucune description.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size:12; -fx-text-fill:#555; -fx-line-spacing:3;");
        details.getChildren().add(desc);

        // ── Météo ────────────────────────────────────────────────
        details.getChildren().add(sectionTitle("~ Meteo a Tunis", color));
        VBox weatherBox = new VBox(8);
        weatherBox.setPadding(new Insets(12));
        weatherBox.setStyle("-fx-background-color:linear-gradient(to right,#e0f2fe,#bae6fd);"
                + "-fx-background-radius:12; -fx-border-color:#0ea5e9;"
                + "-fx-border-radius:12; -fx-border-width:0 0 0 4;");
        Label loadingLbl = new Label("⏳ Chargement...");
        loadingLbl.setStyle("-fx-font-size:12; -fx-text-fill:#0c4a6e;");
        weatherBox.getChildren().add(loadingLbl);
        details.getChildren().add(weatherBox);

        javafx.concurrent.Task<Map<String, Object>> wTask = new javafx.concurrent.Task<>() {
            @Override protected Map<String, Object> call() {
                return weatherService.getWeatherForEvent("Tunis,TN", ev.getDateDebut());
            }
        };
        wTask.setOnSucceeded(e -> {
            Map<String, Object> w = wTask.getValue();
            weatherBox.getChildren().clear();
            if (w == null || !Boolean.TRUE.equals(w.get("available"))) {
                weatherBox.getChildren().add(errLabel("⚠ Données météo indisponibles"));
                return;
            }
            boolean isForecast = Boolean.TRUE.equals(w.get("is_forecast"));
            Label band = new Label(isForecast
                    ? ">> Prevision pour le jour J a Tunis"
                    : "i  Meteo actuelle (evenement > 5 jours)");
            band.setWrapText(true);
            band.setStyle("-fx-font-size:11; -fx-font-weight:600;"
                    + "-fx-text-fill:" + (isForecast ? "#166534" : "#92400e") + ";"
                    + "-fx-background-color:" + (isForecast ? "rgba(34,197,94,0.15)" : "rgba(251,191,36,0.15)") + ";"
                    + "-fx-background-radius:8; -fx-padding:5 10 5 10;");
            HBox mainRow = new HBox(14);
            mainRow.setAlignment(Pos.CENTER_LEFT);
            Label emojiLbl = new Label(weatherService.getWeatherEmoji((String) w.get("icon")));
            emojiLbl.setStyle("-fx-font-size:36;");
            VBox tempBox = new VBox(2);
            Label tempLbl = new Label(w.get("temperature") + "°C");
            tempLbl.setStyle("-fx-font-size:24; -fx-font-weight:800; -fx-text-fill:#0c4a6e;");
            Label descLbl = new Label((String) w.get("description"));
            descLbl.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#075985;");
            HBox metaRow = new HBox(12);
            metaRow.getChildren().addAll(
                wMeta("🌡 " + w.get("feels_like") + "°C"),
                wMeta("💨 " + w.get("wind_speed") + " km/h"),
                wMeta("💧 " + w.get("humidity") + "%")
            );
            tempBox.getChildren().addAll(tempLbl, descLbl, metaRow);
            mainRow.getChildren().addAll(emojiLbl, tempBox);
            int temp = ((Number) w.get("temperature")).intValue();
            String descStr = ((String) w.get("description")).toLowerCase();
            String advice = !isForecast ? "i  Prevision disponible 5 jours avant l'evenement."
                    : temp > 25 ? "Soleil - Il fera chaud ! Hydratez-vous."
                    : temp < 10 ? "Froid - Il fera froid ! Couvrez-vous."
                    : descStr.contains("rain") ? "Pluie prevue ! Prenez un parapluie."
                    : "Meteo agreable pour l'evenement !";
            Label adviceLbl = new Label(advice);
            adviceLbl.setWrapText(true);
            adviceLbl.setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-text-fill:#0c4a6e;"
                    + "-fx-background-color:rgba(255,255,255,0.75); -fx-background-radius:8; -fx-padding:7 10 7 10;");
            weatherBox.getChildren().addAll(band, mainRow, adviceLbl);
        });
        wTask.setOnFailed(e -> {
            weatherBox.getChildren().clear();
            weatherBox.getChildren().add(errLabel("⚠ Météo indisponible"));
        });
        new Thread(wTask).start();

        // Places disponibles
        int spots = ev.getNbMax() - nbEquipes;
        details.getChildren().add(sectionTitle("o  Places disponibles", color));
        HBox spotsBox = new HBox();
        spotsBox.setPadding(new Insets(10, 14, 10, 14));
        spotsBox.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:10;"
                + "-fx-border-color:" + color + "; -fx-border-radius:10; -fx-border-width:0 0 0 4;");
        Label spotsLbl = new Label(spots + " places restantes sur " + ev.getNbMax() + " au total");
        spotsLbl.setStyle("-fx-font-size:13; -fx-font-weight:600; -fx-text-fill:#1e4d8c;");
        spotsBox.getChildren().add(spotsLbl);
        details.getChildren().add(spotsBox);

        // Équipes participantes
        List<Equipe> equipes = equipeService.getByEvenement(ev.getId());
        details.getChildren().add(sectionTitle(">> Equipes participantes (" + equipes.size() + ")", color));
        if (equipes.isEmpty()) {
            details.getChildren().add(errLabel("Aucune équipe inscrite. Soyez le premier !"));
        } else {
            FlowPane teamsFlow = new FlowPane(10, 10);
            for (Equipe eq : equipes) {
                VBox tc = new VBox(4);
                tc.setAlignment(Pos.CENTER);
                tc.setPadding(new Insets(10));
                tc.setStyle("-fx-background-color:white; -fx-background-radius:12;"
                        + "-fx-border-color:#eeeeee; -fx-border-radius:12; -fx-border-width:1;"
                        + "-fx-min-width:100;");
                Label av = new Label("👤");
                av.setStyle("-fx-font-size:22; -fx-background-color:" + bg + ";"
                        + "-fx-background-radius:50%; -fx-padding:6 8 6 8;");
                Label nm = new Label(eq.getNom());
                nm.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
                int m = equipeService.countMembres(eq.getId());
                Label ml = new Label(m + " membres");
                ml.setStyle("-fx-font-size:10; -fx-text-fill:#888;");
                tc.getChildren().addAll(av, nm, ml);
                teamsFlow.getChildren().add(tc);
            }
            details.getChildren().add(teamsFlow);
        }

        // Bouton action
        if (isCancelled) {
            details.getChildren().add(buildBanner("#fee2e2","#fca5a5","#dc2626","X","Evenement annule","Aucune inscription acceptee."));
        } else if (isPast) {
            details.getChildren().add(buildBanner("#e8eaf6","#c5cae9","#3949ab","[]","Evenement termine","Les inscriptions sont closes."));
        } else if (spots > 0) {
            Button btn = new Button(">>  Participer a cet evenement");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-font-size:13;"
                    + "-fx-font-weight:700; -fx-padding:12 0 12 0; -fx-background-radius:12;"
                    + "-fx-cursor:hand; -fx-border-width:0;");
            btn.setOnAction(e -> {
                if (onClose != null) onClose.run();
                try { MainApp.showJoinEvent(ev); } catch (Exception ex) { ex.printStackTrace(); }
            });
            details.getChildren().add(btn);
        } else {
            details.getChildren().add(errLabel("❌ Complet — aucune place disponible"));
        }
    }

    private Label sectionTitle(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:" + color + ";");
        return l;
    }
    private Label errLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#888;");
        return l;
    }
    private Label wMeta(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11; -fx-text-fill:#0c4a6e;");
        return l;
    }
    private Label metaLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#666;");
        return l;
    }
    private Label weatherMeta(String text) { return wMeta(text); }

    private HBox buildBanner(String bg, String border, String fg, String icon, String title, String msg) {
        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER);
        banner.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:12;"
                + "-fx-border-color:" + border + "; -fx-border-radius:12; -fx-border-width:1; -fx-padding:16;");
        VBox c = new VBox(4);
        c.setAlignment(Pos.CENTER);
        Label ic = new Label(icon); ic.setStyle("-fx-font-size:22; -fx-text-fill:" + fg + ";");
        Label ti = new Label(title); ti.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:" + fg + ";");
        Label ms = new Label(msg); ms.setStyle("-fx-font-size:12; -fx-text-fill:#666;");
        c.getChildren().addAll(ic, ti, ms);
        banner.getChildren().add(c);
        return banner;
    }

    @FXML private void onVoirCalendrier() {
        try { MainApp.showCalendrierEvenements(); } catch (Exception e) { e.printStackTrace(); }
    }
    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
