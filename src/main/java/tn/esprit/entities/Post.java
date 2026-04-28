package tn.esprit.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private int id;
    private String contenu;
    private String titre;
    private String aiReaction;
    private String aiReactionData;
    private String summary;
    private String imageFile;
    private String videoFile;
    private LocalDateTime createdAt;
    private int communauteId; // FK vers communaute.id
    private int userId;

    // OneToMany : commentaires du post (orphanRemoval)
    private List<Commentaire> commentaires = new ArrayList<>();

    public Post() {}

    public Post(String contenu, String titre, int communauteId, int userId) {
        this.contenu = contenu;
        this.titre = titre;
        this.communauteId = communauteId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public Post(int id, String contenu, String titre, String aiReaction, String aiReactionData,
                String summary, String imageFile, String videoFile, LocalDateTime createdAt,
                int communauteId, int userId) {
        this.id = id;
        this.contenu = contenu;
        this.titre = titre;
        this.aiReaction = aiReaction;
        this.aiReactionData = aiReactionData;
        this.summary = summary;
        this.imageFile = imageFile;
        this.videoFile = videoFile;
        this.createdAt = createdAt;
        this.communauteId = communauteId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getAiReaction() { return aiReaction; }
    public void setAiReaction(String aiReaction) { this.aiReaction = aiReaction; }

    public String getAiReactionData() { return aiReactionData; }
    public void setAiReactionData(String aiReactionData) { this.aiReactionData = aiReactionData; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getImageFile() { return imageFile; }
    public void setImageFile(String imageFile) { this.imageFile = imageFile; }

    public String getVideoFile() { return videoFile; }
    public void setVideoFile(String videoFile) { this.videoFile = videoFile; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getCommunauteId() { return communauteId; }
    public void setCommunauteId(int communauteId) { this.communauteId = communauteId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public List<Commentaire> getCommentaires() { return commentaires; }
    public void setCommentaires(List<Commentaire> commentaires) { this.commentaires = commentaires; }

    @Override
    public String toString() {
        return "Post{id=" + id + ", titre='" + titre + "', communauteId=" + communauteId + ", userId=" + userId + '}';
    }
}
