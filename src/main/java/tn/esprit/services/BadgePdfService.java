package tn.esprit.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service de génération de badges PDF professionnels pour les participants aux événements.
 * Utilise l'API iText 5 pour créer des badges au format A6 paysage (badge physique).
 */
public class BadgePdfService {

    // Palette de couleurs AutoLearn
    private static final BaseColor COLOR_PRIMARY    = new BaseColor(122, 106, 216);  // #7a6ad8 violet
    private static final BaseColor COLOR_DARK       = new BaseColor(30, 30, 30);     // #1e1e1e
    private static final BaseColor COLOR_ACCENT     = new BaseColor(5, 150, 105);    // #059669 vert
    private static final BaseColor COLOR_LIGHT_BG   = new BaseColor(245, 243, 255);  // #f5f3ff
    private static final BaseColor COLOR_WHITE      = BaseColor.WHITE;
    private static final BaseColor COLOR_GRAY       = new BaseColor(107, 114, 128);  // #6b7280
    private static final BaseColor COLOR_GOLD       = new BaseColor(245, 158, 11);   // #f59e0b

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);

    /**
     * Génère un badge PDF pour un participant.
     *
     * @param nomParticipant   Prénom + Nom du participant
     * @param nomEquipe        Nom de l'équipe
     * @param nomEvenement     Titre de l'événement
     * @param typeEvenement    Type (Hackathon, Conference, Workshop)
     * @param dateEvenement    Date de l'événement
     * @param lieuEvenement    Lieu de l'événement
     * @param participationId  ID de la participation (pour le numéro de badge)
     * @param qrCodeBytes      QR code en bytes PNG (peut être null)
     * @return bytes du PDF généré
     */
    public byte[] generateBadge(String nomParticipant, String nomEquipe, String nomEvenement,
                                  String typeEvenement, LocalDateTime dateEvenement,
                                  String lieuEvenement, int participationId, byte[] qrCodeBytes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Format A6 paysage (148mm x 105mm) — taille badge standard
            Rectangle pageSize = new Rectangle(PageSize.A6.getHeight(), PageSize.A6.getWidth());
            Document doc = new Document(pageSize, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            PdfContentByte canvas = writer.getDirectContent();
            float W = pageSize.getWidth();
            float H = pageSize.getHeight();

            // ── Fond principal blanc ──────────────────────────────────────────
            canvas.setColorFill(COLOR_WHITE);
            canvas.rectangle(0, 0, W, H);
            canvas.fill();

            // ── Bande supérieure violette ─────────────────────────────────────
            canvas.setColorFill(COLOR_PRIMARY);
            canvas.rectangle(0, H - 52, W, 52);
            canvas.fill();

            // ── Accent vert en bas ────────────────────────────────────────────
            canvas.setColorFill(COLOR_ACCENT);
            canvas.rectangle(0, 0, W, 8);
            canvas.fill();

            // ── Ligne décorative dorée ────────────────────────────────────────
            canvas.setColorFill(COLOR_GOLD);
            canvas.rectangle(0, 8, W, 3);
            canvas.fill();

            // ── Cercle décoratif en haut à droite ────────────────────────────
            canvas.setColorFill(new BaseColor(255, 255, 255, 30));
            canvas.circle(W - 20, H - 10, 55);
            canvas.fill();
            canvas.setColorFill(new BaseColor(255, 255, 255, 15));
            canvas.circle(W - 10, H - 5, 80);
            canvas.fill();

            // ── Logo / Titre plateforme ───────────────────────────────────────
            Font fontLogo = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, COLOR_WHITE);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("🎓 AutoLearn", fontLogo), 18, H - 32, 0);

            // ── Badge type pill ───────────────────────────────────────────────
            String typeLabel = typeEvenement != null ? typeEvenement.toUpperCase() : "ÉVÉNEMENT";
            float pillW = 80, pillH = 18;
            float pillX = W - pillW - 14;
            float pillY = H - 38;
            canvas.setColorFill(COLOR_GOLD);
            canvas.roundRectangle(pillX, pillY, pillW, pillH, 9);
            canvas.fill();
            Font fontPill = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, COLOR_DARK);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                    new Phrase(typeLabel, fontPill), pillX + pillW / 2, pillY + 5, 0);

            // ── Nom du participant ────────────────────────────────────────────
            Font fontName = new Font(Font.FontFamily.HELVETICA, 17, Font.BOLD, COLOR_DARK);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(nomParticipant, fontName), 18, H - 78, 0);

            // ── Ligne séparatrice ─────────────────────────────────────────────
            canvas.setColorFill(COLOR_PRIMARY);
            canvas.rectangle(18, H - 84, 40, 2);
            canvas.fill();

            // ── Nom de l'équipe ───────────────────────────────────────────────
            Font fontTeam = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_PRIMARY);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("👥  " + nomEquipe, fontTeam), 18, H - 100, 0);

            // ── Nom de l'événement ────────────────────────────────────────────
            Font fontEvent = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, COLOR_GRAY);
            String eventDisplay = nomEvenement.length() > 38 ? nomEvenement.substring(0, 35) + "..." : nomEvenement;
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("📅  " + eventDisplay, fontEvent), 18, H - 116, 0);

            // ── Date et lieu ──────────────────────────────────────────────────
            Font fontMeta = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, COLOR_GRAY);
            if (dateEvenement != null) {
                ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                        new Phrase("🗓  " + dateEvenement.format(DATE_FMT), fontMeta), 18, H - 130, 0);
            }
            if (lieuEvenement != null && !lieuEvenement.isBlank()) {
                String lieuDisplay = lieuEvenement.length() > 30 ? lieuEvenement.substring(0, 27) + "..." : lieuEvenement;
                ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                        new Phrase("📍  " + lieuDisplay, fontMeta), 18, H - 142, 0);
            }

            // ── QR Code ───────────────────────────────────────────────────────
            if (qrCodeBytes != null && qrCodeBytes.length > 0) {
                try {
                    Image qr = Image.getInstance(qrCodeBytes);
                    float qrSize = 62;
                    qr.scaleToFit(qrSize, qrSize);
                    qr.setAbsolutePosition(W - qrSize - 14, 22);
                    doc.add(qr);
                } catch (Exception ignored) {}
            }

            // ── Numéro de badge ───────────────────────────────────────────────
            Font fontBadgeNum = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, COLOR_GRAY);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("BADGE #" + String.format("%05d", participationId), fontBadgeNum),
                    18, 18, 0);

            // ── Mention PARTICIPANT ───────────────────────────────────────────
            Font fontRole = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, COLOR_ACCENT);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("✓  PARTICIPANT OFFICIEL", fontRole), 18, 30, 0);

            doc.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            System.err.println("Erreur génération badge PDF: " + e.getMessage());
            return null;
        }
    }
}
