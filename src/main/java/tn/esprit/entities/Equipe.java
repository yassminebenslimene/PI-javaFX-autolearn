package tn.esprit.entities;

import java.util.ArrayList;
import java.util.List;

public class Equipe {

    private int id;
    private String nom;
    private int evenementId;
    private List<Etudiant> etudiants = new ArrayList<>();

    public Equipe() {}

    public Equipe(String nom, int evenementId) {
        this.nom = nom;
        this.evenementId = evenementId;
    }

    // Getters / Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public List<Etudiant> getEtudiants() { return etudiants; }
    public void setEtudiants(List<Etudiant> etudiants) { this.etudiants = etudiants; }

    @Override
    public String toString() {
        return "Equipe{id=" + id + ", nom='" + nom + "', evenementId=" + evenementId + "}";
    }
}
