package tn.esprit.controllers.evenement.front;

/**
 * Représente un plat ou item du menu déjeuner / pause café.
 * Données in-memory uniquement — aucune persistance DB.
 */
public record MenuItem(
        String nom,
        String emoji,
        String description,
        String categorie  // "dejeuner" | "cafe"
) {}
