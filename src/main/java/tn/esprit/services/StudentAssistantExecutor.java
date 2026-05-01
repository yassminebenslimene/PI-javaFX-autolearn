package tn.esprit.services;

import com.google.gson.JsonObject;
import tn.esprit.entities.*;
import tn.esprit.session.JwtManager;

import java.util.List;

/**
 * Executes student-specific actions requested by the AI assistant.
 */
public class StudentAssistantExecutor {

    private final EvenementService   evenementService   = new EvenementService();
    private final ServiceCommunaute  communauteService  = new ServiceCommunaute();
    private final ServiceCours       coursService       = new ServiceCours();
    private final ChallengeService   challengeService   = new ChallengeService();
    private final EquipeService      equipeService      = new EquipeService();
    private final ParticipationService participationService = new ParticipationService();

    public record ActionResult(
        boolean success,
        String message,
        Object data,
        String navigateTo   // frontoffice navigation key
    ) {}

    public ActionResult execute(String intent, JsonObject params) {
        return switch (intent) {
            // ── Navigation ─────────────────────────────────────────────────
            case "NAVIGATE_COURS"              -> nav("cours");
            case "NAVIGATE_EVENEMENTS"         -> nav("evenements");
            case "NAVIGATE_CHALLENGES"         -> nav("challenges");
            case "NAVIGATE_COMMUNAUTE"         -> nav("communaute");
            case "NAVIGATE_CLASSEMENT"         -> nav("classement");
            case "NAVIGATE_PROFIL"             -> nav("profil");
            case "NAVIGATE_MES_PARTICIPATIONS" -> nav("mes_participations");
            case "NAVIGATE_MES_EQUIPES"        -> nav("mes_equipes");

            // ── List ───────────────────────────────────────────────────────
            case "LIST_EVENEMENTS"   -> listEvenements();
            case "LIST_COURS"        -> listCours();
            case "LIST_CHALLENGES"   -> listChallenges();
            case "LIST_COMMUNAUTES"  -> listCommunautes();

            // ── Student actions ────────────────────────────────────────────
            case "JOIN_EVENEMENT"    -> joinEvenement(params);
            case "CREATE_EQUIPE"     -> createEquipe(params);
            case "JOIN_COMMUNAUTE"   -> joinCommunaute(params);
            case "CREATE_COMMUNAUTE" -> createCommunaute(params);

            default -> new ActionResult(true, "", null, null);
        };
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private ActionResult nav(String section) {
        return new ActionResult(true, "", null, section);
    }

    // ── List actions ──────────────────────────────────────────────────────────

    private ActionResult listEvenements() {
        try {
            List<Evenement> list = evenementService.getAll();
            if (list.isEmpty())
                return new ActionResult(true, "Aucun événement disponible pour le moment. 😔", list, null);

            StringBuilder sb = new StringBuilder("🎉 **" + list.size() + " événements disponibles :**\n\n");
            list.stream().limit(8).forEach(e -> {
                sb.append("• **").append(e.getTitre()).append("**");
                if (e.getLieu() != null) sb.append(" — 📍 ").append(e.getLieu());
                if (e.getType() != null) sb.append(" (").append(e.getType()).append(")");
                sb.append(" [ID: ").append(e.getId()).append("]\n");
            });
            if (list.size() > 8) sb.append("\n_...et ").append(list.size() - 8).append(" autres_");
            sb.append("\n\n💡 Dites **\"rejoindre événement [ID]\"** pour vous inscrire !");
            return new ActionResult(true, sb.toString(), list, "evenements");
        } catch (Exception e) {
            return new ActionResult(false, "Impossible de charger les événements. ❌", null, null);
        }
    }

    private ActionResult listCours() {
        try {
            List<Cours> list = coursService.consulter();
            if (list.isEmpty())
                return new ActionResult(true, "Aucun cours disponible pour le moment. 😔", list, null);

            StringBuilder sb = new StringBuilder("📚 **" + list.size() + " cours disponibles :**\n\n");
            list.stream().limit(8).forEach(c -> {
                sb.append("• **").append(c.getTitre()).append("**");
                if (c.getMatiere() != null) sb.append(" — ").append(c.getMatiere());
                if (c.getNiveau() != null) sb.append(" (").append(c.getNiveau()).append(")");
                sb.append("\n");
            });
            if (list.size() > 8) sb.append("\n_...et ").append(list.size() - 8).append(" autres_");
            return new ActionResult(true, sb.toString(), list, "cours");
        } catch (Exception e) {
            return new ActionResult(false, "Impossible de charger les cours. ❌", null, null);
        }
    }

    private ActionResult listChallenges() {
        try {
            List<Challenge> list = challengeService.getAll();
            if (list.isEmpty())
                return new ActionResult(true, "Aucun challenge disponible pour le moment. 😔", list, null);

            StringBuilder sb = new StringBuilder("🏆 **" + list.size() + " challenges disponibles :**\n\n");
            list.stream().limit(8).forEach(c -> {
                sb.append("• **").append(c.getTitre()).append("**");
                if (c.getNiveau() != null) sb.append(" — ").append(c.getNiveau());
                sb.append("\n");
            });
            if (list.size() > 8) sb.append("\n_...et ").append(list.size() - 8).append(" autres_");
            return new ActionResult(true, sb.toString(), list, "challenges");
        } catch (Exception e) {
            return new ActionResult(false, "Impossible de charger les challenges. ❌", null, null);
        }
    }

    private ActionResult listCommunautes() {
        try {
            List<Communaute> list = communauteService.getList();
            if (list.isEmpty())
                return new ActionResult(true, "Aucune communauté disponible pour le moment. 😔", list, null);

            StringBuilder sb = new StringBuilder("👥 **" + list.size() + " communautés disponibles :**\n\n");
            list.stream().limit(8).forEach(c -> {
                sb.append("• **").append(c.getNom()).append("**");
                if (c.getDescription() != null && !c.getDescription().isBlank())
                    sb.append(" — ").append(c.getDescription().length() > 50
                        ? c.getDescription().substring(0, 50) + "..." : c.getDescription());
                sb.append(" [ID: ").append(c.getId()).append("]\n");
            });
            if (list.size() > 8) sb.append("\n_...et ").append(list.size() - 8).append(" autres_");
            sb.append("\n\n💡 Dites **\"rejoindre communauté [ID]\"** pour rejoindre !");
            return new ActionResult(true, sb.toString(), list, "communaute");
        } catch (Exception e) {
            return new ActionResult(false, "Impossible de charger les communautés. ❌", null, null);
        }
    }

    // ── Student actions ───────────────────────────────────────────────────────

    private ActionResult joinEvenement(JsonObject params) {
        User user = JwtManager.getCurrentUser();
        if (user == null) return new ActionResult(false, "Vous devez être connecté pour vous inscrire. 🔒", null, null);

        if (!params.has("evenement_id"))
            return new ActionResult(false, "Quel est l'ID de l'événement auquel vous souhaitez vous inscrire ?", null, null);

        try {
            int eventId = params.get("evenement_id").getAsInt();
            Evenement event = evenementService.getById(eventId);
            if (event == null)
                return new ActionResult(false, "Événement introuvable (ID: " + eventId + "). ❌", null, null);

            // Check if already participating
            List<Participation> existing = participationService.getByEtudiant(user.getId());
            boolean alreadyJoined = existing.stream().anyMatch(p -> p.getEvenementId() == eventId);
            if (alreadyJoined)
                return new ActionResult(false, "Vous êtes déjà inscrit à **" + event.getTitre() + "** ! ✅", null, "evenements");

            // Create a solo team first, then participation
            Equipe equipe = new Equipe(user.getPrenom() + " " + user.getNom(), eventId);
            int equipeId = equipeService.ajouterEtRetournerId(equipe);
            equipeService.ajouterEtudiantEquipe(equipeId, user.getId());

            Participation p = new Participation(equipeId, eventId);
            participationService.ajouter(p);

            return new ActionResult(true,
                "✅ Inscription réussie à **" + event.getTitre() + "** !\n\n" +
                "📍 Lieu : " + event.getLieu() + "\n" +
                "📋 Type : " + event.getType() + "\n\n" +
                "Bonne chance ! 🎉",
                p, "evenements");
        } catch (Exception e) {
            return new ActionResult(false, "Erreur lors de l'inscription : " + e.getMessage() + " ❌", null, null);
        }
    }

    private ActionResult createEquipe(JsonObject params) {
        User user = JwtManager.getCurrentUser();
        if (user == null) return new ActionResult(false, "Vous devez être connecté pour créer une équipe. 🔒", null, null);

        if (!params.has("nom"))
            return new ActionResult(false, "Quel nom souhaitez-vous donner à votre équipe ?", null, null);

        if (!params.has("evenement_id"))
            return new ActionResult(false, "Pour quel événement souhaitez-vous créer une équipe ? (donnez l'ID)", null, null);

        try {
            String nom = params.get("nom").getAsString();
            int eventId = params.get("evenement_id").getAsInt();

            Evenement event = evenementService.getById(eventId);
            if (event == null)
                return new ActionResult(false, "Événement introuvable (ID: " + eventId + "). ❌", null, null);

            Equipe equipe = new Equipe(nom, eventId);
            int equipeId = equipeService.ajouterEtRetournerId(equipe);
            equipeService.ajouterEtudiantEquipe(equipeId, user.getId());

            return new ActionResult(true,
                "🎊 Équipe **" + nom + "** créée avec succès !\n\n" +
                "📅 Événement : " + event.getTitre() + "\n\n" +
                "Invitez vos amis à rejoindre votre équipe ! 👫",
                equipe, "mes_equipes");
        } catch (Exception e) {
            return new ActionResult(false, "Erreur lors de la création de l'équipe : " + e.getMessage() + " ❌", null, null);
        }
    }

    private ActionResult joinCommunaute(JsonObject params) {
        User user = JwtManager.getCurrentUser();
        if (user == null) return new ActionResult(false, "Vous devez être connecté pour rejoindre une communauté. 🔒", null, null);

        if (!params.has("communaute_id"))
            return new ActionResult(false, "Quel est l'ID de la communauté que vous souhaitez rejoindre ?", null, null);

        try {
            int commId = params.get("communaute_id").getAsInt();
            Communaute comm = communauteService.getById(commId);
            if (comm == null)
                return new ActionResult(false, "Communauté introuvable (ID: " + commId + "). ❌", null, null);

            communauteService.ajouterMembre(commId, user.getId());

            return new ActionResult(true,
                "🎉 Vous avez rejoint la communauté **" + comm.getNom() + "** !\n\n" +
                "Bienvenue dans la communauté ! Commencez à partager et échanger. 💬",
                comm, "communaute");
        } catch (Exception e) {
            return new ActionResult(false, "Erreur : " + e.getMessage() + " ❌", null, null);
        }
    }

    private ActionResult createCommunaute(JsonObject params) {
        User user = JwtManager.getCurrentUser();
        if (user == null) return new ActionResult(false, "Vous devez être connecté pour créer une communauté. 🔒", null, null);

        if (!params.has("nom"))
            return new ActionResult(false, "Quel nom souhaitez-vous donner à votre communauté ?", null, null);

        try {
            String nom = params.get("nom").getAsString();
            String desc = params.has("description") ? params.get("description").getAsString() : "Communauté créée via l'assistant IA";

            Communaute comm = new Communaute();
            comm.setNom(nom);
            comm.setDescription(desc);
            comm.setOwnerId(user.getId());
            communauteService.ajouter(comm);

            return new ActionResult(true,
                "🎊 Communauté **" + nom + "** créée avec succès !\n\n" +
                "Invitez d'autres étudiants à rejoindre votre communauté ! 👥",
                comm, "communaute");
        } catch (Exception e) {
            return new ActionResult(false, "Erreur lors de la création : " + e.getMessage() + " ❌", null, null);
        }
    }
}
