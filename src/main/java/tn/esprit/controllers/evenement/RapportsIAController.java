package tn.esprit.controllers.evenement;

import com.google.gson.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import tn.esprit.services.GroqService;
import tn.esprit.services.ReportPdfService;
import tn.esprit.tools.MyConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RapportsIAController implements Initializable {

    @FXML private TabPane tabPane;
    @FXML private Button btnAmeliorations;
    @FXML private Button btnPdfAmeliorations;
    @FXML private ProgressIndicator spinAmeliorations;
    @FXML private WebView webAmeliorations;
    @FXML private Button btnSuggestions;
    @FXML private Button btnPdfSuggestions;
    @FXML private ProgressIndicator spinSuggestions;
    @FXML private WebView webSuggestions;
    @FXML private Button btnAnalyse;
    @FXML private Button btnPdfAnalyse;
    @FXML private ProgressIndicator spinAnalyse;
    @FXML private WebView webAnalyse;
    
    // Stats cards
    @FXML private Label statNoteHackathon;
    @FXML private Label statFbHackathon;
    @FXML private Label statSatHackathon;
    @FXML private Label statNoteWorkshop;
    @FXML private Label statFbWorkshop;
    @FXML private Label statSatWorkshop;
    @FXML private Label statNoteConference;
    @FXML private Label statFbConference;
    @FXML private Label statSatConference;

    private final GroqService groq = new GroqService();
    private final Connection conn = MyConnection.getInstance().getConnection();
    private final Gson gson = new Gson();
    private final ReportPdfService pdfService = new ReportPdfService();
    private String lastAmeliorationsHtml = "";
    private String lastSuggestionsHtml = "";
    private String lastAnalyseHtml = "";
    private String currentFilter = null; // null = tous, "Hackathon", "Workshop", "Conference"

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        spinAmeliorations.setVisible(false);
        spinSuggestions.setVisible(false);
        spinAnalyse.setVisible(false);
        String ph = buildHtml("<p style='color:rgba(255,255,255,0.4);font-size:14px;text-align:center;margin-top:80px;'>"
                + "🤖 Cliquez sur le bouton pour générer le rapport IA basé sur les feedbacks étudiants</p>");
        webAmeliorations.getEngine().loadContent(ph);
        webSuggestions.getEngine().loadContent(ph);
        webAnalyse.getEngine().loadContent(ph);
        
        // Load stats
        loadStats();
        
        // Add click handlers to stat cards for filtering
        statNoteHackathon.getParent().setStyle(statNoteHackathon.getParent().getStyle() + "; -fx-cursor:hand;");
        statNoteHackathon.getParent().setOnMouseClicked(e -> setFilter("Hackathon"));
        statNoteWorkshop.getParent().setStyle(statNoteWorkshop.getParent().getStyle() + "; -fx-cursor:hand;");
        statNoteWorkshop.getParent().setOnMouseClicked(e -> setFilter("Workshop"));
        statNoteConference.getParent().setStyle(statNoteConference.getParent().getStyle() + "; -fx-cursor:hand;");
        statNoteConference.getParent().setOnMouseClicked(e -> setFilter("Conference"));
    }
    
    private void setFilter(String type) {
        currentFilter = currentFilter != null && currentFilter.equals(type) ? null : type;
        loadStats();
    }
    
    private void loadStats() {
        try {
            // Hackathon stats
            Map<String, Object> hackathonStats = getEventTypeStats("Hackathon");
            statNoteHackathon.setText(String.format("%.1f/5", (double) hackathonStats.get("avgNote")));
            statFbHackathon.setText(hackathonStats.get("fbCount") + " feedbacks");
            statSatHackathon.setText(hackathonStats.get("satisfaction") + "% satisfaction");
            
            // Workshop stats
            Map<String, Object> workshopStats = getEventTypeStats("Workshop");
            statNoteWorkshop.setText(String.format("%.1f/5", (double) workshopStats.get("avgNote")));
            statFbWorkshop.setText(workshopStats.get("fbCount") + " feedbacks");
            statSatWorkshop.setText(workshopStats.get("satisfaction") + "% satisfaction");
            
            // Conference stats
            Map<String, Object> conferenceStats = getEventTypeStats("Conference");
            statNoteConference.setText(String.format("%.1f/5", (double) conferenceStats.get("avgNote")));
            statFbConference.setText(conferenceStats.get("fbCount") + " feedbacks");
            statSatConference.setText(conferenceStats.get("satisfaction") + "% satisfaction");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private Map<String, Object> getEventTypeStats(String type) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String q = "SELECT e.titre, p.feedbacks FROM participation p "
                 + "JOIN evenement e ON p.evenement_id = e.id "
                 + "WHERE e.type = ? AND p.feedbacks IS NOT NULL AND p.feedbacks != ''";
        
        List<JsonObject> feedbacks = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("feedbacks");
                    try {
                        JsonArray arr = gson.fromJson(json, JsonArray.class);
                        for (JsonElement el : arr) {
                            feedbacks.add(el.getAsJsonObject());
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        
        double avgNote = feedbacks.isEmpty() ? 0 : feedbacks.stream()
            .mapToInt(fb -> getInt(fb, "rating_global")).average().orElse(0);
        long satisfaction = feedbacks.isEmpty() ? 0 : feedbacks.stream()
            .filter(fb -> getInt(fb, "rating_global") >= 4).count() * 100 / feedbacks.size();
        
        stats.put("avgNote", avgNote);
        stats.put("fbCount", feedbacks.size());
        stats.put("satisfaction", satisfaction);
        return stats;
    }

    // ─── Helpers : lire feedbacks JSON depuis participation ───────────────────

    /** Retourne tous les feedbacks parsés depuis la colonne participation.feedbacks */
    private List<JsonObject> getAllFeedbacks() throws SQLException {
        List<JsonObject> list = new ArrayList<>();
        String q = "SELECT p.feedbacks, e.titre, e.type FROM participation p "
                 + "JOIN evenement e ON p.evenement_id = e.id "
                 + "WHERE p.feedbacks IS NOT NULL AND p.feedbacks != '' AND p.feedbacks != '[]'";
        if (currentFilter != null) {
            q += " AND e.type = '" + currentFilter + "'";
        }
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(q)) {
            while (rs.next()) {
                String json = rs.getString("feedbacks");
                String titre = rs.getString("titre");
                String type  = rs.getString("type");
                try {
                    JsonArray arr = gson.fromJson(json, JsonArray.class);
                    for (JsonElement el : arr) {
                        JsonObject fb = el.getAsJsonObject();
                        fb.addProperty("_evenement_titre", titre);
                        fb.addProperty("_evenement_type", type);
                        list.add(fb);
                    }
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private int getInt(JsonObject o, String key) {
        try { return o.has(key) ? o.get(key).getAsInt() : 0; } catch (Exception e) { return 0; }
    }
    private String getString(JsonObject o, String key) {
        try { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : ""; } catch (Exception e) { return ""; }
    }
    private int getCategoryRating(JsonObject fb, String cat) {
        try {
            if (fb.has("rating_categories")) {
                JsonObject cats = fb.getAsJsonObject("rating_categories");
                return cats.has(cat) ? cats.get(cat).getAsInt() : 0;
            }
        } catch (Exception ignored) {}
        return 0;
    }

    // ─── Rapport 1 : Améliorations ────────────────────────────────────────────

    @FXML
    private void onGenererAmeliorations() {
        btnAmeliorations.setDisable(true);
        spinAmeliorations.setVisible(true);
        webAmeliorations.getEngine().loadContent(buildHtml(
                "<p style='color:rgba(255,255,255,0.5);text-align:center;margin-top:80px;'>⏳ Génération en cours...</p>"));
        CompletableFuture.supplyAsync(() -> {
            try {
                return groq.ask(
                    "Tu es un expert en gestion d'événements académiques. Analyse les feedbacks étudiants et génère "
                    + "un rapport professionnel d'améliorations en HTML (sans balises html/head/body). "
                    + "Utilise des sections h2/h3, listes ul/li, badges de priorité (HAUTE/MOYENNE/BASSE), "
                    + "et cite des commentaires réels. Réponds en français.",
                    collectDataAmeliorations());
            } catch (Exception e) {
                return "<p style='color:#f87171;'>❌ Erreur: " + e.getMessage() + "</p>";
            }
        }).thenAccept(html -> Platform.runLater(() -> {
            lastAmeliorationsHtml = html;
            webAmeliorations.getEngine().loadContent(buildHtml(html));
            btnAmeliorations.setDisable(false);
            spinAmeliorations.setVisible(false);
        }));
    }

    private String collectDataAmeliorations() throws SQLException {
        List<JsonObject> feedbacks = getAllFeedbacks();
        StringBuilder sb = new StringBuilder();
        sb.append("RAPPORT D'AMÉLIORATIONS — basé sur ").append(feedbacks.size()).append(" feedbacks étudiants réels.\n\n");

        // Stats générales événements
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) total, SUM(is_canceled) annules, "
                + "SUM(CASE WHEN status='Passé' THEN 1 ELSE 0 END) passes FROM evenement")) {
            if (rs.next()) {
                sb.append("STATISTIQUES ÉVÉNEMENTS:\n")
                  .append("- Total: ").append(rs.getInt("total"))
                  .append(", Annulés: ").append(rs.getInt("annules"))
                  .append(", Passés: ").append(rs.getInt("passes")).append("\n\n");
            }
        }

        // Taux de remplissage
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT e.titre, e.type, e.nb_max, COUNT(DISTINCT eq.id) nb_eq, "
                + "ROUND(COUNT(DISTINCT eq.id)*100.0/NULLIF(e.nb_max,0),1) taux "
                + "FROM evenement e LEFT JOIN equipe eq ON eq.evenement_id=e.id "
                + "GROUP BY e.id ORDER BY taux ASC LIMIT 8")) {
            sb.append("TAUX DE REMPLISSAGE (moins remplis en premier):\n");
            while (rs.next())
                sb.append("- ").append(rs.getString("titre")).append(" [").append(rs.getString("type")).append("]: ")
                  .append(rs.getInt("nb_eq")).append("/").append(rs.getInt("nb_max"))
                  .append(" équipes (").append(rs.getDouble("taux")).append("%)\n");
            sb.append("\n");
        }

        // Analyse feedbacks par note
        Map<String, List<Integer>> notesByEvent = new LinkedHashMap<>();
        Map<String, List<String>> commentsByEvent = new LinkedHashMap<>();
        Map<String, int[]> categoryTotals = new LinkedHashMap<>(); // [orga, contenu, lieu, animation, count]

        for (JsonObject fb : feedbacks) {
            String titre = getString(fb, "_evenement_titre");
            int note = getInt(fb, "rating_global");
            notesByEvent.computeIfAbsent(titre, k -> new ArrayList<>()).add(note);
            String comment = getString(fb, "comment");
            if (!comment.isEmpty())
                commentsByEvent.computeIfAbsent(titre, k -> new ArrayList<>()).add(comment);
            int[] cats = categoryTotals.computeIfAbsent(titre, k -> new int[5]);
            cats[0] += getCategoryRating(fb, "organisation");
            cats[1] += getCategoryRating(fb, "contenu");
            cats[2] += getCategoryRating(fb, "lieu");
            cats[3] += getCategoryRating(fb, "animation");
            cats[4]++;
        }

        sb.append("NOTES MOYENNES PAR ÉVÉNEMENT (feedbacks étudiants):\n");
        notesByEvent.forEach((titre, notes) -> {
            double avg = notes.stream().mapToInt(i -> i).average().orElse(0);
            sb.append("- ").append(titre).append(": ").append(String.format("%.2f", avg)).append("/5")
              .append(" (").append(notes.size()).append(" feedbacks)");
            if (avg < 3) sb.append(" ⚠️ ATTENTION");
            sb.append("\n");
        });

        sb.append("\nANALYSE PAR CATÉGORIE (organisation, contenu, lieu, animation):\n");
        categoryTotals.forEach((titre, cats) -> {
            if (cats[4] > 0) {
                sb.append("- ").append(titre).append(": orga=").append(String.format("%.1f", (double)cats[0]/cats[4]))
                  .append(", contenu=").append(String.format("%.1f", (double)cats[1]/cats[4]))
                  .append(", lieu=").append(String.format("%.1f", (double)cats[2]/cats[4]))
                  .append(", animation=").append(String.format("%.1f", (double)cats[3]/cats[4])).append("\n");
            }
        });

        sb.append("\nCOMMENTAIRES NÉGATIFS (note ≤ 2):\n");
        feedbacks.stream().filter(fb -> getInt(fb, "rating_global") <= 2).limit(10).forEach(fb -> {
            String c = getString(fb, "comment");
            if (!c.isEmpty())
                sb.append("- [").append(getString(fb, "_evenement_titre")).append("] \"").append(c).append("\"\n");
        });

        sb.append("\nTous les commentaires récents:\n");
        feedbacks.stream().limit(20).forEach(fb -> {
            String c = getString(fb, "comment");
            if (!c.isEmpty())
                sb.append("- note ").append(getInt(fb, "rating_global")).append("/5 [")
                  .append(getString(fb, "_evenement_titre")).append("]: \"").append(c).append("\"\n");
        });

        sb.append("\nGénère un rapport HTML professionnel d'améliorations avec priorités basé sur ces données réelles.");
        return sb.toString();
    }

    // ─── Rapport 2 : Suggestions d'événements ────────────────────────────────

    @FXML
    private void onGenererSuggestions() {
        btnSuggestions.setDisable(true);
        spinSuggestions.setVisible(true);
        webSuggestions.getEngine().loadContent(buildHtml(
                "<p style='color:rgba(255,255,255,0.5);text-align:center;margin-top:80px;'>⏳ Génération en cours...</p>"));
        CompletableFuture.supplyAsync(() -> {
            try {
                return groq.ask(
                    "Tu es un expert en planification d'événements académiques. Basé sur les feedbacks étudiants réels, "
                    + "suggère 5 nouveaux événements innovants en HTML (sans balises html/head/body). "
                    + "Pour chaque suggestion: titre, type, description, public cible, durée, capacité recommandée, "
                    + "et justification basée sur les feedbacks. Réponds en français.",
                    collectDataSuggestions());
            } catch (Exception e) {
                return "<p style='color:#f87171;'>❌ Erreur: " + e.getMessage() + "</p>";
            }
        }).thenAccept(html -> Platform.runLater(() -> {
            lastSuggestionsHtml = html;
            webSuggestions.getEngine().loadContent(buildHtml(html));
            btnSuggestions.setDisable(false);
            spinSuggestions.setVisible(false);
        }));
    }

    private String collectDataSuggestions() throws SQLException {
        List<JsonObject> feedbacks = getAllFeedbacks();
        StringBuilder sb = new StringBuilder();
        sb.append("DONNÉES POUR SUGGESTIONS — ").append(feedbacks.size()).append(" feedbacks étudiants.\n\n");

        // Types existants
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT type, COUNT(*) nb, AVG(nb_max) avg_places FROM evenement GROUP BY type ORDER BY nb DESC")) {
            sb.append("TYPES D'ÉVÉNEMENTS EXISTANTS:\n");
            while (rs.next())
                sb.append("- ").append(rs.getString("type")).append(": ").append(rs.getInt("nb"))
                  .append(" événements, ").append(String.format("%.0f", rs.getDouble("avg_places"))).append(" places moy.\n");
            sb.append("\n");
        }

        // Événements les plus populaires par participation
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT e.titre, e.type, COUNT(DISTINCT p.id) nb FROM evenement e "
                + "LEFT JOIN participation p ON p.evenement_id=e.id "
                + "GROUP BY e.id ORDER BY nb DESC LIMIT 5")) {
            sb.append("ÉVÉNEMENTS LES PLUS POPULAIRES:\n");
            while (rs.next())
                sb.append("- ").append(rs.getString("titre")).append(" [").append(rs.getString("type")).append("]: ")
                  .append(rs.getInt("nb")).append(" participations\n");
            sb.append("\n");
        }

        // Sentiments positifs
        sb.append("SENTIMENTS DES ÉTUDIANTS:\n");
        Map<String, Integer> sentimentCount = new LinkedHashMap<>();
        feedbacks.forEach(fb -> {
            String s = getString(fb, "sentiment");
            if (!s.isEmpty()) sentimentCount.merge(s, 1, Integer::sum);
        });
        sentimentCount.forEach((s, c) -> sb.append("- ").append(s).append(": ").append(c).append("\n"));

        // Commentaires positifs
        sb.append("\nCOMMENTAIRES POSITIFS (note ≥ 4) — thèmes appréciés:\n");
        feedbacks.stream().filter(fb -> getInt(fb, "rating_global") >= 4).limit(15).forEach(fb -> {
            String c = getString(fb, "comment");
            if (!c.isEmpty())
                sb.append("- [").append(getString(fb, "_evenement_type")).append("] \"").append(c).append("\"\n");
        });

        // Distribution par mois
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT MONTH(date_debut) mois, COUNT(*) nb FROM evenement GROUP BY mois ORDER BY nb DESC")) {
            sb.append("\nDISTRIBUTION PAR MOIS:\n");
            while (rs.next())
                sb.append("- Mois ").append(rs.getInt("mois")).append(": ").append(rs.getInt("nb")).append(" événements\n");
        }

        sb.append("\nBasé sur ces feedbacks réels, suggère 5 nouveaux événements pertinents en HTML.");
        return sb.toString();
    }

    // ─── Rapport 3 : Analyse globale ─────────────────────────────────────────

    @FXML
    private void onGenererAnalyse() {
        btnAnalyse.setDisable(true);
        spinAnalyse.setVisible(true);
        webAnalyse.getEngine().loadContent(buildHtml(
                "<p style='color:rgba(255,255,255,0.5);text-align:center;margin-top:80px;'>⏳ Génération en cours...</p>"));
        CompletableFuture.supplyAsync(() -> {
            try {
                return groq.ask(
                    "Tu es un analyste de données spécialisé en événementiel académique. "
                    + "Génère un rapport d'analyse globale complet en HTML (sans balises html/head/body). "
                    + "Inclus: résumé exécutif, KPIs clés avec chiffres, analyse des feedbacks par catégorie, "
                    + "tendances, points forts, points faibles, et recommandations stratégiques. "
                    + "Utilise des tableaux HTML et des badges colorés. Réponds en français.",
                    collectDataAnalyse());
            } catch (Exception e) {
                return "<p style='color:#f87171;'>❌ Erreur: " + e.getMessage() + "</p>";
            }
        }).thenAccept(html -> Platform.runLater(() -> {
            lastAnalyseHtml = html;
            webAnalyse.getEngine().loadContent(buildHtml(html));
            btnAnalyse.setDisable(false);
            spinAnalyse.setVisible(false);
        }));
    }

    private String collectDataAnalyse() throws SQLException {
        List<JsonObject> feedbacks = getAllFeedbacks();
        StringBuilder sb = new StringBuilder();
        sb.append("ANALYSE GLOBALE — ").append(feedbacks.size()).append(" feedbacks étudiants réels.\n\n");

        // Vue d'ensemble événements
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) total, SUM(is_canceled) annules, "
                + "SUM(CASE WHEN status='En cours' THEN 1 ELSE 0 END) en_cours, "
                + "SUM(CASE WHEN status='Passé' THEN 1 ELSE 0 END) passes, "
                + "SUM(CASE WHEN status='Plannifié' THEN 1 ELSE 0 END) planifies FROM evenement")) {
            if (rs.next())
                sb.append("ÉVÉNEMENTS: total=").append(rs.getInt("total"))
                  .append(", planifiés=").append(rs.getInt("planifies"))
                  .append(", en cours=").append(rs.getInt("en_cours"))
                  .append(", passés=").append(rs.getInt("passes"))
                  .append(", annulés=").append(rs.getInt("annules")).append("\n\n");
        }

        // Participations & équipes
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) total FROM participation")) {
            if (rs.next()) sb.append("PARTICIPATIONS TOTALES: ").append(rs.getInt("total")).append("\n");
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) total, AVG(m) avg_m FROM "
                + "(SELECT eq.id, COUNT(ee.etudiant_id) m FROM equipe eq "
                + "LEFT JOIN equipe_etudiant ee ON ee.equipe_id=eq.id GROUP BY eq.id) t")) {
            if (rs.next())
                sb.append("ÉQUIPES: total=").append(rs.getInt("total"))
                  .append(", membres moy=").append(String.format("%.1f", rs.getDouble("avg_m"))).append("\n\n");
        }

        // Analyse complète des feedbacks
        if (!feedbacks.isEmpty()) {
            double avgGlobal = feedbacks.stream().mapToInt(fb -> getInt(fb, "rating_global")).average().orElse(0);
            long positifs = feedbacks.stream().filter(fb -> getInt(fb, "rating_global") >= 4).count();
            long negatifs = feedbacks.stream().filter(fb -> getInt(fb, "rating_global") <= 2).count();
            double avgOrga = feedbacks.stream().mapToInt(fb -> getCategoryRating(fb, "organisation")).average().orElse(0);
            double avgContenu = feedbacks.stream().mapToInt(fb -> getCategoryRating(fb, "contenu")).average().orElse(0);
            double avgLieu = feedbacks.stream().mapToInt(fb -> getCategoryRating(fb, "lieu")).average().orElse(0);
            double avgAnim = feedbacks.stream().mapToInt(fb -> getCategoryRating(fb, "animation")).average().orElse(0);

            sb.append("ANALYSE FEEDBACKS:\n")
              .append("- Note globale moyenne: ").append(String.format("%.2f", avgGlobal)).append("/5\n")
              .append("- Feedbacks positifs (≥4): ").append(positifs).append(" (")
              .append(String.format("%.0f", positifs * 100.0 / feedbacks.size())).append("%)\n")
              .append("- Feedbacks négatifs (≤2): ").append(negatifs).append("\n")
              .append("- Organisation: ").append(String.format("%.2f", avgOrga)).append("/5\n")
              .append("- Contenu: ").append(String.format("%.2f", avgContenu)).append("/5\n")
              .append("- Lieu: ").append(String.format("%.2f", avgLieu)).append("/5\n")
              .append("- Animation: ").append(String.format("%.2f", avgAnim)).append("/5\n\n");

            // Sentiments
            Map<String, Long> sentiments = new LinkedHashMap<>();
            feedbacks.forEach(fb -> {
                String s = getString(fb, "sentiment");
                if (!s.isEmpty()) sentiments.merge(s, 1L, Long::sum);
            });
            sb.append("DISTRIBUTION SENTIMENTS:\n");
            sentiments.forEach((s, c) -> sb.append("- ").append(s).append(": ").append(c).append("\n"));
        }

        // Top événements
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT e.titre, e.type, COUNT(p.id) nb FROM evenement e "
                + "LEFT JOIN participation p ON p.evenement_id=e.id "
                + "GROUP BY e.id ORDER BY nb DESC LIMIT 5")) {
            sb.append("\nTOP 5 ÉVÉNEMENTS PAR PARTICIPATION:\n");
            int r = 1;
            while (rs.next())
                sb.append(r++).append(". ").append(rs.getString("titre"))
                  .append(" [").append(rs.getString("type")).append("]: ")
                  .append(rs.getInt("nb")).append(" participations\n");
        }

        // Tous les commentaires
        sb.append("\nTOUS LES COMMENTAIRES ÉTUDIANTS:\n");
        feedbacks.stream().limit(30).forEach(fb -> {
            String c = getString(fb, "comment");
            if (!c.isEmpty())
                sb.append("- note ").append(getInt(fb, "rating_global")).append("/5 [")
                  .append(getString(fb, "_evenement_titre")).append("]: \"").append(c).append("\"\n");
        });

        sb.append("\nGénère un rapport d'analyse globale complet et professionnel en HTML avec tableaux et KPIs.");
        return sb.toString();
    }

    // ─── Téléchargement PDF ──────────────────────────────────────────────────

    @FXML
    private void onPdfAmeliorations() {
        if (lastAmeliorationsHtml.isEmpty()) {
            showAlert("Aucun rapport", "Générez d'abord le rapport d'améliorations.");
            return;
        }
        downloadPdf("Rapport_Ameliorations_Evenements.pdf", "Améliorations", lastAmeliorationsHtml);
    }

    @FXML
    private void onPdfSuggestions() {
        if (lastSuggestionsHtml.isEmpty()) {
            showAlert("Aucun rapport", "Générez d'abord le rapport de suggestions.");
            return;
        }
        downloadPdf("Rapport_Suggestions_Evenements.pdf", "Suggestions", lastSuggestionsHtml);
    }

    @FXML
    private void onPdfAnalyse() {
        if (lastAnalyseHtml.isEmpty()) {
            showAlert("Aucun rapport", "Générez d'abord le rapport d'analyse.");
            return;
        }
        downloadPdf("Rapport_Analyse_Globale_Evenements.pdf", "Analyse Globale", lastAnalyseHtml);
    }

    private void downloadPdf(String filename, String reportType, String content) {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Télécharger le rapport");
            fc.setInitialFileName(filename);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File file = fc.showSaveDialog(btnAmeliorations.getScene().getWindow());
            if (file != null) {
                byte[] pdf = pdfService.generateReportPdf(reportType, content, reportType);
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(pdf);
                }
                showAlert("Succès", "Rapport téléchargé: " + file.getName());
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du téléchargement: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─── Helper HTML ─────────────────────────────────────────────────────────

    private String buildHtml(String content) {
        // Convert markdown to basic HTML if needed
        String processed = convertMarkdown(content);
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
                + "* { box-sizing:border-box; margin:0; padding:0; }"
                + "body { background:#ffffff; color:#3d3d3d; font-family:'Segoe UI',Arial,sans-serif; font-size:14px; line-height:1.8; padding:28px 32px; }"
                + "h1 { color:#5c3317; font-size:24px; font-weight:800; margin:0 0 16px; padding-bottom:12px; border-bottom:3px solid #d4a96a; }"
                + "h2 { color:#8b6614; font-size:19px; font-weight:700; margin:28px 0 14px; padding:12px 18px; background:#f5e6c8; border-radius:8px; border-left:5px solid #8b6614; }"
                + "h3 { color:#5c3317; font-size:17px; font-weight:700; margin:20px 0 12px; }"
                + "h4 { color:#8b6614; font-size:16px; font-weight:600; margin:16px 0 10px; }"
                + "p { margin-bottom:14px; color:#3d3d3d; }"
                + "ul,ol { padding-left:28px; margin-bottom:16px; }"
                + "li { margin-bottom:8px; color:#3d3d3d; }"
                + "strong,b { color:#5c3317; font-weight:700; }"
                + "em,i { color:#8b6614; font-style:italic; }"
                + "table { width:100%; border-collapse:collapse; margin:20px 0; border-radius:10px; overflow:hidden; box-shadow:0 2px 8px rgba(92,51,23,0.12); }"
                + "th { background:#8b6614; color:#ffffff; padding:14px 18px; text-align:left; font-size:13px; font-weight:700; }"
                + "td { padding:12px 18px; border-bottom:1px solid #e8dcc8; color:#3d3d3d; }"
                + "tr:nth-child(even) td { background:#faf8f3; }"
                + ".card { background:#faf8f3; border:1px solid #d4a96a; border-radius:12px; padding:20px 24px; margin:16px 0; box-shadow:0 2px 8px rgba(212,169,106,0.15); }"
                + ".badge { display:inline-block; padding:5px 16px; border-radius:20px; font-size:12px; font-weight:700; margin:4px; }"
                + ".badge-green,.badge-haute { background:#c8e6c9; color:#1b5e20; }"
                + ".badge-red,.badge-basse { background:#ffcdd2; color:#b71c1c; }"
                + ".badge-yellow,.badge-moyenne { background:#ffe0b2; color:#e65100; }"
                + ".badge-blue { background:#bbdefb; color:#0d47a1; }"
                + "blockquote { border-left:5px solid #d4a96a; padding:14px 20px; margin:16px 0; background:#f5e6c8; border-radius:0 8px 8px 0; color:#3d3d3d; font-style:italic; }"
                + "hr { border:none; border-top:2px solid #d4a96a; margin:24px 0; }"
                + ".section { background:#faf8f3; border-radius:12px; padding:22px 24px; margin:18px 0; border:1px solid #d4a96a; }"
                + ".kpi { display:inline-block; background:#f5e6c8; border:2px solid #d4a96a; border-radius:10px; padding:14px 22px; margin:10px; text-align:center; }"
                + ".kpi-val { font-size:26px; font-weight:800; color:#8b6614; display:block; }"
                + ".kpi-lbl { font-size:12px; color:#5c3317; font-weight:600; }"
                + "code { background:#f0ebe3; padding:4px 10px; border-radius:4px; font-size:12px; color:#8b6614; }"
                + "</style></head><body>" + processed + "</body></html>";
    }

    /** Converts basic markdown to HTML so AI responses render properly */
    private String convertMarkdown(String md) {
        if (md == null) return "";
        if (md.trim().startsWith("<")) return md;
        
        String s = md;
        // Headers first
        s = s.replaceAll("(?m)^### (.+)$", "<h3>$1</h3>");
        s = s.replaceAll("(?m)^## (.+)$", "<h2>$1</h2>");
        s = s.replaceAll("(?m)^# (.+)$", "<h1>$1</h1>");
        
        // Bold & Italic
        s = s.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        s = s.replaceAll("__(.+?)__", "<strong>$1</strong>");
        s = s.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        
        // Horizontal rule
        s = s.replaceAll("(?m)^---+$", "<hr/>");
        
        // Process lists properly
        String[] lines = s.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean inUl = false, inOl = false;
        
        for (String line : lines) {
            String t = line.trim();
            
            if (t.isEmpty()) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (inOl) { sb.append("</ol>"); inOl = false; }
                sb.append("\n");
                continue;
            }
            
            // Unordered list
            if (t.matches("^[-*] .+")) {
                if (!inUl) { if (inOl) sb.append("</ol>"); inOl = false; sb.append("<ul>"); inUl = true; }
                sb.append("<li>").append(t.substring(2)).append("</li>\n");
            }
            // Ordered list
            else if (t.matches("^\\d+\\. .+")) {
                if (!inOl) { if (inUl) sb.append("</ul>"); inUl = false; sb.append("<ol>"); inOl = true; }
                sb.append("<li>").append(t.replaceFirst("^\\d+\\. ", "")).append("</li>\n");
            }
            // Blockquote
            else if (t.startsWith(">")) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (inOl) { sb.append("</ol>"); inOl = false; }
                sb.append("<blockquote>").append(t.substring(1).trim()).append("</blockquote>\n");
            }
            // Already HTML tag
            else if (t.startsWith("<")) {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (inOl) { sb.append("</ol>"); inOl = false; }
                sb.append(t).append("\n");
            }
            // Regular paragraph
            else {
                if (inUl) { sb.append("</ul>"); inUl = false; }
                if (inOl) { sb.append("</ol>"); inOl = false; }
                sb.append("<p>").append(t).append("</p>\n");
            }
        }
        
        if (inUl) sb.append("</ul>");
        if (inOl) sb.append("</ol>");
        
        return sb.toString();
    }
}
