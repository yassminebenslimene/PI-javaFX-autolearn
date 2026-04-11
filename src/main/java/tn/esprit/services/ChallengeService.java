package tn.esprit.services;

import tn.esprit.entities.Challenge;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ChallengeService {

    private Connection connection;

    public ChallengeService() {
        connection = MyConnection.getInstance().getConnection();
    }

    public void add(Challenge challenge) {
        String query = "INSERT INTO challenge (titre, description, date_debut, date_fin, niveau, duree, created_by) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, challenge.getTitre());
            pst.setString(2, challenge.getDescription());
            pst.setDate(3, Date.valueOf(challenge.getDateDebut()));
            pst.setDate(4, Date.valueOf(challenge.getDateFin()));
            pst.setString(5, challenge.getNiveau());
            pst.setInt(6, challenge.getDuree());
            pst.setInt(7, challenge.getCreatedBy());
            pst.executeUpdate();

            // Récupérer l'ID généré
            ResultSet rs = pst.getGeneratedKeys();
            if (rs.next()) {
                int challengeId = rs.getInt(1);
                // Ajouter les relations avec les exercices
                for (int exerciceId : challenge.getExerciceIds()) {
                    addChallengeExercice(challengeId, exerciceId);
                }
                // Ajouter les relations avec les quiz
                for (int quizId : challenge.getQuizIds()) {
                    addChallengeQuiz(challengeId, quizId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Challenge challenge) {
        String query = "UPDATE challenge SET titre=?, description=?, date_debut=?, date_fin=?, niveau=?, duree=?, created_by=? WHERE id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setString(1, challenge.getTitre());
            pst.setString(2, challenge.getDescription());
            pst.setDate(3, Date.valueOf(challenge.getDateDebut()));
            pst.setDate(4, Date.valueOf(challenge.getDateFin()));
            pst.setString(5, challenge.getNiveau());
            pst.setInt(6, challenge.getDuree());
            pst.setInt(7, challenge.getCreatedBy());
            pst.setInt(8, challenge.getId());
            pst.executeUpdate();

            // Supprimer les anciennes relations
            deleteChallengeExercices(challenge.getId());
            deleteChallengeQuizzes(challenge.getId());

            // Ajouter les nouvelles relations
            for (int exerciceId : challenge.getExerciceIds()) {
                addChallengeExercice(challenge.getId(), exerciceId);
            }
            for (int quizId : challenge.getQuizIds()) {
                addChallengeQuiz(challenge.getId(), quizId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM challenge WHERE id=?";
        try {
            // Supprimer d'abord les relations
            deleteChallengeExercices(id);
            deleteChallengeQuizzes(id);

            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Challenge> getAll() {
        List<Challenge> challenges = new ArrayList<>();
        String query = "SELECT * FROM challenge";
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Challenge c = new Challenge();
                c.setId(rs.getInt("id"));
                c.setTitre(rs.getString("titre"));
                c.setDescription(rs.getString("description"));
                c.setDateDebut(rs.getDate("date_debut").toLocalDate());
                c.setDateFin(rs.getDate("date_fin").toLocalDate());
                c.setNiveau(rs.getString("niveau"));
                c.setDuree(rs.getInt("duree"));
                c.setCreatedBy(rs.getInt("created_by"));

                // Charger les exercices associés
                c.setExerciceIds(getChallengeExercices(c.getId()));

                // Temporairement, ne pas charger les quiz
                // c.setQuizIds(getChallengeQuizzes(c.getId()));
                c.setQuizIds(new ArrayList<>()); // Liste vide

                challenges.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return challenges;
    }

    // Gestion des relations
    private void addChallengeExercice(int challengeId, int exerciceId) {
        String query = "INSERT INTO challenge_exercice (challenge_id, exercice_id) VALUES (?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, challengeId);
            pst.setInt(2, exerciceId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addChallengeQuiz(int challengeId, int quizId) {
        String query = "INSERT INTO challenge_quiz (challenge_id, quiz_id) VALUES (?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, challengeId);
            pst.setInt(2, quizId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteChallengeExercices(int challengeId) {
        String query = "DELETE FROM challenge_exercice WHERE challenge_id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, challengeId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteChallengeQuizzes(int challengeId) {
        String query = "DELETE FROM challenge_quiz WHERE challenge_id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, challengeId);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Integer> getChallengeExercices(int challengeId) {
        List<Integer> exerciceIds = new ArrayList<>();
        String query = "SELECT exercice_id FROM challenge_exercice WHERE challenge_id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, challengeId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                exerciceIds.add(rs.getInt("exercice_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exerciceIds;
    }

    private List<Integer> getChallengeQuizzes(int challengeId) {
        List<Integer> quizIds = new ArrayList<>();
        String query = "SELECT quiz_id FROM challenge_quiz WHERE challenge_id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, challengeId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                quizIds.add(rs.getInt("quiz_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return quizIds;
    }
}