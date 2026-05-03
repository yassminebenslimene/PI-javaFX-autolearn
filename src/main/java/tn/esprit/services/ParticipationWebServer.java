package tn.esprit.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Participation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Mini serveur HTTP embarqué (port 8765) qui sert les pages de participation.
 * Accessible via QR code : http://localhost:8765/participation/{id}?eid=X&uid=Y
 *
 * Ce serveur démarre automatiquement avec l'application JavaFX et permet
 * aux participants de scanner leur QR code pour voir leurs détails de participation
 * dans un navigateur web avec un design professionnel.
 */
public class ParticipationWebServer {

    private static final int PORT = 8765;
    private static HttpServer server;
    private static boolean running = false;

    private static final ParticipationService participationService = new ParticipationService();
    private static final EquipeService equipeService               = new EquipeService();
    private static final EvenementService evenementService         = new EvenementService();
    private static final WeatherService weatherService             = new WeatherService();

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy 'à' HH'h'mm", Locale.FRENCH);

    /**
     * Démarre le serveur HTTP embarqué.
     * Appelé au démarrage de l'application dans MainApp.
     */
    public static void start() {
        if (running) return;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);
            server.createContext("/participation", ParticipationWebServer::handleParticipation);
            server.createContext("/health", exchange -> {
                byte[] resp = "OK".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
                exchange.getResponseBody().write(resp);
                exchange.getResponseBody().close();
            });
            server.setExecutor(Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "web-server-thread");
                t.setDaemon(true);
                return t;
            }));
            server.start();
            running = true;
            System.out.println("✅ AutoLearn Web Server démarré sur http://localhost:" + PORT);
        } catch (IOException e) {
            System.err.println("⚠️ Impossible de démarrer le serveur web: " + e.getMessage());
        }
    }

    /**
     * Arrête le serveur proprement.
     */
    public static void stop() {
        if (server != null) {
            server.stop(1);
            running = false;
        }
    }

    public static int getPort() { return PORT; }

    public static String getParticipationUrl(int participationId, int etudiantId, int evenementId) {
        return "http://localhost:" + PORT + "/participation/" + participationId
                + "?eid=" + evenementId + "&uid=" + etudiantId;
    }

    // ── Handler principal ─────────────────────────────────────────────────────

    private static void handleParticipation(HttpExchange exchange) throws IOException {
        try {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath(); // /participation/123
            String query = uri.getQuery(); // eid=X&uid=Y

            // Extraire l'ID de participation depuis le path
            String[] parts = path.split("/");
            int participationId = -1;
            if (parts.length >= 3) {
                try { participationId = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
            }

            // Extraire les paramètres de query
            Map<String, String> params = parseQuery(query);
            int evenementId = parseInt(params.get("eid"), -1);
            int etudiantId  = parseInt(params.get("uid"), -1);

            String html;
            if (participationId < 0) {
                html = buildErrorPage("Lien invalide", "L'identifiant de participation est manquant ou incorrect.");
            } else {
                html = buildParticipationPage(participationId, evenementId, etudiantId);
            }

            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        } catch (Exception e) {
            String err = buildErrorPage("Erreur serveur", e.getMessage());
            byte[] response = err.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(500, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }
    }

    // ── Construction de la page de participation ──────────────────────────────

    private static String buildParticipationPage(int participationId, int evenementId, int etudiantId) {
        Participation participation = participationService.getById(participationId);
        if (participation == null) {
            return buildErrorPage("Participation introuvable",
                    "Aucune participation trouvée avec l'identifiant #" + participationId + ".");
        }

        Equipe equipe = equipeService.getById(participation.getEquipeId());
        Evenement evenement = evenementService.getById(participation.getEvenementId());
        if (equipe == null || evenement == null) {
            return buildErrorPage("Données manquantes", "Impossible de charger les détails de cet événement.");
        }

        List<Etudiant> membres = equipeService.getEtudiantsByEquipe(equipe.getId());
        Etudiant etudiant = membres.stream()
                .filter(e -> e.getId() == etudiantId)
                .findFirst().orElse(membres.isEmpty() ? null : membres.get(0));

        // Météo
        Map<String, Object> weather = null;
        if (evenement.getLieu() != null && evenement.getDateDebut() != null) {
            weather = weatherService.getWeatherForEvent(
                    evenement.getLieu() + ",TN", evenement.getDateDebut());
        }

        String nomParticipant = etudiant != null
                ? etudiant.getPrenom() + " " + etudiant.getNom() : "Participant";
        String dateStr = evenement.getDateDebut() != null
                ? evenement.getDateDebut().format(DATE_FMT) : "Date à confirmer";
        String lieu = evenement.getLieu() != null ? evenement.getLieu() : "Lieu à confirmer";
        String type = evenement.getType() != null ? evenement.getType() : "Événement";

        String typeColor = switch (type.toLowerCase()) {
            case "hackathon"  -> "#7a6ad8";
            case "conference" -> "#059669";
            case "workshop"   -> "#f59e0b";
            default           -> "#6b7280";
        };

        String membresHtml = buildMembresHtml(membres);
        String weatherHtml = buildWeatherHtml(weather);

        return """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
  <title>Participation — %s | AutoLearn</title>
  <style>
    *{box-sizing:border-box;margin:0;padding:0}
    body{font-family:'Segoe UI',system-ui,sans-serif;background:#f0f0f7;min-height:100vh;padding:24px 16px}
    .container{max-width:640px;margin:0 auto}
    .card{background:#fff;border-radius:20px;overflow:hidden;box-shadow:0 8px 40px rgba(122,106,216,.13);margin-bottom:16px}
    .header{background:linear-gradient(135deg,#7a6ad8,#5b4fc4);padding:36px 32px;text-align:center;color:#fff}
    .header .logo{font-size:12px;font-weight:700;letter-spacing:3px;opacity:.7;text-transform:uppercase;margin-bottom:10px}
    .header h1{font-size:22px;font-weight:800;margin-bottom:6px}
    .header p{font-size:14px;opacity:.85}
    .badge-check{font-size:48px;margin-bottom:12px}
    .body{padding:28px 32px}
    .type-pill{display:inline-block;padding:4px 16px;border-radius:20px;font-size:11px;font-weight:700;
               letter-spacing:1px;color:#fff;margin-bottom:16px}
    .event-title{font-size:20px;font-weight:800;color:#1e1e1e;margin-bottom:16px;line-height:1.3}
    .meta-row{display:flex;align-items:center;gap:10px;padding:8px 0;border-bottom:1px solid #f3f4f6}
    .meta-row:last-child{border-bottom:none}
    .meta-icon{font-size:18px;width:28px;text-align:center}
    .meta-label{font-size:13px;color:#6b7280;font-weight:600;min-width:80px}
    .meta-value{font-size:14px;color:#1e1e1e;font-weight:500}
    .section-title{font-size:13px;font-weight:700;color:#6b7280;letter-spacing:1px;
                   text-transform:uppercase;margin-bottom:14px}
    .member-row{display:flex;align-items:center;gap:12px;padding:10px 0;border-bottom:1px solid #f9fafb}
    .member-row:last-child{border-bottom:none}
    .avatar{width:38px;height:38px;border-radius:50%;background:#7a6ad8;color:#fff;
            font-weight:700;font-size:13px;display:flex;align-items:center;justify-content:center;flex-shrink:0}
    .member-name{font-size:14px;font-weight:600;color:#1e1e1e}
    .member-email{font-size:12px;color:#9ca3af}
    .status-badge{display:inline-flex;align-items:center;gap:6px;background:#d1fae5;
                  color:#065f46;padding:6px 16px;border-radius:20px;font-size:13px;font-weight:700}
    .weather-card{background:#f5f3ff;border-radius:16px;padding:20px;border:1px solid #ddd6fe}
    .weather-temp{font-size:40px;font-weight:800;line-height:1}
    .weather-desc{font-size:14px;color:#374151;margin-top:4px}
    .weather-meta{font-size:12px;color:#6b7280;margin-top:6px}
    .tip-box{background:#fef3c7;border-radius:12px;padding:16px;border:1px solid #fde68a;
             font-size:14px;color:#92400e;line-height:1.6;margin-top:12px}
    .footer{text-align:center;padding:20px;font-size:12px;color:#9ca3af}
    @media(max-width:480px){.body{padding:20px 18px}.header{padding:28px 20px}}
  </style>
</head>
<body>
  <div class="container">

    <!-- Header -->
    <div class="card">
      <div class="header">
        <div class="logo">AutoLearn Platform</div>
        <div class="badge-check">✅</div>
        <h1>Participation Vérifiée</h1>
        <p>Badge N° <strong>#%05d</strong> — %s</p>
      </div>
      <div class="body">
        <div style="text-align:center;margin-bottom:20px;">
          <span class="status-badge">✓ Participation Acceptée</span>
        </div>
        <div class="meta-row">
          <span class="meta-icon">👤</span>
          <span class="meta-label">Participant</span>
          <span class="meta-value" style="font-weight:700;color:#7a6ad8;">%s</span>
        </div>
        <div class="meta-row">
          <span class="meta-icon">👥</span>
          <span class="meta-label">Équipe</span>
          <span class="meta-value">%s</span>
        </div>
      </div>
    </div>

    <!-- Détails événement -->
    <div class="card">
      <div class="body">
        <div class="section-title">📅 Détails de l'Événement</div>
        <span class="type-pill" style="background:%s;">%s</span>
        <div class="event-title">%s</div>
        <div class="meta-row">
          <span class="meta-icon">📅</span>
          <span class="meta-label">Date</span>
          <span class="meta-value">%s</span>
        </div>
        <div class="meta-row">
          <span class="meta-icon">📍</span>
          <span class="meta-label">Lieu</span>
          <span class="meta-value">%s</span>
        </div>
        <div class="meta-row">
          <span class="meta-icon">🎫</span>
          <span class="meta-label">Badge N°</span>
          <span class="meta-value" style="color:#7a6ad8;font-weight:700;">#%05d</span>
        </div>
      </div>
    </div>

    <!-- Membres équipe -->
    <div class="card">
      <div class="body">
        <div class="section-title">👥 Membres de l'Équipe (%d)</div>
        %s
      </div>
    </div>

    <!-- Météo -->
    %s

    <!-- Footer -->
    <div class="footer">
      <p>🎓 <strong>AutoLearn</strong> — Plateforme d'apprentissage</p>
      <p style="margin-top:4px;">Ce QR code est personnel et lié à votre participation.</p>
    </div>

  </div>
</body>
</html>
""".formatted(
                evenement.getTitre(),                    // title tag
                participationId, type,                   // header subtitle
                nomParticipant,                          // participant name
                equipe.getNom(),                         // équipe
                typeColor, type.toUpperCase(),           // type pill
                evenement.getTitre(),                    // event title
                dateStr,                                 // date
                lieu,                                    // lieu
                participationId,                         // badge num
                membres.size(),                          // membres count
                membresHtml,                             // membres list
                weatherHtml                              // météo
        );
    }

    private static String buildMembresHtml(List<Etudiant> membres) {
        if (membres.isEmpty()) return "<p style=\"color:#9ca3af;font-size:14px;\">Aucun membre trouvé.</p>";
        StringBuilder sb = new StringBuilder();
        for (Etudiant et : membres) {
            String initials = et.getPrenom().substring(0, 1).toUpperCase()
                    + et.getNom().substring(0, 1).toUpperCase();
            sb.append("""
                <div class="member-row">
                  <div class="avatar">%s</div>
                  <div>
                    <div class="member-name">%s %s</div>
                    <div class="member-email">%s%s</div>
                  </div>
                </div>
            """.formatted(initials, et.getPrenom(), et.getNom(), et.getEmail(),
                    et.getNiveau() != null ? " · " + et.getNiveau().toUpperCase() : ""));
        }
        return sb.toString();
    }

    private static String buildWeatherHtml(Map<String, Object> weather) {
        if (weather == null || !Boolean.TRUE.equals(weather.get("available"))) return "";

        int temp = weather.get("temperature") instanceof Number n ? n.intValue() : 0;
        int feelsLike = weather.get("feels_like") instanceof Number n ? n.intValue() : 0;
        String desc = (String) weather.getOrDefault("description", "");
        String icon = (String) weather.getOrDefault("icon", "");
        int humidity = weather.get("humidity") instanceof Number n ? n.intValue() : 0;
        Object windObj = weather.get("wind_speed");
        String wind = windObj != null ? windObj.toString() : "0";
        boolean isForecast = Boolean.TRUE.equals(weather.get("is_forecast"));

        String emoji = switch (icon != null ? icon : "") {
            case "01d" -> "☀️"; case "01n" -> "🌙";
            case "02d","02n" -> "⛅"; case "03d","03n","04d","04n" -> "☁️";
            case "09d","09n" -> "🌧️"; case "10d" -> "🌦️"; case "10n" -> "🌧️";
            case "11d","11n" -> "⛈️"; case "13d","13n" -> "❄️"; case "50d","50n" -> "🌫️";
            default -> "🌤️";
        };

        String tempColor = temp >= 30 ? "#ea580c" : temp >= 20 ? "#16a34a" : "#2563eb";
        String label = isForecast ? "Prévision météo le jour J" : "Météo actuelle (référence)";

        return """
        <div class="card">
          <div class="body">
            <div class="section-title">🌤 %s</div>
            <div class="weather-card">
              <div style="display:flex;align-items:center;gap:16px;">
                <div style="font-size:52px;line-height:1;">%s</div>
                <div>
                  <div class="weather-temp" style="color:%s;">%d°C</div>
                  <div class="weather-desc">%s</div>
                  <div class="weather-meta">
                    Ressenti %d°C &nbsp;·&nbsp; Humidité %d%% &nbsp;·&nbsp; Vent %s km/h
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        """.formatted(label, emoji, tempColor, temp, desc, feelsLike, humidity, wind);
    }

    private static String buildErrorPage(String title, String message) {
        return """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
  <title>Erreur — AutoLearn</title>
  <style>
    body{font-family:'Segoe UI',sans-serif;background:#f0f0f7;display:flex;
         align-items:center;justify-content:center;min-height:100vh;padding:24px}
    .card{background:#fff;border-radius:20px;padding:40px;text-align:center;
          max-width:400px;box-shadow:0 8px 40px rgba(0,0,0,.1)}
    h1{color:#1e1e1e;font-size:20px;margin:16px 0 8px}
    p{color:#6b7280;font-size:14px;line-height:1.6}
  </style>
</head>
<body>
  <div class="card">
    <div style="font-size:48px;">❌</div>
    <h1>%s</h1>
    <p>%s</p>
    <p style="margin-top:16px;font-size:12px;color:#9ca3af;">AutoLearn Platform</p>
  </div>
</body>
</html>
""".formatted(title, message);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) map.put(kv[0], kv[1]);
        }
        return map;
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}
