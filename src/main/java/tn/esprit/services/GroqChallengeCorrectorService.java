package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GroqChallengeCorrectorService — Correction intelligente des réponses de challenge.
 *
 * Utilise le même modèle Groq (Llama 4 Scout) que le correcteur de quiz.
 * Analyse sémantique : comprend le sens de la réponse, pas juste les mots-clés.
 * Attribution de points partielle si la réponse est partiellement correcte.
 */
public class GroqChallengeCorrectorService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String API_KEY      = "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";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();
    private static final Gson GSON = new Gson();

    // ── Résultat de correction ────────────────────────────────────────────────

    /**
     * Résultat de la correction d'une réponse d'exercice.
     *
     * @param pointsObtenus  Points attribués (0 à maxPoints)
     * @param pourcentage    Pourcentage de réussite (0-100)
     * @param feedback       Feedback détaillé et pédagogique
     * @param pointsForts    Ce que l'étudiant a bien compris
     * @param pointsManques  Ce qui manque ou est incorrect
     * @param conseil        Conseil pour améliorer
     * @param isCorrect      true si réponse correcte ou quasi-correcte (>= 70%)
     */
    public record CorrectionResult(
        int pointsObtenus,
        int pourcentage,
        String feedback,
        String pointsForts,
        String pointsManques,
        String conseil,
        boolean isCorrect
    ) {}

    /**
     * Résumé global du challenge.
     */
    public record ChallengeResume(
        String messageGeneral,
        List<String> pointsForts,
        List<String> pointsAmeliorer,
        List<String> recommandations,
        String encouragement,
        int scoreTotal,
        int scoreMax,
        double pourcentage
    ) {}

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Corrige une réponse d'exercice de manière intelligente.
     * Analyse le sens, pas juste les mots-clés.
     *
     * @param question    La question posée
     * @param bonneReponse La réponse attendue/correcte
     * @param reponseUser  La réponse de l'étudiant
     * @param maxPoints    Points maximum pour cet exercice
     */
    public CompletableFuture<CorrectionResult> corrigerReponse(
            String question, String bonneReponse, String reponseUser, int maxPoints) {

        return CompletableFuture.supplyAsync(() -> {
            if (reponseUser == null || reponseUser.trim().isEmpty()) {
                return new CorrectionResult(0, 0,
                    "❌ Aucune réponse fournie.",
                    "", "Réponse manquante",
                    "Prenez le temps de répondre à chaque question.", false);
            }

            try {
                return appellerGroqCorrection(question, bonneReponse, reponseUser, maxPoints);
            } catch (Exception e) {
                System.err.println("[ChallengeAI] Erreur correction: " + e.getMessage());
                return fallbackCorrection(reponseUser, maxPoints);
            }
        });
    }

    /**
     * Génère le résumé pédagogique global du challenge.
     *
     * @param corrections  Map question → CorrectionResult
     * @param scoreTotal   Score total obtenu
     * @param scoreMax     Score maximum possible
     */
    public CompletableFuture<ChallengeResume> genererResume(
            Map<String, CorrectionResult> corrections,
            int scoreTotal, int scoreMax) {

        return CompletableFuture.supplyAsync(() -> {
            double pct = scoreMax > 0 ? (scoreTotal * 100.0 / scoreMax) : 0;
            try {
                return appellerGroqResume(corrections, scoreTotal, scoreMax, pct);
            } catch (Exception e) {
                System.err.println("[ChallengeAI] Erreur résumé: " + e.getMessage());
                return fallbackResume(scoreTotal, scoreMax, pct);
            }
        });
    }

    // ── Appels Groq ───────────────────────────────────────────────────────────

    private CorrectionResult appellerGroqCorrection(
            String question, String bonneReponse, String reponseUser, int maxPoints) throws Exception {

        String prompt = """
            Tu es un professeur expert qui corrige des réponses d'étudiants de manière intelligente et bienveillante.
            
            QUESTION: %s
            
            RÉPONSE ATTENDUE: %s
            
            RÉPONSE DE L'ÉTUDIANT: %s
            
            POINTS MAXIMUM: %d
            
            INSTRUCTIONS D'ÉVALUATION:
            - Analyse le SENS et la COMPRÉHENSION, pas juste les mots exacts
            - Si la réponse a le même sens que la bonne réponse → 100%% des points
            - Si la réponse est partiellement correcte (manque des détails) → 50-80%% des points
            - Si la réponse montre une compréhension partielle → 20-50%% des points
            - Si la réponse est hors sujet ou incorrecte → 0-20%% des points
            - Sois généreux si l'étudiant montre qu'il comprend le concept principal
            
            RÉPONDS UNIQUEMENT en JSON avec ce format EXACT:
            {
              "points_obtenus": <nombre entre 0 et %d>,
              "pourcentage": <nombre entre 0 et 100>,
              "feedback": "Feedback détaillé et encourageant (2-3 phrases)",
              "points_forts": "Ce que l'étudiant a bien compris",
              "points_manques": "Ce qui manque ou pourrait être amélioré (vide si parfait)",
              "conseil": "Conseil pratique pour progresser"
            }
            """.formatted(question, bonneReponse, reponseUser, maxPoints, maxPoints);

        String responseBody = appellerGroq(
            "Tu es un professeur bienveillant qui évalue les réponses de manière intelligente et équitable. " +
            "Tu réponds UNIQUEMENT en JSON valide.",
            prompt, 600
        );

        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        String content = json.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();

        JsonObject result = GSON.fromJson(content, JsonObject.class);

        int points = result.has("points_obtenus") ? result.get("points_obtenus").getAsInt() : 0;
        int pct    = result.has("pourcentage")    ? result.get("pourcentage").getAsInt()    : 0;

        // Sécurité : ne pas dépasser le max
        points = Math.min(points, maxPoints);
        pct    = Math.min(pct, 100);

        return new CorrectionResult(
            points, pct,
            getStr(result, "feedback"),
            getStr(result, "points_forts"),
            getStr(result, "points_manques"),
            getStr(result, "conseil"),
            pct >= 70
        );
    }

    private ChallengeResume appellerGroqResume(
            Map<String, CorrectionResult> corrections,
            int scoreTotal, int scoreMax, double pct) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("Génère un bilan pédagogique pour un étudiant qui vient de terminer un challenge.\n\n");
        sb.append("SCORE: ").append(scoreTotal).append("/").append(scoreMax)
          .append(" (").append(String.format("%.0f", pct)).append("%)\n\n");
        sb.append("DÉTAIL DES RÉPONSES:\n");

        for (Map.Entry<String, CorrectionResult> entry : corrections.entrySet()) {
            CorrectionResult c = entry.getValue();
            sb.append("- Question: ").append(entry.getKey()).append("\n");
            sb.append("  Score: ").append(c.pourcentage()).append("% | ");
            sb.append(c.isCorrect() ? "✅ Correct" : "❌ À revoir").append("\n");
        }

        sb.append("""
            
            RÉPONDS en JSON avec ce format EXACT:
            {
              "message_general": "Message d'ouverture adapté au score (1-2 phrases)",
              "points_forts": ["Point fort 1", "Point fort 2"],
              "points_ameliorer": ["Domaine à revoir 1", "Domaine 2"],
              "recommandations": ["Conseil pratique 1", "Conseil 2", "Conseil 3"],
              "encouragement": "Message final motivant et personnalisé"
            }
            """);

        String responseBody = appellerGroq(
            "Tu es un professeur qui fait un bilan pédagogique personnalisé et motivant. " +
            "Tu réponds UNIQUEMENT en JSON valide.",
            sb.toString(), 500
        );

        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        String content = json.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();

        JsonObject resume = GSON.fromJson(content, JsonObject.class);

        return new ChallengeResume(
            getStr(resume, "message_general"),
            getStrList(resume, "points_forts"),
            getStrList(resume, "points_ameliorer"),
            getStrList(resume, "recommandations"),
            getStr(resume, "encouragement"),
            scoreTotal, scoreMax, pct
        );
    }

    private String appellerGroq(String systemPrompt, String userPrompt, int maxTokens) throws Exception {
        String body = GSON.toJson(Map.of(
            "model", MODEL,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "temperature", 0.4,
            "max_tokens", maxTokens,
            "response_format", Map.of("type", "json_object")
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_API_URL))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Groq API HTTP " + response.statusCode());
        return response.body();
    }

    // ── Fallbacks ─────────────────────────────────────────────────────────────

    private CorrectionResult fallbackCorrection(String reponseUser, int maxPoints) {
        boolean hasContent = reponseUser.length() > 20;
        int points = hasContent ? maxPoints / 2 : 0;
        return new CorrectionResult(
            points, hasContent ? 50 : 0,
            hasContent ? "Réponse reçue. Analyse IA temporairement indisponible." : "Réponse trop courte.",
            hasContent ? "Vous avez fourni une réponse." : "",
            hasContent ? "Développez davantage votre réponse." : "Aucune réponse fournie.",
            "Réessayez avec plus de détails.",
            false
        );
    }

    private ChallengeResume fallbackResume(int scoreTotal, int scoreMax, double pct) {
        String msg = pct >= 75 ? "Excellent travail !" : pct >= 50 ? "Bon travail !" : "Continuez vos efforts !";
        return new ChallengeResume(
            msg + " Score: " + scoreTotal + "/" + scoreMax,
            List.of("Participation active"),
            List.of("Révisez les questions manquées"),
            List.of("Relisez le cours", "Pratiquez davantage"),
            "Chaque effort vous rapproche de la maîtrise ! 💪",
            scoreTotal, scoreMax, pct
        );
    }

    // ── Helpers JSON ──────────────────────────────────────────────────────────

    private String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
            ? json.get(key).getAsString() : "";
    }

    private List<String> getStrList(JsonObject json, String key) {
        List<String> list = new java.util.ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            json.getAsJsonArray(key).forEach(e -> {
                if (!e.isJsonNull()) list.add(e.getAsString());
            });
        }
        return list;
    }
}
