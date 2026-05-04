package tn.esprit.entities;

public class Participation {

    private int id;
    private int equipeId;
    private int evenementId;
    private String statut;
    private String feedbacks;
    private Integer tableNumero; // NULL = pas de réservation salle 3D (compatible Symfony)

    public Participation() {}

    public Participation(int equipeId, int evenementId) {
        this.equipeId = equipeId;
        this.evenementId = evenementId;
        this.statut = "En attente";
    }

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

    public Integer getTableNumero() { return tableNumero; }
    public void setTableNumero(Integer tableNumero) { this.tableNumero = tableNumero; }

    @Override
    public String toString() {
        return "Participation{id=" + id + ", equipeId=" + equipeId
                + ", evenementId=" + evenementId + ", statut='" + statut + "'}";
    }
}
