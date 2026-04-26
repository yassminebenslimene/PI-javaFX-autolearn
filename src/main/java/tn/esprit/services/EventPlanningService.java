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
        return "Génère un planning détaillé et professionnel pour cet événement:\n\n" +
               "Titre: " + eventTitle + "\n" +
               "Type: " + eventType + "\n" +
               "Début: " + startTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
               "Fin: " + endTime.format(DateTimeFormatter.ofPattern("HH:mm")) + "\n" +
               "Participants: " + nbParticipants + "\n\n" +
               "IMPORTANT: Utilise UNIQUEMENT des prénoms et noms tunisiens arabes pour les animateurs " +
               "(ex: Mohamed Ben Ali, Sarra Trabelsi, Youssef Chaabane, Ines Belhaj, Karim Mansouri, " +
               "Nour Hamdi, Anis Bouazizi, Rania Khelifi, Tarek Jebali, Fatma Zouari).\n" +
               "Les rôles doivent être des postes réels et spécialisés selon le type d'événement:\n" +
               "- Pour Hackathon: Expert en développement logiciel, Coach en innovation, Jury technique, Expert en cybersécurité, Mentor startup\n" +
               "- Pour Conference: Conférencier expert, Modérateur de panel, Expert en recherche, Responsable académique\n" +
               "- Pour Workshop: Formateur expert, Coach pratique, Expert technique, Facilitateur\n\n" +
               "Réponds UNIQUEMENT en JSON valide (sans markdown, sans texte avant ou après) avec cette structure exacte:\n" +
               "{\n" +
               "  \"planning\": [\n" +
               "    {\n" +
               "      \"heure_debut\": \"HH:mm\",\n" +
               "      \"heure_fin\": \"HH:mm\",\n" +
               "      \"activite\": \"Nom de l'activité\",\n" +
               "      \"description\": \"Description courte\",\n" +
               "      \"lieu\": \"Salle/Espace\",\n" +
               "      \"animateurs\": [\"Prénom Nom\"],\n" +
               "      \"type\": \"accueil\",\n" +
               "      \"capacite\": 50\n" +
               "    }\n" +
               "  ],\n" +
               "  \"animateurs\": [\n" +
               "    {\n" +
               "      \"nom\": \"Prénom Nom tunisien\",\n" +
               "      \"role\": \"Poste réel spécialisé\",\n" +
               "      \"specialite\": \"Domaine d'expertise lié au sujet\",\n" +
               "      \"statut\": \"Confirmé\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"notes\": \"Notes importantes\"\n" +
               "}\n\n" +
               "Assure-toi que:\n" +
               "- Le planning couvre toute la durée de l'événement\n" +
               "- Les pauses sont bien placées (café 10h30, déjeuner 12h30)\n" +
               "- Les activités sont adaptées au type d'événement\n" +
               "- Les animateurs ont des noms tunisiens arabes authentiques\n" +
               "- Les rôles sont des postes professionnels réels et spécialisés\n" +
               "- Le JSON est valide et complet";
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
