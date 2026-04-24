package tn.esprit.services;

import tn.esprit.entities.User;
import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = "autolearn66@gmail.com";
    private static final String FROM_PASSWORD = "ehoptwntzqwhdvwj";

    /**
     * Envoie un email avec les résultats du challenge à l'utilisateur connecté
     */
    public void sendChallengeResult(User user, String challengeTitle, int score, int totalPoints, LocalDateTime completedAt) {

        String toEmail = user.getEmail();
        String userName = user.getPrenom() + " " + user.getNom();

        String subject = "📊 Résultat de votre challenge - " + challengeTitle;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDate = completedAt.format(formatter);

        int percentage = totalPoints > 0 ? (score * 100) / totalPoints : 0;
        String status = percentage >= 50 ? "✅ Félicitations ! Vous avez réussi !" : "❌ Vous n'avez pas atteint le seuil de réussite.";

        String stars = getStars(percentage);

        StringBuilder body = new StringBuilder();
        body.append("<html>\n");
        body.append("<head>\n");
        body.append("<style>\n");
        body.append("    body { font-family: 'Segoe UI', Arial, sans-serif; background-color: #f5f5f5; padding: 20px; }\n");
        body.append("    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 20px; padding: 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }\n");
        body.append("    .header { text-align: center; border-bottom: 3px solid #7a6ad8; padding-bottom: 20px; margin-bottom: 20px; }\n");
        body.append("    .logo { font-size: 24px; font-weight: bold; color: #7a6ad8; }\n");
        body.append("    .score-circle { width: 120px; height: 120px; margin: 20px auto; border-radius: 50%; background: linear-gradient(135deg, #7a6ad8, #4e3b9c); display: flex; align-items: center; justify-content: center; }\n");
        body.append("    .score-number { font-size: 36px; font-weight: bold; color: white; }\n");
        body.append("    .score-total { font-size: 18px; color: rgba(255,255,255,0.8); }\n");
        body.append("    .stars { text-align: center; margin: 15px 0; }\n");
        body.append("    .star { font-size: 24px; }\n");
        body.append("    .details { background-color: #f8f9fa; padding: 20px; border-radius: 15px; margin: 20px 0; }\n");
        body.append("    .detail-row { margin: 12px 0; }\n");
        body.append("    .status-success { color: #28a745; font-weight: bold; }\n");
        body.append("    .status-fail { color: #dc3545; font-weight: bold; }\n");
        body.append("    .footer { text-align: center; color: #999; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; }\n");
        body.append("</style>\n");
        body.append("</head>\n");
        body.append("<body>\n");
        body.append("    <div class=\"container\">\n");
        body.append("        <div class=\"header\">\n");
        body.append("            <div class=\"logo\">🎓 AutoLearn</div>\n");
        body.append("            <h3>Résultat du challenge</h3>\n");
        body.append("        </div>\n");
        body.append("        <div class=\"score-circle\">\n");
        body.append("            <div>\n");
        body.append("                <div class=\"score-number\">" + score + "</div>\n");
        body.append("                <div class=\"score-total\">/" + totalPoints + "</div>\n");
        body.append("            </div>\n");
        body.append("        </div>\n");
        body.append("        <div class=\"stars\">\n");
        body.append("            " + stars + "\n");
        body.append("        </div>\n");
        body.append("        <div class=\"details\">\n");
        body.append("            <div class=\"detail-row\"><strong>👤 Étudiant :</strong> " + userName + "</div>\n");
        body.append("            <div class=\"detail-row\"><strong>🏆 Challenge :</strong> " + challengeTitle + "</div>\n");
        body.append("            <div class=\"detail-row\"><strong>📊 Pourcentage :</strong> " + percentage + "%</div>\n");
        body.append("            <div class=\"detail-row\"><strong>📅 Date de completion :</strong> " + formattedDate + "</div>\n");
        body.append("            <div class=\"detail-row\"><strong>✨ Statut :</strong> <span class='" + (percentage >= 50 ? "status-success" : "status-fail") + "'>" + status + "</span></div>\n");
        body.append("        </div>\n");
        body.append("        <div class=\"footer\">\n");
        body.append("            <p>AutoLearn - Plateforme d'apprentissage intelligent</p>\n");
        body.append("            <p>© 2026 AutoLearn. Tous droits réservés.</p>\n");
        body.append("        </div>\n");
        body.append("    </div>\n");
        body.append("</body>\n");
        body.append("</html>");

        sendEmail(toEmail, subject, body.toString());
        System.out.println("Email envoyé à : " + toEmail);
    }

    private String getStars(int percentage) {
        int starCount = percentage / 20;
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= starCount) {
                stars.append("<span style='color:#f1c40f; font-size:24px;'>★</span>");
            } else {
                stars.append("<span style='color:#ddd; font-size:24px;'>☆</span>");
            }
        }
        return stars.toString();
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=utf-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé avec succès à " + to);
        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}