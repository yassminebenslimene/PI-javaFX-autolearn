package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GroqService {

    private static final String API_KEY = "System.getProperty("GROQ_API_KEY", "YOUR_GROQ_KEY")";
    private static final String MODEL   = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final Gson gson = new Gson();

    public String ask(String systemPrompt, String userPrompt) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 2048);

        JsonArray messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        messages.add(sys);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userPrompt);
        messages.add(user);

        body.add("messages", messages);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(GROQ_URL);
            post.setHeader("Authorization", "Bearer " + API_KEY);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON));

            return client.execute(post, response -> {
                try (InputStream is = response.getEntity().getContent()) {
                    String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    JsonObject resp = gson.fromJson(json, JsonObject.class);
                    if (resp.has("error")) {
                        throw new RuntimeException("Groq API error: " + resp.get("error").toString());
                    }
                    return resp.getAsJsonArray("choices")
                               .get(0).getAsJsonObject()
                               .getAsJsonObject("message")
                               .get("content").getAsString();
                }
            });
        }
    }
}