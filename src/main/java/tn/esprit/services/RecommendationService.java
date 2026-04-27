package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.entities.Cours;
import tn.esprit.entities.Evenement;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service avancé de recommandation personnalisée.
 * Combine requête SQL complexe + IA Groq pour générer des recommandations pertinentes.
 * 
 * Requête SQL : analyse les participations passées, les feedbacks, les types d'événements
 * et génère un profil utilisateur détaillé.
 * 
 * IA : utilise ce profil pour générer des recommandations contextualisées et professionnelles.
 */
public class RecommendationService {

    private final Connection connection = MyConnection.getInstance().getConnection();
    private final GroqService groqService = new GroqService();
    private final EvenementService evenementService = new EvenementService();
    private final ServiceCours coursService = new ServiceCours();

    /**
     * Génère un profil utilisateur détaillé basé sur ses participations et feedbacks.
     * Requête SQL complexe qui analyse :
     * - Types d'événements auxquels l'utilisateur a participé
     * - Notes moyennes par type d'événement (depuis les feedbacks)
     * - Nombre de participations par type
     * - Tendances et préférences
     */
    public UserProfile buildUserProfile(int userId) {
        UserProfile profile = new UserProfile();
        profile.userId = userId;

        try {
            // Requête SQL complexe : analyse complète des participations et feedbacks
            String sql = """
                SELECT 
                    ev.type,
                    COUNT(DISTINCT p.id) as nb_participations,
                    AVG(CAST(JSON_EXTRACT(p.feedbacks, '$.rating_global') AS DECIMAL(3,1))) as avg_rating,
                    GROUP_CONCAT(DISTINCT JSON_EXTRACT(p.feedbacks, '$.comment') SEPARATOR ' | ') as comments,
                    MAX(ev.date_debut) as last_event_date
                FROM participation p
                JOIN equipe_etudiant ee ON p.equipe_id = ee.equipe_id
                JOIN evenement ev ON p.evenement_id = ev.id
                WHERE ee.etudiant_id = ?
                  AND p.feedbacks IS NOT NULL
                  AND p.feedbacks != ''
                  AND p.feedbacks != 'null'
                GROUP BY ev.type
                ORDER BY avg_rating DESC, nb_participations DESC
                """;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String type = rs.getString("type");
                    int nbParticipations = rs.getInt("nb_participations");
                    double avgRating = rs.getDouble("avg_rating");
                    String comments = rs.getString("comments");

                    TypePreference pref = new TypePreference();
                    pref.type = type;
                    pref.participationCount = nbParticipations;
                    pref.averageRating = avgRating;
                    pref.feedback = comments != null ? comments : "";

                    profile.typePreferences.add(pref);
                }
            }

            // Récupérer aussi les types sans feedback (participations sans évaluation)
            String sqlNoFeedback = """
                SELECT DISTINCT ev.type
                FROM participation p
                JOIN equipe_etudiant ee ON p.equipe_id = ee.equipe_id
                JOIN evenement ev ON p.evenement_id = ev.id
                WHERE ee.etudiant_id = ?
                  AND (p.feedbacks IS NULL OR p.feedbacks = '' OR p.feedbacks = 'null')
                """;

            try (PreparedStatement ps = connection.prepareStatement(sqlNoFeedback)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String type = rs.getString("type");
                    boolean exists = profile.typePreferences.stream()
                            .anyMatch(tp -> tp.type != null && tp.type.equals(type));
                    if (!exists) {
                        TypePreference pref = new TypePreference();
                        pref.type = type;
                        pref.participationCount = 1;
                        pref.averageRating = 0;
                        pref.feedback = "";
                        profile.typePreferences.add(pref);
                    }
                }
            }

            // Nombre total de participations
            String sqlTotal = """
                SELECT COUNT(DISTINCT p.id) as total
                FROM participation p
                JOIN equipe_etudiant ee ON p.equipe_id = ee.equipe_id
                WHERE ee.etudiant_id = ?
                """;

            try (PreparedStatement ps = connection.prepareStatement(sqlTotal)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) profile.totalParticipations = rs.getInt("total");
            }

            // Événements déjà participés (pour les exclure)
            String sqlParticipated = """
                SELECT DISTINCT p.evenement_id
                FROM participation p
                JOIN equipe_etudiant ee ON p.equipe_id = ee.equipe_id
                WHERE ee.etudiant_id = ?
                """;

            try (PreparedStatement ps = connection.prepareStatement(sqlParticipated)) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    profile.participatedEventIds.add(rs.getInt("evenement_id"));
                }
            }

        } catch (SQLException e) {
            System.err.println("[RecommendationService] Erreur SQL: " + e.getMessage());
        }

        return profile;
    }

    /**
     * Génère des recommandations d'événements via IA basée sur le profil utilisateur.
     * L'IA reçoit le profil et génère des recommandations contextualisées.
     */
    public List<Evenement> generateEventRecommendations(UserProfile profile, int limit) {
        List<Evenement> recommendations = new ArrayList<>();

        try {
            // Construire le prompt pour l'IA
            String prompt = buildRecommendationPrompt(profile);

            // Appeler l'IA
            String iaResponse = groqService.ask(
                    "Tu es un expert en recommandation d'événements académiques. "
                            + "Analyse le profil utilisateur et recommande les types d'événements les plus pertinents.",
                    prompt
            );

            // Parser la réponse IA pour extraire les types recommandés
            List<String> recommendedTypes = parseIARecommendations(iaResponse);

            // Récupérer tous les événements futurs
            List<Evenement> allFutureEvents = evenementService.getAll().stream()
                    .filter(ev -> !ev.isIsCanceled())
                    .filter(ev -> ev.getDateDebut() != null && ev.getDateDebut().isAfter(LocalDateTime.now()))
                    .filter(ev -> !profile.participatedEventIds.contains(ev.getId()))
                    .toList();

            // Trier par pertinence : d'abord les types recommandés, puis les autres
            for (String type : recommendedTypes) {
                allFutureEvents.stream()
                        .filter(ev -> type.equalsIgnoreCase(ev.getType()))
                        .limit(limit - recommendations.size())
                        .forEach(recommendations::add);
            }

            // Compléter avec d'autres événements si nécessaire
            if (recommendations.size() < limit) {
                allFutureEvents.stream()
                        .filter(ev -> !recommendations.contains(ev))
                        .limit(limit - recommendations.size())
                        .forEach(recommendations::add);
            }

        } catch (Exception e) {
            System.err.println("[RecommendationService] Erreur génération recommandations: " + e.getMessage());
            // Fallback : retourner les événements futurs simples
            recommendations = evenementService.getAll().stream()
                    .filter(ev -> !ev.isIsCanceled())
                    .filter(ev -> ev.getDateDebut() != null && ev.getDateDebut().isAfter(LocalDateTime.now()))
                    .filter(ev -> !profile.participatedEventIds.contains(ev.getId()))
                    .limit(limit)
                    .toList();
        }

        return recommendations;
    }

    /**
     * Génère des recommandations de cours via IA basée sur le profil utilisateur.
     */
    public List<Cours> generateCourseRecommendations(UserProfile profile, int limit) {
        List<Cours> recommendations = new ArrayList<>();

        try {
            String prompt = buildCourseRecommendationPrompt(profile);

            String iaResponse = groqService.ask(
                    "Tu es un expert en recommandation de cours académiques. "
                            + "Analyse le profil utilisateur et recommande les matières les plus pertinentes.",
                    prompt
            );

            List<String> recommendedSubjects = parseCourseRecommendations(iaResponse);
            List<Cours> allCourses = coursService.getAll();

            // Trier par pertinence
            for (String subject : recommendedSubjects) {
                allCourses.stream()
                        .filter(c -> c.getMatiere() != null && c.getMatiere().toLowerCase().contains(subject.toLowerCase()))
                        .limit(limit - recommendations.size())
                        .forEach(recommendations::add);
            }

            // Compléter si nécessaire
            if (recommendations.size() < limit) {
                allCourses.stream()
                        .filter(c -> !recommendations.contains(c))
                        .limit(limit - recommendations.size())
                        .forEach(recommendations::add);
            }

        } catch (Exception e) {
            System.err.println("[RecommendationService] Erreur recommandations cours: " + e.getMessage());
            // Fallback
            recommendations = coursService.getAll().stream().limit(limit).toList();
        }

        return recommendations;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String buildRecommendationPrompt(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Profil utilisateur :\n");
        sb.append("- Total participations : ").append(profile.totalParticipations).append("\n");
        sb.append("- Historique par type d'événement :\n");

        for (TypePreference pref : profile.typePreferences) {
            sb.append("  * ").append(pref.type).append(" : ")
                    .append(pref.participationCount).append(" participation(s), ")
                    .append(String.format("%.1f", pref.averageRating)).append("/5 moyenne");
            if (!pref.feedback.isEmpty()) {
                String feedback = pref.feedback.length() > 100
                        ? pref.feedback.substring(0, 100) + "..."
                        : pref.feedback;
                sb.append(", feedback: \"").append(feedback).append("\"");
            }
            sb.append("\n");
        }

        sb.append("\nBasé sur ce profil, recommande les 3 types d'événements les plus pertinents pour cet utilisateur.\n");
        sb.append("Réponds UNIQUEMENT avec une liste JSON valide (sans markdown) :\n");
        sb.append("[\"Type1\", \"Type2\", \"Type3\"]\n");
        sb.append("Les types doivent être parmi : Hackathon, Conference, Workshop\n");

        return sb.toString();
    }

    private String buildCourseRecommendationPrompt(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Profil utilisateur :\n");
        sb.append("- Types d'événements préférés : ");
        profile.typePreferences.stream()
                .limit(3)
                .forEach(p -> sb.append(p.type).append(" (").append(String.format("%.1f", p.averageRating)).append("/5), "));
        sb.append("\n");

        sb.append("Basé sur ces préférences, recommande les 3 matières de cours les plus pertinentes.\n");
        sb.append("Réponds UNIQUEMENT avec une liste JSON valide (sans markdown) :\n");
        sb.append("[\"Matière1\", \"Matière2\", \"Matière3\"]\n");
        sb.append("Les matières peuvent être : Informatique, Développement, Sciences, Gestion, etc.\n");

        return sb.toString();
    }

    private List<String> parseIARecommendations(String response) {
        List<String> types = new ArrayList<>();
        try {
            String cleaned = response.trim();
            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                JsonArray arr = com.google.gson.JsonParser.parseString(cleaned).getAsJsonArray();
                for (com.google.gson.JsonElement el : arr) {
                    types.add(el.getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("[RecommendationService] Erreur parsing IA: " + e.getMessage());
        }
        return types;
    }

    private List<String> parseCourseRecommendations(String response) {
        return parseIARecommendations(response);
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    public static class UserProfile {
        public int userId;
        public int totalParticipations = 0;
        public List<TypePreference> typePreferences = new ArrayList<>();
        public Set<Integer> participatedEventIds = new HashSet<>();
    }

    public static class TypePreference {
        public String type;
        public int participationCount;
        public double averageRating;
        public String feedback;
    }
}
