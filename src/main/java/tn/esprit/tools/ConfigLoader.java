package tn.esprit.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties;

    static {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getGitHubToken() {
        String token = properties.getProperty("github.api.token", "");
        // Si le token n'est pas configuré, retourner null
        if (token.isEmpty() || token.equals("METS_TON_TOKEN_ICI")) {
            return null;
        }
        return token;
    }

    public static String getGroqApiKey() {
        String key = properties.getProperty("groq.api.key", "");
        if (key.isEmpty() || key.equals("METS_TA_CLE_GROQ_ICI")) {
            return null;
        }
        return key;
    }

    public static String getGroqModel() {
        return properties.getProperty("groq.model", "llama-3.3-70b-versatile");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
