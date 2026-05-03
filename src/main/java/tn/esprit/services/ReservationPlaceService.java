package tn.esprit.services;

import java.util.Map;

/**
 * Façade de réservation de place dans la salle 3D.
 * Délègue entièrement à ParticipationService via la colonne table_numero (NULL par défaut).
 * Aucune nouvelle table en base — compatible avec Symfony.
 */
public class ReservationPlaceService {

    private final ParticipationService participationService = new ParticipationService();

    /** Réserve une table. Retourne true si succès, false si déjà prise. */
    public boolean reserverPlace(int evenementId, int equipeId, int tableNumero) {
        return participationService.reserverTable(evenementId, equipeId, tableNumero);
    }

    /** Libère la réservation (remet table_numero à NULL). */
    public void libererPlace(int evenementId, int equipeId) {
        participationService.libererTable(evenementId, equipeId);
    }

    /** Retourne la map tableNumero → equipeId pour colorier les tables. */
    public Map<Integer, Integer> getReservations(int evenementId) {
        return participationService.getReservationsTable(evenementId);
    }

    /** Retourne le numéro de table de l'équipe, ou -1 si aucune. */
    public int getTableByEquipe(int evenementId, int equipeId) {
        return participationService.getTableByEquipe(evenementId, equipeId);
    }
}
