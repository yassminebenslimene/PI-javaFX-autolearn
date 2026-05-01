package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * GroqQuizCorrectorService — génère des explications IA pour chaque réponse du quiz.
 * Équivalent Java du QuizCorrectorAIService Symfony.
 *
 * Pour chaque question : 1 appel Groq → explication personnalisée (correct ou incorrect)
 * Pour le bilan global : 1 appel Groq → résumé pédagogique
 */
public class GroqQuizCorrectorService {

    // Même API et modèle que le générateur de quiz
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String API_KEY      = "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";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .build();
    private static final Gson GSON = new Gson();

    // ── Modèle de résultat ────────────────────────────────────────────────────

    /** Explication IA pour une question */
    public record ExplicationQuestion(
        String message,           // Message principal (encouragement ou erreur)
        String pourquoiIncorrect, // Pourquoi la réponse est fausse (vide si correct)
        String pourquoiCorrect,   // Explication de la bonne réponse
        String conseil,           // Conseil pratique
        List<String> ressources,  // Suggestions pour aller plus loin
        boolean isCorrect         // true = bonne réponse
    ) {}

    /** Résumé pédagogique global */
    public record ResumePedagogique(
        String messageGeneral,        // Message adapté au score
        List<String> pointsForts,     // Ce qui a été bien réussi
        List<String> pointsAmeliorer, // Ce qui doit être revu
        List<String> conseilsRevision,// Conseils concrets
        String encouragement          // Message final motivant
    ) {}

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Génère les explications pour toutes les questions de manière asynchrone.
     * Retourne une Map questionId → ExplicationQuestion.
     *
     * @param questions       liste des questions du quiz
     * @param reponsesChoisies Map questionId → optionId choisie par l'étudiant
     * @param optionsParQuestion Map questionId → liste des options
     */
    public CompletableFuture<Map<Integer, ExplicationQuestion>> genererExplications(
            List<Question> questions,
            Map<Integer, Integer> reponsesChoisies,
            Map<Integer, List<Option>> optionsParQuestion) {

        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, ExplicationQuestion> result = new HashMap<>();

            for (Question q : questions) {
                Integer choisiId = reponsesChoisies.get(q.getId());
                List<Option> opts = optionsParQuestion.getOrDefault(q.getId(), new ArrayList<>());

                // Trouver l'option choisie et la bonne réponse
                Option optionChoisie = null;
                Option optionCorrecte = null;
                for (Option o : opts) {
                    if (choisiId != null && o.getId() == choisiId.intValue()) optionChoisie = o;
                    if (o.isEstCorrecte()) optionCorrecte = o;
                }

                boolean isCorrect = optionChoisie != null && optionChoisie.isEstCorrecte();

                try {
                    ExplicationQuestion expl = appellerGroqPourQuestion(q, optionChoisie, optionCorrecte, isCorrect);
                    result.put(q.getId(), expl);
                } catch (Exception e) {
                    // Fallback sans IA si l'appel échoue
                    System.err.println("[CorrectorAI] Erreur question " + q.getId() + " : " + e.getMessage());
                    result.put(q.getId(), fallbackQuestion(isCorrect, optionCorrecte));
                }
            }

            return result;
        });
    }

    /**
     * Génère le résumé pédagogique global de manière asynchrone.
     *
     * @param questions        liste des questions
     * @param reponsesChoisies Map questionId → optionId choisie
     * @param optionsParQuestion Map questionId → options
     * @param percentage       pourcentage de réussite (0-100)
     */
    public CompletableFuture<ResumePedagogique> genererResume(
            List<Question> questions,
            Map<Integer, Integer> reponsesChoisies,
            Map<Integer, List<Option>> optionsParQuestion,
            double percentage) {

        return CompletableFuture.supplyAsync(() -> {
            // Calculer les stats
            int total = questions.size();
            int correct = 0;
            List<String> questionsManquees = new ArrayList<>();

            for (Question q : questions) {
                Integer choisiId = reponsesChoisies.get(q.getId());
                List<Option> opts = optionsParQuestion.getOrDefault(q.getId(), new ArrayList<>());
                boolean isOk = false;
                for (Option o : opts) {
                    if (o.isEstCorrecte() && choisiId != null && o.getId() == choisiId.intValue()) {
                        isOk = true;
                        break;
                    }
                }
                if (isOk) correct++;
                else questionsManquees.add(q.getTexteQuestion());
            }

            try {
                return appellerGroqPourResume(total, correct, total - correct, percentage, questionsManquees);
            } catch (Exception e) {
                System.err.println("[CorrectorAI] Erreur résumé : " + e.getMessage());
                return fallbackResume(percentage);
            }
        });
    }

    // ── Appels API Groq ───────────────────────────────────────────────────────

    /** Appelle Groq pour générer l'explication d'une question */
    private ExplicationQuestion appellerGroqPourQuestion(
            Question question, Option optionChoisie, Option optionCorrecte, boolean isCorrect) throws Exception {

        String prompt = construirePromptQuestion(question, optionChoisie, optionCorrecte, isCorrect);

        String responseBody = appellerGroq(
            "Tu es un professeur bienveillant et pédagogue. Tu expliques les erreurs de manière " +
            "claire, encourageante et constructive. Tu réponds UNIQUEMENT en JSON valide.",
            prompt, 500
        );

        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        String content = json.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();

        JsonObject expl = GSON.fromJson(content, JsonObject.class);

        return new ExplicationQuestion(
            getStr(expl, "message"),
            getStr(expl, "pourquoi_incorrect"),
            getStr(expl, "pourquoi_correct"),
            getStr(expl, "conseil"),
            getStrList(expl, "ressources"),
            isCorrect
        );
    }

    /** Appelle Groq pour générer le résumé pédagogique global */
    private ResumePedagogique appellerGroqPourResume(
            int total, int correct, int incorrect, double pct, List<String> questionsManquees) throws Exception {

        String prompt = construirePromptResume(total, correct, incorrect, pct, questionsManquees);

        String responseBody = appellerGroq(
            "Tu es un professeur qui fait un bilan pédagogique personnalisé et motivant. " +
            "Tu réponds UNIQUEMENT en JSON valide.",
            prompt, 400
        );

        JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
        String content = json.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();

        JsonObject resume = GSON.fromJson(content, JsonObject.class);

        return new ResumePedagogique(
            getStr(resume, "message_general"),
            getStrList(resume, "points_forts"),
            getStrList(resume, "points_amelioration"),
            getStrList(resume, "conseils_revision"),
            getStr(resume, "encouragement")
        );
    }

    /** Effectue l'appel HTTP à l'API Groq */
    private String appellerGroq(String systemPrompt, String userPrompt, int maxTokens) throws Exception {
        String body = GSON.toJson(Map.of(
            "model", MODEL,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "temperature", 0.7,
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

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API HTTP " + response.statusCode());
        }
        return response.body();
    }

    // ── Construction des prompts ──────────────────────────────────────────────

    private String construirePromptQuestion(
            Question question, Option optionChoisie, Option optionCorrecte, boolean isCorrect) {

        String texteChoisi  = optionChoisie  != null ? optionChoisie.getTexteOption()  : "(aucune réponse)";
        String texteCorrect = optionCorrecte != null ? optionCorrecte.getTexteOption() : "?";

        if (isCorrect) {
            return "L'étudiant a CORRECTEMENT répondu à cette question:\n\n" +
                "QUESTION: " + question.getTexteQuestion() + "\n" +
                "RÉPONSE DE L'ÉTUDIANT: " + texteChoisi + "\n" +
                "BONNE RÉPONSE: " + texteCorrect + "\n\n" +
                "Génère un message d'encouragement et une explication pédagogique.\n\n" +
                "RÉPONDS en JSON avec ce format EXACT:\n" +
                "{\n" +
                "  \"message\": \"Message d'encouragement positif et bref\",\n" +
                "  \"pourquoi_correct\": \"Explication détaillée de pourquoi c'est correct\",\n" +
                "  \"pourquoi_incorrect\": \"\",\n" +
                "  \"conseil\": \"Conseil pour approfondir\",\n" +
                "  \"ressources\": [\"Suggestion 1\", \"Suggestion 2\"]\n" +
                "}";
        } else {
            return "L'étudiant a INCORRECTEMENT répondu à cette question:\n\n" +
                "QUESTION: " + question.getTexteQuestion() + "\n" +
                "RÉPONSE DE L'ÉTUDIANT: " + texteChoisi + "\n" +
                "BONNE RÉPONSE: " + texteCorrect + "\n\n" +
                "Génère une explication pédagogique bienveillante.\n\n" +
                "RÉPONDS en JSON avec ce format EXACT:\n" +
                "{\n" +
                "  \"message\": \"Message d'encouragement bref et positif\",\n" +
                "  \"pourquoi_incorrect\": \"Explication claire de l'erreur commise\",\n" +
                "  \"pourquoi_correct\": \"Explication de la bonne réponse\",\n" +
                "  \"conseil\": \"Conseil pratique pour progresser\",\n" +
                "  \"ressources\": [\"Suggestion 1\", \"Suggestion 2\"]\n" +
                "}";
        }
    }

    private String construirePromptResume(
            int total, int correct, int incorrect, double pct, List<String> questionsManquees) {

        StringBuilder sb = new StringBuilder();
        sb.append("Génère un bilan pédagogique personnalisé pour un étudiant qui vient de terminer un quiz:\n\n");
        sb.append("STATISTIQUES:\n");
        sb.append("- Total de questions: ").append(total).append("\n");
        sb.append("- Réponses correctes: ").append(correct).append("\n");
        sb.append("- Réponses incorrectes: ").append(incorrect).append("\n");
        sb.append("- Score: ").append(String.format("%.0f", pct)).append("%\n\n");

        if (!questionsManquees.isEmpty()) {
            sb.append("QUESTIONS MANQUÉES:\n");
            for (String q : questionsManquees) {
                sb.append("- ").append(q).append("\n");
            }
            sb.append("\n");
        }

        sb.append("RÉPONDS en JSON avec ce format EXACT:\n");
        sb.append("{\n");
        sb.append("  \"message_general\": \"Message d'ouverture adapté au score\",\n");
        sb.append("  \"points_forts\": [\"Point fort 1\", \"Point fort 2\"],\n");
        sb.append("  \"points_amelioration\": [\"Domaine à revoir 1\", \"Domaine 2\"],\n");
        sb.append("  \"conseils_revision\": [\"Conseil pratique 1\", \"Conseil 2\"],\n");
        sb.append("  \"encouragement\": \"Message final motivant\"\n");
        sb.append("}");

        return sb.toString();
    }

    // ── Fallbacks sans IA ─────────────────────────────────────────────────────

    /** Explication par défaut si l'API échoue */
    private ExplicationQuestion fallbackQuestion(boolean isCorrect, Option optionCorrecte) {
        if (isCorrect) {
            return new ExplicationQuestion(
                "✅ Excellente réponse !",
                "",
                "Votre réponse est correcte. Continuez ainsi !",
                "Continuez à approfondir vos connaissances sur ce sujet.",
                List.of(),
                true
            );
        } else {
            String bonneReponse = optionCorrecte != null ? optionCorrecte.getTexteOption() : "?";
            return new ExplicationQuestion(
                "❌ Ce n'est pas la bonne réponse",
                "Votre réponse n'est pas correcte.",
                "La bonne réponse est : " + bonneReponse,
                "Révisez ce concept et réessayez.",
                List.of("Relisez le chapitre correspondant"),
                false
            );
        }
    }

    /** Résumé par défaut si l'API échoue */
    private ResumePedagogique fallbackResume(double pct) {
        String msg = pct >= 75 ? "Très bon travail !" : pct >= 50 ? "Bon travail, continuez !" : "Continuez vos efforts !";
        return new ResumePedagogique(
            msg + " Vous avez obtenu " + String.format("%.0f", pct) + "%.",
            List.of("Participation active au quiz"),
            List.of("Révisez les questions manquées"),
            List.of("Relisez le chapitre", "Refaites le quiz"),
            "Chaque tentative vous rapproche de la maîtrise ! 💪"
        );
    }

    // ── Helpers JSON ──────────────────────────────────────────────────────────

    private String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
            ? json.get(key).getAsString() : "";
    }

    private List<String> getStrList(JsonObject json, String key) {
        List<String> list = new ArrayList<>();
        if (json.has(key) && json.get(key).isJsonArray()) {
            json.getAsJsonArray(key).forEach(e -> {
                if (!e.isJsonNull()) list.add(e.getAsString());
            });
        }
        return list;
    }
}
