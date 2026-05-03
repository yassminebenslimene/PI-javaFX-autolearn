package tn.esprit.services;

import com.google.gson.JsonObject;
import tn.esprit.entities.*;
import tn.esprit.session.JwtManager;
import tn.esprit.tools.PasswordUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Executes CRUD actions requested by the chatbot.
 *
 * Takes the intent + params from ChatbotService and calls the right service.
 * Returns an ActionResult with a user-friendly message and optional data.
 */
public class ChatbotActionExecutor {

    private final UserService        userService        = new UserService();
    private final ServiceCours       coursService       = new ServiceCours();
    private final EvenementService   evenementService   = new EvenementService();
    private final ChallengeService   challengeService   = new ChallengeService();
    private final ServiceCommunaute  communauteService  = new ServiceCommunaute();

    public record ActionResult(
        boolean success,
        String message,
        Object data,        // List or entity for display
        String navigateTo   // FXML path to navigate to (optional)
    ) {}

    // ── Main dispatcher ───────────────────────────────────────────────────────

    public ActionResult execute(String intent, JsonObject params) {
        return switch (intent) {
            // ── Cours ──────────────────────────────────────────────────────
            case "LIST_COURS"       -> listCours();
            case "CREATE_COURS"     -> createCours(params);
            case "UPDATE_COURS"     -> updateCours(params);
            case "DELETE_COURS"     -> deleteCours(params);

            // ── Users ──────────────────────────────────────────────────────
            case "LIST_USERS"       -> listUsers();
            case "CREATE_USER"      -> createUser(params);
            case "DELETE_USER"      -> deleteUser(params);

            // ── Evenements ─────────────────────────────────────────────────
            case "LIST_EVENEMENTS"  -> listEvenements();
            case "CREATE_EVENEMENT" -> createEvenement(params);
            case "DELETE_EVENEMENT" -> deleteEvenement(params);

            // ── Challenges ─────────────────────────────────────────────────
            case "LIST_CHALLENGES"  -> listChallenges();
            case "CREATE_CHALLENGE" -> createChallenge(params);
            case "DELETE_CHALLENGE" -> deleteChallenge(params);

            // ── Communautes ────────────────────────────────────────────────
            case "LIST_COMMUNAUTES"  -> listCommunautes();
            case "CREATE_COMMUNAUTE" -> createCommunaute(params);

            // ── Navigation ─────────────────────────────────────────────────
            case "NAVIGATE_COURS"       -> navigate("/views/backoffice/cours/index.fxml");
            case "NAVIGATE_USERS"       -> navigate("/views/backoffice/user/index.fxml");
            case "NAVIGATE_EVENEMENTS"  -> navigate("/views/backoffice/evenement/index.fxml");
            case "NAVIGATE_CHALLENGES"  -> navigate("/views/backoffice/challenge/index.fxml");
            case "NAVIGATE_COMMUNAUTE"  -> navigate("/views/backoffice/communaute/index.fxml");
            case "NAVIGATE_DASHBOARD"   -> navigate("/views/backoffice/user/index.fxml");

            // ── Chat (no action) ───────────────────────────────────────────
            default -> new ActionResult(true, "", null, null);
        };
    }

    // ── Cours ─────────────────────────────────────────────────────────────────

    private ActionResult listCours() {
        List<Cours> list = coursService.consulter();
        if (list.isEmpty()) return new ActionResult(true, "Aucun cours n'a été trouvé pour le moment.", list, null);
        StringBuilder sb = new StringBuilder(list.size() + " cours disponibles :\n\n");
        for (int i = 0; i < list.size(); i++) {
            Cours c = list.get(i);
            sb.append(i + 1).append(". ").append(c.getTitre())
              .append("  —  ").append(c.getMatiere())
              .append("  |  ").append(c.getNiveau())
              .append("  |  ").append(c.getDuree()).append("h\n");
        }
        return new ActionResult(true, sb.toString(), list, null);
    }

    private ActionResult createCours(JsonObject p) {
        String titre = getString(p, "titre", null);
        String mat   = getString(p, "matiere", null);

        if (titre == null || titre.isBlank() || titre.equals("Nouveau Cours")) {
            return new ActionResult(false,
                "Quel est le titre du cours que vous souhaitez créer ?", null, null);
        }
        if (mat == null || mat.isBlank()) {
            return new ActionResult(false,
                "Quelle est la matière du cours \"" + titre + "\" ? (ex: Informatique, Mathématiques...)", null, null);
        }

        try {
            String desc  = getString(p, "description", "À compléter");
            String niv   = getString(p, "niveau", "DEBUTANT");
            int duree    = getInt(p, "duree", 10);

            Cours cours = new Cours();
            cours.setTitre(titre);
            cours.setDescription(desc);
            cours.setMatiere(mat);
            cours.setNiveau(niv);
            cours.setDuree(duree);
            cours.setCreatedAt(LocalDateTime.now());

            coursService.ajouter(cours);
            ActivityApiClient.logAsync(currentUserId(), "admin.created_cours",
                java.util.Map.of("titre", titre));

            return new ActionResult(true,
                "Le cours \"" + titre + "\" a été créé avec succès !\n" +
                "Matière : " + mat + " | Niveau : " + niv + " | Durée : " + duree + "h", cours, null);
        } catch (Exception e) {
            return new ActionResult(false, "Une erreur s'est produite. Veuillez réessayer.", null, null);
        }
    }

    private ActionResult updateCours(JsonObject p) {
        try {
            int id = getInt(p, "id", 0);
            if (id == 0) return new ActionResult(false, "ID du cours manquant.", null, null);

            Cours cours = coursService.consulterParId(id);
            if (cours == null) return new ActionResult(false, "Cours #" + id + " introuvable.", null, null);

            if (p.has("titre"))       cours.setTitre(p.get("titre").getAsString());
            if (p.has("description")) cours.setDescription(p.get("description").getAsString());
            if (p.has("matiere"))     cours.setMatiere(p.get("matiere").getAsString());
            if (p.has("niveau"))      cours.setNiveau(p.get("niveau").getAsString());
            if (p.has("duree"))       cours.setDuree(p.get("duree").getAsInt());

            coursService.modifier(cours);
            ActivityApiClient.logAsync(currentUserId(), "admin.updated_cours",
                java.util.Map.of("id", String.valueOf(id)));

            return new ActionResult(true, "Cours #" + id + " mis a jour avec succes !", cours, null);
        } catch (Exception e) {
            return new ActionResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    private ActionResult deleteCours(JsonObject p) {
        try {
            int id = getInt(p, "id", 0);
            if (id == 0) return new ActionResult(false, "ID du cours manquant.", null, null);

            coursService.supprimer(id);
            ActivityApiClient.logAsync(currentUserId(), "admin.deleted_cours",
                java.util.Map.of("id", String.valueOf(id)));

            return new ActionResult(true, "Cours #" + id + " supprime avec succes.", null, null);
        } catch (Exception e) {
            return new ActionResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private ActionResult listUsers() {
        List<User> list = userService.afficher();
        if (list.isEmpty()) return new ActionResult(true, "Aucun utilisateur trouvé pour le moment.", list, null);
        long etudiants = list.stream().filter(u -> "ETUDIANT".equals(u.getRole())).count();
        long admins    = list.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        StringBuilder sb = new StringBuilder(list.size() + " utilisateurs (" + etudiants + " étudiants, " + admins + " admins) :\n\n");
        for (int i = 0; i < list.size(); i++) {
            User u = list.get(i);
            sb.append(i + 1).append(". ").append(u.getPrenom()).append(" ").append(u.getNom())
              .append("  —  ").append(u.getEmail())
              .append("  |  ").append(u.getRole()).append("\n");
        }
        return new ActionResult(true, sb.toString(), list, null);
    }

    private ActionResult createUser(JsonObject p) {
        String email  = getString(p, "email", null);
        String prenom = getString(p, "prenom", null);
        String nom    = getString(p, "nom", null);

        if (prenom == null || prenom.isBlank()) {
            return new ActionResult(false, "Quel est le prénom de l'étudiant ?", null, null);
        }
        if (nom == null || nom.isBlank()) {
            return new ActionResult(false, "Quel est le nom de famille de " + prenom + " ?", null, null);
        }
        if (email == null || email.isBlank()) {
            return new ActionResult(false, "Quelle est l'adresse email de " + prenom + " " + nom + " ?", null, null);
        }

        try {
            String niveau = getString(p, "niveau", "DEBUTANT");
            String pwd    = getString(p, "password", "AutoLearn2026!");

            Etudiant e = new Etudiant();
            e.setNom(nom);
            e.setPrenom(prenom);
            e.setEmail(email);
            e.setNiveau(niveau);
            e.setRole("ETUDIANT");
            e.setPassword(PasswordUtil.hash(pwd));

            userService.ajouter(e);
            ActivityApiClient.logAsync(currentUserId(), "admin.created_student",
                java.util.Map.of("email", email, "nom", prenom + " " + nom));

            return new ActionResult(true,
                "L'étudiant " + prenom + " " + nom + " a été créé avec succès !\n" +
                "Email : " + email + " | Niveau : " + niveau + "\n" +
                "Mot de passe temporaire : " + pwd, e, null);
        } catch (Exception e) {
            return new ActionResult(false, "Une erreur s'est produite. Veuillez réessayer.", null, null);
        }
    }

    private ActionResult deleteUser(JsonObject p) {
        try {
            int id = getInt(p, "id", 0);
            if (id == 0) return new ActionResult(false, "ID utilisateur manquant.", null, null);

            userService.supprimer(id);
            ActivityApiClient.logAsync(currentUserId(), "admin.deleted_student",
                java.util.Map.of("id", String.valueOf(id)));

            return new ActionResult(true, "Utilisateur #" + id + " supprime.", null, null);
        } catch (Exception e) {
            return new ActionResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    // ── Evenements ────────────────────────────────────────────────────────────

    private ActionResult listEvenements() {
        List<Evenement> list = evenementService.getAll();
        if (list.isEmpty()) return new ActionResult(true, "Aucun événement trouvé pour le moment.", list, null);
        StringBuilder sb = new StringBuilder(list.size() + " événements :\n\n");
        for (int i = 0; i < list.size(); i++) {
            Evenement ev = list.get(i);
            sb.append(i + 1).append(". ").append(ev.getTitre())
              .append("  —  ").append(ev.getLieu())
              .append("  |  ").append(ev.getType())
              .append("  |  ").append(ev.getNbMax()).append(" places\n");
        }
        return new ActionResult(true, sb.toString(), list, null);
    }

    private ActionResult createEvenement(JsonObject p) {
        // Validate required fields — never create with placeholder data
        String titre = getString(p, "titre", null);
        String lieu  = getString(p, "lieu", null);

        if (titre == null || titre.isBlank() || titre.equals("Nouvel Evenement") || titre.equals("Titre d'evenement")) {
            return new ActionResult(false,
                "Il me manque le titre de l'événement. Quel nom voulez-vous lui donner ?", null, null);
        }
        if (lieu == null || lieu.isBlank()) {
            return new ActionResult(false,
                "Où se déroulera l'événement \"" + titre + "\" ? (ville ou lieu)", null, null);
        }

        try {
            String desc  = getString(p, "description", "À compléter");
            String type  = getString(p, "type", "Conférence");
            int nbMax    = getInt(p, "nb_max", 50);

            Evenement ev = new Evenement();
            ev.setTitre(titre);
            ev.setLieu(lieu);
            ev.setDescription(desc);
            ev.setType(type);
            ev.setNbMax(nbMax);
            ev.setStatus("Plannifié");
            ev.setWorkflowStatus("planifie");
            ev.setIsCanceled(false);
            ev.setDateDebut(LocalDateTime.now().plusDays(7));
            ev.setDateFin(LocalDateTime.now().plusDays(8));

            evenementService.ajouter(ev);
            ActivityApiClient.logAsync(currentUserId(), "admin.created_evenement",
                java.util.Map.of("titre", titre, "lieu", lieu));

            return new ActionResult(true,
                "L'événement \"" + titre + "\" a été créé avec succès !\n" +
                "Lieu : " + lieu + " | Type : " + type + " | Max : " + nbMax + " participants", ev, null);
        } catch (Exception e) {
            return new ActionResult(false, "Une erreur s'est produite. Veuillez réessayer.", null, null);
        }
    }

    private ActionResult deleteEvenement(JsonObject p) {
        try {
            int id = getInt(p, "id", 0);
            if (id == 0) return new ActionResult(false, "ID evenement manquant.", null, null);
            evenementService.supprimer(id);
            return new ActionResult(true, "Evenement #" + id + " supprime.", null, null);
        } catch (Exception e) {
            return new ActionResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    // ── Challenges ────────────────────────────────────────────────────────────

    private ActionResult listChallenges() {
        List<Challenge> list = challengeService.getAll();
        if (list.isEmpty()) return new ActionResult(true, "Aucun challenge trouvé pour le moment.", list, null);
        StringBuilder sb = new StringBuilder(list.size() + " challenges :\n\n");
        for (int i = 0; i < list.size(); i++) {
            Challenge c = list.get(i);
            sb.append(i + 1).append(". ").append(c.getTitre())
              .append("  —  ").append(c.getNiveau())
              .append("  |  ").append(c.getDuree()).append(" min\n");
        }
        return new ActionResult(true, sb.toString(), list, null);
    }

    private ActionResult createChallenge(JsonObject p) {
        String titre = getString(p, "titre", null);
        if (titre == null || titre.isBlank() || titre.equals("Nouveau Challenge")) {
            return new ActionResult(false, "Quel est le titre du challenge ?", null, null);
        }

        try {
            String desc  = getString(p, "description", "À compléter");
            String niv   = getString(p, "niveau", "DEBUTANT");
            int duree    = getInt(p, "duree", 30);

            Challenge c = new Challenge();
            c.setTitre(titre);
            c.setDescription(desc);
            c.setNiveau(niv);
            c.setDuree(duree);
            c.setCreatedBy(currentUserId());

            challengeService.add(c);
            ActivityApiClient.logAsync(currentUserId(), "admin.created_challenge",
                java.util.Map.of("titre", titre));

            return new ActionResult(true,
                "Le challenge \"" + titre + "\" a été créé avec succès !\n" +
                "Niveau : " + niv + " | Durée : " + duree + " minutes", c, null);
        } catch (Exception e) {
            return new ActionResult(false, "Une erreur s'est produite. Veuillez réessayer.", null, null);
        }
    }

    private ActionResult deleteChallenge(JsonObject p) {
        try {
            int id = getInt(p, "id", 0);
            if (id == 0) return new ActionResult(false, "ID challenge manquant.", null, null);
            challengeService.delete(id);
            return new ActionResult(true, "Challenge #" + id + " supprime.", null, null);
        } catch (Exception e) {
            return new ActionResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    // ── Communautes ───────────────────────────────────────────────────────────

    private ActionResult listCommunautes() {
        List<Communaute> list = communauteService.getList();
        if (list.isEmpty()) return new ActionResult(true, "Aucune communaute trouvee.", list, null);
        StringBuilder sb = new StringBuilder("Voici les " + list.size() + " communautes :\n\n");
        for (Communaute c : list) {
            sb.append("• ").append(c.getNom()).append("\n");
        }
        return new ActionResult(true, sb.toString(), list, null);
    }

    private ActionResult createCommunaute(JsonObject p) {
        try {
            String nom  = getString(p, "nom", "Nouvelle Communaute");
            String desc = getString(p, "description", "Description a completer");

            Communaute c = new Communaute();
            c.setNom(nom);
            c.setDescription(desc);
            c.setOwnerId(currentUserId());

            communauteService.ajouter(c);
            ActivityApiClient.logAsync(currentUserId(), "admin.created_communaute",
                java.util.Map.of("nom", nom));

            return new ActionResult(true,
                "Communaute \"" + nom + "\" creee avec succes !", c, null);
        } catch (Exception e) {
            return new ActionResult(false, "Erreur: " + e.getMessage(), null, null);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private ActionResult navigate(String fxml) {
        return new ActionResult(true, "", null, fxml);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int currentUserId() {
        var user = JwtManager.getCurrentUser();
        return user != null ? user.getId() : 0;
    }

    private String getString(JsonObject p, String key, String defaultVal) {
        return (p != null && p.has(key) && !p.get(key).isJsonNull())
            ? p.get(key).getAsString() : defaultVal;
    }

    private int getInt(JsonObject p, String key, int defaultVal) {
        try {
            return (p != null && p.has(key) && !p.get(key).isJsonNull())
                ? p.get(key).getAsInt() : defaultVal;
        } catch (Exception e) { return defaultVal; }
    }
}
