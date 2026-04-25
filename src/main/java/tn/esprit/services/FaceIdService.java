package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class FaceIdService {

    private static final String SERVER_URL = "http://127.0.0.1:5001";
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public record FaceResult(boolean success, String message, double confidence) {}

    public static boolean initOpenCV() { return isServerRunning(); }

    public static boolean isServerRunning() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/status"))
                .timeout(Duration.ofSeconds(2)).GET().build();
            return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) { return false; }
    }

    public static boolean hasFaceRegistered(int userId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/has_face?userId=" + userId))
                .timeout(Duration.ofSeconds(3)).GET().build();
            JsonObject json = GSON.fromJson(HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body(), JsonObject.class);
            return json.has("has_face") && json.get("has_face").getAsBoolean();
        } catch (Exception e) {
            Path d = Path.of(System.getProperty("user.home"), ".autolearn", "faces", "user_" + userId);
            try { return Files.exists(d) && Files.list(d).anyMatch(p -> p.toString().endsWith(".jpg")); }
            catch (Exception ex) { return false; }
        }
    }

    public static FaceResult registerFace(int userId, java.util.function.Consumer<Integer> onProgress) {
        if (!isServerRunning()) return new FaceResult(false, "Serveur Face ID non demarre. Lancez: python faceid_server.py", 0);
        try {
            if (onProgress != null) {
                Thread t = new Thread(() -> {
                    for (int i = 10; i <= 90; i += 10) {
                        try { Thread.sleep(1500); } catch (Exception ignored) {}
                        final int p = i; onProgress.accept(p);
                    }
                });
                t.setDaemon(true); t.start();
            }
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/register?userId=" + userId))
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
            if (onProgress != null) onProgress.accept(100);
            boolean ok = json.has("success") && json.get("success").getAsBoolean();
            String msg = json.has("message") ? json.get("message").getAsString() : "Erreur";
            return new FaceResult(ok, msg, ok ? 1.0 : 0);
        } catch (Exception e) { return new FaceResult(false, "Erreur: " + e.getMessage(), 0); }
    }

    public static FaceResult authenticateFace(int userId) {
        if (!isServerRunning()) return new FaceResult(false, "Serveur Face ID non demarre. Lancez: python faceid_server.py", 0);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/authenticate?userId=" + userId))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
            boolean ok = json.has("success") && json.get("success").getAsBoolean();
            String msg = json.has("message") ? json.get("message").getAsString() : "Erreur";
            double conf = json.has("confidence") ? json.get("confidence").getAsDouble() : 0;
            return new FaceResult(ok, msg, conf);
        } catch (Exception e) { return new FaceResult(false, "Erreur: " + e.getMessage(), 0); }
    }

    public static void deleteFaceData(int userId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(SERVER_URL + "/delete?userId=" + userId))
                .timeout(Duration.ofSeconds(5)).DELETE().build();
            HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) { System.err.println("[FaceID] Delete error: " + e.getMessage()); }
    }

    public static org.opencv.objdetect.CascadeClassifier getDetector() { return null; }
}