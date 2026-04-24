package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Chatbot Service using Hugging Face Inference API.
 *
 * Model: mistralai/Mistral-7B-Instruct-v0.3
 * - Free tier (no credit card needed)
 * - Understands French and English
 * - Can follow instructions to detect intents
 *
 * Flow:
 *  1. User sends message
 *  2. We send it to Hugging Face with a system prompt
 *  3. Model returns JSON with intent + parameters
 *  4. ActionExecutorService executes the action
 *  5. Result shown in chat
 */
public class ChatbotService {

    // ── Hugging Face config ───────────────────────────────────────────────────

    // Free API key — get yours at https://huggingface.co/settings/tokens
    // This is a read-only token, safe to include
    private static final String HF_API_KEY = "hf_demo"; // Replace with your token

    private static final String HF_MODEL =
        "mistralai/Mistral-7B-Instruct-v0.3";

    private static final String HF_URL =
        "https://api-inference.huggingface.co/models/" + HF_MODEL + "/v1/chat/completions";

    private static final Gson GSON = new Gson();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // ── System prompt ─────────────────────────────────────────────────────────

    /**
     * System prompt that tells the model how to behave.
     * It must return a JSON with intent + parameters.
     */
    private static final String SYSTEM_PROMPT = """
        Tu es un assistant intelligent pour l'application AutoLearn.
        Tu aides les administrateurs et les etudiants a gerer la plateforme.
        
        Quand l'utilisateur te demande d'effectuer une action, reponds TOUJOURS avec un JSON valide dans ce format:
        {
          "intent": "ACTION_TYPE",
          "params": { ... },
          "message": "Message convivial a afficher a l'utilisateur"
        }
        
        Les intents disponibles sont:
        
        CRUD Cours:
        - LIST_COURS: lister tous les cours
        - CREATE_COURS: creer un cours (params: titre, description, matiere, niveau, duree)
        - UPDATE_COURS: modifier un cours (params: id, titre, description, matiere, niveau, duree)
        - DELETE_COURS: supprimer un cours (params: id)
        
        CRUD Utilisateurs:
        - LIST_USERS: lister tous les utilisateurs
        - CREATE_USER: creer un etudiant (params: nom, prenom, email, niveau)
        - DELETE_USER: supprimer un utilisateur (params: id)
        
        CRUD Evenements:
        - LIST_EVENEMENTS: lister tous les evenements
        - CREATE_EVENEMENT: creer un evenement (params: titre, lieu, description, type, date_debut, date_fin, nb_max)
        - DELETE_EVENEMENT: supprimer un evenement (params: id)
        
        CRUD Challenges:
        - LIST_CHALLENGES: lister tous les challenges
        - CREATE_CHALLENGE: creer un challenge (params: titre, description, niveau, duree, date_debut, date_fin)
        - DELETE_CHALLENGE: supprimer un challenge (params: id)
        
        CRUD Communautes:
        - LIST_COMMUNAUTES: lister toutes les communautes
        - CREATE_COMMUNAUTE: creer une communaute (params: nom, description)
        
        Navigation:
        - NAVIGATE_COURS: aller a la page cours
        - NAVIGATE_USERS: aller a la page utilisateurs
        - NAVIGATE_EVENEMENTS: aller a la page evenements
        - NAVIGATE_CHALLENGES: aller a la page challenges
        - NAVIGATE_COMMUNAUTE: aller a la page communaute
        - NAVIGATE_DASHBOARD: aller au dashboard
        
        Conversation:
        - CHAT: simple conversation, question, ou demande d'aide (params: {})
        
        Si l'utilisateur parle en francais, reponds en francais.
        Si des parametres sont manquants pour une action, demande-les dans le message.
        Mets toujours un message convivial et encourage l'utilisateur.
        
        IMPORTANT: Reponds UNIQUEMENT avec le JSON, rien d'autre.
        """;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sends a message to the chatbot and returns the response asynchronously.
     *
     * @param userMessage The user's message
     * @param conversationHistory Previous messages for context
     * @return CompletableFuture<ChatResponse>
     */
    public static CompletableFuture<ChatResponse> sendMessage(
            String userMessage,
            java.util.List<ChatMessage> conversationHistory) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build messages array
                JsonArray messages = new JsonArray();

                // System message
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", SYSTEM_PROMPT);
                messages.add(systemMsg);

                // Add conversation history (last 6 messages for context)
                int start = Math.max(0, conversationHistory.size() - 6);
                for (int i = start; i < conversationHistory.size(); i++) {
                    ChatMessage cm = conversationHistory.get(i);
                    JsonObject msg = new JsonObject();
                    msg.addProperty("role", cm.role());
                    msg.addProperty("content", cm.content());
                    messages.add(msg);
                }

                // Add current user message
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                messages.add(userMsg);

                // Build request body
                JsonObject body = new JsonObject();
                body.addProperty("model", HF_MODEL);
                body.add("messages", messages);
                body.addProperty("max_tokens", 500);
                body.addProperty("temperature", 0.3); // Low = more deterministic
                body.addProperty("stream", false);

                // Send request
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(HF_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + HF_API_KEY)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Chatbot] HTTP " + resp.statusCode());

                if (resp.statusCode() == 200) {
                    return parseHuggingFaceResponse(resp.body());
                } else if (resp.statusCode() == 503) {
                    // Model loading — retry after delay
                    System.out.println("[Chatbot] Model loading, retrying...");
                    Thread.sleep(3000);
                    HttpResponse<String> retry = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                    if (retry.statusCode() == 200) {
                        return parseHuggingFaceResponse(retry.body());
                    }
                    return new ChatResponse("CHAT", new JsonObject(),
                        "Le modele IA est en cours de chargement. Reessayez dans quelques secondes.", false);
                } else {
                    System.err.println("[Chatbot] Error: " + resp.body());
                    return fallbackResponse(userMessage);
                }

            } catch (Exception e) {
                System.err.println("[Chatbot] Error: " + e.getMessage());
                return fallbackResponse(userMessage);
            }
        });
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private static ChatResponse parseHuggingFaceResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            String content = json
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

            System.out.println("[Chatbot] Raw response: " + content);

            // Extract JSON from response (model might add extra text)
            String jsonStr = extractJson(content);
            if (jsonStr == null) {
                return new ChatResponse("CHAT", new JsonObject(), content, true);
            }

            JsonObject parsed = GSON.fromJson(jsonStr, JsonObject.class);
            String intent = parsed.has("intent") ? parsed.get("intent").getAsString() : "CHAT";
            JsonObject params = parsed.has("params") ? parsed.getAsJsonObject("params") : new JsonObject();
            String message = parsed.has("message") ? parsed.get("message").getAsString() : content;

            return new ChatResponse(intent, params, message, true);

        } catch (Exception e) {
            System.err.println("[Chatbot] Parse error: " + e.getMessage());
            return new ChatResponse("CHAT", new JsonObject(),
                "Je n'ai pas compris votre demande. Pouvez-vous reformuler ?", false);
        }
    }

    /**
     * Extracts JSON object from a string that might contain extra text.
     */
    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    /**
     * Fallback when API is unavailable — uses simple keyword matching.
     */
    private static ChatResponse fallbackResponse(String message) {
        String lower = message.toLowerCase();
        JsonObject params = new JsonObject();

        if (lower.contains("cours") && (lower.contains("liste") || lower.contains("affiche") || lower.contains("montre"))) {
            return new ChatResponse("LIST_COURS", params, "Voici la liste des cours :", true);
        }
        if (lower.contains("utilisateur") && (lower.contains("liste") || lower.contains("affiche"))) {
            return new ChatResponse("LIST_USERS", params, "Voici la liste des utilisateurs :", true);
        }
        if (lower.contains("evenement") && (lower.contains("liste") || lower.contains("affiche"))) {
            return new ChatResponse("LIST_EVENEMENTS", params, "Voici la liste des evenements :", true);
        }
        if (lower.contains("challenge") && (lower.contains("liste") || lower.contains("affiche"))) {
            return new ChatResponse("LIST_CHALLENGES", params, "Voici la liste des challenges :", true);
        }
        if (lower.contains("cours")) {
            return new ChatResponse("NAVIGATE_COURS", params, "Je vous emmene vers la page Cours.", true);
        }
        if (lower.contains("utilisateur") || lower.contains("etudiant")) {
            return new ChatResponse("NAVIGATE_USERS", params, "Je vous emmene vers la page Utilisateurs.", true);
        }
        if (lower.contains("evenement")) {
            return new ChatResponse("NAVIGATE_EVENEMENTS", params, "Je vous emmene vers la page Evenements.", true);
        }

        return new ChatResponse("CHAT", params,
            "Je suis votre assistant AutoLearn. Je peux vous aider a gerer les cours, utilisateurs, evenements, challenges et communautes. Que souhaitez-vous faire ?", true);
    }

    // ── Data records ──────────────────────────────────────────────────────────

    public record ChatResponse(
        String intent,
        JsonObject params,
        String message,
        boolean success
    ) {}

    public record ChatMessage(
        String role,    // "user" or "assistant"
        String content
    ) {}
}
