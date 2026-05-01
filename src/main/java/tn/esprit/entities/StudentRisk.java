package tn.esprit.entities;

import java.time.LocalDateTime;

public class StudentRisk {
    private int userId;
    private String prenom;
    private String nom;
    private String niveau;
    private int totalTentatives;
    private int challengesTermines;
    private double scoreMoyen;
    private LocalDateTime derniereActivite;
    private long joursInactivite;
    private int challengesAbandonnes;
    private String statutRisque;
    private double tauxAbandon;

    public StudentRisk() {}

    public StudentRisk(int userId, String prenom, String nom, String niveau,
                       int totalTentatives, int challengesTermines, double scoreMoyen,
                       LocalDateTime derniereActivite, long joursInactivite,
                       int challengesAbandonnes, String statutRisque, double tauxAbandon) {
        this.userId = userId;
        this.prenom = prenom;
        this.nom = nom;
        this.niveau = niveau;
        this.totalTentatives = totalTentatives;
        this.challengesTermines = challengesTermines;
        this.scoreMoyen = scoreMoyen;
        this.derniereActivite = derniereActivite;
        this.joursInactivite = joursInactivite;
        this.challengesAbandonnes = challengesAbandonnes;
        this.statutRisque = statutRisque;
        this.tauxAbandon = tauxAbandon;
    }

    // Getters et Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public int getTotalTentatives() { return totalTentatives; }
    public void setTotalTentatives(int totalTentatives) { this.totalTentatives = totalTentatives; }

    public int getChallengesTermines() { return challengesTermines; }
    public void setChallengesTermines(int challengesTermines) { this.challengesTermines = challengesTermines; }

    public double getScoreMoyen() { return scoreMoyen; }
    public void setScoreMoyen(double scoreMoyen) { this.scoreMoyen = scoreMoyen; }

    public LocalDateTime getDerniereActivite() { return derniereActivite; }
    public void setDerniereActivite(LocalDateTime derniereActivite) { this.derniereActivite = derniereActivite; }

    public long getJoursInactivite() { return joursInactivite; }
    public void setJoursInactivite(long joursInactivite) { this.joursInactivite = joursInactivite; }

    public int getChallengesAbandonnes() { return challengesAbandonnes; }
    public void setChallengesAbandonnes(int challengesAbandonnes) { this.challengesAbandonnes = challengesAbandonnes; }

    public String getStatutRisque() { return statutRisque; }
    public void setStatutRisque(String statutRisque) { this.statutRisque = statutRisque; }

    public double getTauxAbandon() { return tauxAbandon; }
    public void setTauxAbandon(double tauxAbandon) { this.tauxAbandon = tauxAbandon; }

    public String getNomComplet() { return prenom + " " + nom; }
}