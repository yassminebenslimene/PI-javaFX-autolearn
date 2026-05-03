package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity tracker — writes directly to the Symfony user_activity table.
 * No Symfony server required. Same table, same schema, fully compatible.
 *
 * Table: user_activity
 *   id, user_id, action, ip_address, user_agent, metadata (JSON),
 *   created_at, location, success, error_message
 */
public class ActivityApiClient {

    private static final Gson GSON = new Gson();

    // User-agent string identifying the JavaFX desktop client
    private static final String USER_AGENT = "AutoLearn-JavaFX-Desktop/1.0 (Windows)";

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "activity-tracker");
        t.setDaemon(true);
        return t;
    });

    // ── ActivityEntry record (unchanged — used by ActivitesController) ────────

    public record ActivityEntry(
        int    id,
        int    userId,
        String userName,
        String userEmail,
        String userRole,
        String action,
        boolean success,
        String ipAddress,
        String location,
        String createdAt,
        Map<String, Object> metadata
    ) {
        public String actionLabel() {
            return switch (action) {
                case "user.login"                -> "Connexion";
                case "user.logout"               -> "Deconnexion";
                case "user.created"              -> "Compte cree";
                case "user.updated"              -> "Profil modifie";
                case "user.suspended"            -> "Suspendu";
                case "user.reactivated"          -> "Reactive";
                case "user.viewed"               -> "Profil consulte";
                case "user.view_cours"           -> "Cours consultes";
                case "user.view_challenges"      -> "Challenges consultes";
                case "user.view_evenements"      -> "Evenements consultes";
                case "user.view_communaute"      -> "Communaute consultee";
                case "user.view_profile"         -> "Profil consulte";
                case "admin.created_student"     -> "Etudiant cree";
                case "admin.updated_student"     -> "Etudiant modifie";
                case "admin.suspended_student"   -> "Etudiant suspendu";
                case "admin.reactivated_student" -> "Etudiant reactive";
                case "admin.created_cours"       -> "Cours cree";
                case "admin.updated_cours"       -> "Cours modifie";
                case "admin.deleted_cours"       -> "Cours supprime";
                case "admin.created_chapitre"    -> "Chapitre cree";
                case "admin.updated_chapitre"    -> "Chapitre modifie";
                case "admin.deleted_chapitre"    -> "Chapitre supprime";
                case "admin.created_quiz"        -> "Quiz cree";
                case "admin.updated_quiz"        -> "Quiz modifie";
                case "admin.deleted_quiz"        -> "Quiz supprime";
                case "admin.created_challenge"   -> "Challenge cree";
                case "admin.updated_challenge"   -> "Challenge modifie";
                case "admin.deleted_challenge"   -> "Challenge supprime";
                case "admin.created_evenement"   -> "Evenement cree";
                case "admin.updated_evenement"   -> "Evenement modifie";
                case "admin.created_communaute"  -> "Communaute creee";
                case "admin.updated_communaute"  -> "Communaute modifiee";
                case "user.update_profile"       -> "Profil mis a jour";
                case "admin.view_dashboard"      -> "Dashboard consulte";
                case "admin.view_users"          -> "Utilisateurs consultes";
                case "admin.view_cours"          -> "Cours consultes";
                case "admin.view_challenges"     -> "Challenges consultes";
                case "admin.view_evenements"     -> "Evenements consultes";
                case "admin.view_quiz"           -> "Quiz consultes";
                case "admin.view_exercices"      -> "Exercices consultes";
                case "admin.view_communaute"     -> "Communaute consultee";
                case "admin.view_chapitres"      -> "Chapitres consultes";
                default -> action.replace("admin.", "").replace("user.", "").replace("_", " ");
            };
        }

        public String actionIcon() {
            return switch (action) {
                case "user.login"                -> "🔑";
                case "user.logout"               -> "🚪";
                case "user.created"              -> "✅";
                case "user.updated"              -> "✏️";
                case "user.suspended"            -> "⛔";
                case "user.reactivated"          -> "✔️";
                case "user.view_cours"           -> "📚";
                case "user.view_challenges"      -> "🏆";
                case "user.view_evenements"      -> "📅";
                case "user.view_communaute"      -> "👥";
                case "user.view_profile"         -> "👤";
                case "admin.created_student"     -> "✅";
                case "admin.updated_student"     -> "✏️";
                case "admin.suspended_student"   -> "⛔";
                case "admin.reactivated_student" -> "✔️";
                case "admin.created_cours"       -> "✅";
                case "admin.updated_cours"       -> "✏️";
                case "admin.deleted_cours"       -> "🗑️";
                case "admin.created_chapitre"    -> "✅";
                case "admin.updated_chapitre"    -> "✏️";
                case "admin.deleted_chapitre"    -> "🗑️";
                case "admin.created_quiz"        -> "✅";
                case "admin.updated_quiz"        -> "✏️";
                case "admin.deleted_quiz"        -> "🗑️";
                case "admin.created_challenge"   -> "✅";
                case "admin.updated_challenge"   -> "✏️";
                case "admin.deleted_challenge"   -> "🗑️";
                case "admin.created_evenement"   -> "✅";
                case "admin.updated_evenement"   -> "✏️";
                case "admin.created_communaute"  -> "✅";
                case "admin.updated_communaute"  -> "✏️";
                case "user.update_profile"       -> "✏️";
                case "admin.view_dashboard"      -> "⊞";
                case "admin.view_users"          -> "👥";
                case "admin.view_cours"          -> "📚";
                case "admin.view_challenges"     -> "🏆";
                case "admin.view_evenements"     -> "📅";
                case "admin.view_quiz"           -> "❓";
                case "admin.view_exercices"      -> "⚡";
                case "admin.view_communaute"     -> "💬";
                case "admin.view_chapitres"      -> "📖";
                default -> "•";
            };
        }
    }

    // ── WRITE: log an activity directly to user_activity table ───────────────

    /**
     * Logs an activity event directly into the Symfony user_activity table.
     * Enriches with geolocation (ip-api.com) and user-agent.
     * Fully async — never blocks the UI thread.
     *
     * @param userId   the user this activity belongs to
     * @param action   action key e.g. "user.login", "admin.created_cours"
     * @param metadata optional extra data (email, titre, reason, etc.)
     */
    public static void logAsync(int userId, String action, Map<String, Object> metadata) {
        POOL.submit(() -> {
            try {
                // Geo enrichment (non-blocking, cached by ApiService)
                ApiService.GeoInfo geo = ApiService.getMyGeoInfo();

                String ip       = geo != null ? geo.ip()                          : null;
                String location = geo != null ? geo.city() + ", " + geo.country() : null;

                // Build JSON metadata — same structure as Symfony ActivityLogger
                JsonObject meta = new JsonObject();
                meta.addProperty("source",   "JavaFX Desktop App");
                meta.addProperty("platform", "Windows");
                if (geo != null) {
                    meta.addProperty("country", geo.country());
                    meta.addProperty("city",    geo.city());
                    meta.addProperty("isp",     geo.isp());
                }
                if (metadata != null) {
                    metadata.forEach((k, v) -> meta.addProperty(k, v != null ? v.toString() : ""));
                }

                // INSERT into user_activity (same table Symfony uses)
                Connection cnx = tn.esprit.tools.MyConnection.getInstance().getConnection();
                if (cnx == null) return;

                String sql = "INSERT INTO user_activity " +
                             "(user_id, action, ip_address, user_agent, metadata, location, success, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?, 1, NOW())";

                try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, action);
                    ps.setString(3, ip);
                    ps.setString(4, USER_AGENT);
                    ps.setString(5, meta.toString());
                    ps.setString(6, location);
                    ps.executeUpdate();
                }

            } catch (Exception e) {
                // Silent — never crash the app for tracking
            }
        });
    }

    /** Convenience overload without metadata */
    public static void logAsync(int userId, String action) {
        logAsync(userId, action, null);
    }

    // ── READ: fetch activities from user_activity table ───────────────────────

    /**
     * Fetches recent activities for the admin dashboard.
     * Reads directly from MySQL — no Symfony needed.
     */
    public static CompletableFuture<List<ActivityEntry>> fetchRecentActivities(int limit) {
        return CompletableFuture.supplyAsync(() -> fetchFromDbDirect(limit), POOL);
    }

    /**
     * Fetches activities for a specific user.
     */
    public static CompletableFuture<List<ActivityEntry>> fetchUserActivities(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            List<ActivityEntry> result = new ArrayList<>();
            Connection cnx = tn.esprit.tools.MyConnection.getInstance().getConnection();
            if (cnx == null) return result;

            String sql = "SELECT ua.id, ua.user_id, ua.action, ua.ip_address, ua.location, " +
                         "       DATE_FORMAT(ua.created_at, '%d/%m/%Y %H:%i') AS created_at, " +
                         "       ua.success, " +
                         "       CONCAT(u.prenom, ' ', u.nom) AS user_name, " +
                         "       u.email AS user_email, u.role AS user_role " +
                         "FROM user_activity ua " +
                         "JOIN user u ON ua.user_id = u.userId " +
                         "WHERE ua.user_id = ? " +
                         "ORDER BY ua.created_at DESC " +
                         "LIMIT 200";
            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            } catch (Exception e) {
                System.err.println("[ActivityTracker] fetchUser: " + e.getMessage());
            }
            return result;
        }, POOL);
    }

    /**
     * Reads all recent activities from user_activity — used by ActivitesController.
     */
    public static List<ActivityEntry> fetchFromDbDirect(int limit) {
        List<ActivityEntry> result = new ArrayList<>();
        Connection cnx = tn.esprit.tools.MyConnection.getInstance().getConnection();
        if (cnx == null) return result;

        String sql = "SELECT ua.id, ua.user_id, ua.action, ua.ip_address, ua.location, " +
                     "       DATE_FORMAT(ua.created_at, '%d/%m/%Y %H:%i') AS created_at, " +
                     "       ua.success, " +
                     "       CONCAT(u.prenom, ' ', u.nom) AS user_name, " +
                     "       u.email AS user_email, u.role AS user_role " +
                     "FROM user_activity ua " +
                     "JOIN user u ON ua.user_id = u.userId " +
                     "ORDER BY ua.created_at DESC " +
                     "LIMIT ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            System.out.println("[ActivityTracker] Loaded " + result.size() + " entries");
        } catch (Exception e) {
            System.err.println("[ActivityTracker] " + e.getMessage());
        }
        return result;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static ActivityEntry mapRow(ResultSet rs) throws Exception {
        return new ActivityEntry(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getString("user_name"),
            rs.getString("user_email"),
            rs.getString("user_role"),
            rs.getString("action"),
            rs.getBoolean("success"),
            rs.getString("ip_address"),
            rs.getString("location"),
            rs.getString("created_at"),
            null
        );
    }
}
