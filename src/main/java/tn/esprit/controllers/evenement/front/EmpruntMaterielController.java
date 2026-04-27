package tn.esprit.controllers.evenement.front;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import tn.esprit.entities.Evenement;
import tn.esprit.session.SessionManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Emprunt de Matériel — modal in-memory, aucune DB.
 * Intègre : ZXing (QR code), iText 5 (PDF reçu), javax.sound.sampled (son confirmation).
 * État géré en Map<String, ItemMateriel> réinitialisée à chaque ouverture.
 */
public class EmpruntMaterielController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    // ── Initialisation items ─────────────────────────────────────

    public static Map<String, ItemMateriel> initItems() {
        Map<String, ItemMateriel> items = new LinkedHashMap<>();
        String[][] data = {
                {"🔌", "Chargeur laptop"},
                {"🔌", "Multiprise"},
                {"📽️", "Vidéoprojecteur"},
                {"🔗", "Câble HDMI"},
                {"🔌", "Adaptateur USB-C"},
                {"🖊️", "Marqueurs (set)"},
                {"📝", "Post-its (bloc)"},
                {"📡", "Extension WiFi"},
                {"🎧", "Casque audio"},
                {"📷", "Webcam HD"},
                {"🖱️", "Pointeur laser"},
                {"🔌", "Rallonge électrique"}
        };
        for (String[] d : data) {
            ItemMateriel item = new ItemMateriel();
            item.emoji = d[0];
            item.nom = d[1];
            item.disponible = true;
            items.put(d[1], item);
        }
        return items;
    }

    // ── Point d'entrée ───────────────────────────────────────────

    public static void show(Evenement ev, Window owner) {
        Map<String, ItemMateriel> items = initItems();
        double winW = owner.getWidth();
        double winH = owner.getHeight();

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initOwner(owner);

        VBox modal = new VBox(0);
        modal.setPrefWidth(600);
        modal.setMaxWidth(600);
        modal.setPrefHeight(winH * 0.88);
        modal.setMaxHeight(winH * 0.88);
        modal.setStyle("-fx-background-color:white; -fx-background-radius:20;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),30,0,0,8);");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 24, 22, 28));
        header.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
                + "-fx-background-radius:20 20 0 0;");
        Label titre = new Label("🔌  Emprunt de Matériel");
        titre.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:white;");
        HBox.setHgrow(titre, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color:rgba(255,255,255,0.25); -fx-text-fill:white;"
                + "-fx-font-size:15; -fx-font-weight:700; -fx-background-radius:50%;"
                + "-fx-min-width:34; -fx-min-height:34; -fx-max-width:34; -fx-max-height:34;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        closeBtn.setOnAction(e -> dialog.close());
        header.getChildren().addAll(titre, closeBtn);

        // Body scrollable
        VBox body = new VBox(10);
        body.setPadding(new Insets(20, 24, 20, 24));
        body.setStyle("-fx-background-color:white;");

        // Banner message
        Label bannerLbl = new Label("");
        bannerLbl.setVisible(false);
        bannerLbl.setManaged(false);
        bannerLbl.setWrapText(true);
        bannerLbl.setMaxWidth(Double.MAX_VALUE);
        bannerLbl.setPadding(new Insets(10, 16, 10, 16));
        bannerLbl.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#065f46;"
                + "-fx-font-size:13; -fx-font-weight:600; -fx-background-radius:10;");

        body.getChildren().add(bannerLbl);

        // Sous-titre
        Label subTitre = new Label("Sélectionnez un équipement disponible :");
        subTitre.setStyle("-fx-font-size:13; -fx-font-weight:600; -fx-text-fill:#4a5568;");
        body.getChildren().add(subTitre);

        // Liste items
        VBox listBox = new VBox(8);
        renderItemList(items, listBox, bannerLbl, ev, body);
        body.getChildren().add(listBox);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-width:0;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Footer
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(14, 24, 18, 24));
        footer.setStyle("-fx-background-color:#f8f9fa; -fx-background-radius:0 0 20 20;"
                + "-fx-border-color:#eeeeee; -fx-border-width:1 0 0 0;");
        Button fermerBtn = new Button("Fermer");
        fermerBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 28 10 28; -fx-background-radius:25;"
                + "-fx-border-color:#d0d0d0; -fx-border-radius:25; -fx-border-width:1.5;"
                + "-fx-cursor:hand;");
        fermerBtn.setOnAction(e -> dialog.close());
        footer.getChildren().add(fermerBtn);

        modal.getChildren().addAll(header, scroll, footer);

        StackPane root = new StackPane(modal);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:rgba(0,0,0,0.62);");

        Runnable close = () -> {
            FadeTransition ft = new FadeTransition(Duration.millis(180), root);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setOnFinished(e -> dialog.close());
            ft.play();
        };
        root.setOnMouseClicked(e -> { if (e.getTarget() == root) close.run(); });

        Scene scene = new Scene(root, winW, winH);
        scene.setFill(Color.TRANSPARENT);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) close.run();
        });
        dialog.setScene(scene);
        dialog.setX(owner.getX());
        dialog.setY(owner.getY());

        root.setOpacity(0);
        modal.setTranslateY(45);
        dialog.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), root);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        TranslateTransition slideUp = new TranslateTransition(Duration.millis(260), modal);
        slideUp.setFromY(45); slideUp.setToY(0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fadeIn, slideUp).play();
    }

    // ── Rendu liste items ────────────────────────────────────────

    private static void renderItemList(Map<String, ItemMateriel> items, VBox listBox,
                                        Label bannerLbl, Evenement ev, VBox body) {
        listBox.getChildren().clear();
        int i = 0;
        for (ItemMateriel item : items.values()) {
            HBox row = buildItemRow(item, items, listBox, bannerLbl, ev, body);
            row.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(250), row);
            ft.setFromValue(0); ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 50));
            ft.play();
            listBox.getChildren().add(row);
            i++;
        }
    }

    private static HBox buildItemRow(ItemMateriel item, Map<String, ItemMateriel> items,
                                      VBox listBox, Label bannerLbl, Evenement ev, VBox body) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        String borderColor = item.disponible ? "#d1fae5" : "#fee2e2";
        row.setStyle("-fx-background-color:white; -fx-background-radius:12;"
                + "-fx-border-color:" + borderColor + "; -fx-border-radius:12; -fx-border-width:1.5;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        // Emoji
        Label emojiLbl = new Label(item.emoji);
        emojiLbl.setStyle("-fx-font-size:26; -fx-background-color:" + (item.disponible ? "#f0fdf4" : "#fef2f2") + ";"
                + "-fx-background-radius:50%; -fx-padding:8 10 8 10;"
                + "-fx-min-width:48; -fx-min-height:48; -fx-alignment:CENTER;");

        // Nom
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nomLbl = new Label(item.nom);
        nomLbl.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        info.getChildren().add(nomLbl);
        if (!item.disponible && item.emprunteurNom != null) {
            Label empLbl = new Label("Emprunté par : " + item.emprunteurNom + " (" + item.dureeHeures + "h)");
            empLbl.setStyle("-fx-font-size:11; -fx-text-fill:#9ca3af;");
            info.getChildren().add(empLbl);
        }

        // Badge disponibilité
        Label badge = new Label(item.disponible ? "✅ Disponible" : "🔴 Occupé");
        badge.setStyle("-fx-background-color:" + (item.disponible ? "#d1fae5" : "#fee2e2") + ";"
                + "-fx-text-fill:" + (item.disponible ? "#065f46" : "#dc2626") + ";"
                + "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:20;"
                + "-fx-padding:5 12 5 12;");

        row.getChildren().addAll(emojiLbl, info, badge);

        if (item.disponible) {
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#f0fdf4; -fx-background-radius:12;"
                    + "-fx-border-color:#6ee7b7; -fx-border-radius:12; -fx-border-width:1.5;"
                    + "-fx-effect:dropshadow(gaussian,rgba(16,185,129,0.15),10,0,0,3);"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color:white; -fx-background-radius:12;"
                    + "-fx-border-color:#d1fae5; -fx-border-radius:12; -fx-border-width:1.5;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);"));
            row.setOnMouseClicked(e -> showEmpruntForm(item, items, listBox, bannerLbl, ev, body));
        } else {
            row.setOnMouseClicked(e -> {
                bannerLbl.setText("ℹ️  Cet item est actuellement utilisé.");
                bannerLbl.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#92400e;"
                        + "-fx-font-size:13; -fx-font-weight:600; -fx-background-radius:10;"
                        + "-fx-padding:10 16 10 16;");
                bannerLbl.setVisible(true);
                bannerLbl.setManaged(true);
            });
        }
        return row;
    }

    // ── Formulaire emprunt inline ────────────────────────────────

    private static void showEmpruntForm(ItemMateriel item, Map<String, ItemMateriel> items,
                                         VBox listBox, Label bannerLbl, Evenement ev, VBox body) {
        // Masquer la liste, afficher le formulaire dans body
        listBox.setVisible(false);
        listBox.setManaged(false);

        VBox form = new VBox(16);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:16;"
                + "-fx-border-color:#c4b5fd; -fx-border-radius:16; -fx-border-width:2;");

        Label formTitre = new Label("📋  Formulaire d'emprunt — " + item.emoji + " " + item.nom);
        formTitre.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:#4c1d95;");
        formTitre.setWrapText(true);

        // Nom utilisateur
        Label nomLabel = new Label("Votre nom :");
        nomLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#374151;");
        TextField nomField = new TextField();
        var user = SessionManager.getCurrentUser();
        if (user != null) {
            nomField.setText(user.getPrenom() + " " + user.getNom());
        }
        nomField.setStyle("-fx-font-size:13; -fx-padding:10 14 10 14; -fx-background-radius:10;"
                + "-fx-border-color:#c4b5fd; -fx-border-radius:10; -fx-border-width:1.5;");
        nomField.setEditable(false);

        // Durée incrémentale
        Label dureeLabel = new Label("Durée d'utilisation :");
        dureeLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#374151;");

        final int[] duree = {1};
        HBox dureeRow = new HBox(16);
        dureeRow.setAlignment(Pos.CENTER_LEFT);
        Button minusBtn = new Button("−");
        minusBtn.setStyle("-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed; -fx-font-size:18;"
                + "-fx-font-weight:700; -fx-background-radius:50%; -fx-min-width:40; -fx-min-height:40;"
                + "-fx-cursor:hand; -fx-border-width:0;");
        Label dureeVal = new Label("1h");
        dureeVal.setStyle("-fx-font-size:20; -fx-font-weight:800; -fx-text-fill:#4c1d95;"
                + "-fx-min-width:60; -fx-alignment:CENTER;");
        Button plusBtn = new Button("+");
        plusBtn.setStyle("-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed; -fx-font-size:18;"
                + "-fx-font-weight:700; -fx-background-radius:50%; -fx-min-width:40; -fx-min-height:40;"
                + "-fx-cursor:hand; -fx-border-width:0;");

        minusBtn.setOnAction(e -> {
            duree[0] = Math.max(1, duree[0] - 1);
            dureeVal.setText(duree[0] + "h");
        });
        plusBtn.setOnAction(e -> {
            duree[0] = Math.min(8, duree[0] + 1);
            dureeVal.setText(duree[0] + "h");
        });
        dureeRow.getChildren().addAll(minusBtn, dureeVal, plusBtn,
                new Label("(max 8h)") {{
                    setStyle("-fx-font-size:11; -fx-text-fill:#9ca3af;");
                }});

        // Boutons action
        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        Button annulerBtn = new Button("Annuler");
        annulerBtn.setStyle("-fx-background-color:white; -fx-text-fill:#555; -fx-font-size:13;"
                + "-fx-font-weight:600; -fx-padding:10 22 10 22; -fx-background-radius:25;"
                + "-fx-border-color:#d0d0d0; -fx-border-radius:25; -fx-border-width:1.5;"
                + "-fx-cursor:hand;");
        annulerBtn.setOnAction(e -> {
            body.getChildren().remove(form);
            listBox.setVisible(true);
            listBox.setManaged(true);
        });

        Button confirmerBtn = new Button("✅  Confirmer l'emprunt");
        confirmerBtn.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
                + "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;"
                + "-fx-padding:10 22 10 22; -fx-background-radius:25; -fx-cursor:hand;"
                + "-fx-border-width:0;");
        confirmerBtn.setOnAction(e -> {
            String userName = nomField.getText().trim();
            confirmEmprunt(item, userName.isEmpty() ? null : userName, duree[0]);
            if (!item.disponible) {
                body.getChildren().remove(form);
                listBox.setVisible(true);
                listBox.setManaged(true);
                renderItemList(items, listBox, bannerLbl, ev, body);
                showConfirmationBanner(bannerLbl, item, duree[0]);
                showQRAndPDF(item, ev, duree[0], body);
                SoundUtil.playSound("confirmation");
            } else {
                bannerLbl.setText("❌  Utilisateur non connecté.");
                bannerLbl.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#dc2626;"
                        + "-fx-font-size:13; -fx-font-weight:600; -fx-background-radius:10;"
                        + "-fx-padding:10 16 10 16;");
                bannerLbl.setVisible(true);
                bannerLbl.setManaged(true);
            }
        });
        btnRow.getChildren().addAll(annulerBtn, confirmerBtn);

        form.getChildren().addAll(formTitre, nomLabel, nomField, dureeLabel, dureeRow, btnRow);

        // Animation entrée formulaire
        form.setOpacity(0);
        form.setTranslateY(15);
        body.getChildren().add(form);
        FadeTransition ft = new FadeTransition(Duration.millis(300), form);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), form);
        tt.setFromY(15); tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(ft, tt).play();
    }

    // ── Confirmation emprunt ─────────────────────────────────────

    public static void confirmEmprunt(ItemMateriel item, String userName, int duree) {
        if (userName == null || userName.isBlank()) return;
        item.disponible = false;
        item.emprunteurNom = userName;
        item.dureeHeures = duree;
    }

    private static void showConfirmationBanner(Label bannerLbl, ItemMateriel item, int duree) {
        bannerLbl.setText("✅  " + item.nom + " emprunté pour " + duree + "h. Bonne utilisation !");
        bannerLbl.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#065f46;"
                + "-fx-font-size:13; -fx-font-weight:600; -fx-background-radius:10;"
                + "-fx-padding:10 16 10 16;");
        bannerLbl.setVisible(true);
        bannerLbl.setManaged(true);
    }

    // ── QR Code + PDF ────────────────────────────────────────────

    private static void showQRAndPDF(ItemMateriel item, Evenement ev, int duree, VBox body) {
        String content = "EMPRUNT MATERIEL - AutoLearn\n"
                + "Item: " + item.nom + "\n"
                + "Emprunteur: " + item.emprunteurNom + "\n"
                + "Durée: " + duree + "h\n"
                + "Heure: " + LocalDateTime.now().format(FMT) + "\n"
                + "Événement: " + (ev != null ? ev.getTitre() : "N/A");

        HBox qrRow = new HBox(16);
        qrRow.setAlignment(Pos.CENTER_LEFT);
        qrRow.setPadding(new Insets(12));
        qrRow.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:12;"
                + "-fx-border-color:#c4b5fd; -fx-border-radius:12; -fx-border-width:1.5;");

        // QR Code
        try {
            javafx.scene.image.Image qrImg = generateQRCodeImage(content, 120);
            ImageView qrView = new ImageView(qrImg);
            qrView.setFitWidth(100);
            qrView.setFitHeight(100);
            qrView.setPreserveRatio(true);

            VBox qrInfo = new VBox(8);
            HBox.setHgrow(qrInfo, Priority.ALWAYS);
            Label qrTitre = new Label("📱 QR Code de confirmation");
            qrTitre.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#4c1d95;");
            Label qrDesc = new Label("Scannez pour vérifier l'emprunt");
            qrDesc.setStyle("-fx-font-size:11; -fx-text-fill:#6b7280;");

            Button pdfBtn = new Button("📄  Télécharger le reçu PDF");
            pdfBtn.setStyle("-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
                    + "-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700;"
                    + "-fx-padding:8 18 8 18; -fx-background-radius:20; -fx-cursor:hand;"
                    + "-fx-border-width:0;");
            pdfBtn.setOnAction(e -> generateAndSavePDF(item, ev, duree, content));

            qrInfo.getChildren().addAll(qrTitre, qrDesc, pdfBtn);
            qrRow.getChildren().addAll(qrView, qrInfo);
        } catch (Exception ex) {
            Label errLbl = new Label("QR indisponible");
            errLbl.setStyle("-fx-font-size:11; -fx-text-fill:#9ca3af;");
            qrRow.getChildren().add(errLbl);
        }

        qrRow.setOpacity(0);
        body.getChildren().add(qrRow);
        FadeTransition ft = new FadeTransition(Duration.millis(400), qrRow);
        ft.setFromValue(0); ft.setToValue(1);
        ft.setDelay(Duration.millis(300));
        ft.play();
    }

    private static javafx.scene.image.Image generateQRCodeImage(String content, int size)
            throws WriterException, java.io.IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        BufferedImage buffered = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "png", baos);
        return new javafx.scene.image.Image(new java.io.ByteArrayInputStream(baos.toByteArray()));
    }

    private static void generateAndSavePDF(ItemMateriel item, Evenement ev, int duree, String qrContent) {
        new Thread(() -> {
            try {
                String fileName = "recu_emprunt_" + item.nom.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
                String path = System.getProperty("user.home") + File.separator + fileName;

                Document doc = new Document(PageSize.A4);
                PdfWriter.getInstance(doc, new FileOutputStream(path));
                doc.open();

                // Header coloré
                PdfPTable headerTable = new PdfPTable(1);
                headerTable.setWidthPercentage(100);
                PdfPCell headerCell = new PdfPCell();
                headerCell.setBackgroundColor(new BaseColor(102, 126, 234));
                headerCell.setPadding(20);
                headerCell.setBorder(Rectangle.NO_BORDER);
                Paragraph headerText = new Paragraph("AutoLearn — Reçu d'Emprunt",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.WHITE));
                headerCell.addElement(headerText);
                headerTable.addCell(headerCell);
                doc.add(headerTable);
                doc.add(new Paragraph(" "));

                // Tableau infos
                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{1, 2});
                addTableRow(table, "Item", item.emoji + " " + item.nom);
                addTableRow(table, "Emprunteur", item.emprunteurNom);
                addTableRow(table, "Durée", duree + " heure(s)");
                addTableRow(table, "Heure d'emprunt", LocalDateTime.now().format(FMT));
                addTableRow(table, "Événement", ev != null ? ev.getTitre() : "N/A");
                doc.add(table);
                doc.add(new Paragraph(" "));

                // QR Code
                try {
                    QRCodeWriter writer = new QRCodeWriter();
                    BitMatrix matrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 150, 150);
                    BufferedImage buffered = MatrixToImageWriter.toBufferedImage(matrix);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(buffered, "png", baos);
                    com.itextpdf.text.Image qrImg = com.itextpdf.text.Image.getInstance(baos.toByteArray());
                    qrImg.setAlignment(Element.ALIGN_CENTER);
                    doc.add(qrImg);
                } catch (Exception ignored) {}

                // Footer
                doc.add(new Paragraph(" "));
                Paragraph footer = new Paragraph("Document généré automatiquement par AutoLearn.",
                        FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY));
                footer.setAlignment(Element.ALIGN_CENTER);
                doc.add(footer);

                doc.close();

                javafx.application.Platform.runLater(() ->
                        showToast("✅ PDF sauvegardé : " + path));
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() ->
                        showToast("❌ Erreur génération PDF"));
            }
        }, "pdf-generator").start();
    }

    private static void addTableRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        keyCell.setBackgroundColor(new BaseColor(237, 233, 254));
        keyCell.setPadding(8);
        PdfPCell valCell = new PdfPCell(new Phrase(value != null ? value : "",
                FontFactory.getFont(FontFactory.HELVETICA, 11)));
        valCell.setPadding(8);
        table.addCell(keyCell);
        table.addCell(valCell);
    }

    private static void showToast(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("AutoLearn");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
