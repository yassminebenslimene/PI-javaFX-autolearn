package tn.esprit.services;

import com.google.gson.*;
import tn.esprit.entities.*;
import tn.esprit.session.SessionManager;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Student AI Assistant Service
 * Uses Groq API (Llama 3.3 70B) to understand student requests
 * and execute actions: join event, create team, view courses, etc.
 */
public class StudentAssistantService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";
    private static final Gson   GSON         = new Gson();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final String SYSTEM_PROMPT =
        "Tu es un assistant IA personnel pour les étudiants de la plateforme AutoLearn. " +
        "Tu es amical, encourageant, et tu parles en français (ou dans la langue de l'étudiant). " +
        "Tu aides les étudiants à naviguer, apprendre, et effectuer des actions sur la plateforme. " +
        "\n\nPour les ACTIONS, réponds UNIQUEMENT avec du JSON: " +
        "{\"intent\": \"ACTION\", \"params\": {}, \"message\": \"message amical\"} " +
        "\n\nActions disponibles pour les étudiants:" +
        "\n- NAVIGATE_COURS : voir les cours disponibles" +
        "\n- NAVIGATE_EVENEMENTS : voir les événements" +
        "\n- NAVIGATE_CHALLENGES : voir les challenges" +
        "\n- NAVIGATE_COMMUNAUTE : voir les communautés" +
        "\n- NAVIGATE_CLASSEMENT : voir le classement" +
        "\n- NAVIGATE_PROFIL : voir mon profil" +
        "\n- NAVIGATE_MES_PARTICIPATIONS : voir mes participations aux événements" +
        "\n- NAVIGATE_MES_EQUIPES : voir mes équipes" +
        "\n- LIST_EVENEMENTS : lister les événements disponibles" +
        "\n- LIST_COURS : lister les cours disponibles" +
        "\n- LIST_CHALLENGES : lister les challenges disponibles" +
        "\n- LIST_COMMUNAUTES : lister les communautés disponibles" +
        "\n- JOIN_EVENEMENT(evenement_id) : s'inscrire à un événement" +
        "\n- CREATE_EQUIPE(nom, evenement_id) : créer une équipe pour un événement" +
        "\n- JOIN_COMMUNAUTE(communaute_id) : rejoindre une communauté" +
        "\n- CREATE_COMMUNAUTE(nom, description) : créer une nouvelle communauté" +
        "\n\nRÈGLES:" +
        "\n1. Pour les salutations et questions générales → intent CHAT, réponds naturellement." +
        "\n2. Pour les actions avec toutes les infos → retourne le JSON immédiatement." +
        "\n3. Pour les actions avec infos manquantes → demande les infos manquantes (pas de JSON)." +
        "\n4. Sois toujours encourageant et positif." +
        "\n5. Utilise des emojis pour rendre les réponses plus conviviales." +
        "\n6. Si l'étudiant demande de l'aide pour apprendre → suggère des cours ou challenges adaptés.";

    public record ChatResponse(String intent, JsonObject params, String message, boolean success) {}
    public record ChatMessage(String role, String content) {}

    // ── Public API ────────────────────────────────────────────────────────────

    public static CompletableFuture<ChatResponse> sendMessage(
            String userMessage,
            List<ChatMessage> history) {

        String context = buildStudentContext();

        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiKey = tn.esprit.tools.ConfigLoader.getGroqApiKey();
                if (apiKey == null || apiKey.isEmpty()) {
                    return smartFallback(userMessage);
                }

                JsonArray messages = new JsonArray();

                // System prompt
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", SYSTEM_PROMPT + context);
                messages.add(systemMsg);

                // History (last 8 messages)
                int start = Math.max(0, history.size() - 8);
                for (int i = start; i < history.size(); i++) {
                    ChatMessage cm = history.get(i);
                    JsonObject msg = new JsonObject();
                    msg.addProperty("role", cm.role());
                    msg.addProperty("content", cm.content());
                    messages.add(msg);
                }

                // Current message
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                messages.add(userMsg);

                JsonObject body = new JsonObject();
                body.addProperty("model", GROQ_MODEL);
                body.add("messages", messages);
                body.addProperty("temperature", 0.3);
                body.addProperty("max_tokens", 800);

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    return parseGroqResponse(resp.body());
                } else {
                    System.err.println("[StudentAI] Groq error " + resp.statusCode());
                    return smartFallback(userMessage);
                }

            } catch (java.net.http.HttpTimeoutException e) {
                return smartFallback(userMessage);
            } catch (Exception e) {
                System.err.println("[StudentAI] Error: " + e.getMessage());
                return smartFallback(userMessage);
            }
        });
    }

    // ── Response parsing ──────────────────────────────────────────────────────

    private static ChatResponse parseGroqResponse(String body) {
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.size() == 0)
                return new ChatResponse("CHAT", new JsonObject(), "Désolé, je n'ai pas pu répondre.", false);

            String content = choices.get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString().trim();

            // Try to extract JSON intent
            int start = content.indexOf('{');
            int end   = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String candidate = content.substring(start, end + 1);
                if (candidate.contains("\"intent\"")) {
                    try {
                        JsonObject parsed = GSON.fromJson(candidate, JsonObject.class);
                        String intent  = parsed.has("intent")  ? parsed.get("intent").getAsString()  : "CHAT";
                        JsonObject p   = parsed.has("params")  ? parsed.getAsJsonObject("params")    : new JsonObject();
                        String message = parsed.has("message") ? parsed.get("message").getAsString() : content;
                        return new ChatResponse(intent, p, message, true);
                    } catch (Exception ignored) {}
                }
            }
            // Pure conversational response
            return new ChatResponse("CHAT", new JsonObject(), content, true);

        } catch (Exception e) {
            return new ChatResponse("CHAT", new JsonObject(), "Je n'ai pas compris. Pouvez-vous reformuler ?", false);
        }
    }

    // ── Smart fallback (no API) ───────────────────────────────────────────────

    private static ChatResponse smartFallback(String message) {
        String lower = message.toLowerCase().trim();
        JsonObject p = new JsonObject();

        if (lower.matches("(bonjour|salut|hello|hey|coucou|bonsoir).*"))
            return new ChatResponse("CHAT", p,
                "Bonjour ! 👋 Je suis votre assistant AutoLearn. Comment puis-je vous aider aujourd'hui ?", true);

        if (lower.contains("cours") && (lower.contains("voir") || lower.contains("liste") || lower.contains("affiche")))
            return new ChatResponse("NAVIGATE_COURS", p, "Je vous emmène vers les cours ! 📚", true);

        if (lower.contains("evenement") || lower.contains("événement"))
            return new ChatResponse("NAVIGATE_EVENEMENTS", p, "Voici les événements disponibles ! 🎉", true);

        if (lower.contains("challenge"))
            return new ChatResponse("NAVIGATE_CHALLENGES", p, "Prêt pour un challenge ? 🏆", true);

        if (lower.contains("communaute") || lower.contains("communauté"))
            return new ChatResponse("NAVIGATE_COMMUNAUTE", p, "Rejoignez la communauté ! 👥", true);

        if (lower.contains("classement") || lower.contains("leaderboard"))
            return new ChatResponse("NAVIGATE_CLASSEMENT", p, "Voyons votre classement ! 🏅", true);

        if (lower.contains("profil") || lower.contains("profile"))
            return new ChatResponse("NAVIGATE_PROFIL", p, "Voici votre profil ! 👤", true);

        if (lower.contains("participation") || lower.contains("mes event"))
            return new ChatResponse("NAVIGATE_MES_PARTICIPATIONS", p, "Vos participations aux événements ! 📋", true);

        if (lower.contains("equipe") || lower.contains("équipe") || lower.contains("team"))
            return new ChatResponse("NAVIGATE_MES_EQUIPES", p, "Vos équipes ! 👫", true);

        return new ChatResponse("CHAT", p,
            "Je peux vous aider à :\n" +
            "📚 Voir les **cours** disponibles\n" +
            "🎉 Voir les **événements** et s'inscrire\n" +
            "🏆 Voir les **challenges**\n" +
            "👥 Rejoindre des **communautés**\n" +
            "🏅 Voir le **classement**\n\n" +
            "Que souhaitez-vous faire ?", true);
    }

    // ── Student context ───────────────────────────────────────────────────────

    private static String buildStudentContext() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) return "";

            StringBuilder ctx = new StringBuilder("\n\n=== CONTEXTE ÉTUDIANT ===\n");
            ctx.append("Étudiant: ").append(user.getPrenom()).append(" ").append(user.getNom()).append("\n");
            ctx.append("Niveau: ").append(
                (user instanceof Etudiant e && e.getNiveau() != null) ? e.getNiveau() : "Non défini"
            ).append("\n");

            // Available events
            try {
                EvenementService evService = new EvenementService();
                List<Evenement> events = evService.getAll();
                ctx.append("Événements disponibles (").append(events.size()).append("):\n");
                events.stream().limit(5).forEach(e ->
                    ctx.append("  - [ID:").append(e.getId()).append("] ").append(e.getTitre())
                       .append(" (").append(e.getLieu()).append(", ").append(e.getType()).append(")\n"));
            } catch (Exception ignored) {}

            // Available courses
            try {
                ServiceCours coursService = new ServiceCours();
                List<Cours> cours = coursService.consulter();
                ctx.append("Cours disponibles (").append(cours.size()).append("):\n");
                cours.stream().limit(5).forEach(c ->
                    ctx.append("  - [ID:").append(c.getId()).append("] ").append(c.getTitre())
                       .append(" (").append(c.getMatiere()).append(", ").append(c.getNiveau()).append(")\n"));
            } catch (Exception ignored) {}

            // Available communities
            try {
                ServiceCommunaute commService = new ServiceCommunaute();
                List<Communaute> comms = commService.getList();
                ctx.append("Communautés disponibles (").append(comms.size()).append("):\n");
                comms.stream().limit(5).forEach(c ->
                    ctx.append("  - [ID:").append(c.getId()).append("] ").append(c.getNom()).append("\n"));
            } catch (Exception ignored) {}

            ctx.append("=== FIN CONTEXTE ===");
            return ctx.toString();

        } catch (Exception e) {
            return "";
        }
    }
}
