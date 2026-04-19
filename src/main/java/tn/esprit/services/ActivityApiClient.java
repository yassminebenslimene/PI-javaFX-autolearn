package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP client that calls the Symfony ActivityApiController.
 *
 * Base URL: http://localhost:8000  (change to your Symfony server URL)
 * Auth:     X-App-Token header (shared secret)
 *
 * All calls are async — the UI never blocks.
 */
public class ActivityApiClient {

    private static final String BASE_URL   = "http://localhost:8000";
    private static final String APP_TOKEN  = "autolearn-javafx-2026";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(4))
        .build();
    private static final Gson GSON = new Gson();

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "activity-api");
        t.setDaemon(true);
        return t;
    });

    // ── Activity record (returned by GET endpoints) ───────────────────────────

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
        /** Human-readable action label */
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

        /** Emoji icon per action */
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

    // ── POST: log an activity ─────────────────────────────────────────────────

    /**
     * Logs an activity event to the Symfony backend asynchronously.
     * Enriches the payload with geo info from ip-api.com.
     *
     * @param userId   the user this activity belongs to
     * @param action   action key e.g. "user.login", "user.suspended"
     * @param metadata optional extra data (reason, changes, etc.)
     */
    public static void logAsync(int userId, String action, Map<String, Object> metadata) {
        POOL.submit(() -> {
            try {
                // Fetch geo info (may be null if offline)
                ApiService.GeoInfo geo = ApiService.getMyGeoInfo();

                JsonObject body = new JsonObject();
                body.addProperty("userId",  userId);
                body.addProperty("action",  action);
                body.addProperty("success", true);
                if (geo != null) {
                    body.addProperty("ipAddress", geo.ip());
                    body.addProperty("location",  geo.city() + ", " + geo.country());
                }

                // Merge metadata
                JsonObject meta = new JsonObject();
                meta.addProperty("source", "JavaFX Desktop App");
                if (geo != null) {
                    meta.addProperty("country", geo.country());
                    meta.addProperty("city",    geo.city());
                    meta.addProperty("isp",     geo.isp());
                }
                if (metadata != null) {
                    metadata.forEach((k, v) -> meta.addProperty(k, v != null ? v.toString() : ""));
                }
                body.add("metadata", meta);

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/activity/log"))
                    .header("Content-Type", "application/json")
                    .header("X-App-Token", APP_TOKEN)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[ActivityAPI] " + action + " → " + resp.statusCode());

            } catch (Exception e) {
                System.err.println("[ActivityAPI] Failed to log " + action + ": " + e.getMessage());
            }
        });
    }

    /** Convenience overload without metadata */
    public static void logAsync(int userId, String action) {
        logAsync(userId, action, null);
    }

    // ── GET: fetch activities for display ─────────────────────────────────────

    /**
     * Fetches recent activities for all users (admin dashboard).
     * Returns empty list on error.
     */
    public static CompletableFuture<List<ActivityEntry>> fetchRecentActivities(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/activity/recent?limit=" + limit))
                    .header("X-App-Token", APP_TOKEN)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[ActivityAPI] fetchRecent → HTTP " + resp.statusCode()
                    + " (" + resp.body().length() + " chars)");
                if (resp.statusCode() != 200) {
                    System.err.println("[ActivityAPI] Response body: " + resp.body());
                    return List.of();
                }
                List<ActivityEntry> result = parseEntries(resp.body());
                System.out.println("[ActivityAPI] Parsed " + result.size() + " entries");
                return result;
            } catch (Exception e) {
                System.err.println("[ActivityAPI] fetchRecent failed: " + e.getMessage()
                    + " — Is Symfony running on " + BASE_URL + " ?");
                return List.of();
            }
        }, POOL);
    }

    /**
     * Fetches activities for a specific user.
     */
    public static CompletableFuture<List<ActivityEntry>> fetchUserActivities(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/activity/user/" + userId))
                    .header("X-App-Token", APP_TOKEN)
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() != 200) return List.of();

                return parseEntries(resp.body());
            } catch (Exception e) {
                System.err.println("[ActivityAPI] fetchUser failed: " + e.getMessage());
                return List.of();
            }
        }, POOL);
    }

    // ── Direct DB fallback (when Symfony is offline) ──────────────────────────

    /**
     * Reads user_activity directly from MySQL — used when Symfony is not running.
     * Returns the same ActivityEntry format.
     */
    public static List<ActivityEntry> fetchFromDbDirect(int limit) {
        List<ActivityEntry> result = new java.util.ArrayList<>();
        var cnx = tn.esprit.tools.MyConnection.getInstance().getConnection();
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
        try (var ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, limit);
            var rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new ActivityEntry(
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
                ));
            }
            System.out.println("[ActivityAPI-DB] Loaded " + result.size() + " entries from user_activity table");
        } catch (Exception e) {
            System.err.println("[ActivityAPI-DB] " + e.getMessage());
        }
        return result;
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static List<ActivityEntry> parseEntries(String json) {
        List<ActivityEntry> result = new ArrayList<>();
        try {
            JsonArray arr = GSON.fromJson(json, JsonArray.class);
            for (var el : arr) {
                JsonObject o = el.getAsJsonObject();
                result.add(new ActivityEntry(
                    o.has("id") && !o.get("id").isJsonNull()
                        ? o.get("id").getAsInt() : 0,
                    o.has("userId") && !o.get("userId").isJsonNull()
                        ? o.get("userId").getAsInt() : 0,
                    o.has("userName") && !o.get("userName").isJsonNull()
                        ? o.get("userName").getAsString() : "—",
                    o.has("userEmail") && !o.get("userEmail").isJsonNull()
                        ? o.get("userEmail").getAsString() : "—",
                    o.has("userRole") && !o.get("userRole").isJsonNull()
                        ? o.get("userRole").getAsString() : "—",
                    o.has("action") && !o.get("action").isJsonNull()
                        ? o.get("action").getAsString() : "—",
                    o.has("success") && !o.get("success").isJsonNull()
                        ? o.get("success").getAsBoolean() : true,
                    o.has("ipAddress") && !o.get("ipAddress").isJsonNull()
                        ? o.get("ipAddress").getAsString() : "—",
                    o.has("location") && !o.get("location").isJsonNull()
                        ? o.get("location").getAsString() : "—",
                    o.has("createdAt") && !o.get("createdAt").isJsonNull()
                        ? o.get("createdAt").getAsString() : "—",
                    null
                ));
            }
        } catch (Exception e) {
            System.err.println("[ActivityAPI] Parse error: " + e.getClass().getSimpleName()
                + " — " + e.getMessage());
        }
        return result;
    }
}
