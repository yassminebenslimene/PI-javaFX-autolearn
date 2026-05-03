package tn.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service Exchange Rate — conversion de prix TND vers EUR/USD.
 * API gratuite open.er-api.com, sans clé, sans inscription.
 * Cache in-memory 1h. Fallback : Map vide (affichage TND uniquement).
 */
public class ExchangeRateService {

    private static final String URL = "https://open.er-api.com/v6/latest/TND";
    private static final long CACHE_DURATION_MS = 60 * 60 * 1000L; // 1 heure

    // Cache statique partagé
    private static Map<String, Double> cachedRates = null;
    private static long cacheTimestamp = 0L;

    /**
     * Retourne les taux de change depuis TND.
     * Ex: {"EUR": 0.29, "USD": 0.32}
     * Retourne Map vide si indisponible.
     */
    public Map<String, Double> getRates() {
        // Vérifier le cache
        long now = System.currentTimeMillis();
        if (cachedRates != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
            return cachedRates;
        }

        try {
            RequestConfig config = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.of(4, TimeUnit.SECONDS))
                    .setResponseTimeout(Timeout.of(4, TimeUnit.SECONDS))
                    .build();

            try (CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(config).build()) {

                HttpGet get = new HttpGet(URL);
                Map<String, Double> rates = client.execute(get, response -> {
                    if (response.getCode() != 200) return Map.of();
                    try (InputStream is = response.getEntity().getContent()) {
                        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                        if (!root.has("rates")) return Map.of();

                        JsonObject ratesObj = root.getAsJsonObject("rates");
                        Map<String, Double> result = new HashMap<>();

                        if (ratesObj.has("EUR")) result.put("EUR", ratesObj.get("EUR").getAsDouble());
                        if (ratesObj.has("USD")) result.put("USD", ratesObj.get("USD").getAsDouble());

                        return result;
                    }
                });

                // Mettre en cache
                cachedRates = rates;
                cacheTimestamp = now;
                return rates;
            }
        } catch (Exception e) {
            // Silencieux — affichage TND uniquement en fallback
            return Map.of();
        }
    }

    /**
     * Convertit un prix TND vers une devise cible.
     * @param prixTND prix en dinars tunisiens
     * @param devise "EUR" ou "USD"
     * @return prix converti, ou -1 si taux indisponible
     */
    public double convertir(double prixTND, String devise) {
        Map<String, Double> rates = getRates();
        if (!rates.containsKey(devise)) return -1;
        return Math.round(prixTND * rates.get(devise) * 100.0) / 100.0;
    }
}
