package tn.esprit.entities;

public class Communaute {
    private int id;
    private String nom;
    private String description;
    private int ownerId;

    public Communaute() {}

    public Communaute(String nom, String description, int ownerId) {
        this.nom = nom;
        this.description = description;
        this.ownerId = ownerId;
    }

    public Communaute(int id, String nom, String description, int ownerId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.ownerId = ownerId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    @Override
    public String toString() {
        return "Communaute{id=" + id + ", nom='" + nom + "', description='" + description + "', ownerId=" + ownerId + '}';
    }
}
