package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service d'envoi d'emails via l'API REST Brevo (ex-Sendinblue).
 * Utilise l'API Brevo v3 — 300 emails/jour gratuits.
 * Même clé API que le module Symfony AutoLearn.
 */
public class BrevoEmailService {

    private static final String BREVO_API_KEY =
            "System.getProperty("BREVO_API_KEY", "YOUR_BREVO_KEY")";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String FROM_EMAIL    = "autolearn66@gmail.com";
    private static final String FROM_NAME     = "AutoLearn";

    /**
     * Envoie un email HTML avec pièce jointe PDF optionnelle.
     *
     * @param toEmail      email du destinataire
     * @param toName       nom du destinataire
     * @param subject      sujet de l'email
     * @param htmlContent  contenu HTML de l'email
     * @param pdfBytes     bytes du PDF à joindre (peut être null)
     * @param pdfFileName  nom du fichier PDF joint
     * @return true si envoi réussi
     */
    public boolean sendEmail(String toEmail, String toName, String subject,
                              String htmlContent, byte[] pdfBytes, String pdfFileName) {
        try {
            JsonObject body = new JsonObject();

            // Expéditeur
            JsonObject sender = new JsonObject();
            sender.addProperty("name", FROM_NAME);
            sender.addProperty("email", FROM_EMAIL);
            body.add("sender", sender);

            // Destinataire
            JsonArray to = new JsonArray();
            JsonObject recipient = new JsonObject();
            recipient.addProperty("email", toEmail);
            recipient.addProperty("name", toName);
            to.add(recipient);
            body.add("to", to);

            body.addProperty("subject", subject);
            body.addProperty("htmlContent", htmlContent);

            // Pièce jointe PDF
            if (pdfBytes != null && pdfBytes.length > 0) {
                JsonArray attachments = new JsonArray();
                JsonObject attachment = new JsonObject();
                attachment.addProperty("name", pdfFileName != null ? pdfFileName : "badge.pdf");
                attachment.addProperty("content", Base64.getEncoder().encodeToString(pdfBytes));
                attachments.add(attachment);
                body.add("attachment", attachments);
            }

            // Envoi HTTP POST
            URL url = new URL(BREVO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "application/json");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("content-type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201 || responseCode == 200) {
                System.out.println("✅ Email envoyé via Brevo à: " + toEmail);
                return true;
            } else {
                InputStream errStream = conn.getErrorStream();
                if (errStream != null) {
                    String errMsg = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                    System.err.println("❌ Erreur Brevo [" + responseCode + "]: " + errMsg);
                    // Conseil si IP non autorisée
                    if (errMsg.contains("unrecognised IP") || errMsg.contains("unauthorized")) {
                        System.err.println("💡 Solution: Autorisez votre IP sur https://app.brevo.com/security/authorised_ips");
                    }
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ Exception envoi email Brevo: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envoi simple sans pièce jointe.
     */
    public boolean sendEmail(String toEmail, String toName, String subject, String htmlContent) {
        return sendEmail(toEmail, toName, subject, htmlContent, null, null);
    }
}
