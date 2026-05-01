package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * GeoLocationService — Détecte la localisation de l'utilisateur via ip-api.com.
 *
 * API gratuite, sans clé : http://ip-api.com/json/
 * Retourne : pays, ville, région, timezone, drapeau emoji, etc.
 *
 * Utilisé pour personnaliser le message de bienvenue sur la page d'accueil.
 * Exemple : "Bienvenue depuis Tunis, Tunisie 🇹🇳 !"
 */
public class GeoLocationService {

    private static final String API_URL = "http://ip-api.com/json/";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final Gson GSON = new Gson();

    // Cache pour éviter plusieurs appels API
    private static GeoInfo cachedInfo = null;

    // ── Classe de résultat ────────────────────────────────────────────────────

    public record GeoInfo(
        String city,        // ex: "Tunis"
        String region,      // ex: "Tunis Governorate"
        String country,     // ex: "Tunisia"
        String countryCode, // ex: "TN"
        String timezone,    // ex: "Africa/Tunis"
        String ip           // ex: "197.x.x.x"
    ) {
        /**
         * Retourne l'emoji drapeau du pays basé sur le code pays ISO.
         * Fonctionne en convertissant les lettres en Regional Indicator Symbols.
         */
        public String getFlagEmoji() {
            if (countryCode == null || countryCode.length() != 2) return "🌍";
            int offset = 0x1F1E6 - 'A';
            int first  = countryCode.charAt(0) + offset;
            int second = countryCode.charAt(1) + offset;
            return new String(Character.toChars(first)) + new String(Character.toChars(second));
        }

        /**
         * Message de bienvenue personnalisé.
         * Ex: "Bienvenue depuis Tunis, Tunisie 🇹🇳"
         */
        public String getBienvenueMessage(String prenom) {
            String flag = getFlagEmoji();
            if (city != null && !city.isBlank() && country != null && !country.isBlank()) {
                return "Bienvenue, " + prenom + " ! Connecté depuis " + city + ", " + country + " " + flag;
            } else if (country != null && !country.isBlank()) {
                return "Bienvenue, " + prenom + " ! Connecté depuis " + country + " " + flag;
            }
            return "Bienvenue, " + prenom + " ! Prêt à apprendre aujourd'hui !";
        }
    }

    // ── Appel API asynchrone ──────────────────────────────────────────────────

    /**
     * Récupère les informations de géolocalisation de manière asynchrone.
     * Utilise un cache pour éviter les appels répétés.
     *
     * @return CompletableFuture<GeoInfo> — null si erreur ou timeout
     */
    public static CompletableFuture<GeoInfo> getLocationAsync() {
        // Retourner le cache si disponible
        if (cachedInfo != null) {
            return CompletableFuture.completedFuture(cachedInfo);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 AutoLearn/1.0")
                    .GET()
                    .build();

                HttpResponse<String> response = HTTP.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);

                    // Vérifier que ce n'est pas une erreur (ip-api.com retourne status:"fail")
                    if (json.has("status") && "fail".equals(json.get("status").getAsString())) {
                        System.err.println("[GeoLocation] API error: " + json);
                        return null;
                    }

                    // ip-api.com field names
                    String city        = getStr(json, "city");
                    String region      = getStr(json, "regionName");
                    String country     = getStr(json, "country");
                    String countryCode = getStr(json, "countryCode");
                    String timezone    = getStr(json, "timezone");
                    String ip          = getStr(json, "query");

                    cachedInfo = new GeoInfo(city, region, country, countryCode, timezone, ip);
                    System.out.println("[GeoLocation] Détecté : " + city + ", " + country
                        + " " + cachedInfo.getFlagEmoji());
                    return cachedInfo;
                }
            } catch (java.net.ConnectException e) {
                System.err.println("[GeoLocation] Pas de connexion internet");
            } catch (Exception e) {
                System.err.println("[GeoLocation] Erreur : " + e.getMessage());
            }
            return null;
        });
    }

    /** Vide le cache (utile pour les tests). */
    public static void clearCache() {
        cachedInfo = null;
    }

    private static String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
            ? json.get(key).getAsString() : "";
    }
}
