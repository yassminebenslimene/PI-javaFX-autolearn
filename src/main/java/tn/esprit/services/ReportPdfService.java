package tn.esprit.services;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportPdfService {

    // Platform palette: violet (AutoLearn identity)
    private static final BaseColor PRIMARY   = new BaseColor(102, 126, 234);   // #667eea violet
    private static final BaseColor SECONDARY = new BaseColor(118, 75, 162);    // #764ba2 violet dark
    private static final BaseColor ACCENT    = new BaseColor(102, 126, 234);   // #667eea violet
    private static final BaseColor BG_LIGHT  = new BaseColor(240, 235, 255);   // #f0ebff violet light
    private static final BaseColor BG_CARD   = new BaseColor(245, 243, 255);   // #f5f3ff very light
    private static final BaseColor TEXT_DARK = new BaseColor(45, 55, 72);      // #2d3748 dark
    private static final BaseColor TEXT_BODY = new BaseColor(74, 85, 104);     // #4a5568 body
    private static final BaseColor TEXT_MUTED= new BaseColor(107, 114, 128);   // #6b7280 muted
    private static final BaseColor BORDER    = new BaseColor(232, 224, 255);   // #e8e0ff light violet
    private static final BaseColor GREEN_BG  = new BaseColor(212, 232, 212);
    private static final BaseColor GREEN_FG  = new BaseColor(45, 90, 45);
    private static final BaseColor RED_BG    = new BaseColor(240, 212, 212);
    private static final BaseColor RED_FG    = new BaseColor(107, 45, 45);
    private static final BaseColor YELLOW_BG = new BaseColor(240, 235, 255);
    private static final BaseColor YELLOW_FG = new BaseColor(102, 126, 234);

    private static Font font(int size, int style, BaseColor color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    public byte[] generateReportPdf(String title, String content, String reportType) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 45, 45, 50, 45);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        addHeader(doc, title, reportType);
        addMetadata(doc);
        addDivider(doc);
        addStructuredContent(doc, content);
        addFooter(doc);

        doc.close();
        return baos.toByteArray();
    }

    private void addHeader(Document doc, String title, String reportType) throws DocumentException {
        // Platform name
        Paragraph platform = new Paragraph("AutoLearn", font(11, Font.BOLD, ACCENT));
        platform.setAlignment(Element.ALIGN_CENTER);
        platform.setSpacingAfter(2);
        doc.add(platform);

        // Main title
        Paragraph header = new Paragraph("RAPPORT IA — ÉVÉNEMENTS", font(22, Font.BOLD, PRIMARY));
        header.setAlignment(Element.ALIGN_CENTER);
        header.setSpacingAfter(4);
        doc.add(header);

        // Subtitle
        Paragraph sub = new Paragraph(title + " — " + reportType, font(13, Font.NORMAL, SECONDARY));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(14);
        doc.add(sub);
    }

    private void addDivider(Document doc) throws DocumentException {
        LineSeparator ls = new LineSeparator(1.5f, 100, PRIMARY, Element.ALIGN_CENTER, 0);
        doc.add(new Chunk(ls));
        doc.add(new Paragraph("\n"));
    }

    private void addMetadata(Document doc) throws DocumentException {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{1, 3});
        meta.setSpacingAfter(10);

        addMetaRow(meta, "Date", date);
        addMetaRow(meta, "Plateforme", "AutoLearn — Gestion d'Événements");
        addMetaRow(meta, "Source", "Analyse IA basée sur les feedbacks étudiants réels");
        doc.add(meta);
    }

    private void addMetaRow(PdfPTable table, String key, String value) {
        PdfPCell k = new PdfPCell(new Phrase(key, font(10, Font.BOLD, TEXT_DARK)));
        k.setBorder(Rectangle.NO_BORDER);
        k.setPadding(3);
        k.setBackgroundColor(BG_LIGHT);
        PdfPCell v = new PdfPCell(new Phrase(value, font(10, Font.NORMAL, TEXT_BODY)));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(3);
        v.setBackgroundColor(BG_LIGHT);
        table.addCell(k);
        table.addCell(v);
    }

    private void addStructuredContent(Document doc, String htmlContent) throws DocumentException {
        // Strip HTML tags and clean markdown
        String clean = htmlContent
            .replaceAll("<br\\s*/?>", "\n")
            .replaceAll("<p[^>]*>", "")
            .replaceAll("</p>", "\n")
            .replaceAll("<li[^>]*>", "• ")
            .replaceAll("</li>", "\n")
            .replaceAll("<ul[^>]*>|</ul>|<ol[^>]*>|</ol>", "\n")
            .replaceAll("<h1[^>]*>", "§H1§")
            .replaceAll("</h1>", "\n")
            .replaceAll("<h2[^>]*>", "§H2§")
            .replaceAll("</h2>", "\n")
            .replaceAll("<h3[^>]*>", "§H3§")
            .replaceAll("</h3>", "\n")
            .replaceAll("<h4[^>]*>", "§H4§")
            .replaceAll("</h4>", "\n")
            .replaceAll("<strong[^>]*>|</strong>|<b[^>]*>|</b>", "**")
            .replaceAll("<em[^>]*>|</em>|<i[^>]*>|</i>", "_")
            .replaceAll("<hr[^>]*/?>", "\n---\n")
            .replaceAll("<blockquote[^>]*>", "❝ ")
            .replaceAll("</blockquote>", "\n")
            .replaceAll("<table[^>]*>|</table>|<thead>|</thead>|<tbody>|</tbody>|<tr[^>]*>|</tr>", "\n")
            .replaceAll("<th[^>]*>", "| ")
            .replaceAll("</th>", " ")
            .replaceAll("<td[^>]*>", "| ")
            .replaceAll("</td>", " ")
            .replaceAll("<[^>]+>", "")
            .replaceAll("&nbsp;", " ")
            .replaceAll("&amp;", "&")
            .replaceAll("&lt;", "<")
            .replaceAll("&gt;", ">")
            .replaceAll("&eacute;", "é")
            .replaceAll("&egrave;", "è")
            .replaceAll("&agrave;", "à")
            .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
            .replaceAll("#{1,6}\\s*", "")
            .replaceAll("\\s{3,}", "\n\n")
            .trim();

        boolean inSection = false;
        for (String line : clean.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                if (inSection) {
                    doc.add(new Paragraph(" ", font(6, Font.NORMAL, TEXT_BODY)));
                }
                continue;
            }
            if (t.equals("---")) {
                doc.add(new Chunk(new LineSeparator(0.5f, 100, BORDER, Element.ALIGN_CENTER, 0)));
                doc.add(new Paragraph(" ", font(4, Font.NORMAL, TEXT_BODY)));
                inSection = false;
                continue;
            }
            if (t.startsWith("§H1§")) {
                addSectionHeader(doc, t.replace("§H1§", "").trim(), PRIMARY, 18);
                inSection = true;
            } else if (t.startsWith("§H2§")) {
                addSectionHeader(doc, t.replace("§H2§", "").trim(), SECONDARY, 15);
                inSection = true;
            } else if (t.startsWith("§H3§")) {
                addSubHeader(doc, t.replace("§H3§", "").trim());
                inSection = true;
            } else if (t.startsWith("§H4§")) {
                Paragraph p = new Paragraph(t.replace("§H4§", "").trim(), font(12, Font.BOLD, ACCENT));
                p.setSpacingBefore(8); p.setSpacingAfter(4);
                doc.add(p);
                inSection = true;
            } else if (t.startsWith("•")) {
                addBulletItem(doc, t.substring(1).trim());
                inSection = true;
            } else if (t.startsWith("❝")) {
                addQuote(doc, t.substring(1).trim());
                inSection = true;
            } else if (t.contains("HAUTE") || t.contains("haute")) {
                addBadgeLine(doc, t, GREEN_BG, GREEN_FG);
                inSection = true;
            } else if (t.contains("BASSE") || t.contains("basse")) {
                addBadgeLine(doc, t, RED_BG, RED_FG);
                inSection = true;
            } else if (t.contains("MOYENNE") || t.contains("moyenne")) {
                addBadgeLine(doc, t, YELLOW_BG, YELLOW_FG);
                inSection = true;
            } else {
                Paragraph p = new Paragraph(t, font(11, Font.NORMAL, TEXT_BODY));
                p.setAlignment(Element.ALIGN_JUSTIFIED);
                p.setSpacingAfter(6);
                p.setLeading(17);
                doc.add(p);
                inSection = true;
            }
        }
    }

    private void addSectionHeader(Document doc, String text, BaseColor color, int size) throws DocumentException {
        doc.add(new Paragraph(" ", font(4, Font.NORMAL, TEXT_BODY)));
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(8);
        PdfPCell cell = new PdfPCell(new Phrase(text, font(size, Font.BOLD, color)));
        cell.setBackgroundColor(BG_LIGHT);
        cell.setBorderColor(color);
        cell.setBorderWidthLeft(4);
        cell.setBorderWidthTop(0); cell.setBorderWidthRight(0); cell.setBorderWidthBottom(0);
        cell.setPadding(10);
        t.addCell(cell);
        doc.add(t);
    }

    private void addSubHeader(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, font(13, Font.BOLD, SECONDARY));
        p.setSpacingBefore(12); p.setSpacingAfter(5);
        doc.add(p);
    }

    private void addBulletItem(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph();
        p.setIndentationLeft(16);
        p.setSpacingAfter(4);
        p.add(new Chunk("▸  ", font(11, Font.BOLD, PRIMARY)));
        p.add(new Chunk(text, font(11, Font.NORMAL, TEXT_BODY)));
        doc.add(p);
    }

    private void addQuote(Document doc, String text) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(95);
        t.setSpacingBefore(6); t.setSpacingAfter(6);
        PdfPCell cell = new PdfPCell(new Phrase("❝  " + text, font(11, Font.ITALIC, TEXT_BODY)));
        cell.setBackgroundColor(BG_CARD);
        cell.setBorderColor(ACCENT);
        cell.setBorderWidthLeft(3);
        cell.setBorderWidthTop(0); cell.setBorderWidthRight(0); cell.setBorderWidthBottom(0);
        cell.setPadding(10);
        t.addCell(cell);
        doc.add(t);
    }

    private void addBadgeLine(Document doc, String text, BaseColor bg, BaseColor fg) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(4);
        PdfPCell cell = new PdfPCell(new Phrase(text, font(11, Font.BOLD, fg)));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(7);
        t.addCell(cell);
        doc.add(t);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(new Paragraph("\n"));
        doc.add(new Chunk(new LineSeparator(0.5f, 100, BORDER, Element.ALIGN_CENTER, 0)));
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(8);
        footer.add(new Chunk("Rapport généré automatiquement par le système IA AutoLearn\n",
                font(9, Font.ITALIC, TEXT_MUTED)));
        footer.add(new Chunk("© 2026 AutoLearn — Tous droits réservés",
                font(8, Font.NORMAL, TEXT_MUTED)));
        doc.add(footer);
    }
}
