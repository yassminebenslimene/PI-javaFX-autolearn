package tn.esprit.services;

import com.google.gson.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

public class QuoteService {

    private static final String API_URL = "https://zenquotes.io/api/random";

    // Citations de secours en cas d'échec de l'API
    private static final String[][] FALLBACK_QUOTES = {
            {"La persévérance est la clé du succès.", "Napoléon Hill"},
            {"L'apprentissage est un trésor qui suivra son propriétaire partout.", "Proverbe chinois"},
            {"Le succès, c'est tomber sept fois, se relever huit.", "Proverbe japonais"},
            {"Ne remets pas à demain ce que tu peux faire aujourd'hui.", "Benjamin Franklin"},
            {"La connaissance s'acquiert par l'expérience, tout le reste n'est que de l'information.", "Albert Einstein"},
            {"Le seul endroit où le succès vient avant le travail, c'est dans le dictionnaire.", "Vince Lombardi"},
            {"Apprendre, c'est comme ramer à contre-courant : dès qu'on s'arrête, on recule.", "Proverbe chinois"},
            {"L'échec est le fondement de la réussite.", "Lao Tseu"},
            {"La motivation vous fait commencer. L'habitude vous fait continuer.", "Jim Ryun"},
            {"Chaque expert a été un débutant.", "Proverbe anglais"}
    };

    private final Random random = new Random();

    /**
     * Récupère une citation aléatoire depuis l'API ZenQuotes
     * @return Citation formatée avec auteur
     */
    public String getRandomQuote() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("User-Agent", "AutoLearn Application")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonArray jsonArray = JsonParser.parseString(response.body()).getAsJsonArray();
                JsonObject quoteObj = jsonArray.get(0).getAsJsonObject();

                String quote = quoteObj.get("q").getAsString();
                String author = quoteObj.get("a").getAsString();

                return "💬 \"" + quote + "\" — " + author;
            } else {
                return getFallbackQuote();
            }

        } catch (Exception e) {
            System.err.println("Erreur API Citation : " + e.getMessage());
            return getFallbackQuote();
        }
    }

    /**
     * Récupère une citation de motivation pour le début d'un challenge
     */
    public String getMotivationalQuote() {
        String[] motivationQuotes = {
                "🔥 " + getRandomQuoteFromList("Aujourd'hui est un nouveau jour pour apprendre !"),
                "💪 " + getRandomQuoteFromList("Tu es capable de grandes choses !"),
                "🎯 " + getRandomQuoteFromList("Concentre-toi, tu vas y arriver !"),
                "🌟 " + getRandomQuoteFromList("Chaque effort te rapproche de ton objectif."),
                "📚 " + getRandomQuoteFromList("L'apprentissage est un voyage, pas une destination.")
        };
        return motivationQuotes[random.nextInt(motivationQuotes.length)];
    }

    /**
     * Récupère une citation de félicitations pour la fin d'un challenge
     */
    public String getCongratulationQuote(int score, int totalPoints) {
        int percentage = (score * 100) / totalPoints;

        if (percentage >= 80) {
            String[] excellentQuotes = {
                    "🏆 Exceptionnel ! Tu as déchiré ce challenge !",
                    "🎉 Félicitations ! Tu es un champion !",
                    "⭐ Excellent travail ! Continue comme ça !",
                    "🚀 Tu as explosé ce challenge ! Bravo !"
            };
            return excellentQuotes[random.nextInt(excellentQuotes.length)];
        } else if (percentage >= 50) {
            String[] goodQuotes = {
                    "👍 Bien joué ! Tu progresses bien !",
                    "📈 Bon travail ! Continue tes efforts !",
                    "💪 Tu es sur la bonne voie ! Persévère !",
                    "🎯 Objectif atteint ! Bravo !"
            };
            return goodQuotes[random.nextInt(goodQuotes.length)];
        } else {
            String[] encouragementQuotes = {
                    "📚 Ne lâche rien ! Chaque erreur est une leçon.",
                    "💪 Continue à t'entraîner, tu vas y arriver !",
                    "🌟 La persévérance est la clé du succès !",
                    "🎯 Réessaye, tu feras mieux la prochaine fois !"
            };
            return encouragementQuotes[random.nextInt(encouragementQuotes.length)];
        }
    }

    /**
     * Récupère une citation de secours (hors ligne)
     */
    private String getFallbackQuote() {
        int index = random.nextInt(FALLBACK_QUOTES.length);
        return "💬 \"" + FALLBACK_QUOTES[index][0] + "\" — " + FALLBACK_QUOTES[index][1];
    }

    private String getRandomQuoteFromList(String defaultQuote) {
        // 50% de chance d'avoir une citation aléatoire réelle
        if (random.nextBoolean()) {
            try {
                return getRandomQuote().replace("💬 ", "");
            } catch (Exception e) {
                return defaultQuote;
            }
        }
        return defaultQuote;
    }
}