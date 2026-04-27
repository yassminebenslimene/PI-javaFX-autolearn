package tn.esprit.services;

import tn.esprit.entities.Equipe;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service orchestrateur pour la confirmation de participation.
 * Envoie un email professionnel avec :
 *   - Template HTML desktop full-width
 *   - Météo via OpenWeatherMap (ville extraite du lieu ou Tunis par défaut)
 *   - QR code en pièce jointe inline (cid:) — compatible Gmail
 *   - Badge PDF en pièce jointe
 *   - Lien vers page de détails encodée en base64 (accessible sans serveur local)
 */
public class ParticipationConfirmationService {

    private final WeatherService weatherService   = new WeatherService();
    private final QrCodeService qrCodeService     = new QrCodeService();
    private final BadgePdfService badgePdfService = new BadgePdfService();
    private final EquipeService equipeService     = new EquipeService();
    private final PlanningPdfService planningPdfService = new PlanningPdfService();
    private final EventPlanningService eventPlanningService = new EventPlanningService();

    private static final String FROM_EMAIL   = System.getenv("AUTOLEARN_EMAIL") != null 
        ? System.getenv("AUTOLEARN_EMAIL") 
        : "autolearn66@gmail.com";
    private static final String APP_PASSWORD = System.getenv("AUTOLEARN_PASSWORD") != null 
        ? System.getenv("AUTOLEARN_PASSWORD") 
        : "nnna xrkp hrsv ynci";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);
    private static final DateTimeFormatter DATE_SHORT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);

    public void sendConfirmationToTeam(Equipe equipe, Evenement evenement, int participationId) {
        sendConfirmationToTeam(equipe, evenement, participationId, null);
    }

    /**
     * Envoie la confirmation avec le planning PDF en pièce jointe (optionnel).
     * @param planningJson JSON du planning généré par l'IA (peut être null)
     */
    public void sendConfirmationToTeam(Equipe equipe, Evenement evenement, int participationId, String planningJson) {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "email-confirmation-thread");
            t.setDaemon(true);
            return t;
        });
        executor.submit(() -> {
            try {
                List<Etudiant> membres = equipeService.getEtudiantsByEquipe(equipe.getId());
                if (membres.isEmpty()) {
                    System.err.println("[Email] Aucun membre trouvé pour l'équipe " + equipe.getId());
                    return;
                }

                // 1. Météo
                String ville = extractVille(evenement.getLieu());
                Map<String, Object> weather = null;
                if (evenement.getDateDebut() != null) {
                    weather = weatherService.getWeatherForEvent(ville, evenement.getDateDebut());
                    System.out.println("[Email] Météo pour '" + ville + "': available=" + weather.get("available")
                            + (weather.get("error") != null ? " error=" + weather.get("message") : ""));
                }

                // 2. QR code PNG
                byte[] qrBytes = qrCodeService.generateParticipationQrCode(
                        participationId, membres.get(0).getId(), evenement.getId());

                // 3. Planning PDF (généré à la volée si pas fourni)
                String planningJsonFinal = planningJson;
                if (planningJsonFinal == null || planningJsonFinal.isBlank()) {
                    // Générer un planning par défaut basé sur les infos de l'événement
                    try {
                        planningJsonFinal = eventPlanningService.generatePlanning(
                                evenement.getTitre(),
                                evenement.getType() != null ? evenement.getType() : "Conference",
                                evenement.getDateDebut(),
                                evenement.getDateFin() != null ? evenement.getDateFin() : evenement.getDateDebut().plusHours(8),
                                evenement.getNbMax() * 5
                        );
                    } catch (Exception e) {
                        System.err.println("[Email] Impossible de générer le planning: " + e.getMessage());
                    }
                }

                byte[] planningPdf = null;
                String planningFileName = null;
                if (planningJsonFinal != null && !planningJsonFinal.isBlank()) {
                    try {
                        planningPdf = planningPdfService.generatePlanningPdf(
                                evenement.getTitre(),
                                evenement.getType() != null ? evenement.getType() : "Événement",
                                evenement.getDateDebut(),
                                evenement.getDateFin() != null ? evenement.getDateFin() : evenement.getDateDebut().plusHours(8),
                                planningJsonFinal
                        );
                        planningFileName = "planning_" + evenement.getTitre()
                                .replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";
                    } catch (Exception e) {
                        System.err.println("[Email] Erreur génération planning PDF: " + e.getMessage());
                    }
                }

                // 4. Email personnalisé pour chaque membre
                for (Etudiant etudiant : membres) {
                    try {
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
                                participationId, weather, planningPdf != null);

                        String badgeFileName = "badge_" + evenement.getTitre()
                                .replaceAll("[^a-zA-Z0-9]", "_") + ".pdf";

                        sendMultipartEmail(
                                etudiant.getEmail(),
                                etudiant.getPrenom() + " " + etudiant.getNom(),
                                "🎉 Confirmation de participation — " + evenement.getTitre(),
                                htmlEmail,
                                qrBytes,
                                badgePdf,
                                badgeFileName,
                                planningPdf,
                                planningFileName
                        );
                    } catch (Exception e) {
                        System.err.println("[Email] Erreur envoi à " + etudiant.getEmail() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Email] Erreur service confirmation: " + e.getMessage());
                e.printStackTrace();
            }
        });
        executor.shutdown();
    }

    // ── Envoi SMTP avec QR code inline (cid:) + badge PDF ────────────────────

    private void sendMultipartEmail(String toEmail, String toName, String subject,
                                     String htmlBody, byte[] qrBytes,
                                     byte[] pdfBytes, String pdfFileName,
                                     byte[] planningPdfBytes, String planningFileName) throws MessagingException {
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
        msg.setFrom(new InternetAddress(FROM_EMAIL));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        msg.setSubject(subject);

        MimeMultipart mixedPart = new MimeMultipart("mixed");

        // Partie related (HTML + QR inline)
        MimeMultipart relatedPart = new MimeMultipart("related");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
        relatedPart.addBodyPart(htmlPart);

        if (qrBytes != null && qrBytes.length > 0) {
            MimeBodyPart qrPart = new MimeBodyPart();
            qrPart.setContent(qrBytes, "image/png");
            qrPart.setContentID("<qrcode>");
            qrPart.setDisposition(MimeBodyPart.INLINE);
            qrPart.setFileName("qrcode.png");
            relatedPart.addBodyPart(qrPart);
        }

        MimeBodyPart relatedWrapper = new MimeBodyPart();
        relatedWrapper.setContent(relatedPart);
        mixedPart.addBodyPart(relatedWrapper);

        // Badge PDF
        if (pdfBytes != null && pdfBytes.length > 0) {
            MimeBodyPart pdfPart = new MimeBodyPart();
            pdfPart.setContent(pdfBytes, "application/pdf");
            pdfPart.setFileName(pdfFileName != null ? pdfFileName : "badge_autolearn.pdf");
            pdfPart.setDisposition(MimeBodyPart.ATTACHMENT);
            mixedPart.addBodyPart(pdfPart);
        }

        // Planning PDF
        if (planningPdfBytes != null && planningPdfBytes.length > 0) {
            MimeBodyPart planningPart = new MimeBodyPart();
            planningPart.setContent(planningPdfBytes, "application/pdf");
            planningPart.setFileName(planningFileName != null ? planningFileName : "planning_evenement.pdf");
            planningPart.setDisposition(MimeBodyPart.ATTACHMENT);
            mixedPart.addBodyPart(planningPart);
        }

        msg.setContent(mixedPart);
        Transport.send(msg);
        System.out.println("[Email] ✅ Envoyé à: " + toEmail);
    }

    // ── Construction du HTML de l'email ──────────────────────────────────────

    private String buildEmailHtml(Etudiant etudiant, Equipe equipe, Evenement evenement,
                                   int participationId, Map<String, Object> weather, boolean hasPlanningPdf) {

        String dateStr = evenement.getDateDebut() != null
                ? evenement.getDateDebut().format(DATE_FMT) : "Date à confirmer";
        String lieu = evenement.getLieu() != null ? evenement.getLieu() : "Lieu à confirmer";
        String type = evenement.getType() != null ? evenement.getType() : "Événement";

        String typeColor = switch (type.toLowerCase()) {
            case "hackathon"  -> "#4facfe";
            case "conference" -> "#f093fb";
            case "workshop"   -> "#667eea";
            default           -> "#667eea";
        };

        String weatherSection = buildWeatherSection(weather, evenement.getDateDebut());
        String weatherTip     = buildWeatherTip(weather);

        // QR code via cid: (compatible Gmail — pas de base64 inline)
        String qrSection = "<img src=\"cid:qrcode\" alt=\"QR Code\" "
                + "style=\"width:160px;height:160px;display:block;margin:0 auto;"
                + "border-radius:8px;\" />";

        // Lien de détails — URL locale
        String detailsUrl = "http://localhost:8765/participation/" + participationId + "?eid=" + evenement.getId() + "&uid=" + etudiant.getId();

        return "<!DOCTYPE html>\n"
            + "<html lang=\"fr\">\n"
            + "<head>\n"
            + "  <meta charset=\"UTF-8\" />\n"
            + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n"
            + "  <title>Confirmation de participation — AutoLearn</title>\n"
            + "</head>\n"
            + "<body style=\"margin:0;padding:0;background:#f0f0f7;"
            +   "font-family:'Segoe UI',Helvetica,Arial,sans-serif;\">\n"

            // ── Wrapper pleine largeur ──
            + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\""
            +   " style=\"background:#f0f0f7;\">\n"
            + "<tr><td align=\"center\" style=\"padding:32px 20px;\">\n"

            // ── Conteneur principal 680px ──
            + "<table role=\"presentation\" width=\"680\" cellpadding=\"0\" cellspacing=\"0\""
            +   " style=\"background:#ffffff;border-radius:20px;overflow:hidden;"
            +   "box-shadow:0 8px 40px rgba(122,106,216,0.15);\">\n"

            // ── HEADER ──
            + "<tr><td style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);"
            +   "padding:48px 48px 40px;text-align:center;\">\n"
            + "  <div style=\"font-size:11px;font-weight:700;color:rgba(255,255,255,0.6);"
            +     "letter-spacing:4px;text-transform:uppercase;margin-bottom:16px;\">AutoLearn Platform</div>\n"
            + "  <div style=\"font-size:52px;margin-bottom:16px;\">🎉</div>\n"
            + "  <h1 style=\"margin:0;color:#ffffff;font-size:30px;font-weight:800;line-height:1.2;\">"
            +     "Participation Confirmée !</h1>\n"
            + "  <p style=\"margin:12px 0 0;color:rgba(255,255,255,0.8);font-size:16px;\">"
            +     "Votre place est officiellement réservée</p>\n"
            + "</td></tr>\n"

            // ── SALUTATION ──
            + "<tr><td style=\"padding:40px 48px 0;\">\n"
            + "  <p style=\"margin:0;font-size:17px;color:#1e1e1e;line-height:1.6;\">"
            +     "Bonjour <strong style=\"color:#7a6ad8;\">"
            +     etudiant.getPrenom() + " " + etudiant.getNom() + "</strong>,</p>\n"
            + "  <p style=\"margin:14px 0 0;font-size:15px;color:#4b5563;line-height:1.8;\">"
            +     "Félicitations ! Votre participation à l'événement ci-dessous a été "
            +     "<strong style=\"color:#059669;\">officiellement acceptée</strong>. "
            +     "Votre badge officiel est en pièce jointe — imprimez-le et portez-le le jour J.</p>\n"
            + "</td></tr>\n"

            // ── CARTE ÉVÉNEMENT ──
            + "<tr><td style=\"padding:28px 48px 0;\">\n"
            + "  <div style=\"background:#f0ebff;border-radius:16px;padding:28px;"
            +     "border-left:5px solid #667eea;\">\n"
            + "    <span style=\"display:inline-block;background:" + typeColor + ";color:#fff;"
            +       "font-size:11px;font-weight:700;letter-spacing:1px;text-transform:uppercase;"
            +       "padding:5px 16px;border-radius:20px;margin-bottom:16px;\">" + type + "</span>\n"
            + "    <h2 style=\"margin:0 0 20px;font-size:22px;color:#1e1e1e;font-weight:800;\">"
            +       evenement.getTitre() + "</h2>\n"
            + "    <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n"
            + "      <tr><td style=\"padding:8px 0;border-bottom:1px solid #e8e4ff;\">"
            +         "<span style=\"font-size:20px;\">📅</span>"
            +         "<span style=\"font-size:14px;color:#374151;margin-left:10px;\">"
            +         "<strong>Date :</strong> " + dateStr + "</span></td></tr>\n"
            + "      <tr><td style=\"padding:8px 0;border-bottom:1px solid #e8e4ff;\">"
            +         "<span style=\"font-size:20px;\">📍</span>"
            +         "<span style=\"font-size:14px;color:#374151;margin-left:10px;\">"
            +         "<strong>Lieu :</strong> " + lieu + "</span></td></tr>\n"
            + "      <tr><td style=\"padding:8px 0;border-bottom:1px solid #e8e4ff;\">"
            +         "<span style=\"font-size:20px;\">👥</span>"
            +         "<span style=\"font-size:14px;color:#374151;margin-left:10px;\">"
            +         "<strong>Équipe :</strong> " + equipe.getNom() + "</span></td></tr>\n"
            + "      <tr><td style=\"padding:8px 0;\">"
            +         "<span style=\"font-size:20px;\">🎫</span>"
            +         "<span style=\"font-size:14px;color:#374151;margin-left:10px;\">"
            +         "<strong>Badge N° :</strong> "
            +         "<span style=\"color:#667eea;font-weight:700;font-size:16px;\">#"
            +         String.format("%05d", participationId) + "</span></span></td></tr>\n"
            + "    </table>\n"
            + "  </div>\n"
            + "</td></tr>\n"

            // ── MÉTÉO ──
            + weatherSection

            // ── CONSEIL MÉTÉO ──
            + weatherTip

            // ── QR CODE ──
            + "<tr><td style=\"padding:28px 48px 0;\">\n"
            + "  <div style=\"background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);border-radius:16px;padding:32px;text-align:center;\">\n"
            + "    <h3 style=\"margin:0 0 8px;color:#ffffff;font-size:18px;font-weight:700;\">"
            +       "🔍 Votre QR Code de Participation</h3>\n"
            + "    <p style=\"margin:0 0 24px;color:rgba(255,255,255,0.8);font-size:14px;\">"
            +       "Présentez ce code à l'entrée de l'événement</p>\n"
            + "    <div style=\"background:#ffffff;border-radius:12px;padding:20px;"
            +       "display:inline-block;\">\n"
            + "      " + qrSection + "\n"
            + "    </div>\n"
            + "    <p style=\"margin:20px 0 0;font-size:13px;color:rgba(255,255,255,0.7);\">"
            +       "Lien de vos détails de participation :<br/>"
            +       "<span style=\"color:rgba(255,255,255,0.9);font-size:12px;\">" + detailsUrl + "</span></p>\n"
            + "  </div>\n"
            + "</td></tr>\n"

            // ── BADGE INFO ──
            + "<tr><td style=\"padding:24px 48px 0;\">\n"
            + "  <div style=\"background:#f0ebff;border-radius:12px;padding:20px;"
            +     "border-left:4px solid #667eea;\">\n"
            + "    <table cellpadding=\"0\" cellspacing=\"0\"><tr>\n"
            + "      <td style=\"font-size:28px;padding-right:16px;\">📎</td>\n"
            + "      <td><p style=\"margin:0;font-size:15px;font-weight:700;color:#667eea;\">"
            +           "Votre badge officiel est en pièce jointe</p>\n"
            + "          <p style=\"margin:4px 0 0;font-size:13px;color:#764ba2;\">"
            +           "Imprimez-le et portez-le le jour de l'événement. "
            +           "Il contient votre QR code d'accès.</p></td>\n"
            + "    </tr></table>\n"
            + "  </div>\n"
            + "</td></tr>\n"

            // ── PLANNING PDF ──
            + (hasPlanningPdf
                ? "<tr><td style=\"padding:16px 48px 0;\">\n"
                + "  <div style=\"background:#f0ebff;border-radius:12px;padding:20px;"
                +     "border-left:4px solid #764ba2;\">\n"
                + "    <table cellpadding=\"0\" cellspacing=\"0\"><tr>\n"
                + "      <td style=\"font-size:28px;padding-right:16px;\">📋</td>\n"
                + "      <td><p style=\"margin:0;font-size:15px;font-weight:700;color:#764ba2;\">"
                +           "Planning de l'événement en pièce jointe</p>\n"
                + "          <p style=\"margin:4px 0 0;font-size:13px;color:#667eea;\">"
                +           "Le planning détaillé de l'événement est joint à cet email. "
                +           "Consultez-le pour préparer votre participation.</p></td>\n"
                + "    </tr></table>\n"
                + "  </div>\n"
                + "</td></tr>\n"
                : "")

            // ── FOOTER ──
            + "<tr><td style=\"padding:36px 48px 40px;\">\n"
            + "  <hr style=\"border:none;border-top:1px solid #e5e7eb;margin:0 0 24px;\" />\n"
            + "  <p style=\"margin:0;font-size:13px;color:#9ca3af;text-align:center;line-height:1.8;\">\n"
            + "    Cet email a été envoyé automatiquement par "
            +     "<strong style=\"color:#667eea;\">AutoLearn</strong>.<br/>\n"
            + "    Pour toute question : "
            +     "<a href=\"mailto:autolearn66@gmail.com\" style=\"color:#667eea;\">autolearn66@gmail.com</a><br/>\n"
            + "    <span style=\"font-size:11px;\">© 2026 AutoLearn — Tunisie</span>\n"
            + "  </p>\n"
            + "</td></tr>\n"

            + "</table>\n"  // fin conteneur 680px
            + "</td></tr></table>\n"  // fin wrapper
            + "</body></html>";
    }

    // ── Météo ─────────────────────────────────────────────────────────────────

    /**
     * Extrait une ville utilisable par OpenWeatherMap depuis le champ lieu.
     * Ex: "Cité de culture" → "Tunis,TN"
     *     "Sfax" → "Sfax,TN"
     */
    private String extractVille(String lieu) {
        if (lieu == null || lieu.isBlank()) return "Tunis,TN";
        String l = lieu.trim().toLowerCase();
        // Villes tunisiennes connues
        if (l.contains("tunis") || l.contains("cité") || l.contains("cite")
                || l.contains("culture") || l.contains("belvédère") || l.contains("belvedere")
                || l.contains("lac") || l.contains("centre")) return "Tunis,TN";
        if (l.contains("sfax"))   return "Sfax,TN";
        if (l.contains("sousse")) return "Sousse,TN";
        if (l.contains("bizerte")) return "Bizerte,TN";
        if (l.contains("nabeul")) return "Nabeul,TN";
        if (l.contains("monastir")) return "Monastir,TN";
        if (l.contains("gabes"))  return "Gabes,TN";
        if (l.contains("gafsa"))  return "Gafsa,TN";
        if (l.contains("ariana")) return "Ariana,TN";
        if (l.contains("manouba")) return "Manouba,TN";
        // Par défaut Tunis
        return "Tunis,TN";
    }

    private String buildWeatherSection(Map<String, Object> weather, LocalDateTime eventDate) {
        if (weather == null || !Boolean.TRUE.equals(weather.get("available"))) {
            return ""; // Pas de données météo
        }

        int temp      = weather.get("temperature") instanceof Number n ? n.intValue() : 0;
        int feelsLike = weather.get("feels_like")  instanceof Number n ? n.intValue() : 0;
        String desc   = (String) weather.getOrDefault("description", "");
        String icon   = (String) weather.getOrDefault("icon", "");
        int humidity  = weather.get("humidity")    instanceof Number n ? n.intValue() : 0;
        String wind   = weather.getOrDefault("wind_speed", "0").toString();
        String city   = (String) weather.getOrDefault("city", "");
        boolean isForecast = Boolean.TRUE.equals(weather.get("is_forecast"));

        String emoji     = getWeatherEmoji(icon);
        String label     = isForecast ? "Prévision météo le jour J" : "Météo actuelle (référence)";
        String dateLabel = eventDate != null ? " — " + eventDate.format(DATE_SHORT) : "";
        String tempColor = temp >= 30 ? "#ea580c" : temp >= 20 ? "#16a34a" : "#2563eb";
        String bgColor   = "#f0ebff";
        String border    = "#e8e4ff";

        return "<tr><td style=\"padding:28px 48px 0;\">\n"
            + "  <div style=\"background:" + bgColor + ";border-radius:16px;padding:24px;"
            +     "border-left:5px solid #667eea;\">\n"
            + "    <div style=\"font-size:11px;font-weight:700;color:#667eea;letter-spacing:2px;"
            +       "text-transform:uppercase;margin-bottom:16px;\">🌤 " + label + dateLabel + "</div>\n"
            + "    <table cellpadding=\"0\" cellspacing=\"0\"><tr>\n"
            + "      <td style=\"font-size:56px;line-height:1;padding-right:20px;\">" + emoji + "</td>\n"
            + "      <td>\n"
            + "        <div style=\"font-size:42px;font-weight:800;color:" + tempColor + ";line-height:1;\">"
            +           temp + "°C</div>\n"
            + "        <div style=\"font-size:15px;color:#374151;margin-top:6px;\">" + desc + "</div>\n"
            + "        <div style=\"font-size:13px;color:#6b7280;margin-top:4px;\">"
            +           "Ressenti " + feelsLike + "°C &nbsp;·&nbsp; "
            +           "Humidité " + humidity + "% &nbsp;·&nbsp; "
            +           "Vent " + wind + " km/h</div>\n"
            + (city.isBlank() ? "" : "        <div style=\"font-size:12px;color:#667eea;margin-top:4px;font-weight:600;\">📍 " + city + "</div>\n")
            + "      </td>\n"
            + "    </tr></table>\n"
            + "  </div>\n"
            + "</td></tr>\n";
    }

    private String buildWeatherTip(Map<String, Object> weather) {
        String tip;
        if (weather == null || !Boolean.TRUE.equals(weather.get("available"))) {
            tip = "💡 <strong>Conseil :</strong> Pensez à vérifier la météo la veille de l'événement "
                + "et à vous habiller en conséquence. Bonne chance à toute votre équipe ! 🚀";
        } else {
            String icon = (String) weather.getOrDefault("icon", "");
            int temp    = weather.get("temperature") instanceof Number n ? n.intValue() : 20;
            if (icon.startsWith("11"))
                tip = "⛈️ <strong>Attention :</strong> Orages prévus ! Prévoyez un imperméable et partez plus tôt.";
            else if (icon.startsWith("09") || icon.startsWith("10"))
                tip = "🌧️ <strong>Pluie prévue :</strong> N'oubliez pas votre parapluie et des chaussures imperméables !";
            else if (icon.startsWith("13"))
                tip = "❄️ <strong>Neige prévue :</strong> Habillez-vous chaudement et soyez prudent sur la route.";
            else if (icon.startsWith("50"))
                tip = "🌫️ <strong>Brouillard :</strong> Visibilité réduite — partez plus tôt que prévu.";
            else if (temp >= 35)
                tip = "🥵 <strong>Forte chaleur :</strong> Hydratez-vous bien et portez des vêtements légers !";
            else if (temp >= 28)
                tip = "☀️ <strong>Beau temps chaud :</strong> Parfait pour l'événement ! Pensez à emporter de l'eau.";
            else if (temp >= 18)
                tip = "🌤️ <strong>Météo idéale :</strong> Conditions parfaites — profitez-en avec votre équipe !";
            else if (temp >= 10)
                tip = "🧥 <strong>Temps frais :</strong> Prévoyez une veste. Les températures sont agréables.";
            else
                tip = "🥶 <strong>Temps froid :</strong> Couvrez-vous bien ! Écharpe et manteau recommandés.";
        }

        return "<tr><td style=\"padding:16px 48px 0;\">\n"
            + "  <div style=\"background:#f0ebff;border-radius:12px;padding:18px;"
            +     "border-left:4px solid #667eea;\">\n"
            + "    <p style=\"margin:0;font-size:14px;color:#667eea;line-height:1.7;font-weight:600;\">" + tip + "</p>\n"
            + "  </div>\n"
            + "</td></tr>\n";
    }

    private String getWeatherEmoji(String icon) {
        if (icon == null) return "🌤️";
        return switch (icon) {
            case "01d" -> "☀️"; case "01n" -> "🌙";
            case "02d","02n" -> "⛅"; case "03d","03n","04d","04n" -> "☁️";
            case "09d","09n" -> "🌧️"; case "10d" -> "🌦️"; case "10n" -> "🌧️";
            case "11d","11n" -> "⛈️"; case "13d","13n" -> "❄️"; case "50d","50n" -> "🌫️";
            default -> "🌤️";
        };
    }
}
