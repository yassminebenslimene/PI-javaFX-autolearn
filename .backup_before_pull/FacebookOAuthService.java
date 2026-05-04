package tn.esprit.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Facebook OAuth 2.0 Service for AutoLearn
 * Handles authentication flow with Facebook
 */
public class FacebookOAuthService {

    // TODO: Replace with your actual Facebook App credentials
    private static final String APP_ID = "26377871941908258";
    private static final String APP_SECRET = "0f5bd349830d9dd3a126653076a23329";
    private static final String REDIRECT_URI = "http://localhost:8081/callback";
    private static final int PORT = 8081;

    private static HttpServer server;
    private static CompletableFuture<Map<String, String>> authFuture;
    private static boolean isAuthenticating = false;

    /**
     * Initiates Facebook OAuth flow
     * @return CompletableFuture with user info (email, name, first_name, last_name)
     */
    public static CompletableFuture<Map<String, String>> authenticate() {
        // Prevent multiple simultaneous authentication attempts
        if (isAuthenticating) {
            CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
            future.completeExceptionally(new Exception("Une authentification Facebook est déjà en cours. Veuillez patienter."));
            return future;
        }

        isAuthenticating = true;
        authFuture = new CompletableFuture<>();

        try {
            // Stop any existing server first
            stopServer();

            // Small delay to let OS release the port
            Thread.sleep(300);

            // Start local HTTP server with SO_REUSEADDR to avoid BindException
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/callback", FacebookOAuthService::handleCallback);
            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "facebook-oauth");
                t.setDaemon(true);
                return t;
            }));
            server.start();

            // Auto-timeout after 2 minutes
            scheduleTimeout();

            // Build authorization URL
            String authUrl = "https://www.facebook.com/v18.0/dialog/oauth?" +
                    "client_id=" + URLEncoder.encode(APP_ID, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode("email,public_profile", StandardCharsets.UTF_8) +
                    "&response_type=code";

            // Open browser
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(authUrl));
            } else {
                System.out.println("Please open this URL in your browser:");
                System.out.println(authUrl);
            }

        } catch (Exception e) {
            authFuture.completeExceptionally(e);
            stopServer();
            isAuthenticating = false;
        }

        return authFuture;
    }

    private static void scheduleTimeout() {
        // Auto-close server after 2 minutes if no response
        new Thread(() -> {
            try {
                Thread.sleep(120000); // 2 minutes
                if (isAuthenticating && !authFuture.isDone()) {
                    authFuture.completeExceptionally(new Exception("Timeout: Aucune réponse après 2 minutes"));
                    stopServer();
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private static void handleCallback(HttpExchange exchange) {
        try {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(query);

            if (params.containsKey("error")) {
                sendResponse(exchange, 400, "Authentication failed: " + params.get("error_description"));
                authFuture.completeExceptionally(new Exception("User denied access"));
                stopServer();
                return;
            }

            String code = params.get("code");
            if (code == null) {
                sendResponse(exchange, 400, "No authorization code received");
                authFuture.completeExceptionally(new Exception("No code"));
                stopServer();
                return;
            }

            // Exchange code for access token
            String tokenResponse = exchangeCodeForToken(code);
            JSONObject tokenJson = new JSONObject(tokenResponse);
            String accessToken = tokenJson.getString("access_token");

            // Get user info
            Map<String, String> userInfo = getUserInfo(accessToken);

            sendResponse(exchange, 200,
                    "<html><body style='font-family:sans-serif;text-align:center;padding:50px;'>" +
                    "<h2 style='color:#1877f2;'>✓ Connexion Facebook reussie !</h2>" +
                    "<p>Vous pouvez fermer cette fenetre et retourner a AutoLearn.</p>" +
                    "</body></html>");

            authFuture.complete(userInfo);
            stopServer();

        } catch (Exception e) {
            try {
                sendResponse(exchange, 500, "Error: " + e.getMessage());
            } catch (Exception ignored) {}
            authFuture.completeExceptionally(e);
            stopServer();
        }
    }

    private static String exchangeCodeForToken(String code) throws Exception {
        String tokenUrl = "https://graph.facebook.com/v18.0/oauth/access_token?" +
                "client_id=" + URLEncoder.encode(APP_ID, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(APP_SECRET, StandardCharsets.UTF_8) +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URI(tokenUrl).toURL().openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    private static Map<String, String> getUserInfo(String accessToken) throws Exception {
        String userInfoUrl = "https://graph.facebook.com/me?fields=id,name,email,first_name,last_name&access_token=" +
                URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URI(userInfoUrl).toURL().openConnection();
        conn.setRequestMethod("GET");

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String response = br.lines().collect(Collectors.joining());
            JSONObject json = new JSONObject(response);

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("email", json.optString("email", ""));
            userInfo.put("name", json.optString("name", ""));
            userInfo.put("given_name", json.optString("first_name", ""));
            userInfo.put("family_name", json.optString("last_name", ""));
            userInfo.put("provider", "facebook");

            return userInfo;
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }
        return params;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws Exception {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void stopServer() {
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception ignored) {}
            server = null;
        }
        isAuthenticating = false;
    }
}
