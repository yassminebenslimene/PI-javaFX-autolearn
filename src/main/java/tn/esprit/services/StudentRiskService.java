package tn.esprit.services;

import tn.esprit.entities.StudentRisk;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentRiskService {

    private Connection connection;

    public StudentRiskService() {
        connection = MyConnection.getInstance().getConnection();
    }

    public List<StudentRisk> getStudentsAtRisk() {
        List<StudentRisk> students = new ArrayList<>();

        String sql = """
            WITH stats_etudiant AS (
                SELECT
                    u.userId,
                    u.prenom,
                    u.nom,
                    COALESCE(u.niveau, 'Non défini') as niveau,
                    COUNT(uc.id) as total_tentatives,
                    COUNT(CASE WHEN uc.completed = 1 THEN 1 END) as challenges_termines,
                    COALESCE(AVG(CASE WHEN uc.completed = 1 THEN uc.score * 100.0 / NULLIF(uc.total_points, 0) END), 0) as score_moyen,
                    MAX(uc.completed_at) as derniere_activite,
                    COALESCE(DATEDIFF(NOW(), MAX(uc.completed_at)), 999) as jours_inactivite,
                    SUM(CASE WHEN uc.completed = 0 AND uc.current_index > 0 THEN 1 ELSE 0 END) as challenges_abandonnes
                FROM user u
                LEFT JOIN user_challenge uc ON u.userId = uc.user_id
                WHERE u.role = 'etudiant'
                GROUP BY u.userId, u.prenom, u.nom, u.niveau
            )
            SELECT
                userId, prenom, nom, niveau,
                total_tentatives, challenges_termines,
                ROUND(score_moyen, 2) as score_moyen,
                derniere_activite, jours_inactivite, challenges_abandonnes,
                CASE
                    WHEN jours_inactivite > 30 THEN 'CRITIQUE'
                    WHEN jours_inactivite > 14 AND score_moyen < 40 THEN 'ATTENTION'
                    WHEN challenges_abandonnes > 3 THEN 'SURVEILLANCE'
                    ELSE 'NORMAL'
                END as statut_risque,
                ROUND(CASE
                    WHEN total_tentatives > 0 THEN challenges_abandonnes * 100.0 / total_tentatives
                    ELSE 0
                END, 2) as taux_abandon
            FROM stats_etudiant
            WHERE jours_inactivite > 14 OR score_moyen < 40 OR challenges_abandonnes > 2
            ORDER BY
                CASE statut_risque
                    WHEN 'CRITIQUE' THEN 1
                    WHEN 'ATTENTION' THEN 2
                    WHEN 'SURVEILLANCE' THEN 3
                    ELSE 4
                END,
                jours_inactivite DESC, score_moyen ASC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                StudentRisk s = new StudentRisk();
                s.setUserId(rs.getInt("userId"));
                s.setPrenom(rs.getString("prenom"));
                s.setNom(rs.getString("nom"));
                s.setNiveau(rs.getString("niveau"));
                s.setTotalTentatives(rs.getInt("total_tentatives"));
                s.setChallengesTermines(rs.getInt("challenges_termines"));
                s.setScoreMoyen(rs.getDouble("score_moyen"));
                Timestamp ts = rs.getTimestamp("derniere_activite");
                if (ts != null) s.setDerniereActivite(ts.toLocalDateTime());
                s.setJoursInactivite(rs.getLong("jours_inactivite"));
                s.setChallengesAbandonnes(rs.getInt("challenges_abandonnes"));
                s.setStatutRisque(rs.getString("statut_risque"));
                s.setTauxAbandon(rs.getDouble("taux_abandon"));
                students.add(s);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public int getCritiqueCount() {
        String sql = """
            SELECT COUNT(*) as count FROM (
                SELECT u.userId,
                    COALESCE(DATEDIFF(NOW(), MAX(uc.completed_at)), 999) as jours_inactivite
                FROM user u
                LEFT JOIN user_challenge uc ON u.userId = uc.user_id
                WHERE u.role = 'etudiant'
                GROUP BY u.userId
            ) stats WHERE jours_inactivite > 30
        """;
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getAttentionCount() {
        String sql = """
            SELECT COUNT(*) as count FROM (
                SELECT u.userId,
                    COALESCE(DATEDIFF(NOW(), MAX(uc.completed_at)), 999) as jours_inactivite,
                    COALESCE(AVG(CASE WHEN uc.completed = 1 THEN uc.score * 100.0 / NULLIF(uc.total_points, 0) END), 0) as score_moyen
                FROM user u
                LEFT JOIN user_challenge uc ON u.userId = uc.user_id
                WHERE u.role = 'etudiant'
                GROUP BY u.userId
            ) stats WHERE jours_inactivite > 14 AND score_moyen < 40
        """;
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getSurveillanceCount() {
        String sql = """
            SELECT COUNT(*) as count FROM (
                SELECT u.userId,
                    SUM(CASE WHEN uc.completed = 0 AND uc.current_index > 0 THEN 1 ELSE 0 END) as challenges_abandonnes
                FROM user u
                LEFT JOIN user_challenge uc ON u.userId = uc.user_id
                WHERE u.role = 'etudiant'
                GROUP BY u.userId
            ) stats WHERE challenges_abandonnes > 3
        """;
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("count");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
