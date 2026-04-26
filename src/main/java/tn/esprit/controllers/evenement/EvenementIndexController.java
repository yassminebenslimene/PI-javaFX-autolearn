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
    @FXML private VBox statsContainer;

    private final EvenementService service = new EvenementService();
    private final GroqService groqService = new GroqService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<Evenement> allEvents = new ArrayList<>();
    private String currentFilter = "Tous les types d'événements";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterCombo.getItems().addAll(
            "Tous les types d'événements", "Hackathon", "Conference", "Workshop"
        );
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

    @FXML
    private void onFilterChanged() {
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
        List<Evenement> filtered = getFiltered();

        if (currentFilter.equals("Tous les types d'événements")) {
            // Show one card per type
            Map<String, List<Evenement>> byType = new LinkedHashMap<>();
            byType.put("Hackathon", new ArrayList<>());
            byType.put("Conference", new ArrayList<>());
            byType.put("Workshop", new ArrayList<>());
            for (Evenement e : allEvents) {
                if (e.getType() != null) byType.computeIfAbsent(e.getType(), k -> new ArrayList<>()).add(e);
            }
            HBox row = new HBox(16);
            row.setAlignment(Pos.CENTER_LEFT);
            for (Map.Entry<String, List<Evenement>> entry : byType.entrySet()) {
                if (!entry.getValue().isEmpty()) row.getChildren().add(buildStatCard(entry.getKey(), entry.getValue()));
            }
            if (!row.getChildren().isEmpty()) statsContainer.getChildren().add(row);
        } else if (!filtered.isEmpty()) {
            statsContainer.getChildren().add(buildStatCard(currentFilter, filtered));
        }
    }

    private VBox buildStatCard(String type, List<Evenement> events) {
        String color = typeColor(type);
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color:rgba(102,126,234,0.12);" +
                      "-fx-background-radius:12; -fx-border-color:" + color + ";" +
                      "-fx-border-radius:12; -fx-border-width:1;");
        Label lType = new Label(type);
        lType.setStyle("-fx-text-fill:" + color + "; -fx-font-size:13; -fx-font-weight:bold;");
        Label lRating = new Label("3.9/5");
        lRating.setStyle("-fx-text-fill:white; -fx-font-size:26; -fx-font-weight:bold;");
        Label lFeedbacks = new Label("📋 " + (events.size() * 4) + " feedbacks");
        lFeedbacks.setStyle("-fx-text-fill:rgba(255,255,255,0.65); -fx-font-size:11;");
        Label lSatisfaction = new Label("✓ 100% satisfaction");
        lSatisfaction.setStyle("-fx-text-fill:#34d399; -fx-font-size:11; -fx-font-weight:bold;");
        card.getChildren().addAll(lType, lRating, lFeedbacks, lSatisfaction);
        return card;
    }

    @FXML private void onGenerateAnalysis()       { generateReport("Analyse"); }
    @FXML private void onGenerateRecommendations(){ generateReport("Recommandations"); }
    @FXML private void onGenerateSuggestions()    { generateReport("Suggestions"); }

    private void generateReport(String type) {
        List<Evenement> filtered = getFiltered();
        if (filtered.isEmpty()) {
            showAlert("Aucun événement", "Sélectionnez un filtre avec des événements.");
            return;
        }
        String prompt = buildPrompt(type, filtered);
        Button[] btns = getReportButtons();
        for (Button b : btns) if (b != null) b.setDisable(true);

        CompletableFuture.supplyAsync(() -> {
            try {
                return groqService.ask(
                    "Tu es un expert en gestion d'événements académiques. Fournis une analyse professionnelle et détaillée en français.",
                    prompt);
            } catch (Exception e) { return null; }
        }).thenAccept(report -> Platform.runLater(() -> {
            for (Button b : btns) if (b != null) b.setDisable(false);
            if (report != null && !report.isBlank()) showReportDialog(type, report);
            else showAlert("Erreur IA", "Impossible de générer le rapport. Vérifiez votre connexion.");
        }));
    }

    private Button[] getReportButtons() {
        // Lookup buttons by fx:id via scene — safe approach
        try {
            Button b1 = (Button) tableRows.getScene().lookup("#btnAnalyse");
            Button b2 = (Button) tableRows.getScene().lookup("#btnRecommandations");
            Button b3 = (Button) tableRows.getScene().lookup("#btnSuggestions");
            return new Button[]{b1, b2, b3};
        } catch (Exception e) { return new Button[0]; }
    }

    private String buildPrompt(String type, List<Evenement> events) {
        String list = events.stream()
            .map(e -> "- " + e.getTitre() + " (" + e.getType() + ", " + e.computeStatus() + ")")
            .collect(Collectors.joining("\n"));
        return switch (type) {
            case "Analyse" -> "Analyse complète et détaillée de ces événements:\n" + list;
            case "Recommandations" -> "Recommandations concrètes pour améliorer ces événements:\n" + list;
            case "Suggestions" -> "Suggestions créatives et innovantes pour ces événements:\n" + list;
            default -> list;
        };
    }

    private void showReportDialog(String type, String content) {
        String[] icons = {"📈", "💡", "✨"};
        String[] types = {"Analyse", "Recommandations", "Suggestions"};
        String icon = icons[Arrays.asList(types).indexOf(type) < 0 ? 0 : Arrays.asList(types).indexOf(type)];

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(icon + " Rapport " + type);

        VBox root = new VBox(0);
        root.setPrefWidth(680);

        // Header
        VBox header = new VBox(4);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);");
        Label lTitle = new Label(icon + "  Rapport " + type);
        lTitle.setStyle("-fx-text-fill:white; -fx-font-size:16; -fx-font-weight:bold;");
        Label lSub = new Label("Filtre: " + currentFilter + "  •  " + getFiltered().size() + " événements analysés");
        lSub.setStyle("-fx-text-fill:rgba(255,255,255,0.75); -fx-font-size:11;");
        header.getChildren().addAll(lTitle, lSub);

        // Content
        VBox body = new VBox(12);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:#f8f9ff;");

        // Parse sections (lines starting with ** or numbered)
        String[] lines = content.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                Label section = new Label(trimmed.replace("**", ""));
                section.setStyle("-fx-text-fill:#667eea; -fx-font-size:13; -fx-font-weight:bold; -fx-padding:8 0 2 0;");
                section.setWrapText(true);
                body.getChildren().add(section);
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                HBox item = new HBox(8);
                item.setAlignment(Pos.TOP_LEFT);
                Label bullet = new Label("•");
                bullet.setStyle("-fx-text-fill:#667eea; -fx-font-size:14; -fx-font-weight:bold;");
                bullet.setMinWidth(12);
                Label text = new Label(trimmed.substring(2));
                text.setStyle("-fx-text-fill:#2d3748; -fx-font-size:12;");
                text.setWrapText(true);
                text.setMaxWidth(580);
                item.getChildren().addAll(bullet, text);
                body.getChildren().add(item);
            } else {
                Label text = new Label(trimmed);
                text.setStyle("-fx-text-fill:#4a5568; -fx-font-size:12;");
                text.setWrapText(true);
                text.setMaxWidth(620);
                body.getChildren().add(text);
            }
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setStyle("-fx-background-color:#f8f9ff; -fx-background:transparent; -fx-border-width:0;");

        // Footer with PDF button
        HBox footer = new HBox(12);
        footer.setPadding(new Insets(12, 24, 12, 24));
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0 transparent transparent transparent; -fx-border-width:1 0 0 0;");
        Button btnPdf = new Button("⬇ Télécharger PDF");
        btnPdf.setStyle("-fx-background-color:#667eea; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:bold;" +
                        "-fx-padding:9 18 9 18; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnPdf.setOnAction(ev -> exportReportPdf(type, icon, content));
        footer.getChildren().add(btnPdf);

        root.getChildren().addAll(header, scroll, footer);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-padding:0;");
        dialog.showAndWait();
    }

    private void exportReportPdf(String type, String icon, String content) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("rapport_" + type.toLowerCase() + "_evenements.pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File file = fc.showSaveDialog(tableRows.getScene().getWindow());
        if (file == null) return;
        try {
            byte[] pdf = buildReportPdf(type, icon, content);
            if (pdf != null) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) { fos.write(pdf); }
            }
        } catch (Exception e) {
            showAlert("Erreur PDF", e.getMessage());
        }
    }

    private byte[] buildReportPdf(String type, String icon, String content) {
        try {
            com.itextpdf.text.Document doc = new com.itextpdf.text.Document(com.itextpdf.text.PageSize.A4, 50, 50, 60, 50);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, baos);
            doc.open();

            com.itextpdf.text.BaseColor violet = new com.itextpdf.text.BaseColor(102, 126, 234);
            com.itextpdf.text.BaseColor dark = new com.itextpdf.text.BaseColor(45, 55, 72);
            com.itextpdf.text.BaseColor grey = new com.itextpdf.text.BaseColor(74, 85, 104);

            // Header
            com.itextpdf.text.Paragraph title = new com.itextpdf.text.Paragraph("AutoLearn — Rapport " + type,
                com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 20,
                    com.itextpdf.text.Font.BOLD, violet));
            title.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            doc.add(title);

            com.itextpdf.text.Paragraph sub = new com.itextpdf.text.Paragraph(
                "Filtre: " + currentFilter + "  •  " + getFiltered().size() + " événements",
                com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11,
                    com.itextpdf.text.Font.NORMAL, grey));
            sub.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            sub.setSpacingAfter(16);
            doc.add(sub);

            com.itextpdf.text.pdf.draw.LineSeparator ls = new com.itextpdf.text.pdf.draw.LineSeparator(
                1.5f, 100, violet, com.itextpdf.text.Element.ALIGN_CENTER, 0);
            doc.add(new com.itextpdf.text.Chunk(ls));
            doc.add(new com.itextpdf.text.Paragraph("\n"));

            // Content
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) { doc.add(new com.itextpdf.text.Paragraph(" ")); continue; }
                if (trimmed.startsWith("**") && trimmed.endsWith("**")) {
                    com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph(trimmed.replace("**", ""),
                        com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 12,
                            com.itextpdf.text.Font.BOLD, violet));
                    p.setSpacingBefore(8);
                    doc.add(p);
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("• ")) {
                    com.itextpdf.text.Paragraph p = new com.itextpdf.text.Paragraph("  • " + trimmed.substring(2),
                        com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11,
                            com.itextpdf.text.Font.NORMAL, dark));
                    p.setIndentationLeft(12);
                    doc.add(p);
                } else {
                    doc.add(new com.itextpdf.text.Paragraph(trimmed,
                        com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 11,
                            com.itextpdf.text.Font.NORMAL, grey)));
                }
            }

            // Footer
            doc.add(new com.itextpdf.text.Paragraph("\n"));
            doc.add(new com.itextpdf.text.Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(
                0.5f, 100, new com.itextpdf.text.BaseColor(200, 200, 220),
                com.itextpdf.text.Element.ALIGN_CENTER, 0)));
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                "Généré par AutoLearn  •  © 2026",
                com.itextpdf.text.FontFactory.getFont(com.itextpdf.text.FontFactory.HELVETICA, 9,
                    com.itextpdf.text.Font.ITALIC, grey));
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            footer.setSpacingBefore(6);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erreur PDF rapport: " + e.getMessage());
            return null;
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void loadTable() {
        tableRows.getChildren().clear();
        List<Evenement> filtered = getFiltered();
        if (filtered.isEmpty()) {
            Label noData = new Label("Aucun événement trouvé.");
            noData.setStyle("-fx-text-fill:rgba(255,255,255,0.5); -fx-font-size:12; -fx-padding:20;");
            tableRows.getChildren().add(noData);
            return;
        }
        for (int i = 0; i < filtered.size(); i++) {
            tableRows.getChildren().add(buildRow(filtered.get(i), i % 2 == 0));
        }
    }

    private HBox buildRow(Evenement e, boolean even) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 20, 14, 20));
        row.setStyle(even
            ? "-fx-background-color:rgba(255,255,255,0.02); -fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;"
            : "-fx-background-color:transparent; -fx-border-color:transparent transparent rgba(255,255,255,0.04) transparent; -fx-border-width:0 0 1 0;");

        Label lTitre = new Label(e.getTitre());
        lTitre.setPrefWidth(180); lTitre.setStyle("-fx-text-fill:white; -fx-font-size:13;");

        Label lType = new Label(e.getType() != null ? e.getType() : "—");
        lType.setPrefWidth(100); lType.setStyle(getTypeStyle(e.getType()) + " -fx-font-size:12;");

        Label lDebut = new Label(e.getDateDebut() != null ? e.getDateDebut().format(FMT) : "—");
        lDebut.setPrefWidth(120); lDebut.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        Label lFin = new Label(e.getDateFin() != null ? e.getDateFin().format(FMT) : "—");
        lFin.setPrefWidth(120); lFin.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        String statut = e.computeStatus();
        Label lStatut = new Label("● " + statut);
        lStatut.setPrefWidth(100); lStatut.setStyle(getStatutStyle(statut));

        Label lNbMax = new Label(String.valueOf(e.getNbMax()));
        lNbMax.setPrefWidth(80); lNbMax.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:12;");

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnVoir = new Button("👁 Voir");
        btnVoir.setStyle("-fx-background-color:#667eea; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnVoir.setOnAction(ev -> onVoir(e));

        Button btnModifier = new Button("✏ Modifier");
        btnModifier.setStyle("-fx-background-color:#db2777; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnModifier.setOnAction(ev -> onModifier(e));

        Button btnSupprimer = new Button("🗑 Supprimer");
        btnSupprimer.setStyle("-fx-background-color:#e11d48; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnSupprimer.setOnAction(ev -> onSupprimer(e));

        actions.getChildren().addAll(btnVoir, btnModifier, btnSupprimer);

        if (!e.isIsCanceled() && !"Passé".equals(statut)) {
            Button btnAnnuler = new Button("✖ Annuler");
            btnAnnuler.setStyle("-fx-background-color:#dc2626; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:bold; -fx-padding:7 10 7 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
            btnAnnuler.setOnAction(ev -> onAnnuler(e));
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
            case "hackathon"   -> "-fx-text-fill:#10b981; -fx-font-weight:bold;";
            case "conference"  -> "-fx-text-fill:#6366f1; -fx-font-weight:bold;";
            case "workshop"    -> "-fx-text-fill:#f59e0b; -fx-font-weight:bold;";
            default            -> "-fx-text-fill:rgba(255,255,255,0.7);";
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
            URL r = getClass().getResource("/views/backoffice/evenement/show.fxml");
            FXMLLoader loader = new FXMLLoader(r);
            javafx.scene.Parent view = loader.load();
            ((EvenementShowController) loader.getController()).setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void onModifier(Evenement e) {
        try {
            URL r = getClass().getResource("/views/backoffice/evenement/form.fxml");
            FXMLLoader loader = new FXMLLoader(r);
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
            URL r = getClass().getResource(fxml);
            FXMLLoader loader = new FXMLLoader(r);
            javafx.scene.Parent view = loader.load();
            if (e != null) ((EvenementFormController) loader.getController()).setEvenement(e);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private javafx.scene.layout.StackPane getContentArea() {
        return (javafx.scene.layout.StackPane) tableRows.getScene().lookup("#contentArea");
    }
}
