package tn.esprit.services;

import tn.esprit.entities.Equipe;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service orchestrateur pour la confirmation de participation.
 * Après acceptation d'une participation, ce service :
 *   1. Récupère la météo du jour J via WeatherService (OpenWeatherMap API)
 *   2. Génère un QR code via QrCodeService (ZXing API)
 *   3. Génère un badge PDF via BadgePdfService (iText)
 *   4. Envoie un email de confirmation via BrevoEmailService (Brevo API)
 *
 * L'envoi est asynchrone pour ne pas bloquer l'UI JavaFX.
 */
public class ParticipationConfirmationService {

    private final WeatherService weatherService       = new WeatherService();
    private final QrCodeService qrCodeService         = new QrCodeService();
    private final BadgePdfService badgePdfService     = new BadgePdfService();
    private final BrevoEmailService brevoEmailService = new BrevoEmailService();
    private final EquipeService equipeService         = new EquipeService();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);
    private static final DateTimeFormatter DATE_SHORT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    /**
     * Envoie les emails de confirmation à tous les membres de l'équipe.
     * Exécuté en arrière-plan pour ne pas bloquer l'UI.
     *
     * @param equipe          l'équipe participante
     * @param evenement       l'événement
     * @param participationId ID de la participation créée
     */
    public void sendConfirmationToTeam(Equipe equipe, Evenement evenement, int participationId) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "email-confirmation-thread");
            t.setDaemon(true);
            return t;
        });
        executor.submit(() -> {
            try {
                List<Etudiant> membres = equipeService.getEtudiantsByEquipe(equipe.getId());
                if (membres.isEmpty()) return;

                // 1. Météo du jour J
                Map<String, Object> weather = null;
                if (evenement.getLieu() != null && !evenement.getLieu().isBlank()
                        && evenement.getDateDebut() != null) {
                    weather = weatherService.getWeatherForEvent(
                            evenement.getLieu() + ",TN", evenement.getDateDebut());
                }

                // 2. QR code (un seul pour toute l'équipe, lié à la participation)
                byte[] qrBytes = qrCodeService.generateParticipationQrCode(
                        participationId, membres.get(0).getId(), evenement.getId());

                // 3. Envoyer un email personnalisé à chaque membre
                for (Etudiant etudiant : membres) {
                    try {
                        // Badge PDF personnalisé par membre
                        byte[] badgePdf = badgePdfService.generateBadge(
                                etudiant.getPrenom() + " " + etudiant.getNom(),
                                equipe.getNom(),
                                evenement.getTitre(),
                                evenement.getType(),
                                evenement.getDateDebut(),
                                evenement.getLieu(),
                                participationId,
                                qrBytes
                        );

                        String htmlEmail = buildEmailHtml(etudiant, equipe, evenement,
                                participationId, weather, qrBytes);

                        String badgeFileName = "badge_" + evenement.getTitre()
                                .replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

                        brevoEmailService.sendEmail(
                                etudiant.getEmail(),
                                etudiant.getPrenom() + " " + etudiant.getNom(),
                                "🎉 Confirmation de participation — " + evenement.getTitre(),
                                htmlEmail,
                                badgePdf,
                                badgeFileName
                        );
                    } catch (Exception e) {
                        System.err.println("Erreur envoi email à " + etudiant.getEmail() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur service confirmation: " + e.getMessage());
            }
        });
        executor.shutdown();
    }

    /**
     * Construit le HTML de l'email de confirmation.
     */
    private String buildEmailHtml(Etudiant etudiant, Equipe equipe, Evenement evenement,
                                   int participationId, Map<String, Object> weather, byte[] qrBytes) {

        String dateStr = evenement.getDateDebut() != null
                ? evenement.getDateDebut().format(DATE_FMT) : "Date à confirmer";
        String dateShort = evenement.getDateDebut() != null
                ? evenement.getDateDebut().format(DATE_SHORT) : "";
        String lieu = evenement.getLieu() != null ? evenement.getLieu() : "Lieu à confirmer";
        String type = evenement.getType() != null ? evenement.getType() : "Événement";
        String qrUrl = qrCodeService.getParticipationUrl(
                participationId, etudiant.getId(), evenement.getId());

        // ── Bloc météo ────────────────────────────────────────────────────────
        String weatherBlock = buildWeatherBlock(weather, evenement.getDateDebut());

        // ── Conseil selon météo ───────────────────────────────────────────────
        String weatherTip = buildWeatherTip(weather);

        // ── Badge QR code en base64 pour l'email ─────────────────────────────
        String qrImgTag = "";
        if (qrBytes != null) {
            String qrBase64 = java.util.Base64.getEncoder().encodeToString(qrBytes);
            qrImgTag = "<img src=\"data:image/png;base64," + qrBase64
                    + "\" alt=\"QR Code\" style=\"width:120px;height:120px;display:block;margin:0 auto;\" />";
        }

        // ── Type badge color ──────────────────────────────────────────────────
        String typeColor = switch (type.toLowerCase()) {
            case "hackathon"   -> "#7a6ad8";
            case "conference"  -> "#059669";
            case "workshop"    -> "#f59e0b";
            default            -> "#6b7280";
        };

        return """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Confirmation de participation</title>
</head>
<body style="margin:0;padding:0;background-color:#f0f0f7;font-family:'Segoe UI',Arial,sans-serif;">

  <!-- Wrapper -->
  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f0f0f7;padding:32px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0"
             style="background:#ffffff;border-radius:20px;overflow:hidden;
                    box-shadow:0 8px 40px rgba(122,106,216,0.13);">

        <!-- ── Header ── -->
        <tr>
          <td style="background:linear-gradient(135deg,#7a6ad8 0%%,#5b4fc4 100%%);
                     padding:40px 40px 32px;text-align:center;">
            <div style="font-size:13px;font-weight:700;color:rgba(255,255,255,0.7);
                        letter-spacing:3px;text-transform:uppercase;margin-bottom:12px;">
              AutoLearn Platform
            </div>
            <div style="font-size:42px;margin-bottom:12px;">🎉</div>
            <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:800;line-height:1.3;">
              Participation Confirmée !
            </h1>
            <p style="margin:10px 0 0;color:rgba(255,255,255,0.85);font-size:15px;">
              Votre place est réservée pour cet événement
            </p>
          </td>
        </tr>

        <!-- ── Salutation ── -->
        <tr>
          <td style="padding:32px 40px 0;">
            <p style="margin:0;font-size:16px;color:#1e1e1e;line-height:1.6;">
              Bonjour <strong style="color:#7a6ad8;">%s</strong>,
            </p>
            <p style="margin:12px 0 0;font-size:15px;color:#4b5563;line-height:1.7;">
              Félicitations ! Votre participation à l'événement ci-dessous a été
              <strong style="color:#059669;">officiellement acceptée</strong>.
              Retrouvez tous les détails et votre badge officiel en pièce jointe.
            </p>
          </td>
        </tr>

        <!-- ── Carte événement ── -->
        <tr>
          <td style="padding:24px 40px 0;">
            <div style="background:#f5f3ff;border-radius:16px;padding:24px;
                        border-left:5px solid #7a6ad8;">
              <div style="display:inline-block;background:%s;color:#fff;
                          font-size:11px;font-weight:700;letter-spacing:1px;
                          padding:4px 14px;border-radius:20px;margin-bottom:14px;">
                %s
              </div>
              <h2 style="margin:0 0 16px;font-size:20px;color:#1e1e1e;font-weight:800;">
                %s
              </h2>
              <table cellpadding="0" cellspacing="0" width="100%%">
                <tr>
                  <td style="padding:6px 0;">
                    <span style="font-size:18px;">📅</span>
                    <span style="font-size:14px;color:#374151;margin-left:8px;">
                      <strong>Date :</strong> %s
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style="padding:6px 0;">
                    <span style="font-size:18px;">📍</span>
                    <span style="font-size:14px;color:#374151;margin-left:8px;">
                      <strong>Lieu :</strong> %s
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style="padding:6px 0;">
                    <span style="font-size:18px;">👥</span>
                    <span style="font-size:14px;color:#374151;margin-left:8px;">
                      <strong>Équipe :</strong> %s
                    </span>
                  </td>
                </tr>
                <tr>
                  <td style="padding:6px 0;">
                    <span style="font-size:18px;">🎫</span>
                    <span style="font-size:14px;color:#374151;margin-left:8px;">
                      <strong>Badge N° :</strong>
                      <span style="color:#7a6ad8;font-weight:700;">#%05d</span>
                    </span>
                  </td>
                </tr>
              </table>
            </div>
          </td>
        </tr>

        <!-- ── Météo ── -->
        %s

        <!-- ── Conseil météo ── -->
        %s

        <!-- ── QR Code ── -->
        <tr>
          <td style="padding:24px 40px 0;">
            <div style="background:#1e1e1e;border-radius:16px;padding:28px;text-align:center;">
              <h3 style="margin:0 0 6px;color:#ffffff;font-size:16px;font-weight:700;">
                🔍 Votre QR Code de Participation
              </h3>
              <p style="margin:0 0 20px;color:#9ca3af;font-size:13px;">
                Présentez ce code à l'entrée de l'événement
              </p>
              <div style="background:#ffffff;border-radius:12px;padding:16px;
                          display:inline-block;margin:0 auto;">
                %s
              </div>
              <p style="margin:16px 0 0;font-size:12px;color:#6b7280;">
                Ou accédez directement à votre espace participant :<br/>
                <a href="%s" style="color:#7a6ad8;font-weight:600;word-break:break-all;">
                  %s
                </a>
              </p>
            </div>
          </td>
        </tr>

        <!-- ── Badge info ── -->
        <tr>
          <td style="padding:24px 40px 0;">
            <div style="background:#ecfdf5;border-radius:12px;padding:18px;
                        border:1px solid #a7f3d0;display:flex;align-items:center;">
              <span style="font-size:24px;margin-right:12px;">📎</span>
              <div>
                <p style="margin:0;font-size:14px;font-weight:700;color:#065f46;">
                  Votre badge officiel est en pièce jointe
                </p>
                <p style="margin:4px 0 0;font-size:13px;color:#047857;">
                  Imprimez-le et portez-le le jour de l'événement.
                  Il contient votre QR code d'accès.
                </p>
              </div>
            </div>
          </td>
        </tr>

        <!-- ── Footer ── -->
        <tr>
          <td style="padding:32px 40px 40px;">
            <hr style="border:none;border-top:1px solid #e5e7eb;margin:0 0 24px;" />
            <p style="margin:0;font-size:13px;color:#9ca3af;text-align:center;line-height:1.8;">
              Cet email a été envoyé automatiquement par la plateforme
              <strong style="color:#7a6ad8;">AutoLearn</strong>.<br/>
              Pour toute question, contactez l'administration de votre établissement.<br/>
              <span style="font-size:11px;">© 2025 AutoLearn — Tous droits réservés</span>
            </p>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>

</body>
</html>
""".formatted(
                etudiant.getPrenom() + " " + etudiant.getNom(),  // salutation
                typeColor, type.toUpperCase(),                    // badge type
                evenement.getTitre(),                             // titre événement
                dateStr,                                          // date
                lieu,                                             // lieu
                equipe.getNom(),                                  // équipe
                participationId,                                  // badge numéro
                weatherBlock,                                     // bloc météo
                weatherTip,                                       // conseil météo
                qrImgTag,                                         // QR code image
                qrUrl, qrUrl                                      // lien QR
        );
    }

    /**
     * Construit le bloc HTML de la météo.
     */
    private String buildWeatherBlock(Map<String, Object> weather, LocalDateTime eventDate) {
        if (weather == null || !Boolean.TRUE.equals(weather.get("available"))) {
            return ""; // Pas de météo disponible
        }

        boolean isForecast = Boolean.TRUE.equals(weather.get("is_forecast"));
        int temp = weather.get("temperature") instanceof Number n ? n.intValue() : 0;
        int feelsLike = weather.get("feels_like") instanceof Number n ? n.intValue() : 0;
        String desc = (String) weather.getOrDefault("description", "");
        String icon = (String) weather.getOrDefault("icon", "");
        int humidity = weather.get("humidity") instanceof Number n ? n.intValue() : 0;
        Object windObj = weather.get("wind_speed");
        String wind = windObj != null ? windObj.toString() : "0";
        String city = (String) weather.getOrDefault("city", "");

        String emoji = getWeatherEmoji(icon);
        String forecastLabel = isForecast ? "Prévision météo" : "Météo actuelle (référence)";
        String dateLabel = eventDate != null ? " — " + eventDate.format(DATE_SHORT) : "";

        // Couleur de fond selon température
        String bgColor = temp >= 30 ? "#fff7ed" : temp >= 20 ? "#f0fdf4" : temp >= 10 ? "#eff6ff" : "#f0f9ff";
        String borderColor = temp >= 30 ? "#fed7aa" : temp >= 20 ? "#bbf7d0" : temp >= 10 ? "#bfdbfe" : "#bae6fd";
        String tempColor = temp >= 30 ? "#ea580c" : temp >= 20 ? "#16a34a" : temp >= 10 ? "#2563eb" : "#0284c7";

        return """
        <tr>
          <td style="padding:24px 40px 0;">
            <div style="background:%s;border-radius:16px;padding:22px;border:1px solid %s;">
              <div style="font-size:12px;font-weight:700;color:#6b7280;letter-spacing:1px;
                          text-transform:uppercase;margin-bottom:12px;">
                🌤 %s%s
              </div>
              <div style="display:flex;align-items:center;gap:16px;">
                <div style="font-size:48px;line-height:1;">%s</div>
                <div>
                  <div style="font-size:36px;font-weight:800;color:%s;line-height:1;">
                    %d°C
                  </div>
                  <div style="font-size:13px;color:#374151;margin-top:4px;">%s</div>
                  <div style="font-size:12px;color:#6b7280;margin-top:2px;">
                    Ressenti %d°C &nbsp;·&nbsp; Humidité %d%% &nbsp;·&nbsp; Vent %s km/h
                  </div>
                </div>
              </div>
              %s
            </div>
          </td>
        </tr>
        """.formatted(bgColor, borderColor, forecastLabel, dateLabel,
                emoji, tempColor, temp, desc, feelsLike, humidity, wind,
                city.isBlank() ? "" : "<div style=\"font-size:12px;color:#9ca3af;margin-top:8px;\">📍 " + city + "</div>");
    }

    /**
     * Construit le conseil météo selon les conditions.
     */
    private String buildWeatherTip(Map<String, Object> weather) {
        if (weather == null || !Boolean.TRUE.equals(weather.get("available"))) {
            return """
            <tr>
              <td style="padding:16px 40px 0;">
                <div style="background:#fef3c7;border-radius:12px;padding:16px;border:1px solid #fde68a;">
                  <p style="margin:0;font-size:14px;color:#92400e;">
                    💡 <strong>Conseil :</strong> Pensez à vérifier la météo la veille de l'événement
                    et à vous habiller en conséquence. Bonne chance à toute votre équipe ! 🚀
                  </p>
                </div>
              </td>
            </tr>
            """;
        }

        String icon = (String) weather.getOrDefault("icon", "");
        int temp = weather.get("temperature") instanceof Number n ? n.intValue() : 20;
        String tip;

        if (icon.startsWith("11")) {
            tip = "⛈️ <strong>Attention :</strong> Orages prévus ! Prévoyez un imperméable et partez plus tôt pour éviter les embouteillages.";
        } else if (icon.startsWith("09") || icon.startsWith("10")) {
            tip = "🌧️ <strong>Pluie prévue :</strong> N'oubliez pas votre parapluie ! Portez des chaussures imperméables et prévoyez un peu plus de temps pour le trajet.";
        } else if (icon.startsWith("13")) {
            tip = "❄️ <strong>Neige prévue :</strong> Habillez-vous chaudement et soyez prudent sur la route. Prévoyez des vêtements en couches.";
        } else if (icon.startsWith("50")) {
            tip = "🌫️ <strong>Brouillard :</strong> Visibilité réduite, soyez prudent sur la route et partez plus tôt que prévu.";
        } else if (temp >= 35) {
            tip = "🥵 <strong>Forte chaleur :</strong> Hydratez-vous bien, portez des vêtements légers et clairs, et n'oubliez pas votre crème solaire !";
        } else if (temp >= 28) {
            tip = "☀️ <strong>Beau temps chaud :</strong> Parfait pour l'événement ! Pensez à emporter de l'eau et à vous protéger du soleil.";
        } else if (temp >= 18) {
            tip = "🌤️ <strong>Météo idéale :</strong> Conditions parfaites pour votre événement. Profitez-en pleinement avec votre équipe !";
        } else if (temp >= 10) {
            tip = "🧥 <strong>Temps frais :</strong> Prévoyez une veste ou un pull. Les températures sont agréables mais un peu fraîches.";
        } else {
            tip = "🥶 <strong>Temps froid :</strong> Couvrez-vous bien ! Portez des vêtements chauds et n'oubliez pas votre écharpe.";
        }

        return """
        <tr>
          <td style="padding:16px 40px 0;">
            <div style="background:#fef3c7;border-radius:12px;padding:16px;border:1px solid #fde68a;">
              <p style="margin:0;font-size:14px;color:#92400e;line-height:1.6;">
                💡 <strong>Conseil AutoLearn :</strong> %s
              </p>
            </div>
          </td>
        </tr>
        """.formatted(tip);
    }

    private String getWeatherEmoji(String icon) {
        if (icon == null) return "🌤️";
        return switch (icon) {
            case "01d" -> "☀️";
            case "01n" -> "🌙";
            case "02d", "02n" -> "⛅";
            case "03d", "03n", "04d", "04n" -> "☁️";
            case "09d", "09n" -> "🌧️";
            case "10d" -> "🌦️";
            case "10n" -> "🌧️";
            case "11d", "11n" -> "⛈️";
            case "13d", "13n" -> "❄️";
            case "50d", "50n" -> "🌫️";
            default -> "🌤️";
        };
    }
}
