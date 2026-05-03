package tn.esprit.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Badge PDF format A5 portrait (148mm x 210mm).
 * Design professionnel : header violet, corps centré, QR code centré en bas.
 */
public class BadgePdfService {

    private static final BaseColor VIOLET_PRIMARY = new BaseColor(102, 126, 234);  // #667eea
    private static final BaseColor VIOLET_DARK = new BaseColor(118, 75, 162);      // #764ba2
    private static final BaseColor VIOLET_LIGHT = new BaseColor(240, 235, 255);    // #f0ebff
    private static final BaseColor BLANC  = BaseColor.WHITE;
    private static final BaseColor TEXT_DARK = new BaseColor(45, 55, 72);          // #2d3748
    private static final BaseColor TEXT_MUTED = new BaseColor(107, 114, 128);      // #6b7280

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH'h'mm", java.util.Locale.FRENCH);

    public byte[] generateBadge(String nomParticipant, String nomEquipe, String nomEvenement,
                                 String typeEvenement, LocalDateTime dateEvenement,
                                 String lieuEvenement, int participationId, byte[] qrCodeBytes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // A5 portrait : 419.5 x 595.3 pt
            Document doc = new Document(PageSize.A5, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            doc.open();

            PdfContentByte cb = writer.getDirectContent();
            float W = PageSize.A5.getWidth();   // 419.5
            float H = PageSize.A5.getHeight();  // 595.3

            // ── Fond blanc ────────────────────────────────────────────────────
            cb.saveState();
            cb.setColorFill(BLANC);
            cb.rectangle(0, 0, W, H);
            cb.fill();
            cb.restoreState();

            // ── Header violet (haut, 160pt) ───────────────────────────────────
            float headerH = 160f;
            cb.saveState();
            cb.setColorFill(VIOLET_PRIMARY);
            cb.rectangle(0, H - headerH, W, headerH);
            cb.fill();
            cb.restoreState();

            // Cercles décoratifs dans le header (couleurs solides légèrement plus claires)
            cb.saveState();
            cb.setColorFill(new BaseColor(130, 150, 240));
            cb.circle(W - 30, H - 20, 90); cb.fill();
            cb.setColorFill(new BaseColor(140, 160, 245));
            cb.circle(W + 10, H - 60, 120); cb.fill();
            cb.setColorFill(new BaseColor(125, 145, 238));
            cb.circle(30, H - 10, 60); cb.fill();
            cb.restoreState();

            // ── Barre violet foncé (séparation header/corps) ─────────────────────────
            cb.saveState();
            cb.setColorFill(VIOLET_DARK);
            cb.rectangle(0, H - headerH - 4f, W, 4f);
            cb.fill();
            cb.restoreState();

            // ── Pied de page violet ─────────────────────────────────────────────
            cb.saveState();
            cb.setColorFill(VIOLET_DARK);
            cb.rectangle(0, 0, W, 12f);
            cb.fill();
            cb.setColorFill(VIOLET_LIGHT);
            cb.rectangle(0, 12f, W, 3f);
            cb.fill();
            cb.restoreState();

            // ── Texte dans le header ──────────────────────────────────────────
            // "AutoLearn" en haut à gauche
            Font fLogo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, BLANC);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("AutoLearn", fLogo), 28f, H - 30f, 0);

            // Sous-titre plateforme
            Font fSub = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BLANC);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Plateforme d'apprentissage", fSub), 28f, H - 44f, 0);

            // Type pill (centré dans le header)
            String typeLabel = typeEvenement != null ? typeEvenement.toUpperCase() : "ÉVÉNEMENT";
            BaseColor typeColor = getTypeColor(typeEvenement);
            float pillW = 100f, pillH = 22f;
            float pillX = (W - pillW) / 2f;
            float pillY = H - headerH + (headerH - 22f) / 2f + 20f;
            cb.saveState();
            cb.setColorFill(typeColor);
            cb.roundRectangle(pillX, pillY, pillW, pillH, 11f);
            cb.fill();
            cb.restoreState();
            Font fPill = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BLANC);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(typeLabel, fPill), W / 2f, pillY + 6f, 0);

            // Icône événement (grand emoji centré)
            Font fIcon = new Font(Font.FontFamily.HELVETICA, 36, Font.NORMAL, BLANC);
            String typeIcon = getTypeIcon(typeEvenement);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(typeIcon, fIcon), W / 2f, H - 90f, 0);

            // ── Corps du badge (zone blanche) ─────────────────────────────────
            // Zone : de y=15 à y=(H-headerH-4)
            float bodyTop    = H - headerH - 4f;
            float bodyBottom = 15f;
            float centerX    = W / 2f;

            // Nom du participant (grand, centré)
            Font fName = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, VIOLET_PRIMARY);
            String nomDisplay = nomParticipant.length() > 24
                    ? nomParticipant.substring(0, 21) + "..." : nomParticipant;
            float yName = bodyTop - 28f;
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(nomDisplay, fName), centerX, yName, 0);

            // Ligne décorative sous le nom
            float lineW = Math.min(nomDisplay.length() * 7f, 180f);
            cb.saveState();
            cb.setColorFill(VIOLET_PRIMARY);
            cb.rectangle(centerX - lineW / 2f, yName - 8f, lineW, 2.5f);
            cb.fill();
            cb.restoreState();

            // Équipe
            Font fTeam = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, VIOLET_DARK);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Équipe : " + truncate(nomEquipe, 28), fTeam),
                    centerX, yName - 26f, 0);

            // Séparateur
            cb.saveState();
            cb.setColorFill(new BaseColor(230, 230, 240));
            cb.rectangle(28f, yName - 40f, W - 56f, 1f);
            cb.fill();
            cb.restoreState();

            // Événement
            Font fEvent = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(truncate(nomEvenement, 36), fEvent),
                    centerX, yName - 56f, 0);

            // Date
            if (dateEvenement != null) {
                Font fDate = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_MUTED);
                String dateStr = dateEvenement.format(DATE_FMT) + " à " + dateEvenement.format(TIME_FMT);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("📅  " + dateStr, fDate), centerX, yName - 72f, 0);
            }

            // Lieu
            if (lieuEvenement != null && !lieuEvenement.isBlank()) {
                Font fLieu = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_MUTED);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("📍  " + truncate(lieuEvenement, 34), fLieu),
                        centerX, yName - 87f, 0);
            }

            // ── QR Code centré ────────────────────────────────────────────────
            float qrSize = 110f;
            float qrX    = centerX - qrSize / 2f;
            float qrY    = bodyBottom + 40f;

            // Fond violet clair pour le QR
            cb.saveState();
            cb.setColorFill(VIOLET_LIGHT);
            cb.roundRectangle(qrX - 10f, qrY - 10f, qrSize + 20f, qrSize + 20f, 12f);
            cb.fill();
            cb.restoreState();

            if (qrCodeBytes != null && qrCodeBytes.length > 0) {
                try {
                    Image qr = Image.getInstance(qrCodeBytes);
                    qr.scaleToFit(qrSize, qrSize);
                    qr.setAbsolutePosition(qrX, qrY);
                    cb.addImage(qr);
                } catch (Exception e) {
                    System.err.println("Erreur ajout QR badge: " + e.getMessage());
                }
            }

            // Label sous le QR
            Font fQrLabel = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, VIOLET_PRIMARY);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Scanner pour vérifier", fQrLabel), centerX, qrY - 8f, 0);

            // ── Numéro de badge et statut ─────────────────────────────────────
            Font fBadge = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("BADGE #" + String.format("%05d", participationId), fBadge),
                    28f, 24f, 0);

            Font fRole = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, VIOLET_PRIMARY);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("✓ PARTICIPANT OFFICIEL", fRole),
                    W - 28f, 24f, 0);

            doc.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            System.err.println("Erreur génération badge PDF: " + e.getMessage());
            return null;
        }
    }

    private BaseColor getTypeColor(String type) {
        if (type == null) return VIOLET_PRIMARY;
        return switch (type.toLowerCase()) {
            case "hackathon"  -> new BaseColor(79, 172, 254);    // #4facfe
            case "conference" -> new BaseColor(240, 147, 251);   // #f093fb
            case "workshop"   -> new BaseColor(102, 126, 234);   // #667eea
            default           -> VIOLET_PRIMARY;
        };
    }

    private String getTypeIcon(String type) {
        if (type == null) return "🎯";
        return switch (type.toLowerCase()) {
            case "hackathon"  -> "💻";
            case "conference" -> "🎤";
            case "workshop"   -> "🛠";
            default           -> "🎯";
        };
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
