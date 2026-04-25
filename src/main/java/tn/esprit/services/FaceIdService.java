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
public class FaceIdService {
    private static final Path STORAGE_DIR = Path.of(System.getProperty("user.home"), ".autolearn", "faces");
    private static final double SIMILARITY_THRESHOLD = 0.75;
    private static final int TRAINING_SAMPLES = 20;
    private static final int FACE_SIZE = 100;
    private static boolean opencvLoaded = false;
    public record FaceResult(boolean success, String message, double confidence) {}
    public static boolean initOpenCV() {
        if (opencvLoaded) return true;
        try { nu.pattern.OpenCV.loadLocally(); opencvLoaded = true; return true; }
        catch (Exception e) { System.err.println("[FaceID] OpenCV load failed: " + e.getMessage()); return false; }
    }
    public static boolean hasFaceRegistered(int userId) {
        Path userDir = STORAGE_DIR.resolve("user_" + userId);
        if (!Files.exists(userDir)) return false;
        try { return Files.list(userDir).anyMatch(p -> p.toString().endsWith(".jpg")); }
        catch (Exception e) { return false; }
    }
    public static FaceResult registerFace(int userId, java.util.function.Consumer<Integer> onProgress) {
        if (!initOpenCV()) return new FaceResult(false, "OpenCV non disponible", 0);
        try {
            Files.createDirectories(STORAGE_DIR);
            Path userDir = STORAGE_DIR.resolve("user_" + userId);
            if (Files.exists(userDir)) Files.walk(userDir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
            Files.createDirectories(userDir);
            CascadeClassifier detector = loadFaceDetector();
            if (detector == null || detector.empty()) return new FaceResult(false, "Detecteur non disponible", 0);
            VideoCapture camera = new VideoCapture(0);
            if (!camera.isOpened()) return new FaceResult(false, "Webcam inaccessible", 0);
            int captured = 0; Mat frame = new Mat(); long lastCapture = 0;
            while (captured < TRAINING_SAMPLES) {
                if (!camera.read(frame) || frame.empty()) continue;
                long now = System.currentTimeMillis();
                if (now - lastCapture < 200) continue;
                lastCapture = now;
                Mat gray = toGray(frame);
                Rect[] rects = detectFaces(detector, gray);
                if (rects.length > 0) {
                    Mat faceMat = extractFace(gray, getLargestRect(rects));
                    Imgcodecs.imwrite(userDir.resolve("face_" + captured + ".jpg").toString(), faceMat);
                    captured++;
                    if (onProgress != null) onProgress.accept((captured * 100) / TRAINING_SAMPLES);
                }
            }
            camera.release();
            return new FaceResult(true, "Visage enregistre avec succes !", 1.0);
        } catch (Exception e) { return new FaceResult(false, "Erreur: " + e.getMessage(), 0); }
    }
    public static FaceResult authenticateFace(int userId) {
        if (!initOpenCV()) return new FaceResult(false, "OpenCV non disponible", 0);
        Path userDir = STORAGE_DIR.resolve("user_" + userId);
        if (!Files.exists(userDir)) return new FaceResult(false, "Aucun visage enregistre", 0);
        try {
            List<Mat> storedHists = loadStoredHistograms(userDir);
            if (storedHists.isEmpty()) return new FaceResult(false, "Donnees corrompues. Re-enregistrez.", 0);
            CascadeClassifier detector = loadFaceDetector();
            if (detector == null || detector.empty()) return new FaceResult(false, "Detecteur non disponible", 0);
            VideoCapture camera = new VideoCapture(0);
            if (!camera.isOpened()) return new FaceResult(false, "Webcam inaccessible", 0);
            Mat frame = new Mat(); int attempts = 0;
            while (attempts < 80) {
                if (!camera.read(frame) || frame.empty()) { attempts++; Thread.sleep(50); continue; }
                Mat gray = toGray(frame);
                Rect[] rects = detectFaces(detector, gray);
                if (rects.length > 0) {
                    Mat liveFace = extractFace(gray, getLargestRect(rects));
                    Mat liveHist = computeHistogram(liveFace);
                    double bestSim = 0;
                    for (Mat stored : storedHists) { double sim = Imgproc.compareHist(liveHist, stored, Imgproc.CV_COMP_CORREL); if (sim > bestSim) bestSim = sim; }
                    camera.release();
                    if (bestSim >= SIMILARITY_THRESHOLD) return new FaceResult(true, "Visage reconnu ! (" + String.format("%.0f", bestSim * 100) + "%)", bestSim);
                    else return new FaceResult(false, "Visage non reconnu (" + String.format("%.0f", bestSim * 100) + "%). Reessayez.", bestSim);
                }
                attempts++; Thread.sleep(50);
            }
            camera.release();
            return new FaceResult(false, "Aucun visage detecte. Regardez la camera.", 0);
        } catch (Exception e) { return new FaceResult(false, "Erreur: " + e.getMessage(), 0); }
    }
    public static void deleteFaceData(int userId) {
        try { Path userDir = STORAGE_DIR.resolve("user_" + userId); if (Files.exists(userDir)) Files.walk(userDir).sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} }); }
        catch (Exception e) { System.err.println("[FaceID] Delete error: " + e.getMessage()); }
    }
    private static List<Mat> loadStoredHistograms(Path userDir) {
        List<Mat> hists = new ArrayList<>();
        try { Files.list(userDir).filter(p -> p.toString().endsWith(".jpg")).forEach(p -> { Mat img = Imgcodecs.imread(p.toString(), Imgcodecs.IMREAD_GRAYSCALE); if (!img.empty()) hists.add(computeHistogram(img)); }); }
        catch (Exception e) { System.err.println("[FaceID] Load error: " + e.getMessage()); }
        return hists;
    }
    private static Mat computeHistogram(Mat gray) {
        Mat hist = new Mat();
        Imgproc.calcHist(List.of(gray), new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0f, 256f));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }
    private static Mat toGray(Mat frame) { Mat gray = new Mat(); Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY); Imgproc.equalizeHist(gray, gray); return gray; }
    private static Rect[] detectFaces(CascadeClassifier detector, Mat gray) { MatOfRect fr = new MatOfRect(); detector.detectMultiScale(gray, fr, 1.1, 5, 0, new Size(80, 80), new Size()); return fr.toArray(); }
    private static Mat extractFace(Mat gray, Rect rect) { Mat r = new Mat(); Imgproc.resize(new Mat(gray, rect), r, new Size(FACE_SIZE, FACE_SIZE)); return r; }
    private static Rect getLargestRect(Rect[] rects) { Rect l = rects[0]; for (Rect r : rects) if (r.area() > l.area()) l = r; return l; }
    private static CascadeClassifier loadFaceDetector() {
        try {
            InputStream is = FaceIdService.class.getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) is = FaceIdService.class.getResourceAsStream("/opencv/haarcascade_frontalface_default.xml");
            if (is == null) { System.err.println("[FaceID] Cascade XML not found"); return null; }
            Path tmp = Files.createTempFile("haarcascade", ".xml");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING); is.close();
            return new CascadeClassifier(tmp.toString());
        } catch (Exception e) { System.err.println("[FaceID] Cascade load failed: " + e.getMessage()); return null; }
    }
}