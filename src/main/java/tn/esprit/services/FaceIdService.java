package tn.esprit.services;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Face ID Service using webcam-capture (reliable on Windows) + OpenCV for detection.
 * webcam-capture uses a different driver than MSMF - no more black screen issues.
 */
public class FaceIdService {

    private static final Path STORAGE_DIR =
        Path.of(System.getProperty("user.home"), ".autolearn", "faces");

    private static final double SIMILARITY_THRESHOLD = 0.55;
    private static final int TRAINING_SAMPLES = 15;
    private static final int FACE_SIZE = 100;
    private static boolean opencvLoaded = false;
    private static CascadeClassifier cachedDetector = null;

    public record FaceResult(boolean success, String message, double confidence) {}

    // ── OpenCV init ───────────────────────────────────────────────────────────

    public static boolean initOpenCV() {
        if (opencvLoaded) return true;
        try {
            nu.pattern.OpenCV.loadLocally();
            opencvLoaded = true;
            System.out.println("[FaceID] OpenCV " + Core.VERSION + " loaded");
            return true;
        } catch (Exception e) {
            System.err.println("[FaceID] OpenCV load failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean hasFaceRegistered(int userId) {
        Path userDir = STORAGE_DIR.resolve("user_" + userId);
        if (!Files.exists(userDir)) return false;
        try { return Files.list(userDir).anyMatch(p -> p.toString().endsWith(".jpg")); }
        catch (Exception e) { return false; }
    }

    public static CascadeClassifier getDetector() {
        if (cachedDetector == null || cachedDetector.empty()) {
            cachedDetector = loadFaceDetector();
        }
        return cachedDetector;
    }

    // ── Webcam helpers ────────────────────────────────────────────────────────

    /**
     * Opens webcam using webcam-capture library (no MSMF, no black screen).
     */
    public static Webcam openWebcam() {
        try {
            Webcam webcam = Webcam.getDefault();
            if (webcam == null) return null;
            webcam.setCustomViewSizes(new java.awt.Dimension(640, 480));
            webcam.setViewSize(WebcamResolution.VGA.getSize());
            webcam.open();
            return webcam;
        } catch (Exception e) {
            System.err.println("[FaceID] Webcam open error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts BufferedImage from webcam to OpenCV Mat.
     */
    public static Mat bufferedImageToMat(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            byte[] bytes = baos.toByteArray();
            Mat mat = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
            return mat;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts webcam BufferedImage to JavaFX Image for display.
     */
    public static javafx.scene.image.Image bufferedImageToFxImage(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return new javafx.scene.image.Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (Exception e) { return null; }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    public static FaceResult registerFace(int userId,
                                          java.util.function.Consumer<Integer> onProgress) {
        if (!initOpenCV()) return new FaceResult(false, "OpenCV non disponible", 0);
        try {
            Files.createDirectories(STORAGE_DIR);
            Path userDir = STORAGE_DIR.resolve("user_" + userId);
            if (Files.exists(userDir)) {
                Files.walk(userDir).sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
            }
            Files.createDirectories(userDir);

            CascadeClassifier detector = getDetector();
            if (detector == null || detector.empty())
                return new FaceResult(false, "Detecteur de visage non disponible", 0);

            Webcam webcam = openWebcam();
            if (webcam == null)
                return new FaceResult(false, "Impossible d acces a la webcam", 0);

            int captured = 0;
            long lastCapture = 0;

            while (captured < TRAINING_SAMPLES) {
                BufferedImage img = webcam.getImage();
                if (img == null) { Thread.sleep(50); continue; }

                long now = System.currentTimeMillis();
                if (now - lastCapture < 300) { Thread.sleep(50); continue; }
                lastCapture = now;

                Mat frame = bufferedImageToMat(img);
                if (frame == null || frame.empty()) continue;

                Mat gray = toGray(frame);
                Rect[] rects = detectFaces(detector, gray);

                if (rects.length > 0) {
                    Mat faceMat = extractFace(gray, getLargestRect(rects));
                    Imgcodecs.imwrite(userDir.resolve("face_" + captured + ".jpg").toString(), faceMat);
                    captured++;
                    if (onProgress != null) onProgress.accept((captured * 100) / TRAINING_SAMPLES);
                    System.out.println("[FaceID] Captured " + captured + "/" + TRAINING_SAMPLES);
                }
            }

            webcam.close();
            return new FaceResult(true, "Visage enregistre avec succes ! (" + captured + " photos)", 1.0);

        } catch (Exception e) {
            System.err.println("[FaceID] Register error: " + e.getMessage());
            return new FaceResult(false, "Erreur: " + e.getMessage(), 0);
        }
    }

    // ── Authenticate ──────────────────────────────────────────────────────────

    public static FaceResult authenticateFace(int userId) {
        if (!initOpenCV()) return new FaceResult(false, "OpenCV non disponible", 0);
        Path userDir = STORAGE_DIR.resolve("user_" + userId);
        if (!Files.exists(userDir))
            return new FaceResult(false, "Aucun visage enregistre pour cet utilisateur", 0);

        try {
            List<Mat> storedFaces = loadStoredFaces(userDir);
            if (storedFaces.isEmpty())
                return new FaceResult(false, "Donnees corrompues. Veuillez re-enregistrer.", 0);

            CascadeClassifier detector = getDetector();
            if (detector == null || detector.empty())
                return new FaceResult(false, "Detecteur de visage non disponible", 0);

            Webcam webcam = openWebcam();
            if (webcam == null)
                return new FaceResult(false, "Impossible d acces a la webcam", 0);

            Mat liveFace = null;
            int attempts = 0;

            while (liveFace == null && attempts < 80) {
                BufferedImage img = webcam.getImage();
                if (img == null) { attempts++; Thread.sleep(50); continue; }

                Mat frame = bufferedImageToMat(img);
                if (frame == null || frame.empty()) { attempts++; continue; }

                Mat gray = toGray(frame);
                Rect[] rects = detectFaces(detector, gray);

                if (rects.length > 0) {
                    liveFace = extractFace(gray, getLargestRect(rects));
                }
                attempts++;
                Thread.sleep(50);
            }

            webcam.close();

            if (liveFace == null)
                return new FaceResult(false, "Aucun visage detecte. Regardez la camera.", 0);

            Mat liveHist = computeHistogram(liveFace);
            double bestSim = 0;
            for (Mat stored : storedFaces) {
                double sim = safeHistCorr(liveHist, computeHistogram(stored));
                if (sim > bestSim) bestSim = sim;
            }

            System.out.println("[FaceID] Similarity: " + String.format("%.3f", bestSim));

            if (bestSim >= SIMILARITY_THRESHOLD)
                return new FaceResult(true, "Identite verifiee ! (" + (int)(bestSim*100) + "%)", bestSim);
            else
                return new FaceResult(false, "Visage non reconnu (" + (int)(bestSim*100) + "%). Reessayez.", bestSim);

        } catch (Exception e) {
            System.err.println("[FaceID] Auth error: " + e.getMessage());
            return new FaceResult(false, "Erreur: " + e.getMessage(), 0);
        }
    }

    public static void deleteFaceData(int userId) {
        try {
            Path userDir = STORAGE_DIR.resolve("user_" + userId);
            if (Files.exists(userDir)) {
                Files.walk(userDir).sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
            }
        } catch (Exception e) { System.err.println("[FaceID] Delete error: " + e.getMessage()); }
    }

    // ── Feature computation ───────────────────────────────────────────────────

    private static Mat computeHistogram(Mat gray) {
        Mat hist = new Mat();
        Imgproc.calcHist(List.of(gray), new MatOfInt(0), new Mat(),
            hist, new MatOfInt(64), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    private static double safeHistCorr(Mat a, Mat b) {
        try { return Math.max(0, Imgproc.compareHist(a, b, Imgproc.CV_COMP_CORREL)); }
        catch (Exception e) { return 0; }
    }

    private static List<Mat> loadStoredFaces(Path userDir) {
        List<Mat> faces = new ArrayList<>();
        try {
            Files.list(userDir).filter(p -> p.toString().endsWith(".jpg")).forEach(p -> {
                Mat img = Imgcodecs.imread(p.toString(), Imgcodecs.IMREAD_GRAYSCALE);
                if (!img.empty()) faces.add(img);
            });
        } catch (Exception e) { System.err.println("[FaceID] Load error: " + e.getMessage()); }
        return faces;
    }

    private static Mat toGray(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);
        return gray;
    }

    private static Rect[] detectFaces(CascadeClassifier detector, Mat gray) {
        try {
            MatOfRect fr = new MatOfRect();
            detector.detectMultiScale(gray, fr, 1.05, 4, 0, new Size(60, 60), new Size());
            return fr.toArray();
        } catch (Exception e) { return new Rect[0]; }
    }

    private static Mat extractFace(Mat gray, Rect rect) {
        Mat r = new Mat();
        Imgproc.resize(new Mat(gray, rect), r, new Size(FACE_SIZE, FACE_SIZE));
        return r;
    }

    private static Rect getLargestRect(Rect[] rects) {
        Rect l = rects[0];
        for (Rect r : rects) if (r.area() > l.area()) l = r;
        return l;
    }

    private static CascadeClassifier loadFaceDetector() {
        try {
            java.io.InputStream is = FaceIdService.class.getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) return null;
            Path tmp = Files.createTempFile("haarcascade", ".xml");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return new CascadeClassifier(tmp.toString());
        } catch (Exception e) { return null; }
    }
}