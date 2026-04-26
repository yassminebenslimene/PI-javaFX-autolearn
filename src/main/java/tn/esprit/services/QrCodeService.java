package tn.esprit.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Service de génération de QR codes pour les participations aux événements.
 * Utilise l'API ZXing (Google) pour générer des QR codes haute qualité.
 */
public class QrCodeService {

    /**
     * Génère un QR code pointant vers la page de détails de participation.
     * L'URL pointe vers le mini serveur HTTP embarqué de l'application (port 8765).
     * @param participationId  ID de la participation
     * @param etudiantId       ID de l'étudiant
     * @param evenementId      ID de l'événement
     * @return bytes PNG du QR code, ou null en cas d'erreur
     */
    public byte[] generateParticipationQrCode(int participationId, int etudiantId, int evenementId) {
        String url = "http://localhost:8765/participation/" + participationId + "?eid=" + evenementId + "&uid=" + etudiantId;
        return generateQrCode(url, 300);
    }

    /**
     * Génère un QR code à partir d'un contenu texte/URL.
     * @param content  contenu à encoder
     * @param size     taille en pixels (carré)
     * @return bytes PNG du QR code
     */
    public byte[] generateQrCode(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | java.io.IOException e) {
            System.err.println("Erreur génération QR code: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retourne l'URL de la page de participation (celle encodée dans le QR code).
     */
    public String getParticipationUrl(int participationId, int etudiantId, int evenementId) {
        return "http://localhost:8765/participation/" + participationId + "?eid=" + evenementId + "&uid=" + etudiantId;
    }
}
