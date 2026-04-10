package tn.esprit.entities;

import java.time.LocalDateTime;

public class Question {
    private int id;
    private String texteQuestion;
    private int point;
    private LocalDateTime updatedAt;
    private int quizId;

    public Question() {
    }

    public Question(String texteQuestion, int point, LocalDateTime updatedAt, int quizId) {
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.updatedAt = updatedAt;
        this.quizId = quizId;
    }

    public Question(int id, String texteQuestion, int point, LocalDateTime updatedAt, int quizId) {
        this.id = id;
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.updatedAt = updatedAt;
        this.quizId = quizId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTexteQuestion() {
        return texteQuestion;
    }

    public void setTexteQuestion(String texteQuestion) {
        this.texteQuestion = texteQuestion;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", texteQuestion='" + texteQuestion + '\'' +
                ", point=" + point +
                ", updatedAt=" + updatedAt +
                ", quizId=" + quizId +
                '}';
    }
}
