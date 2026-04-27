package tn.esprit.services;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Face ID Service - uses:
 *  - sarxos webcam-capture : camera access (no Python needed)
 *  - Face++ Compare API    : cloud face comparison (no local AI needed)
 */
public class FaceIdService {

    private static final String FACEPP_KEY    = "4PZ2lLdMxDZRLMmzslnTCoHQ0_SUExmo";
    private static final String FACEPP_SECRET = "p5eE-kVNdmKt9oHC6P_zjIbgnWB8uEeh";
    private static final String FACEPP_URL    = "https://api-us.faceplusplus.com/facepp/v3/compare";
    private static final double CONFIDENCE_THRESHOLD = 70.0;

    private static final Path STORAGE_DIR = Path.of(
        System.getProperty("user.home"), ".autolearn", "faces");

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    // Single dedicated thread for ALL webcam operations
    private static final ExecutorService WEBCAM_THREAD =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "webcam-thread");
            t.setDaemon(true);
            return t;
        });

    private static Webcam sharedWebcam = null;
    private static boolean webcamWarmedUp = false;

    public record FaceResult(boolean success, String message, double confidence) {}
    public record FrameResult(javafx.scene.image.Image image, boolean faceDetected) {}

    public static boolean isServerRunning() {
        return true;
    }

    // ── Internal: open webcam and wait until it produces real frames ───────────
    // Must be called ONLY from within WEBCAM_THREAD

    private static Webcam getOrOpenWebcam() {
        try {
            if (sharedWebcam == null) {
                sharedWebcam = Webcam.getDefault();
            }
            if (sharedWebcam == null) {
                System.err.println("[FaceID] No webcam found");
                return null;
            }
            if (!sharedWebcam.isOpen()) {
                sharedWebcam.setViewSize(WebcamResolution.VGA.getSize());
                sharedWebcam.open();
                System.out.println("[FaceID] Webcam opened: " + sharedWebcam.getName());
                webcamWarmedUp = false;
            }
            // Warm up: keep reading until we get a non-null frame
            if (!webcamWarmedUp) {
                System.out.println("[FaceID] Warming up camera...");
                BufferedImage img = null;
                int attempts = 0;
                while (img == null && attempts < 40) {
                    Thread.sleep(150);
                    img = sharedWebcam.getImage();
                    attempts++;
                }
                if (img != null) {
                    webcamWarmedUp = true;
                    System.out.println("[FaceID] Camera ready after " + attempts + " attempts");
                } else {
                    System.err.println("[FaceID] Camera never produced a frame");
                    return null;
                }
            }
            return sharedWebcam;
        } catch (Exception e) {
            System.err.println("[FaceID] getOrOpenWebcam error: " + e.getMessage());
            return null;
        }
    }

    // ── Live camera preview frame ──────────────────────────────────────────────

    public static FrameResult getFrame() {
        try {
            return WEBCAM_THREAD.submit(() -> {
                Webcam webcam = getOrOpenWebcam();
                if (webcam == null) return null;
                BufferedImage img = webcam.getImage();
                if (img == null) return null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "jpg", baos);
                byte[] bytes = baos.toByteArray();
                javafx.scene.image.Image fxImage =
                    new javafx.scene.image.Image(new ByteArrayInputStream(bytes));
                return new FrameResult(fxImage, true);
            }).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Suppress repeated null errors in preview loop
            return null;
        }
    }

    // ── Check if user has a registered face ───────────────────────────────────

    public static boolean hasFaceRegistered(int userId) {
        Path ref = getReferenceImagePath(userId);
        return Files.exists(ref) && ref.toFile().length() > 0;
    }

    // ── Register: capture photo and save as reference ─────────────────────────

    public static FaceResult registerFace(int userId,
                                          java.util.function.Consumer<Integer> onProgress) {
        try {
            if (onProgress != null) onProgress.accept(10);

            BufferedImage photo = WEBCAM_THREAD.submit(() -> {
                Webcam webcam = getOrOpenWebcam();
                if (webcam == null) return null;
                if (onProgress != null) onProgress.accept(50);
                // Take a few extra frames to get a stable bright image
                BufferedImage img = null;
                for (int i = 0; i < 5; i++) {
                    img = webcam.getImage();
                    Thread.sleep(200);
                }
                return img;
            }).get(30, TimeUnit.SECONDS);

            if (photo == null) {
                return new FaceResult(false, "Impossible de capturer une photo.", 0);
            }

            Path userDir = STORAGE_DIR.resolve("user_" + userId);
            Files.createDirectories(userDir);
            Path refPath = userDir.resolve("reference.jpg");
            ImageIO.write(photo, "jpg", refPath.toFile());

            if (onProgress != null) onProgress.accept(100);
            System.out.println("[FaceID] Reference saved: " + refPath);
            return new FaceResult(true, "Visage enregistre avec succes !", 1.0);

        } catch (Exception e) {
            System.err.println("[FaceID] registerFace error: " + e.getMessage());
            return new FaceResult(false, "Erreur: " + e.getMessage(), 0);
        }
    }

    // ── Authenticate: capture live photo -> compare with Face++ ───────────────

    public static FaceResult authenticateFace(int userId) {
        Path refPath = getReferenceImagePath(userId);
        if (!Files.exists(refPath)) {
            return new FaceResult(false, "Aucun visage enregistre pour cet utilisateur.", 0);
        }

        Path livePath = null;
        try {
            BufferedImage livePhoto = WEBCAM_THREAD.submit(() -> {
                Webcam webcam = getOrOpenWebcam();
                if (webcam == null) return null;
                BufferedImage img = null;
                for (int i = 0; i < 5; i++) {
                    img = webcam.getImage();
                    Thread.sleep(200);
                }
                return img;
            }).get(30, TimeUnit.SECONDS);

            if (livePhoto == null) {
                return new FaceResult(false, "Impossible de capturer une photo.", 0);
            }

            livePath = Files.createTempFile("faceid_live_", ".jpg");
            ImageIO.write(livePhoto, "jpg", livePath.toFile());

            return compareFaces(refPath, livePath);

        } catch (Exception e) {
            System.err.println("[FaceID] authenticateFace error: " + e.getMessage());
            return new FaceResult(false, "Erreur: " + e.getMessage(), 0);
        } finally {
            if (livePath != null) try { Files.deleteIfExists(livePath); } catch (Exception ignored) {}
        }
    }

    // ── Face++ Compare API ─────────────────────────────────────────────────────

    private static FaceResult compareFaces(Path image1, Path image2) throws Exception {
        String boundary = UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        appendFormField(body, boundary, "api_key",    FACEPP_KEY);
        appendFormField(body, boundary, "api_secret", FACEPP_SECRET);
        appendFilePart(body,  boundary, "image_file1", image1);
        appendFilePart(body,  boundary, "image_file2", image2);
        body.write(("--" + boundary + "--\r\n").getBytes());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(FACEPP_URL))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();
        System.out.println("[FaceID] Face++ response: " + responseBody);

        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

        if (json.has("error_message")) {
            String err = json.get("error_message").getAsString();
            if (err.contains("NO_FACE_FOUND"))
                return new FaceResult(false,
                    "Aucun visage detecte.\nAssurez-vous d'etre bien eclaire et face a la camera.", 0);
            return new FaceResult(false, "Erreur API: " + err, 0);
        }

        if (!json.has("confidence"))
            return new FaceResult(false, "Reponse API invalide.", 0);

        double confidence = json.get("confidence").getAsDouble();
        int pct = (int) confidence;
        System.out.println("[FaceID] Confidence: " + confidence);

        if (confidence >= CONFIDENCE_THRESHOLD)
            return new FaceResult(true, "Identite verifiee ! (" + pct + "%)", confidence);
        return new FaceResult(false,
            "Visage non reconnu (" + pct + "%).\nReessayez ou re-enregistrez votre visage.", confidence);
    }

    // ── Multipart helpers ──────────────────────────────────────────────────────

    private static void appendFormField(ByteArrayOutputStream out,
                                        String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes());
        out.write((value + "\r\n").getBytes());
    }

    private static void appendFilePart(ByteArrayOutputStream out,
                                       String boundary, String fieldName, Path filePath) throws IOException {
        String fileName = filePath.getFileName().toString();
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + fieldName
                   + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
        out.write("Content-Type: image/jpeg\r\n\r\n".getBytes());
        out.write(Files.readAllBytes(filePath));
        out.write("\r\n".getBytes());
    }

    // ── Delete face data ───────────────────────────────────────────────────────

    public static void deleteFaceData(int userId) {
        try {
            Path userDir = STORAGE_DIR.resolve("user_" + userId);
            if (Files.exists(userDir))
                Files.walk(userDir).sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        } catch (Exception e) {
            System.err.println("[FaceID] deleteFaceData error: " + e.getMessage());
        }
    }

    // ── Stop camera ────────────────────────────────────────────────────────────

    public static void stopCamera() {
        WEBCAM_THREAD.submit(() -> {
            try {
                if (sharedWebcam != null && sharedWebcam.isOpen()) {
                    sharedWebcam.close();
                    sharedWebcam = null;
                    webcamWarmedUp = false;
                    System.out.println("[FaceID] Webcam closed.");
                }
            } catch (Exception ignored) {}
        });
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private static Path getReferenceImagePath(int userId) {
        return STORAGE_DIR.resolve("user_" + userId).resolve("reference.jpg");
    }
}