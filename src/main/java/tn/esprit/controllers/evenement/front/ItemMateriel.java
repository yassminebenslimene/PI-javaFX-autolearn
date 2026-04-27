package tn.esprit.controllers.evenement.front;

/**
 * Représente un item de matériel empruntable pendant un événement.
 * État géré in-memory uniquement — aucune persistance DB.
 */
public class ItemMateriel {
    public String nom;
    public String emoji;
    public boolean disponible;
    public String emprunteurNom;  // null si disponible
    public int dureeHeures;       // 0 si disponible

    /** Constructeur no-arg requis par EmpruntMaterielController.initItems() */
    public ItemMateriel() {
        this.disponible = true;
        this.emprunteurNom = null;
        this.dureeHeures = 0;
    }

    public ItemMateriel(String nom, String emoji) {
        this.nom = nom;
        this.emoji = emoji;
        this.disponible = true;
        this.emprunteurNom = null;
        this.dureeHeures = 0;
    }
}
