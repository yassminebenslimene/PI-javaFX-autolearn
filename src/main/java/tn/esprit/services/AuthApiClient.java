package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Authentication API client for JWT-based login.
 * 
 * Communicates with Symfony AuthApiController to:
 * - Login and get JWT token
 * - Validate JWT token
 * - Refresh JWT token
 */
public class AuthApiClient {
    
    private static final String BASE_URL = "http://localhost:8000";
    private static final Gson GSON = new Gson();
    
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    
    // ── Login Response ────────────────────────────────────────────────────────
    
    public record LoginResponse(
        boolean success,
        String token,
        int expiresIn,
        UserInfo user,
        String error,
        String reason  // For suspended accounts
    ) {}
    
    public record UserInfo(
        int id,
        String email,
        String role,
        String prenom,
        String nom,
        String niveau
    ) {}
    
    // ── Login ─────────────────────────────────────────────────────────────────
    
    /**
     * Login with email and password, get JWT token.
     * 
     * @param email User email
     * @param password User password
     * @return LoginResponse with token if successful, or error message
     */
    public static LoginResponse login(String email, String password) {
        try {
            // Build request body
            JsonObject body = new JsonObject();
            body.addProperty("email", email);
            body.addProperty("password", password);
            
            // Send POST request
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
            
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("[AuthAPI] Login → HTTP " + resp.statusCode());
            
            // Parse response
            JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
            
            if (resp.statusCode() == 200 && json.has("success") && json.get("success").getAsBoolean()) {
                // Success
                String token = json.get("token").getAsString();
                int expiresIn = json.get("expiresIn").getAsInt();
                
                JsonObject userJson = json.getAsJsonObject("user");
                UserInfo user = new UserInfo(
                    userJson.get("id").getAsInt(),
                    userJson.get("email").getAsString(),
                    userJson.get("role").getAsString(),
                    userJson.get("prenom").getAsString(),
                    userJson.get("nom").getAsString(),
                    userJson.has("niveau") && !userJson.get("niveau").isJsonNull() 
                        ? userJson.get("niveau").getAsString() : null
                );
                
                return new LoginResponse(true, token, expiresIn, user, null, null);
            } else {
                // Error
                String error = json.has("error") ? json.get("error").getAsString() : "Unknown error";
                String reason = json.has("reason") ? json.get("reason").getAsString() : null;
                return new LoginResponse(false, null, 0, null, error, reason);
            }
            
        } catch (Exception e) {
            System.err.println("[AuthAPI] Login failed: " + e.getMessage());
            return new LoginResponse(false, null, 0, null, 
                "Connection error: " + e.getMessage(), null);
        }
    }
    
    // ── Validate Token ────────────────────────────────────────────────────────
    
    /**
     * Validate a JWT token.
     * 
     * @param token The JWT token to validate
     * @return true if valid, false if expired or invalid
     */
    public static boolean validateToken(String token) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/validate"))
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() == 200) {
                JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
                return json.has("valid") && json.get("valid").getAsBoolean();
            }
            
            return false;
            
        } catch (Exception e) {
            System.err.println("[AuthAPI] Validate failed: " + e.getMessage());
            return false;
        }
    }
    
    // ── Refresh Token ─────────────────────────────────────────────────────────
    
    /**
     * Refresh a JWT token (get a new one before expiration).
     * 
     * @param oldToken The current JWT token
     * @return New JWT token, or null if refresh failed
     */
    public static String refreshToken(String oldToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/auth/refresh"))
                .header("Authorization", "Bearer " + oldToken)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (resp.statusCode() == 200) {
                JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    String newToken = json.get("token").getAsString();
                    System.out.println("[AuthAPI] Token refreshed successfully");
                    return newToken;
                }
            }
            
            System.err.println("[AuthAPI] Token refresh failed: HTTP " + resp.statusCode());
            return null;
            
        } catch (Exception e) {
            System.err.println("[AuthAPI] Refresh failed: " + e.getMessage());
            return null;
        }
    }
}
