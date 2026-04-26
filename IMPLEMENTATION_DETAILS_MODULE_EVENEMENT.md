# 🔍 DÉTAILS D'IMPLÉMENTATION - MODULE ÉVÉNEMENT

**Date:** 26 Avril 2026  
**Statut:** ✅ COMPLET

---

## 1️⃣ ESPACE 3D - IMPLÉMENTATION DÉTAILLÉE

### Fichier
```
src/main/resources/views/frontoffice/salle3d.html (441 lignes)
```

### Architecture
```
HTML5 Canvas
├── Raycasting 2D Engine
├── Player Movement (WASD)
├── Camera Rotation (Arrows + Mouse)
├── Minimap Rendering
└── UI Overlay
```

### Géométrie du Monde
```javascript
world = {
  corridor: {
    width: 8,
    length: 50,
    height: 4,
    doors: [
      { x: 2, y: 10, label: 'A', room: 'salle_a' },
      { x: 2, y: 25, label: 'B', room: 'salle_b' },
      { x: 2, y: 40, label: 'C', room: 'salle_c' }
    ],
    elements: [
      { type: 'plant', x: 1, y: 5, size: 0.5 },
      { type: 'plant', x: 1, y: 15, size: 0.5 },
      { type: 'plant', x: 1, y: 30, size: 0.5 },
      { type: 'plant', x: 1, y: 45, size: 0.5 },
      { type: 'table', x: 4, y: 8, size: 1, id: 'c1', status: 'free' },
      { type: 'table', x: 4, y: 20, size: 1, id: 'c2', status: 'free' },
      { type: 'table', x: 4, y: 35, size: 1, id: 'c3', status: 'free' },
      { type: 'bar', x: 6, y: 48, size: 1.5 },
      { type: 'vending', x: 6, y: 5, size: 0.8 }
    ]
  },
  salle_a: {
    width: 6,
    length: 8,
    height: 3.5,
    label: 'Salle A - Hackathon',
    tables: [
      { x: 1.5, y: 2, id: 'a1', status: 'free' },
      { x: 4.5, y: 2, id: 'a2', status: 'free' },
      { x: 1.5, y: 5, id: 'a3', status: 'occupied' },
      { x: 4.5, y: 5, id: 'a4', status: 'free' }
    ]
  },
  // salle_b et salle_c similaires
}
```

### Palette Couleurs
```javascript
const colors = {
  wall: '#8b6614',           // Marron
  floor: '#f5e6c8',          // Beige clair
  ceiling: '#d4a96a',        // Or/Doré
  door: '#5c3317',           // Marron foncé
  table: '#a0826d',          // Nude/Taupe
  bar: '#8b6614',            // Marron
  plant: '#6b8e23',          // Vert
  light: '#ffd700',          // Jaune
  text: '#5c3317'            // Marron foncé
};
```

### Raycasting Engine
```javascript
function castRay(angle) {
  const room = world[player.room];
  let minDist = 100;
  
  // Murs
  const walls = [
    { x1: 0, y1: 0, x2: room.width, y2: 0 },
    { x1: room.width, y1: 0, x2: room.width, y2: room.length },
    { x1: room.width, y1: room.length, x2: 0, y2: room.length },
    { x1: 0, y1: room.length, x2: 0, y2: 0 }
  ];

  walls.forEach(wall => {
    const dist = rayWallDist(angle, wall);
    if (dist > 0) minDist = Math.min(minDist, dist);
  });

  return minDist;
}
```

### Navigation
```javascript
// Mouvement
if (keys['W']) {
  player.x += Math.cos(player.angle) * player.speed;
  player.y += Math.sin(player.angle) * player.speed;
}

// Rotation
if (keys['ARROWLEFT']) player.angle -= player.rotSpeed;
if (keys['ARROWRIGHT']) player.angle += player.rotSpeed;

// Portes
if (player.room === 'corridor' && keys['E']) {
  room.doors.forEach(door => {
    if (Math.abs(player.x - door.x) < 0.5 && Math.abs(player.y - door.y) < 0.5) {
      player.room = door.room;
      player.x = 3;
      player.y = 1;
    }
  });
}
```

---

## 2️⃣ EMAIL DE CONFIRMATION - IMPLÉMENTATION DÉTAILLÉE

### Fichier Principal
```
src/main/java/tn/esprit/services/ParticipationConfirmationService.java
```

### Flux d'Envoi
```
sendConfirmationToTeam()
├── Récupérer météo (OpenWeatherMap)
├── Générer QR code (QrCodeService)
├── Pour chaque membre:
│   ├── Générer badge PDF (BadgePdfService)
│   ├── Construire HTML email
│   └── Envoyer via Brevo (BrevoEmailService)
└── Logging
```

### Contenu Email HTML
```html
<table>
  <tr>
    <td>Équipe: {equipe.nom}</td>
  </tr>
  <tr>
    <td>Événement: {evenement.titre}</td>
  </tr>
  <tr>
    <td>Date: {date formatée}</td>
  </tr>
  <tr>
    <td>Lieu: {evenement.lieu}</td>
  </tr>
  <tr>
    <td>
      <!-- Météo -->
      Température: {weather.temp}°C
      Conditions: {weather.description}
      Conseil: {weather.tip}
    </td>
  </tr>
  <tr>
    <td>
      <!-- QR Code -->
      <img src="cid:qrcode" alt="QR Code" />
    </td>
  </tr>
</table>
```

### Intégration Brevo
```java
public boolean sendEmail(String toEmail, String toName, String subject,
                          String htmlContent, byte[] pdfBytes, String pdfFileName) {
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
    attachment.addProperty("name", pdfFileName);
    attachment.addProperty("content", Base64.getEncoder().encodeToString(pdfBytes));
    attachments.add(attachment);
    body.add("attachment", attachments);
  }
  
  // Envoi HTTP POST
  URL url = new URL(BREVO_API_URL);
  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
  conn.setRequestMethod("POST");
  conn.setRequestProperty("api-key", BREVO_API_KEY);
  conn.setRequestProperty("content-type", "application/json");
  
  try (OutputStream os = conn.getOutputStream()) {
    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
  }
  
  int responseCode = conn.getResponseCode();
  return responseCode == 201 || responseCode == 200;
}
```

---

## 3️⃣ QR CODE - IMPLÉMENTATION DÉTAILLÉE

### Fichier
```
src/main/java/tn/esprit/services/ParticipationWebServer.java
```

### Démarrage Web Server
```java
public static void start() {
  if (running) return;
  try {
    // Try port 8765, fallback to 8766, 8767
    int port = PORT;
    for (int attempt = 0; attempt < 3; attempt++) {
      try {
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        break;
      } catch (IOException e) {
        System.err.println("⚠️ Port " + port + " occupé");
        port++;
      }
    }
    
    // Endpoint /participation
    server.createContext("/participation", ex -> {
      try {
        handleParticipation(ex);
      } catch (IOException e) {
        System.err.println("[WebServer] Erreur I/O: " + e.getMessage());
      }
    });
    
    // Endpoint /health
    server.createContext("/health", ex -> {
      try {
        byte[] r = "OK".getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, r.length);
        ex.getResponseBody().write(r);
        ex.getResponseBody().close();
      } catch (IOException e) {
        System.err.println("[WebServer] Erreur I/O: " + e.getMessage());
      }
    });
    
    server.setExecutor(Executors.newFixedThreadPool(4));
    server.start();
    running = true;
    System.out.println("✅ Web Server démarré sur http://localhost:" + port);
  } catch (IOException e) {
    System.err.println("⚠️ Impossible de démarrer: " + e.getMessage());
  }
}
```

### Gestion Requête
```java
private static void handleParticipation(HttpExchange exchange) throws IOException {
  String query = exchange.getRequestURI().getQuery();
  Map<String, String> params = parseQuery(query);
  
  int participationId = parseInt(params.get("id"), -1);
  int evenementId = parseInt(params.get("eid"), -1);
  int etudiantId = parseInt(params.get("uid"), -1);
  
  String page = buildPage(participationId, evenementId, etudiantId);
  
  byte[] response = page.getBytes(StandardCharsets.UTF_8);
  exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
  exchange.sendResponseHeaders(200, response.length);
  exchange.getResponseBody().write(response);
  exchange.getResponseBody().close();
}
```

### URL Générée
```java
public static String getParticipationUrl(int participationId, int etudiantId, int evenementId) {
  return "http://localhost:" + PORT + "/participation?id=" + participationId 
         + "&eid=" + evenementId + "&uid=" + etudiantId;
}
```

---

## 4️⃣ RAPPORTS IA - IMPLÉMENTATION DÉTAILLÉE

### Fichier
```
src/main/java/tn/esprit/controllers/evenement/RapportsIAController.java
```

### Intégration Groq
```java
private final GroqService groqService = new GroqService();

public void generateReport(String type) {
  String prompt = buildPrompt(type);
  String response = groqService.generateReport(prompt);
  String htmlContent = markdownToHtml(response);
  displayReport(htmlContent);
}
```

### Types de Rapports
```java
private String buildPrompt(String type) {
  switch(type) {
    case "AMELIORATIONS":
      return "Suggérez 5 améliorations pour cet événement...";
    case "SUGGESTIONS":
      return "Donnez 5 suggestions pratiques pour...";
    case "ANALYSE":
      return "Fournissez une analyse globale de...";
    default:
      return "";
  }
}
```

### Conversion Markdown → HTML
```java
private String markdownToHtml(String markdown) {
  // Titres
  markdown = markdown.replaceAll("^### (.+)$", "<h3>$1</h3>");
  markdown = markdown.replaceAll("^## (.+)$", "<h2>$1</h2>");
  markdown = markdown.replaceAll("^# (.+)$", "<h1>$1</h1>");
  
  // Listes
  markdown = markdown.replaceAll("^- (.+)$", "<li>$1</li>");
  
  // Code
  markdown = markdown.replaceAll("`([^`]+)`", "<code>$1</code>");
  
  // Gras
  markdown = markdown.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
  
  return "<html><body>" + markdown + "</body></html>";
}
```

### Export PDF
```java
public void exportToPdf(String content, String filename) {
  byte[] pdfBytes = reportPdfService.generatePdf(content);
  saveFile(pdfBytes, filename);
}
```

---

## 5️⃣ MÉTÉO - IMPLÉMENTATION DÉTAILLÉE

### Fichier
```
src/main/java/tn/esprit/services/WeatherService.java
```

### Récupération Données
```java
public Map<String, Object> getWeatherForEvent(String ville, LocalDateTime eventDate) {
  try {
    String url = "https://api.openweathermap.org/data/2.5/weather?q=" + ville 
                 + "&appid=" + API_KEY + "&units=metric&lang=fr";
    
    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
    conn.setRequestMethod("GET");
    
    int responseCode = conn.getResponseCode();
    if (responseCode == 200) {
      String response = new String(conn.getInputStream().readAllBytes());
      JsonObject json = JsonParser.parseString(response).getAsJsonObject();
      
      Map<String, Object> weather = new HashMap<>();
      weather.put("available", true);
      weather.put("temp", json.getAsJsonObject("main").get("temp").getAsDouble());
      weather.put("description", json.getAsJsonArray("weather")
                                      .get(0).getAsJsonObject()
                                      .get("description").getAsString());
      weather.put("humidity", json.getAsJsonObject("main").get("humidity").getAsInt());
      weather.put("wind", json.getAsJsonObject("wind").get("speed").getAsDouble());
      weather.put("icon", json.getAsJsonArray("weather")
                               .get(0).getAsJsonObject()
                               .get("icon").getAsString());
      
      return weather;
    }
  } catch (Exception e) {
    System.err.println("Erreur météo: " + e.getMessage());
  }
  
  return getDefaultWeather();
}
```

### Logique Affichage
```java
public Map<String, Object> getWeatherForEvent(String ville, LocalDateTime eventDate) {
  long daysUntilEvent = ChronoUnit.DAYS.between(LocalDateTime.now(), eventDate);
  
  if (daysUntilEvent <= 5) {
    // Prévisions
    return getForecast(ville, eventDate);
  } else {
    // Météo actuelle
    return getCurrentWeather(ville);
  }
}
```

### Conseil Vestimentaire
```java
private String buildWeatherTip(Map<String, Object> weather) {
  double temp = (double) weather.get("temp");
  String description = (String) weather.get("description");
  
  if (temp < 10) {
    return "🧥 Apportez un manteau chaud";
  } else if (temp < 15) {
    return "🧢 Un pull ou une veste sera nécessaire";
  } else if (temp > 25) {
    return "☀️ N'oubliez pas la crème solaire";
  } else if (description.contains("pluie")) {
    return "☔ Apportez un parapluie";
  }
  
  return "👕 Tenue confortable recommandée";
}
```

---

## 🔧 PROBLÈME IOException - SOLUTION TECHNIQUE

### Problème
```
exception java.io.IOException is never thrown in body of corresponding try statement
```

### Cause Racine
```
Référence de méthode: ParticipationWebServer::handleParticipation
Interface: HttpHandler { void handle(HttpExchange) throws IOException; }
Conflit: La méthode déclare throws IOException, mais l'interface ne peut pas propager
```

### Solution
```java
// ❌ AVANT (ERREUR)
server.createContext("/participation", ParticipationWebServer::handleParticipation);

// ✅ APRÈS (CORRECT)
server.createContext("/participation", ex -> {
    try {
        handleParticipation(ex);
    } catch (IOException e) {
        System.err.println("[WebServer] Erreur I/O: " + e.getMessage());
    }
});
```

### Raison
- La lambda wrapper implémente `HttpHandler`
- Elle capture l'exception dans un try-catch
- Aucune exception n'est propagée au-delà
- Le compilateur est satisfait

---

## 📊 RÉSUMÉ TECHNIQUE

| Composant | Technologie | Statut |
|-----------|-------------|--------|
| Espace 3D | HTML5 Canvas + Raycasting | ✅ OK |
| Email | Brevo API + Gmail SMTP | ✅ OK |
| QR Code | Java HttpServer | ✅ OK |
| Rapports IA | Groq API | ✅ OK |
| Météo | OpenWeatherMap API | ✅ OK |

---

**Implémentation Complète et Fonctionnelle** 🎉
