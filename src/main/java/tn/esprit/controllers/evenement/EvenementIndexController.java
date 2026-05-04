package tn.esprit.controllers.evenement;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.GroqService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EvenementIndexController implements Initializable {

    @FXML private VBox tableRows;
    @FXML private ComboBox<String> filterCombo;
    @FXML private HBox statsContainer;

    private final EvenementService service = new EvenementService();
    private final GroqService groqService = new GroqService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<Evenement> allEvents = new ArrayList<>();
    private String currentFilter = "Tous les types d'événements";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterCombo.getItems().addAll("Tous les types d'événements", "Hackathon", "Conference", "Workshop");
        filterCombo.setValue("Tous les types d'événements");
        filterCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill:#2d3748; -fx-font-size:13;");
            }
        });
        allEvents = service.getAll();
        updateStats();
        loadTable();
    }

    @FXML private void onFilterChanged() {
        currentFilter = filterCombo.getValue();
        updateStats();
        loadTable();
    }

    private List<Evenement> getFiltered() {
        if (currentFilter == null || currentFilter.equals("Tous les types d'événements")) return allEvents;
        return allEvents.stream()
            .filter(e -> e.getType() != null && e.getType().equalsIgnoreCase(currentFilter))
            .collect(Collectors.toList());
    }

    private void updateStats() {
        statsContainer.getChildren().clear();
        if (currentFilter.equals("Tous les types d'événements")) {
            Map<String, List<Evenement>> byType = new LinkedHashMap<>();
            byType.put("Hackathon", new ArrayList<>());
            byType.put("Conference", new ArrayList<>());
            byType.put("Workshop", new ArrayList<>());
            for (Evenement e : allEvents)
                if (e.getType() != null) byType.computeIfAbsent(e.getType(), k -> new ArrayList<>()).add(e);
            for (Map.Entry<String, List<Evenement>> entry : byType.entrySet())
                if (!entry.getValue().isEmpty()) statsContainer.getChildren().add(buildStatCard(entry.getKey(), entry.getValue()));
        } else {
            List<Evenement> filtered = getFiltered();
            if (!filtered.isEmpty()) statsContainer.getChildren().add(buildStatCard(currentFilter, filtered));
        }
    }

    private VBox buildStatCard(String type, List<Evenement> events) {
        String color = typeColor(type);
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color:rgba(102,126,234,0.12); -fx-background-radius:12; -fx-border-color:" + color + "; -fx-border-radius:12; -fx-border-width:1;");
        Label lType = new Label(type);
        lType.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13; -fx-font-weight:bold;");
        Label lCount = new Label(events.size() + " événements");
        lCount.setStyle("-fx-text-fill:white; -fx-font-size:20; -fx-font-weight:bold;");
        long passes = events.stream().filter(e -> "Passé".equals(e.computeStatus())).count();
        long planif = events.stream().filter(e -> "Plannifié".equals(e.computeStatus())).count();
        Label lStats = new Label("✓ " + passes + " passés  •  " + planif + " planifiés");
        lStats.setStyle("-fx-text-fill:rgba(255,255,255,0.65); -fx-font-size:11;");
        card.getChildren().addAll(lType, lCount, lStats);
        return card;
    }

    // ── Report generation ────────────────────────────────────────────────────

    @FXML private void onGenerateAnalysis()        { generateReport("Analyse"); }
    @FXML private void onGenerateRecommendations() { generateReport("Recommandations"); }
    @FXML private void onGenerateSuggestions()     { generateReport("Suggestions"); }

    private void generateReport(String type) {
        List<Evenement> filtered = getFiltered();
        if (filtered.isEmpty()) { showAlert("Aucun événement", "Sélectionnez un filtre avec des événements."); return; }
        Button[] btns = getReportButtons();
        for (Button b : btns) if (b != null) b.setDisable(true);

        long total   = filtered.size();
        long passes  = filtered.stream().filter(e -> "Passé".equals(e.computeStatus())).count();
        long encours = filtered.stream().filter(e -> "En cours".equals(e.computeStatus())).count();
        long planif  = filtered.stream().filter(e -> "Plannifié".equals(e.computeStatus())).count();
        long annules = filtered.stream().filter(e -> "Annulé".equals(e.computeStatus())).count();
        String typeLabel = currentFilter.equals("Tous les types d'événements") ? "tous types" : currentFilter;

        String stats = "Données: " + total + " événements (" + typeLabel + ") — " +
                       passes + " passés, " + encours + " en cours, " + planif + " planifiés, " + annules + " annulés.";
        String prompt = switch (type) {
            case "Analyse"         -> stats + " Génère une analyse globale synthétique (pas d'énumération des événements un par un).";
            case "Recommandations" -> stats + " Propose 4 nouveaux événements futurs pertinents à organiser avec un planning suggéré sur l'année.";
            case "Suggestions"     -> stats + " Propose des idées innovantes pour améliorer et diversifier les événements futurs.";
            default -> stats;
        };
        String systemMsg = buildSystemMessage(type);

        CompletableFuture.supplyAsync(() -> {
            try { return groqService.ask(systemMsg, prompt); }
            catch (Exception e) { return null; }
        }).thenAccept(report -> Platform.runLater(() -> {
            for (Button b : btns) if (b != null) b.setDisable(false);
            if (report != null && !report.isBlank()) showReportDialog(type, report);
            else showAlert("Erreur IA", "Impossible de générer le rapport. Vérifiez votre connexion.");
        }));
    }

    private String buildSystemMessage(String type) {
        return switch (type) {
            case "Analyse" ->
                "Tu es un analyste expert en événements académiques. " +
                "Réponds UNIQUEMENT en JSON valide (sans markdown, sans texte avant/après) avec cette structure:\n" +
                "{\"kpis\":[{\"label\":\"Taux de réalisation\",\"value\":\"XX%\",\"icon\":\"✅\",\"color\":\"#10b981\"}," +
                "{\"label\":\"Taux d'annulation\",\"value\":\"XX%\",\"icon\":\"❌\",\"color\":\"#e11d48\"}," +
                "{\"label\":\"Diversité types\",\"value\":\"X types\",\"icon\":\"🎯\",\"color\":\"#6366f1\"}," +
                "{\"label\":\"Engagement estimé\",\"value\":\"XX%\",\"icon\":\"📊\",\"color\":\"#f59e0b\"}]," +
                "\"points_forts\":[\"...\",\"...\",\"...\"]," +
                "\"points_faibles\":[\"...\",\"...\"]," +
                "\"tendances\":[\"...\",\"...\"]," +
                "\"conclusion\":\"...\"}\n" +
                "Calcule les KPIs à partir des données fournies. Sois concis (max 15 mots par point).";
            case "Recommandations" ->
                "Tu es un expert en planification d'événements académiques. " +
                "Réponds UNIQUEMENT en JSON valide (sans markdown, sans texte avant/après) avec cette structure:\n" +
                "{\"evenements_suggeres\":[{\"titre\":\"...\",\"type\":\"Hackathon|Conference|Workshop\",\"periode\":\"Mai 2026\",\"objectif\":\"...\",\"public\":\"...\",\"duree\":\"...\"}]," +
                "\"planning_annuel\":[{\"mois\":\"...\",\"evenement\":\"...\",\"priorite\":\"haute|moyenne|basse\"}]," +
                "\"conseil_global\":\"...\"}\n" +
                "evenements_suggeres: exactement 4 événements. planning_annuel: exactement 6 entrées. Sois concis et pratique.";
            case "Suggestions" ->
                "Tu es un expert en innovation événementielle académique. " +
                "Réponds UNIQUEMENT en JSON valide (sans markdown, sans texte avant/après) avec cette structure:\n" +
                "{\"innovations\":[{\"titre\":\"...\",\"description\":\"...\",\"impact\":\"fort|moyen|faible\",\"facilite\":\"facile|moyen|complexe\"}]," +
                "\"formats_nouveaux\":[{\"format\":\"...\",\"avantage\":\"...\"}]," +
                "\"themes_tendance\":[\"...\",\"...\",\"...\",\"...\"]," +
                "\"quick_wins\":[\"...\",\"...\",\"...\"]}\n" +
                "innovations: 4 idées. formats_nouveaux: 3 formats. Sois créatif et concis (max 15 mots par item).";
            default -> "Tu es un expert en événements académiques. Réponds en JSON.";
        };
    }

    private Button[] getReportButtons() {
        try {
            Button b1 = (Button) tableRows.getScene().lookup("#btnAnalyse");
            Button b2 = (Button) tableRows.getScene().lookup("#btnRecommandations");
            Button b3 = (Button) tableRows.getScene().lookup("#btnSuggestions");
            return new Button[]{b1, b2, b3};
        } catch (Exception e) { return new Button[0]; }
    }

    // ── Dialog ───────────────────────────────────────────────────────────────

    private void showReportDialog(String type, String content) {
        String[] icons = {"📈", "💡", "✨"};
        String[] types = {"Analyse", "Recommandations", "Suggestions"};
        int idx = Arrays.asList(types).indexOf(type);
        String icon = idx < 0 ? "📊" : icons[idx];
        String[] gradients = {
            "linear-gradient(to right,#667eea,#764ba2)",
            "linear-gradient(to right,#f093fb,#f5576c)",
            "linear-gradient(to right,#00f2fe,#4facfe)"
        };
        String gradient = idx < 0 ? gradients[0] : gradients[idx];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(icon + " Rapport " + type);

        VBox root = new VBox(0);
        root.setPrefWidth(820);
        root.setMaxWidth(820);

        // Header
        VBox header = new VBox(6);
        header.setPadding(new Insets(22, 28, 18, 28));
        header.setStyle("-fx-background-color:" + gradient + "; -fx-background-radius:8 8 0 0;");
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label lIcon = new Label(icon);
        lIcon.setStyle("-fx-font-size:24;");
        VBox titleBox = new VBox(3);
        Label lTitle = new Label("Rapport " + type);
        lTitle.setStyle("-fx-text-fill:white; -fx-font-size:18; -fx-font-weight:bold;");
        Label lSub = new Label("AutoLearn  •  " + currentFilter + "  •  " + getFiltered().size() + " événements");
        lSub.setStyle("-fx-text-fill:rgba(255,255,255,0.8); -fx-font-size:11;");
        titleBox.getChildren().addAll(lTitle, lSub);
        titleRow.getChildren().addAll(lIcon, titleBox);
        header.getChildren().add(titleRow);

        // Body
        VBox body = new VBox(14);
        body.setStyle("-fx-background-color:#f0f2ff;");
        body.setPadding(new Insets(20, 24, 20, 24));

        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(extractJson(content)).getAsJsonObject();
            renderJsonReport(type, json, body);
        } catch (Exception e) {
            renderTextFallback(content, body);
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(500);
        scroll.setStyle("-fx-background-color:#f0f2ff; -fx-background:#f0f2ff; -fx-border-width:0;");

        // Footer
        HBox footer = new HBox(10);
        footer.setPadding(new Insets(14, 24, 14, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0 transparent transparent transparent; -fx-border-width:1 0 0 0;");
        Label lInfo = new Label("Généré par AutoLearn AI  •  " + java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        lInfo.setStyle("-fx-text-fill:#a0aec0; -fx-font-size:10;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnPdf = new Button("⬇  Télécharger PDF");
        btnPdf.setStyle("-fx-background-color:" + gradient + "; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:bold; -fx-padding:10 20 10 20; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnPdf.setOnAction(ev -> exportReportPdf(type, icon, content));
        footer.getChildren().addAll(lInfo, spacer, btnPdf);

        root.getChildren().addAll(header, scroll, footer);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-padding:0;");
        dialog.getDialogPane().setPrefWidth(840);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) closeBtn.setStyle("-fx-background-color:#e2e8f0; -fx-text-fill:#4a5568; -fx-font-size:12; -fx-padding:8 18 8 18; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        dialog.showAndWait();
    }

    private String extractJson(String raw) {
        String s = raw.trim();
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start >= 0 && end > start) return s.substring(start, end + 1);
        return s;
    }

    private void renderJsonReport(String type, com.google.gson.JsonObject json, VBox body) {
        switch (type) {
            case "Analyse"         -> renderAnalyse(json, body);
            case "Recommandations" -> renderRecommandations(json, body);
            case "Suggestions"     -> renderSuggestions(json, body);
            default                -> renderTextFallback(json.toString(), body);
        }
    }

    // ── Analyse renderer ─────────────────────────────────────────────────────
    private void renderAnalyse(com.google.gson.JsonObject json, VBox body) {
        // KPI cards
        if (json.has("kpis")) {
            HBox kpiRow = new HBox(12);
            kpiRow.setAlignment(Pos.CENTER_LEFT);
            for (com.google.gson.JsonElement el : json.getAsJsonArray("kpis")) {
                com.google.gson.JsonObject kpi = el.getAsJsonObject();
                String color = kpi.has("color") ? kpi.get("color").getAsString() : "#667eea";
                String lbl   = kpi.has("label") ? kpi.get("label").getAsString() : "";
                String val   = kpi.has("value") ? kpi.get("value").getAsString() : "";
                String ico   = kpi.has("icon")  ? kpi.get("icon").getAsString()  : "📊";
                VBox card = new VBox(4);
                card.setPadding(new Insets(14, 16, 14, 16));
                card.setPrefWidth(175);
                card.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-color:" + color +
                              "; -fx-border-radius:12; -fx-border-width:2; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");
                Label lIco = new Label(ico); lIco.setStyle("-fx-font-size:18;");
                Label lVal = new Label(val); lVal.setStyle("-fx-text-fill:" + color + "; -fx-font-size:20; -fx-font-weight:bold;");
                Label lLbl = new Label(lbl); lLbl.setStyle("-fx-text-fill:#718096; -fx-font-size:10;"); lLbl.setWrapText(true);
                card.getChildren().addAll(lIco, lVal, lLbl);
                kpiRow.getChildren().add(card);
            }
            body.getChildren().add(kpiRow);
        }
        // Points forts / faibles
        HBox pfRow = new HBox(12);
        if (json.has("points_forts"))  pfRow.getChildren().add(buildListCard("✅ Points forts",       json.getAsJsonArray("points_forts"),  "#10b981", "#f0fdf4"));
        if (json.has("points_faibles")) pfRow.getChildren().add(buildListCard("⚠️ Points à améliorer", json.getAsJsonArray("points_faibles"), "#f59e0b", "#fffbeb"));
        if (!pfRow.getChildren().isEmpty()) body.getChildren().add(pfRow);
        // Tendances
        if (json.has("tendances")) body.getChildren().add(buildListCard("📈 Tendances observées", json.getAsJsonArray("tendances"), "#667eea", "#eef2ff"));
        // Conclusion
        if (json.has("conclusion")) {
            VBox concl = new VBox(6);
            concl.setPadding(new Insets(14, 16, 14, 16));
            concl.setStyle("-fx-background-color:linear-gradient(to right,rgba(102,126,234,0.1),rgba(118,75,162,0.1)); -fx-background-radius:10; -fx-border-color:#667eea; -fx-border-radius:10; -fx-border-width:1;");
            Label lc = new Label("💬 Conclusion"); lc.setStyle("-fx-text-fill:#667eea; -fx-font-size:12; -fx-font-weight:bold;");
            Label lt = new Label(json.get("conclusion").getAsString()); lt.setStyle("-fx-text-fill:#2d3748; -fx-font-size:12;"); lt.setWrapText(true);
            concl.getChildren().addAll(lc, lt);
            body.getChildren().add(concl);
        }
    }

    // ── Recommandations renderer ──────────────────────────────────────────────
    private void renderRecommandations(com.google.gson.JsonObject json, VBox body) {
        if (json.has("evenements_suggeres")) {
            Label title = new Label("🗓 Événements suggérés à organiser");
            title.setStyle("-fx-text-fill:#2d3748; -fx-font-size:13; -fx-font-weight:bold; -fx-padding:0 0 4 0;");
            body.getChildren().add(title);
            String[] colors = {"#667eea","#10b981","#f59e0b","#e11d48"};
            String[] bgs    = {"#eef2ff","#f0fdf4","#fffbeb","#fff1f2"};
            int ci = 0;
            for (com.google.gson.JsonElement el : json.getAsJsonArray("evenements_suggeres")) {
                com.google.gson.JsonObject ev = el.getAsJsonObject();
                String color = colors[ci % colors.length];
                String bg    = bgs[ci % bgs.length];
                HBox card = new HBox(14);
                card.setPadding(new Insets(14, 16, 14, 16));
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:10; -fx-border-color:" + color + "; -fx-border-radius:10; -fx-border-width:1.5;");
                Region accent = new Region();
                accent.setPrefWidth(4); accent.setPrefHeight(50);
                accent.setStyle("-fx-background-color:" + color + "; -fx-background-radius:4;");
                VBox info = new VBox(4);
                HBox.setHgrow(info, Priority.ALWAYS);
                String titre   = ev.has("titre")    ? ev.get("titre").getAsString()    : "—";
                String evType  = ev.has("type")     ? ev.get("type").getAsString()     : "";
                String periode = ev.has("periode")  ? ev.get("periode").getAsString()  : "";
                String objectif= ev.has("objectif") ? ev.get("objectif").getAsString() : "";
                String pub     = ev.has("public")   ? ev.get("public").getAsString()   : "";
                String duree   = ev.has("duree")    ? ev.get("duree").getAsString()    : "";
                HBox topRow = new HBox(8); topRow.setAlignment(Pos.CENTER_LEFT);
                Label lTitre = new Label(titre); lTitre.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13; -fx-font-weight:bold;");
                Label lType  = new Label(evType);  lType.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-font-size:10; -fx-font-weight:bold; -fx-padding:2 8 2 8; -fx-background-radius:20;");
                topRow.getChildren().addAll(lTitre, lType);
                Label lObj = new Label("🎯 " + objectif); lObj.setStyle("-fx-text-fill:#4a5568; -fx-font-size:11;"); lObj.setWrapText(true);
                HBox metaRow = new HBox(12); metaRow.setAlignment(Pos.CENTER_LEFT);
                if (!periode.isEmpty()) metaRow.getChildren().add(makeTag("📅 " + periode));
                if (!pub.isEmpty())     metaRow.getChildren().add(makeTag("👥 " + pub));
                if (!duree.isEmpty())   metaRow.getChildren().add(makeTag("⏱ " + duree));
                info.getChildren().addAll(topRow, lObj, metaRow);
                card.getChildren().addAll(accent, info);
                body.getChildren().add(card);
                ci++;
            }
        }
        if (json.has("planning_annuel")) {
            Label title = new Label("📆 Planning suggéré");
            title.setStyle("-fx-text-fill:#2d3748; -fx-font-size:13; -fx-font-weight:bold; -fx-padding:8 0 4 0;");
            body.getChildren().add(title);
            VBox planBox = new VBox(0);
            planBox.setStyle("-fx-background-color:white; -fx-background-radius:10; -fx-border-color:#e2e8f0; -fx-border-radius:10; -fx-border-width:1;");
            int pi = 0;
            for (com.google.gson.JsonElement el : json.getAsJsonArray("planning_annuel")) {
                com.google.gson.JsonObject entry = el.getAsJsonObject();
                String mois   = entry.has("mois")      ? entry.get("mois").getAsString()      : "—";
                String evName = entry.has("evenement")  ? entry.get("evenement").getAsString()  : "—";
                String prio   = entry.has("priorite")   ? entry.get("priorite").getAsString()   : "moyenne";
                String prioColor = switch (prio.toLowerCase()) { case "haute" -> "#e11d48"; case "basse" -> "#10b981"; default -> "#f59e0b"; };
                HBox row = new HBox(12);
                row.setPadding(new Insets(10, 16, 10, 16));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle((pi % 2 == 0 ? "-fx-background-color:#fafbff;" : "-fx-background-color:white;"));
                Label lMois = new Label(mois); lMois.setPrefWidth(90); lMois.setStyle("-fx-text-fill:#667eea; -fx-font-size:11; -fx-font-weight:bold;");
                Label lEv = new Label(evName); lEv.setStyle("-fx-text-fill:#2d3748; -fx-font-size:12;"); HBox.setHgrow(lEv, Priority.ALWAYS); lEv.setWrapText(true);
                Label lPrio = new Label(prio.toUpperCase()); lPrio.setStyle("-fx-background-color:" + prioColor + "; -fx-text-fill:white; -fx-font-size:9; -fx-font-weight:bold; -fx-padding:2 8 2 8; -fx-background-radius:20;");
                row.getChildren().addAll(lMois, lEv, lPrio);
                planBox.getChildren().add(row);
                pi++;
            }
            body.getChildren().add(planBox);
        }
        if (json.has("conseil_global")) {
            VBox tip = new VBox(6);
            tip.setPadding(new Insets(14, 16, 14, 16));
            tip.setStyle("-fx-background-color:#fff7ed; -fx-background-radius:10; -fx-border-color:#f59e0b; -fx-border-radius:10; -fx-border-width:1;");
            Label lc = new Label("💡 Conseil stratégique"); lc.setStyle("-fx-text-fill:#d97706; -fx-font-size:12; -fx-font-weight:bold;");
            Label lt = new Label(json.get("conseil_global").getAsString()); lt.setStyle("-fx-text-fill:#92400e; -fx-font-size:12;"); lt.setWrapText(true);
            tip.getChildren().addAll(lc, lt);
            body.getChildren().add(tip);
        }
    }

    // ── Suggestions renderer ──────────────────────────────────────────────────
    private void renderSuggestions(com.google.gson.JsonObject json, VBox body) {
        if (json.has("innovations")) {
            Label title = new Label("🚀 Idées innovantes");
            title.setStyle("-fx-text-fill:#2d3748; -fx-font-size:13; -fx-font-weight:bold; -fx-padding:0 0 4 0;");
            body.getChildren().add(title);
            for (com.google.gson.JsonElement el : json.getAsJsonArray("innovations")) {
                com.google.gson.JsonObject inn = el.getAsJsonObject();
                String titre    = inn.has("titre")       ? inn.get("titre").getAsString()       : "—";
                String desc     = inn.has("description") ? inn.get("description").getAsString() : "";
                String impact   = inn.has("impact")      ? inn.get("impact").getAsString()      : "moyen";
                String facilite = inn.has("facilite")    ? inn.get("facilite").getAsString()    : "moyen";
                String impactColor = switch (impact.toLowerCase()) { case "fort" -> "#10b981"; case "faible" -> "#94a3b8"; default -> "#f59e0b"; };
                HBox card = new HBox(12);
                card.setPadding(new Insets(12, 14, 12, 14));
                card.setAlignment(Pos.TOP_LEFT);
                card.setStyle("-fx-background-color:white; -fx-background-radius:10; -fx-border-color:#e2e8f0; -fx-border-radius:10; -fx-border-width:1; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,1);");
                Label lIco = new Label("💡"); lIco.setStyle("-fx-font-size:18; -fx-padding:2 0 0 0;");
                VBox info = new VBox(4); HBox.setHgrow(info, Priority.ALWAYS);
                HBox topRow = new HBox(8); topRow.setAlignment(Pos.CENTER_LEFT);
                Label lTitre = new Label(titre); lTitre.setStyle("-fx-text-fill:#2d3748; -fx-font-size:12; -fx-font-weight:bold;");
                Label lImpact = new Label("Impact " + impact); lImpact.setStyle("-fx-background-color:" + impactColor + "; -fx-text-fill:white; -fx-font-size:9; -fx-font-weight:bold; -fx-padding:2 7 2 7; -fx-background-radius:20;");
                Label lFac = new Label(facilite); lFac.setStyle("-fx-background-color:#e2e8f0; -fx-text-fill:#4a5568; -fx-font-size:9; -fx-padding:2 7 2 7; -fx-background-radius:20;");
                topRow.getChildren().addAll(lTitre, lImpact, lFac);
                Label lDesc = new Label(desc); lDesc.setStyle("-fx-text-fill:#718096; -fx-font-size:11;"); lDesc.setWrapText(true);
                info.getChildren().addAll(topRow, lDesc);
                card.getChildren().addAll(lIco, info);
                body.getChildren().add(card);
            }
        }
        HBox row2 = new HBox(12);
        if (json.has("formats_nouveaux")) row2.getChildren().add(buildKeyValueCard("🎭 Formats innovants", json.getAsJsonArray("formats_nouveaux"), "format", "avantage", "#6366f1", "#eef2ff"));
        if (json.has("themes_tendance"))  row2.getChildren().add(buildTagsCard("🔥 Thèmes tendance", json.getAsJsonArray("themes_tendance"), "#f59e0b", "#fffbeb"));
        if (!row2.getChildren().isEmpty()) body.getChildren().add(row2);
        if (json.has("quick_wins")) body.getChildren().add(buildListCard("⚡ Actions rapides", json.getAsJsonArray("quick_wins"), "#10b981", "#f0fdf4"));
    }

    // ── Helper card builders ──────────────────────────────────────────────────
    private VBox buildListCard(String title, com.google.gson.JsonArray items, String color, String bg) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:10; -fx-border-color:" + color + "; -fx-border-radius:10; -fx-border-width:1.5;");
        Label lTitle = new Label(title); lTitle.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12; -fx-font-weight:bold;");
        card.getChildren().add(lTitle);
        for (com.google.gson.JsonElement el : items) {
            HBox item = new HBox(8); item.setAlignment(Pos.TOP_LEFT);
            Label bullet = new Label("•"); bullet.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13; -fx-font-weight:bold;"); bullet.setMinWidth(10);
            Label lbl = new Label(el.getAsString()); lbl.setStyle("-fx-text-fill:#2d3748; -fx-font-size:11;"); lbl.setWrapText(true); lbl.setMaxWidth(320);
            item.getChildren().addAll(bullet, lbl);
            card.getChildren().add(item);
        }
        return card;
    }

    private VBox buildKeyValueCard(String title, com.google.gson.JsonArray items, String keyField, String valField, String color, String bg) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:10; -fx-border-color:" + color + "; -fx-border-radius:10; -fx-border-width:1.5;");
        Label lTitle = new Label(title); lTitle.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12; -fx-font-weight:bold;");
        card.getChildren().add(lTitle);
        for (com.google.gson.JsonElement el : items) {
            com.google.gson.JsonObject obj = el.getAsJsonObject();
            String key = obj.has(keyField) ? obj.get(keyField).getAsString() : "";
            String val = obj.has(valField) ? obj.get(valField).getAsString() : "";
            VBox entry = new VBox(2);
            Label lKey = new Label(key); lKey.setStyle("-fx-text-fill:" + color + "; -fx-font-size:11; -fx-font-weight:bold;");
            Label lVal = new Label(val); lVal.setStyle("-fx-text-fill:#4a5568; -fx-font-size:10;"); lVal.setWrapText(true);
            entry.getChildren().addAll(lKey, lVal);
            card.getChildren().add(entry);
        }
        return card;
    }

    private VBox buildTagsCard(String title, com.google.gson.JsonArray items, String color, String bg) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color:" + bg + "; -fx-background-radius:10; -fx-border-color:" + color + "; -fx-border-radius:10; -fx-border-width:1.5;");
        Label lTitle = new Label(title); lTitle.setStyle("-fx-text-fill:" + color + "; -fx-font-size:12; -fx-font-weight:bold;");
        card.getChildren().add(lTitle);
        FlowPane tags = new FlowPane(6, 6);
        for (com.google.gson.JsonElement el : items) {
            Label tag = new Label(el.getAsString());
            tag.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-font-size:10; -fx-font-weight:bold; -fx-padding:4 10 4 10; -fx-background-radius:20;");
            tags.getChildren().add(tag);
        }
        card.getChildren().add(tags);
        return card;
    }

    private Label makeTag(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:#718096; -fx-font-size:10; -fx-background-color:rgba(0,0,0,0.06); -fx-padding:2 8 2 8; -fx-background-radius:20;");
        return l;
    }

    private void renderTextFallback(String content, VBox body) {
        for (String line : content.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            Label lbl = new Label(t.replaceAll("\\*\\*(.*?)\\*\\*", "$1").replaceAll("^#+\\s*", "").replaceAll("^[-•]\\s*", "• "));
            lbl.setStyle("-fx-text-fill:#2d3748; -fx-font-size:12;");
            lbl.setWrapText(true); lbl.setMaxWidth(760);
            body.getChildren().add(lbl);
        }
    }

    // ── PDF export ────────────────────────────────────────────────────────────
    private void exportReportPdf(String type, String icon, String content) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("rapport_" + type.toLowerCase() + "_evenements.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File file = fc.showSaveDialog(tableRows.getScene().getWindow());
        if (file == null) return;
        try {
            byte[] pdf = buildReportPdf(type, content);
            if (pdf != null) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) { fos.write(pdf); }
            }
        } catch (Exception e) { showAlert("Erreur PDF", e.getMessage()); }
    }

    private byte[] buildReportPdf(String type, String content) {
        try {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 45, 45, 55, 45);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, baos);
            doc.open();

            // Colors
            com.itextpdf.text.BaseColor violet  = new com.itextpdf.text.BaseColor(102, 126, 234);
            com.itextpdf.text.BaseColor green   = new com.itextpdf.text.BaseColor(16, 185, 129);
            com.itextpdf.text.BaseColor orange  = new com.itextpdf.text.BaseColor(245, 158, 11);
            com.itextpdf.text.BaseColor red     = new com.itextpdf.text.BaseColor(225, 29, 72);
            com.itextpdf.text.BaseColor dark    = new com.itextpdf.text.BaseColor(26, 32, 44);
            com.itextpdf.text.BaseColor grey    = new com.itextpdf.text.BaseColor(74, 85, 104);
            com.itextpdf.text.BaseColor lgrey   = new com.itextpdf.text.BaseColor(226, 232, 240);
            com.itextpdf.text.BaseColor bgBlue  = new com.itextpdf.text.BaseColor(238, 242, 255);
            com.itextpdf.text.BaseColor bgGreen = new com.itextpdf.text.BaseColor(240, 253, 244);
            com.itextpdf.text.BaseColor bgOrange= new com.itextpdf.text.BaseColor(255, 251, 235);

            // Fonts
            com.itextpdf.text.Font fTitle  = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 22, violet);
            com.itextpdf.text.Font fSub    = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10, grey);
            com.itextpdf.text.Font fH2     = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 13, dark);
            com.itextpdf.text.Font fH3v    = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 11, violet);
            com.itextpdf.text.Font fH3g    = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 11, green);
            com.itextpdf.text.Font fH3o    = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 11, orange);
            com.itextpdf.text.Font fBody   = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10, grey);
            com.itextpdf.text.Font fBullet = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10, dark);
            com.itextpdf.text.Font fBold   = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10, dark);
            com.itextpdf.text.Font fFooter = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_OBLIQUE, 8, lgrey);
            com.itextpdf.text.Font fWhite  = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10, com.itextpdf.text.BaseColor.WHITE);
            com.itextpdf.text.Font fTag    = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9, violet);

            // ── Header ───────────────────────────────────────────────────────
            com.itextpdf.text.Paragraph titleP = new com.itextpdf.text.Paragraph("AutoLearn — Rapport " + type, fTitle);
            titleP.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER); titleP.setSpacingAfter(4);
            doc.add(titleP);
            com.itextpdf.text.Paragraph subP = new com.itextpdf.text.Paragraph(
                currentFilter + "  •  " + getFiltered().size() + " événements  •  " +
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), fSub);
            subP.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER); subP.setSpacingAfter(12);
            doc.add(subP);
            doc.add(new com.itextpdf.text.Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(2f, 100, violet, com.itextpdf.text.Element.ALIGN_CENTER, 0)));
            doc.add(new com.itextpdf.text.Paragraph(" ", fBody));

            // ── Parse JSON and render ─────────────────────────────────────────
            try {
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(extractJson(content)).getAsJsonObject();

                if ("Analyse".equals(type)) {
                    // KPI table
                    if (json.has("kpis")) {
                        com.itextpdf.text.pdf.PdfPTable kpiTable = new com.itextpdf.text.pdf.PdfPTable(4);
                        kpiTable.setWidthPercentage(100); kpiTable.setSpacingAfter(12);
                        for (com.google.gson.JsonElement el : json.getAsJsonArray("kpis")) {
                            com.google.gson.JsonObject kpi = el.getAsJsonObject();
                            String val = kpi.has("value") ? kpi.get("value").getAsString() : "";
                            String lbl = kpi.has("label") ? kpi.get("label").getAsString() : "";
                            String colorHex = kpi.has("color") ? kpi.get("color").getAsString() : "#667eea";
                            com.itextpdf.text.BaseColor cellColor = hexToColor(colorHex);
                            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell();
                            cell.setBackgroundColor(bgBlue); cell.setPadding(10); cell.setBorderColor(cellColor); cell.setBorderWidth(1.5f);
                            com.itextpdf.text.Font fVal = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 16, cellColor);
                            com.itextpdf.text.Paragraph pVal = new com.itextpdf.text.Paragraph(val, fVal);
                            pVal.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                            com.itextpdf.text.Paragraph pLbl = new com.itextpdf.text.Paragraph(lbl, fBody);
                            pLbl.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
                            cell.addElement(pVal); cell.addElement(pLbl);
                            kpiTable.addCell(cell);
                        }
                        doc.add(kpiTable);
                    }
                    addPdfSection(doc, "✅ Points forts", json, "points_forts", fH3g, green, bgGreen, fBullet);
                    addPdfSection(doc, "⚠️ Points à améliorer", json, "points_faibles", fH3o, orange, bgOrange, fBullet);
                    addPdfSection(doc, "📈 Tendances", json, "tendances", fH3v, violet, bgBlue, fBullet);
                    if (json.has("conclusion")) {
                        com.itextpdf.text.pdf.PdfPTable t = new com.itextpdf.text.pdf.PdfPTable(1);
                        t.setWidthPercentage(100); t.setSpacingBefore(8);
                        com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell();
                        c.setBackgroundColor(bgBlue); c.setPadding(12); c.setBorderColor(violet); c.setBorderWidth(1f);
                        c.addElement(new com.itextpdf.text.Paragraph("💬 Conclusion", fH3v));
                        c.addElement(new com.itextpdf.text.Paragraph(json.get("conclusion").getAsString(), fBody));
                        t.addCell(c); doc.add(t);
                    }

                } else if ("Recommandations".equals(type)) {
                    if (json.has("evenements_suggeres")) {
                        com.itextpdf.text.Paragraph h = new com.itextpdf.text.Paragraph("🗓 Événements suggérés", fH2);
                        h.setSpacingBefore(8); h.setSpacingAfter(6); doc.add(h);
                        String[] hexColors = {"#667eea","#10b981","#f59e0b","#e11d48"};
                        int ci = 0;
                        for (com.google.gson.JsonElement el : json.getAsJsonArray("evenements_suggeres")) {
                            com.google.gson.JsonObject ev = el.getAsJsonObject();
                            com.itextpdf.text.BaseColor cc = hexToColor(hexColors[ci % hexColors.length]);
                            com.itextpdf.text.pdf.PdfPTable t = new com.itextpdf.text.pdf.PdfPTable(1);
                            t.setWidthPercentage(100); t.setSpacingAfter(6);
                            com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell();
                            c.setBackgroundColor(new com.itextpdf.text.BaseColor(248, 250, 255)); c.setPadding(10); c.setBorderColor(cc); c.setBorderWidth(1.5f);
                            com.itextpdf.text.Font fColor = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 12, cc);
                            c.addElement(new com.itextpdf.text.Paragraph((ev.has("titre") ? ev.get("titre").getAsString() : "") + "  [" + (ev.has("type") ? ev.get("type").getAsString() : "") + "]", fColor));
                            if (ev.has("objectif")) c.addElement(new com.itextpdf.text.Paragraph("🎯 " + ev.get("objectif").getAsString(), fBody));
                            String meta = "";
                            if (ev.has("periode")) meta += "📅 " + ev.get("periode").getAsString() + "   ";
                            if (ev.has("public"))  meta += "👥 " + ev.get("public").getAsString()  + "   ";
                            if (ev.has("duree"))   meta += "⏱ " + ev.get("duree").getAsString();
                            if (!meta.isEmpty()) c.addElement(new com.itextpdf.text.Paragraph(meta, fSub));
                            t.addCell(c); doc.add(t); ci++;
                        }
                    }
                    if (json.has("planning_annuel")) {
                        com.itextpdf.text.Paragraph h = new com.itextpdf.text.Paragraph("📆 Planning suggéré", fH2);
                        h.setSpacingBefore(10); h.setSpacingAfter(6); doc.add(h);
                        com.itextpdf.text.pdf.PdfPTable t = new com.itextpdf.text.pdf.PdfPTable(3);
                        t.setWidthPercentage(100); t.setWidths(new float[]{2f, 5f, 1.5f});
                        addPdfTableHeader(t, new String[]{"MOIS","ÉVÉNEMENT","PRIORITÉ"}, violet, fWhite);
                        int pi = 0;
                        for (com.google.gson.JsonElement el : json.getAsJsonArray("planning_annuel")) {
                            com.google.gson.JsonObject entry = el.getAsJsonObject();
                            String prio = entry.has("priorite") ? entry.get("priorite").getAsString() : "moyenne";
                            com.itextpdf.text.BaseColor prioColor = switch (prio.toLowerCase()) { case "haute" -> red; case "basse" -> green; default -> orange; };
                            com.itextpdf.text.BaseColor rowBg = pi % 2 == 0 ? bgBlue : com.itextpdf.text.BaseColor.WHITE;
                            addPdfTableRow(t, new String[]{
                                entry.has("mois") ? entry.get("mois").getAsString() : "—",
                                entry.has("evenement") ? entry.get("evenement").getAsString() : "—",
                                prio.toUpperCase()
                            }, rowBg, new com.itextpdf.text.Font[]{fTag, fBullet, com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 9, prioColor)});
                            pi++;
                        }
                        doc.add(t);
                    }
                    if (json.has("conseil_global")) {
                        com.itextpdf.text.pdf.PdfPTable t = new com.itextpdf.text.pdf.PdfPTable(1);
                        t.setWidthPercentage(100); t.setSpacingBefore(10);
                        com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell();
                        c.setBackgroundColor(bgOrange); c.setPadding(12); c.setBorderColor(orange); c.setBorderWidth(1f);
                        c.addElement(new com.itextpdf.text.Paragraph("💡 Conseil stratégique", fH3o));
                        c.addElement(new com.itextpdf.text.Paragraph(json.get("conseil_global").getAsString(), fBody));
                        t.addCell(c); doc.add(t);
                    }

                } else if ("Suggestions".equals(type)) {
                    if (json.has("innovations")) {
                        com.itextpdf.text.Paragraph h = new com.itextpdf.text.Paragraph("🚀 Idées innovantes", fH2);
                        h.setSpacingBefore(8); h.setSpacingAfter(6); doc.add(h);
                        for (com.google.gson.JsonElement el : json.getAsJsonArray("innovations")) {
                            com.google.gson.JsonObject inn = el.getAsJsonObject();
                            String impact = inn.has("impact") ? inn.get("impact").getAsString() : "moyen";
                            com.itextpdf.text.BaseColor ic = switch (impact.toLowerCase()) { case "fort" -> green; case "faible" -> new com.itextpdf.text.BaseColor(148,163,184); default -> orange; };
                            com.itextpdf.text.pdf.PdfPTable t = new com.itextpdf.text.pdf.PdfPTable(1);
                            t.setWidthPercentage(100); t.setSpacingAfter(5);
                            com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell();
                            c.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE); c.setPadding(10); c.setBorderColor(lgrey); c.setBorderWidth(1f);
                            com.itextpdf.text.Font fImpact = com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA_BOLD, 10, ic);
                            c.addElement(new com.itextpdf.text.Paragraph((inn.has("titre") ? inn.get("titre").getAsString() : "") + "  [Impact: " + impact + "]", fImpact));
                            if (inn.has("description")) c.addElement(new com.itextpdf.text.Paragraph(inn.get("description").getAsString(), fBody));
                            t.addCell(c); doc.add(t);
                        }
                    }
                    addPdfSection(doc, "🎭 Formats innovants", json, "formats_nouveaux", fH3v, violet, bgBlue, fBullet);
                    addPdfSection(doc, "🔥 Thèmes tendance", json, "themes_tendance", fH3o, orange, bgOrange, fBullet);
                    addPdfSection(doc, "⚡ Actions rapides", json, "quick_wins", fH3g, green, bgGreen, fBullet);
                }
            } catch (Exception ex) {
                // Fallback plain text
                for (String line : content.split("\n")) {
                    String t = line.trim().replaceAll("\\*\\*(.*?)\\*\\*", "$1").replaceAll("^#+\\s*", "").replaceAll("^[-•]\\s*", "• ");
                    if (!t.isEmpty()) { com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph(t, fBody); p.setSpacingAfter(2); doc.add(p); }
                }
            }

            // Footer
            doc.add(new com.itextpdf.text.Paragraph("\n"));
            doc.add(new com.itextpdf.text.Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(0.5f, 100, lgrey, com.itextpdf.text.Element.ALIGN_CENTER, 0)));
            com.itextpdf.text.Paragraph footerP = new com.itextpdf.text.Paragraph("Généré par AutoLearn  •  Rapport " + type + "  •  © 2026", fFooter);
            footerP.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER); footerP.setSpacingBefore(6);
            doc.add(footerP);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) { System.err.println("Erreur PDF: " + e.getMessage()); return null; }
    }

    private com.itextpdf.text.BaseColor hexToColor(String hex) {
        try {
            hex = hex.replace("#", "");
            return new com.itextpdf.text.BaseColor(Integer.parseInt(hex.substring(0,2),16), Integer.parseInt(hex.substring(2,4),16), Integer.parseInt(hex.substring(4,6),16));
        } catch (Exception e) { return new com.itextpdf.text.BaseColor(102,126,234); }
    }

    private void addPdfSection(com.itextpdf.text.Document doc, String title, com.google.gson.JsonObject json,
                                String key, com.itextpdf.text.Font titleFont, com.itextpdf.text.BaseColor borderColor,
                                com.itextpdf.text.BaseColor bg, com.itextpdf.text.Font bodyFont) throws Exception {
        if (!json.has(key)) return;
        com.itextpdf.text.Paragraph h = new com.itextpdf.text.Paragraph(title, titleFont);
        h.setSpacingBefore(10); h.setSpacingAfter(4); doc.add(h);
        com.itextpdf.text.pdf.PdfPTable t = new com.itextpdf.text.pdf.PdfPTable(1);
        t.setWidthPercentage(100); t.setSpacingAfter(6);
        com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell();
        c.setBackgroundColor(bg); c.setPadding(10); c.setBorderColor(borderColor); c.setBorderWidth(1f);
        com.google.gson.JsonArray arr = json.getAsJsonArray(key);
        for (com.google.gson.JsonElement el : arr) {
            String text = el.isJsonPrimitive() ? el.getAsString() :
                          (el.getAsJsonObject().has("format") ? el.getAsJsonObject().get("format").getAsString() + " — " + el.getAsJsonObject().get("avantage").getAsString() : el.toString());
            c.addElement(new com.itextpdf.text.Paragraph("  •  " + text, bodyFont));
        }
        t.addCell(c); doc.add(t);
    }

    private void addPdfTableHeader(com.itextpdf.text.pdf.PdfPTable table, String[] headers,
                                    com.itextpdf.text.BaseColor bg, com.itextpdf.text.Font font) {
        for (String h : headers) {
            com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(h, font));
            c.setBackgroundColor(bg); c.setPadding(8); c.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            table.addCell(c);
        }
    }

    private void addPdfTableRow(com.itextpdf.text.pdf.PdfPTable table, String[] values,
                                 com.itextpdf.text.BaseColor bg, com.itextpdf.text.Font[] fonts) {
        for (int i = 0; i < values.length; i++) {
            com.itextpdf.text.Font f = (fonts != null && i < fonts.length) ? fonts[i] : com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 10);
            com.itextpdf.text.pdf.PdfPCell c = new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(values[i], f));
            c.setBackgroundColor(bg); c.setPadding(7);
            table.addCell(c);
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    private void loadTable() {
        tableRows.getChildren().clear();
        List<Evenement> filtered = getFiltered();
        if (filtered.isEmpty()) {
            Label noData = new Label("Aucun événement trouvé.");
            noData.setStyle("-fx-text-fill:rgba(255,255,255,0.5); -fx-font-size:12; -fx-padding:20;");
            tableRows.getChildren().add(noData);
            return;
        }
        for (int i = 0; i < filtered.size(); i++) tableRows.getChildren().add(buildRow(filtered.get(i), i % 2 == 0));
    }

    private HBox buildRow(Evenement e, boolean even) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setStyle(even
            ? "-fx-background-color:rgba(255,255,255,0.02); -fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;"
            : "-fx-background-color:transparent; -fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;");
        Label lTitre = new Label(e.getTitre()); lTitre.setPrefWidth(180); lTitre.setStyle("-fx-text-fill:white; -fx-font-size:13;");
        Label lType  = new Label(e.getType() != null ? e.getType() : "—"); lType.setPrefWidth(100); lType.setStyle(getTypeStyle(e.getType()) + " -fx-font-size:12;");
        Label lDebut = new Label(e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "—"); lDebut.setPrefWidth(120); lDebut.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");
        Label lFin   = new Label(e.getDateFin()   != null ? e.getDateFin().format(FMT)   : "—"); lFin.setPrefWidth(120);   lFin.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");
        String statut = e.computeStatus();
        Label lStatut = new Label("● " + statut); lStatut.setPrefWidth(100); lStatut.setStyle(getStatutStyle(statut));
        Label lNbMax  = new Label(String.valueOf(e.getNbMax())); lNbMax.setPrefWidth(80); lNbMax.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");
        HBox actions = new HBox(6); actions.setAlignment(Pos.CENTER_LEFT);
        Button btnVoir = new Button("👁 Voir"); btnVoir.setStyle("-fx-background-color:#667eea; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"); btnVoir.setOnAction(ev -> onVoir(e));
        Button btnModifier = new Button("✏ Modifier"); btnModifier.setStyle("-fx-background-color:#db2777; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"); btnModifier.setOnAction(ev -> onModifier(e));
        Button btnSupprimer = new Button("🗑 Supprimer"); btnSupprimer.setStyle("-fx-background-color:#e11d48; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"); btnSupprimer.setOnAction(ev -> onSupprimer(e));
        actions.getChildren().addAll(btnVoir, btnModifier, btnSupprimer);
        if (!e.isIsCanceled() && !"Passé".equals(statut)) {
            Button btnAnnuler = new Button("✖ Annuler"); btnAnnuler.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;"); btnAnnuler.setOnAction(ev -> onAnnuler(e));
            actions.getChildren().add(btnAnnuler);
        }
        row.getChildren().addAll(lTitre, lType, lDebut, lFin, lStatut, lNbMax, actions);
        return row;
    }

    private String getStatutStyle(String s) {
        return switch (s) {
            case "Plannifié" -> "-fx-text-fill:#60a5fa; -fx-font-size:12; -fx-font-weight:bold;";
            case "En cours"  -> "-fx-text-fill:#34d399; -fx-font-size:12; -fx-font-weight:bold;";
            case "Passé"     -> "-fx-text-fill:#4ade80; -fx-font-size:12; -fx-font-weight:bold;";
            case "Annulé"    -> "-fx-text-fill:#fbbf24; -fx-font-size:12; -fx-font-weight:bold;";
            default          -> "-fx-text-fill:rgba(255,255,255,0.6); -fx-font-size:12;";
        };
    }

    private String getTypeStyle(String type) {
        if (type == null) return "-fx-text-fill:rgba(255,255,255,0.7);";
        return switch (type.toLowerCase()) {
            case "hackathon"  -> "-fx-text-fill:#10b981; -fx-font-weight:bold;";
            case "conference" -> "-fx-text-fill:#6366f1; -fx-font-weight:bold;";
            case "workshop"   -> "-fx-text-fill:#f59e0b; -fx-font-weight:bold;";
            default           -> "-fx-text-fill:rgba(255,255,255,0.7);";
        };
    }

    private String typeColor(String type) {
        if (type == null) return "#667eea";
        return switch (type.toLowerCase()) {
            case "hackathon"  -> "#10b981";
            case "conference" -> "#6366f1";
            case "workshop"   -> "#f59e0b";
            default           -> "#667eea";
        };
    }

    @FXML private void onAjouter() { loadView("/views/backoffice/evenement/form.fxml", null); }

    private void onVoir(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/evenement/show.fxml"));
            javafx.scene.Parent view = loader.load();
            ((EvenementShowController) loader.getController()).setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void onModifier(Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/evenement/form.fxml"));
            javafx.scene.Parent view = loader.load();
            ((EvenementFormController) loader.getController()).setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void onSupprimer(Evenement e) {
        service.supprimer(e.getId());
        allEvents = service.getAll();
        updateStats();
        loadTable();
    }

    private void onAnnuler(Evenement e) {
        e.setIsCanceled(true); e.setWorkflowStatus("annule"); e.setStatus("Annulé");
        service.modifier(e);
        allEvents = service.getAll();
        updateStats();
        loadTable();
    }

    private void loadView(String fxml, Evenement e) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            javafx.scene.Parent view = loader.load();
            if (e != null) ((EvenementFormController) loader.getController()).setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private javafx.scene.layout.StackPane getContentArea() {
        return (javafx.scene.layout.StackPane) tableRows.getScene().lookup("#contentArea");
    }
}
