package tn.esprit.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service de génération de planning personnalisé pour événements.
 * Génère un planning complet basé sur le type d'événement via l'IA Groq.
 */
public class EventPlanningService {

    private final GroqService groqService = new GroqService();

    /**
     * Génère un planning personnalisé pour un événement.
     * @param eventTitle Titre de l'événement
     * @param eventType Type d'événement (Hackathon, Conference, Workshop)
     * @param startTime Heure de début
     * @param endTime Heure de fin
     * @param nbParticipants Nombre de participants
     * @return Planning structuré en JSON
     */
    public String generatePlanning(String eventTitle, String eventType, 
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   int nbParticipants) {
        try {
            String prompt = buildPlanningPrompt(eventTitle, eventType, startTime, endTime, nbParticipants);
            String response = groqService.ask(
                "Tu es un expert en planification d'événements. Génère des plannings détaillés et professionnels.",
                prompt);
            return parsePlanningResponse(response);
        } catch (Exception e) {
            System.err.println("Erreur génération planning: " + e.getMessage());
            return generateDefaultPlanning(eventType, startTime, endTime);
        }
    }

    private String buildPlanningPrompt(String eventTitle, String eventType,
                                       LocalDateTime startTime, LocalDateTime endTime,
                                       int nbParticipants) {
        return "Génère un planning pour cet événement. IMPORTANT: maximum 6 activités et 3 animateurs.\n\n" +
               "Titre: " + eventTitle + "\n" +
               "Type: " + eventType + "\n" +
               "Début: " + startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
               "Fin: " + endTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n\n" +
               "Noms tunisiens pour animateurs: Mohamed Ben Ali, Sarra Trabelsi, Youssef Chaabane.\n\n" +
               "Réponds UNIQUEMENT avec ce JSON valide, sans texte avant ou après:\n" +
               "{\"planning\":[{\"heure_debut\":\"HH:mm\",\"heure_fin\":\"HH:mm\",\"activite\":\"nom\",\"lieu\":\"salle\",\"animateurs\":[\"Nom\"],\"type\":\"accueil\"}]," +
               "\"animateurs\":[{\"nom\":\"Nom\",\"role\":\"Role\",\"specialite\":\"Domaine\",\"statut\":\"Confirme\"}]," +
               "\"notes\":\"note\"}\n\n" +
               "Règles strictes:\n" +
               "- EXACTEMENT 6 activités maximum\n" +
               "- EXACTEMENT 3 animateurs maximum\n" +
               "- Pas de champ 'description' ni 'capacite'\n" +
               "- JSON compact, pas d'espaces inutiles\n" +
               "- Le JSON DOIT être complet et fermé avec }";
    }

    private String parsePlanningResponse(String response) {
        try {
            // Nettoyer la réponse (enlever markdown si présent)
            String cleaned = response
                .replaceAll("```json\\n?", "")
                .replaceAll("```\\n?", "")
                .trim();
            
            // Valider que c'est du JSON
            if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                return cleaned;
            }
            
            // Essayer d'extraire le JSON
            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");
            if (start >= 0 && end > start) {
                return cleaned.substring(start, end + 1);
            }
            
            return generateDefaultPlanning("", null, null);
        } catch (Exception e) {
            return generateDefaultPlanning("", null, null);
        }
    }

    private String generateDefaultPlanning(String eventType, LocalDateTime start, LocalDateTime end) {
        return "{\n" +
               "  \"planning\": [\n" +
               "    {\n" +
               "      \"heure_debut\": \"09:00\",\n" +
               "      \"heure_fin\": \"09:30\",\n" +
               "      \"activite\": \"Accueil & Inscription\",\n" +
               "      \"description\": \"Accueil des participants et enregistrement\",\n" +
               "      \"lieu\": \"Hall d'entrée\",\n" +
               "      \"animateurs\": [\"Mohamed Ben Ali\"],\n" +
               "      \"type\": \"accueil\",\n" +
               "      \"capacite\": 100\n" +
               "    },\n" +
               "    {\n" +
               "      \"heure_debut\": \"09:30\",\n" +
               "      \"heure_fin\": \"10:00\",\n" +
               "      \"activite\": \"Présentation générale\",\n" +
               "      \"description\": \"Présentation de l'événement et des objectifs\",\n" +
               "      \"lieu\": \"Amphithéâtre\",\n" +
               "      \"animateurs\": [\"Sarra Trabelsi\"],\n" +
               "      \"type\": \"presentation\",\n" +
               "      \"capacite\": 100\n" +
               "    },\n" +
               "    {\n" +
               "      \"heure_debut\": \"10:00\",\n" +
               "      \"heure_fin\": \"10:30\",\n" +
               "      \"activite\": \"Pause Café\",\n" +
               "      \"description\": \"Pause et networking\",\n" +
               "      \"lieu\": \"Espace café\",\n" +
               "      \"animateurs\": [],\n" +
               "      \"type\": \"pause\",\n" +
               "      \"capacite\": 100\n" +
               "    }\n" +
               "  ],\n" +
               "  \"animateurs\": [\n" +
               "    {\n" +
               "      \"nom\": \"Mohamed Ben Ali\",\n" +
               "      \"role\": \"Responsable académique\",\n" +
               "      \"specialite\": \"Gestion d'événements\",\n" +
               "      \"statut\": \"Confirmé\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"nom\": \"Sarra Trabelsi\",\n" +
               "      \"role\": \"Coordinatrice pédagogique\",\n" +
               "      \"specialite\": \"Formation et développement\",\n" +
               "      \"statut\": \"Confirmé\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"notes\": \"Planning généré automatiquement. À personnaliser selon vos besoins.\"\n" +
               "}";
    }

    /**
     * Extrait les informations du planning JSON.
     */
    public Map<String, Object> parsePlanning(String planningJson) {
        Map<String, Object> result = new HashMap<>();
        try {
            // Parsing simple du JSON (sans dépendance externe)
            result.put("raw", planningJson);
            result.put("valid", planningJson.contains("\"planning\""));
        } catch (Exception e) {
            result.put("valid", false);
        }
        return result;
    }
}
