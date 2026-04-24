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

public class CourseProgressService {

    private final Connection connection;

    public CourseProgressService() {
        this.connection = MyConnection.getInstance().getConnection();
        createTableIfNotExists();
    }

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

    public void markChapterViewed(int userId, int chapitreId, int coursId) {
        String sql = "INSERT INTO chapter_progress "
            + "(user_id, chapitre_id, cours_id, quiz_score, completed_at) "
            + "VALUES (?, ?, ?, NULL, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "cours_id = IFNULL(cours_id, ?), "
            + "completed_at = IFNULL(completed_at, ?)";        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            ps.setInt(3, coursId);
            ps.setTimestamp(4, now);
            ps.setInt(5, coursId);
            ps.setTimestamp(6, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(">>> ERREUR markChapterViewed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void markChapterCompleted(int userId, int chapitreId, int coursId, int quizScore) {
        String sql = "INSERT INTO chapter_progress "
            + "(user_id, chapitre_id, cours_id, quiz_score, completed_at) "
            + "VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "cours_id = IFNULL(cours_id, ?), "
            + "quiz_score = GREATEST(IFNULL(quiz_score, 0), ?), "
            + "completed_at = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setInt(1, userId);
            ps.setInt(2, chapitreId);
            ps.setInt(3, coursId);
            ps.setInt(4, quizScore);
            ps.setTimestamp(5, now);
            ps.setInt(6, coursId);
            ps.setInt(7, quizScore);
            ps.setTimestamp(8, now);
            int rows = ps.executeUpdate();
            System.out.println(">>> INSERT chapter_progress: " + rows + " ligne(s)");
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
}
