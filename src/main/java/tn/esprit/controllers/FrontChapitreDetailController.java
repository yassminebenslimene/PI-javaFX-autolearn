package tn.esprit.controllers;

import com.itextpdf.text.*;
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

        // Ressource
        String res = chapitre.getRessources();
        if (res != null && !res.isBlank()) {
            labelRessource.setText(res);
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
            Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            Font fontAccent  = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(74, 58, 156));
            Font fontSub     = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, BaseColor.DARK_GRAY);
            Font fontBody    = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
            Font fontMeta    = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);

            // Header
            Paragraph header = new Paragraph("AutoLearn  —  " + cours.getTitre(), fontMeta);
            header.setAlignment(Element.ALIGN_RIGHT);
            doc.add(header);
            doc.add(new Paragraph(" "));

            // Titre
            doc.add(new Paragraph("Chapitre " + chapitre.getOrdre() + " : " + chapitre.getTitre(), fontAccent));
            com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
            line.setLineColor(new BaseColor(122, 106, 216));
            doc.add(new Chunk(line));
            doc.add(new Paragraph(" "));

            // Infos
            doc.add(new Paragraph("Cours : " + cours.getTitre()
                + "   |   Matière : " + cours.getMatiere()
                + "   |   Niveau : " + cours.getNiveau(), fontMeta));
            doc.add(new Paragraph(" "));

            // Contenu (texte brut sans HTML)
            doc.add(new Paragraph("Contenu", fontSub));
            doc.add(new Paragraph(" "));
            String contenu = chapitre.getContenu() == null ? "" : chapitre.getContenu();
            // Supprimer les balises HTML pour le PDF
            String texte = contenu.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
            Paragraph contenuPara = new Paragraph(texte, fontBody);
            contenuPara.setLeading(16);
            doc.add(contenuPara);

            // Ressource
            if (chapitre.getRessources() != null && !chapitre.getRessources().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Ressource", fontSub));
                doc.add(new Paragraph(chapitre.getRessources(), fontBody));
            }

            // Footer
            doc.add(new Paragraph(" "));
            doc.add(new Chunk(line));
            Paragraph footer = new Paragraph("Généré par AutoLearn", fontMeta);
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
}
