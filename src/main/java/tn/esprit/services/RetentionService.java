package tn.esprit.services;

import tn.esprit.entities.RetentionData;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RetentionService {

    private Connection connection;

    public RetentionService() {
        connection = MyConnection.getInstance().getConnection();
    }

    /**
     * Requête SQL complexe avec :
     * - CTE (WITH) pour first_challenge et activite_mensuelle
     * - Fonctions de fenêtrage (MAX OVER)
     * - Agrégations multiples
     * - Calcul de taux de rétention
     */
    public List<RetentionData> getRetentionData() {
        List<RetentionData> retentionData = new ArrayList<>();

        String sql = """
            WITH first_challenge AS (
                SELECT 
                    user_id,
                    MIN(DATE(completed_at)) as premiere_activite,
                    DATE_FORMAT(MIN(completed_at), '%Y-%m') as cohorte
                FROM user_challenge
                WHERE completed = 1
                GROUP BY user_id
            ),
            activite_mensuelle AS (
                SELECT 
                    fc.cohorte,
                    uc.user_id,
                    DATE_FORMAT(uc.completed_at, '%Y-%m') as mois_activite,
                    TIMESTAMPDIFF(MONTH, fc.premiere_activite, uc.completed_at) as mois_relatif
                FROM user_challenge uc
                JOIN first_challenge fc ON uc.user_id = fc.user_id
                WHERE uc.completed = 1
            )
            SELECT 
                cohorte,
                mois_relatif,
                COUNT(DISTINCT user_id) as nb_etudiants,
                ROUND(COUNT(DISTINCT user_id) * 100.0 / 
                    MAX(COUNT(DISTINCT user_id)) OVER (PARTITION BY cohorte), 2) as taux_retention
            FROM activite_mensuelle
            GROUP BY cohorte, mois_relatif
            ORDER BY cohorte, mois_relatif
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                RetentionData data = new RetentionData();
                data.setCohorte(rs.getString("cohorte"));
                data.setMoisRelatif(rs.getInt("mois_relatif"));
                data.setNbEtudiants(rs.getInt("nb_etudiants"));
                data.setTauxRetention(rs.getDouble("taux_retention"));
                retentionData.add(data);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return retentionData;
    }

    public List<String> getCohortes() {
        List<String> cohortes = new ArrayList<>();
        String sql = """
            WITH first_challenge AS (
                SELECT 
                    user_id,
                    DATE_FORMAT(MIN(completed_at), '%Y-%m') as cohorte
                FROM user_challenge
                WHERE completed = 1
                GROUP BY user_id
            )
            SELECT DISTINCT cohorte
            FROM first_challenge
            ORDER BY cohorte DESC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cohortes.add(rs.getString("cohorte"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cohortes;
    }

    public RetentionSummary getRetentionSummary() {
        RetentionSummary summary = new RetentionSummary();
        String sql = """
            WITH first_challenge AS (
                SELECT 
                    user_id,
                    MIN(DATE(completed_at)) as premiere_activite,
                    DATE_FORMAT(MIN(completed_at), '%Y-%m') as cohorte
                FROM user_challenge
                WHERE completed = 1
                GROUP BY user_id
            ),
            retention_calc AS (
                SELECT 
                    fc.cohorte,
                    fc.user_id,
                    TIMESTAMPDIFF(MONTH, fc.premiere_activite, NOW()) as mois_actuel,
                    CASE 
                        WHEN TIMESTAMPDIFF(MONTH, fc.premiere_activite, NOW()) >= 1 
                        AND EXISTS (
                            SELECT 1 FROM user_challenge uc 
                            WHERE uc.user_id = fc.user_id 
                            AND uc.completed = 1 
                            AND DATE(uc.completed_at) >= DATE_ADD(fc.premiere_activite, INTERVAL 1 MONTH)
                        ) THEN 1 ELSE 0 
                    END as retention_1mois,
                    CASE 
                        WHEN TIMESTAMPDIFF(MONTH, fc.premiere_activite, NOW()) >= 3 
                        AND EXISTS (
                            SELECT 1 FROM user_challenge uc 
                            WHERE uc.user_id = fc.user_id 
                            AND uc.completed = 1 
                            AND DATE(uc.completed_at) >= DATE_ADD(fc.premiere_activite, INTERVAL 3 MONTH)
                        ) THEN 1 ELSE 0 
                    END as retention_3mois,
                    CASE 
                        WHEN TIMESTAMPDIFF(MONTH, fc.premiere_activite, NOW()) >= 6 
                        AND EXISTS (
                            SELECT 1 FROM user_challenge uc 
                            WHERE uc.user_id = fc.user_id 
                            AND uc.completed = 1 
                            AND DATE(uc.completed_at) >= DATE_ADD(fc.premiere_activite, INTERVAL 6 MONTH)
                        ) THEN 1 ELSE 0 
                    END as retention_6mois
                FROM first_challenge fc
            )
            SELECT 
                COUNT(DISTINCT user_id) as total_etudiants,
                ROUND(SUM(retention_1mois) * 100.0 / COUNT(*), 2) as retention_1mois,
                ROUND(SUM(retention_3mois) * 100.0 / COUNT(*), 2) as retention_3mois,
                ROUND(SUM(retention_6mois) * 100.0 / COUNT(*), 2) as retention_6mois
            FROM retention_calc
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                summary.setTotalEtudiants(rs.getInt("total_etudiants"));
                summary.setRetention1Mois(rs.getDouble("retention_1mois"));
                summary.setRetention3Mois(rs.getDouble("retention_3mois"));
                summary.setRetention6Mois(rs.getDouble("retention_6mois"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    public static class RetentionSummary {
        private int totalEtudiants;
        private double retention1Mois;
        private double retention3Mois;
        private double retention6Mois;

        public int getTotalEtudiants() { return totalEtudiants; }
        public void setTotalEtudiants(int totalEtudiants) { this.totalEtudiants = totalEtudiants; }

        public double getRetention1Mois() { return retention1Mois; }
        public void setRetention1Mois(double retention1Mois) { this.retention1Mois = retention1Mois; }

        public double getRetention3Mois() { return retention3Mois; }
        public void setRetention3Mois(double retention3Mois) { this.retention3Mois = retention3Mois; }

        public double getRetention6Mois() { return retention6Mois; }
        public void setRetention6Mois(double retention6Mois) { this.retention6Mois = retention6Mois; }
    }
}