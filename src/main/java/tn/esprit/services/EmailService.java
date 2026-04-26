package tn.esprit.services;

import tn.esprit.entities.User;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailService {

    // Configuration Brevo
    private static final String BREVO_API_KEY = "Brevo_API_KEY";
    private static final String FROM_EMAIL = "autolearn66@gmail.com";
    private static final String FROM_NAME = "AutoLearn";
    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendChallengeResult(User user, String challengeTitle, int score, int totalPoints, LocalDateTime completedAt) {

        String toEmail = user.getEmail();
        String userName = user.getPrenom() + " " + user.getNom();

        String subject = "📊 Résultat de votre challenge - " + challengeTitle;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String formattedDate = completedAt.format(formatter);

        int percentage = totalPoints > 0 ? (score * 100) / totalPoints : 0;
        String status = percentage >= 50 ? "✅ Félicitations ! Vous avez réussi !" : "❌ Vous n'avez pas atteint le seuil de réussite.";
        String stars = getStars(percentage);

        String htmlBody = buildHtmlBody(userName, challengeTitle, score, totalPoints, percentage, formattedDate, status, stars);

        sendEmail(toEmail, subject, htmlBody);
        System.out.println("Email envoyé à : " + toEmail);
    }

    private String buildHtmlBody(String userName, String challengeTitle, int score, int totalPoints,
                                 int percentage, String formattedDate, String status, String stars) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><style>");
        sb.append("body{font-family:'Segoe UI',Arial,sans-serif;background-color:#f5f5f5;padding:20px;}");
        sb.append(".container{max-width:600px;margin:0 auto;background-color:white;border-radius:20px;padding:30px;box-shadow:0 4px 20px rgba(0,0,0,0.1);}");
        sb.append(".header{text-align:center;border-bottom:3px solid #7a6ad8;padding-bottom:20px;margin-bottom:20px;}");
        sb.append(".logo{font-size:24px;font-weight:bold;color:#7a6ad8;}");
        sb.append(".score-circle{width:120px;height:120px;margin:20px auto;border-radius:50%;background:linear-gradient(135deg,#7a6ad8,#4e3b9c);display:flex;align-items:center;justify-content:center;}");
        sb.append(".score-number{font-size:36px;font-weight:bold;color:white;}");
        sb.append(".score-total{font-size:18px;color:rgba(255,255,255,0.8);}");
        sb.append(".stars{text-align:center;margin:15px 0;font-size:24px;}");
        sb.append(".details{background-color:#f8f9fa;padding:20px;border-radius:15px;margin:20px 0;}");
        sb.append(".detail-row{margin:12px 0;}");
        sb.append(".status-success{color:#28a745;font-weight:bold;}");
        sb.append(".status-fail{color:#dc3545;font-weight:bold;}");
        sb.append(".footer{text-align:center;color:#999;font-size:12px;margin-top:30px;padding-top:20px;border-top:1px solid #eee;}");
        sb.append("</style></head><body>");
        sb.append("<div class='container'>");
        sb.append("<div class='header'><div class='logo'>🎓 AutoLearn</div><h3>Résultat du challenge</h3></div>");
        sb.append("<div class='score-circle'><div><div class='score-number'>").append(score).append("</div>");
        sb.append("<div class='score-total'>/").append(totalPoints).append("</div></div></div>");
        sb.append("<div class='stars'>").append(stars).append("</div>");
        sb.append("<div class='details'>");
        sb.append("<div class='detail-row'><strong>👤 Étudiant :</strong> ").append(userName).append("</div>");
        sb.append("<div class='detail-row'><strong>🏆 Challenge :</strong> ").append(challengeTitle).append("</div>");
        sb.append("<div class='detail-row'><strong>📊 Pourcentage :</strong> ").append(percentage).append("%</div>");
        sb.append("<div class='detail-row'><strong>📅 Date de completion :</strong> ").append(formattedDate).append("</div>");
        sb.append("<div class='detail-row'><strong>✨ Statut :</strong> <span class='").append(percentage >= 50 ? "status-success" : "status-fail").append("'>").append(status).append("</span></div>");
        sb.append("</div><div class='footer'><p>AutoLearn - Plateforme d'apprentissage intelligent</p>");
        sb.append("<p>© 2026 AutoLearn. Tous droits réservés.</p></div></div></body></html>");

        return sb.toString();
    }

    private String getStars(int percentage) {
        int starCount = percentage / 20;
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= starCount ? "★" : "☆");
        }
        return stars.toString();
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            String jsonBody = "{\n" +
                    "    \"sender\": {\"email\": \"" + FROM_EMAIL + "\", \"name\": \"" + FROM_NAME + "\"},\n" +
                    "    \"to\": [{\"email\": \"" + to + "\"}],\n" +
                    "    \"subject\": \"" + subject + "\",\n" +
                    "    \"htmlContent\": " + escapeJson(htmlBody) + "\n" +
                    "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("api-key", BREVO_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                System.out.println("✅ Email envoyé avec succès à " + to);
            } else {
                System.err.println("❌ Erreur Brevo: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") + "\"";
    }
}