package tn.esprit.entities;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class UserChallenge {

    private int id;
    private int userId;
    private int challengeId;
    private int currentIndex;
    private Map<Integer, String> answersMap;
    private int score;
    private int totalPoints;
    private boolean completed;
    private LocalDateTime completedAt;

    // Constructeurs
    public UserChallenge() {
        this.answersMap = new HashMap<>();
        this.currentIndex = 0;
        this.score = 0;
        this.totalPoints = 0;
        this.completed = false;
    }

    public UserChallenge(int userId, int challengeId) {
        this();
        this.userId = userId;
        this.challengeId = challengeId;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(int challengeId) {
        this.challengeId = challengeId;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public Map<Integer, String> getAnswersMap() {
        return answersMap;
    }

    public void setAnswersMap(Map<Integer, String> answersMap) {
        this.answersMap = answersMap;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "UserChallenge{" +
                "id=" + id +
                ", userId=" + userId +
                ", challengeId=" + challengeId +
                ", currentIndex=" + currentIndex +
                ", score=" + score +
                ", totalPoints=" + totalPoints +
                ", completed=" + completed +
                '}';
    }
}