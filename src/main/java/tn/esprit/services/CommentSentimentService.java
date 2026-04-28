package tn.esprit.services;

import com.google.gson.*;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.nio.charset.StandardCharsets;

/**
 * AI-Powered Feedback Loop via Groq (LLaMA3)
 * ─────────────────────────────────────────────────────────────────────────────
 * Analyzes a comment for:
 *   - Sentiment: NEGATIVE / NEUTRAL / POSITIVE
 *   - Topic: the main concept the student is struggling with
 *   - Quiz suggestion: a short quiz title to help reinforce the concept
 *
 * If sentiment is NEGATIVE and a topic is detected → triggers a quiz suggestion
 * in the UI so the student can practice immediately.
 */
public class CommentSentimentService {

    private static final String API_KEY  = GroqTopicService.API_KEY;
    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.3-70b-versatile";

    public record AnalysisResult(
        String sentiment,   // "NEGATIVE" | "NEUTRAL" | "POSITIVE"
        String topic,       // e.g. "Abstract Classes", "Recursion", "SQL Joins"
        String quizTitle,   // suggested quiz title
        boolean shouldSuggest // true only when NEGATIVE + topic found
    ) {}

    /**
     * Analyzes a comment text and returns sentiment + topic + quiz suggestion.
     * Runs on a background thread — never call on JavaFX thread directly.
     */
    public AnalysisResult analyze(String commentText) {
        if (commentText == null || commentText.isBlank() || commentText.length() < 8)
            return new AnalysisResult("NEUTRAL", null, null, false);

        if (GroqTopicService.API_KEY_PLACEHOLDER)
            return new AnalysisResult("NEUTRAL", null, null, false);

        String prompt = """
            Analyze this student comment from an e-learning platform.
            Return ONLY a JSON object with these exact fields:
            {
              "sentiment": "NEGATIVE" or "NEUTRAL" or "POSITIVE",
              "topic": "the main concept/topic the student mentions (null if none)",
              "quiz_title": "a short quiz title to help reinforce the topic (null if not needed)"
            }
            
            Rules:
            - sentiment is NEGATIVE if the student expresses confusion, difficulty, or not understanding
            - topic should be the specific concept (e.g. "Abstract Classes", "Recursion", "SQL Joins")
            - quiz_title should be like "Quiz: Abstract Classes — 5 questions rapides"
            - if sentiment is not NEGATIVE, set topic and quiz_title to null
            
            Comment: "%s"
            """.formatted(commentText.length() > 400 ? commentText.substring(0, 400) : commentText);

        try {
            String raw = callGroq(prompt);
            return parseResult(raw);
        } catch (Exception e) {
            System.err.println("[Sentiment] error: " + e.getMessage());
            return new AnalysisResult("NEUTRAL", null, null, false);
        }
    }

    private AnalysisResult parseResult(String raw) {
        try {
            int start = raw.indexOf('{');
            int end   = raw.lastIndexOf('}');
            if (start == -1 || end == -1) return new AnalysisResult("NEUTRAL", null, null, false);

            JsonObject obj = JsonParser.parseString(raw.substring(start, end + 1)).getAsJsonObject();
            String sentiment = obj.has("sentiment") ? obj.get("sentiment").getAsString().toUpperCase() : "NEUTRAL";
            String topic     = obj.has("topic") && !obj.get("topic").isJsonNull()
                               ? obj.get("topic").getAsString() : null;
            String quizTitle = obj.has("quiz_title") && !obj.get("quiz_title").isJsonNull()
                               ? obj.get("quiz_title").getAsString() : null;

            boolean shouldSuggest = "NEGATIVE".equals(sentiment) && topic != null && !topic.isBlank();
            System.out.printf("[Sentiment] sentiment=%s topic=%s suggest=%b%n", sentiment, topic, shouldSuggest);
            return new AnalysisResult(sentiment, topic, quizTitle, shouldSuggest);
        } catch (Exception e) {
            System.err.println("[Sentiment] parse error: " + e.getMessage() + " raw=" + raw);
            return new AnalysisResult("NEUTRAL", null, null, false);
        }
    }

    private String callGroq(String userPrompt) throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(ENDPOINT);
            request.setHeader("Authorization", "Bearer " + API_KEY);
            request.setHeader("Content-Type", "application/json");

            JsonObject body = new JsonObject();
            body.addProperty("model", MODEL);
            body.addProperty("temperature", 0.1);
            body.addProperty("max_tokens", 150);

            JsonArray messages = new JsonArray();
            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.addProperty("content", userPrompt);
            messages.add(msg);
            body.add("messages", messages);

            request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            return client.execute(request, response -> {
                String json = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                return obj.getAsJsonArray("choices")
                          .get(0).getAsJsonObject()
                          .getAsJsonObject("message")
                          .get("content").getAsString().trim();
            });
        }
    }
}
