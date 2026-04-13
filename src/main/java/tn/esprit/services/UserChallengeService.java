package tn.esprit.services;

import tn.esprit.entities.UserChallenge;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class UserChallengeService {

    private Connection connection;

    public UserChallengeService() {
        connection = MyConnection.getInstance().getConnection();
    }

    public UserChallenge findByUserAndChallenge(int userId, int challengeId) {
        String query = "SELECT * FROM user_challenge WHERE user_id = ? AND challenge_id = ?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, userId);
            pst.setInt(2, challengeId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                UserChallenge uc = new UserChallenge();
                uc.setId(rs.getInt("id"));
                uc.setUserId(rs.getInt("user_id"));
                uc.setChallengeId(rs.getInt("challenge_id"));
                uc.setCurrentIndex(rs.getInt("current_index"));
                uc.setScore(rs.getInt("score"));
                uc.setTotalPoints(rs.getInt("total_points"));
                uc.setCompleted(rs.getBoolean("completed"));
                if (rs.getTimestamp("completed_at") != null) {
                    uc.setCompletedAt(rs.getTimestamp("completed_at").toLocalDateTime());
                }

                // Récupérer les réponses (stockées en JSON)
                String answersJson = rs.getString("answers");
                if (answersJson != null && !answersJson.isEmpty()) {
                    // Parser JSON simple
                    Map<Integer, String> answers = new HashMap<>();
                    // Implémentez le parsing JSON selon votre format
                    uc.setAnswersMap(answers);
                }
                return uc;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(UserChallenge userChallenge) {
        UserChallenge existing = findByUserAndChallenge(userChallenge.getUserId(), userChallenge.getChallengeId());

        if (existing != null) {
            // Update
            String query = "UPDATE user_challenge SET current_index=?, answers=?, score=?, total_points=?, completed=?, completed_at=? WHERE id=?";
            try {
                PreparedStatement pst = connection.prepareStatement(query);
                pst.setInt(1, userChallenge.getCurrentIndex());
                pst.setString(2, convertAnswersToJson(userChallenge.getAnswersMap()));
                pst.setInt(3, userChallenge.getScore());
                pst.setInt(4, userChallenge.getTotalPoints());
                pst.setBoolean(5, userChallenge.isCompleted());
                pst.setTimestamp(6, userChallenge.getCompletedAt() != null ? Timestamp.valueOf(userChallenge.getCompletedAt()) : null);
                pst.setInt(7, existing.getId());
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Insert
            String query = "INSERT INTO user_challenge (user_id, challenge_id, current_index, answers, score, total_points, completed, completed_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try {
                PreparedStatement pst = connection.prepareStatement(query);
                pst.setInt(1, userChallenge.getUserId());
                pst.setInt(2, userChallenge.getChallengeId());
                pst.setInt(3, userChallenge.getCurrentIndex());
                pst.setString(4, convertAnswersToJson(userChallenge.getAnswersMap()));
                pst.setInt(5, userChallenge.getScore());
                pst.setInt(6, userChallenge.getTotalPoints());
                pst.setBoolean(7, userChallenge.isCompleted());
                pst.setTimestamp(8, userChallenge.getCompletedAt() != null ? Timestamp.valueOf(userChallenge.getCompletedAt()) : null);
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String convertAnswersToJson(Map<Integer, String> answers) {
        if (answers == null || answers.isEmpty()) return "{}";
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":\"").append(escapeJson(entry.getValue())).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
