package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.Challenge;
import tn.esprit.entities.Exercice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GroqChallengeGeneratorService — Génère automatiquement des challenges complets via Groq AI.
 * Même modèle et API que le générateur de quiz.
 *
 * Génère : titre, description, niveau, durée, exercices avec questions et réponses attendues.
 */
public class GroqChallengeGeneratorService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String API_KEY      = "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";

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    private static final Gson GSON = new Gson();

    private final ChallengeService challengeService = new ChallengeService();
    private final ExerciceService  exerciceService  = new ExerciceService();

    // ── Modèles de résultat ───────────────────────────────────────────────────

    public record ExerciceGenere(
        String question,
        String reponseAttendue,
        int points
    ) {}

    public record ChallengeGenere(
        String titre,
        String description,
        String niveau,
        int duree,
        List<ExerciceGenere> exercices
    ) {}

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Génère un challenge complet avec exercices via Groq AI.
     *
     * @param sujet          Sujet/thème du challenge (ex: "Java POO", "SQL avancé")
     * @param niveau         "Débutant", "Intermédiaire" ou "Avancé"
     * @param nbExercices    Nombre d'exercices (1-10)
     * @param createdBy      ID de l'admin créateur
     * @return CompletableFuture<Challenge> — le challenge créé en BDD
     */
    public CompletableFuture<Challenge> genererChallengeAsync(
            String sujet, String niveau, int nbExercices, int createdBy) {

        int nb = Math.max(1, Math.min(nbExercices, 10));

        return CompletableFuture.supplyAsync(() -> {
            try {
                // ① Appeler Groq pour générer le challenge
                ChallengeGenere generated = appellerGroq(sujet, niveau, nb);

                // ② Créer le Challenge en BDD
                Challenge challenge = new Challenge();
                // Tronquer le titre si trop long (max 100 caractères pour éviter l'erreur MySQL)
                String generatedTitre = generated.titre();
                final String titre = generatedTitre.length() > 100 
                    ? generatedTitre.substring(0, 97) + "..." 
                    : generatedTitre;
                
                challenge.setTitre(titre);
                challenge.setDescription(generated.description());
                challenge.setNiveau(generated.niveau());
                challenge.setDuree(generated.duree());
                challenge.setCreatedBy(createdBy);
                challenge.setExerciceIds(new ArrayList<>());
                challenge.setQuizIds(new ArrayList<>());

                challengeService.add(challenge);

                // Récupérer le challenge créé (dernier par titre)
                Challenge created = challengeService.getAll().stream()
                    .filter(c -> titre.equals(c.getTitre()))
                    .reduce((a, b) -> b)
                    .orElse(null);

                if (created == null) throw new RuntimeException("Challenge créé introuvable en BDD.");

                // ③ Créer les exercices et les lier au challenge
                List<Integer> exerciceIds = new ArrayList<>();
                for (ExerciceGenere eg : generated.exercices()) {
                    Exercice exercice = new Exercice();
                    exercice.setQuestion(eg.question());
                    exercice.setReponse(eg.reponseAttendue());
                    exercice.setPoints(eg.points());
                    exerciceService.add(exercice);

                    // Récupérer l'exercice créé
                    Exercice createdEx = exerciceService.getAll().stream()
                        .filter(e -> eg.question().equals(e.getQuestion()))
                        .reduce((a, b) -> b)
                        .orElse(null);

                    if (createdEx != null) exerciceIds.add(createdEx.getId());
                }

                // Mettre à jour le challenge avec les exercices
                created.setExerciceIds(exerciceIds);
                challengeService.update(created);

                System.out.println("[ChallengeAI] Challenge généré: " + created.getTitre()
                    + " (" + exerciceIds.size() + " exercices)");
                return created;

            } catch (Exception e) {
                System.err.println("[ChallengeAI] Erreur génération: " + e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    // ── Appel Groq ────────────────────────────────────────────────────────────

    private ChallengeGenere appellerGroq(String sujet, String niveau, int nbExercices) throws Exception {
        String niveauDesc = switch (niveau) {
            case "Débutant"      -> "faciles, adaptées aux débutants";
            case "Avancé"        -> "difficiles, nécessitant une expertise";
            default              -> "de difficulté intermédiaire";
        };

        int duree = switch (niveau) {
            case "Débutant" -> 30;
            case "Avancé"   -> 90;
            default         -> 60;
        };

        String prompt = """
            Crée un challenge pédagogique complet sur le sujet: "%s"
            Niveau: %s
            Nombre d'exercices: %d (questions à réponse ouverte)
            
            INSTRUCTIONS:
            1. Le titre doit être accrocheur et descriptif
            2. La description doit motiver l'étudiant (2-3 phrases)
            3. Chaque exercice doit avoir une question claire et une réponse attendue détaillée
            4. Les questions doivent être %s
            5. La réponse attendue doit être complète (2-4 phrases) pour permettre une correction IA
            6. Attribue 10-20 points par exercice selon la difficulté
            
            RÉPONDS UNIQUEMENT en JSON avec ce format EXACT:
            {
              "titre": "Titre du challenge",
              "description": "Description motivante du challenge",
              "niveau": "%s",
              "duree": %d,
              "exercices": [
                {
                  "question": "Question claire et précise?",
                  "reponse_attendue": "Réponse complète et détaillée attendue de l'étudiant",
                  "points": 15
                }
              ]
            }
            """.formatted(sujet, niveau, nbExercices, niveauDesc, niveau, duree);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 3000);

        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        JsonArray messages = new JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
            "Tu es un expert pédagogique qui crée des challenges de programmation de qualité. " +
            "Tu réponds UNIQUEMENT en JSON valide, sans texte avant ou après.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_API_URL))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Groq API HTTP " + response.statusCode() + ": " + response.body());

        JsonObject responseJson = GSON.fromJson(response.body(), JsonObject.class);
        String content = responseJson.getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();

        return parseChallenge(content);
    }

    private ChallengeGenere parseChallenge(String jsonContent) {
        JsonObject data = GSON.fromJson(jsonContent, JsonObject.class);

        String titre       = getStr(data, "titre");
        String description = getStr(data, "description");
        String niveau      = getStr(data, "niveau");
        int duree          = data.has("duree") ? data.get("duree").getAsInt() : 60;

        List<ExerciceGenere> exercices = new ArrayList<>();
        if (data.has("exercices") && data.get("exercices").isJsonArray()) {
            for (var el : data.getAsJsonArray("exercices")) {
                JsonObject e = el.getAsJsonObject();
                String question = getStr(e, "question");
                String reponse  = getStr(e, "reponse_attendue");
                int points      = e.has("points") ? e.get("points").getAsInt() : 10;
                if (!question.isBlank()) {
                    exercices.add(new ExerciceGenere(question, reponse, points));
                }
            }
        }

        if (titre.isBlank()) titre = "Challenge IA";
        if (description.isBlank()) description = "Challenge généré par IA";
        if (niveau.isBlank()) niveau = "Intermédiaire";

        return new ChallengeGenere(titre, description, niveau, duree, exercices);
    }

    private String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
            ? json.get(key).getAsString().trim() : "";
    }
}
