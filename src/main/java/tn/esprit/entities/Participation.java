package tn.esprit.entities;

public class Participation {

    private int id;
    private int equipeId;
    private int evenementId;
    private String statut; // EN_ATTENTE, ACCEPTE, REFUSE
    private String feedbacks; // JSON stocké en String

    public Participation() {}

    public Participation(int equipeId, int evenementId) {
        this.equipeId = equipeId;
        this.evenementId = evenementId;
        this.statut = "En attente";
    }

    // Getters / Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEquipeId() { return equipeId; }
    public void setEquipeId(int equipeId) { this.equipeId = equipeId; }

    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getFeedbacks() { return feedbacks; }
    public void setFeedbacks(String feedbacks) { this.feedbacks = feedbacks; }

    @Override
    public String toString() {
        return "Participation{id=" + id + ", equipeId=" + equipeId + ", evenementId=" + evenementId + ", statut='" + statut + "'}";
    }
}
