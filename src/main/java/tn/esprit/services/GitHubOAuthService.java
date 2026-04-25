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
 * GitHub OAuth 2.0 Service for AutoLearn
 * Handles authentication flow with GitHub
 */
public class GitHubOAuthService {

    // GitHub OAuth credentials (you provided these earlier)
    private static final String CLIENT_ID = "Ov23liaGRyNv6Q340ANg";
    private static final String CLIENT_SECRET = "83cf8926b7e97be668ec646ef08ad7d226c81684";
    private static final String REDIRECT_URI = "http://localhost:8082/callback";
    private static final int PORT = 8082;

    private static HttpServer server;
    private static CompletableFuture<Map<String, String>> authFuture;
    private static boolean isAuthenticating = false;

    /**
     * Initiates GitHub OAuth flow
     * @return CompletableFuture with user info (email, name, login)
     */
    public static CompletableFuture<Map<String, String>> authenticate() {
        // Prevent multiple simultaneous authentication attempts
        if (isAuthenticating) {
            CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
            future.completeExceptionally(new Exception("Une authentification GitHub est déjà en cours. Veuillez patienter."));
            return future;
        }

        isAuthenticating = true;
        authFuture = new CompletableFuture<>();

        try {
            // Stop any existing server first
            stopServer();

            // Small delay to let OS release the port
            Thread.sleep(300);

            // Start local HTTP server to receive callback
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/callback", GitHubOAuthService::handleCallback);
            server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "github-oauth");
                t.setDaemon(true);
                return t;
            }));
            server.start();

            // Auto-timeout after 2 minutes
            scheduleTimeout();

            // Build authorization URL
            String authUrl = "https://github.com/login/oauth/authorize?" +
                    "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                    "&scope=" + URLEncoder.encode("user:email", StandardCharsets.UTF_8);

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
            Map<String, String> tokenParams = parseQuery(tokenResponse);
            String accessToken = tokenParams.get("access_token");

            if (accessToken == null) {
                throw new Exception("No access token received");
            }

            // Get user info
            Map<String, String> userInfo = getUserInfo(accessToken);

            sendResponse(exchange, 200,
                    "<html><body style='font-family:sans-serif;text-align:center;padding:50px;'>" +
                    "<h2 style='color:#6e5494;'>✓ Connexion GitHub reussie !</h2>" +
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
        String tokenUrl = "https://github.com/login/oauth/access_token";

        String postData = "client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8) +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

        HttpURLConnection conn = (HttpURLConnection) new URI(tokenUrl).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    private static Map<String, String> getUserInfo(String accessToken) throws Exception {
        // Get user profile
        String userUrl = "https://api.github.com/user";
        HttpURLConnection conn = (HttpURLConnection) new URI(userUrl).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/vnd.github+json");

        String userResponse;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            userResponse = br.lines().collect(Collectors.joining());
        }

        JSONObject userJson = new JSONObject(userResponse);

        // Get user emails
        String emailUrl = "https://api.github.com/user/emails";
        HttpURLConnection emailConn = (HttpURLConnection) new URI(emailUrl).toURL().openConnection();
        emailConn.setRequestMethod("GET");
        emailConn.setRequestProperty("Authorization", "Bearer " + accessToken);
        emailConn.setRequestProperty("Accept", "application/vnd.github+json");

        String emailResponse;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(emailConn.getInputStream()))) {
            emailResponse = br.lines().collect(Collectors.joining());
        }

        // Parse emails and find primary
        String email = "";
        try {
            org.json.JSONArray emails = new org.json.JSONArray(emailResponse);
            for (int i = 0; i < emails.length(); i++) {
                JSONObject emailObj = emails.getJSONObject(i);
                if (emailObj.optBoolean("primary", false)) {
                    email = emailObj.optString("email", "");
                    break;
                }
            }
            // Fallback to first email if no primary
            if (email.isEmpty() && emails.length() > 0) {
                email = emails.getJSONObject(0).optString("email", "");
            }
        } catch (Exception ignored) {}

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("email", email);
        userInfo.put("name", userJson.optString("name", userJson.optString("login", "")));
        userInfo.put("login", userJson.optString("login", ""));
        
        // Split name into first and last name
        String fullName = userJson.optString("name", "");
        if (!fullName.isEmpty()) {
            String[] parts = fullName.split("\\s+", 2);
            userInfo.put("given_name", parts[0]);
            userInfo.put("family_name", parts.length > 1 ? parts[1] : "");
        } else {
            userInfo.put("given_name", userJson.optString("login", ""));
            userInfo.put("family_name", "");
        }
        
        userInfo.put("provider", "github");

        return userInfo;
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
