package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePost {

    // Toujours récupérer la connexion fraîche (auto-reconnect)
    private Connection conn() {
        return MyConnection.getInstance().getConnection();
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try {
            ResultSet rs = conn().createStatement().executeQuery("SELECT * FROM post ORDER BY created_at DESC");
            while (rs.next()) list.add(fromRs(rs));
        } catch (SQLException e) { System.err.println("[ServicePost] getAll: " + e.getMessage()); }
        return list;
    }

    public List<Post> getByCommunaute(int communauteId) {
        List<Post> list = new ArrayList<>();
        String req = "SELECT * FROM post WHERE communaute_id=? ORDER BY created_at DESC";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, communauteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(fromRs(rs));
            System.out.println("[ServicePost] getByCommunaute(" + communauteId + ") -> " + list.size() + " posts");
        } catch (SQLException e) { System.err.println("[ServicePost] getByCommunaute: " + e.getMessage()); }
        return list;
    }

    /**
     * Hot Ranking Algorithm — Reddit-style:
     *
     *   score = (likes * 2 + nb_comments * 1.5 + 1) / POW(age_hours + 2, 1.8)
     *
     * - likes    = COALESCE(ai_reaction, 0)  — nombre de réactions IA
     * - comments = COUNT(commentaire)
     * - age      = TIMESTAMPDIFF(HOUR, created_at, NOW())
     * - +2 dans le dénominateur : évite division par zéro
     * - POW(x, 1.8) : décroissance progressive de l'âge
     */
    public List<Post> getHotByCommunaute(int communauteId) {
        List<Post> list = new ArrayList<>();
        String req =
            "SELECT p.*, " +
            "  COUNT(c.id)                                          AS nb_comments, " +
            "  COALESCE(CAST(p.ai_reaction AS UNSIGNED), 0)        AS nb_likes, " +
            "  TIMESTAMPDIFF(HOUR, p.created_at, NOW())            AS age_hours, " +
            "  (COALESCE(CAST(p.ai_reaction AS UNSIGNED), 0) * 2   " +
            "   + COUNT(c.id) * 1.5 + 1)                           " +
            "  / POW(TIMESTAMPDIFF(HOUR, p.created_at, NOW()) + 2, 1.8) AS hot_score " +
            "FROM post p " +
            "LEFT JOIN commentaire c ON c.post_id = p.id " +
            "WHERE p.communaute_id = ? " +
            "GROUP BY p.id " +
            "ORDER BY hot_score DESC";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, communauteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Post post = fromRs(rs);
                double score    = rs.getDouble("hot_score");
                int    comments = rs.getInt("nb_comments");
                int    likes    = rs.getInt("nb_likes");
                int    age      = rs.getInt("age_hours");
                System.out.printf("[ServicePost] HOT post#%d score=%.4f likes=%d comments=%d age=%dh%n",
                        post.getId(), score, likes, comments, age);
                list.add(post);
            }
            System.out.println("[ServicePost] getHotByCommunaute(" + communauteId + ") -> " + list.size() + " posts");
        } catch (SQLException e) {
            System.err.println("[ServicePost] getHotByCommunaute: " + e.getMessage());
            // fallback to date order
            return getByCommunaute(communauteId);
        }
        return list;
    }

    /**
     * Hot Ranking global — tous posts confondus, même formule que getHotByCommunaute.
     * Retourne une liste de PostHotEntry (Post + score + nb_comments + nb_likes + age_hours).
     */
    public List<PostHotEntry> getHotAll() {
        List<PostHotEntry> list = new ArrayList<>();
        String req =
            "SELECT p.*, " +
            "  COUNT(c.id)                                          AS nb_comments, " +
            "  COALESCE(CAST(p.ai_reaction AS UNSIGNED), 0)        AS nb_likes, " +
            "  TIMESTAMPDIFF(HOUR, p.created_at, NOW())            AS age_hours, " +
            "  (COALESCE(CAST(p.ai_reaction AS UNSIGNED), 0) * 2   " +
            "   + COUNT(c.id) * 1.5 + 1)                           " +
            "  / POW(TIMESTAMPDIFF(HOUR, p.created_at, NOW()) + 2, 1.8) AS hot_score " +
            "FROM post p " +
            "LEFT JOIN commentaire c ON c.post_id = p.id " +
            "GROUP BY p.id " +
            "ORDER BY hot_score DESC";
        try {
            ResultSet rs = conn().createStatement().executeQuery(req);
            while (rs.next()) {
                Post post    = fromRs(rs);
                double score = rs.getDouble("hot_score");
                int comments = rs.getInt("nb_comments");
                int likes    = rs.getInt("nb_likes");
                int age      = rs.getInt("age_hours");
                list.add(new PostHotEntry(post, score, comments, likes, age));
            }
        } catch (SQLException e) {
            System.err.println("[ServicePost] getHotAll: " + e.getMessage());
        }
        return list;
    }

    /** Wrapper léger pour transporter un Post + ses métriques hot. */
    public static class PostHotEntry {
        public final Post   post;
        public final double hotScore;
        public final int    nbComments;
        public final int    nbLikes;
        public final int    ageHours;

        public PostHotEntry(Post post, double hotScore, int nbComments, int nbLikes, int ageHours) {
            this.post       = post;
            this.hotScore   = hotScore;
            this.nbComments = nbComments;
            this.nbLikes    = nbLikes;
            this.ageHours   = ageHours;
        }
    }

    public Post getById(int id) {
        String req = "SELECT * FROM post WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return fromRs(rs);
        } catch (SQLException e) { System.err.println("[ServicePost] getById: " + e.getMessage()); }
        return null;
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    public void ajouter(Post p) {
        // Auto-extract tags from titre + contenu before saving
        if (p.getTags() == null || p.getTags().isBlank()) {
            p.setTags(extractTags(p.getTitre(), p.getContenu()));
        }

        // Ensure tags column exists (added later — may be missing on some installs)
        try {
            conn().createStatement().executeUpdate(
                "ALTER TABLE post ADD COLUMN IF NOT EXISTS tags VARCHAR(500) NULL DEFAULT NULL");
        } catch (SQLException ignored) {}

        // Also ensure communaute_id and user_id columns exist
        try {
            conn().createStatement().executeUpdate(
                "ALTER TABLE post ADD COLUMN IF NOT EXISTS communaute_id INT NULL DEFAULT NULL");
        } catch (SQLException ignored) {}
        try {
            conn().createStatement().executeUpdate(
                "ALTER TABLE post ADD COLUMN IF NOT EXISTS user_id INT NULL DEFAULT NULL");
        } catch (SQLException ignored) {}

        String req = "INSERT INTO post (contenu, titre, ai_reaction, ai_reaction_data, summary, " +
                     "image_file, video_file, created_at, communaute_id, user_id, tags) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn().prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getContenu());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getAiReaction());
            ps.setString(4, p.getAiReactionData());
            ps.setString(5, p.getSummary());
            ps.setString(6, p.getImageFile());
            ps.setString(7, p.getVideoFile());
            ps.setTimestamp(8, p.getCreatedAt() != null
                ? Timestamp.valueOf(p.getCreatedAt())
                : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setInt(9, p.getCommunauteId());
            ps.setInt(10, p.getUserId());
            ps.setString(11, p.getTags());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                p.setId(keys.getInt(1));
                System.out.println("[ServicePost] ajouter OK id=" + p.getId() + " tags=" + p.getTags());
            }
        } catch (SQLException e) {
            System.err.println("[ServicePost] ajouter ERREUR: " + e.getMessage());
            // Fallback: insert without tags column
            String reqFallback = "INSERT INTO post (contenu, titre, ai_reaction, ai_reaction_data, summary, " +
                         "image_file, video_file, created_at, communaute_id, user_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try {
                PreparedStatement ps2 = conn().prepareStatement(reqFallback, Statement.RETURN_GENERATED_KEYS);
                ps2.setString(1, p.getContenu());
                ps2.setString(2, p.getTitre());
                ps2.setString(3, p.getAiReaction());
                ps2.setString(4, p.getAiReactionData());
                ps2.setString(5, p.getSummary());
                ps2.setString(6, p.getImageFile());
                ps2.setString(7, p.getVideoFile());
                ps2.setTimestamp(8, p.getCreatedAt() != null
                    ? Timestamp.valueOf(p.getCreatedAt())
                    : Timestamp.valueOf(java.time.LocalDateTime.now()));
                ps2.setInt(9, p.getCommunauteId());
                ps2.setInt(10, p.getUserId());
                ps2.executeUpdate();
                ResultSet keys2 = ps2.getGeneratedKeys();
                if (keys2.next()) {
                    p.setId(keys2.getInt(1));
                    System.out.println("[ServicePost] ajouter fallback OK id=" + p.getId());
                }
            } catch (SQLException e2) {
                System.err.println("[ServicePost] ajouter fallback ERREUR: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }

    public void modifier(Post p) {
        // Ensure tags column exists
        try {
            conn().createStatement().executeUpdate(
                "ALTER TABLE post ADD COLUMN IF NOT EXISTS tags VARCHAR(500) NULL DEFAULT NULL");
        } catch (SQLException ignored) {}

        String req = "UPDATE post SET contenu=?, titre=?, ai_reaction=?, ai_reaction_data=?, " +
                     "summary=?, image_file=?, video_file=?, communaute_id=?, user_id=?, tags=? WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setString(1, p.getContenu());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getAiReaction());
            ps.setString(4, p.getAiReactionData());
            ps.setString(5, p.getSummary());
            ps.setString(6, p.getImageFile());
            ps.setString(7, p.getVideoFile());
            ps.setInt(8, p.getCommunauteId());
            ps.setInt(9, p.getUserId());
            ps.setString(10, p.getTags());
            ps.setInt(11, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("[ServicePost] modifier: " + e.getMessage()); }
    }

    public void supprimer(Post p) {
        String req = "DELETE FROM post WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("[ServicePost] supprimer: " + e.getMessage()); }
    }

    // ── Similarity & Tags ─────────────────────────────────────────────────────

    /**
     * Jaccard Similarity: |A ∩ B| / |A ∪ B|
     * Returns 0.0 → 1.0
     */
    public double jaccardSimilarity(Post a, Post b) {
        java.util.Set<String> tagsA = a.getTagSet();
        java.util.Set<String> tagsB = b.getTagSet();
        if (tagsA.isEmpty() && tagsB.isEmpty()) return 0.0;

        java.util.Set<String> intersection = new java.util.HashSet<>(tagsA);
        intersection.retainAll(tagsB);

        java.util.Set<String> union = new java.util.HashSet<>(tagsA);
        union.addAll(tagsB);

        double score = (double) intersection.size() / union.size();
        System.out.printf("[Jaccard] post#%d ↔ post#%d  ∩=%d ∪=%d  score=%.3f%n",
                a.getId(), b.getId(), intersection.size(), union.size(), score);
        return score;
    }

    /**
     * Returns top-N similar posts to the given post (from same communauté),
     * sorted by Jaccard score DESC, minimum threshold 0.1
     */
    public List<Post> getSimilarPosts(Post source, int communauteId, int topN) {
        List<Post> all = getByCommunaute(communauteId);
        return all.stream()
            .filter(p -> p.getId() != source.getId())
            .map(p -> new Object[]{ p, jaccardSimilarity(source, p) })
            .filter(pair -> (double) pair[1] >= 0.1)
            .sorted((x, y) -> Double.compare((double) y[1], (double) x[1]))
            .limit(topN)
            .map(pair -> (Post) pair[0])
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Auto-extracts tags from titre + contenu.
     * Keeps words ≥ 4 chars, removes stop-words, lowercases, deduplicates.
     * Returns comma-separated string: "java,spring,backend"
     */
    public static String extractTags(String titre, String contenu) {
        java.util.Set<String> stopWords = java.util.Set.of(
            "pour", "dans", "avec", "cette", "votre", "notre", "vous", "nous",
            "les", "des", "une", "est", "que", "qui", "par", "sur", "mais",
            "tout", "plus", "bien", "aussi", "comme", "when", "what", "this",
            "that", "with", "from", "have", "will", "been", "they", "their"
        );
        String text = ((titre != null ? titre : "") + " " + (contenu != null ? contenu : ""))
                .toLowerCase()
                .replaceAll("[^a-zàâäéèêëîïôùûüç0-9\\s]", " ");

        java.util.LinkedHashSet<String> tags = new java.util.LinkedHashSet<>();
        for (String word : text.split("\\s+")) {
            if (word.length() >= 4 && !stopWords.contains(word) && tags.size() < 10) {
                tags.add(word);
            }
        }
        return String.join(",", tags);
    }

    // ── Helper privé ─────────────────────────────────────────────────────────

    private Post fromRs(ResultSet rs) throws SQLException {
        Post p = new Post(
            rs.getInt("id"),
            rs.getString("contenu"),
            rs.getString("titre"),
            rs.getString("ai_reaction"),
            rs.getString("ai_reaction_data"),
            rs.getString("summary"),
            rs.getString("image_file"),
            rs.getString("video_file"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
            rs.getInt("communaute_id"),
            rs.getInt("user_id")
        );
        try { p.setTags(rs.getString("tags")); } catch (SQLException ignored) {}
        return p;
    }
}
