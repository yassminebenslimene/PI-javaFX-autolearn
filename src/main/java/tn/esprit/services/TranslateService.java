package tn.esprit.services;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class TranslateService {

    public static final String[] SUPPORTED_LANGUAGES = {
            "fr", "en", "es", "de", "it", "pt", "ar", "ru", "zh", "ja"
    };

    public static final String[] LANGUAGE_NAMES = {
            "Français", "English", "Español", "Deutsch", "Italiano",
            "Português", "العربية", "Русский", "中文", "日本語"
    };

    // API Lingva (gratuite)
    private static final String API_URL = "https://lingva.ml/api/v1/fr/";

    public String translate(String text, String targetLang) {
        if (text == null || text.isEmpty()) return text;
        if (targetLang.equals("fr")) return text;

        try {
            // Nettoyer le texte pour une meilleure traduction
            String cleanText = text
                    .replace("[EXPERT]", "")
                    .replace("[AVANCÉ]", "")
                    .replace("[DÉBUTANT]", "")
                    .replace("[INTERMÉDIAIRE]", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            String encodedText = URLEncoder.encode(cleanText, StandardCharsets.UTF_8.toString());
            String url = API_URL + targetLang + "/" + encodedText;

            System.out.println("🌐 Appel API: " + url);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "AutoLearn/1.0")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                // Extraire le texte traduit
                if (jsonResponse.contains("\"translation\"")) {
                    int start = jsonResponse.indexOf("\"translation\":\"") + 14;
                    int end = jsonResponse.indexOf("\"", start);
                    if (start > 14 && end > start) {
                        String translated = jsonResponse.substring(start, end);
                        translated = decodeUnicode(translated);
                        // Nettoyer la traduction
                        translated = translated.replace("&quot;", "\"")
                                .replace("&amp;", "&")
                                .replace("&#39;", "'");
                        System.out.println("✅ Traduction " + targetLang + " réussie");
                        return "📖 " + translated;
                    }
                }
            }

            // Fallback: traduction basique
            return basicTranslate(text, targetLang);

        } catch (Exception e) {
            System.err.println("❌ Erreur traduction: " + e.getMessage());
            return basicTranslate(text, targetLang);
        }
    }

    private String decodeUnicode(String text) {
        try {
            while (text.contains("\\u")) {
                int start = text.indexOf("\\u");
                String hex = text.substring(start + 2, start + 6);
                char c = (char) Integer.parseInt(hex, 16);
                text = text.substring(0, start) + c + text.substring(start + 6);
            }
        } catch (Exception e) {}
        return text;
    }

    private String basicTranslate(String text, String targetLang) {
        if (targetLang.equals("en")) {
            String result = text;
            result = result.replace("Qu'est-ce que", "What is");
            result = result.replace("qu'est-ce que", "what is");
            result = result.replace("Expliquez", "Explain");
            result = result.replace("expliquez", "explain");
            result = result.replace("Décrivez", "Describe");
            result = result.replace("décrivez", "describe");
            result = result.replace("Comparez", "Compare");
            result = result.replace("comparez", "compare");
            result = result.replace("Comment", "How to");
            result = result.replace("comment", "how to");
            result = result.replace("Pourquoi", "Why");
            result = result.replace("pourquoi", "why");
            result = result.replace("architecture", "architecture");
            result = result.replace("scalable", "scalable");
            result = result.replace("application", "application");
            result = result.replace("boucles", "loops");
            result = result.replace("java", "Java");
            result = result.replace("exemples", "examples");
            result = result.replace("code", "code");
            result = result.replace("concrets", "concrete");
            result = result.replace("une", "a");
            result = result.replace("le", "the");
            result = result.replace("la", "the");
            result = result.replace("les", "the");
            result = result.replace("et", "and");
            result = result.replace("avec", "with");
            result = result.replace("pour", "for");
            result = result.replace("est", "is");
            result = result.replace("sont", "are");
            result = result.replace("fort", "high");
            result = result.replace("trafic", "traffic");
            return "📖 " + result;
        }
        return "📖 [Traduction approximative] " + text;
    }

    public String getLangName(String code) {
        for (int i = 0; i < SUPPORTED_LANGUAGES.length; i++) {
            if (SUPPORTED_LANGUAGES[i].equals(code)) {
                return LANGUAGE_NAMES[i];
            }
        }
        return code;
    }
}