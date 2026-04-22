package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Service météo utilisant l'API OpenWeatherMap.
 * - Si l'événement est dans <= 5 jours : prévision précise (forecast)
 * - Sinon : météo actuelle comme référence
 */
public class WeatherService {

    private static final String API_KEY = "bd5e378503939ddaee76f12ad7a97608";
    private static final String CURRENT_URL =
            "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=en";
    private static final String FORECAST_URL =
            "https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric&lang=en";

    /**
     * Retourne les données météo pour un événement.
     * @param city  ville (ex: "Tunis,TN")
     * @param eventDate date de début de l'événement
     * @return Map avec les clés: available, is_forecast, temperature, feels_like,
     *         description, icon, humidity, wind_speed, city, error, message
     */
    public Map<String, Object> getWeatherForEvent(String city, LocalDateTime eventDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
            LocalDateTime eventDay = eventDate.truncatedTo(java.time.temporal.ChronoUnit.DAYS);
            long daysUntil = ChronoUnit.DAYS.between(now, eventDay);

            if (daysUntil > 5 || daysUntil < 0) {
                // Météo actuelle
                String json = fetch(String.format(CURRENT_URL, encode(city), API_KEY));
                if (json == null) { result.put("available", false); return result; }
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                result.put("available", true);
                result.put("is_forecast", false);
                result.put("temperature", Math.round(data.getAsJsonObject("main").get("temp").getAsFloat()));
                result.put("feels_like", Math.round(data.getAsJsonObject("main").get("feels_like").getAsFloat()));
                result.put("description", capitalize(data.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString()));
                result.put("icon", data.getAsJsonArray("weather").get(0).getAsJsonObject().get("icon").getAsString());
                result.put("humidity", data.getAsJsonObject("main").get("humidity").getAsInt());
                result.put("wind_speed", Math.round(data.getAsJsonObject("wind").get("speed").getAsFloat() * 3.6 * 10.0) / 10.0);
                result.put("city", data.get("name").getAsString());
            } else {
                // Prévision
                String json = fetch(String.format(FORECAST_URL, encode(city), API_KEY));
                if (json == null) { result.put("available", false); return result; }
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                JsonArray list = data.getAsJsonArray("list");
                long targetTs = eventDate.toEpochSecond(java.time.ZoneOffset.UTC);
                JsonObject closest = null;
                long minDiff = Long.MAX_VALUE;
                for (int i = 0; i < list.size(); i++) {
                    JsonObject item = list.get(i).getAsJsonObject();
                    long diff = Math.abs(item.get("dt").getAsLong() - targetTs);
                    if (diff < minDiff) { minDiff = diff; closest = item; }
                }
                if (closest == null) { result.put("available", false); return result; }
                result.put("available", true);
                result.put("is_forecast", true);
                result.put("temperature", Math.round(closest.getAsJsonObject("main").get("temp").getAsFloat()));
                result.put("feels_like", Math.round(closest.getAsJsonObject("main").get("feels_like").getAsFloat()));
                result.put("description", capitalize(closest.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString()));
                result.put("icon", closest.getAsJsonArray("weather").get(0).getAsJsonObject().get("icon").getAsString());
                result.put("humidity", closest.getAsJsonObject("main").get("humidity").getAsInt());
                result.put("wind_speed", Math.round(closest.getAsJsonObject("wind").get("speed").getAsFloat() * 3.6 * 10.0) / 10.0);
                result.put("city", data.getAsJsonObject("city").get("name").getAsString());
            }
        } catch (Exception e) {
            result.put("available", false);
            result.put("error", true);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** Retourne l'emoji correspondant au code icône OpenWeatherMap */
    public String getWeatherEmoji(String icon) {
        if (icon == null) return "🌤";
        return switch (icon) {
            case "01d" -> "☀️";
            case "01n" -> "🌙";
            case "02d", "02n" -> "⛅";
            case "03d", "03n", "04d", "04n" -> "☁️";
            case "09d", "09n" -> "🌧️";
            case "10d" -> "🌦️";
            case "10n" -> "🌧️";
            case "11d", "11n" -> "⛈️";
            case "13d", "13n" -> "❄️";
            case "50d", "50n" -> "🌫️";
            default -> "🌤️";
        };
    }

    private String fetch(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() != 200) return null;
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
