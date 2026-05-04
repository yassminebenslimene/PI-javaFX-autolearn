package tn.esprit.services;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class GroqService {


    private static final String API_KEY = "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";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";


    public String ask(String systemPrompt, String userMessage) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 1024);


        JsonArray msgs = new JsonArray();


        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        msgs.add(sys);


        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userMessage);
        msgs.add(usr);


        body.add("messages", msgs);


        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);


        OutputStream os = conn.getOutputStream();
        byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
        os.close();


        int code = conn.getResponseCode();
        if (code != 200) {
            BufferedReader errBr = new BufferedReader(new InputStreamReader(
                    conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8));
            StringBuilder errBody = new StringBuilder();
            String errLine;
            while ((errLine = errBr.readLine()) != null) errBody.append(errLine);
            errBr.close();
            throw new Exception("API error: " + code + " - " + errBody);
        }


        BufferedReader br = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();


        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        return json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
    }
}


