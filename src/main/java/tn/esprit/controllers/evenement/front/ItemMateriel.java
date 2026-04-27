package tn.esprit.controllers.evenement.front;

/**
 * Classe pour représenter un item d'emprunt de matériel.
 */
public class ItemMateriel {
    public String emoji;
    public String nom;
    public boolean disponible;
    public String emprunteurNom;
    public int dureeHeures;

    public ItemMateriel() {
        this.disponible = true;
        this.emprunteurNom = null;
        this.dureeHeures = 0;
    }
}
