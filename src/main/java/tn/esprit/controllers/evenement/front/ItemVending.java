package tn.esprit.controllers.evenement.front;

/**
 * Classe pour représenter un item de la machine à vendre.
 */
public class ItemVending {
    private String nom;
    private String emoji;
    private double prixTND;

    public ItemVending(String nom, String emoji, double prixTND) {
        this.nom = nom;
        this.emoji = emoji;
        this.prixTND = prixTND;
    }

    public String nom() { return nom; }
    public String emoji() { return emoji; }
    public double prixTND() { return prixTND; }
}
