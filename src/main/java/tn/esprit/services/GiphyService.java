package tn.esprit.services;

import com.google.gson.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class GiphyService {

    private static final String API_KEY = "JD802eoEA02PAGbdlE7gblxBSTYIuTaS";
    private static final String API_URL = "https://api.giphy.com/v1/gifs/random";
    private final Random random = new Random();

    public GiphyService() {
        System.out.println("GiphyService initialisé");
    }

    public String getRandomGifUrl(String tag) {
        try {
            String encodedTag = URLEncoder.encode(tag, StandardCharsets.UTF_8.toString());
            String url = API_URL + "?api_key=" + API_KEY + "&tag=" + encodedTag + "&rating=g";

            System.out.println("Appel API: " + url);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "AutoLearn Application")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject data = jsonResponse.getAsJsonObject("data");

                if (data != null && !data.isJsonNull()) {
                    JsonObject images = data.getAsJsonObject("images");

                    // Essayer différentes tailles jusqu'à trouver une URL qui fonctionne
                    String[] sizes = {"downsized", "fixed_width", "fixed_height", "original"};

                    for (String size : sizes) {
                        JsonObject sizeObj = images.getAsJsonObject(size);
                        if (sizeObj != null && !sizeObj.isJsonNull()) {
                            String gifUrl = sizeObj.get("url").getAsString();
                            if (gifUrl != null && !gifUrl.isEmpty()) {
                                System.out.println("✅ GIF trouvé (" + size + ") pour tag '" + tag + "': " + gifUrl);
                                return gifUrl;
                            }
                        }
                    }
                }
            } else {
                System.err.println("❌ Erreur API GIPHY: " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'appel GIPHY: " + e.getMessage());
        }

        return getFallbackGifUrl(tag);
    }

    public String getCongratulationGif(int score, int totalPoints) {
        int percentage = (score * 100) / totalPoints;

        if (percentage >= 80) {
            String[] excellentTags = {"excellent", "amazing", "congratulations", "winner"};
            return getRandomGifUrl(excellentTags[random.nextInt(excellentTags.length)]);
        } else if (percentage >= 50) {
            String[] goodTags = {"good job", "well done", "nice", "success"};
            return getRandomGifUrl(goodTags[random.nextInt(goodTags.length)]);
        } else {
            String[] encouragementTags = {"keep going", "motivation", "encouragement"};
            return getRandomGifUrl(encouragementTags[random.nextInt(encouragementTags.length)]);
        }
    }

    private String getFallbackGifUrl(String tag) {
        // URLs de GIFs simples qui fonctionnent avec JavaFX
        String[] fallbackGifs = {
                "https://media.giphy.com/media/3o7abB06u9bNzA8LC8/giphy.gif",
                "https://media.giphy.com/media/l0MYt5jH6gk4LgR3y/giphy.gif",
                "https://media.giphy.com/media/3o6Zt481isNVuQI1l6/giphy.gif",
                "https://media.giphy.com/media/xTiTnxpQ3ghPiB2Hp6/giphy.gif"
        };
        String selected = fallbackGifs[random.nextInt(fallbackGifs.length)];
        System.out.println("📦 Utilisation du GIF de secours: " + selected);
        return selected;
    }
}