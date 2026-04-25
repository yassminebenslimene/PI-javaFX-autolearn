package tn.esprit.services;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Face ID Service - Professional grade face recognition.
 * Multi-method fusion: Histogram + LBP + Pixel correlation + Edge features.
 * Crash-safe: all native OpenCV calls wrapped in try-catch.
 */
public class FaceIdService {

    private static final Path STORAGE_DIR =
        Path.of(System.getProperty("user.home"), ".autolearn", "faces");

    private static final double AUTH_THRESHOLD = 0.65;
    private static final int TRAINING_SAMPLES  = 30;
    private static final int FACE_SIZE         = 100;
    private static final int AUTH_ATTEMPTS     = 3;

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
        try {
            return Files.list(userDir).anyMatch(p -> p.toString().endsWith(".jpg"));
        } catch (Exception e) { return false; }
    }

    public static CascadeClassifier getDetector() {
        if (cachedDetector == null || cachedDetector.empty()) {
            cachedDetector = loadFaceDetector();
        }
        return cachedDetector;
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

            VideoCapture camera = new VideoCapture(0);
            if (!camera.isOpened())
                return new FaceResult(false, "Impossible d acces a la webcam", 0);

            camera.set(3, 640);
            camera.set(4, 480);

            int captured = 0;
            Mat frame = new Mat();
            long lastCapture = 0;

            while (captured < TRAINING_SAMPLES) {
                if (!camera.read(frame) || frame.empty()) continue;
                long now = System.currentTimeMillis();
                if (now - lastCapture < 250) continue;
                lastCapture = now;

                Mat gray = safeToGray(frame);
                Rect[] rects = detectFaces(detector, gray);

                if (rects.length > 0) {
                    Mat faceMat = extractFace(gray, getLargestRect(rects));
                    String path = userDir.resolve("face_" + captured + ".jpg").toString();
                    Imgcodecs.imwrite(path, faceMat);
                    captured++;
                    if (onProgress != null) onProgress.accept((captured * 100) / TRAINING_SAMPLES);
                    System.out.println("[FaceID] Captured " + captured + "/" + TRAINING_SAMPLES);
                }
            }

            camera.release();
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

            VideoCapture camera = new VideoCapture(0);
            if (!camera.isOpened())
                return new FaceResult(false, "Impossible d acces a la webcam", 0);

            camera.set(3, 640);
            camera.set(4, 480);

            Mat frame = new Mat();
            int attempts = 0;
            List<Double> scores = new ArrayList<>();

            while (attempts < 100 && scores.size() < AUTH_ATTEMPTS) {
                if (!camera.read(frame) || frame.empty()) {
                    attempts++;
                    Thread.sleep(30);
                    continue;
                }

                Mat gray = safeToGray(frame);
                Rect[] rects = detectFaces(detector, gray);

                if (rects.length > 0) {
                    Mat liveFace = extractFace(gray, getLargestRect(rects));
                    double bestScore = 0;
                    for (Mat stored : storedFaces) {
                        double score = computeFusionScore(liveFace, stored);
                        if (score > bestScore) bestScore = score;
                    }
                    scores.add(bestScore);
                    System.out.println("[FaceID] Frame score: " + String.format("%.3f", bestScore));
                }

                attempts++;
                Thread.sleep(30);
            }

            camera.release();

            if (scores.isEmpty())
                return new FaceResult(false, "Aucun visage detecte. Regardez la camera.", 0);

            double finalScore = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            System.out.println("[FaceID] Final: " + String.format("%.3f", finalScore));

            if (finalScore >= AUTH_THRESHOLD) {
                return new FaceResult(true,
                    "Identite verifiee ! (" + (int)(finalScore * 100) + "% de correspondance)", finalScore);
            } else {
                return new FaceResult(false,
                    "Visage non reconnu (" + (int)(finalScore * 100) + "%). Reessayez.", finalScore);
            }

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

    // ── Fusion score ──────────────────────────────────────────────────────────

    private static double computeFusionScore(Mat live, Mat stored) {
        double histScore  = safeHistCorr(live, stored);
        double lbpScore   = safeLBPCorr(live, stored);
        double edgeScore  = safeEdgeCorr(live, stored);

        // Weighted fusion
        double fusion = 0.30 * histScore + 0.45 * lbpScore + 0.25 * edgeScore;

        System.out.println("[FaceID] hist=" + String.format("%.2f", histScore)
            + " lbp=" + String.format("%.2f", lbpScore)
            + " edge=" + String.format("%.2f", edgeScore)
            + " => " + String.format("%.3f", fusion));

        return Math.max(0, Math.min(1, fusion));
    }

    // ── Safe feature methods ──────────────────────────────────────────────────

    private static double safeHistCorr(Mat a, Mat b) {
        try {
            Mat ha = computeHistogram(a);
            Mat hb = computeHistogram(b);
            double v = Imgproc.compareHist(ha, hb, Imgproc.CV_COMP_CORREL);
            return Math.max(0, v);
        } catch (Exception e) { return 0; }
    }

    private static double safeLBPCorr(Mat a, Mat b) {
        try {
            Mat la = computeLBP(a);
            Mat lb = computeLBP(b);
            double v = Imgproc.compareHist(la, lb, Imgproc.CV_COMP_CORREL);
            return Math.max(0, v);
        } catch (Exception e) { return 0; }
    }

    private static double safeEdgeCorr(Mat a, Mat b) {
        try {
            Mat ea = computeEdgeHist(a);
            Mat eb = computeEdgeHist(b);
            double v = Imgproc.compareHist(ea, eb, Imgproc.CV_COMP_CORREL);
            return Math.max(0, v);
        } catch (Exception e) { return 0; }
    }

    // ── Feature computation ───────────────────────────────────────────────────

    private static Mat computeHistogram(Mat gray) {
        Mat hist = new Mat();
        Imgproc.calcHist(List.of(gray), new MatOfInt(0), new Mat(),
            hist, new MatOfInt(64), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    private static Mat computeLBP(Mat gray) {
        Mat lbp = new Mat(gray.size(), CvType.CV_8UC1, Scalar.all(0));
        int rows = gray.rows();
        int cols = gray.cols();
        int[][] nb = {{-1,-1},{-1,0},{-1,1},{0,1},{1,1},{1,0},{1,-1},{0,-1}};
        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {
                double center = gray.get(r, c)[0];
                int code = 0;
                for (int i = 0; i < 8; i++) {
                    if (gray.get(r + nb[i][0], c + nb[i][1])[0] >= center)
                        code |= (1 << i);
                }
                lbp.put(r, c, code);
            }
        }
        Mat hist = new Mat();
        Imgproc.calcHist(List.of(lbp), new MatOfInt(0), new Mat(),
            hist, new MatOfInt(256), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    private static Mat computeEdgeHist(Mat gray) {
        Mat edges = new Mat();
        Imgproc.Canny(gray, edges, 50, 150);
        Mat hist = new Mat();
        Imgproc.calcHist(List.of(edges), new MatOfInt(0), new Mat(),
            hist, new MatOfInt(32), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }

    // ── Preprocessing ─────────────────────────────────────────────────────────

    private static Mat safeToGray(Mat frame) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            // Try CLAHE for better contrast
            try {
                Mat result = new Mat();
                org.opencv.imgproc.CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
                clahe.apply(gray, result);
                return result;
            } catch (Exception e) {
                // Fallback to simple equalization
                Imgproc.equalizeHist(gray, gray);
                return gray;
            }
        } catch (Exception e) {
            return frame;
        }
    }

    private static Mat extractFace(Mat gray, Rect rect) {
        try {
            int margin = (int)(rect.width * 0.08);
            int x = Math.max(0, rect.x - margin);
            int y = Math.max(0, rect.y - margin);
            int w = Math.min(gray.cols() - x, rect.width + 2 * margin);
            int h = Math.min(gray.rows() - y, rect.height + 2 * margin);
            Mat face = new Mat(gray, new Rect(x, y, w, h));
            Mat resized = new Mat();
            Imgproc.resize(face, resized, new Size(FACE_SIZE, FACE_SIZE));
            return resized;
        } catch (Exception e) {
            Mat resized = new Mat();
            Imgproc.resize(new Mat(gray, rect), resized, new Size(FACE_SIZE, FACE_SIZE));
            return resized;
        }
    }

    // ── Load stored faces ─────────────────────────────────────────────────────

    private static List<Mat> loadStoredFaces(Path userDir) {
        List<Mat> faces = new ArrayList<>();
        try {
            Files.list(userDir)
                .filter(p -> p.toString().endsWith(".jpg"))
                .forEach(p -> {
                    Mat img = Imgcodecs.imread(p.toString(), Imgcodecs.IMREAD_GRAYSCALE);
                    if (!img.empty()) faces.add(img);
                });
        } catch (Exception e) {
            System.err.println("[FaceID] Load error: " + e.getMessage());
        }
        return faces;
    }

    // ── OpenCV helpers ────────────────────────────────────────────────────────

    private static Rect[] detectFaces(CascadeClassifier detector, Mat gray) {
        try {
            MatOfRect faceRects = new MatOfRect();
            detector.detectMultiScale(gray, faceRects, 1.05, 4, 0,
                new Size(60, 60), new Size(400, 400));
            return faceRects.toArray();
        } catch (Exception e) {
            return new Rect[0];
        }
    }

    private static Rect getLargestRect(Rect[] rects) {
        Rect largest = rects[0];
        for (Rect r : rects) if (r.area() > largest.area()) largest = r;
        return largest;
    }

    private static CascadeClassifier loadFaceDetector() {
        try {
            InputStream is = FaceIdService.class.getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) is = FaceIdService.class.getResourceAsStream("/opencv/haarcascade_frontalface_default.xml");
            if (is == null) { System.err.println("[FaceID] Cascade XML not found"); return null; }
            Path tmp = Files.createTempFile("haarcascade", ".xml");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            is.close();
            return new CascadeClassifier(tmp.toString());
        } catch (Exception e) {
            System.err.println("[FaceID] Cascade load failed: " + e.getMessage());
            return null;
        }
    }
}