package tn.esprit.entities;

import java.util.Objects;

public class Exercice {

    private int id;
    private String question;
    private String reponse;
    private int points;
    private Integer challengeId; // Peut être null car le challenge est optionnel

    // Constructeur par défaut
    public Exercice() {}

    // Constructeur avec tous les champs
    public Exercice(int id, String question, String reponse, int points, Integer challengeId) {
        this.id = id;
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.challengeId = challengeId;
    }
    public Exercice(int id, String question, String reponse, int points) {
        this.id = id;
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.challengeId = null;
    }
    // Constructeur sans ID (pour la création)
    public Exercice(String question, String reponse, int points, Integer challengeId) {
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.challengeId = challengeId;
    }

    // Constructeur simple sans challenge
    public Exercice(String question, String reponse, int points) {
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.challengeId = null;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public Integer getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(Integer challengeId) {
        this.challengeId = challengeId;
    }

    // Méthode utilitaire pour vérifier si l'exercice est associé à un challenge
    public boolean hasChallenge() {
        return challengeId != null && challengeId > 0;
    }

    @Override
    public String toString() {
        return "Exercice{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", reponse='" + reponse + '\'' +
                ", points=" + points +
                ", challengeId=" + challengeId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exercice exercice = (Exercice) o;
        return id == exercice.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
