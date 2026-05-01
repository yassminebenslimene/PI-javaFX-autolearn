package tn.esprit.entities;

public class LeaderboardEntry {
    private int userId;
    private String prenom;
    private String nom;
    private String niveau;
    private int rang;
    private int challengesCompletes;
    private int totalPoints;
    private double moyenne;
    private String medailles;

    public LeaderboardEntry() {}

    public LeaderboardEntry(int userId, String prenom, String nom, String niveau,
                            int rang, int challengesCompletes, int totalPoints,
                            double moyenne, String medailles) {
        this.userId = userId;
        this.prenom = prenom;
        this.nom = nom;
        this.niveau = niveau;
        this.rang = rang;
        this.challengesCompletes = challengesCompletes;
        this.totalPoints = totalPoints;
        this.moyenne = moyenne;
        this.medailles = medailles;
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

    public int getRang() { return rang; }
    public void setRang(int rang) { this.rang = rang; }

    public int getChallengesCompletes() { return challengesCompletes; }
    public void setChallengesCompletes(int challengesCompletes) { this.challengesCompletes = challengesCompletes; }

    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }

    public double getMoyenne() { return moyenne; }
    public void setMoyenne(double moyenne) { this.moyenne = moyenne; }

    public String getMedailles() { return medailles; }
    public void setMedailles(String medailles) { this.medailles = medailles; }

    public String getNomComplet() { return prenom + " " + nom; }
}