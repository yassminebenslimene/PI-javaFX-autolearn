package tn.esprit.entities;

import java.time.LocalDateTime;

/**
 * ChapterProgress — table de liaison entre un User et un Chapitre.
 * Un enregistrement est créé quand l'étudiant réussit le quiz d'un chapitre.
 * Correspond à la table SQL "chapter_progress".
 */
public class ChapterProgress {

    private int           id;
    private int           userId;      // FK → user
    private int           chapitreId;  // FK → chapitre
    private int           coursId;     // FK → cours (pour calculer la progression du cours)
    private int           quizScore;   // score obtenu au quiz (en %)
    private LocalDateTime completedAt; // date/heure de complétion

    public ChapterProgress() {}

    public ChapterProgress(int userId, int chapitreId, int coursId, int quizScore) {
        this.userId     = userId;
        this.chapitreId = chapitreId;
        this.coursId    = coursId;
        this.quizScore  = quizScore;
        this.completedAt = LocalDateTime.now();
    }

    public int           getId()          { return id; }
    public void          setId(int id)    { this.id = id; }
    public int           getUserId()      { return userId; }
    public void          setUserId(int v) { this.userId = v; }
    public int           getChapitreId()  { return chapitreId; }
    public void          setChapitreId(int v) { this.chapitreId = v; }
    public int           getCoursId()     { return coursId; }
    public void          setCoursId(int v){ this.coursId = v; }
    public int           getQuizScore()   { return quizScore; }
    public void          setQuizScore(int v){ this.quizScore = v; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void          setCompletedAt(LocalDateTime v){ this.completedAt = v; }
}
