package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chatbot Service using Ollama (local AI, no API key, no internet needed).
 *
 * Setup:
 *  1. Install Ollama: https://ollama.com
 *  2. Run: ollama pull mistral
 *  3. Ollama starts automatically at http://localhost:11434
 */
public class ChatbotService {

    private static final String OLLAMA_URL   = "http://localhost:11434/api/chat";
    private static final String OLLAMA_MODEL = "mistral";

    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final String SYSTEM_PROMPT =
        "You are AutoLearn assistant. You help admins manage the platform. " +
        "You speak French or English depending on the user. " +
        "When user asks to CREATE something, ask for the required fields one by one in a friendly way. " +
        "When you have ALL required fields, respond with ONLY a valid JSON: " +
        "{\"intent\": \"ACTION\", \"params\": {field: value, ...}, \"message\": \"friendly confirmation message\"} " +
        "Available intents and required fields: " +
        "CREATE_COURS: titre, matiere, niveau(DEBUTANT/INTERMEDIAIRE/AVANCE), duree(hours), description. " +
        "CREATE_EVENEMENT: titre, lieu, type(Conference/Atelier/Hackathon/Autre), nb_max(number), description. " +
        "CREATE_CHALLENGE: titre, niveau(DEBUTANT/INTERMEDIAIRE/AVANCE), duree(minutes), description. " +
        "CREATE_USER: prenom, nom, email, niveau(DEBUTANT/INTERMEDIAIRE/AVANCE). " +
        "CREATE_COMMUNAUTE: nom, description. " +
        "LIST_COURS, LIST_USERS, LIST_EVENEMENTS, LIST_CHALLENGES, LIST_COMMUNAUTES: no params needed. " +
        "DELETE_COURS(id), DELETE_USER(id), DELETE_EVENEMENT(id), DELETE_CHALLENGE(id): ask for id. " +
        "NAVIGATE_COURS, NAVIGATE_USERS, NAVIGATE_EVENEMENTS, NAVIGATE_CHALLENGES, NAVIGATE_COMMUNAUTE, NAVIGATE_DASHBOARD. " +
        "CHAT: for general conversation. " +
        "IMPORTANT: If user just says 'creer un evenement' or 'create an event' WITHOUT providing details, " +
        "do NOT return JSON yet. Instead ask for the required fields in a friendly message. " +
        "Only return JSON when you have all required fields. " +
        "Respond ONLY with JSON when executing an action, otherwise respond normally.";

    // ── Public API ────────────────────────────────────────────────────────────

    public static CompletableFuture<ChatResponse> sendMessage(
            String userMessage,
            List<ChatMessage> conversationHistory) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonArray messages = new JsonArray();

                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", SYSTEM_PROMPT);
                messages.add(systemMsg);

                // Last 10 messages for context (important for multi-turn)
                int start = Math.max(0, conversationHistory.size() - 10);
                for (int i = start; i < conversationHistory.size(); i++) {
                    ChatMessage cm = conversationHistory.get(i);
                    JsonObject msg = new JsonObject();
                    msg.addProperty("role", cm.role());
                    msg.addProperty("content", cm.content());
                    messages.add(msg);
                }

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                messages.add(userMsg);

                JsonObject body = new JsonObject();
                body.addProperty("model", OLLAMA_MODEL);
                body.add("messages", messages);
                body.addProperty("stream", false);

                JsonObject options = new JsonObject();
                options.addProperty("temperature", 0.2);
                options.addProperty("num_predict", 500);
                body.add("options", options);

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120)) // 2 minutes for slow machines
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Chatbot] Ollama HTTP " + resp.statusCode());

                if (resp.statusCode() == 200) {
                    return parseOllamaResponse(resp.body());
                } else {
                    System.err.println("[Chatbot] Error " + resp.statusCode() + ": " + resp.body());
                    return fallbackResponse(userMessage);
                }

            } catch (java.net.ConnectException e) {
                System.err.println("[Chatbot] Ollama not running!");
                return new ChatResponse("CHAT", new JsonObject(),
                    "L assistant IA n est pas disponible. Assurez-vous qu Ollama est installe et demarre.", false);
            } catch (Exception e) {
                System.err.println("[Chatbot] Error: " + e.getMessage());
                // If timeout, use fallback
                return fallbackResponse(userMessage);
            }
        });
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private static ChatResponse parseOllamaResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            String content = json
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

            System.out.println("[Chatbot] Raw: " + content);

            String jsonStr = extractJson(content);
            if (jsonStr == null) {
                // Model is asking for more info (multi-turn conversation)
                return new ChatResponse("CHAT", new JsonObject(), content, true);
            }

            JsonObject parsed = GSON.fromJson(jsonStr, JsonObject.class);
            String intent  = parsed.has("intent")  ? parsed.get("intent").getAsString()  : "CHAT";
            JsonObject params = parsed.has("params") ? parsed.getAsJsonObject("params")   : new JsonObject();
            String message = parsed.has("message") ? parsed.get("message").getAsString() : content;

            return new ChatResponse(intent, params, message, true);

        } catch (Exception e) {
            System.err.println("[Chatbot] Parse error: " + e.getMessage());
            return new ChatResponse("CHAT", new JsonObject(),
                "Je n ai pas compris. Pouvez-vous reformuler ?", false);
        }
    }

    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String candidate = text.substring(start, end + 1);
            // Verify it has "intent" key to avoid false positives
            if (candidate.contains("\"intent\"")) return candidate;
        }
        return null;
    }

    // ── Fallback (when Ollama is offline or times out) ────────────────────────

    private static ChatResponse fallbackResponse(String message) {
        String lower = message.toLowerCase();
        JsonObject p = new JsonObject();

        // LIST actions (must check before create/navigate)
        if ((lower.contains("liste") || lower.contains("affiche") || lower.contains("montre") || lower.contains("voir") || lower.contains("show") || lower.contains("list") || lower.contains("display")) ) {
            if (lower.contains("cours") || lower.contains("course"))
                return new ChatResponse("LIST_COURS", p, "Voici la liste des cours :", true);
            if (lower.contains("utilisateur") || lower.contains("etudiant") || lower.contains("user") || lower.contains("student"))
                return new ChatResponse("LIST_USERS", p, "Voici la liste des utilisateurs :", true);
            if (lower.contains("evenement") || lower.contains("event"))
                return new ChatResponse("LIST_EVENEMENTS", p, "Voici la liste des evenements :", true);
            if (lower.contains("challenge"))
                return new ChatResponse("LIST_CHALLENGES", p, "Voici la liste des challenges :", true);
            if (lower.contains("communaute") || lower.contains("community"))
                return new ChatResponse("LIST_COMMUNAUTES", p, "Voici la liste des communautes :", true);
        }

        // CREATE actions - ask for info instead of navigating
        if (lower.contains("creer") || lower.contains("create") || lower.contains("ajouter") || lower.contains("add") || lower.contains("nouveau") || lower.contains("new")) {
            if (lower.contains("evenement") || lower.contains("event"))
                return new ChatResponse("CHAT", p,
                    "Je vais creer un evenement ! J ai besoin des informations suivantes :\n\n" +
                    "1. Titre de l evenement ?\n" +
                    "2. Lieu ?\n" +
                    "3. Type ? (Conference / Atelier / Hackathon / Autre)\n" +
                    "4. Nombre maximum de participants ?\n" +
                    "5. Description ?\n\n" +
                    "Vous pouvez tout donner en une fois, par exemple :\n" +
                    "\"Titre: Java Day, Lieu: Tunis, Type: Conference, Max: 100, Description: Journee Java\"", true);
            if (lower.contains("cours") || lower.contains("course"))
                return new ChatResponse("CHAT", p,
                    "Je vais creer un cours ! J ai besoin de :\n\n" +
                    "1. Titre ?\n" +
                    "2. Matiere ?\n" +
                    "3. Niveau ? (DEBUTANT / INTERMEDIAIRE / AVANCE)\n" +
                    "4. Duree (en heures) ?\n" +
                    "5. Description ?\n\n" +
                    "Donnez-moi ces informations et je cree le cours immediatement !", true);
            if (lower.contains("challenge"))
                return new ChatResponse("CHAT", p,
                    "Je vais creer un challenge ! J ai besoin de :\n\n" +
                    "1. Titre ?\n" +
                    "2. Niveau ? (DEBUTANT / INTERMEDIAIRE / AVANCE)\n" +
                    "3. Duree (en minutes) ?\n" +
                    "4. Description ?\n\n" +
                    "Donnez-moi ces informations !", true);
            if (lower.contains("utilisateur") || lower.contains("etudiant") || lower.contains("user") || lower.contains("student"))
                return new ChatResponse("CHAT", p,
                    "Je vais creer un etudiant ! J ai besoin de :\n\n" +
                    "1. Prenom ?\n" +
                    "2. Nom ?\n" +
                    "3. Email ?\n" +
                    "4. Niveau ? (DEBUTANT / INTERMEDIAIRE / AVANCE)\n\n" +
                    "Donnez-moi ces informations !", true);
            if (lower.contains("communaute") || lower.contains("community"))
                return new ChatResponse("CHAT", p,
                    "Je vais creer une communaute ! J ai besoin de :\n\n" +
                    "1. Nom ?\n" +
                    "2. Description ?\n\n" +
                    "Donnez-moi ces informations !", true);
        }

        // Parse creation with provided fields
        if (lower.contains("titre:") || lower.contains("title:") || lower.contains("nom:") || lower.contains("name:")) {
            return parseInlineCreation(message, lower);
        }

        // NAVIGATE actions (only when no create/list keyword)
        if (lower.contains("cours") || lower.contains("course"))
            return new ChatResponse("NAVIGATE_COURS", p, "Navigation vers la page Cours.", true);
        if (lower.contains("utilisateur") || lower.contains("etudiant") || lower.contains("user") || lower.contains("student"))
            return new ChatResponse("NAVIGATE_USERS", p, "Navigation vers la page Utilisateurs.", true);
        if (lower.contains("evenement") || lower.contains("event"))
            return new ChatResponse("NAVIGATE_EVENEMENTS", p, "Navigation vers la page Evenements.", true);
        if (lower.contains("challenge"))
            return new ChatResponse("NAVIGATE_CHALLENGES", p, "Navigation vers la page Challenges.", true);
        if (lower.contains("communaute") || lower.contains("community"))
            return new ChatResponse("NAVIGATE_COMMUNAUTE", p, "Navigation vers la page Communaute.", true);
        if (lower.contains("dashboard") || lower.contains("accueil") || lower.contains("home"))
            return new ChatResponse("NAVIGATE_DASHBOARD", p, "Navigation vers le Dashboard.", true);

        return new ChatResponse("CHAT", p,
            "Bonjour ! Je suis votre assistant AutoLearn.\n\n" +
            "Je peux vous aider a :\n" +
            "• Lister les cours, etudiants, evenements, challenges\n" +
            "• Creer des cours, evenements, challenges, etudiants\n" +
            "• Naviguer dans l application\n\n" +
            "Exemples : \"liste les cours\", \"cree un evenement\", \"affiche les etudiants\"", true);
    }

    /**
     * Parses inline creation like:
     * "Titre: Java Day, Lieu: Tunis, Type: Conference, Max: 100, Description: Journee Java"
     */
    private static ChatResponse parseInlineCreation(String message, String lower) {
        JsonObject p = new JsonObject();

        // Extract key: value pairs
        String[] parts = message.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().toLowerCase();
                String val = kv[1].trim();
                switch (key) {
                    case "titre", "title"       -> p.addProperty("titre", val);
                    case "lieu", "location"     -> p.addProperty("lieu", val);
                    case "type"                 -> p.addProperty("type", val);
                    case "max", "nb_max", "participants" -> {
                        try { p.addProperty("nb_max", Integer.parseInt(val)); }
                        catch (Exception e) { p.addProperty("nb_max", 50); }
                    }
                    case "description", "desc"  -> p.addProperty("description", val);
                    case "matiere", "subject"   -> p.addProperty("matiere", val);
                    case "niveau", "level"      -> p.addProperty("niveau", val.toUpperCase());
                    case "duree", "duration"    -> {
                        try { p.addProperty("duree", Integer.parseInt(val)); }
                        catch (Exception e) { p.addProperty("duree", 10); }
                    }
                    case "prenom", "firstname"  -> p.addProperty("prenom", val);
                    case "nom", "lastname"      -> p.addProperty("nom", val);
                    case "email"                -> p.addProperty("email", val);
                    case "nom communaute"       -> p.addProperty("nom", val);
                }
            }
        }

        // Determine what to create based on context
        if (p.has("lieu") || lower.contains("evenement") || lower.contains("event")) {
            if (!p.has("titre")) p.addProperty("titre", "Nouvel Evenement");
            if (!p.has("lieu")) p.addProperty("lieu", "Tunis");
            if (!p.has("type")) p.addProperty("type", "Conference");
            if (!p.has("nb_max")) p.addProperty("nb_max", 50);
            if (!p.has("description")) p.addProperty("description", "A completer");
            return new ChatResponse("CREATE_EVENEMENT", p,
                "Parfait ! Je cree l evenement \"" + p.get("titre").getAsString() + "\" maintenant...", true);
        }
        if (p.has("matiere") || lower.contains("cours") || lower.contains("course")) {
            if (!p.has("titre")) p.addProperty("titre", "Nouveau Cours");
            if (!p.has("matiere")) p.addProperty("matiere", "Informatique");
            if (!p.has("niveau")) p.addProperty("niveau", "DEBUTANT");
            if (!p.has("duree")) p.addProperty("duree", 10);
            if (!p.has("description")) p.addProperty("description", "A completer");
            return new ChatResponse("CREATE_COURS", p,
                "Parfait ! Je cree le cours \"" + p.get("titre").getAsString() + "\" maintenant...", true);
        }
        if (p.has("email")) {
            if (!p.has("prenom")) p.addProperty("prenom", "Prenom");
            if (!p.has("nom")) p.addProperty("nom", "Nom");
            if (!p.has("niveau")) p.addProperty("niveau", "DEBUTANT");
            return new ChatResponse("CREATE_USER", p,
                "Parfait ! Je cree l etudiant maintenant...", true);
        }

        return new ChatResponse("CHAT", new JsonObject(),
            "J ai recu vos informations. Pouvez-vous preciser ce que vous souhaitez creer ? (evenement, cours, etudiant, challenge)", true);
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record ChatResponse(String intent, JsonObject params, String message, boolean success) {}
    public record ChatMessage(String role, String content) {}
}