package tn.esprit.entities;

import java.time.LocalDateTime;

/**
 * Entit├® Question ÔÇö repr├®sente une question appartenant ├á un quiz.
 * Correspond ├á la table "question" en SQL.
 * Une question est li├®e ├á un quiz via quizId (relation ManyToOne).
 */
public class Question {

    // Identifiant unique de la question (auto-incr├®ment├® par la BDD)
    private int id;

    // Texte de la question (entre 10 et 1000 caract├¿res)
    private String texteQuestion;

    // Nombre de points attribu├®s si la bonne r├®ponse est choisie (entre 1 et 100)
    private int point;

    // Date et heure de la derni├¿re modification
    private LocalDateTime updatedAt;

    // Identifiant du quiz auquel appartient cette question (cl├® ├®trang├¿re)
    private int quizId;

    // Constructeur vide (n├®cessaire pour JavaFX et JDBC)
    public Question() {
    }

    // Constructeur sans id (utilis├® pour cr├®er une nouvelle question avant insertion en BDD)
    public Question(String texteQuestion, int point, LocalDateTime updatedAt, int quizId) {
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.updatedAt = updatedAt;
        this.quizId = quizId;
    }

    // Constructeur complet avec id (utilis├® quand on lit une question depuis la BDD)
    public Question(int id, String texteQuestion, int point, LocalDateTime updatedAt, int quizId) {
        this.id = id;
        this.texteQuestion = texteQuestion;
        this.point = point;
        this.updatedAt = updatedAt;
        this.quizId = quizId;
    }

    // ÔöÇÔöÇ Getters et Setters ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ

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