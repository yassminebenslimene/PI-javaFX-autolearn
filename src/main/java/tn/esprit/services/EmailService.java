package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends HTML emails from autolearn66@gmail.com using Gmail SMTP + App Password.
 * All sends are async (fire-and-forget) so the UI never blocks.
 *
 * Extended to support PDF attachments for the Evenement module (badge confirmation).
 */
public class EmailService {

    // ── Gmail credentials ─────────────────────────────────────────────────────
    private static final String FROM_EMAIL   = System.getenv("AUTOLEARN_EMAIL") != null 
        ? System.getenv("AUTOLEARN_EMAIL") 
        : "autolearn66@gmail.com";
    private static final String FROM_NAME    = "AutoLearn";
    private static final String APP_PASSWORD = System.getenv("AUTOLEARN_PASSWORD") != null 
        ? System.getenv("AUTOLEARN_PASSWORD") 
        : "nnna xrkp hrsv ynci";

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    // ── Module User — emails ──────────────────────────────────────────────────

    public static void sendRegistrationConfirmation(String toEmail, String prenom, String nom) {
        String subject = "Bienvenue sur AutoLearn !";
        String body = htmlTemplate(
            "Bienvenue, " + prenom + " !",
            "Votre compte a été créé avec succès.",
            "<p>Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
            "<p>Votre compte étudiant AutoLearn est maintenant actif. " +
            "Vous pouvez dès maintenant accéder à tous nos cours, challenges et événements.</p>" +
            "<p>Bonne formation !</p>",
            "Se connecter", "https://autolearn.tn/login"
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendAdminCreatedAccount(String toEmail, String prenom, String nom,
                                               String plainPassword) {
        String subject = "Votre compte AutoLearn a été créé";
        String body = htmlTemplate(
            "Votre compte a été créé",
            "Un administrateur AutoLearn a créé votre compte.",
            "<p>Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>" +
            "<p>Un administrateur a créé votre compte étudiant sur la plateforme AutoLearn.</p>" +
            "<table style='border-collapse:collapse;margin:16px 0;'>" +
            "  <tr><td style='padding:6px 16px 6px 0;color:#888;'>Email</td>" +
            "      <td style='padding:6px 0;font-weight:600;'>" + toEmail + "</td></tr>" +
            "  <tr><td style='padding:6px 16px 6px 0;color:#888;'>Mot de passe temporaire</td>" +
            "      <td style='padding:6px 0;font-weight:600;font-family:monospace;background:#f3f4f6;" +
            "          padding:4px 10px;border-radius:6px;'>" + plainPassword + "</td></tr>" +
            "</table>" +
            "<p style='color:#dc2626;font-size:13px;'>Veuillez changer votre mot de passe dès votre première connexion.</p>",
            "Se connecter", "https://autolearn.tn/login"
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendSuspensionNotification(String toEmail, String prenom, String reason) {
        String subject = "Votre compte AutoLearn a été suspendu";
        String body = htmlTemplate(
            "Compte suspendu",
            "Votre accès à AutoLearn a été temporairement suspendu.",
            "<p>Bonjour <strong>" + prenom + "</strong>,</p>" +
            "<p>Votre compte a été suspendu pour la raison suivante :</p>" +
            "<blockquote style='border-left:4px solid #dc2626;margin:16px 0;padding:12px 16px;" +
            "background:#fef2f2;border-radius:0 8px 8px 0;color:#991b1b;font-style:italic;'>" +
            reason + "</blockquote>" +
            "<p>Si vous pensez qu'il s'agit d'une erreur, contactez-nous à " +
            "<a href='mailto:autolearn66@gmail.com' style='color:#7a6ad8;'>autolearn66@gmail.com</a>.</p>",
            "Contacter le support", "mailto:autolearn66@gmail.com"
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendReactivationNotification(String toEmail, String prenom) {
        String subject = "Votre compte AutoLearn a été réactivé";
        String body = htmlTemplate(
            "Compte réactivé ✓",
            "Votre accès à AutoLearn a été rétabli.",
            "<p>Bonjour <strong>" + prenom + "</strong>,</p>" +
            "<p>Bonne nouvelle ! Votre compte a été réactivé. " +
            "Vous pouvez à nouveau accéder à tous vos cours et challenges.</p>" +
            "<p>Bienvenue de retour !</p>",
            "Se connecter", "https://autolearn.tn/login"
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendPasswordReset(String toEmail, String prenom, String resetToken) {
        String subject = "Réinitialisation de votre mot de passe AutoLearn";
        String body = htmlTemplate(
            "Réinitialisation du mot de passe",
            "Vous avez demandé à réinitialiser votre mot de passe.",
            "<p>Bonjour <strong>" + prenom + "</strong>,</p>" +
            "<p>Voici votre code de réinitialisation :</p>" +
            "<div style='text-align:center;margin:24px 0;'>" +
            "  <span style='font-size:32px;font-weight:900;letter-spacing:8px;" +
            "    font-family:monospace;background:#f3f4f6;padding:12px 24px;" +
            "    border-radius:12px;color:#7a6ad8;'>" + resetToken + "</span>" +
            "</div>" +
            "<p style='color:#888;font-size:13px;'>Ce code expire dans <strong>15 minutes</strong>. " +
            "Si vous n'avez pas fait cette demande, ignorez cet email.</p>",
            null, null
        );
        sendAsync(toEmail, subject, body);
    }

    public static void sendAsync_BreachedPasswordWarning(String toEmail, String prenom, int breachCount) {
        String subject = "Securite : votre mot de passe a ete detecte dans des fuites de donnees";
        String body = htmlTemplate(
            "Alerte de securite",
            "Votre mot de passe a ete trouve dans des bases de donnees compromises.",
            "<p>Bonjour <strong>" + prenom + "</strong>,</p>" +
            "<p>Lors de votre inscription, nous avons verifie votre mot de passe via le service " +
            "<strong>Have I Been Pwned</strong>.</p>" +
            "<p>Resultat : votre mot de passe a ete trouve dans <strong>" + breachCount +
            " fuite(s) de donnees</strong> connues.</p>" +
            "<p style='color:#dc2626;font-weight:bold;'>Nous vous recommandons fortement de changer votre mot de passe immediatement.</p>",
            "Changer mon mot de passe", "https://autolearn.tn/reset-password"
        );
        sendAsync(toEmail, subject, body);
    }

    // ── Module Evenement — confirmation de participation avec badge PDF ────────

    /**
     * Envoie l'email de confirmation de participation avec badge PDF en pièce jointe.
     * Utilise exactement le même SMTP Gmail que les autres emails AutoLearn.
     * Asynchrone — ne bloque pas l'UI JavaFX.
     *
     * @param toEmail      email du participant
     * @param subject      sujet de l'email
     * @param htmlBody     contenu HTML complet de l'email
     * @param pdfBytes     badge PDF en bytes (peut être null)
     * @param pdfFileName  nom du fichier PDF joint
     */
    public static void sendWithAttachmentAsync(String toEmail, String subject, String htmlBody,
                                                byte[] pdfBytes, String pdfFileName) {
        POOL.submit(() -> {
            try {
                sendWithAttachment(toEmail, subject, htmlBody, pdfBytes, pdfFileName);
                System.out.println("[Email] ✅ Envoyé (avec badge) à: " + toEmail);
            } catch (Exception e) {
                System.err.println("[Email] ❌ Échec envoi à " + toEmail + ": " + e.getMessage());
            }
        });
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private static void sendAsync(String to, String subject, String htmlBody) {
        POOL.submit(() -> {
            try {
                sendWithAttachment(to, subject, htmlBody, null, null);
                System.out.println("[Email] Sent to " + to + " — " + subject);
            } catch (Exception e) {
                System.err.println("[Email] Failed to send to " + to + ": " + e.getMessage());
            }
        });
    }

    /**
     * Méthode SMTP centrale — supporte HTML simple et HTML + pièce jointe PDF.
     */
    private static void sendWithAttachment(String to, String subject, String htmlBody,
                                            byte[] pdfBytes, String pdfFileName)
            throws MessagingException {

        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);

        if (pdfBytes != null && pdfBytes.length > 0) {
            // Email multipart : HTML + pièce jointe PDF
            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            MimeBodyPart pdfPart = new MimeBodyPart();
            pdfPart.setContent(pdfBytes, "application/pdf");
            pdfPart.setFileName(pdfFileName != null ? pdfFileName : "badge_autolearn.pdf");
            multipart.addBodyPart(pdfPart);

            msg.setContent(multipart);
        } else {
            msg.setContent(htmlBody, "text/html; charset=UTF-8");
        }

        Transport.send(msg);
    }

    // ── Template HTML AutoLearn ───────────────────────────────────────────────

    public static String htmlTemplate(String title, String subtitle,
                                       String content, String btnText, String btnHref) {
        String btn = (btnText != null && btnHref != null)
            ? "<div style='text-align:center;margin:28px 0 8px;'>" +
              "  <a href='" + btnHref + "' style='background:#7a6ad8;color:white;text-decoration:none;" +
              "    font-weight:700;font-size:15px;padding:14px 36px;border-radius:8px;" +
              "    display:inline-block;'>" + btnText + "</a>" +
              "</div>"
            : "";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body " +
               "style='margin:0;padding:0;background:#f8f7ff;font-family:Arial,sans-serif;'>" +
               "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f8f7ff;padding:40px 0;'>" +
               "<tr><td align='center'>" +
               "<table width='560' cellpadding='0' cellspacing='0' " +
               "style='background:white;border-radius:16px;overflow:hidden;" +
               "box-shadow:0 4px 24px rgba(122,106,216,0.12);'>" +
               "<tr><td style='background:#7a6ad8;padding:32px 40px;text-align:center;'>" +
               "  <span style='font-size:24px;font-weight:900;color:white;'>AutoLearn</span><br>" +
               "  <span style='font-size:13px;color:rgba(255,255,255,0.7);'>Votre plateforme d'apprentissage</span>" +
               "</td></tr>" +
               "<tr><td style='padding:36px 40px 28px;'>" +
               "  <h2 style='margin:0 0 6px;font-size:22px;color:#1a1a2e;'>" + title + "</h2>" +
               "  <p style='margin:0 0 20px;font-size:14px;color:#888;'>" + subtitle + "</p>" +
               "  <div style='font-size:14px;color:#444;line-height:1.7;'>" + content + "</div>" +
               btn +
               "</td></tr>" +
               "<tr><td style='background:#f8f7ff;padding:20px 40px;text-align:center;" +
               "border-top:1px solid #eeeeee;'>" +
               "  <p style='margin:0;font-size:12px;color:#aaa;'>" +
               "    © 2026 AutoLearn — Tunisie — " +
               "    <a href='mailto:autolearn66@gmail.com' style='color:#7a6ad8;'>autolearn66@gmail.com</a>" +
               "  </p>" +
               "</td></tr>" +
               "</table></td></tr></table></body></html>";
    }
}
