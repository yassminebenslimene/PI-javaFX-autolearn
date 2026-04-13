package tn.esprit.entities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Challenge {

    private int id;
    private String titre;
    private String description;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String niveau;
    private int duree;
    private int createdBy; // ID de l'utilisateur créateur

    // Listes pour les relations
    private List<Integer> exerciceIds = new ArrayList<>();
    private List<Integer> quizIds = new ArrayList<>();

    // Constructeurs
    public Challenge() {}

    public Challenge(String titre, String description, LocalDate dateDebut, LocalDate dateFin, String niveau, int duree) {
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.niveau = niveau;
        this.duree = duree;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public List<Integer> getExerciceIds() { return exerciceIds; }
    public void setExerciceIds(List<Integer> exerciceIds) { this.exerciceIds = exerciceIds; }

    public List<Integer> getQuizIds() { return quizIds; }
    public void setQuizIds(List<Integer> quizIds) { this.quizIds = quizIds; }

    @Override
    public String toString() {
        return "Challenge{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", niveau='" + niveau + '\'' +
                '}';
    }
}
