package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Time-Sensitive Trend Analyzer
 * ─────────────────────────────────────────────────────────────────────────────
 * Algorithm:
 *   1. Fetch all posts + comments from the last 24h (configurable window)
 *   2. Tokenize text → extract meaningful keywords (≥4 chars, no stop-words)
 *   3. Count frequency per keyword weighted by recency:
 *        weight = 1 / (1 + age_hours * 0.1)   → recent posts count more
 *   4. trend_score = frequency × avg_weight × log(1 + frequency)
 *   5. Return top-N keywords with score ≥ threshold
 */
public class TrendAnalyzerService {

    private Connection conn() { return MyConnection.getInstance().getConnection(); }

    private static final int    WINDOW_HOURS = 24;
    private static final int    MIN_FREQ     = 2;    // must appear ≥2 times
    private static final double THRESHOLD    = 0.5;
    private static final int    TOP_N        = 8;

    private static final Set<String> STOP_WORDS = Set.of(
        "pour", "dans", "avec", "cette", "votre", "notre", "vous", "nous",
        "les", "des", "une", "est", "que", "qui", "par", "sur", "mais",
        "tout", "plus", "bien", "aussi", "comme", "when", "what", "this",
        "that", "with", "from", "have", "will", "been", "they", "their",
        "bonjour", "salut", "merci", "hello", "cest", "cela",
        "alors", "comment", "faire", "utiliser", "avoir"
    );

    public record TrendWord(String word, int count, double score) {}

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Analyzes posts + comments from the last WINDOW_HOURS hours
     * for a given communauté and returns trending keywords.
     */
    public List<TrendWord> getTrends(int communauteId) {
        // word → [total_weighted_score, count]
        Map<String, double[]> wordStats = new LinkedHashMap<>();

        fetchPostTexts(communauteId, wordStats);
        fetchCommentTexts(communauteId, wordStats);

        // Compute final trend score
        List<TrendWord> trends = new ArrayList<>();
        for (var entry : wordStats.entrySet()) {
            double[] stats  = entry.getValue();
            double   count  = stats[0];
            double   wSum   = stats[1];
            if (count < MIN_FREQ) continue;
            double avgWeight = wSum / count;
            double score     = count * avgWeight * Math.log(1 + count);
            if (score >= THRESHOLD) {
                trends.add(new TrendWord(entry.getKey(), (int) count, score));
            }
        }

        trends.sort(Comparator.comparingDouble(TrendWord::score).reversed());
        List<TrendWord> result = trends.stream().limit(TOP_N).collect(Collectors.toList());

        System.out.println("[Trends] communaute#" + communauteId + " → " +
            result.stream().map(t -> t.word() + "(" + String.format("%.2f", t.score()) + ")")
                  .collect(Collectors.joining(", ")));
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void fetchPostTexts(int communauteId, Map<String, double[]> stats) {
        String sql = "SELECT CONCAT(COALESCE(titre,''), ' ', COALESCE(contenu,''), ' ', COALESCE(tags,'')) AS text, " +
                     "TIMESTAMPDIFF(HOUR, created_at, NOW()) AS age_h " +
                     "FROM post " +
                     "WHERE communaute_id = ? " +
                     "  AND created_at >= NOW() - INTERVAL " + WINDOW_HOURS + " HOUR";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, communauteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double weight = recencyWeight(rs.getInt("age_h"));
                tokenize(rs.getString("text"), weight, stats);
            }
        } catch (SQLException e) { System.err.println("[Trends] posts: " + e.getMessage()); }
    }

    private void fetchCommentTexts(int communauteId, Map<String, double[]> stats) {
        String dateCol = detectDateCol();
        String sql = "SELECT c.contenu AS text, " +
                     "TIMESTAMPDIFF(HOUR, c." + dateCol + ", NOW()) AS age_h " +
                     "FROM commentaire c " +
                     "JOIN post p ON p.id = c.post_id " +
                     "WHERE p.communaute_id = ? " +
                     "  AND c." + dateCol + " >= NOW() - INTERVAL " + WINDOW_HOURS + " HOUR";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, communauteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double weight = recencyWeight(rs.getInt("age_h"));
                tokenize(rs.getString("text"), weight, stats);
            }
        } catch (SQLException e) { System.err.println("[Trends] comments: " + e.getMessage()); }
    }

    /**
     * Recency weight: recent content scores higher.
     * weight = 1 / (1 + age_hours × 0.08)
     * age=0h → 1.0,  age=12h → 0.51,  age=24h → 0.34
     */
    private double recencyWeight(int ageHours) {
        return 1.0 / (1.0 + ageHours * 0.08);
    }

    private void tokenize(String text, double weight, Map<String, double[]> stats) {
        if (text == null || text.isBlank()) return;
        String normalized = text.toLowerCase()
                .replaceAll("[^a-zàâäéèêëîïôùûüç0-9#\\s]", " ");
        for (String token : normalized.split("\\s+")) {
            // Handle hashtags
            String word = token.startsWith("#") ? token.substring(1) : token;
            if (word.length() < 3 || STOP_WORDS.contains(word)) continue;
            stats.computeIfAbsent(word, k -> new double[]{0, 0});
            stats.get(word)[0]++;       // count
            stats.get(word)[1] += weight; // weighted sum
        }
    }

    private String detectDateCol() {
        try {
            ResultSet rs = conn().getMetaData().getColumns(null, null, "commentaire", null);
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col.equalsIgnoreCase("created_at") || col.equalsIgnoreCase("creaed_at"))
                    return col;
            }
        } catch (Exception ignored) {}
        return "created_at";
    }
}
