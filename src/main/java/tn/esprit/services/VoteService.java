package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;

public class VoteService {

    private Connection connection;

    public VoteService() {
        connection = MyConnection.getInstance().getConnection();
    }

    public Integer getUserRatingForChallenge(int userId, int challengeId) {
        String query = "SELECT valeur FROM vote WHERE user_id = ? AND challenge_id = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, userId);
            pst.setInt(2, challengeId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt("valeur");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean saveOrUpdateVote(int userId, int challengeId, int valeur) {
        Integer existing = getUserRatingForChallenge(userId, challengeId);

        if (existing != null) {
            // Update
            String query = "UPDATE vote SET valeur = ? WHERE user_id = ? AND challenge_id = ?";
            try {
                PreparedStatement pst = connection.prepareStatement(query);
                pst.setInt(1, valeur);
                pst.setInt(2, userId);
                pst.setInt(3, challengeId);
                pst.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            // Insert - Utiliser createdvote_at au lieu de created_at
            String query = "INSERT INTO vote (user_id, challenge_id, valeur, createdvote_at) VALUES (?, ?, ?, NOW())";
            try {
                PreparedStatement pst = connection.prepareStatement(query);
                pst.setInt(1, userId);
                pst.setInt(2, challengeId);
                pst.setInt(3, valeur);
                pst.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    public double getAverageRatingForChallenge(int challengeId) {
        String query = "SELECT AVG(valeur) FROM vote WHERE challenge_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, challengeId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}