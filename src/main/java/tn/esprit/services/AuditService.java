package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the Doctrine EntityAudit tables (revisions + *_audit) that were
 * created by the Symfony backend.
 *
 * Schema:
 *   revisions(id, timestamp, username)
 *   cours_audit(id, titre, ..., rev, revtype)
 *   challenge_audit(id, titre, ..., rev, revtype)
 *   evenement_audit(id, titre, ..., rev, revtype)
 *   quiz_audit(id, titre, ..., rev, revtype)
 *   chapitre_audit(id, titre, ..., rev, revtype)
 *   commentaire_audit(id, contenu, user_id, ..., rev, revtype)
 *   post_audit(id, contenu, user_id, ..., rev, revtype)
 *
 * revtype values: INS = created, UPD = updated, DEL = deleted
 */
public class AuditService {

    // ── Data record ───────────────────────────────────────────────────────────

    public record AuditEntry(
        int    revId,
        String timestamp,
        String username,       // Symfony user who performed the action
        String entityType,     // "Cours", "Challenge", "Evenement", etc.
        int    entityId,
        String entityLabel,    // titre / contenu preview
        String revType         // INS / UPD / DEL
    ) {
        /** Human-readable action label */
        public String actionLabel() {
            return switch (revType) {
                case "INS" -> "Création";
                case "UPD" -> "Modification";
                case "DEL" -> "Suppression";
                default    -> revType;
            };
        }

        /** Emoji icon per action */
        public String actionIcon() {
            return switch (revType) {
                case "INS" -> "✅";
                case "UPD" -> "✏️";
                case "DEL" -> "🗑️";
                default    -> "•";
            };
        }

        /** Emoji icon per entity type */
        public String entityIcon() {
            return switch (entityType) {
                case "Cours"       -> "📚";
                case "Challenge"   -> "🏆";
                case "Evenement"   -> "📅";
                case "Quiz"        -> "❓";
                case "Chapitre"    -> "📖";
                case "Commentaire" -> "💬";
                case "Post"        -> "📝";
                case "Communaute"  -> "👥";
                case "Equipe"      -> "🤝";
                case "Exercice"    -> "⚡";
                default            -> "•";
            };
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns all audit entries across all tracked entities,
     * ordered by revision timestamp descending (most recent first).
     *
     * @param limit max number of entries to return (0 = no limit)
     */
    public List<AuditEntry> getAllAuditEntries(int limit) {
        List<AuditEntry> entries = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getConnection();
        if (cnx == null) return entries;

        // UNION across all audit tables
        String sql = buildUnionQuery(limit);
        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                entries.add(new AuditEntry(
                    rs.getInt("rev_id"),
                    rs.getString("ts"),
                    rs.getString("username"),
                    rs.getString("entity_type"),
                    rs.getInt("entity_id"),
                    rs.getString("entity_label"),
                    rs.getString("revtype")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[AuditService] getAllAuditEntries: " + e.getMessage());
        }
        return entries;
    }

    /**
     * Returns audit entries filtered by username (Symfony user).
     */
    public List<AuditEntry> getAuditEntriesByUser(String username, int limit) {
        List<AuditEntry> all = getAllAuditEntries(0);
        return all.stream()
            .filter(e -> username.equalsIgnoreCase(e.username()))
            .limit(limit > 0 ? limit : Long.MAX_VALUE)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns audit entries filtered by entity type.
     */
    public List<AuditEntry> getAuditEntriesByType(String entityType, int limit) {
        List<AuditEntry> all = getAllAuditEntries(0);
        return all.stream()
            .filter(e -> entityType.equalsIgnoreCase(e.entityType()))
            .limit(limit > 0 ? limit : Long.MAX_VALUE)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns all distinct usernames that appear in the revisions table.
     */
    public List<String> getAllAuditUsers() {
        List<String> users = new ArrayList<>();
        Connection cnx = MyConnection.getInstance().getConnection();
        if (cnx == null) return users;
        String sql = "SELECT DISTINCT username FROM revisions WHERE username IS NOT NULL ORDER BY username";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(rs.getString("username"));
        } catch (SQLException e) {
            System.err.println("[AuditService] getAllAuditUsers: " + e.getMessage());
        }
        return users;
    }

    /**
     * Returns summary stats: total revisions, per entity type counts.
     */
    public java.util.Map<String, Integer> getAuditStats() {
        java.util.Map<String, Integer> stats = new java.util.LinkedHashMap<>();
        List<AuditEntry> all = getAllAuditEntries(0);
        stats.put("Total", all.size());
        stats.put("Créations",     (int) all.stream().filter(e -> "INS".equals(e.revType())).count());
        stats.put("Modifications", (int) all.stream().filter(e -> "UPD".equals(e.revType())).count());
        stats.put("Suppressions",  (int) all.stream().filter(e -> "DEL".equals(e.revType())).count());
        return stats;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildUnionQuery(int limit) {
        // Each sub-query: join audit table with revisions to get timestamp + username
        String[] tables = {
            "cours_audit",       "Cours",       "titre",
            "challenge_audit",   "Challenge",   "titre",
            "evenement_audit",   "Evenement",   "titre",
            "quiz_audit",        "Quiz",        "titre",
            "chapitre_audit",    "Chapitre",    "titre",
            "commentaire_audit", "Commentaire", "contenu",
            "post_audit",        "Post",        "contenu",
            "communaute_audit",  "Communaute",  "nom",
            "equipe_audit",      "Equipe",      "nom",
            "exercice_audit",    "Exercice",    "question",
        };

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tables.length; i += 3) {
            String table      = tables[i];
            String entityType = tables[i + 1];
            String labelCol   = tables[i + 2];

            if (sb.length() > 0) sb.append("\nUNION ALL\n");
            sb.append(
                "SELECT r.id AS rev_id, " +
                "       DATE_FORMAT(r.timestamp, '%d/%m/%Y %H:%i') AS ts, " +
                "       COALESCE(r.username, 'Système') AS username, " +
                "       '" + entityType + "' AS entity_type, " +
                "       a.id AS entity_id, " +
                "       COALESCE(LEFT(a." + labelCol + ", 60), '—') AS entity_label, " +
                "       a.revtype " +
                "FROM " + table + " a " +
                "JOIN revisions r ON a.rev = r.id"
            );
        }

        sb.append("\nORDER BY rev_id DESC");
        if (limit > 0) sb.append("\nLIMIT ").append(limit);
        return sb.toString();
    }
}
