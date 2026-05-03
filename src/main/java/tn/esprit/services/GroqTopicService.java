package tn.esprit.services;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Smart Categorization via Groq API (LLaMA 3)
 * ─────────────────────────────────────────────────────────────────────────────
 * Uses Groq's ultra-fast LLaMA3 inference to perform semantic topic extraction.
 * This replaces keyword-based matching with true NLP understanding:
 *
 *   "Spring Boot REST API avec Docker" → ["backend", "java", "devops", "api"]
 *   "réseau de neurones pour la vision" → ["python", "machine learning", "deep learning"]
 *
 * The model understands context, synonyms, and implicit topics — not just keywords.
 */
public class GroqTopicService {

    // ── Config ────────────────────────────────────────────────────────────────
    public static final String API_KEY;
    public static final boolean API_KEY_PLACEHOLDER;

    static {
        // 1. Try ConfigLoader (config.properties - our standard config)
        String key = null;
        try {
            key = tn.esprit.tools.ConfigLoader.getGroqApiKey();
        } catch (Exception ignored) {}
        // 2. Try environment variable
        if (key == null || key.isBlank()) {
            key = System.getenv("GROQ_API_KEY");
        }
        // 3. Try groq.properties file (legacy)
        if (key == null || key.isBlank()) {
            try {
                java.util.Properties props = new java.util.Properties();
                java.io.File f = new java.io.File("groq.properties");
                if (f.exists()) {
                    props.load(new java.io.FileInputStream(f));
                    key = props.getProperty("GROQ_API_KEY");
                }
            } catch (Exception ignored) {}
        }
        API_KEY = (key != null && !key.isBlank()) ? key : "gsk_placeholder";
        API_KEY_PLACEHOLDER = API_KEY.equals("gsk_placeholder");
        System.out.println("[GroqTopicService] API key loaded: " + (API_KEY_PLACEHOLDER ? "PLACEHOLDER (AI disabled)" : "OK (" + API_KEY.substring(0, 8) + "...)"));
    }
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Extracts semantic topics from a post's text using LLaMA3.
     * Returns a list of 3-6 lowercase topic keywords.
     *
     * Example:
     *   input:  "Comment configurer Spring Security avec JWT pour une API REST?"
     *   output: ["java", "spring", "security", "jwt", "backend", "api"]
     */
    public List<String> extractTopics(String titre, String contenu) {
        String text = ((titre != null ? titre : "") + " " + (contenu != null ? contenu : "")).trim();
        if (text.isBlank()) return Collections.emptyList();

        String prompt = """
            Analyze this text and extract 3 to 6 relevant technical topics/keywords.
            Return ONLY a JSON array of lowercase strings, nothing else.
            Example: ["java", "spring", "backend", "api"]
            
            Text: "%s"
            """.formatted(text.length() > 500 ? text.substring(0, 500) : text);

        try {
            String response = callGroq(prompt);
            return parseTopics(response);
        } catch (Exception e) {
            System.err.println("[Groq] extractTopics error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (e.getCause() != null) System.err.println("[Groq] cause: " + e.getCause().getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Corrects and improves a post text using LLaMA3.
     * Fixes grammar, spelling, and improves clarity while keeping the original meaning.
     * Returns the improved text, or the original if Groq fails.
     */
    public String improvePost(String text) {
        if (text == null || text.isBlank()) return text;

        String prompt = """
            Correct the grammar and spelling of this text, and improve its clarity slightly.
            Keep the same language (French or English or Arabic — whatever the original is).
            Keep the same meaning and tone. Return ONLY the corrected text, nothing else, no explanation.
            
            Text: "%s"
            """.formatted(text.length() > 800 ? text.substring(0, 800) : text);

        try {
            String result = callGroq(prompt);
            // Remove surrounding quotes if present
            result = result.trim();
            if (result.startsWith("\"") && result.endsWith("\""))
                result = result.substring(1, result.length() - 1);
            System.out.println("[Groq] improved text: " + result.substring(0, Math.min(100, result.length())));
            return result;
        } catch (Exception e) {
            System.err.println("[Groq] improvePost error: " + e.getMessage());
            return text; // fallback: return original
        }
    }

    /**
     * Checks if a post is semantically related to a resource (cours/quiz).
     * Returns a similarity score 0.0 → 1.0.
     */
    public double semanticSimilarity(String postText, String resourceText) {
        if (postText == null || resourceText == null) return 0.0;

        String prompt = """
            Rate the semantic similarity between these two texts on a scale from 0.0 to 1.0.
            Return ONLY a decimal number like 0.7, nothing else.
            
            Text 1: "%s"
            Text 2: "%s"
            """.formatted(
                postText.length() > 300 ? postText.substring(0, 300) : postText,
                resourceText.length() > 200 ? resourceText.substring(0, 200) : resourceText
        );

        try {
            String response = callGroq(prompt);
            return Double.parseDouble(response.trim().replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            System.err.println("[Groq] similarity error: " + e.getMessage());
            return 0.0;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String callGroq(String userPrompt) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(ENDPOINT);
            request.setHeader("Authorization", "Bearer " + API_KEY);
            request.setHeader("Content-Type", "application/json");

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.addProperty("temperature", 0.1);
            body.addProperty("max_tokens", 100);

            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", userPrompt);
            messages.add(msg);
            body.add("messages", messages);

            request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            return client.execute(request, response -> {
                String json = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("[Groq] response: " + json.substring(0, Math.min(200, json.length())));
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return obj.getAsJsonArray("choices")
                          .get(0).getAsJsonObject()
                          .getAsJsonObject("message")
                          .get("content").getAsString().trim();
            });
        }
    }

    private List<String> parseTopics(String response) {
        try {
            // Extract JSON array from response
            int start = response.indexOf('[');
            int end   = response.lastIndexOf(']');
            if (start == -1 || end == -1) return Collections.emptyList();
            String json = response.substring(start, end + 1);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            List<String> topics = new ArrayList<>();
            for (JsonElement el : arr) topics.add(el.getAsString().toLowerCase().trim());
            System.out.println("[Groq] topics extracted: " + topics);
            return topics;
        } catch (Exception e) {
            System.err.println("[Groq] parseTopics error: " + e.getMessage() + " | raw: " + response);
            return Collections.emptyList();
        }
    }
}
