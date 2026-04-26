package tn.esprit.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de génération de PDF pour les plannings d'événements.
 * Respecte la palette violet de la plateforme.
 */
public class PlanningPdfService {

    private static final BaseColor VIOLET_PRIMARY = new BaseColor(102, 126, 234);   // #667eea
    private static final BaseColor VIOLET_DARK = new BaseColor(118, 75, 162);       // #764ba2
    private static final BaseColor VIOLET_LIGHT = new BaseColor(240, 235, 255);     // #f0ebff
    private static final BaseColor TEXT_DARK = new BaseColor(45, 55, 72);           // #2d3748
    private static final BaseColor TEXT_BODY = new BaseColor(74, 85, 104);          // #4a5568
    private static final BaseColor BORDER_LIGHT = new BaseColor(232, 224, 255);     // #e8e0ff
    private static final BaseColor WHITE = BaseColor.WHITE;

    public byte[] generatePlanningPdf(String eventTitle, String eventType,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      String planningJson) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            addHeader(doc, eventTitle, eventType, startTime, endTime);
            addDivider(doc);
            addPlanningTable(doc, planningJson);
            addAnimatorsSection(doc, planningJson);
            addFooter(doc);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("Erreur génération PDF planning: " + e.getMessage());
            return null;
        }
    }

    private void addHeader(Document doc, String title, String type,
                          LocalDateTime start, LocalDateTime end) throws DocumentException {
        // Platform name
        Paragraph platform = new Paragraph("AutoLearn", 
            FontFactory.getFont(FontFactory.HELVETICA, 11, Font.BOLD, VIOLET_PRIMARY));
        platform.setAlignment(Element.ALIGN_CENTER);
        platform.setSpacingAfter(2);
        doc.add(platform);

        // Main title
        Paragraph header = new Paragraph("PLANNING D'ÉVÉNEMENT", 
            FontFactory.getFont(FontFactory.HELVETICA, 22, Font.BOLD, VIOLET_PRIMARY));
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(4);
        doc.add(header);

        // Event details
        Paragraph details = new Paragraph(title + " — " + type, 
            FontFactory.getFont(FontFactory.HELVETICA, 13, Font.NORMAL, VIOLET_DARK));
        details.setAlignment(Element.ALIGN_CENTER);
        details.setSpacingAfter(2);
        doc.add(details);

        // Date/Time
        String dateStr = start.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", 
            java.util.Locale.FRENCH));
        String timeStr = start.format(DateTimeFormatter.ofPattern("HH:mm")) + " - " +
                        end.format(DateTimeFormatter.ofPattern("HH:mm"));
        Paragraph datetime = new Paragraph("📅 " + dateStr + " | ⏰ " + timeStr, 
            FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL, TEXT_BODY));
        datetime.setAlignment(Element.ALIGN_CENTER);
        datetime.setSpacingAfter(14);
        doc.add(datetime);
    }

    private void addDivider(Document doc) throws DocumentException {
        LineSeparator ls = new LineSeparator(1.5f, 100, VIOLET_PRIMARY, Element.ALIGN_CENTER, 0);
        doc.add(new Chunk(ls));
        doc.add(new Paragraph("\n"));
    }

    private void addPlanningTable(Document doc, String planningJson) throws DocumentException {
        doc.add(new Paragraph("📋 Planning Détaillé", 
            FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, VIOLET_PRIMARY)));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.2f, 2.5f, 1.5f, 1.5f});
        table.setSpacingAfter(16);

        // Headers
        String[] headers = {"Heure", "Fin", "Activité", "Lieu", "Animateurs"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, 
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, WHITE)));
            cell.setBackgroundColor(VIOLET_PRIMARY);
            cell.setPadding(10);
            cell.setBorderColor(VIOLET_PRIMARY);
            table.addCell(cell);
        }

        // Parse and add rows
        List<Map<String, String>> activities = parsePlanningActivities(planningJson);
        for (int i = 0; i < activities.size(); i++) {
            Map<String, String> activity = activities.get(i);
            
            BaseColor bgColor = i % 2 == 0 ? VIOLET_LIGHT : WHITE;
            
            addTableCell(table, activity.get("heure_debut"), bgColor);
            addTableCell(table, activity.get("heure_fin"), bgColor);
            addTableCell(table, activity.get("activite"), bgColor);
            addTableCell(table, activity.get("lieu"), bgColor);
            addTableCell(table, activity.get("animateurs"), bgColor);
        }

        doc.add(table);
    }

    private void addTableCell(PdfPTable table, String text, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", 
            FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, TEXT_BODY)));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_LIGHT);
        table.addCell(cell);
    }

    private void addAnimatorsSection(Document doc, String planningJson) throws DocumentException {
        doc.add(new Paragraph("\n"));
        doc.add(new Paragraph("👥 Équipe d'Animation", 
            FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, VIOLET_PRIMARY)));
        doc.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 1.5f, 1.5f, 1.5f});
        table.setSpacingAfter(16);

        // Headers
        String[] headers = {"Nom", "Rôle", "Spécialité", "Statut"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, 
                FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, WHITE)));
            cell.setBackgroundColor(VIOLET_DARK);
            cell.setPadding(10);
            cell.setBorderColor(VIOLET_DARK);
            table.addCell(cell);
        }

        // Parse and add animators
        List<Map<String, String>> animators = parseAnimators(planningJson);
        for (int i = 0; i < animators.size(); i++) {
            Map<String, String> animator = animators.get(i);
            
            BaseColor bgColor = i % 2 == 0 ? VIOLET_LIGHT : WHITE;
            
            addTableCell(table, animator.get("nom"), bgColor);
            addTableCell(table, animator.get("role"), bgColor);
            addTableCell(table, animator.get("specialite"), bgColor);
            addTableCell(table, animator.get("statut"), bgColor);
        }

        doc.add(table);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(new Paragraph("\n"));
        doc.add(new Chunk(new LineSeparator(0.5f, 100, BORDER_LIGHT, Element.ALIGN_CENTER, 0)));
        
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        footer.add(new Chunk("Planning généré automatiquement par AutoLearn\n",
            FontFactory.getFont(FontFactory.HELVETICA, 9, Font.ITALIC, TEXT_BODY)));
        footer.add(new Chunk("© 2026 AutoLearn — Tous droits réservés",
            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, TEXT_BODY)));
        doc.add(footer);
    }

    private java.util.List<Map<String, String>> parsePlanningActivities(String json) {
        java.util.List<Map<String, String>> activities = new ArrayList<>();
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("planning")) return activities;
            for (com.google.gson.JsonElement el : obj.getAsJsonArray("planning")) {
                com.google.gson.JsonObject slot = el.getAsJsonObject();
                Map<String, String> activity = new HashMap<>();
                activity.put("heure_debut", getString(slot, "heure_debut"));
                activity.put("heure_fin",   getString(slot, "heure_fin"));
                activity.put("activite",    getString(slot, "activite"));
                activity.put("lieu",        getString(slot, "lieu"));
                // animateurs field inside a slot is an array of strings
                if (slot.has("animateurs") && slot.get("animateurs").isJsonArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (com.google.gson.JsonElement a : slot.getAsJsonArray("animateurs")) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(a.getAsString());
                    }
                    activity.put("animateurs", sb.length() > 0 ? sb.toString() : "—");
                } else {
                    activity.put("animateurs", "—");
                }
                activities.add(activity);
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing planning activities: " + e.getMessage());
        }
        return activities;
    }

    private java.util.List<Map<String, String>> parseAnimators(String json) {
        java.util.List<Map<String, String>> animators = new ArrayList<>();
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!obj.has("animateurs")) return animators;
            for (com.google.gson.JsonElement el : obj.getAsJsonArray("animateurs")) {
                com.google.gson.JsonObject anim = el.getAsJsonObject();
                Map<String, String> animator = new HashMap<>();
                animator.put("nom",        getString(anim, "nom"));
                animator.put("role",       getString(anim, "role"));
                animator.put("specialite", getString(anim, "specialite"));
                animator.put("statut",     getString(anim, "statut"));
                animators.add(animator);
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing animators: " + e.getMessage());
        }
        return animators;
    }

    private String getString(com.google.gson.JsonObject obj, String key) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception ignored) {}
        return "—";
    }
}
