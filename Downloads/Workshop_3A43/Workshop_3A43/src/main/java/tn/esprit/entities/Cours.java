package tn.esprit.entities;

import java.time.LocalDateTime;

public class Cours {
    private int id;
    private String titre;
    private String description;
    private String matiere;
    private String niveau;
    private int duree;
    private LocalDateTime createdAt;

    public Cours() {
    }

    public Cours(String titre, String description, String matiere, String niveau, int duree, LocalDateTime createdAt) {
        this.titre = titre;
        this.description = description;
        this.matiere = matiere;
        this.niveau = niveau;
        this.duree = duree;
        this.createdAt = createdAt;
    }

    public Cours(int id, String titre, String description, String matiere, String niveau, int duree, LocalDateTime createdAt) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.matiere = matiere;
        this.niveau = niveau;
        this.duree = duree;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMatiere() {
        return matiere;
    }

    public void setMatiere(String matiere) {
        this.matiere = matiere;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Cours{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", matiere='" + matiere + '\'' +
                ", niveau='" + niveau + '\'' +
                ", duree=" + duree +
                ", createdAt=" + createdAt +
                '}';
    }
}