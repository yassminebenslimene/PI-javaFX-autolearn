package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Service Open Food Facts — enrichissement nutritionnel des items vending.
 * API gratuite, sans clé, sans inscription.
 * Tout échec retourne NutritionInfo.empty() silencieusement.
 */
public class OpenFoodFactsService {

    private static final String BASE_URL =
            "https://world.openfoodfacts.org/cgi/search.pl?search_terms=%s&json=1&page_size=1&search_simple=1&action=process";

    /**
     * Retourne les infos nutritionnelles pour un produit.
     * @param productName nom du produit à rechercher
     * @return NutritionInfo avec calories et sucre, ou NutritionInfo.empty() si indisponible
     */
    public NutritionInfo getNutrition(String productName) {
        try {
            String encoded = java.net.URLEncoder.encode(productName, StandardCharsets.UTF_8);
            String url = String.format(BASE_URL, encoded);

            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.of(3, TimeUnit.SECONDS))
                    .setResponseTimeout(Timeout.of(3, TimeUnit.SECONDS))
                    .build();

            try (CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(config).build()) {

                HttpGet get = new HttpGet(url);
                get.setHeader("User-Agent", "AutoLearn-JavaFX/1.0");

                return client.execute(get, response -> {
                    if (response.getCode() != 200) return NutritionInfo.empty();
                    try (InputStream is = response.getEntity().getContent()) {
                        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                        JsonArray products = root.getAsJsonArray("products");
                        if (products == null || products.isEmpty()) return NutritionInfo.empty();

                        JsonObject product = products.get(0).getAsJsonObject();
                        if (!product.has("nutriments")) return NutritionInfo.empty();

                        JsonObject nutriments = product.getAsJsonObject("nutriments");
                        int calories = 0;
                        int sucre = 0;

                        if (nutriments.has("energy-kcal_100g")) {
                            calories = (int) nutriments.get("energy-kcal_100g").getAsDouble();
                        } else if (nutriments.has("energy_100g")) {
                            // Convertir kJ en kcal si nécessaire
                            calories = (int) (nutriments.get("energy_100g").getAsDouble() / 4.184);
                        }

                        if (nutriments.has("sugars_100g")) {
                            sucre = (int) nutriments.get("sugars_100g").getAsDouble();
                        }

                        return new NutritionInfo(calories, sucre);
                    }
                });
            }
        } catch (Exception e) {
            // Silencieux — l'enrichissement nutritionnel est optionnel
            return NutritionInfo.empty();
        }
    }

    /**
     * Infos nutritionnelles d'un produit (pour 100g).
     */
    public record NutritionInfo(int calories, int sucreG) {
        public static NutritionInfo empty() {
            return new NutritionInfo(0, 0);
        }

        public boolean hasData() {
            return calories > 0 || sucreG > 0;
        }
    }
}
