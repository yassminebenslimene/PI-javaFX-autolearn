package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Option;
import tn.esprit.entities.Question;
import tn.esprit.entities.Quiz;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * GroqQuizGeneratorService — Génère automatiquement des questions QCM
 * à partir du contenu d'un chapitre via l'API Groq (Llama 4 Scout).
 *
 * Flux :
 *   Chapitre → extraire contenu → construire prompt → appel Groq API
 *   → parser JSON → créer Quiz + Questions + Options en BDD
 *
 * Modèle : meta-llama/llama-4-scout-17b-16e-instruct
 * API    : https://api.groq.com/openai/v1/chat/completions
 */
public class GroqQuizGeneratorService {

    // ── Configuration ─────────────────────────────────────────────────────────
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL        = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String API_KEY      = "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";
    private static final int    MAX_QUESTIONS = 10;
    private static final int    MAX_CONTENU   = 4000; // caractères max envoyés à l'IA

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    private static final Gson GSON = new Gson();

    // Services BDD
    private final ServiceQuiz     serviceQuiz     = new ServiceQuiz();
    private final ServiceQuestion serviceQuestion = new ServiceQuestion();
    private final ServiceOption   serviceOption   = new ServiceOption();

    // ── Modèle de résultat ────────────────────────────────────────────────────

    public record QuestionGeneree(
        String texte,
        int points,
        List<OptionGeneree> options
    ) {}

    public record OptionGeneree(
        String texte,
        boolean correcte
    ) {}

    // ── Méthode principale ────────────────────────────────────────────────────

    /**
     * Génère un quiz complet pour un chapitre de manière asynchrone.
     *
     * @param chapitre        le chapitre source
     * @param nombreQuestions nombre de questions à générer (1-10)
     * @param difficulte      "facile", "moyen" ou "difficile"
     * @param titreQuiz       titre du quiz (null = auto)
     * @param chapitreId      ID du chapitre pour la BDD
     * @return CompletableFuture<Quiz> — le quiz créé en BDD, ou null si erreur
     */
    public CompletableFuture<Quiz> genererQuizAsync(
            Chapitre chapitre,
            int nombreQuestions,
            String difficulte,
            String titreQuiz,
            int chapitreId) {

        // Validation
        int nb = Math.max(1, Math.min(nombreQuestions, MAX_QUESTIONS));

        return CompletableFuture.supplyAsync(() -> {
            try {
                // ① Extraire le contenu du chapitre
                String contenu = extraireContenu(chapitre);
                if (contenu.isBlank()) {
                    throw new RuntimeException("Le chapitre '" + chapitre.getTitre() + "' n'a pas de contenu.");
                }

                // ② Construire le prompt
                String prompt = construirePrompt(contenu, nb, difficulte, chapitre.getTitre());

                // ③ Appeler l'API Groq
                List<QuestionGeneree> questions = appellerGroq(prompt);

                // ④ Créer le Quiz en BDD
                String titre = (titreQuiz != null && !titreQuiz.isBlank())
                    ? titreQuiz
                    : "Quiz IA - " + chapitre.getTitre();

                Quiz quiz = new Quiz(titre,
                    "Quiz généré automatiquement par IA Groq (Llama 4 Scout)",
                    "actif", null, 60, null, null, null, null, chapitreId);

                boolean ok = serviceQuiz.ajouter(quiz);
                if (!ok) throw new RuntimeException("Impossible de créer le quiz en BDD.");

                // Récupérer l'ID du quiz créé
                Quiz quizCree = trouverDernierQuizDuChapitre(chapitreId, titre);
                if (quizCree == null) throw new RuntimeException("Quiz créé introuvable en BDD.");

                // ⑤ Créer les Questions et Options en BDD
                for (QuestionGeneree qg : questions) {
                    Question question = new Question(
                        qg.texte(), qg.points(), null, quizCree.getId());
                    boolean qOk = serviceQuestion.ajouter(question);
                    if (!qOk) continue;

                    // Récupérer l'ID de la question créée
                    Question questionCreee = trouverDerniereQuestion(quizCree.getId(), qg.texte());
                    if (questionCreee == null) continue;

                    for (OptionGeneree og : qg.options()) {
                        Option option = new Option(
                            og.texte(), og.correcte(), questionCreee.getId());
                        serviceOption.ajouter(option);
                    }
                }

                System.out.println("[Groq] Quiz généré : " + quizCree.getTitre()
                    + " (" + questions.size() + " questions)");
                return quizCree;

            } catch (Exception e) {
                System.err.println("[Groq] Erreur génération : " + e.getMessage());
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    // ── Appel API Groq ────────────────────────────────────────────────────────

    private List<QuestionGeneree> appellerGroq(String prompt) throws Exception {
        // Corps de la requête JSON
        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("temperature", 0.7);
        body.addProperty("max_tokens", 3000);

        // Format JSON forcé
        JsonObject responseFormat = new JsonObject();
        responseFormat.addProperty("type", "json_object");
        body.add("response_format", responseFormat);

        // Messages
        JsonArray messages = new JsonArray();

        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content",
            "Tu es un expert pédagogique qui crée des quiz de qualité. " +
            "Tu réponds UNIQUEMENT en JSON valide, sans texte avant ou après.");
        messages.add(system);

        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        body.add("messages", messages);

        // Requête HTTP
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_API_URL))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + API_KEY)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = HTTP.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new RuntimeException("Clé API Groq invalide (401). Vérifiez votre clé.");
        }
        if (response.statusCode() == 429) {
            throw new RuntimeException("Limite de requêtes Groq dépassée (429). Réessayez dans quelques secondes.");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur API Groq (HTTP " + response.statusCode() + ") : " + response.body());
        }

        // Parser la réponse
        JsonObject responseJson = GSON.fromJson(response.body(), JsonObject.class);
        String content = responseJson
            .getAsJsonArray("choices")
            .get(0).getAsJsonObject()
            .getAsJsonObject("message")
            .get("content").getAsString();

        return validerEtParser(content);
    }

    // ── Validation et parsing du JSON ─────────────────────────────────────────

    private List<QuestionGeneree> validerEtParser(String jsonContent) {
        try {
            JsonObject data = GSON.fromJson(jsonContent, JsonObject.class);

            if (!data.has("questions") || !data.get("questions").isJsonArray()) {
                throw new RuntimeException("Format invalide : clé 'questions' manquante.");
            }

            JsonArray questionsJson = data.getAsJsonArray("questions");
            List<QuestionGeneree> questions = new ArrayList<>();

            for (int i = 0; i < questionsJson.size(); i++) {
                JsonObject qJson = questionsJson.get(i).getAsJsonObject();

                String texte = getStr(qJson, "texte");
                if (texte.isBlank()) continue;

                int points = qJson.has("points") ? qJson.get("points").getAsInt() : 10;

                if (!qJson.has("options") || !qJson.get("options").isJsonArray()) continue;
                JsonArray optionsJson = qJson.getAsJsonArray("options");

                List<OptionGeneree> options = new ArrayList<>();
                boolean hasCorrect = false;

                for (int j = 0; j < optionsJson.size(); j++) {
                    JsonObject oJson = optionsJson.get(j).getAsJsonObject();
                    String oTexte = getStr(oJson, "texte");
                    if (oTexte.isBlank()) continue;
                    boolean correcte = oJson.has("correcte") && oJson.get("correcte").getAsBoolean();
                    if (correcte) hasCorrect = true;
                    options.add(new OptionGeneree(oTexte, correcte));
                }

                if (!hasCorrect || options.size() < 2) continue;
                questions.add(new QuestionGeneree(texte, points, options));
            }

            if (questions.isEmpty()) {
                throw new RuntimeException("Aucune question valide dans la réponse de l'IA.");
            }

            return questions;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de parser la réponse JSON de Groq : " + e.getMessage());
        }
    }

    // ── Construction du prompt ────────────────────────────────────────────────

    private String construirePrompt(String contenu, int nb, String difficulte, String titreChapitre) {
        String niveau = switch (difficulte == null ? "moyen" : difficulte.toLowerCase()) {
            case "facile"    -> "faciles, adaptées aux débutants";
            case "difficile" -> "difficiles, nécessitant une compréhension approfondie";
            default          -> "de difficulté moyenne";
        };

        return """
            Crée exactement %d questions à choix multiples (QCM) basées sur le contenu suivant.
            Les questions doivent être %s.
            Chapitre : "%s"

            CONTENU DU CHAPITRE:
            %s

            INSTRUCTIONS:
            1. Crée exactement %d questions pertinentes basées UNIQUEMENT sur ce contenu
            2. Chaque question doit avoir exactement 4 options de réponse
            3. Une seule option doit être correcte par question
            4. Les questions doivent évaluer la compréhension du contenu
            5. Utilise un langage clair et professionnel en français
            6. Attribue 10 points par question

            RÉPONDS UNIQUEMENT avec un objet JSON dans ce format exact:
            {
              "questions": [
                {
                  "texte": "Quelle est la question?",
                  "points": 10,
                  "options": [
                    {"texte": "Option A", "correcte": false},
                    {"texte": "Option B", "correcte": true},
                    {"texte": "Option C", "correcte": false},
                    {"texte": "Option D", "correcte": false}
                  ]
                }
              ]
            }
            """.formatted(nb, niveau, titreChapitre, contenu, nb);
    }

    // ── Extraction du contenu ─────────────────────────────────────────────────

    private String extraireContenu(Chapitre chapitre) {
        StringBuilder sb = new StringBuilder();

        // Titre du chapitre
        if (chapitre.getTitre() != null) {
            sb.append("Titre : ").append(chapitre.getTitre()).append("\n\n");
        }

        // Contenu principal
        if (chapitre.getContenu() != null && !chapitre.getContenu().isBlank()) {
            // Supprimer les balises HTML si présentes
            String contenu = chapitre.getContenu()
                .replaceAll("<[^>]+>", " ")   // supprimer balises HTML
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ")       // normaliser espaces
                .trim();
            sb.append(contenu);
        }

        // Ressources textuelles
        if (chapitre.getRessources() != null && !chapitre.getRessources().isBlank()) {
            sb.append("\n\nRessources : ").append(chapitre.getRessources());
        }

        String result = sb.toString().trim();

        // Tronquer à MAX_CONTENU caractères
        if (result.length() > MAX_CONTENU) {
            result = result.substring(0, MAX_CONTENU) + "...";
        }

        return result;
    }

    // ── Helpers BDD ───────────────────────────────────────────────────────────

    private Quiz trouverDernierQuizDuChapitre(int chapitreId, String titre) {
        return serviceQuiz.findByChapitreId(chapitreId).stream()
            .filter(q -> titre.equals(q.getTitre()))
            .reduce((first, second) -> second) // dernier
            .orElse(null);
    }

    private Question trouverDerniereQuestion(int quizId, String texte) {
        return serviceQuestion.findByQuizId(quizId).stream()
            .filter(q -> texte.equals(q.getTexteQuestion()))
            .reduce((first, second) -> second)
            .orElse(null);
    }

    private static String getStr(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
            ? json.get(key).getAsString().trim() : "";
    }
}
