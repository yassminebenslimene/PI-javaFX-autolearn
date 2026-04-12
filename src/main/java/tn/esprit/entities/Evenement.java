package tn.esprit.entities;

import java.time.LocalDateTime;

public class Evenement {

    private int id;
    private String titre;
    private String lieu;
    private String description;
    private String type;           // Hackathon, Conference, Workshop, Competition
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String status;         // Planifie, En_cours, Passe, Annule
    private boolean isCanceled;
    private String workflowStatus; // planifie, en_cours, termine, annule
    private int nbMax;

    public Evenement() {}

    public Evenement(String titre, String lieu, String description, String type,
                     LocalDateTime dateDebut, LocalDateTime dateFin, int nbMax) {
        this.titre = titre;
        this.lieu = lieu;
        this.description = description;
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.nbMax = nbMax;
        this.status = "Plannifié";
        this.workflowStatus = "planifie";
        this.isCanceled = false;
    }

    // Calcule le statut dynamiquement selon les dates
    public String computeStatus() {
        if (isCanceled) return "Annulé";
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(dateFin)) return "Passé";
        if (!now.isBefore(dateDebut)) return "En cours";
        return "Plannifié";
    }

    // Getters / Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isIsCanceled() { return isCanceled; }
    public void setIsCanceled(boolean isCanceled) { this.isCanceled = isCanceled; }

    public String getWorkflowStatus() { return workflowStatus; }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }

    public int getNbMax() { return nbMax; }
    public void setNbMax(int nbMax) { this.nbMax = nbMax; }

    @Override
    public String toString() {
        return "Evenement{id=" + id + ", titre='" + titre + "', type='" + type + "', status='" + status + "'}";
    }
}
