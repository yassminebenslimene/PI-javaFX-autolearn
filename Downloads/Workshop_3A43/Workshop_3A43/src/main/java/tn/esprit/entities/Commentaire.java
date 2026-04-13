package tn.esprit.entities;

import java.time.LocalDateTime;

public class Commentaire {
    private int id;
    private String contenu;
    private LocalDateTime createdAt;
    private String sentiment;
    private double sentimentScore;
    private int postId;   // FK vers post.id (orphanRemoval depuis Post)
    private int userId;

    public Commentaire() {}

    public Commentaire(String contenu, int postId, int userId) {
        this.contenu = contenu;
        this.postId = postId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }

    public Commentaire(int id, String contenu, LocalDateTime createdAt, String sentiment,
                       double sentimentScore, int postId, int userId) {
        this.id = id;
        this.contenu = contenu;
        this.createdAt = createdAt;
        this.sentiment = sentiment;
        this.sentimentScore = sentimentScore;
        this.postId = postId;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }

    public int getPostId() { return postId; }
    public void setPostId(int postId) { this.postId = postId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Commentaire{id=" + id + ", contenu='" + contenu + "', postId=" + postId + ", userId=" + userId + '}';
    }
}
