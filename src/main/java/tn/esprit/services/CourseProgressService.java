package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CourseProgressService — toute la logique métier de progression.
 *
 * Règle principale : un chapitre est "complété" seulement si l'étudiant
 * a réussi son quiz (score >= seuil de réussite du quiz, défaut 50%).
 *
 * La table SQL "chapter_progress" est créée automatiquement si elle n'existe pas.
 */
public class CourseProgressService {

    private final Connection connection;

    public CourseProgressService() {
        this.connection = MyConnection.getInstance().getConnection();
        createTableIfNotExists();
    }

    // ── Création de la table ──────────────────────────────────────────────────
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS chapter_progress ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id INT NOT NULL,"
            + "chapitre_id INT NOT NULL,"
            + "cours_id INT NULL DEFAULT NULL,"
            + "quiz_score INT NOT NULL DEFAULT 0,"
            + "completed_at DATETIME NOT NULL,"
            + "UNIQUE KEY uq_user_chapitre (user_id, chapitre_id)"
            + ")";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("CourseProgressService init: " + e.getMessage());
        }
        // Ajouter cours_id si la table existait sans cette colonne
        // NULL autorisé pour compatibilité avec Symfony (intégration future)
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "chapter_progress", "cours_id");
            if (!rs.next()) {
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate("ALTER TABLE chapter_progress ADD COLUMN cours_id INT NULL DEFAULT NULL");
                    System.out.println(">>> Colonne cours_id ajoutée à chapter_progress (nullable)");
                }
            }
        } catch (SQLException e) {
            System.err.println("ALTER chapter_progress: " + e.getMessage());
        }
    }

    // ── Marquer un chapitre comme complété ────────────────────────────────────
    /**
     * Appelé après un quiz réussi.
     * Crée ou met à jour l'enregistrement de progression pour ce chapitre.
     * (INSERT ... ON DUPLICATE KEY UPDATE pour gérer les re-tentatives)
     *
     * @param userId     ID de l'étudiant connecté
     * @param chapitreId ID du chapitre complété
     * @param coursId    ID du cours parent
     * @param quizScore  score obtenu en % (ex: 75)
     */
    public void markChapterCompleted(int userId, int chapitreId, int coursId, int quizScore) {
        System.out.println(">>> markChapterCompleted appelé: userId=" + userId
            + " chapitreId=" + chapitreId + " coursId=" + coursId + " score=" + quizScore);
        String sql = "INSERT INTO chapter_progress (user_id, chapitre_id, cours_id, quiz_score, completed_at) "
            + "VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE quiz_score = GREATEST(quiz_score, ?), completed_at = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            ps.setInt(3, coursId);
            ps.setInt(4, quizScore);
            ps.setTimestamp(5, now);
            ps.setInt(6, quizScore);
            ps.setTimestamp(7, now);
            int rows = ps.executeUpdate();
            System.out.println(">>> INSERT chapter_progress: " + rows + " ligne(s) affectée(s)");
        } catch (SQLException e) {
            System.err.println(">>> ERREUR markChapterCompleted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Vérifier si un chapitre est complété ──────────────────────────────────
    public boolean isChapterCompleted(int userId, int chapitreId) {
        String sql = "SELECT id FROM chapter_progress WHERE user_id = ? AND chapitre_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, chapitreId);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    // ── Progression d'un cours (0 à 100) ─────────────────────────────────────
    /**
     * Calcule le pourcentage de progression d'un cours pour un étudiant.
     * Formule : (chapitres complétés / total chapitres) × 100
     *
     * @return entier entre 0 et 100
     */
    public int getCourseProgress(int userId, int coursId) {
        int total = 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM chapitre WHERE cours_id = ?")) {
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) total = rs.getInt(1);
        } catch (SQLException e) { return 0; }

        if (total == 0) return 0;

        int completed = 0;
        // Chercher par cours_id OU par chapitre_id appartenant au cours (si cours_id est NULL)
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM chapter_progress cp "
                + "WHERE cp.user_id = ? AND ("
                + "  cp.cours_id = ? "
                + "  OR (cp.cours_id IS NULL AND cp.chapitre_id IN "
                + "      (SELECT id FROM chapitre WHERE cours_id = ?))"
                + ")")) {
            ps.setInt(1, userId); ps.setInt(2, coursId); ps.setInt(3, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) completed = rs.getInt(1);
        } catch (SQLException e) { return 0; }

        int result = (int) Math.round((completed * 100.0) / total);
        System.out.println(">>> getCourseProgress: userId=" + userId + " coursId=" + coursId
            + " total=" + total + " completed=" + completed + " => " + result + "%");
        return result;
    }

    // ── Liste des chapitres complétés pour un cours ───────────────────────────
    public List<Integer> getCompletedChapitreIds(int userId, int coursId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT chapitre_id FROM chapter_progress cp "
            + "WHERE cp.user_id = ? AND ("
            + "  cp.cours_id = ? "
            + "  OR (cp.cours_id IS NULL AND cp.chapitre_id IN "
            + "      (SELECT id FROM chapitre WHERE cours_id = ?))"
            + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, coursId); ps.setInt(3, coursId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("chapitre_id"));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return ids;
    }

    // ── Score du quiz pour un chapitre ────────────────────────────────────────
    public int getQuizScore(int userId, int chapitreId) {
        String sql = "SELECT quiz_score FROM chapter_progress WHERE user_id = ? AND chapitre_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, chapitreId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("quiz_score");
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return 0;
    }

    // ── Stats globales d'un étudiant ──────────────────────────────────────────
    public int getTotalCompletedChapitres(int userId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM chapter_progress WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return 0;
    }
}
