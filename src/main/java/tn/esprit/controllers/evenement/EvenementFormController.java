package tn.esprit.controllers.evenement;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Evenement;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.EvenementService;
import tn.esprit.services.GroqService;
import tn.esprit.services.EventPlanningService;
import tn.esprit.services.PlanningPdfService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.MyConnection;

import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class EvenementFormController implements Initializable {

    @FXML private Label labelFormTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldDescription;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField fieldNbMax;
    @FXML private javafx.scene.control.DatePicker pickerDateDebut;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerHeureDebut;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerMinDebut;
    @FXML private javafx.scene.control.DatePicker pickerDateFin;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerHeureFin;
    @FXML private javafx.scene.control.Spinner<Integer> spinnerMinFin;
    @FXML private TextField fieldLieu;
    @FXML private Label labelError;
    @FXML private Button btnSubmit;
    @FXML private Label errTitre;
    @FXML private Label errDescription;
    @FXML private Label errType;
    @FXML private Label errNbMax;
    @FXML private Label errDateDebut;
    @FXML private Label errDateFin;
    @FXML private Label errLieu;

    // IA controls
    @FXML private Button btnGenererDesc;
    @FXML private ProgressIndicator spinDesc;
    @FXML private Button btnEstimerNb;
    @FXML private ProgressIndicator spinNb;
    @FXML private Button btnGenererPlanning;
    @FXML private Button btnTelechargerPdf;
    @FXML private ProgressIndicator spinPlanning;
    @FXML private TextArea fieldPlanning;
    @FXML private Label errPlanning;
    @FXML private VBox planningCardsContainer;
    @FXML private Label planningPlaceholder;

    private final EvenementService service = new EvenementService();
    private final GroqService groq = new GroqService();
    private final EventPlanningService planningService = new EventPlanningService();
    private Evenement evenementToEdit = null;

    private Connection getConnection() {
        try {
            // Return a fresh connection for async operations (never use singleton in background threads)
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/autolearn_db", "root", "");
        } catch (Exception e) {
            System.err.println("[EvenementForm] Error getting connection: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboType.getItems().addAll("Hackathon", "Conference", "Workshop");
        comboType.setValue("Conference");

        // Force white text on ComboBox (CSS alone is not always enough)
        comboType.setStyle(comboType.getStyle() != null ? comboType.getStyle() : "");
        comboType.setCellFactory(lv -> {
            javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                    setStyle("-fx-text-fill:#2d3748; -fx-font-size:13; -fx-background-color:white;");
                }
            };
            return cell;
        });
        comboType.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle("-fx-text-fill:white; -fx-font-size:13; -fx-background-color:transparent;");
            }
        });

        java.time.format.DateTimeFormatter dateFmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        javafx.util.StringConverter<java.time.LocalDate> converter = new javafx.util.StringConverter<>() {
            public String toString(java.time.LocalDate d) { return d != null ? d.format(dateFmt) : ""; }
            public java.time.LocalDate fromString(String s) {
                try { return s != null && !s.isBlank() ? java.time.LocalDate.parse(s, dateFmt) : null; }
                catch (Exception e) { return null; }
            }
        };
        pickerDateDebut.setConverter(converter);
        pickerDateFin.setConverter(converter);

        fieldTitre.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldTitre));
        fieldDescription.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldDescription));
        fieldLieu.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldLieu));
        fieldNbMax.textProperty().addListener((o, ov, nv) -> clearFieldError(fieldNbMax));
        pickerDateDebut.valueProperty().addListener((o, ov, nv) -> { if (errDateDebut != null) errDateDebut.setText(""); });
        pickerDateFin.valueProperty().addListener((o, ov, nv) -> { if (errDateFin != null) errDateFin.setText(""); });
        fieldNbMax.textProperty().addListener((o, ov, nv) -> {
            if (!nv.matches("\\d*")) fieldNbMax.setText(nv.replaceAll("[^\\d]", ""));
        });

        if (spinDesc != null) spinDesc.setVisible(false);
        if (spinNb != null) spinNb.setVisible(false);
    }

    // --- IA: Generate description -------------------------------------------

    @FXML
    private void onGenererDescription() {
        final String titre = fieldTitre.getText().trim();
        if (titre.length() < 3) {
            errTitre.setText("Entrez d'abord un titre (min 3 caracteres) pour generer la description.");
            return;
        }
        if (btnGenererDesc != null) btnGenererDesc.setDisable(true);
        if (spinDesc != null) spinDesc.setVisible(true);

        CompletableFuture.<String>supplyAsync(() -> {
            try {
                StringBuilder feedbackCtx = new StringBuilder();
                String q = "SELECT p.feedbacks FROM participation p "
                         + "WHERE p.feedbacks IS NOT NULL AND p.feedbacks != '' AND p.feedbacks != '[]' LIMIT 20";
                Connection conn = getConnection();
                if (conn == null) return "";
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
                    while (rs.next()) {
                        try {
                            com.google.gson.JsonArray arr = new com.google.gson.Gson()
                                .fromJson(rs.getString("feedbacks"), com.google.gson.JsonArray.class);
                            for (com.google.gson.JsonElement el : arr) {
                                com.google.gson.JsonObject fb = el.getAsJsonObject();
                                int note = fb.has("rating_global") ? fb.get("rating_global").getAsInt() : 0;
                                String comment = (fb.has("comment") && !fb.get("comment").isJsonNull())
                                    ? fb.get("comment").getAsString() : "";
                                if (note >= 4 && !comment.isEmpty())
                                    feedbackCtx.append("- \"").append(comment).append("\"\n");
                            }
                        } catch (Exception ignored) {}
                    }
                } finally {
                    if (conn != null) {
                        try { conn.close(); } catch (Exception ignored) {}
                    }
                }
                String type = comboType.getValue() != null ? comboType.getValue() : "evenement";
                String prompt = "Genere une description professionnelle et engageante pour un evenement academique.\n"
                    + "Titre: " + titre + "\nType: " + type + "\n"
                    + (feedbackCtx.length() > 0 ? "Commentaires positifs d'etudiants sur des evenements similaires:\n" + feedbackCtx + "\n" : "")
                    + "La description doit: etre entre 100 et 250 mots, professionnelle et motivante, "
                    + "decrire les objectifs, le public cible et les benefices. "
                    + "Reponds UNIQUEMENT avec la description, sans titre ni introduction.";
                return groq.ask(
                    "Tu es un expert en communication evenementielle academique. Genere des descriptions concises et professionnelles.",
                    prompt);
            } catch (Exception e) {
                return "";
            }
        }).thenAccept(desc -> Platform.runLater(() -> {
            if (desc != null && !desc.isBlank()) {
                fieldDescription.setText(desc.trim());
            } else {
                errDescription.setText("Erreur lors de la generation. Verifiez votre connexion.");
            }
            if (btnGenererDesc != null) btnGenererDesc.setDisable(false);
            if (spinDesc != null) spinDesc.setVisible(false);
        }));
    }

    // --- IA: Estimate max teams ---------------------------------------------

    @FXML
    private void onEstimerNbEquipes() {
        if (btnEstimerNb != null) btnEstimerNb.setDisable(true);
        if (spinNb != null) spinNb.setVisible(true);

        CompletableFuture.<String>supplyAsync(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("Donnees historiques pour estimer le nombre optimal d'equipes:\n\n");

                Connection conn = getConnection();
                if (conn == null) {
                    // No DB data, still ask AI with just the event info
                    sb.append("Pas de données historiques disponibles.\n");
                } else {
                    try {
                        String q1 = "SELECT e.type, e.nb_max, COUNT(DISTINCT eq.id) nb_eq, "
                                   + "ROUND(COUNT(DISTINCT eq.id)*100.0/NULLIF(e.nb_max,0),1) taux "
                                   + "FROM evenement e LEFT JOIN equipe eq ON eq.evenement_id=e.id "
                                   + "GROUP BY e.id ORDER BY e.type";
                        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q1)) {
                            sb.append("HISTORIQUE PAR EVENEMENT:\n");
                            while (rs.next())
                                sb.append("- [").append(rs.getString("type")).append("] nb_max=")
                                  .append(rs.getInt("nb_max")).append(", equipes=").append(rs.getInt("nb_eq"))
                                  .append(", taux=").append(rs.getDouble("taux")).append("%\n");
                        }

                        String q2 = "SELECT type, AVG(nb_max) avg_max, AVG(nb_eq) avg_eq FROM "
                                   + "(SELECT e.type, e.nb_max, COUNT(DISTINCT eq.id) nb_eq FROM evenement e "
                                   + "LEFT JOIN equipe eq ON eq.evenement_id=e.id GROUP BY e.id) t GROUP BY type";
                        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q2)) {
                            sb.append("\nMOYENNES PAR TYPE:\n");
                            while (rs.next())
                                sb.append("- ").append(rs.getString("type"))
                                  .append(": nb_max_moy=").append(String.format("%.1f", rs.getDouble("avg_max")))
                                  .append(", equipes_moy=").append(String.format("%.1f", rs.getDouble("avg_eq"))).append("\n");
                        }
                    } catch (Exception dbEx) {
                        sb.append("Erreur lecture BD: ").append(dbEx.getMessage()).append("\n");
                    } finally {
                        try { conn.close(); } catch (Exception ignored) {}
                    }
                }

                String type = comboType.getValue() != null ? comboType.getValue() : "evenement";
                String titre = fieldTitre.getText().trim();
                sb.append("\nNouvel evenement: \"").append(titre).append("\" de type ").append(type).append("\n");
                sb.append("Reponds avec SEULEMENT un nombre entier entre 5 et 50, rien d'autre.");

                return groq.ask(
                    "Tu es un expert en gestion d'evenements. Retourne uniquement un nombre entier.",
                    sb.toString());
            } catch (Exception e) {
                System.err.println("[Estimer] Erreur: " + e.getMessage());
                return "";
            }
        }).thenAccept(result -> Platform.runLater(() -> {
            if (result != null && !result.isBlank()) {
                String cleaned = result.trim().replaceAll("[^0-9]", "");
                if (!cleaned.isEmpty()) {
                    try {
                        int val = Integer.parseInt(cleaned.length() > 3 ? cleaned.substring(0, 3) : cleaned);
                        if (val >= 1 && val <= 100) {
                            fieldNbMax.setText(String.valueOf(val));
                            if (errNbMax != null) {
                                errNbMax.setText("✓ Estimation IA: " + val + " équipes recommandées");
                                errNbMax.setStyle("-fx-text-fill:#34d399; -fx-font-size:11;");
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                if (errNbMax != null) {
                    errNbMax.setText("Erreur estimation. Vérifiez votre connexion.");
                    errNbMax.setStyle("-fx-text-fill:#f87171; -fx-font-size:11;");
                }
            }
            if (btnEstimerNb != null) btnEstimerNb.setDisable(false);
            if (spinNb != null) spinNb.setVisible(false);
        }));
    }

    // --- IA: Generate Planning -----------------------------------------------

    @FXML
    private void onGenererPlanning() {
        final String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) {
            if (errPlanning != null) errPlanning.setText("Entrez d'abord un titre pour générer le planning.");
            return;
        }

        final String type = comboType.getValue() != null ? comboType.getValue() : "Conference";

        if (pickerDateDebut.getValue() == null) {
            if (errPlanning != null) errPlanning.setText("Définissez d'abord la date et heure de début.");
            return;
        }

        if (pickerDateFin.getValue() == null) {
            if (errPlanning != null) errPlanning.setText("Définissez d'abord la date et heure de fin.");
            return;
        }

        final LocalDateTime startTime = pickerDateDebut.getValue()
            .atTime(spinnerHeureDebut.getValue(), spinnerMinDebut.getValue());
        final LocalDateTime endTime = pickerDateFin.getValue()
            .atTime(spinnerHeureFin.getValue(), spinnerMinFin.getValue());

        if (!endTime.isAfter(startTime)) {
            if (errPlanning != null) errPlanning.setText("La date/heure de fin doit être après le début.");
            return;
        }

        int tempNbParticipants = 0;
        try {
            String nbStr = fieldNbMax.getText().trim();
            if (!nbStr.isEmpty()) tempNbParticipants = Integer.parseInt(nbStr) * 5;
        } catch (Exception ignored) {}
        final int nbParticipants = tempNbParticipants;

        if (btnGenererPlanning != null) btnGenererPlanning.setDisable(true);
        if (btnTelechargerPdf != null) btnTelechargerPdf.setVisible(false);
        if (spinPlanning != null) spinPlanning.setVisible(true);
        if (errPlanning != null) errPlanning.setText("");
        if (planningCardsContainer != null) {
            planningCardsContainer.setVisible(false);
            planningCardsContainer.setManaged(false);
        }

        CompletableFuture.<String>supplyAsync(() -> {
            try {
                return planningService.generatePlanning(titre, type, startTime, endTime, nbParticipants);
            } catch (Exception e) {
                return "";
            }
        }).thenAccept(planning -> Platform.runLater(() -> {
            if (planning != null && !planning.isBlank()) {
                if (fieldPlanning != null) fieldPlanning.setText(planning);
                renderPlanningCards(planning, titre, type, startTime, endTime);
                if (errPlanning != null) {
                    errPlanning.setText("✓ Planning généré avec succès");
                    errPlanning.setStyle("-fx-text-fill:#34d399; -fx-font-size:11;");
                }
                if (btnTelechargerPdf != null) btnTelechargerPdf.setVisible(true);
            } else {
                if (errPlanning != null) errPlanning.setText("Erreur lors de la génération du planning.");
            }
            if (btnGenererPlanning != null) btnGenererPlanning.setDisable(false);
            if (spinPlanning != null) spinPlanning.setVisible(false);
        }));
    }

    private void renderPlanningCards(String planningJson, String titre, String type,
                                     LocalDateTime start, LocalDateTime end) {
        if (planningCardsContainer == null) return;
        planningCardsContainer.getChildren().clear();

        try {
            // Clean JSON before parsing
            String cleanJson = planningJson.trim();
            // Remove BOM if present
            if (cleanJson.startsWith("\uFEFF")) cleanJson = cleanJson.substring(1);
            // Remove markdown code blocks
            cleanJson = cleanJson.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            // Extract JSON object
            int jsonStart = cleanJson.indexOf('{');
            if (jsonStart > 0) cleanJson = cleanJson.substring(jsonStart);
            // Find last complete closing brace
            int lastClose = cleanJson.lastIndexOf('}');
            if (lastClose > 0) cleanJson = cleanJson.substring(0, lastClose + 1);

            com.google.gson.JsonObject obj;
            try {
                obj = com.google.gson.JsonParser.parseString(cleanJson).getAsJsonObject();
            } catch (Exception parseEx) {
                // JSON is truncated — show what we have as raw text fallback
                System.err.println("[renderPlanning] JSON truncated, showing raw: " + parseEx.getMessage());
                javafx.scene.control.Label fallback = new javafx.scene.control.Label(
                    "Planning généré (format brut):\n" + planningJson);
                fallback.setStyle("-fx-text-fill:#2d3748; -fx-font-size:11; -fx-padding:12;");
                fallback.setWrapText(true);
                planningCardsContainer.getChildren().add(fallback);
                planningCardsContainer.setVisible(true);
                planningCardsContainer.setManaged(true);
                return;
            }

            // ── Header card ──────────────────────────────────────────────────
            javafx.scene.layout.VBox headerCard = new javafx.scene.layout.VBox(4);
            headerCard.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);" +
                                "-fx-padding:16 20 16 20; -fx-background-radius:10 10 0 0;");
            javafx.scene.control.Label lTitle = new javafx.scene.control.Label("📅 " + titre);
            lTitle.setStyle("-fx-text-fill:white; -fx-font-size:15; -fx-font-weight:bold;");
            javafx.scene.control.Label lMeta = new javafx.scene.control.Label(
                type + "  •  " +
                start.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                "  •  " +
                start.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " → " +
                end.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            lMeta.setStyle("-fx-text-fill:rgba(255,255,255,0.8); -fx-font-size:11;");
            headerCard.getChildren().addAll(lTitle, lMeta);
            planningCardsContainer.getChildren().add(headerCard);

            // ── Timeline rows ─────────────────────────────────────────────────
            if (obj.has("planning")) {
                com.google.gson.JsonArray slots = obj.getAsJsonArray("planning");
                // Column header
                javafx.scene.layout.HBox colHeader = new javafx.scene.layout.HBox();
                colHeader.setStyle("-fx-background-color:#f0ebff; -fx-padding:8 20 8 20;");
                for (String[] col : new String[][]{{"HEURE","80"},{"ACTIVITÉ","220"},{"LIEU","140"},{"DESCRIPTION","0"}}) {
                    javafx.scene.control.Label lh = new javafx.scene.control.Label(col[0]);
                    lh.setStyle("-fx-text-fill:#667eea; -fx-font-size:10; -fx-font-weight:bold;");
                    if (!col[1].equals("0")) lh.setPrefWidth(Double.parseDouble(col[1]));
                    else { lh.setMaxWidth(Double.MAX_VALUE); javafx.scene.layout.HBox.setHgrow(lh, javafx.scene.layout.Priority.ALWAYS); }
                    colHeader.getChildren().add(lh);
                }
                planningCardsContainer.getChildren().add(colHeader);

                int idx = 0;
                for (com.google.gson.JsonElement el : slots) {
                    com.google.gson.JsonObject slot = el.getAsJsonObject();
                    String debut = slot.has("heure_debut") ? slot.get("heure_debut").getAsString() : "—";
                    String fin   = slot.has("heure_fin")   ? slot.get("heure_fin").getAsString()   : "—";
                    String act   = slot.has("activite")    ? slot.get("activite").getAsString()    : "—";
                    String lieu  = slot.has("lieu")        ? slot.get("lieu").getAsString()        : "—";
                    String desc  = slot.has("description") ? slot.get("description").getAsString() : "";
                    String slotType = slot.has("type") ? slot.get("type").getAsString() : "";

                    String rowBg = idx % 2 == 0 ? "white" : "#faf9ff";
                    String accentColor = getSlotColor(slotType);

                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color:" + rowBg + ";" +
                                 "-fx-padding:10 20 10 0;" +
                                 "-fx-border-color:transparent transparent #ede9fe transparent;" +
                                 "-fx-border-width:0 0 1 0;");

                    // Accent bar
                    javafx.scene.layout.Region accent = new javafx.scene.layout.Region();
                    accent.setPrefWidth(4);
                    accent.setPrefHeight(40);
                    accent.setStyle("-fx-background-color:" + accentColor + ";");
                    javafx.scene.layout.HBox.setMargin(accent, new javafx.geometry.Insets(0, 16, 0, 0));

                    javafx.scene.control.Label lHeure = new javafx.scene.control.Label(debut + "\n" + fin);
                    lHeure.setPrefWidth(80);
                    lHeure.setStyle("-fx-text-fill:#4a5568; -fx-font-size:11; -fx-font-weight:bold;");

                    javafx.scene.layout.VBox actBox = new javafx.scene.layout.VBox(2);
                    actBox.setPrefWidth(220);
                    javafx.scene.control.Label lAct = new javafx.scene.control.Label(act);
                    lAct.setStyle("-fx-text-fill:#2d3748; -fx-font-size:12; -fx-font-weight:bold;");
                    lAct.setWrapText(true);
                    actBox.getChildren().add(lAct);

                    javafx.scene.control.Label lLieu = new javafx.scene.control.Label("📍 " + lieu);
                    lLieu.setPrefWidth(140);
                    lLieu.setStyle("-fx-text-fill:#667eea; -fx-font-size:11;");
                    lLieu.setWrapText(true);

                    javafx.scene.control.Label lDesc = new javafx.scene.control.Label(desc);
                    lDesc.setStyle("-fx-text-fill:#718096; -fx-font-size:11;");
                    lDesc.setWrapText(true);
                    lDesc.setMaxWidth(Double.MAX_VALUE);
                    javafx.scene.layout.HBox.setHgrow(lDesc, javafx.scene.layout.Priority.ALWAYS);

                    row.getChildren().addAll(accent, lHeure, actBox, lLieu, lDesc);
                    planningCardsContainer.getChildren().add(row);
                    idx++;
                }
            }

            // ── Animateurs section ────────────────────────────────────────────
            if (obj.has("animateurs") && obj.getAsJsonArray("animateurs").size() > 0) {
                javafx.scene.layout.VBox animSection = new javafx.scene.layout.VBox(0);
                animSection.setStyle("-fx-background-color:#f0ebff; -fx-padding:10 20 6 20;");
                javafx.scene.control.Label animTitle = new javafx.scene.control.Label("👥 Équipe d'animation");
                animTitle.setStyle("-fx-text-fill:#667eea; -fx-font-size:11; -fx-font-weight:bold;");
                animSection.getChildren().add(animTitle);

                javafx.scene.layout.HBox animRow = new javafx.scene.layout.HBox(12);
                animRow.setStyle("-fx-padding:8 0 4 0;");
                for (com.google.gson.JsonElement el : obj.getAsJsonArray("animateurs")) {
                    com.google.gson.JsonObject anim = el.getAsJsonObject();
                    String nom  = anim.has("nom")  ? anim.get("nom").getAsString()  : "—";
                    String role = anim.has("role") ? anim.get("role").getAsString() : "—";
                    javafx.scene.layout.VBox chip = new javafx.scene.layout.VBox(2);
                    chip.setStyle("-fx-background-color:white; -fx-background-radius:8;" +
                                  "-fx-border-color:#c4b5fd; -fx-border-radius:8; -fx-border-width:1;" +
                                  "-fx-padding:6 12 6 12;");
                    javafx.scene.control.Label lNom = new javafx.scene.control.Label(nom);
                    lNom.setStyle("-fx-text-fill:#4c1d95; -fx-font-size:11; -fx-font-weight:bold;");
                    javafx.scene.control.Label lRole = new javafx.scene.control.Label(role);
                    lRole.setStyle("-fx-text-fill:#7c3aed; -fx-font-size:10;");
                    chip.getChildren().addAll(lNom, lRole);
                    animRow.getChildren().add(chip);
                }
                animSection.getChildren().add(animRow);
                planningCardsContainer.getChildren().add(animSection);
            }

            // ── Notes ─────────────────────────────────────────────────────────
            if (obj.has("notes") && !obj.get("notes").getAsString().isBlank()) {
                javafx.scene.layout.HBox notesBox = new javafx.scene.layout.HBox(8);
                notesBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                notesBox.setStyle("-fx-background-color:#fffbeb; -fx-padding:10 20 10 20;" +
                                  "-fx-background-radius:0 0 10 10;");
                javafx.scene.control.Label lNotes = new javafx.scene.control.Label(
                    "💡 " + obj.get("notes").getAsString());
                lNotes.setStyle("-fx-text-fill:#92400e; -fx-font-size:11;");
                lNotes.setWrapText(true);
                notesBox.getChildren().add(lNotes);
                planningCardsContainer.getChildren().add(notesBox);
            }

        } catch (Exception e) {
            // Fallback: show raw text
            javafx.scene.control.Label fallback = new javafx.scene.control.Label(planningJson);
            fallback.setStyle("-fx-text-fill:#2d3748; -fx-font-size:11; -fx-padding:12;");
            fallback.setWrapText(true);
            planningCardsContainer.getChildren().add(fallback);
        }

        planningCardsContainer.setVisible(true);
        planningCardsContainer.setManaged(true);
        if (planningPlaceholder != null) {
            planningPlaceholder.setVisible(false);
            planningPlaceholder.setManaged(false);
        }
    }

    private String getSlotColor(String type) {
        if (type == null) return "#667eea";
        return switch (type.toLowerCase()) {
            case "accueil"      -> "#10b981";
            case "pause"        -> "#f59e0b";
            case "repas"        -> "#f59e0b";
            case "networking"   -> "#06b6d4";
            case "presentation" -> "#667eea";
            case "atelier"      -> "#8b5cf6";
            case "coaching"     -> "#ec4899";
            case "cloture"      -> "#6366f1";
            default             -> "#667eea";
        };
    }

    @FXML
    private void onTelechargerPdf() {
        String planningJson = fieldPlanning != null ? fieldPlanning.getText() : "";
        if (planningJson == null || planningJson.isBlank()) return;

        String titre = fieldTitre.getText().trim();
        String type  = comboType.getValue() != null ? comboType.getValue() : "Événement";
        LocalDateTime start = pickerDateDebut.getValue() != null
            ? pickerDateDebut.getValue().atTime(spinnerHeureDebut.getValue(), spinnerMinDebut.getValue())
            : java.time.LocalDateTime.now();
        LocalDateTime end = pickerDateFin.getValue() != null
            ? pickerDateFin.getValue().atTime(spinnerHeureFin.getValue(), spinnerMinFin.getValue())
            : start.plusHours(8);

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Enregistrer le planning PDF");
        fc.setInitialFileName("planning_" + titre.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File file = fc.showSaveDialog(fieldTitre.getScene().getWindow());
        if (file == null) return;

        try {
            PlanningPdfService pdfService = new PlanningPdfService();
            byte[] pdf = pdfService.generatePlanningPdf(titre, type, start, end, planningJson);
            if (pdf != null) {
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    fos.write(pdf);
                }
                if (errPlanning != null) {
                    errPlanning.setText("✓ PDF enregistré: " + file.getName());
                    errPlanning.setStyle("-fx-text-fill:#34d399; -fx-font-size:11;");
                }
            }
        } catch (Exception e) {
            if (errPlanning != null) {
                errPlanning.setText("Erreur PDF: " + e.getMessage());
                errPlanning.setStyle("-fx-text-fill:#f87171; -fx-font-size:11;");
            }
        }
    }

    // --- Form logic ---------------------------------------------------------

    public void setEvenement(Evenement e) {
        this.evenementToEdit = e;
        labelFormTitle.setText("Modifier l'Evenement: " + e.getTitre());
        btnSubmit.setText("Enregistrer");
        fieldTitre.setText(e.getTitre());
        fieldDescription.setText(e.getDescription());
        comboType.setValue(e.getType());
        fieldNbMax.setText(String.valueOf(e.getNbMax()));
        fieldLieu.setText(e.getLieu());
        if (e.getDateDebut() != null) {
            pickerDateDebut.setValue(e.getDateDebut().toLocalDate());
            spinnerHeureDebut.getValueFactory().setValue(e.getDateDebut().getHour());
            spinnerMinDebut.getValueFactory().setValue(e.getDateDebut().getMinute());
        }
        if (e.getDateFin() != null) {
            pickerDateFin.setValue(e.getDateFin().toLocalDate());
            spinnerHeureFin.getValueFactory().setValue(e.getDateFin().getHour());
            spinnerMinFin.getValueFactory().setValue(e.getDateFin().getMinute());
        }
    }

    @FXML
    private void onSubmit() {
        labelError.setText("");
        resetFieldStyles();
        boolean valid = true;

        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) { setFieldError(fieldTitre, "Le titre est obligatoire."); valid = false; }
        else if (titre.length() < 5) { setFieldError(fieldTitre, "Le titre doit contenir au moins 5 caracteres."); valid = false; }
        else if (titre.length() > 255) { setFieldError(fieldTitre, "Le titre ne peut pas depasser 255 caracteres."); valid = false; }

        String description = fieldDescription.getText().trim();
        if (description.isEmpty()) { setFieldError(fieldDescription, "La description est obligatoire."); valid = false; }
        else if (description.length() < 10) { setFieldError(fieldDescription, "La description doit contenir au moins 10 caracteres."); valid = false; }
        else if (description.length() > 2000) { setFieldError(fieldDescription, "La description ne peut pas depasser 2000 caracteres."); valid = false; }

        String lieu = fieldLieu.getText().trim();
        if (lieu.isEmpty()) { setFieldError(fieldLieu, "Le lieu est obligatoire."); valid = false; }
        else if (lieu.length() < 2) { setFieldError(fieldLieu, "Le lieu doit contenir au moins 2 caracteres."); valid = false; }

        if (comboType.getValue() == null) {
            if (errType != null) errType.setText("Veuillez selectionner un type d'evenement.");
            else labelError.setText("Veuillez selectionner un type d'evenement.");
            valid = false;
        }

        int nbMax = 0;
        String nbMaxStr = fieldNbMax.getText().trim();
        if (nbMaxStr.isEmpty()) { setFieldError(fieldNbMax, "Le nombre maximum d'equipes est obligatoire."); valid = false; }
        else {
            try {
                nbMax = Integer.parseInt(nbMaxStr);
                if (nbMax < 1 || nbMax > 100) { setFieldError(fieldNbMax, "Le nombre d'equipes doit etre entre 1 et 100."); valid = false; }
            } catch (NumberFormatException ex) { setFieldError(fieldNbMax, "Veuillez entrer un nombre valide."); valid = false; }
        }

        if (pickerDateDebut.getValue() == null && pickerDateDebut.getEditor().getText() != null
                && !pickerDateDebut.getEditor().getText().trim().isEmpty())
            pickerDateDebut.getEditor().commitValue();

        LocalDateTime dateDebut = null;
        if (pickerDateDebut.getValue() == null) {
            if (errDateDebut != null) errDateDebut.setText("La date de debut est obligatoire.");
            valid = false;
        } else {
            dateDebut = pickerDateDebut.getValue().atTime(spinnerHeureDebut.getValue(), spinnerMinDebut.getValue());
        }

        if (pickerDateFin.getValue() == null && pickerDateFin.getEditor().getText() != null
                && !pickerDateFin.getEditor().getText().trim().isEmpty())
            pickerDateFin.getEditor().commitValue();

        LocalDateTime dateFin = null;
        if (pickerDateFin.getValue() == null) {
            if (errDateFin != null) errDateFin.setText("La date de fin est obligatoire.");
            valid = false;
        } else {
            dateFin = pickerDateFin.getValue().atTime(spinnerHeureFin.getValue(), spinnerMinFin.getValue());
            if (dateDebut != null && !dateFin.isAfter(dateDebut)) {
                if (errDateFin != null) errDateFin.setText(
                    dateFin.toLocalDate().isBefore(dateDebut.toLocalDate())
                    ? "La date de fin ne peut pas etre anterieure a la date de debut."
                    : "L'heure de fin doit etre superieure a l'heure de debut (" + String.format("%02d:%02d", dateDebut.getHour(), dateDebut.getMinute()) + ").");
                valid = false;
            }
        }

        if (!valid) return;

        if (evenementToEdit == null) {
            Evenement e = new Evenement(titre, lieu, description, comboType.getValue(), dateDebut, dateFin, nbMax);
            service.ajouter(e);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_evenement",
                java.util.Map.of("titre", titre, "lieu", lieu));
        } else {
            evenementToEdit.setTitre(titre);
            evenementToEdit.setLieu(lieu);
            evenementToEdit.setDescription(description);
            evenementToEdit.setType(comboType.getValue());
            evenementToEdit.setDateDebut(dateDebut);
            evenementToEdit.setDateFin(dateFin);
            evenementToEdit.setNbMax(nbMax);
            String newStatus = evenementToEdit.computeStatus();
            evenementToEdit.setStatus(newStatus);
            String wf = switch (newStatus) {
                case "En cours" -> "en_cours";
                case "Passe"    -> "termine";
                case "Annule"   -> "annule";
                default         -> "planifie";
            };
            evenementToEdit.setWorkflowStatus(wf);
            service.modifier(evenementToEdit);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_evenement",
                java.util.Map.of("titre", titre));
        }
        retourListe();
    }

    @FXML
    private void onAnnuler() { retourListe(); }

    private void retourListe() {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/index.fxml");
            Parent view = FXMLLoader.load(resource);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setFieldError(Control field, String message) {
        field.setStyle(field.getStyle() + "-fx-border-color:#f87171 !important; -fx-border-width:1.5;");
        Label errLabel = getErrLabel(field);
        if (errLabel != null) errLabel.setText(message);
        else labelError.setText(message);
    }

    private Label getErrLabel(Control field) {
        if (field == fieldTitre) return errTitre;
        if (field == fieldDescription) return errDescription;
        if (field == fieldNbMax) return errNbMax;
        if (field == fieldLieu) return errLieu;
        return null;
    }

    private void clearFieldError(Control field) {
        String style = field.getStyle().replaceAll("-fx-border-color:#f87171 !important;", "")
                                       .replaceAll("-fx-border-width:1\\.5;", "");
        field.setStyle(style);
        Label errLabel = getErrLabel(field);
        if (errLabel != null) errLabel.setText("");
        if (labelError != null) labelError.setText("");
    }

    private void resetFieldStyles() {
        for (Control c : new Control[]{fieldTitre, fieldLieu, fieldNbMax}) clearFieldError(c);
        clearFieldError(fieldDescription);
        if (errDateDebut != null) errDateDebut.setText("");
        if (errDateFin != null) errDateFin.setText("");
        if (errType != null) errType.setText("");
        if (labelError != null) labelError.setText("");
    }

    private StackPane getContentArea() {
        return (StackPane) fieldTitre.getScene().lookup("#contentArea");
    }
}