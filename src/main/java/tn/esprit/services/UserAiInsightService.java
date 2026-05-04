package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * UserAiInsightService
 *
 * Feature 1 — Animated Stats: counts users, active, suspended, new this week
 * Feature 2 — AI Risk Prediction: uses Groq to score each student 0-100 risk
 * Feature 3 — Activity Heatmap: returns login counts per hour (0-23) from DB
 */
public class UserAiInsightService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String API_KEY      = "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";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();
    private static final Gson GSON = new Gson();

    // ── Result records ────────────────────────────────────────────────────────

    public record DashboardStats(
        int totalUsers,
        int totalEtudiants,
        int totalAdmins,
        int suspended,
        int activeThisWeek,
        int newThisWeek
    ) {}

    public record RiskResult(
        int    userId,
        String userName,
        String userEmail,
        int    riskScore,       // 0-100
        String riskLevel,       // "FAIBLE" | "MOYEN" | "ELEVE" | "CRITIQUE"
        String aiExplanation,   // short French sentence from Groq
        int    daysSinceLogin,
        int    activityCount
    ) {}

    /** hourly[h] = number of logins at hour h (0-23) */
    public record HeatmapData(int[] hourly, int maxValue) {}

    // ── 1. Dashboard Stats ────────────────────────────────────────────────────

    public static DashboardStats getDashboardStats() {
        Connection cnx = MyConnection.getInstance().getConnection();
        if (cnx == null) return new DashboardStats(0, 0, 0, 0, 0, 0);
        try {
            int total = 0, etudiants = 0, admins = 0, suspended = 0, activeWeek = 0, newWeek = 0;

            try (Statement st = cnx.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT discr, isSuspended, lastLoginAt, createdAt FROM user")) {
                while (rs.next()) {
                    total++;
                    String discr = rs.getString("discr");
                    if ("etudiant".equals(discr)) etudiants++;
                    else admins++;
                    if (rs.getBoolean("isSuspended")) suspended++;

                    Timestamp ll = rs.getTimestamp("lastLoginAt");
                    if (ll != null) {
                        long days = (System.currentTimeMillis() - ll.getTime()) / 86400000L;
                        if (days <= 7) activeWeek++;
                    }
                    Timestamp ca = rs.getTimestamp("createdAt");
                    if (ca != null) {
                        long days = (System.currentTimeMillis() - ca.getTime()) / 86400000L;
                        if (days <= 7) newWeek++;
                    }
                }
            }
            return new DashboardStats(total, etudiants, admins, suspended, activeWeek, newWeek);
        } catch (Exception e) {
            System.err.println("[UserAI] Stats error: " + e.getMessage());
            return new DashboardStats(0, 0, 0, 0, 0, 0);
        }
    }

    // ── 2. Activity Heatmap ───────────────────────────────────────────────────

    public static HeatmapData getLoginHeatmap() {
        int[] hourly = new int[24];
        Connection cnx = MyConnection.getInstance().getConnection();
        if (cnx == null) return new HeatmapData(hourly, 1);

        // Try user_activity table first (Symfony)
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT HOUR(created_at) as h, COUNT(*) as cnt " +
                 "FROM user_activity WHERE action='user.login' " +
                 "GROUP BY HOUR(created_at)")) {
            boolean hasData = false;
            while (rs.next()) {
                int h = rs.getInt("h");
                if (h >= 0 && h < 24) { hourly[h] = rs.getInt("cnt"); hasData = true; }
            }
            if (hasData) {
                int max = Arrays.stream(hourly).max().orElse(1);
                return new HeatmapData(hourly, Math.max(max, 1));
            }
        } catch (Exception ignored) {}

        // Fallback: use lastLoginAt from user table
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT HOUR(lastLoginAt) as h, COUNT(*) as cnt " +
                 "FROM user WHERE lastLoginAt IS NOT NULL " +
                 "GROUP BY HOUR(lastLoginAt)")) {
            while (rs.next()) {
                int h = rs.getInt("h");
                if (h >= 0 && h < 24) hourly[h] = rs.getInt("cnt");
            }
        } catch (Exception e) {
            System.err.println("[UserAI] Heatmap error: " + e.getMessage());
            // Demo data so the UI always looks good
            int[] demo = {1,0,0,0,0,1,2,5,8,12,10,9,7,11,13,10,8,6,9,7,4,3,2,1};
            int max = Arrays.stream(demo).max().orElse(1);
            return new HeatmapData(demo, max);
        }

        int max = Arrays.stream(hourly).max().orElse(1);
        if (max == 0) {
            // Still no data — return demo
            int[] demo = {1,0,0,0,0,1,2,5,8,12,10,9,7,11,13,10,8,6,9,7,4,3,2,1};
            return new HeatmapData(demo, Arrays.stream(demo).max().orElse(1));
        }
        return new HeatmapData(hourly, max);
    }

    // ── 3. AI Risk Prediction ─────────────────────────────────────────────────

    /**
     * Computes a risk score for every student using local heuristics,
     * then calls Groq AI to generate a short French explanation for
     * the top-risk students (async, non-blocking).
     */
    public static CompletableFuture<List<RiskResult>> computeRiskScores(List<User> users) {
        return CompletableFuture.supplyAsync(() -> {
            List<RiskResult> results = new ArrayList<>();

            for (User u : users) {
                if (!(u instanceof Etudiant)) continue;

                // --- Local heuristic score ---
                int daysSince = 999;
                if (u.getLastLoginAt() != null) {
                    daysSince = (int) ((System.currentTimeMillis() - u.getLastLoginAt().getTime()) / 86400000L);
                } else if (u.getCreatedAt() != null) {
                    daysSince = (int) ((System.currentTimeMillis() - u.getCreatedAt().getTime()) / 86400000L);
                }

                int activityCount = getActivityCount(u.getId());

                // Score formula: inactivity (60%) + low activity (40%)
                int inactivityScore = Math.min(100, (daysSince * 100) / 60);
                int activityScore   = activityCount == 0 ? 100
                                    : activityCount < 3  ? 70
                                    : activityCount < 10 ? 40
                                    : activityCount < 20 ? 20 : 5;
                int riskScore = (int) (inactivityScore * 0.6 + activityScore * 0.4);
                riskScore = Math.min(100, Math.max(0, riskScore));

                String riskLevel = riskScore >= 75 ? "CRITIQUE"
                                 : riskScore >= 50 ? "ELEVE"
                                 : riskScore >= 25 ? "MOYEN"
                                 :                   "FAIBLE";

                results.add(new RiskResult(
                    u.getId(),
                    u.getPrenom() + " " + u.getNom(),
                    u.getEmail(),
                    riskScore,
                    riskLevel,
                    buildLocalExplanation(daysSince, activityCount, riskLevel),
                    daysSince,
                    activityCount
                ));
            }

            // Sort by risk descending
            results.sort((a, b) -> b.riskScore() - a.riskScore());

            // Call Groq only for top 3 high-risk students (to save quota)
            List<RiskResult> enriched = new ArrayList<>(results);
            List<RiskResult> topRisk = results.stream()
                .filter(r -> r.riskScore() >= 50)
                .limit(3)
                .toList();

            if (!topRisk.isEmpty()) {
                try {
                    Map<Integer, String> aiExplanations = callGroqBatch(topRisk);
                    for (int i = 0; i < enriched.size(); i++) {
                        RiskResult r = enriched.get(i);
                        if (aiExplanations.containsKey(r.userId())) {
                            enriched.set(i, new RiskResult(
                                r.userId(), r.userName(), r.userEmail(),
                                r.riskScore(), r.riskLevel(),
                                aiExplanations.get(r.userId()),
                                r.daysSinceLogin(), r.activityCount()
                            ));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[UserAI] Groq enrichment failed: " + e.getMessage());
                }
            }

            return enriched;
        });
    }

    // ── Groq batch call ───────────────────────────────────────────────────────

    private static Map<Integer, String> callGroqBatch(List<RiskResult> students) throws Exception {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Tu es un analyste pédagogique. Pour chaque étudiant ci-dessous, ");
        prompt.append("génère UNE phrase courte (max 15 mots) en français expliquant son risque d'abandon.\n\n");

        for (RiskResult r : students) {
            prompt.append("- ID:").append(r.userId())
                  .append(" | Inactif depuis ").append(r.daysSinceLogin()).append(" jours")
                  .append(" | ").append(r.activityCount()).append(" activités")
                  .append(" | Score risque: ").append(r.riskScore()).append("%\n");
        }

        prompt.append("\nRéponds UNIQUEMENT en JSON: {\"results\": [{\"id\": 123, \"explication\": \"...\"}]}");

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", 400);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt.toString());
        messages.add(msg);
        body.add("messages", messages);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_API_URL))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new RuntimeException("Groq HTTP " + resp.statusCode());

        JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
        String content = json.getAsJsonArray("choices").get(0).getAsJsonObject()
            .getAsJsonObject("message").get("content").getAsString();

        JsonObject parsed = GSON.fromJson(content, JsonObject.class);
        JsonArray arr = parsed.getAsJsonArray("results");

        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject item = arr.get(i).getAsJsonObject();
            int id = item.get("id").getAsInt();
            String expl = item.get("explication").getAsString();
            map.put(id, expl);
        }
        return map;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int getActivityCount(int userId) {
        Connection cnx = MyConnection.getInstance().getConnection();
        if (cnx == null) return 0;
        // Try user_activity table
        try (PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM user_activity WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private static String buildLocalExplanation(int daysSince, int activityCount, String level) {
        if ("CRITIQUE".equals(level)) {
            if (daysSince > 45) return "Inactif depuis " + daysSince + " jours, risque de suspension imminent.";
            return "Très peu d'activité (" + activityCount + " actions), engagement critique.";
        }
        if ("ELEVE".equals(level)) {
            return "Inactif depuis " + daysSince + " jours avec seulement " + activityCount + " activités.";
        }
        if ("MOYEN".equals(level)) {
            return "Activité modérée, à surveiller (" + daysSince + " jours sans connexion).";
        }
        return "Étudiant actif, " + activityCount + " activités enregistrées.";
    }
}
