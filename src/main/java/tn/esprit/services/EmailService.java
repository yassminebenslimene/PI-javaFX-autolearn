package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends HTML emails from autolearn66@gmail.com using Gmail SMTP + App Password.
 * All sends are async (fire-and-forget) so the UI never blocks.
 */
public class EmailService {

    // ── Gmail credentials ─────────────────────────────────────────────────────
    private static final String FROM_EMAIL    = "autolearn66@gmail.com";
    private static final String FROM_NAME     = "AutoLearn";
    // Generate an App Password at https://myaccount.google.com/apppasswords
    // (2-Step Verification must be enabled on the account)
    private static final String APP_PASSWORD  = "iqxb xtqb iqxb xtqb";   // ← replace with real app password

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "email-sender");
        t.setDaemon(true);
        return t;
    });

    // ── Public API ────────────────────────────────────────────────────────────

    /** 1. Confirmation email after self-registration */
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

    /** 2. Email sent to a new student created by an admin */
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

    /** 3. Suspension notification */
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

    /** 4. Reactivation notification */
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

    /** 5. Password reset email with a token/link */
    public static void sendPasswordReset(String toEmail, String prenom, String resetToken) {
        String subject = "Réinitialisation de votre mot de passe AutoLearn";
        // In a real app this would be a deep-link; for desktop we show the token
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

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static void sendAsync(String to, String subject, String htmlBody) {
        POOL.submit(() -> {
            try {
                send(to, subject, htmlBody);
                System.out.println("[Email] Sent to " + to + " — " + subject);
            } catch (Exception e) {
                System.err.println("[Email] Failed to send to " + to + ": " + e.getMessage());
            }
        });
    }

    private static void send(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject);
        msg.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(msg);
    }

    /**
     * Builds a clean HTML email template matching the AutoLearn violet brand.
     *
     * @param title    Big heading inside the card
     * @param subtitle Smaller subtitle line
     * @param content  Raw HTML body content
     * @param btnText  CTA button text (null = no button)
     * @param btnHref  CTA button href (null = no button)
     */
    private static String htmlTemplate(String title, String subtitle,
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
               // Header
               "<tr><td style='background:#7a6ad8;padding:32px 40px;text-align:center;'>" +
               "  <span style='font-size:24px;font-weight:900;color:white;'>AutoLearn</span><br>" +
               "  <span style='font-size:13px;color:rgba(255,255,255,0.7);'>Votre plateforme d'apprentissage</span>" +
               "</td></tr>" +
               // Body
               "<tr><td style='padding:36px 40px 28px;'>" +
               "  <h2 style='margin:0 0 6px;font-size:22px;color:#1a1a2e;'>" + title + "</h2>" +
               "  <p style='margin:0 0 20px;font-size:14px;color:#888;'>" + subtitle + "</p>" +
               "  <div style='font-size:14px;color:#444;line-height:1.7;'>" + content + "</div>" +
               btn +
               "</td></tr>" +
               // Footer
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
