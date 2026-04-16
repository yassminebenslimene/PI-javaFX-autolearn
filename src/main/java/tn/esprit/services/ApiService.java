package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * External API integrations for GestionUser.
 *
 * APIs used (all free, no API key required except HaveIBeenPwned):
 *  1. Gravatar       — user avatar from email hash
 *  2. HaveIBeenPwned — check if a password was leaked (k-anonymity model)
 *  3. ip-api.com     — IP geolocation for login audit
 *  4. Webhook        — Slack/Discord admin alerts
 */
public class ApiService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final Gson GSON = new Gson();

    // ── 1. GRAVATAR ───────────────────────────────────────────────────────────

    /**
     * Returns the Gravatar URL for a given email.
     * If the user has no Gravatar, falls back to a generated identicon.
     *
     * @param email user email
     * @param size  pixel size (e.g. 80)
     * @return full Gravatar URL
     */
    public static String getGravatarUrl(String email, int size) {
        String hash = md5(email.trim().toLowerCase());
        return "https://www.gravatar.com/avatar/" + hash
            + "?s=" + size + "&d=identicon&r=pg";
    }

    /**
     * Asynchronously loads the Gravatar image bytes for a given email.
     * Returns null if the request fails.
     */
    public static CompletableFuture<byte[]> fetchGravatarBytes(String email, int size) {
        String url = getGravatarUrl(email, size);
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(4))
            .GET()
            .build();
        return HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
            .thenApply(resp -> resp.statusCode() == 200 ? resp.body() : null)
            .exceptionally(e -> {
                System.err.println("[Gravatar] Failed: " + e.getMessage());
                return null;
            });
    }

    // ── 2. HAVE I BEEN PWNED (k-anonymity) ───────────────────────────────────

    /**
     * Checks if a plain-text password appears in known data breaches.
     * Uses the k-anonymity model: only the first 5 chars of the SHA-1 hash
     * are sent to the API — the full password never leaves the device.
     *
     * @param plainPassword the password to check
     * @return number of times it appeared in breaches (0 = safe)
     */
    public static int checkPasswordBreached(String plainPassword) {
        try {
            String sha1 = sha1(plainPassword).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                .header("Add-Padding", "true")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return 0;

            for (String line : resp.body().split("\r?\n")) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].equalsIgnoreCase(suffix)) {
                    return Integer.parseInt(parts[1].trim());
                }
            }
        } catch (Exception e) {
            System.err.println("[HIBP] Check failed: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Async version — returns a CompletableFuture<Integer> breach count.
     */
    public static CompletableFuture<Integer> checkPasswordBreachedAsync(String plainPassword) {
        return CompletableFuture.supplyAsync(() -> checkPasswordBreached(plainPassword));
    }

    // ── 3. IP GEOLOCATION ─────────────────────────────────────────────────────

    /**
     * Fetches geolocation info for the current machine's public IP.
     * Used to log the country/city on login for security audit.
     *
     * @return GeoInfo record, or null if the request fails
     */
    public static GeoInfo getMyGeoInfo() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://ip-api.com/json/?fields=status,country,city,isp,query"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;

            JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
            if (!"success".equals(json.get("status").getAsString())) return null;

            return new GeoInfo(
                json.get("query").getAsString(),
                json.get("country").getAsString(),
                json.get("city").getAsString(),
                json.get("isp").getAsString()
            );
        } catch (Exception e) {
            System.err.println("[GeoIP] Failed: " + e.getMessage());
            return null;
        }
    }

    public record GeoInfo(String ip, String country, String city, String isp) {
        @Override public String toString() {
            return city + ", " + country + " (" + ip + ")";
        }
    }

    // ── 4. WEBHOOK ADMIN ALERT ────────────────────────────────────────────────

    /**
     * Sends a JSON payload to a webhook URL (Slack/Discord/custom).
     * Used to alert admins of security events: new registrations,
     * suspicious logins, auto-suspensions, etc.
     *
     * Set WEBHOOK_URL to your Slack/Discord incoming webhook URL.
     * Leave empty to disable.
     */
    private static final String WEBHOOK_URL = ""; // e.g. https://hooks.slack.com/services/xxx

    public static void sendAdminAlert(String title, String message) {
        if (WEBHOOK_URL == null || WEBHOOK_URL.isBlank()) return;

        // Works for both Slack and Discord (both accept {"text": "..."})
        String body = GSON.toJson(java.util.Map.of(
            "text", "*[AutoLearn]* " + title + "\n" + message
        ));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(WEBHOOK_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(5))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding())
            .thenAccept(resp -> System.out.println("[Webhook] Status: " + resp.statusCode()))
            .exceptionally(e -> { System.err.println("[Webhook] Failed: " + e.getMessage()); return null; });
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    private static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }
}
