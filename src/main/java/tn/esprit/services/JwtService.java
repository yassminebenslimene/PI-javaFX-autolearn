package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT Service - Generate and validate JWT tokens in Java
 * 
 * No need for Symfony API - everything is done locally in Java.
 * Uses HMAC-SHA256 for signing.
 */
public class JwtService {
    
    // Secret key - CHANGE THIS IN PRODUCTION!
    private static final String SECRET_KEY = "autolearn-java-jwt-secret-2026-change-in-production";
    
    // Token validity: 24 hours (in seconds)
    private static final long TOKEN_LIFETIME = 86400; // 24 * 60 * 60
    
    private static final Gson GSON = new Gson();
    
    /**
     * Generate a JWT token for a user
     * 
     * @param user The authenticated user
     * @return The JWT token
     */
    public static String generateToken(User user) {
        long now = System.currentTimeMillis() / 1000;
        long expiresAt = now + TOKEN_LIFETIME;
        
        // Header
        JsonObject header = new JsonObject();
        header.addProperty("typ", "JWT");
        header.addProperty("alg", "HS256");
        
        // Payload (claims)
        JsonObject payload = new JsonObject();
        payload.addProperty("iss", "autolearn-java");           // Issuer
        payload.addProperty("aud", "autolearn-javafx");         // Audience
        payload.addProperty("iat", now);                        // Issued at
        payload.addProperty("exp", expiresAt);                  // Expiration
        payload.addProperty("userId", user.getId());
        payload.addProperty("email", user.getEmail());
        payload.addProperty("role", user.getRole());
        payload.addProperty("prenom", user.getPrenom());
        payload.addProperty("nom", user.getNom());
        
        // Add niveau if user is Etudiant
        if (user instanceof Etudiant) {
            Etudiant etudiant = (Etudiant) user;
            if (etudiant.getNiveau() != null) {
                payload.addProperty("niveau", etudiant.getNiveau());
            }
        }
        
        // Encode header and payload
        String base64UrlHeader = base64UrlEncode(header.toString());
        String base64UrlPayload = base64UrlEncode(payload.toString());
        
        // Create signature
        String signature = createSignature(base64UrlHeader + "." + base64UrlPayload);
        
        // Return complete JWT
        return base64UrlHeader + "." + base64UrlPayload + "." + signature;
    }
    
    /**
     * Validate a JWT token
     * 
     * @param token The JWT token to validate
     * @return true if valid, false if expired or invalid
     */
    public static boolean validateToken(String token) {
        try {
            // Split token into parts
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String base64UrlHeader = parts[0];
            String base64UrlPayload = parts[1];
            String providedSignature = parts[2];
            
            // Verify signature
            String expectedSignature = createSignature(base64UrlHeader + "." + base64UrlPayload);
            if (!expectedSignature.equals(providedSignature)) {
                System.err.println("[JwtService] Invalid signature");
                return false;
            }
            
            // Decode payload
            String payloadJson = base64UrlDecode(base64UrlPayload);
            JsonObject payload = GSON.fromJson(payloadJson, JsonObject.class);
            
            // Check expiration
            if (payload.has("exp")) {
                long exp = payload.get("exp").getAsLong();
                long now = System.currentTimeMillis() / 1000;
                if (now > exp) {
                    System.err.println("[JwtService] Token expired");
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("[JwtService] Validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get remaining seconds until token expiration
     * 
     * @param token The JWT token
     * @return Seconds until expiration, or -1 if invalid
     */
    public static long getExpirationSeconds(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return -1;
            
            String payloadJson = base64UrlDecode(parts[1]);
            JsonObject payload = GSON.fromJson(payloadJson, JsonObject.class);
            
            if (payload.has("exp")) {
                long exp = payload.get("exp").getAsLong();
                long now = System.currentTimeMillis() / 1000;
                return exp - now;
            }
        } catch (Exception e) {
            System.err.println("[JwtService] Error getting expiration: " + e.getMessage());
        }
        return -1;
    }
    
    /**
     * Refresh a token (generate a new one with extended expiration)
     * 
     * @param oldToken The current token
     * @return New token, or null if old token is invalid
     */
    public static String refreshToken(String oldToken) {
        if (!validateToken(oldToken)) {
            return null;
        }
        
        try {
            // Decode old payload
            String[] parts = oldToken.split("\\.");
            String payloadJson = base64UrlDecode(parts[1]);
            JsonObject oldPayload = GSON.fromJson(payloadJson, JsonObject.class);
            
            // Create new payload with new expiration
            long now = System.currentTimeMillis() / 1000;
            long expiresAt = now + TOKEN_LIFETIME;
            
            JsonObject newPayload = new JsonObject();
            newPayload.addProperty("iss", oldPayload.get("iss").getAsString());
            newPayload.addProperty("aud", oldPayload.get("aud").getAsString());
            newPayload.addProperty("iat", now);
            newPayload.addProperty("exp", expiresAt);
            newPayload.addProperty("userId", oldPayload.get("userId").getAsInt());
            newPayload.addProperty("email", oldPayload.get("email").getAsString());
            newPayload.addProperty("role", oldPayload.get("role").getAsString());
            newPayload.addProperty("prenom", oldPayload.get("prenom").getAsString());
            newPayload.addProperty("nom", oldPayload.get("nom").getAsString());
            
            if (oldPayload.has("niveau") && !oldPayload.get("niveau").isJsonNull()) {
                newPayload.addProperty("niveau", oldPayload.get("niveau").getAsString());
            }
            
            // Create new token
            JsonObject header = new JsonObject();
            header.addProperty("typ", "JWT");
            header.addProperty("alg", "HS256");
            
            String base64UrlHeader = base64UrlEncode(header.toString());
            String base64UrlPayload = base64UrlEncode(newPayload.toString());
            String signature = createSignature(base64UrlHeader + "." + base64UrlPayload);
            
            return base64UrlHeader + "." + base64UrlPayload + "." + signature;
            
        } catch (Exception e) {
            System.err.println("[JwtService] Refresh error: " + e.getMessage());
            return null;
        }
    }
    
    // ── Private helpers ───────────────────────────────────────────────────────
    
    /**
     * Create HMAC-SHA256 signature
     */
    private static String createSignature(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create signature", e);
        }
    }
    
    /**
     * Base64 URL encode (RFC 4648)
     */
    private static String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }
    
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(data);
    }
    
    /**
     * Base64 URL decode (RFC 4648)
     */
    private static String base64UrlDecode(String data) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(data);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
