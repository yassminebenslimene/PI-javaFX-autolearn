package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GroqTranslationService {
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final String apiKey;
    private final String model;
    private final Gson gson;

    public GroqTranslationService(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.gson = new Gson();
    }

    /**
     * Traduire un texte vers une langue cible
     */
    public String translate(String text, String targetLanguage) {
        try {
            String prompt = buildTranslationPrompt(text, targetLanguage);
            return callGroqAPI(prompt);
        } catch (Exception e) {
            System.err.println("Erreur de traduction: " + e.getMessage());
            e.printStackTrace();
            return "Erreur de traduction: " + e.getMessage();
        }
    }

    /**
     * Détecter la langue d'un texte
     */
    public String detectLanguage(String text) {
        try {
            String prompt = "Détecte la langue de ce texte et réponds UNIQUEMENT avec le nom de la langue en français (ex: Français, Anglais, Arabe):\n\n" + text;
            return callGroqAPI(prompt).trim();
        } catch (Exception e) {
            return "Inconnu";
        }
    }

    private String buildTranslationPrompt(String text, String targetLanguage) {
        // Nettoyer le nom de la langue cible
        String cleanLanguage = targetLanguage;
        if (targetLanguage.contains("Français")) {
            cleanLanguage = "français";
        } else if (targetLanguage.contains("English")) {
            cleanLanguage = "anglais";
        } else if (targetLanguage.contains("Arabic") || targetLanguage.contains("العربية")) {
            cleanLanguage = "arabe";
        }
        
        return String.format(
            "Tu es un traducteur professionnel. Traduis OBLIGATOIREMENT le texte suivant en %s. " +
            "IMPORTANT: Tu DOIS traduire le texte, même s'il est déjà dans une autre langue. " +
            "Ne réponds QUE avec la traduction, sans explications ni commentaires.\n\n" +
            "Texte à traduire:\n%s",
            cleanLanguage, text
        );
    }

    private String callGroqAPI(String prompt) throws Exception {
        URL url = new URL(GROQ_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        // Construire le payload JSON
        JsonObject payload = new JsonObject();
        payload.addProperty("model", model);
        
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        
        payload.add("messages", messages);
        payload.addProperty("temperature", 0.3);
        payload.addProperty("max_tokens", 2000);

        // Envoyer la requête
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Lire la réponse avec encodage UTF-8 explicite
        int responseCode = conn.getResponseCode();
        System.out.println("Groq API Response Code: " + responseCode);

        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new Exception("Groq API Error: " + errorResponse.toString());
        }

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        // Parser la réponse JSON
        JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);
        JsonArray choices = jsonResponse.getAsJsonArray("choices");
        if (choices != null && choices.size() > 0) {
            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject messageObj = firstChoice.getAsJsonObject("message");
            return messageObj.get("content").getAsString();
        }

        throw new Exception("Aucune réponse de l'API Groq");
    }
}
