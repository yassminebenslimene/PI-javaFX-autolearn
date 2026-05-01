package tn.esprit.services;

import tn.esprit.entities.LeaderboardEntry;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardService {

    private Connection connection;

    public LeaderboardService() {
        connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Requête SQL adaptée à la structure de la table user (userId = clé primaire)
     */
    public List<LeaderboardEntry> getLeaderboard(String niveauFilter) {
        List<LeaderboardEntry> entries = new ArrayList<>();

        String sql = """
            SELECT 
                u.userId,
                u.prenom,
                u.nom,
                u.niveau,
                COALESCE(SUM(uc.score), 0) as total_points,
                COUNT(DISTINCT CASE WHEN uc.completed = 1 THEN uc.challenge_id END) as challenges_completes,
                ROUND(COALESCE(AVG(uc.score * 100.0 / NULLIF(uc.total_points, 0)), 0), 2) as moyenne
            FROM user u
            LEFT JOIN user_challenge uc ON u.userId = uc.user_id AND uc.completed = 1
            WHERE u.role = 'etudiant'
            """ + (niveauFilter != null && !niveauFilter.equals("Tous") ? " AND u.niveau = '" + niveauFilter + "'" : "") + """
            GROUP BY u.userId, u.prenom, u.nom, u.niveau
            ORDER BY total_points DESC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int rank = 1;
            while (rs.next()) {
                LeaderboardEntry entry = new LeaderboardEntry();
                entry.setUserId(rs.getInt("userId"));
                entry.setPrenom(rs.getString("prenom"));
                entry.setNom(rs.getString("nom"));
                entry.setNiveau(rs.getString("niveau"));
                entry.setRang(rank++);
                entry.setChallengesCompletes(rs.getInt("challenges_completes"));
                entry.setTotalPoints(rs.getInt("total_points"));
                entry.setMoyenne(rs.getDouble("moyenne"));

                // Calcul des médailles
                if (entry.getRang() == 1) entry.setMedailles("🥇 OR");
                else if (entry.getRang() == 2) entry.setMedailles("🥈 ARGENT");
                else if (entry.getRang() == 3) entry.setMedailles("🥉 BRONZE");
                else entry.setMedailles("");

                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    /**
     * Statistiques globales pour le leaderboard
     */
    public String getGlobalStats() {
        String sql = """
            SELECT 
                COUNT(DISTINCT u.userId) as total_etudiants,
                COUNT(DISTINCT CASE WHEN uc.completed = 1 THEN u.userId END) as etudiants_actifs,
                COUNT(CASE WHEN uc.completed = 1 THEN 1 END) as total_challenges_termines,
                COALESCE(SUM(uc.score), 0) as points_totaux
            FROM user u
            LEFT JOIN user_challenge uc ON u.userId = uc.user_id
            WHERE u.role = 'etudiant'
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int totalEtudiants = rs.getInt("total_etudiants");
                int etudiantsActifs = rs.getInt("etudiants_actifs");
                int challengesTermines = rs.getInt("total_challenges_termines");
                int pointsTotaux = rs.getInt("points_totaux");

                return String.format("📊 %d étudiants | 🎯 %d actifs | 🏆 %d challenges | ⭐ %d points",
                        totalEtudiants, etudiantsActifs, challengesTermines, pointsTotaux);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return "Aucune donnée disponible";
    }
}