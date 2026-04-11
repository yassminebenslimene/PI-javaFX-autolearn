package tn.esprit.entities;

import java.time.LocalDateTime;

public class Quiz {
    private int id;
    private String titre;
    private String description;
    private String etat;
    private Integer dureeMaxMinutes;
    private Integer seuilReussite;
    private Integer maxTentatives;
    private String imageName;
    private Integer imageSize;
    private LocalDateTime updatedAt;

    public Quiz() {
    }

    public Quiz(String titre, String description, String etat, Integer dureeMaxMinutes, Integer seuilReussite,
                Integer maxTentatives, String imageName, Integer imageSize, LocalDateTime updatedAt) {
        this.titre = titre;
        this.description = description;
        this.etat = etat;
        this.dureeMaxMinutes = dureeMaxMinutes;
        this.seuilReussite = seuilReussite;
        this.maxTentatives = maxTentatives;
        this.imageName = imageName;
        this.imageSize = imageSize;
        this.updatedAt = updatedAt;
    }

    public Quiz(int id, String titre, String description, String etat, Integer dureeMaxMinutes, Integer seuilReussite,
                Integer maxTentatives, String imageName, Integer imageSize, LocalDateTime updatedAt) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.etat = etat;
        this.dureeMaxMinutes = dureeMaxMinutes;
        this.seuilReussite = seuilReussite;
        this.maxTentatives = maxTentatives;
        this.imageName = imageName;
        this.imageSize = imageSize;
        this.updatedAt = updatedAt;
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

    public String getEtat() {
        return etat;
    }

    public void setEtat(String etat) {
        this.etat = etat;
    }

    public Integer getDureeMaxMinutes() {
        return dureeMaxMinutes;
    }

    public void setDureeMaxMinutes(Integer dureeMaxMinutes) {
        this.dureeMaxMinutes = dureeMaxMinutes;
    }

    public Integer getSeuilReussite() {
        return seuilReussite;
    }

    public void setSeuilReussite(Integer seuilReussite) {
        this.seuilReussite = seuilReussite;
    }

    public Integer getMaxTentatives() {
        return maxTentatives;
    }

    public void setMaxTentatives(Integer maxTentatives) {
        this.maxTentatives = maxTentatives;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Integer getImageSize() {
        return imageSize;
    }

    public void setImageSize(Integer imageSize) {
        this.imageSize = imageSize;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Quiz{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", etat='" + etat + '\'' +
                ", dureeMaxMinutes=" + dureeMaxMinutes +
                ", seuilReussite=" + seuilReussite +
                ", maxTentatives=" + maxTentatives +
                ", imageName='" + imageName + '\'' +
                ", imageSize=" + imageSize +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
