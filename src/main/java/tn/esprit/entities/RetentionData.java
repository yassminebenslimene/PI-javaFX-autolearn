package tn.esprit.entities;

public class RetentionData {
    private String cohorte;
    private int moisRelatif;
    private int nbEtudiants;
    private double tauxRetention;

    public RetentionData() {}

    public RetentionData(String cohorte, int moisRelatif, int nbEtudiants, double tauxRetention) {
        this.cohorte = cohorte;
        this.moisRelatif = moisRelatif;
        this.nbEtudiants = nbEtudiants;
        this.tauxRetention = tauxRetention;
    }

    public String getCohorte() { return cohorte; }
    public void setCohorte(String cohorte) { this.cohorte = cohorte; }

    public int getMoisRelatif() { return moisRelatif; }
    public void setMoisRelatif(int moisRelatif) { this.moisRelatif = moisRelatif; }

    public int getNbEtudiants() { return nbEtudiants; }
    public void setNbEtudiants(int nbEtudiants) { this.nbEtudiants = nbEtudiants; }

    public double getTauxRetention() { return tauxRetention; }
    public void setTauxRetention(double tauxRetention) { this.tauxRetention = tauxRetention; }

    public String getMoisRelatifLabel() {
        if (moisRelatif == 0) return "Mois 0 (Inscription)";
        if (moisRelatif == 1) return "Mois 1";
        if (moisRelatif == 2) return "Mois 2";
        if (moisRelatif == 3) return "Mois 3";
        return "Mois " + moisRelatif;
    }
}