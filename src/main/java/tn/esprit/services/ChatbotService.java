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
 *
 * Flow:
 *  1. User sends message
 *  2. Java calls Ollama API at localhost:11434
 *  3. Mistral model returns JSON with intent + parameters
 *  4. ChatbotActionExecutor executes the action
 *  5. Result shown in chat
 */
public class ChatbotService {

    // Ollama runs locally - no API key needed
    private static final String OLLAMA_URL   = "http://localhost:11434/api/chat";
    private static final String OLLAMA_MODEL = "mistral"; // or llama3, phi3, gemma2

    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    // System prompt - tells Mistral how to respond
    private static final String SYSTEM_PROMPT =
        "Tu es un assistant intelligent pour l application AutoLearn. " +
        "Tu aides les administrateurs a gerer la plateforme. " +
        "Quand l utilisateur demande une action, reponds UNIQUEMENT avec un JSON valide: " +
        "{\"intent\": \"ACTION\", \"params\": {}, \"message\": \"message convivial\"} " +
        "Intents disponibles: " +
        "LIST_COURS, CREATE_COURS(titre,description,matiere,niveau,duree), UPDATE_COURS(id,...), DELETE_COURS(id), " +
        "LIST_USERS, CREATE_USER(nom,prenom,email,niveau), DELETE_USER(id), " +
        "LIST_EVENEMENTS, CREATE_EVENEMENT(titre,lieu,description,type,nb_max), DELETE_EVENEMENT(id), " +
        "LIST_CHALLENGES, CREATE_CHALLENGE(titre,description,niveau,duree), DELETE_CHALLENGE(id), " +
        "LIST_COMMUNAUTES, CREATE_COMMUNAUTE(nom,description), " +
        "NAVIGATE_COURS, NAVIGATE_USERS, NAVIGATE_EVENEMENTS, NAVIGATE_CHALLENGES, NAVIGATE_COMMUNAUTE, NAVIGATE_DASHBOARD, " +
        "CHAT (pour conversation normale). " +
        "Reponds en francais. Reponds UNIQUEMENT avec le JSON, rien d autre.";

    // ── Public API ────────────────────────────────────────────────────────────

    public static CompletableFuture<ChatResponse> sendMessage(
            String userMessage,
            List<ChatMessage> conversationHistory) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build Ollama request
                JsonArray messages = new JsonArray();

                // System message
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", SYSTEM_PROMPT);
                messages.add(systemMsg);

                // Last 6 messages for context
                int start = Math.max(0, conversationHistory.size() - 6);
                for (int i = start; i < conversationHistory.size(); i++) {
                    ChatMessage cm = conversationHistory.get(i);
                    JsonObject msg = new JsonObject();
                    msg.addProperty("role", cm.role());
                    msg.addProperty("content", cm.content());
                    messages.add(msg);
                }

                // Current user message
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                messages.add(userMsg);

                // Ollama request body
                JsonObject body = new JsonObject();
                body.addProperty("model", OLLAMA_MODEL);
                body.add("messages", messages);
                body.addProperty("stream", false);

                // Options for deterministic output
                JsonObject options = new JsonObject();
                options.addProperty("temperature", 0.1);
                options.addProperty("num_predict", 400);
                body.add("options", options);

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Chatbot] Ollama HTTP " + resp.statusCode());

                if (resp.statusCode() == 200) {
                    return parseOllamaResponse(resp.body());
                } else {
                    System.err.println("[Chatbot] Ollama error " + resp.statusCode() + ": " + resp.body());
                    return fallbackResponse(userMessage);
                }

            } catch (java.net.ConnectException e) {
                System.err.println("[Chatbot] Ollama not running! Start it with: ollama serve");
                return new ChatResponse("CHAT", new JsonObject(),
                    "L assistant IA n est pas disponible. Assurez-vous qu Ollama est installe et demarre (ollama serve).", false);
            } catch (Exception e) {
                System.err.println("[Chatbot] Error: " + e.getMessage());
                return fallbackResponse(userMessage);
            }
        });
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private static ChatResponse parseOllamaResponse(String responseBody) {
        try {
            // Ollama response format: {"message": {"role": "assistant", "content": "..."}}
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            String content = json
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

            System.out.println("[Chatbot] Raw: " + content);

            // Extract JSON from response
            String jsonStr = extractJson(content);
            if (jsonStr == null) {
                // Model returned plain text, treat as CHAT
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
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return null;
    }

    // ── Fallback (keyword matching when Ollama is offline) ────────────────────

    private static ChatResponse fallbackResponse(String message) {
        String lower = message.toLowerCase();
        JsonObject p = new JsonObject();

        if (lower.contains("cours") && (lower.contains("liste") || lower.contains("affiche") || lower.contains("montre") || lower.contains("voir")))
            return new ChatResponse("LIST_COURS", p, "Voici la liste des cours :", true);
        if ((lower.contains("utilisateur") || lower.contains("etudiant")) && (lower.contains("liste") || lower.contains("affiche") || lower.contains("voir")))
            return new ChatResponse("LIST_USERS", p, "Voici la liste des utilisateurs :", true);
        if (lower.contains("evenement") && (lower.contains("liste") || lower.contains("affiche") || lower.contains("voir")))
            return new ChatResponse("LIST_EVENEMENTS", p, "Voici la liste des evenements :", true);
        if (lower.contains("challenge") && (lower.contains("liste") || lower.contains("affiche") || lower.contains("voir")))
            return new ChatResponse("LIST_CHALLENGES", p, "Voici la liste des challenges :", true);
        if (lower.contains("communaute") && (lower.contains("liste") || lower.contains("affiche") || lower.contains("voir")))
            return new ChatResponse("LIST_COMMUNAUTES", p, "Voici la liste des communautes :", true);
        if (lower.contains("cours"))
            return new ChatResponse("NAVIGATE_COURS", p, "Navigation vers la page Cours.", true);
        if (lower.contains("utilisateur") || lower.contains("etudiant"))
            return new ChatResponse("NAVIGATE_USERS", p, "Navigation vers la page Utilisateurs.", true);
        if (lower.contains("evenement"))
            return new ChatResponse("NAVIGATE_EVENEMENTS", p, "Navigation vers la page Evenements.", true);
        if (lower.contains("challenge"))
            return new ChatResponse("NAVIGATE_CHALLENGES", p, "Navigation vers la page Challenges.", true);
        if (lower.contains("dashboard") || lower.contains("accueil"))
            return new ChatResponse("NAVIGATE_DASHBOARD", p, "Navigation vers le Dashboard.", true);

        return new ChatResponse("CHAT", p,
            "Je suis votre assistant AutoLearn. Dites-moi ce que vous souhaitez faire !\n" +
            "Exemples: \"liste les cours\", \"cree un evenement\", \"affiche les etudiants\"", true);
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record ChatResponse(String intent, JsonObject params, String message, boolean success) {}
    public record ChatMessage(String role, String content) {}
}