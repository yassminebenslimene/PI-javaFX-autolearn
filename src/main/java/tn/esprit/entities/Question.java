package tn.esprit.entities;

import java.time.LocalDateTime;

/**
 * Entité Question — représente une question appartenant à un quiz.
 * Correspond à la table "question" en SQL.
 * Une question est liée à un quiz via quizId (relation ManyToOne).
 */
public class Question {

    // Identifiant unique de la question (auto-incrémenté par la BDD)
    private int id;

    // Texte de la question (entre 10 et 1000 caractères)
    private String texteQuestion;

    // Nombre de points attribués si la bonne réponse est choisie (entre 1 et 100)
    private int point;

    // Date et heure de la dernière modification
    private LocalDateTime updatedAt;

    // Identifiant du quiz auquel appartient cette question (clé étrangère)
    private int quizId;

    // Constructeur vide (nécessaire pour JavaFX et JDBC)
    public Question() {
    }

    // Constructeur sans id (utilisé pour créer une nouvelle question avant insertion en BDD)
    public Question(String texteQuestion, int point, LocalDateTime updatedAt, int quizId) {
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.updatedAt = updatedAt;
        this.quizId = quizId;
    }

    // Constructeur complet avec id (utilisé quand on lit une question depuis la BDD)
    public Question(int id, String texteQuestion, int point, LocalDateTime updatedAt, int quizId) {
        this.id = id;
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.updatedAt = updatedAt;
        this.quizId = quizId;
    }

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTexteQuestion() { return texteQuestion; }
    public void setTexteQuestion(String texteQuestion) { this.texteQuestion = texteQuestion; }

    public int getPoint() { return point; }
    public void setPoint(int point) { this.point = point; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Retourne l'id du quiz parent
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    // Affichage de la question sous forme de texte (utile pour le debug)
    @Override
    public String toString() {
        return "Question{id=" + id + ", texte='" + texteQuestion + "', point=" + point + ", quizId=" + quizId + "}";
    }
}
