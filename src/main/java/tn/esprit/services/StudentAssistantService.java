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
        "\n\nActions disponibles:" +
        "\n- LIST_EVENEMENTS : lister les événements (affiche dans le chat)" +
        "\n- LIST_COURS : lister les cours (affiche dans le chat)" +
        "\n- LIST_CHALLENGES : lister les challenges (affiche dans le chat)" +
        "\n- LIST_COMMUNAUTES : lister les communautés (affiche dans le chat)" +
        "\n- JOIN_EVENEMENT(evenement_id) : s'inscrire à un événement" +
        "\n- CREATE_EQUIPE(nom, evenement_id) : créer une équipe pour un événement" +
        "\n- JOIN_COMMUNAUTE(communaute_id) : rejoindre une communauté" +
        "\n- CREATE_COMMUNAUTE(nom, description) : créer une nouvelle communauté" +
        "\n- NAVIGATE_COURS : UNIQUEMENT si l'étudiant dit explicitement 'aller aux cours', 'ouvrir cours', 'naviguer vers cours'" +
        "\n- NAVIGATE_EVENEMENTS : UNIQUEMENT si l'étudiant dit explicitement 'aller aux événements', 'ouvrir événements'" +
        "\n- NAVIGATE_CHALLENGES : UNIQUEMENT si l'étudiant dit explicitement 'aller aux challenges', 'ouvrir challenges'" +
        "\n- NAVIGATE_COMMUNAUTE : UNIQUEMENT si l'étudiant dit explicitement 'aller à la communauté', 'ouvrir communauté'" +
        "\n- NAVIGATE_CLASSEMENT : UNIQUEMENT si l'étudiant dit explicitement 'aller au classement'" +
        "\n- NAVIGATE_PROFIL : UNIQUEMENT si l'étudiant dit explicitement 'aller à mon profil'" +
        "\n- NAVIGATE_MES_PARTICIPATIONS : UNIQUEMENT si l'étudiant dit explicitement 'voir mes participations'" +
        "\n- NAVIGATE_MES_EQUIPES : UNIQUEMENT si l'étudiant dit explicitement 'voir mes équipes'" +
        "\n\nRÈGLES IMPORTANTES:" +
        "\n1. Pour les salutations et questions générales → intent CHAT, réponds naturellement." +
        "\n2. Si l'étudiant demande 'quels sont les cours?' ou 'liste les cours' → utilise LIST_COURS (affiche dans le chat, NE NAVIGUE PAS)." +
        "\n3. NAVIGATE_* UNIQUEMENT si l'étudiant dit explicitement 'aller à', 'ouvrir', 'naviguer vers'." +
        "\n4. Pour les actions avec infos manquantes → demande les infos (pas de JSON)." +
        "\n5. Sois toujours encourageant, utilise des emojis." +
        "\n6. Réponds TOUJOURS en français sauf si l'étudiant écrit en anglais.";

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

        // LIST actions - show in chat, don't navigate
        if (lower.contains("cours") && (lower.contains("liste") || lower.contains("quels") || lower.contains("voir") || lower.contains("affiche")))
            return new ChatResponse("LIST_COURS", p, "Voici les cours disponibles 📚", true);

        if ((lower.contains("evenement") || lower.contains("événement")) && (lower.contains("liste") || lower.contains("quels") || lower.contains("voir")))
            return new ChatResponse("LIST_EVENEMENTS", p, "Voici les événements disponibles 🎉", true);

        if (lower.contains("challenge") && (lower.contains("liste") || lower.contains("quels") || lower.contains("voir")))
            return new ChatResponse("LIST_CHALLENGES", p, "Voici les challenges disponibles 🏆", true);

        if ((lower.contains("communaute") || lower.contains("communauté")) && (lower.contains("liste") || lower.contains("quels") || lower.contains("voir")))
            return new ChatResponse("LIST_COMMUNAUTES", p, "Voici les communautés disponibles 👥", true);

        // NAVIGATE actions - only when explicitly asked
        if (lower.contains("aller") || lower.contains("ouvrir") || lower.contains("naviguer") || lower.contains("go to")) {
            if (lower.contains("cours"))       return new ChatResponse("NAVIGATE_COURS", p, "Je vous emmène vers les cours ! 📚", true);
            if (lower.contains("evenement") || lower.contains("événement")) return new ChatResponse("NAVIGATE_EVENEMENTS", p, "Je vous emmène vers les événements ! 🎉", true);
            if (lower.contains("challenge"))   return new ChatResponse("NAVIGATE_CHALLENGES", p, "Je vous emmène vers les challenges ! 🏆", true);
            if (lower.contains("communaute") || lower.contains("communauté")) return new ChatResponse("NAVIGATE_COMMUNAUTE", p, "Je vous emmène vers la communauté ! 👥", true);
            if (lower.contains("classement") || lower.contains("leaderboard")) return new ChatResponse("NAVIGATE_CLASSEMENT", p, "Je vous emmène vers le classement ! 🏅", true);
            if (lower.contains("profil"))      return new ChatResponse("NAVIGATE_PROFIL", p, "Je vous emmène vers votre profil ! 👤", true);
            if (lower.contains("participation")) return new ChatResponse("NAVIGATE_MES_PARTICIPATIONS", p, "Vos participations ! 📋", true);
            if (lower.contains("equipe") || lower.contains("équipe")) return new ChatResponse("NAVIGATE_MES_EQUIPES", p, "Vos équipes ! 👫", true);
        }

        return new ChatResponse("CHAT", p,
            "Je peux vous aider à :\n" +
            "📚 **Lister** les cours, événements, challenges\n" +
            "🎉 **S'inscrire** à un événement\n" +
            "👥 **Rejoindre** une communauté\n" +
            "👫 **Créer** une équipe\n" +
            "🧭 **Naviguer** : dites 'aller aux cours', 'ouvrir les événements'...\n\n" +
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
