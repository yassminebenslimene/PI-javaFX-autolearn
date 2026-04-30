package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * SERVICE : PROGRESSION DES COURS
 * ═══════════════════════════════════════════════════════════════
 * Gère la progression de l'étudiant dans les chapitres et cours.
 *
 * FONCTIONNALITÉS :
 *   - Marquer un chapitre comme complété (après quiz réussi ≥ 50%)
 *   - Calculer le % de progression par cours
 *   - Récupérer les IDs des chapitres complétés
 *   - Gamification : points, streak, badges
 *   - Recommandations : prochain chapitre à faire
 *
 * TABLE BDD : chapter_progress
 *   user_id, chapitre_id, cours_id, quiz_score, is_completed, completed_at
 * ═══════════════════════════════════════════════════════════════
 */
public class CourseProgressService {

    private final Connection connection;

    public CourseProgressService() {
        this.connection = MyConnection.getInstance().getConnection();
        createTableIfNotExists();
    }

    // Crée la table chapter_progress si elle n'existe pas encore
    // et ajoute les colonnes manquantes si la table existait déjà
    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS chapter_progress ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id INT NOT NULL,"
            + "chapitre_id INT NOT NULL,"
            + "cours_id INT NULL DEFAULT NULL,"
            + "quiz_score INT NULL DEFAULT NULL,"
            + "is_completed TINYINT(1) NULL DEFAULT NULL,"
            + "completed_at DATETIME NULL DEFAULT NULL,"
            + "UNIQUE KEY uq_user_chapitre (user_id, chapitre_id)"
            + ")";
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("CourseProgressService init: " + e.getMessage());
        }

        ensureColumnExists("cours_id", "ALTER TABLE chapter_progress ADD COLUMN cours_id INT NULL DEFAULT NULL");
        ensureColumnExists("is_completed",
            "ALTER TABLE chapter_progress ADD COLUMN is_completed TINYINT(1) NULL DEFAULT NULL");
        // Ajouter colonnes manquantes si table existait deja
        String[] cols = {"quiz_score INT NULL DEFAULT NULL", "cours_id INT NULL DEFAULT NULL", "completed_at DATETIME NULL DEFAULT NULL"};
        String[] colNames = {"quiz_score", "cours_id", "completed_at"};
        for (int i = 0; i < cols.length; i++) {
            try { ResultSet rc = connection.getMetaData().getColumns(null, null, "chapter_progress", colNames[i]);
                if (!rc.next()) { try (Statement st2 = connection.createStatement()) { st2.executeUpdate("ALTER TABLE chapter_progress ADD COLUMN " + cols[i]); System.out.println(">>> Added column: " + colNames[i]); } }
            } catch (SQLException ex) { System.err.println("ALTER " + colNames[i] + ": " + ex.getMessage()); }
        }
    }

    private void ensureColumnExists(String columnName, String alterSql) {
        try {
            ResultSet rs = connection.getMetaData().getColumns(null, null, "chapter_progress", columnName);
            if (!rs.next()) {
                try (Statement st = connection.createStatement()) {
                    st.executeUpdate(alterSql);
                    System.out.println(">>> Colonne " + columnName + " ajoutee a chapter_progress");
                }
            }
        } catch (SQLException e) {
            System.err.println("ALTER chapter_progress " + columnName + ": " + e.getMessage());
        }
    }

    public void markChapterCompleted(int userId, int chapitreId, int coursId, int quizScore) {
        // Un chapitre est considéré complété si le score au quiz est >= 50%
        boolean isCompleted = quizScore >= 50;
        
        // INSERT ou UPDATE si la ligne existe déjà (ON DUPLICATE KEY)
        // On garde toujours le meilleur score (GREATEST)
        String sql = "INSERT INTO chapter_progress "
            + "(user_id, chapitre_id, cours_id, quiz_score, is_completed, completed_at) "
            + "VALUES (?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "cours_id = IFNULL(cours_id, ?), "
            + "quiz_score = GREATEST(IFNULL(quiz_score, 0), ?), "
            + "is_completed = IF(GREATEST(IFNULL(quiz_score, 0), ?) >= 50, 1, 0), "
            + "completed_at = IF(GREATEST(IFNULL(quiz_score, 0), ?) >= 50, ?, completed_at)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            ps.setInt(3, coursId);
            ps.setInt(4, quizScore);
            ps.setBoolean(5, isCompleted);
            ps.setTimestamp(6, isCompleted ? now : null);
            ps.setInt(7, coursId);
            ps.setInt(8, quizScore);
            ps.setInt(9, quizScore);
            ps.setInt(10, quizScore);
            ps.setTimestamp(11, now);
            int rows = ps.executeUpdate();
            System.out.println(">>> INSERT chapter_progress: " + rows + " ligne(s) - Score: " + quizScore + "% - Complété: " + isCompleted);
        } catch (SQLException e) {
            System.err.println(">>> ERREUR markChapterCompleted: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isChapterCompleted(int userId, int chapitreId) {
        String sql = "SELECT id FROM chapter_progress "
            + "WHERE user_id = ? AND chapitre_id = ? AND is_completed = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean isChapterStarted(int userId, int chapitreId) {
        String sql = "SELECT id FROM chapter_progress WHERE user_id = ? AND chapitre_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public int getCourseProgress(int userId, int coursId) {
        int total = 0;
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT COUNT(*) FROM chapitre WHERE cours_id = ?")) {
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }

        if (total == 0) {
            return 0;
        }

        int completed = 0;
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT COUNT(DISTINCT cp.chapitre_id) "
                + "FROM chapter_progress cp "
                + "JOIN chapitre ch ON ch.id = cp.chapitre_id "
                + "WHERE cp.user_id = ? "
                + "AND cp.is_completed = 1 "
                + "AND ch.cours_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                completed = rs.getInt(1);
            }
        } catch (SQLException e) {
            return 0;
        }

        return (int) Math.round((completed * 100.0) / total);
    }

    public List<Integer> getCompletedChapitreIds(int userId, int coursId) {
        return getChapitreIdsForCourse(userId, coursId, true);
    }

    public List<Integer> getStartedChapitreIds(int userId, int coursId) {
        return getChapitreIdsForCourse(userId, coursId, false);
    }

    private List<Integer> getChapitreIdsForCourse(int userId, int coursId, boolean completedOnly) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT DISTINCT cp.chapitre_id "
            + "FROM chapter_progress cp "
            + "JOIN chapitre ch ON ch.id = cp.chapitre_id "
            + "WHERE cp.user_id = ? "
            + "AND ch.cours_id = ?"
            + (completedOnly ? " AND cp.is_completed = 1" : "");
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, coursId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("chapitre_id"));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return ids;
    }

    public int getQuizScore(int userId, int chapitreId) {
        String sql = "SELECT quiz_score FROM chapter_progress WHERE user_id = ? AND chapitre_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("quiz_score");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return 0;
    }

    // ── Recommandations ───────────────────────────────────────────────────────
    /**
     * Retourne le prochain chapitre non complété d'un cours (ordre croissant).
     * Utilisé dans TodoController pour les recommandations personnalisées.
     */
    public tn.esprit.entities.Chapitre getNextChapitre(int userId, int coursId) {
        String sql = "SELECT c.* FROM chapitre c "
            + "WHERE c.cours_id = ? "
            + "AND c.id NOT IN (SELECT chapitre_id FROM chapter_progress WHERE user_id = ?) "
            + "ORDER BY c.ordre ASC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, coursId); ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new tn.esprit.entities.Chapitre(
                    rs.getInt("id"), rs.getString("titre"), rs.getString("contenu"),
                    rs.getInt("ordre"), rs.getString("ressources"), rs.getInt("cours_id"),
                    rs.getString("ressource_type"), rs.getString("ressource_fichier"));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return null;
    }

    // ── Stats globales ────────────────────────────────────────────────────────
    public int getTotalCompletedChapitres(int userId) {
        try (PreparedStatement ps = connection.prepareStatement(
            "SELECT COUNT(*) FROM chapter_progress WHERE user_id = ? AND is_completed = 1")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return 0;
    }

    // ── GAMIFICATION ──────────────────────────────────────────────────────────

    /**
     * Calcule les points totaux de l'étudiant.
     * Règle : 10 pts par chapitre complété + 50 pts bonus par cours terminé à 100%
     */
    public int getTotalPoints(int userId, java.util.List<tn.esprit.entities.Cours> allCours) {
        int points = getTotalCompletedChapitres(userId) * 10;
        for (tn.esprit.entities.Cours cours : allCours) {
            if (getCourseProgress(userId, cours.getId()) >= 100) points += 50;
        }
        return points;
    }

    /**
     * Streak : nombre de jours consécutifs où l'étudiant a complété au moins un chapitre.
     * Compte à rebours depuis aujourd'hui.
     */
    public int getStreak(int userId) {
        String sql = "SELECT DATE(completed_at) as day FROM chapter_progress "
            + "WHERE user_id = ? AND completed_at IS NOT NULL "
            + "GROUP BY DATE(completed_at) ORDER BY day DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            int streak = 0;
            java.time.LocalDate expected = java.time.LocalDate.now();
            while (rs.next()) {
                java.time.LocalDate day = rs.getDate("day").toLocalDate();
                if (day.equals(expected) || day.equals(expected.minusDays(1))) {
                    streak++;
                    expected = day.minusDays(1);
                } else break;
            }
            return streak;
        } catch (SQLException e) { return 0; }
    }

    /** Badges débloqués selon les accomplissements */
    public java.util.List<String[]> getBadges(int userId, java.util.List<tn.esprit.entities.Cours> allCours) {
        java.util.List<String[]> badges = new java.util.ArrayList<>();
        int totalChap = getTotalCompletedChapitres(userId);
        int streak    = getStreak(userId);
        long coursTermines = allCours.stream()
            .filter(c -> getCourseProgress(userId, c.getId()) >= 100).count();

        if (totalChap >= 1)   badges.add(new String[]{"⭐", "Premier pas",    "1er chapitre complété"});
        if (totalChap >= 5)   badges.add(new String[]{"📚", "Lecteur",         "5 chapitres complétés"});
        if (totalChap >= 10)  badges.add(new String[]{"🎓", "Étudiant",        "10 chapitres complétés"});
        if (totalChap >= 20)  badges.add(new String[]{"🏅", "Expert",          "20 chapitres complétés"});
        if (coursTermines >= 1) badges.add(new String[]{"🏆", "Diplômé",       "1 cours terminé"});
        if (coursTermines >= 3) badges.add(new String[]{"👑", "Champion",      "3 cours terminés"});
        if (streak >= 3)      badges.add(new String[]{"🔥", "En feu",          streak + " jours consécutifs"});
        if (streak >= 7)      badges.add(new String[]{"💎", "Invincible",      "7 jours consécutifs"});
        return badges;
    }
}
