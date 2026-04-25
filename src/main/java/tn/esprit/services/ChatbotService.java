package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chatbot Service - Ollama local AI + smart fallback parser.
 */
public class ChatbotService {

    private static final String OLLAMA_URL   = "http://localhost:11434/api/chat";
    private static final String OLLAMA_MODEL = "gemma3:4b"; // Google Gemma 3 - smart and fast
    private static final Gson   GSON         = new Gson();

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final String SYSTEM_PROMPT =
        "You are AutoLearn AI assistant. You are smart, friendly, helpful and conversational. " +
        "You speak French or English depending on the user language. " +
        "You help admins manage the AutoLearn e-learning platform. " +
        "You can answer ANY question about the platform data, give statistics, and perform actions. " +
        "\n\nFor ACTIONS, respond ONLY with JSON: {\"intent\": \"ACTION\", \"params\": {}, \"message\": \"friendly message\"} " +
        "\n\nAvailable action intents: " +
        "LIST_COURS, LIST_USERS, LIST_EVENEMENTS, LIST_CHALLENGES, LIST_COMMUNAUTES, " +
        "CREATE_COURS(titre, matiere, niveau[DEBUTANT/INTERMEDIAIRE/AVANCE], duree[int], description), " +
        "CREATE_EVENEMENT(titre, lieu, type[Conference/Atelier/Hackathon/Autre], nb_max[int], description), " +
        "CREATE_CHALLENGE(titre, niveau, duree[int minutes], description), " +
        "CREATE_USER(prenom, nom, email, niveau[DEBUTANT/INTERMEDIAIRE/AVANCE]), " +
        "CREATE_COMMUNAUTE(nom, description), " +
        "DELETE_COURS(id), DELETE_USER(id), DELETE_EVENEMENT(id), DELETE_CHALLENGE(id), " +
        "NAVIGATE_COURS, NAVIGATE_USERS, NAVIGATE_EVENEMENTS, NAVIGATE_CHALLENGES, NAVIGATE_COMMUNAUTE, NAVIGATE_DASHBOARD. " +
        "\n\nFor QUESTIONS and ANALYTICS (statistics, who is most active, how many users, etc.), " +
        "use intent CHAT and answer directly using the platform data provided in the context. " +
        "\n\nRULES: " +
        "1. For analytics questions (qui est le plus actif, combien d etudiants, etc.) -> use CHAT intent and answer from the data. " +
        "2. For greetings, general questions -> use CHAT intent and respond naturally. " +
        "3. For CREATE with all data provided -> extract and return JSON immediately. " +
        "4. For CREATE with missing data -> ask conversationally (no JSON). " +
        "5. For LIST -> return JSON immediately. " +
        "6. NEVER say you cannot answer a question - always try to help.";

    // ── Public API ────────────────────────────────────────────────────────────

    public static CompletableFuture<ChatResponse> sendMessage(
            String userMessage,
            List<ChatMessage> conversationHistory) {

        // Build platform context snapshot for analytics
        String platformContext = buildPlatformContext();

        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonArray messages = new JsonArray();

                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", SYSTEM_PROMPT + platformContext);
                messages.add(systemMsg);

                int start = Math.max(0, conversationHistory.size() - 10);
                for (int i = start; i < conversationHistory.size(); i++) {
                    ChatMessage cm = conversationHistory.get(i);
                    JsonObject msg = new JsonObject();
                    msg.addProperty("role", cm.role());
                    msg.addProperty("content", cm.content());
                    messages.add(msg);
                }

                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                messages.add(userMsg);

                JsonObject body = new JsonObject();
                body.addProperty("model", OLLAMA_MODEL);
                body.add("messages", messages);
                body.addProperty("stream", false);

                JsonObject options = new JsonObject();
                options.addProperty("temperature", 0.1);
                options.addProperty("num_predict", 600);
                body.add("options", options);

                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(120))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[Chatbot] Ollama HTTP " + resp.statusCode());

                if (resp.statusCode() == 200) {
                    return parseOllamaResponse(resp.body());
                } else {
                    System.err.println("[Chatbot] Error " + resp.statusCode());
                    return smartFallback(userMessage, conversationHistory);
                }

            } catch (java.net.ConnectException e) {
                System.err.println("[Chatbot] Ollama not running - using smart fallback");
                return smartFallback(userMessage, conversationHistory);
            } catch (Exception e) {
                System.err.println("[Chatbot] Error: " + e.getMessage());
                return smartFallback(userMessage, conversationHistory);
            }
        });
    }

    // ── Ollama response parsing ───────────────────────────────────────────────

    private static ChatResponse parseOllamaResponse(String responseBody) {
        try {
            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            String content = json
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

            System.out.println("[Chatbot] Raw: " + content);

            String jsonStr = extractJson(content);
            if (jsonStr == null) {
                return new ChatResponse("CHAT", new JsonObject(), content, true);
            }

            JsonObject parsed = GSON.fromJson(jsonStr, JsonObject.class);
            String intent     = parsed.has("intent")  ? parsed.get("intent").getAsString()  : "CHAT";
            JsonObject params = parsed.has("params")  ? parsed.getAsJsonObject("params")    : new JsonObject();
            String message    = parsed.has("message") ? parsed.get("message").getAsString() : content;

            return new ChatResponse(intent, params, message, true);

        } catch (Exception e) {
            System.err.println("[Chatbot] Parse error: " + e.getMessage());
            return new ChatResponse("CHAT", new JsonObject(), "Je n'ai pas compris. Pouvez-vous reformuler ?", false);
        }
    }

    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String candidate = text.substring(start, end + 1);
            if (candidate.contains("\"intent\"")) return candidate;
        }
        return null;
    }

    // ── Smart fallback (no Ollama needed) ─────────────────────────────────────

    /**
     * Smart fallback that understands natural language without AI.
     * Handles: "creer etudiant rahma, ben ali, debutant, rahmabenali@gmail.com"
     */
    private static ChatResponse smartFallback(String message, List<ChatMessage> history) {
        String lower = message.toLowerCase().trim();
        JsonObject p = new JsonObject();

        // ── Greetings ──────────────────────────────────────────────────────
        if (lower.matches("(hello|hi|bonjour|salut|hey|bonsoir|coucou).*")) {
            return new ChatResponse("CHAT", p,
                "Bonjour ! Je suis votre assistant AutoLearn. Comment puis-je vous aider ?\n\n" +
                "Je peux lister, créer, modifier ou supprimer des cours, événements, challenges, utilisateurs et communautés.", true);
        }

        // ── Analytics questions ────────────────────────────────────────────
        if (lower.contains("plus actif") || lower.contains("most active") ||
            lower.contains("combien") || lower.contains("how many") ||
            lower.contains("statistique") || lower.contains("stats") ||
            lower.contains("nombre") || lower.contains("total")) {
            return buildAnalyticsResponse(lower, p);
        }

        // ── LIST ───────────────────────────────────────────────────────────
        boolean isList = lower.contains("liste") || lower.contains("lister") || lower.contains("affiche") ||
                         lower.contains("montre") || lower.contains("voir") || lower.contains("show") ||
                         lower.contains("list") || lower.contains("display") || lower.contains("all") ||
                         lower.contains("tous") || lower.contains("toutes");

        if (isList) {
            if (lower.contains("cours") || lower.contains("course"))
                return new ChatResponse("LIST_COURS", p, "Voici tous les cours disponibles :", true);
            if (lower.contains("utilisateur") || lower.contains("etudiant") || lower.contains("user") || lower.contains("student") || lower.contains("eleve"))
                return new ChatResponse("LIST_USERS", p, "Voici tous les utilisateurs :", true);
            if (lower.contains("evenement") || lower.contains("event"))
                return new ChatResponse("LIST_EVENEMENTS", p, "Voici tous les événements :", true);
            if (lower.contains("challenge"))
                return new ChatResponse("LIST_CHALLENGES", p, "Voici tous les challenges :", true);
            if (lower.contains("communaute") || lower.contains("community"))
                return new ChatResponse("LIST_COMMUNAUTES", p, "Voici toutes les communautés :", true);
        }

        // ── CREATE ─────────────────────────────────────────────────────────
        boolean isCreate = lower.contains("creer") || lower.contains("créer") || lower.contains("create") ||
                           lower.contains("ajouter") || lower.contains("add") || lower.contains("nouveau") ||
                           lower.contains("nouvelle") || lower.contains("new") || lower.contains("inserer");

        if (isCreate) {
            // Detect entity type
            boolean isEvent     = lower.contains("evenement") || lower.contains("event");
            boolean isCours     = lower.contains("cours") || lower.contains("course");
            boolean isUser      = lower.contains("etudiant") || lower.contains("utilisateur") || lower.contains("user") || lower.contains("student") || lower.contains("eleve");
            boolean isChallenge = lower.contains("challenge");
            boolean isCommunaute= lower.contains("communaute") || lower.contains("community");

            // Try to extract inline data (comma-separated or natural language)
            p = extractInlineData(message, lower);

            if (isUser || p.has("email")) {
                if (hasUserData(p)) {
                    ensureUserDefaults(p);
                    return new ChatResponse("CREATE_USER", p,
                        "Parfait ! Je crée l'étudiant **" + p.get("prenom").getAsString() + " " + p.get("nom").getAsString() + "** maintenant...", true);
                }
                return new ChatResponse("CHAT", p,
                    "Je vais créer un étudiant ! Donnez-moi :\n\n" +
                    "• **Prénom** et **Nom**\n• **Email**\n• **Niveau** (DEBUTANT / INTERMEDIAIRE / AVANCE)\n\n" +
                    "Exemple : `creer etudiant Rahma, Ben Ali, DEBUTANT, rahma@gmail.com`", true);
            }

            if (isEvent || p.has("lieu")) {
                if (hasEventData(p)) {
                    ensureEventDefaults(p);
                    return new ChatResponse("CREATE_EVENEMENT", p,
                        "Parfait ! Je crée l'événement **" + p.get("titre").getAsString() + "** maintenant...", true);
                }
                return new ChatResponse("CHAT", p,
                    "Je vais créer un événement ! Donnez-moi :\n\n" +
                    "• **Titre**\n• **Lieu**\n• **Type** (Conference / Atelier / Hackathon / Autre)\n• **Nombre max** de participants\n• **Description**\n\n" +
                    "Exemple : `creer evenement Java Day, Tunis, Conference, 100, Journée Java`", true);
            }

            if (isCours || p.has("matiere")) {
                if (hasCoursData(p)) {
                    ensureCoursDefaults(p);
                    return new ChatResponse("CREATE_COURS", p,
                        "Parfait ! Je crée le cours **" + p.get("titre").getAsString() + "** maintenant...", true);
                }
                return new ChatResponse("CHAT", p,
                    "Je vais créer un cours ! Donnez-moi :\n\n" +
                    "• **Titre**\n• **Matière**\n• **Niveau** (DEBUTANT / INTERMEDIAIRE / AVANCE)\n• **Durée** (heures)\n• **Description**\n\n" +
                    "Exemple : `creer cours Java, Informatique, DEBUTANT, 20, Cours Java complet`", true);
            }

            if (isChallenge) {
                if (hasChallengeData(p)) {
                    ensureChallengeDefaults(p);
                    return new ChatResponse("CREATE_CHALLENGE", p,
                        "Parfait ! Je crée le challenge **" + p.get("titre").getAsString() + "** maintenant...", true);
                }
                return new ChatResponse("CHAT", p,
                    "Je vais créer un challenge ! Donnez-moi :\n\n" +
                    "• **Titre**\n• **Niveau** (DEBUTANT / INTERMEDIAIRE / AVANCE)\n• **Durée** (minutes)\n• **Description**\n\n" +
                    "Exemple : `creer challenge Algo Race, AVANCE, 60, Challenge algorithmique`", true);
            }

            if (isCommunaute) {
                if (p.has("nom") && p.has("description")) {
                    return new ChatResponse("CREATE_COMMUNAUTE", p,
                        "Parfait ! Je crée la communauté **" + p.get("nom").getAsString() + "** maintenant...", true);
                }
                return new ChatResponse("CHAT", p,
                    "Je vais créer une communauté ! Donnez-moi :\n\n" +
                    "• **Nom**\n• **Description**\n\n" +
                    "Exemple : `creer communaute Java Lovers, Communauté des passionnés Java`", true);
            }
        }

        // ── DELETE ─────────────────────────────────────────────────────────
        if (lower.contains("supprimer") || lower.contains("delete") || lower.contains("effacer") || lower.contains("remove")) {
            // Try to extract ID
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\d+").matcher(message);
            if (m.find()) {
                int id = Integer.parseInt(m.group());
                p.addProperty("id", id);
                if (lower.contains("cours") || lower.contains("course"))
                    return new ChatResponse("DELETE_COURS", p, "Suppression du cours #" + id + "...", true);
                if (lower.contains("evenement") || lower.contains("event"))
                    return new ChatResponse("DELETE_EVENEMENT", p, "Suppression de l'événement #" + id + "...", true);
                if (lower.contains("challenge"))
                    return new ChatResponse("DELETE_CHALLENGE", p, "Suppression du challenge #" + id + "...", true);
                if (lower.contains("utilisateur") || lower.contains("etudiant") || lower.contains("user"))
                    return new ChatResponse("DELETE_USER", p, "Suppression de l'utilisateur #" + id + "...", true);
            }
            return new ChatResponse("CHAT", p, "Quel est l'ID de l'élément à supprimer ?", true);
        }

        // ── NAVIGATE ───────────────────────────────────────────────────────
        if (lower.contains("aller") || lower.contains("go to") || lower.contains("ouvrir") || lower.contains("open") || lower.contains("page")) {
            if (lower.contains("cours") || lower.contains("course"))
                return new ChatResponse("NAVIGATE_COURS", p, "Navigation vers Cours...", true);
            if (lower.contains("utilisateur") || lower.contains("etudiant") || lower.contains("user"))
                return new ChatResponse("NAVIGATE_USERS", p, "Navigation vers Utilisateurs...", true);
            if (lower.contains("evenement") || lower.contains("event"))
                return new ChatResponse("NAVIGATE_EVENEMENTS", p, "Navigation vers Événements...", true);
            if (lower.contains("challenge"))
                return new ChatResponse("NAVIGATE_CHALLENGES", p, "Navigation vers Challenges...", true);
            if (lower.contains("communaute") || lower.contains("community"))
                return new ChatResponse("NAVIGATE_COMMUNAUTE", p, "Navigation vers Communauté...", true);
            if (lower.contains("dashboard") || lower.contains("accueil"))
                return new ChatResponse("NAVIGATE_DASHBOARD", p, "Navigation vers Dashboard...", true);
        }

        // ── Default ────────────────────────────────────────────────────────
        return new ChatResponse("CHAT", p,
            "Je suis votre assistant AutoLearn. Voici ce que je peux faire :\n\n" +
            "📋 **Lister** : `liste les cours`, `affiche les étudiants`, `voir les événements`\n" +
            "➕ **Créer** : `creer cours Java, Informatique, DEBUTANT, 20, Description`\n" +
            "🗑️ **Supprimer** : `supprimer cours 5`, `supprimer evenement 3`\n" +
            "🔗 **Naviguer** : `aller aux cours`, `ouvrir les événements`\n\n" +
            "Que souhaitez-vous faire ?", true);
    }

    // ── Smart data extraction ─────────────────────────────────────────────────

    /**
     * Extracts data from natural language or comma-separated input.
     * Handles: "rahma, ben ali, debutant, rahmabenali@gmail.com, Rahma@2003"
     */
    private static JsonObject extractInlineData(String message, String lower) {
        JsonObject p = new JsonObject();

        // Try key:value format first
        if (message.contains(":")) {
            String[] parts = message.split(",");
            for (String part : parts) {
                String[] kv = part.split(":", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim().toLowerCase().replaceAll("[^a-z_]", "");
                    String val = kv[1].trim();
                    mapField(p, key, val);
                }
            }
            if (p.size() > 0) return p;
        }

        // Try positional comma-separated format
        // Remove the action keywords to get just the data
        String cleaned = lower
            .replaceAll("creer|créer|create|ajouter|add|nouveau|nouvelle|new", "")
            .replaceAll("etudiant|utilisateur|user|student|eleve", "")
            .replaceAll("evenement|event|cours|course|challenge|communaute", "")
            .trim();

        if (cleaned.contains(",")) {
            String[] parts = cleaned.split(",");
            // Detect email
            for (String part : parts) {
                String v = part.trim();
                if (v.contains("@") && v.contains(".")) {
                    p.addProperty("email", v);
                }
            }

            // Detect niveau
            for (String part : parts) {
                String v = part.trim().toUpperCase();
                if (v.equals("DEBUTANT") || v.equals("INTERMEDIAIRE") || v.equals("AVANCE") ||
                    v.equals("BEGINNER") || v.equals("INTERMEDIATE") || v.equals("ADVANCED")) {
                    String niveau = v.replace("BEGINNER", "DEBUTANT")
                                    .replace("INTERMEDIATE", "INTERMEDIAIRE")
                                    .replace("ADVANCED", "AVANCE");
                    p.addProperty("niveau", niveau);
                }
            }

            // Detect numbers (duree, nb_max)
            for (String part : parts) {
                String v = part.trim();
                try {
                    int num = Integer.parseInt(v);
                    if (!p.has("duree") && !p.has("nb_max")) {
                        p.addProperty("duree", num);
                    }
                } catch (NumberFormatException ignored) {}
            }

            // Remaining non-special parts → name fields
            java.util.List<String> nameparts = new java.util.ArrayList<>();
            for (String part : parts) {
                String v = part.trim();
                if (v.isEmpty()) continue;
                if (v.contains("@")) continue; // email
                String vUp = v.toUpperCase();
                if (vUp.equals("DEBUTANT") || vUp.equals("INTERMEDIAIRE") || vUp.equals("AVANCE") ||
                    vUp.equals("BEGINNER") || vUp.equals("INTERMEDIATE") || vUp.equals("ADVANCED")) continue;
                try { Integer.parseInt(v); continue; } catch (NumberFormatException ignored) {}
                // Skip action keywords
                if (v.matches("creer|créer|create|ajouter|add|nouveau|nouvelle|new|etudiant|utilisateur|user|student|eleve|evenement|event|cours|course|challenge|communaute")) continue;
                nameparts.add(v);
            }

            // Assign name parts
            if (nameparts.size() >= 2) {
                // Check if it looks like user creation (has email)
                if (p.has("email")) {
                    p.addProperty("prenom", capitalize(nameparts.get(0)));
                    p.addProperty("nom", capitalize(nameparts.get(1)));
                    // Check for password (contains uppercase + digit + special)
                    if (nameparts.size() >= 3) {
                        String possiblePwd = nameparts.get(2);
                        if (possiblePwd.matches(".*[A-Z].*") && possiblePwd.matches(".*\\d.*")) {
                            p.addProperty("password", possiblePwd);
                        }
                    }
                } else {
                    // Could be event/cours: titre, lieu/matiere, ...
                    p.addProperty("titre", capitalize(nameparts.get(0)));
                    if (nameparts.size() >= 2) p.addProperty("lieu", capitalize(nameparts.get(1)));
                    if (nameparts.size() >= 3) p.addProperty("type", capitalize(nameparts.get(2)));
                    if (nameparts.size() >= 4) p.addProperty("description", capitalize(nameparts.get(3)));
                }
            } else if (nameparts.size() == 1) {
                p.addProperty("titre", capitalize(nameparts.get(0)));
            }
        }

        return p;
    }

    private static void mapField(JsonObject p, String key, String val) {
        switch (key) {
            case "titre", "title", "nom", "name" -> p.addProperty("titre", val);
            case "lieu", "location", "place"     -> p.addProperty("lieu", val);
            case "type"                          -> p.addProperty("type", val);
            case "max", "nb_max", "participants" -> { try { p.addProperty("nb_max", Integer.parseInt(val)); } catch (Exception e) { p.addProperty("nb_max", 50); } }
            case "description", "desc"           -> p.addProperty("description", val);
            case "matiere", "subject", "matière" -> p.addProperty("matiere", val);
            case "niveau", "level"               -> p.addProperty("niveau", val.toUpperCase());
            case "duree", "duration", "durée"    -> { try { p.addProperty("duree", Integer.parseInt(val)); } catch (Exception e) { p.addProperty("duree", 10); } }
            case "prenom", "firstname", "prénom" -> p.addProperty("prenom", val);
            case "nom_famille", "lastname"       -> p.addProperty("nom", val);
            case "email"                         -> p.addProperty("email", val);
            case "password", "mdp", "motdepasse" -> p.addProperty("password", val);
        }
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    private static boolean hasUserData(JsonObject p) {
        return p.has("email") && (p.has("prenom") || p.has("nom"));
    }

    private static boolean hasEventData(JsonObject p) {
        return p.has("titre") && p.has("lieu");
    }

    private static boolean hasCoursData(JsonObject p) {
        return p.has("titre") && (p.has("matiere") || p.has("niveau"));
    }

    private static boolean hasChallengeData(JsonObject p) {
        return p.has("titre");
    }

    private static void ensureUserDefaults(JsonObject p) {
        if (!p.has("prenom")) p.addProperty("prenom", "Prenom");
        if (!p.has("nom"))    p.addProperty("nom", "Nom");
        if (!p.has("niveau")) p.addProperty("niveau", "DEBUTANT");
    }

    private static void ensureEventDefaults(JsonObject p) {
        if (!p.has("titre"))       p.addProperty("titre", "Nouvel Evenement");
        if (!p.has("lieu"))        p.addProperty("lieu", "Tunis");
        if (!p.has("type"))        p.addProperty("type", "Conference");
        if (!p.has("nb_max"))      p.addProperty("nb_max", 50);
        if (!p.has("description")) p.addProperty("description", "A completer");
    }

    private static void ensureCoursDefaults(JsonObject p) {
        if (!p.has("titre"))       p.addProperty("titre", "Nouveau Cours");
        if (!p.has("matiere"))     p.addProperty("matiere", "Informatique");
        if (!p.has("niveau"))      p.addProperty("niveau", "DEBUTANT");
        if (!p.has("duree"))       p.addProperty("duree", 10);
        if (!p.has("description")) p.addProperty("description", "A completer");
    }

    private static void ensureChallengeDefaults(JsonObject p) {
        if (!p.has("titre"))       p.addProperty("titre", "Nouveau Challenge");
        if (!p.has("niveau"))      p.addProperty("niveau", "DEBUTANT");
        if (!p.has("duree"))       p.addProperty("duree", 30);
        if (!p.has("description")) p.addProperty("description", "A completer");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ── Analytics fallback ────────────────────────────────────────────────────

    private static ChatResponse buildAnalyticsResponse(String lower, JsonObject p) {
        try {
            tn.esprit.services.UserService userService = new tn.esprit.services.UserService();
            tn.esprit.services.ServiceCours coursService = new tn.esprit.services.ServiceCours();
            tn.esprit.services.EvenementService evenementService = new tn.esprit.services.EvenementService();
            tn.esprit.services.ChallengeService challengeService = new tn.esprit.services.ChallengeService();

            java.util.List<tn.esprit.entities.User> users = userService.afficher();
            long etudiants = users.stream().filter(u -> "ETUDIANT".equals(u.getRole())).count();
            long admins    = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();

            // Most active student (most recent login)
            if (lower.contains("actif") || lower.contains("active")) {
                java.util.Optional<tn.esprit.entities.User> mostActive = users.stream()
                    .filter(u -> "ETUDIANT".equals(u.getRole()) && u.getLastLoginAt() != null)
                    .max(java.util.Comparator.comparing(tn.esprit.entities.User::getLastLoginAt));

                if (mostActive.isPresent()) {
                    tn.esprit.entities.User u = mostActive.get();
                    return new ChatResponse("CHAT", p,
                        "L'étudiant le plus actif est " + u.getPrenom() + " " + u.getNom() +
                        " (" + u.getEmail() + ")\nDernière connexion : " + u.getLastLoginAt(), true);
                }
                return new ChatResponse("CHAT", p, "Aucune donnée de connexion disponible.", true);
            }

            // Statistics
            if (lower.contains("combien") || lower.contains("how many") ||
                lower.contains("nombre") || lower.contains("total") || lower.contains("statistique")) {

                int nbCours      = coursService.consulter().size();
                int nbEvents     = evenementService.getAll().size();
                int nbChallenges = challengeService.getAll().size();

                return new ChatResponse("CHAT", p,
                    "Statistiques de la plateforme AutoLearn :\n\n" +
                    "Utilisateurs : " + users.size() + " (" + etudiants + " étudiants, " + admins + " admins)\n" +
                    "Cours : " + nbCours + "\n" +
                    "Événements : " + nbEvents + "\n" +
                    "Challenges : " + nbChallenges, true);
            }

        } catch (Exception e) {
            System.err.println("[Chatbot] Analytics error: " + e.getMessage());
        }

        return new ChatResponse("CHAT", p,
            "Je n'ai pas pu récupérer ces données. Essayez de relancer l'application.", true);
    }

    // ── Platform context for analytics ───────────────────────────────────────

    /**
     * Builds a real-time snapshot of platform data to inject into the AI context.
     * This allows the AI to answer analytics questions like "who is the most active student".
     */
    private static String buildPlatformContext() {
        try {
            tn.esprit.services.UserService userService = new tn.esprit.services.UserService();
            tn.esprit.services.ServiceCours coursService = new tn.esprit.services.ServiceCours();
            tn.esprit.services.EvenementService evenementService = new tn.esprit.services.EvenementService();
            tn.esprit.services.ChallengeService challengeService = new tn.esprit.services.ChallengeService();

            java.util.List<tn.esprit.entities.User> users = userService.afficher();
            java.util.List<tn.esprit.entities.Cours> cours = coursService.consulter();
            java.util.List<tn.esprit.entities.Evenement> events = evenementService.getAll();
            java.util.List<tn.esprit.entities.Challenge> challenges = challengeService.getAll();

            long etudiants = users.stream().filter(u -> "ETUDIANT".equals(u.getRole())).count();
            long admins    = users.stream().filter(u -> "ADMIN".equals(u.getRole())).count();

            StringBuilder ctx = new StringBuilder("\n\n=== PLATFORM DATA (use this to answer analytics questions) ===\n");
            ctx.append("Total users: ").append(users.size())
               .append(" (").append(etudiants).append(" students, ").append(admins).append(" admins)\n");
            ctx.append("Total courses: ").append(cours.size()).append("\n");
            ctx.append("Total events: ").append(events.size()).append("\n");
            ctx.append("Total challenges: ").append(challenges.size()).append("\n");

            // List students with last login for "most active" queries
            ctx.append("\nStudents list:\n");
            users.stream()
                .filter(u -> "ETUDIANT".equals(u.getRole()))
                .limit(20)
                .forEach(u -> ctx.append("- ").append(u.getPrenom()).append(" ").append(u.getNom())
                    .append(" (").append(u.getEmail()).append(")")
                    .append(u.getLastLoginAt() != null ? " last login: " + u.getLastLoginAt() : "")
                    .append("\n"));

            // List courses
            ctx.append("\nCourses list:\n");
            cours.stream().limit(10).forEach(c ->
                ctx.append("- [ID:").append(c.getId()).append("] ").append(c.getTitre())
                   .append(" (").append(c.getMatiere()).append(", ").append(c.getNiveau()).append(")\n"));

            // List events
            ctx.append("\nEvents list:\n");
            events.stream().limit(10).forEach(e ->
                ctx.append("- [ID:").append(e.getId()).append("] ").append(e.getTitre())
                   .append(" (").append(e.getLieu()).append(", ").append(e.getType()).append(")\n"));

            ctx.append("=== END PLATFORM DATA ===");
            return ctx.toString();

        } catch (Exception e) {
            System.err.println("[Chatbot] Context build error: " + e.getMessage());
            return "";
        }
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record ChatResponse(String intent, JsonObject params, String message, boolean success) {}
    public record ChatMessage(String role, String content) {}
}