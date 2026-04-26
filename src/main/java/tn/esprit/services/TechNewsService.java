package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * TechNewsService — Récupère les actualités tech via GNews API.
 *
 * API gratuite : https://gnews.io  (100 req/jour)
 * Endpoint     : /api/v4/top-headlines?topic=technology&lang=fr&apikey=KEY
 *
 * Avantages vs NewsAPI :
 *  - Articles en français (lang=fr)
 *  - Topic technology direct, pas besoin de filtrer les domaines
 *  - JSON simple à parser
 */
public class TechNewsService {

    // ── Clé API GNews ─────────────────────────────────────────────────────────
    private static final String API_KEY = "6d7162607921dc42290250de2e7c2d52";
    // Recherche ciblée : IA, programmation, développement logiciel — en français
    private static final String API_URL =
        "https://gnews.io/api/v4/search?q=intelligence+artificielle+OR+programmation+OR+developpement+logiciel&lang=fr&max=6&sortby=publishedAt&apikey=" + API_KEY;

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .build();
    private static final Gson GSON = new Gson();

    // Cache simple — évite les appels répétés dans la même session
    private static List<NewsArticle> cachedArticles = null;

    // ── Modèle article ────────────────────────────────────────────────────────

    public record NewsArticle(
        String title,       // Titre de l'article
        String description, // Résumé
        String source,      // Nom de la source
        String url,         // URL de l'article
        String publishedAt  // Date de publication ISO
    ) {
        /** Résumé tronqué à 100 caractères pour l'affichage carte */
        public String getShortDescription() {
            if (description == null || description.isBlank()) return "Lire l'article complet...";
            return description.length() > 100 ? description.substring(0, 100) + "..." : description;
        }

        /** Titre tronqué à 70 caractères */
        public String getShortTitle() {
            if (title == null || title.isBlank()) return "Article sans titre";
            return title.length() > 70 ? title.substring(0, 70) + "..." : title;
        }

        /** Date formatée lisible — ex: "25 avr. 2026" */
        public String getFormattedDate() {
            if (publishedAt == null || publishedAt.length() < 10) return "";
            try {
                String[] parts = publishedAt.substring(0, 10).split("-");
                String[] months = {"", "jan.", "fév.", "mar.", "avr.", "mai", "juin",
                                   "juil.", "août", "sep.", "oct.", "nov.", "déc."};
                int month = Integer.parseInt(parts[1]);
                return parts[2] + " " + months[month] + " " + parts[0];
            } catch (Exception e) {
                return publishedAt.substring(0, 10);
            }
        }
    }

    // ── Appel API asynchrone ──────────────────────────────────────────────────

    /**
     * Récupère les articles tech en français de manière asynchrone.
     * Retourne les articles de démo en cas d'erreur (jamais null).
     */
    public static CompletableFuture<List<NewsArticle>> getTopTechNewsAsync() {
        if (cachedArticles != null) {
            return CompletableFuture.completedFuture(cachedArticles);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "AutoLearn/1.0")
                    .GET()
                    .build();

                HttpResponse<String> response = HTTP.send(request,
                    HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonObject json = GSON.fromJson(response.body(), JsonObject.class);

                    // GNews retourne { "totalArticles": N, "articles": [...] }
                    if (!json.has("articles")) {
                        System.err.println("[TechNews] Réponse inattendue : " + json);
                        return fallbackArticles();
                    }

                    JsonArray articles = json.getAsJsonArray("articles");
                    List<NewsArticle> result = new ArrayList<>();

                    for (int i = 0; i < articles.size() && i < 6; i++) {
                        JsonObject a = articles.get(i).getAsJsonObject();

                        String title       = getStr(a, "title");
                        String description = getStr(a, "description");
                        String url         = getStr(a, "url");
                        String publishedAt = getStr(a, "publishedAt");

                        // GNews : source est un objet { "name": "...", "url": "..." }
                        String source = "";
                        if (a.has("source") && !a.get("source").isJsonNull()) {
                            source = getStr(a.getAsJsonObject("source"), "name");
                        }

                        if (title.isBlank()) continue;
                        result.add(new NewsArticle(title, description, source, url, publishedAt));
                    }

                    if (result.isEmpty()) return fallbackArticles();
                    cachedArticles = result;
                    System.out.println("[TechNews] " + result.size() + " articles GNews chargés.");
                    return result;

                } else if (response.statusCode() == 403 || response.statusCode() == 401) {
                    System.err.println("[TechNews] Clé API invalide (" + response.statusCode() + "). Articles de démo affichés.");
                    return fallbackArticles();
                } else {
                    System.err.println("[TechNews] HTTP " + response.statusCode() + " — " + response.body());
                    return fallbackArticles();
                }

            } catch (Exception e) {
                System.err.println("[TechNews] Erreur : " + e.getMessage());
                return fallbackArticles();
            }
        });
    }

    /** Vide le cache (appelé au démarrage de l'app). */
    public static void clearCache() {
        cachedArticles = null;
    }

    // ── Articles de démo si l'API échoue ─────────────────────────────────────

    private static List<NewsArticle> fallbackArticles() {
        return List.of(
            new NewsArticle(
                "L'IA générative transforme le développement logiciel en 2026",
                "Les outils d'IA comme GitHub Copilot et Claude révolutionnent la façon dont les développeurs écrivent du code.",
                "Tech Actu",
                "https://www.google.com/search?q=IA+generative+developpement+logiciel+2026&tbm=nws",
                "2026-04-25T10:00:00Z"),
            new NewsArticle(
                "Java 24 : les nouvelles fonctionnalités qui changent tout",
                "La dernière version de Java apporte des améliorations majeures en termes de performance et de syntaxe moderne.",
                "Dev Magazine",
                "https://www.google.com/search?q=Java+24+nouvelles+fonctionnalites&tbm=nws",
                "2026-04-24T09:00:00Z"),
            new NewsArticle(
                "JavaFX reste incontournable pour les applications desktop",
                "Malgré la montée des frameworks web, JavaFX continue d'évoluer et reste le choix privilégié pour les apps desktop.",
                "Java Weekly",
                "https://www.google.com/search?q=JavaFX+applications+desktop+2026&tbm=nws",
                "2026-04-23T08:00:00Z"),
            new NewsArticle(
                "Python dépasse Java dans les classements de popularité",
                "Pour la première fois, Python prend la première place du classement TIOBE, devant Java et C.",
                "InfoDev",
                "https://www.google.com/search?q=Python+depasse+Java+TIOBE+2026&tbm=nws",
                "2026-04-22T07:00:00Z"),
            new NewsArticle(
                "Les bases de données vectorielles au cœur de l'IA",
                "Avec l'essor des LLMs, les bases de données vectorielles comme Pinecone et Weaviate deviennent essentielles.",
                "Data Tech",
                "https://www.google.com/search?q=bases+donnees+vectorielles+IA+2026&tbm=nws",
                "2026-04-21T06:00:00Z"),
            new NewsArticle(
                "Spring Boot 4 : migration et nouvelles fonctionnalités",
                "Spring Boot 4 arrive avec le support natif de Java 21 et des améliorations significatives de performance.",
                "Spring Blog",
                "https://www.google.com/search?q=Spring+Boot+4+migration+fonctionnalites&tbm=nws",
                "2026-04-20T05:00:00Z")
        );
    }

    private static String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
            ? json.get(key).getAsString() : "";
    }
}
