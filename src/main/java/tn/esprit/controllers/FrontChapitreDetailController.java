package tn.esprit.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * FrontChapitreDetailController — affiche le contenu complet d'un chapitre.
 * Utilise WebView pour rendre le HTML proprement (titres, listes, code...).
 */
public class FrontChapitreDetailController {

    @FXML private Label   labelBadge;
    @FXML private Label   labelCours;
    @FXML private Label   labelTitre;
    @FXML private WebView webContent;       // rendu HTML du contenu
    @FXML private Label   labelRessource;
    @FXML private VBox    boxRessource;
    @FXML private Button  btnSuivant;
    @FXML private Button  btnQuiz;
    @FXML private Label   labelInfoType;
    @FXML private Label   labelInfoFichier;

    private Cours          cours;
    private List<Chapitre> chapitres;
    private int            currentIndex;
    private Runnable       onRetourCallback;
    private Runnable       onQuizCallback;

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    public void setChapitre(Cours cours, Chapitre chapitre, Runnable onRetour) {
        this.cours            = cours;
        this.onRetourCallback = onRetour;
        this.chapitres        = serviceChapitre.consulterParCoursId(cours.getId());

        this.currentIndex = chapitres.indexOf(chapitre);
        if (currentIndex < 0) {
            for (int i = 0; i < chapitres.size(); i++) {
                if (chapitres.get(i).getId() == chapitre.getId()) {
                    currentIndex = i; break;
                }
            }
        }
        afficher(chapitre);
    }

    public void setOnQuizCallback(Runnable callback) {
        this.onQuizCallback = callback;
    }

    private void afficher(Chapitre chapitre) {
        labelBadge.setText("📌  Chapitre " + chapitre.getOrdre());
        labelCours.setText("🎓  " + cours.getTitre()
            + "   •   " + cours.getMatiere()
            + "   •   Niveau : " + cours.getNiveau());
        labelTitre.setText(chapitre.getTitre());

        // Rendre le contenu dans WebView avec un style CSS propre
        String contenu = chapitre.getContenu() == null ? "" : chapitre.getContenu();
        webContent.getEngine().loadContent(buildHtml(contenu));

        // Ressource — si YouTube → embed dans WebView, sinon lien cliquable
        String res = chapitre.getRessources();
        if (res != null && !res.isBlank()) {
            String embedUrl = convertToYoutubeEmbed(res);
            if (embedUrl != null) {
                // Vidéo YouTube → afficher dans WebView
                String videoHtml = "<!DOCTYPE html><html><body style='margin:0;background:#000;'>"
                    + "<iframe width='100%' height='360' src='" + embedUrl + "' "
                    + "frameborder='0' allow='accelerometer; autoplay; clipboard-write; "
                    + "encrypted-media; gyroscope; picture-in-picture' allowfullscreen></iframe>"
                    + "</body></html>";
                webContent.getEngine().loadContent(videoHtml);
                labelRessource.setText("▶  Vidéo YouTube intégrée");
            } else {
                labelRessource.setText(res);
            }
            boxRessource.setVisible(true); boxRessource.setManaged(true);
        } else {
            boxRessource.setVisible(false); boxRessource.setManaged(false);
        }

        // Infos sidebar
        String type = chapitre.getRessourceType();
        labelInfoType.setText("Type : " + (type != null ? type : "—"));
        String fichier = chapitre.getRessourceFichier();
        labelInfoFichier.setText("Fichier : " + (fichier != null && !fichier.isBlank() ? fichier : "—"));

        // Bouton suivant
        boolean hasSuivant = currentIndex < chapitres.size() - 1;
        btnSuivant.setVisible(hasSuivant); btnSuivant.setManaged(hasSuivant);
    }

    /**
     * Construit le HTML complet avec CSS intégré pour un rendu propre.
     * Gère les balises h2, h3, p, ul, li, code, pre, strong, a.
     */
    private String buildHtml(String contenu) {
        // Si le contenu ne contient pas de balises HTML, on l'enveloppe dans <p>
        boolean isHtml = contenu.contains("<") && contenu.contains(">");
        String body = isHtml ? contenu : "<p>" + contenu.replace("\n", "</p><p>") + "</p>";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<style>"
            + "body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 14px;"
            + "       color: #333; line-height: 1.8; padding: 28px 32px; margin:0;"
            + "       background: white; }"
            + "h1 { font-size: 22px; font-weight: 800; color: #4e3b9c; margin: 24px 0 10px 0; }"
            + "h2 { font-size: 18px; font-weight: 700; color: #4e3b9c; margin: 20px 0 8px 0;"
            + "     border-left: 4px solid #7a6ad8; padding-left: 12px; }"
            + "h3 { font-size: 15px; font-weight: 700; color: #555; margin: 16px 0 6px 0; }"
            + "p  { margin: 8px 0; color: #444; }"
            + "ul, ol { margin: 8px 0 8px 20px; padding: 0; }"
            + "li { margin: 5px 0; color: #444; }"
            + "strong { color: #333; font-weight: 700; }"
            + "code { background: #f0eeff; color: #6d28d9; font-family: 'Consolas', monospace;"
            + "       padding: 2px 7px; border-radius: 5px; font-size: 13px; }"
            + "pre  { background: #1e1e2e; color: #cdd6f4; font-family: 'Consolas', monospace;"
            + "       padding: 16px 20px; border-radius: 10px; overflow-x: auto;"
            + "       font-size: 13px; line-height: 1.6; margin: 12px 0; }"
            + "pre code { background: transparent; color: inherit; padding: 0; }"
            + "a  { color: #7a6ad8; text-decoration: none; }"
            + "a:hover { text-decoration: underline; }"
            + "blockquote { border-left: 4px solid #c4b5fd; margin: 12px 0; padding: 8px 16px;"
            + "             background: #f5f3ff; color: #555; border-radius: 0 8px 8px 0; }"
            + "hr { border: none; border-top: 1px solid #eee; margin: 16px 0; }"
            + "</style></head><body>"
            + body
            + "</body></html>";
    }

    @FXML private void onRetour() { if (onRetourCallback != null) onRetourCallback.run(); }
    @FXML private void onQuiz()   { if (onQuizCallback   != null) onQuizCallback.run(); }

    /**
     * Convertit un lien YouTube normal en URL embed.
     * Supporte : youtube.com/watch?v=ID, youtu.be/ID, youtube.com/shorts/ID
     * Retourne null si ce n'est pas un lien YouTube.
     */
    private String convertToYoutubeEmbed(String url) {
        if (url == null) return null;
        String videoId = null;
        // Format : https://www.youtube.com/watch?v=VIDEO_ID
        if (url.contains("youtube.com/watch")) {
            int idx = url.indexOf("v=");
            if (idx >= 0) {
                videoId = url.substring(idx + 2);
                int amp = videoId.indexOf("&");
                if (amp >= 0) videoId = videoId.substring(0, amp);
            }
        }
        // Format : https://youtu.be/VIDEO_ID
        else if (url.contains("youtu.be/")) {
            int idx = url.lastIndexOf("/");
            if (idx >= 0) videoId = url.substring(idx + 1);
        }
        // Format : https://www.youtube.com/shorts/VIDEO_ID
        else if (url.contains("youtube.com/shorts/")) {
            int idx = url.lastIndexOf("/");
            if (idx >= 0) videoId = url.substring(idx + 1);
        }
        if (videoId != null && !videoId.isBlank()) {
            return "https://www.youtube.com/embed/" + videoId;
        }
        return null;
    }

    @FXML
    private void onSuivant() {
        if (currentIndex < chapitres.size() - 1) {
            currentIndex++;
            afficher(chapitres.get(currentIndex));
        }
    }

    @FXML
    private void onPdf() {
        Chapitre chapitre = chapitres.get(currentIndex);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer en PDF");
        chooser.setInitialFileName("Chapitre_" + chapitre.getOrdre() + "_"
            + chapitre.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File file = chooser.showSaveDialog(webContent.getScene().getWindow());
        if (file == null) return;

        try {
            // ── Couleurs ──────────────────────────────────────────────────────
            BaseColor VIOLET       = new BaseColor(78, 59, 156);   // #4e3b9c
            BaseColor VIOLET_LIGHT = new BaseColor(122, 106, 216); // #7a6ad8
            BaseColor CODE_BG      = new BaseColor(30, 30, 46);    // fond code sombre
            BaseColor CODE_FG      = new BaseColor(205, 214, 244); // texte code clair
            BaseColor GRAY_TEXT    = new BaseColor(100, 100, 100);
            BaseColor BORDER_LEFT  = new BaseColor(122, 106, 216);

            // ── Polices ───────────────────────────────────────────────────────
            Font fontHeader   = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC,  GRAY_TEXT);
            Font fontTitre    = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,    VIOLET);
            Font fontMeta     = new Font(Font.FontFamily.HELVETICA, 9,  Font.ITALIC,  GRAY_TEXT);
            Font fontH2       = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,    VIOLET_LIGHT);
            Font fontH3       = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,    new BaseColor(60, 60, 60));
            Font fontBody     = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL,  new BaseColor(50, 50, 50));
            Font fontCode     = new Font(Font.FontFamily.COURIER,   10, Font.NORMAL,  CODE_FG);
            Font fontRessource= new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL,  VIOLET_LIGHT);

            Document doc = new Document(PageSize.A4, 55, 55, 70, 55);
            PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // ── HEADER (en-tête page) ─────────────────────────────────────────
            Paragraph headerLine = new Paragraph("AutoLearn  —  " + cours.getTitre(), fontHeader);
            headerLine.setAlignment(Element.ALIGN_RIGHT);
            doc.add(headerLine);
            doc.add(new Paragraph(" "));

            // ── BANDEAU TITRE VIOLET ──────────────────────────────────────────
            PdfPTable bannerTable = new PdfPTable(1);
            bannerTable.setWidthPercentage(100);
            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(VIOLET);
            bannerCell.setPadding(18);
            bannerCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);

            // Badge "Chapitre N"
            Paragraph badge = new Paragraph("Chapitre " + chapitre.getOrdre(),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(200, 190, 255)));
            badge.setSpacingAfter(4);
            bannerCell.addElement(badge);

            // Titre principal
            Paragraph titrePara = new Paragraph(chapitre.getTitre(),
                new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.WHITE));
            titrePara.setSpacingAfter(6);
            bannerCell.addElement(titrePara);

            // Méta-infos
            Paragraph metaPara = new Paragraph(
                cours.getTitre() + "   •   " + cours.getMatiere() + "   •   Niveau : " + cours.getNiveau(),
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, new BaseColor(200, 190, 255)));
            bannerCell.addElement(metaPara);

            bannerTable.addCell(bannerCell);
            doc.add(bannerTable);
            doc.add(new Paragraph(" "));

            // ── CONTENU HTML → rendu structuré ───────────────────────────────
            String contenu = chapitre.getContenu() == null ? "" : chapitre.getContenu();
            renderHtmlToPdf(doc, contenu, fontH2, fontH3, fontBody, fontCode,
                            CODE_BG, CODE_FG, BORDER_LEFT, VIOLET_LIGHT);

            // ── RESSOURCE ─────────────────────────────────────────────────────
            if (chapitre.getRessources() != null && !chapitre.getRessources().isBlank()) {
                doc.add(new Paragraph(" "));
                PdfPTable resTable = new PdfPTable(1);
                resTable.setWidthPercentage(100);
                PdfPCell resCell = new PdfPCell();
                resCell.setBackgroundColor(new BaseColor(245, 243, 255));
                resCell.setBorderColor(VIOLET_LIGHT);
                resCell.setBorderWidth(1f);
                resCell.setPadding(10);
                resCell.addElement(new Paragraph("🔗  Ressource",
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, VIOLET_LIGHT)));
                resCell.addElement(new Paragraph(chapitre.getRessources(), fontRessource));
                resTable.addCell(resCell);
                doc.add(resTable);
            }

            // ── FOOTER ────────────────────────────────────────────────────────
            doc.add(new Paragraph(" "));
            com.itextpdf.text.pdf.draw.LineSeparator sep = new com.itextpdf.text.pdf.draw.LineSeparator();
            sep.setLineColor(new BaseColor(220, 215, 255));
            doc.add(new Chunk(sep));
            Paragraph footer = new Paragraph("Généré par AutoLearn  •  " + java.time.LocalDate.now(), fontHeader);
            footer.setAlignment(Element.ALIGN_CENTER);
            doc.add(footer);

            doc.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF téléchargé");
            alert.setHeaderText(null);
            alert.setContentText("Sauvegardé : " + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur PDF : " + ex.getMessage()).showAndWait();
        }
    }

    /**
     * Parse le HTML du contenu et génère des éléments iText structurés.
     * Gère : h2, h3, p, pre/code, ul/li, strong, texte brut.
     */
    private void renderHtmlToPdf(Document doc, String html,
            Font fontH2, Font fontH3, Font fontBody, Font fontCode,
            BaseColor codeBg, BaseColor codeFg, BaseColor borderLeft, BaseColor violet)
            throws Exception {

        // Découper le HTML en blocs par balises principales
        String[] lines = html.split("\n");
        StringBuilder currentBlock = new StringBuilder();
        boolean inPre = false;

        for (String raw : lines) {
            String line = raw.trim();

            // Bloc <pre> ou <code> → fond sombre
            if (line.toLowerCase().contains("<pre") || line.toLowerCase().contains("<code")) {
                inPre = true;
            }
            if (inPre) {
                currentBlock.append(raw).append("\n");
                if (line.toLowerCase().contains("</pre>") || line.toLowerCase().contains("</code>")) {
                    // Rendre le bloc code
                    String codeText = currentBlock.toString()
                        .replaceAll("<[^>]+>", "").replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">").replaceAll("&amp;", "&").trim();
                    PdfPTable codeTable = new PdfPTable(1);
                    codeTable.setWidthPercentage(100);
                    codeTable.setSpacingBefore(6);
                    codeTable.setSpacingAfter(6);
                    PdfPCell codeCell = new PdfPCell();
                    codeCell.setBackgroundColor(codeBg);
                    codeCell.setPadding(12);
                    codeCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                    Paragraph codePara = new Paragraph(codeText, fontCode);
                    codePara.setLeading(14);
                    codeCell.addElement(codePara);
                    codeTable.addCell(codeCell);
                    doc.add(codeTable);
                    currentBlock = new StringBuilder();
                    inPre = false;
                }
                continue;
            }

            // h2 → titre avec bordure gauche violette
            if (line.matches("(?i)<h2[^>]*>.*</h2>")) {
                String text = line.replaceAll("<[^>]+>", "").trim();
                PdfPTable h2Table = new PdfPTable(new float[]{3f, 97f});
                h2Table.setWidthPercentage(100);
                h2Table.setSpacingBefore(10);
                h2Table.setSpacingAfter(4);
                PdfPCell borderCell = new PdfPCell();
                borderCell.setBackgroundColor(borderLeft);
                borderCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                h2Table.addCell(borderCell);
                PdfPCell textCell = new PdfPCell(new Phrase(text, fontH2));
                textCell.setBorder(com.itextpdf.text.Rectangle.NO_BORDER);
                textCell.setBackgroundColor(new BaseColor(245, 243, 255));
                textCell.setPadding(8);
                h2Table.addCell(textCell);
                doc.add(h2Table);
                continue;
            }

            // h3 → sous-titre gras
            if (line.matches("(?i)<h3[^>]*>.*</h3>")) {
                String text = line.replaceAll("<[^>]+>", "").trim();
                Paragraph h3 = new Paragraph(text, fontH3);
                h3.setSpacingBefore(8); h3.setSpacingAfter(3);
                doc.add(h3);
                continue;
            }

            // li → puce
            if (line.matches("(?i)<li[^>]*>.*</li>")) {
                String text = "•  " + line.replaceAll("<[^>]+>", "").trim();
                Paragraph li = new Paragraph(text, fontBody);
                li.setIndentationLeft(15);
                li.setSpacingAfter(2);
                doc.add(li);
                continue;
            }

            // p ou texte brut
            if (!line.isEmpty() && !line.matches("(?i)<(ul|ol|/ul|/ol|/li|html|body|head|/html|/body)[^>]*>")) {
                String text = line.replaceAll("<[^>]+>", "")
                    .replaceAll("&nbsp;", " ").replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">").replaceAll("&amp;", "&").trim();
                if (!text.isEmpty()) {
                    Paragraph p = new Paragraph(text, fontBody);
                    p.setLeading(16);
                    p.setSpacingAfter(3);
                    doc.add(p);
                }
            }
        }
    }
}
