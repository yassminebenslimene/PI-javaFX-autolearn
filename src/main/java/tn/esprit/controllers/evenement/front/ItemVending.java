package tn.esprit.controllers.evenement.front;

/**
 * Représente un item (boisson ou snack) dans la vending machine.
 * Données in-memory uniquement — aucune persistance DB.
 */
public record ItemVending(
        String nom,
        String emoji,
        double prixTND,
        int calories,   // enrichi par OpenFoodFacts, 0 si indisponible
        int sucreG      // enrichi par OpenFoodFacts, 0 si indisponible
) {
    /** Constructeur simplifié sans données nutritionnelles */
    public ItemVending(String nom, String emoji, double prixTND) {
        this(nom, emoji, prixTND, 0, 0);
    }
}
