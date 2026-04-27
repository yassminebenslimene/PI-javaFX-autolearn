package tn.esprit.controllers;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.GroqTranslationService;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.TextToSpeechService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.ConfigLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class FrontChapitreDetailController {

    @FXML private Label labelBadge;
    @FXML private Label labelCours;
    @FXML private Label labelTitre;
    @FXML private WebView webContent;
    @FXML private Label labelRessource;
    @FXML private VBox boxRessource;
    @FXML private Button btnSuivant;
    @FXML private Button btnQuiz;
    @FXML private MenuButton btnTranslate;
    @FXML private Label labelInfoType;
    @FXML private Label labelInfoFichier;

    private Cours cours;
    private List<Chapitre> chapitres;
    private int currentIndex;
    private Runnable onRetourCallback;
    private Runnable onQuizCallback;
    private String originalContent;
    private String currentDisplayedContent; // Contenu actuellement affiché (peut être traduit)
    private String currentLanguage = "original";

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();
    private final CourseProgressService progressService = new CourseProgressService();
    private GroqTranslationService translationService;

    public void setChapitre(Cours cours, Chapitre chapitre, Runnable onRetour) {
        this.cours = cours;
        this.onRetourCallback = onRetour;
        this.chapitres = serviceChapitre.consulterParCoursId(cours.getId());

        this.currentIndex = chapitres.indexOf(chapitre);
        if (currentIndex < 0) {
            for (int i = 0; i < chapitres.size(); i++) {
                if (chapitres.get(i).getId() == chapitre.getId()) {
                    currentIndex = i;
                    break;
                }
            }
        }
        afficher(chapitre);
    }

    public void setOnQuizCallback(Runnable callback) {
        this.onQuizCallback = callback;
    }

    private void afficher(Chapitre chapitre) {
        // Ne plus marquer comme vu automatiquement
        // La progression augmentera seulement après réussite du quiz
        
        labelBadge.setText("📌  Chapitre " + chapitre.getOrdre());
        labelCours.setText("🎓  " + cours.getTitre()
            + "   •   " + cours.getMatiere()
            + "   •   Niveau : " + cours.getNiveau());
        labelTitre.setText(chapitre.getTitre());

        String contenu = chapitre.getContenu() == null ? "" : chapitre.getContenu();
        
        // Stocker le contenu original et le contenu actuellement affiché
        originalContent = contenu;
        currentDisplayedContent = contenu;
        currentLanguage = "original";
        
        webContent.getEngine().loadContent(buildHtml(contenu));

        String res = chapitre.getRessources();
        if (res != null && !res.isBlank()) {
            String embedUrl = convertToYoutubeEmbed(res);
            if (embedUrl != null) {
                String videoHtml = "<!DOCTYPE html><html><body style='margin:0;background:#000;'>"
                    + "<iframe width='100%' height='360' src='" + embedUrl + "' "
                    + "frameborder='0' allow='accelerometer; autoplay; clipboard-write; "
                    + "encrypted-media; gyroscope; picture-in-picture' allowfullscreen></iframe>"
                    + "</body></html>";
                webContent.getEngine().loadContent(videoHtml);
                labelRessource.setText("▶  Video YouTube integree");
            } else {
                labelRessource.setText(res);
            }
            boxRessource.setVisible(true);
            boxRessource.setManaged(true);
        } else {
            boxRessource.setVisible(false);
            boxRessource.setManaged(false);
        }

        String type = chapitre.getRessourceType();
        labelInfoType.setText("Type : " + (type != null ? type : "-"));
        String fichier = chapitre.getRessourceFichier();
        labelInfoFichier.setText("Fichier : " + (fichier != null && !fichier.isBlank() ? fichier : "-"));

        boolean hasSuivant = currentIndex < chapitres.size() - 1;
        btnSuivant.setVisible(hasSuivant);
        btnSuivant.setManaged(hasSuivant);
    }

    private String buildHtml(String contenu) {
        boolean isHtml = contenu.contains("<") && contenu.contains(">");
        String body = isHtml ? contenu : "<p>" + contenu.replace("\n", "</p><p>") + "</p>";

        return "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'>"
            + "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>"
            + "<style>"
            + "body { font-family: 'Segoe UI', 'Arial', sans-serif; font-size: 15px;"
            + " color: #2c2c2c; line-height: 1.8; padding: 0; margin: 0; background: #f8f9fa; }"
            + "h1 { font-size: 28px; font-weight: 800; color: white; margin: 0; padding: 32px 40px;"
            + " background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);"
            + " border-radius: 0; box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3); }"
            + "h2 { font-size: 20px; font-weight: 700; color: #667eea; margin: 32px 40px 16px 40px;"
            + " padding: 12px 0 12px 20px; border-left: 5px solid #667eea;"
            + " background: linear-gradient(90deg, rgba(102, 126, 234, 0.08) 0%, transparent 100%); }"
            + "h3 { font-size: 17px; font-weight: 700; color: #2c2c2c; margin: 24px 40px 12px 40px;"
            + " padding-bottom: 8px; border-bottom: 2px solid #e0e0e0; }"
            + "p { margin: 12px 40px; color: #4a4a4a; line-height: 1.9; font-size: 15px; }"
            + "ul, ol { margin: 12px 40px 12px 60px; padding: 0; }"
            + "li { margin: 10px 0; color: #4a4a4a; line-height: 1.7; }"
            + "li::marker { color: #667eea; font-weight: bold; }"
            + "strong { color: #2c2c2c; font-weight: 700; }"
            + "code { background: #f0f4ff; color: #5b21b6; font-family: 'Consolas', 'Monaco', monospace;"
            + " padding: 3px 8px; border-radius: 4px; font-size: 14px; border: 1px solid #e0e7ff; }"
            + "pre { background: #1e1e2e; color: #e0e0e0; font-family: 'Consolas', 'Monaco', monospace;"
            + " padding: 24px 28px; border-radius: 8px; overflow-x: auto; margin: 20px 40px;"
            + " font-size: 14px; line-height: 1.7; box-shadow: 0 4px 12px rgba(0,0,0,0.15);"
            + " border: 1px solid #2a2a3e; }"
            + "pre code { background: transparent; color: #e0e0e0; padding: 0; border: none; }"
            + "a { color: #667eea; text-decoration: none; font-weight: 500; }"
            + "a:hover { text-decoration: underline; color: #764ba2; }"
            + "blockquote { border-left: 5px solid #667eea; margin: 20px 40px; padding: 16px 24px;"
            + " background: linear-gradient(90deg, rgba(102, 126, 234, 0.08) 0%, transparent 100%);"
            + " color: #4a4a4a; border-radius: 0 8px 8px 0; font-style: italic; }"
            + "hr { border: none; border-top: 2px solid #e0e0e0; margin: 32px 40px; }"
            + ".code-comment { color: #6c7086; font-style: italic; }"
            + ".code-keyword { color: #89b4fa; font-weight: bold; }"
            + ".code-string { color: #a6e3a1; }"
            + ".code-function { color: #f9e2af; }"
            + "</style></head><body>"
            + body
            + "</body></html>";
    }

    @FXML private void onRetour() { if (onRetourCallback != null) onRetourCallback.run(); }
    @FXML private void onQuiz() { if (onQuizCallback != null) onQuizCallback.run(); }

    private String convertToYoutubeEmbed(String url) {
        if (url == null) {
            return null;
        }
        String videoId = null;
        if (url.contains("youtube.com/watch")) {
            int idx = url.indexOf("v=");
            if (idx >= 0) {
                videoId = url.substring(idx + 2);
                int amp = videoId.indexOf("&");
                if (amp >= 0) {
                    videoId = videoId.substring(0, amp);
                }
            }
        } else if (url.contains("youtu.be/") || url.contains("youtube.com/shorts/")) {
            int idx = url.lastIndexOf("/");
            if (idx >= 0) {
                videoId = url.substring(idx + 1);
            }
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
        chooser.setTitle("📄 Enregistrer en PDF");
        chooser.setInitialFileName("Chapitre_" + chapitre.getOrdre() + "_"
            + chapitre.getTitre().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File file = chooser.showSaveDialog(webContent.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            // Couleurs modernes et vibrantes
            BaseColor primaryPurple = new BaseColor(139, 69, 255);     // #8B45FF - Violet principal
            BaseColor lightPurple = new BaseColor(168, 85, 247);       // #A855F7 - Violet clair
            BaseColor darkPurple = new BaseColor(109, 40, 217);        // #6D28D9 - Violet foncé
            BaseColor accentBlue = new BaseColor(59, 130, 246);        // #3B82F6 - Bleu accent
            BaseColor successGreen = new BaseColor(34, 197, 94);       // #22C55E - Vert succès
            BaseColor warningOrange = new BaseColor(251, 146, 60);     // #FB923C - Orange attention
            
            // Couleurs de fond et texte
            BaseColor lightBg = new BaseColor(248, 250, 252);          // #F8FAFC - Fond clair
            BaseColor cardBg = new BaseColor(255, 255, 255);           // #FFFFFF - Fond carte
            BaseColor codeBg = new BaseColor(30, 41, 59);              // #1E293B - Fond code sombre
            BaseColor codeFg = new BaseColor(226, 232, 240);           // #E2E8F0 - Texte code clair
            BaseColor textPrimary = new BaseColor(15, 23, 42);         // #0F172A - Texte principal
            BaseColor textSecondary = new BaseColor(100, 116, 139);    // #64748B - Texte secondaire
            BaseColor borderColor = new BaseColor(226, 232, 240);      // #E2E8F0 - Bordures

            // Polices avec tailles optimisées
            Font fontHeader = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, textSecondary);
            Font fontTitle = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, BaseColor.WHITE);
            Font fontSubtitle = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(203, 213, 225));
            Font fontBadge = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font fontH1 = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, primaryPurple);
            Font fontH2 = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, darkPurple);
            Font fontH3 = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, textPrimary);
            Font fontBody = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, textPrimary);
            Font fontCode = new Font(Font.FontFamily.COURIER, 10, Font.NORMAL, codeFg);
            Font fontEmoji = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, textPrimary);

            Document doc = new Document(com.itextpdf.text.PageSize.A4, 40, 40, 50, 40);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            // En-tête avec logo et branding
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{70f, 30f});
            
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setPadding(0);
            Paragraph logoText = new Paragraph("🎓 AutoLearn", 
                new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, primaryPurple));
            logoCell.addElement(logoText);
            
            PdfPCell dateCell = new PdfPCell();
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setPadding(0);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph dateText = new Paragraph("📅 " + java.time.LocalDate.now().toString(), fontHeader);
            dateCell.addElement(dateText);
            
            headerTable.addCell(logoCell);
            headerTable.addCell(dateCell);
            doc.add(headerTable);
            doc.add(new Paragraph(" "));

            // Bannière principale avec dégradé simulé
            PdfPTable bannerTable = new PdfPTable(1);
            bannerTable.setWidthPercentage(100);
            bannerTable.setSpacingBefore(10);
            
            PdfPCell bannerCell = new PdfPCell();
            bannerCell.setBackgroundColor(primaryPurple);
            bannerCell.setPadding(25);
            bannerCell.setBorder(Rectangle.NO_BORDER);
            
            // Badge du chapitre avec emoji
            Paragraph badge = new Paragraph("📚 Chapitre " + chapitre.getOrdre(), fontBadge);
            badge.setSpacingAfter(8);
            bannerCell.addElement(badge);
            
            // Titre principal
            Paragraph titre = new Paragraph(chapitre.getTitre(), fontTitle);
            titre.setSpacingAfter(10);
            bannerCell.addElement(titre);
            
            // Métadonnées avec emojis
            Paragraph meta = new Paragraph(
                "🎯 " + cours.getTitre() + "   •   📖 " + cours.getMatiere() + "   •   ⭐ Niveau : " + cours.getNiveau(),
                fontSubtitle);
            bannerCell.addElement(meta);
            
            bannerTable.addCell(bannerCell);
            doc.add(bannerTable);
            doc.add(new Paragraph(" "));

            // Carte de contenu principal
            PdfPTable contentCard = new PdfPTable(1);
            contentCard.setWidthPercentage(100);
            contentCard.setSpacingBefore(15);
            
            PdfPCell contentCell = new PdfPCell();
            contentCell.setBackgroundColor(cardBg);
            contentCell.setBorderColor(borderColor);
            contentCell.setBorderWidth(1f);
            contentCell.setPadding(20);
            
            // Titre de section avec emoji
            Paragraph contentTitle = new Paragraph("📝 Contenu du Chapitre", fontH1);
            contentTitle.setSpacingAfter(15);
            contentCell.addElement(contentTitle);
            
            contentCard.addCell(contentCell);
            doc.add(contentCard);

            // Utiliser le contenu actuel du chapitre (peut être traduit ou modifié)
            String contenu = currentDisplayedContent != null ? currentDisplayedContent : 
                           (chapitre.getContenu() == null ? "" : chapitre.getContenu());
            renderModernHtmlToPdf(doc, contenu, fontH1, fontH2, fontH3, fontBody, fontCode, fontEmoji, 
                                codeBg, primaryPurple, accentBlue, successGreen, warningOrange, cardBg, borderColor);

            // Section ressources avec design moderne
            if (chapitre.getRessources() != null && !chapitre.getRessources().isBlank()) {
                doc.add(new Paragraph(" "));
                
                PdfPTable resourceCard = new PdfPTable(1);
                resourceCard.setWidthPercentage(100);
                resourceCard.setSpacingBefore(10);
                
                PdfPCell resourceCell = new PdfPCell();
                resourceCell.setBackgroundColor(new BaseColor(240, 249, 255)); // Bleu très clair
                resourceCell.setBorderColor(accentBlue);
                resourceCell.setBorderWidth(2f);
                resourceCell.setPadding(15);
                
                // Titre de la ressource avec emoji
                Paragraph resourceTitle = new Paragraph("🔗 Ressources Complémentaires", 
                    new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, accentBlue));
                resourceTitle.setSpacingAfter(8);
                resourceCell.addElement(resourceTitle);
                
                // Contenu de la ressource
                String resourceText = chapitre.getRessources();
                if (resourceText.contains("youtube.com") || resourceText.contains("youtu.be")) {
                    resourceText = "🎥 Vidéo YouTube : " + resourceText;
                } else if (resourceText.contains("http")) {
                    resourceText = "🌐 Lien web : " + resourceText;
                } else {
                    resourceText = "📄 " + resourceText;
                }
                
                Paragraph resourceContent = new Paragraph(resourceText, 
                    new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(30, 64, 175)));
                resourceCell.addElement(resourceContent);
                
                resourceCard.addCell(resourceCell);
                doc.add(resourceCard);
            }

            // Séparateur décoratif
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(" "));
            
            // Pied de page moderne avec emojis
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{33f, 34f, 33f});
            
            // Colonne gauche - Branding
            PdfPCell footerLeft = new PdfPCell();
            footerLeft.setBorder(Rectangle.TOP);
            footerLeft.setBorderColor(borderColor);
            footerLeft.setPaddingTop(10);
            footerLeft.setPaddingBottom(5);
            Paragraph brandText = new Paragraph("🚀 Généré par AutoLearn", 
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, textSecondary));
            footerLeft.addElement(brandText);
            
            // Colonne centre - Date
            PdfPCell footerCenter = new PdfPCell();
            footerCenter.setBorder(Rectangle.TOP);
            footerCenter.setBorderColor(borderColor);
            footerCenter.setPaddingTop(10);
            footerCenter.setPaddingBottom(5);
            footerCenter.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph dateFooter = new Paragraph("📅 " + java.time.LocalDate.now(), 
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, textSecondary));
            footerCenter.addElement(dateFooter);
            
            // Colonne droite - Progression
            PdfPCell footerRight = new PdfPCell();
            footerRight.setBorder(Rectangle.TOP);
            footerRight.setBorderColor(borderColor);
            footerRight.setPaddingTop(10);
            footerRight.setPaddingBottom(5);
            footerRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph progressText = new Paragraph("📊 Chapitre " + chapitre.getOrdre() + "/" + chapitres.size(), 
                new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, textSecondary));
            footerRight.addElement(progressText);
            
            footerTable.addCell(footerLeft);
            footerTable.addCell(footerCenter);
            footerTable.addCell(footerRight);
            doc.add(footerTable);

            doc.close();

            // Message de succès avec emoji
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("✅ PDF Généré avec Succès");
            alert.setHeaderText("📄 Votre chapitre est prêt !");
            alert.setContentText("💾 Fichier sauvegardé : " + file.getAbsolutePath() + 
                               "\n\n🎨 Design moderne appliqué avec succès !");
            alert.showAndWait();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("❌ Erreur de Génération");
            errorAlert.setHeaderText("🚫 Impossible de créer le PDF");
            errorAlert.setContentText("💥 Erreur : " + ex.getMessage());
            errorAlert.showAndWait();
        }
    }

    private void renderModernHtmlToPdf(Document doc, String html,
                                     Font fontH1, Font fontH2, Font fontH3, Font fontBody, Font fontCode, Font fontEmoji,
                                     BaseColor codeBg, BaseColor primaryPurple, BaseColor accentBlue, 
                                     BaseColor successGreen, BaseColor warningOrange, BaseColor cardBg, BaseColor borderColor) throws Exception {
        
        String[] lines = html.split("\n");
        StringBuilder currentBlock = new StringBuilder();
        boolean inPre = false;
        boolean inList = false;

        for (String raw : lines) {
            String line = raw.trim();

            // Gestion des blocs de code avec style moderne
            if (line.toLowerCase().contains("<pre") || line.toLowerCase().contains("<code")) {
                inPre = true;
            }
            if (inPre) {
                currentBlock.append(raw).append("\n");
                if (line.toLowerCase().contains("</pre>") || line.toLowerCase().contains("</code>")) {
                    String codeText = currentBlock.toString()
                        .replaceAll("<[^>]+>", "").replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">").replaceAll("&amp;", "&").trim();
                    
                    // Carte de code moderne avec ombre simulée
                    PdfPTable codeCard = new PdfPTable(1);
                    codeCard.setWidthPercentage(100);
                    codeCard.setSpacingBefore(10);
                    codeCard.setSpacingAfter(10);
                    
                    PdfPCell codeCell = new PdfPCell();
                    codeCell.setBackgroundColor(codeBg);
                    codeCell.setBorderColor(new BaseColor(71, 85, 105)); // Bordure gris foncé
                    codeCell.setBorderWidth(1f);
                    codeCell.setPadding(15);
                    
                    // En-tête du code avec emoji
                    Paragraph codeHeader = new Paragraph("💻 Code", 
                        new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(156, 163, 175)));
                    codeHeader.setSpacingAfter(8);
                    codeCell.addElement(codeHeader);
                    
                    // Contenu du code
                    Paragraph codePara = new Paragraph(codeText, fontCode);
                    codePara.setLeading(14);
                    codeCell.addElement(codePara);
                    
                    codeCard.addCell(codeCell);
                    doc.add(codeCard);
                    currentBlock = new StringBuilder();
                    inPre = false;
                }
                continue;
            }

            // Titres H1 avec design moderne
            if (line.matches("(?i)<h1[^>]*>.*</h1>")) {
                String text = line.replaceAll("<[^>]+>", "").trim();
                
                PdfPTable h1Card = new PdfPTable(1);
                h1Card.setWidthPercentage(100);
                h1Card.setSpacingBefore(15);
                h1Card.setSpacingAfter(10);
                
                PdfPCell h1Cell = new PdfPCell();
                h1Cell.setBackgroundColor(new BaseColor(245, 243, 255)); // Violet très clair
                h1Cell.setBorderColor(primaryPurple);
                h1Cell.setBorderWidth(2f);
                h1Cell.setPadding(12);
                
                Paragraph h1Para = new Paragraph("🎯 " + text, fontH1);
                h1Cell.addElement(h1Para);
                h1Card.addCell(h1Cell);
                doc.add(h1Card);
                continue;
            }

            // Titres H2 avec accent coloré
            if (line.matches("(?i)<h2[^>]*>.*</h2>")) {
                String text = line.replaceAll("<[^>]+>", "").trim();
                
                PdfPTable h2Table = new PdfPTable(new float[]{4f, 96f});
                h2Table.setWidthPercentage(100);
                h2Table.setSpacingBefore(12);
                h2Table.setSpacingAfter(6);
                
                // Barre colorée à gauche
                PdfPCell accentCell = new PdfPCell();
                accentCell.setBackgroundColor(accentBlue);
                accentCell.setBorder(Rectangle.NO_BORDER);
                h2Table.addCell(accentCell);
                
                // Contenu du titre
                PdfPCell textCell = new PdfPCell();
                textCell.setBorder(Rectangle.NO_BORDER);
                textCell.setBackgroundColor(new BaseColor(239, 246, 255)); // Bleu très clair
                textCell.setPadding(10);
                Paragraph h2Para = new Paragraph("📋 " + text, fontH2);
                textCell.addElement(h2Para);
                h2Table.addCell(textCell);
                
                doc.add(h2Table);
                continue;
            }

            // Titres H3 simples avec emoji
            if (line.matches("(?i)<h3[^>]*>.*</h3>")) {
                String text = line.replaceAll("<[^>]+>", "").trim();
                Paragraph h3 = new Paragraph("📌 " + text, fontH3);
                h3.setSpacingBefore(10);
                h3.setSpacingAfter(5);
                doc.add(h3);
                continue;
            }

            // Éléments de liste avec emojis et indentation
            if (line.matches("(?i)<li[^>]*>.*</li>")) {
                String text = line.replaceAll("<[^>]+>", "").trim();
                
                // Choisir un emoji selon le contenu
                String emoji = "▶️";
                if (text.toLowerCase().contains("important") || text.toLowerCase().contains("attention")) {
                    emoji = "⚠️";
                } else if (text.toLowerCase().contains("exemple") || text.toLowerCase().contains("example")) {
                    emoji = "💡";
                } else if (text.toLowerCase().contains("note") || text.toLowerCase().contains("remarque")) {
                    emoji = "📝";
                }
                
                PdfPTable listTable = new PdfPTable(new float[]{8f, 92f});
                listTable.setWidthPercentage(100);
                listTable.setSpacingAfter(3);
                
                // Cellule emoji
                PdfPCell emojiCell = new PdfPCell();
                emojiCell.setBorder(Rectangle.NO_BORDER);
                emojiCell.setPaddingLeft(20);
                emojiCell.addElement(new Paragraph(emoji, fontEmoji));
                
                // Cellule texte
                PdfPCell textListCell = new PdfPCell();
                textListCell.setBorder(Rectangle.NO_BORDER);
                textListCell.addElement(new Paragraph(text, fontBody));
                
                listTable.addCell(emojiCell);
                listTable.addCell(textListCell);
                doc.add(listTable);
                continue;
            }

            // Paragraphes normaux avec espacement amélioré
            if (!line.isEmpty() && !line.matches("(?i)<(ul|ol|/ul|/ol|/li|html|body|head|/html|/body)[^>]*>")) {
                String text = line.replaceAll("<[^>]+>", "")
                    .replaceAll("&nbsp;", " ").replaceAll("&lt;", "<")
                    .replaceAll("&gt;", ">").replaceAll("&amp;", "&").trim();
                
                if (!text.isEmpty()) {
                    // Ajouter des emojis contextuels
                    if (text.toLowerCase().contains("important")) {
                        text = "⚡ " + text;
                    } else if (text.toLowerCase().contains("exemple")) {
                        text = "💡 " + text;
                    } else if (text.toLowerCase().contains("attention") || text.toLowerCase().contains("warning")) {
                        text = "⚠️ " + text;
                    }
                    
                    Paragraph p = new Paragraph(text, fontBody);
                    p.setLeading(16);
                    p.setSpacingAfter(6);
                    p.setAlignment(Element.ALIGN_JUSTIFIED);
                    doc.add(p);
                }
            }
        }
    }

    // ========== ASSISTANT IA ==========

    @FXML
    public void onAssistantIA() {
        Chapitre chapitre = chapitres.get(currentIndex);
        String contenu = originalContent != null ? originalContent : chapitre.getContenu();
        if (contenu == null || contenu.isBlank()) {
            new Alert(Alert.AlertType.WARNING, "Ce chapitre n'a pas de contenu.").showAndWait();
            return;
        }
        showAssistantDialog(chapitre, contenu);
    }

    private void showAssistantDialog(Chapitre chapitre, String contenu) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("🤖 Assistant IA — " + chapitre.getTitre());
        dialog.setResizable(true);

        // Root avec couleur de fond claire et moderne
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);
        root.setStyle("-fx-background-color:#f8fafc;");

        // Header avec dégradé violet moderne
        javafx.scene.layout.VBox header = new javafx.scene.layout.VBox(4);
        header.setStyle("-fx-background-color:linear-gradient(to right,#8b5cf6,#a78bfa); -fx-padding:20 24 20 24;");
        javafx.scene.control.Label hTitle = new javafx.scene.control.Label("🤖 Assistant Pédagogique IA");
        hTitle.setStyle("-fx-font-size:17; -fx-font-weight:800; -fx-text-fill:white;");
        javafx.scene.control.Label hSub = new javafx.scene.control.Label("Explication personnalisée • Synthèse vocale • Llama 3.3 70B");
        hSub.setStyle("-fx-font-size:11; -fx-text-fill:rgba(255,255,255,0.9);");
        header.getChildren().addAll(hTitle, hSub);

        // Body
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(16);
        body.setStyle("-fx-padding:20 24 16 24;");

        // Niveau selector avec couleurs claires
        javafx.scene.layout.HBox niveauRow = new javafx.scene.layout.HBox(12);
        niveauRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label niveauLbl = new javafx.scene.control.Label("Niveau d'explication :");
        niveauLbl.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#334155;");
        javafx.scene.control.ToggleGroup tg = new javafx.scene.control.ToggleGroup();
        javafx.scene.control.ToggleButton btnDeb = new javafx.scene.control.ToggleButton("🟢 Débutant");
        javafx.scene.control.ToggleButton btnAdv = new javafx.scene.control.ToggleButton("🔴 Avancé");
        String tbBase = "-fx-font-size:12; -fx-font-weight:600; -fx-padding:7 18 7 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;";
        btnDeb.setStyle(tbBase + "-fx-background-color:#8b5cf6; -fx-text-fill:white;");
        btnAdv.setStyle(tbBase + "-fx-background-color:#e2e8f0; -fx-text-fill:#64748b;");
        btnDeb.setToggleGroup(tg); btnAdv.setToggleGroup(tg); btnDeb.setSelected(true);
        btnDeb.selectedProperty().addListener((o, was, is) -> {
            if (is) { btnDeb.setStyle(tbBase + "-fx-background-color:#8b5cf6; -fx-text-fill:white;"); }
            else    { btnDeb.setStyle(tbBase + "-fx-background-color:#e2e8f0; -fx-text-fill:#64748b;"); }
        });
        btnAdv.selectedProperty().addListener((o, was, is) -> {
            if (is) { btnAdv.setStyle(tbBase + "-fx-background-color:#8b5cf6; -fx-text-fill:white;"); }
            else    { btnAdv.setStyle(tbBase + "-fx-background-color:#e2e8f0; -fx-text-fill:#64748b;"); }
        });
        javafx.scene.control.Button btnGenerate = new javafx.scene.control.Button("✨ Générer l'explication");
        btnGenerate.setStyle("-fx-background-color:linear-gradient(to right,#8b5cf6,#a78bfa); -fx-text-fill:white; -fx-font-weight:700; -fx-font-size:12; -fx-padding:8 20 8 20; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
        niveauRow.getChildren().addAll(niveauLbl, btnDeb, btnAdv, btnGenerate);

        // Status avec couleur visible
        javafx.scene.control.Label statusLbl = new javafx.scene.control.Label("");
        statusLbl.setStyle("-fx-font-size:12; -fx-text-fill:#8b5cf6; -fx-font-weight:600;");

        // Result area (WebView) avec fond clair
        javafx.scene.web.WebView resultView = new javafx.scene.web.WebView();
        resultView.setPrefHeight(380);
        resultView.setStyle("-fx-background-color:white;");
        resultView.getEngine().loadContent(buildAssistantPlaceholder(), "text/html");

        // TTS Controls avec fond clair et bordure colorée
        javafx.scene.layout.VBox ttsBox = new javafx.scene.layout.VBox(10);
        ttsBox.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:12; -fx-padding:14 16 14 16; -fx-border-color:#8b5cf6; -fx-border-width:2; -fx-border-radius:12;");
        ttsBox.setVisible(false);

        javafx.scene.control.Label ttsTitle = new javafx.scene.control.Label("🔊 Synthèse Vocale");
        ttsTitle.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#6d28d9;");

        javafx.scene.layout.HBox ttsControls = new javafx.scene.layout.HBox(10);
        ttsControls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.control.Button btnPlay  = new javafx.scene.control.Button("▶ Lire");
        javafx.scene.control.Button btnPause = new javafx.scene.control.Button("⏸ Pause");
        javafx.scene.control.Button btnStop  = new javafx.scene.control.Button("⏹ Stop");
        javafx.scene.control.Button btnTest  = new javafx.scene.control.Button("🔧 Test");
        String ttsBtn = "-fx-font-size:12; -fx-font-weight:600; -fx-padding:7 16 7 16; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;";
        btnPlay.setStyle(ttsBtn  + "-fx-background-color:#22c55e;  -fx-text-fill:white;");
        btnPause.setStyle(ttsBtn + "-fx-background-color:#f59e0b; -fx-text-fill:white;");
        btnStop.setStyle(ttsBtn  + "-fx-background-color:#ef4444; -fx-text-fill:white;");
        btnTest.setStyle(ttsBtn  + "-fx-background-color:#3b82f6; -fx-text-fill:white;");

        javafx.scene.control.Label speedLbl = new javafx.scene.control.Label("Vitesse :");
        speedLbl.setStyle("-fx-font-size:12; -fx-text-fill:#475569; -fx-font-weight:600;");
        javafx.scene.control.Slider speedSlider = new javafx.scene.control.Slider(0.5, 2.0, 1.0);
        speedSlider.setPrefWidth(100);
        speedSlider.setStyle("-fx-control-inner-background:#ddd6fe;");
        javafx.scene.control.Label speedVal = new javafx.scene.control.Label("1.0x");
        speedVal.setStyle("-fx-font-size:11; -fx-text-fill:#6d28d9; -fx-min-width:35; -fx-font-weight:700;");
        speedSlider.valueProperty().addListener((o, ov, nv) ->
            speedVal.setText(String.format("%.1fx", nv.doubleValue())));

        ttsControls.getChildren().addAll(btnPlay, btnPause, btnStop, btnTest, speedLbl, speedSlider, speedVal);
        ttsBox.getChildren().addAll(ttsTitle, ttsControls);

        // TTS engine using dedicated service
        final String[] ttsText = {""};
        final TextToSpeechService ttsService = new TextToSpeechService();

        btnPlay.setOnAction(e -> {
            if (ttsText[0].isEmpty()) {
                statusLbl.setText("⚠️ Aucun texte à lire");
                return;
            }
            
            statusLbl.setText("🔊 Démarrage de la lecture...");
            double rate = speedSlider.getValue();
            int sapiRate = (int)((rate - 1.0) * 10);
            
            ttsService.speak(
                ttsText[0],
                sapiRate,
                () -> javafx.application.Platform.runLater(() -> 
                    statusLbl.setText("✅ Lecture terminée")
                ),
                error -> javafx.application.Platform.runLater(() -> 
                    statusLbl.setText("❌ Erreur: " + error)
                )
            );
            
            statusLbl.setText("🔊 Lecture en cours...");
        });

        btnPause.setOnAction(e -> {
            ttsService.stop();
            statusLbl.setText("⏸ Lecture arrêtée");
        });

        btnStop.setOnAction(e -> {
            ttsService.stop();
            statusLbl.setText("⏹ Lecture stoppée");
        });
        
        // Bouton de test TTS
        btnTest.setOnAction(e -> {
            statusLbl.setText("🔧 Test de la synthèse vocale...");
            ttsService.test(
                () -> javafx.application.Platform.runLater(() -> 
                    statusLbl.setText("✅ Test réussi ! TTS fonctionne.")
                ),
                error -> javafx.application.Platform.runLater(() -> 
                    statusLbl.setText("❌ Test échoué: " + error)
                )
            );
        });

        // Nettoyer le service TTS quand le dialog se ferme
        dialog.setOnCloseRequest(e -> {
            ttsService.stop();
        });

        // Generate action
        btnGenerate.setOnAction(e -> {
            String niveau = btnDeb.isSelected() ? "Débutant" : "Avancé";
            btnGenerate.setDisable(true);
            statusLbl.setText("⏳ Génération de l'explication en cours...");
            ttsBox.setVisible(false);

            new Thread(() -> {
                try {
                    String apiKey = tn.esprit.tools.ConfigLoader.getGroqApiKey();
                    String model  = tn.esprit.tools.ConfigLoader.getGroqModel();
                    GroqTranslationService groq = new GroqTranslationService(apiKey, model);
                    String prompt = buildAssistantPrompt(chapitre.getTitre(), contenu, niveau);
                    String result = groq.translate(prompt, "");

                    javafx.application.Platform.runLater(() -> {
                        String html = buildAssistantHtml(result, chapitre.getTitre(), niveau);
                        resultView.getEngine().loadContent(html, "text/html");
                        ttsText[0] = stripHtml(result);
                        ttsBox.setVisible(true);
                        statusLbl.setText("✅ Explication générée !");
                        btnGenerate.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        statusLbl.setText("❌ Erreur: " + ex.getMessage());
                        btnGenerate.setDisable(false);
                    });
                }
            }).start();
        });

        body.getChildren().addAll(niveauRow, statusLbl, resultView, ttsBox);
        root.getChildren().addAll(header, body);

        dialog.setScene(new javafx.scene.Scene(root, 720, 680));
        dialog.show();
    }

    private String buildAssistantPrompt(String titre, String contenu, String niveau) {
        String niveauDesc = niveau.equals("Débutant")
            ? "langage très simple, analogies du quotidien, pas de jargon, exemples basiques commentés ligne par ligne"
            : "terminologie technique précise, concepts approfondis, optimisations, patterns professionnels";

        String texte = contenu.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        if (texte.length() > 2000) texte = texte.substring(0, 2000) + "...";

        return String.format(
            "Tu es un professeur expert. Génère une explication TRÈS DÉTAILLÉE du chapitre suivant.\n\n" +
            "Chapitre: \"%s\" | Niveau: %s (%s)\n\nContenu:\n%s\n\n" +
            "GÉNÈRE EXACTEMENT ce HTML structuré en français:\n\n" +
            "<h3>📋 Résumé</h3>\n" +
            "<p>[3-4 phrases résumant clairement l'essentiel du chapitre]</p>\n\n" +
            "<h3>📖 Explication détaillée</h3>\n" +
            "<p>[Paragraphe 1: explication du concept principal, adapté niveau %s, 4-5 phrases]</p>\n" +
            "<p>[Paragraphe 2: approfondissement avec détails techniques]</p>\n" +
            "<p>[Paragraphe 3: cas d'usage concrets et applications pratiques]</p>\n\n" +
            "<h3>💻 Exemples de code</h3>\n" +
            "<p>[Brève description de l'exemple]</p>\n" +
            "<pre><code>[Exemple de code complet et commenté, pertinent au sujet '%s']</code></pre>\n" +
            "<p>[Explication ligne par ligne du code ci-dessus]</p>\n" +
            "<pre><code>[Deuxième exemple de code si pertinent]</code></pre>\n\n" +
            "<h3>🎯 Points clés à retenir</h3>\n" +
            "<ul>\n" +
            "<li>[Point clé 1 avec explication courte]</li>\n" +
            "<li>[Point clé 2 avec explication courte]</li>\n" +
            "<li>[Point clé 3 avec explication courte]</li>\n" +
            "<li>[Point clé 4 avec explication courte]</li>\n" +
            "<li>[Point clé 5 avec explication courte]</li>\n" +
            "</ul>\n\n" +
            "RÈGLES: Réponds UNIQUEMENT avec le HTML. Sois très détaillé. Les exemples de code doivent être réels et fonctionnels.",
            titre, niveau, niveauDesc, texte, niveau, titre
        );
    }

    private String buildAssistantHtml(String content, String titre, String niveau) {
        String badgeColor = niveau.equals("Débutant") ? "#22c55e" : "#ef4444";
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:20px 24px;background:white;color:#1e293b;line-height:1.8;}"
            + "h3{font-size:15px;font-weight:700;color:#6d28d9;margin:20px 0 8px 0;padding:8px 14px;"
            + "background:#f5f3ff;border-left:4px solid #8b5cf6;border-radius:0 8px 8px 0;}"
            + "p{margin:8px 0;color:#334155;font-size:14px;}"
            + "ul{margin:8px 0 8px 20px;padding:0;}"
            + "li{margin:6px 0;color:#334155;font-size:14px;}"
            + "li::marker{color:#8b5cf6;}"
            + "pre{background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;"
            + "padding:16px 18px;margin:12px 0;overflow-x:auto;}"
            + "code{font-family:'Consolas','Monaco',monospace;font-size:13px;color:#475569;line-height:1.6;}"
            + "pre code{background:transparent;padding:0;}"
            + "strong{color:#0f172a;font-weight:700;}"
            + ".badge{display:inline-block;padding:3px 12px;border-radius:20px;font-size:11px;font-weight:700;"
            + "background:" + badgeColor + ";color:white;margin-bottom:12px;}"
            + "</style></head><body>"
            + "<div class='badge'>Niveau " + niveau + "</div>"
            + content
            + "</body></html>";
    }

    private String buildAssistantPlaceholder() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:40px;background:white;"
            + "color:#94a3b8;display:flex;align-items:center;justify-content:center;"
            + "height:240px;text-align:center;}"
            + "div{font-size:14px;line-height:2;}"
            + ".icon{font-size:40px;display:block;margin-bottom:12px;}"
            + "</style></head><body><div><span class='icon'>🤖</span>"
            + "Choisissez un niveau et cliquez sur<br><strong style='color:#8b5cf6'>✨ Générer l'explication</strong>"
            + "</div></body></html>";
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String escapeJs(String text) {
        // Limiter à 2000 chars pour TTS
        if (text.length() > 2000) text = text.substring(0, 2000);
        return "'" + text.replace("\\", "\\\\").replace("'", "\\'")
                         .replace("\n", " ").replace("\r", "") + "'";
    }

    // ========== MÉTHODES DE TRADUCTION ==========
    
    @FXML
    public void onTranslateFrench() {
        System.out.println(">>> onTranslateFrench() appelée");
        System.out.println(">>> currentLanguage = " + currentLanguage);
        // Ne pas retraduire si déjà en français
        if (currentLanguage.equals("Français")) {
            System.out.println(">>> Déjà en français, annulation");
            return;
        }
        translateContent("Français");
    }
    
    @FXML
    public void onTranslateEnglish() {
        System.out.println(">>> onTranslateEnglish() appelée");
        System.out.println(">>> currentLanguage = " + currentLanguage);
        // Ne pas retraduire si déjà en anglais
        if (currentLanguage.equals("English")) {
            System.out.println(">>> Déjà en anglais, annulation");
            return;
        }
        translateContent("English");
    }
    
    @FXML
    public void onShowOriginal() {
        if (originalContent != null) {
            webContent.getEngine().loadContent(buildHtml(originalContent), "text/html; charset=UTF-8");
            currentLanguage = "original";
            currentDisplayedContent = originalContent; // Mettre à jour le contenu affiché
            btnTranslate.setText("🌐 Traduire");
        }
    }
    
    private void translateContent(String targetLanguage) {
        System.out.println("=== DÉBUT TRADUCTION ===");
        System.out.println("Langue cible: " + targetLanguage);
        System.out.println("Langue actuelle: " + currentLanguage);
        
        // Initialiser le service de traduction si nécessaire
        if (translationService == null) {
            String apiKey = ConfigLoader.getGroqApiKey();
            String model = ConfigLoader.getGroqModel();
            
            System.out.println("API Key présente: " + (apiKey != null && !apiKey.isEmpty()));
            System.out.println("Modèle: " + model);
            
            if (apiKey == null || apiKey.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Clé API manquante");
                alert.setHeaderText("Configuration requise");
                alert.setContentText("Veuillez configurer votre clé API Groq dans config.properties\n\n" +
                                   "Obtenez une clé gratuite sur: https://console.groq.com/keys");
                alert.showAndWait();
                return;
            }
            
            translationService = new GroqTranslationService(apiKey, model);
        }
        
        // Sauvegarder le contenu original UNIQUEMENT la première fois
        if (originalContent == null) {
            Chapitre currentChapter = chapitres.get(currentIndex);
            originalContent = currentChapter.getContenu();
            System.out.println("Contenu original sauvegardé (longueur: " + originalContent.length() + ")");
        }
        
        System.out.println("Texte à traduire (premiers 100 chars): " + 
            originalContent.substring(0, Math.min(100, originalContent.length())));
        
        // Afficher un indicateur de chargement
        btnTranslate.setText("⏳ Traduction...");
        btnTranslate.setDisable(true);
        
        // IMPORTANT: Toujours traduire depuis le texte ORIGINAL, pas depuis la dernière traduction
        final String textToTranslate = originalContent;
        
        // Traduire dans un thread séparé
        new Thread(() -> {
            try {
                System.out.println("Appel API Groq...");
                String translatedText = translationService.translate(textToTranslate, targetLanguage);
                System.out.println("Traduction reçue (longueur: " + translatedText.length() + ")");
                System.out.println("Traduction (premiers 200 chars): " + 
                    translatedText.substring(0, Math.min(200, translatedText.length())));
                
                // Vérifier si la traduction contient bien des caractères français
                boolean hasFrenchChars = translatedText.contains("é") || translatedText.contains("è") || 
                                        translatedText.contains("à") || translatedText.contains("ç");
                System.out.println("Contient des caractères français: " + hasFrenchChars);
                
                javafx.application.Platform.runLater(() -> {
                    // Charger avec l'encodage UTF-8 explicite
                    // Utiliser data URI pour forcer l'encodage UTF-8
                    String htmlContent = buildHtml(translatedText);
                    webContent.getEngine().loadContent(htmlContent, "text/html");
                    
                    currentLanguage = targetLanguage;
                    currentDisplayedContent = translatedText; // Mettre à jour le contenu affiché
                    btnTranslate.setText("🌐 " + getLanguageFlag(targetLanguage));
                    btnTranslate.setDisable(false);
                    System.out.println("=== TRADUCTION TERMINÉE ===");
                });
            } catch (Exception e) {
                System.err.println("ERREUR DE TRADUCTION: " + e.getMessage());
                e.printStackTrace();
                
                javafx.application.Platform.runLater(() -> {
                    btnTranslate.setText("🌐 Traduire");
                    btnTranslate.setDisable(false);
                    
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de traduction");
                    alert.setHeaderText("Impossible de traduire");
                    alert.setContentText("Erreur: " + e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    private String getLanguageFlag(String language) {
        if (language.contains("Français")) return "🇫🇷 FR";
        if (language.contains("English")) return "🇬🇧 EN";
        return "Traduire";
    }

}
