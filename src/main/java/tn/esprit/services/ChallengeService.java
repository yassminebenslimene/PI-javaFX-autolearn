package tn.esprit.services;

import tn.esprit.entities.Challenge;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChallengeService {

    private Connection connection;

    public ChallengeService() {
        connection = MyConnection.getInstance().getConnection();
    }

    // Nom de colonne détecté dynamiquement
    private String colCreatedBy = "created_by";

    /** Détecte le vrai nom de colonne created_by */
    private void detectColumnNames() {
        try {
            java.sql.ResultSet rs = connection.getMetaData().getColumns(null, null, "challenge", null);
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME").toLowerCase();
                if (col.equals("createdby") || col.equals("created_by")) colCreatedBy = rs.getString("COLUMN_NAME");
            }
        } catch (java.sql.SQLException e) {
            // Garder la valeur par défaut
        }
    }

    public void add(Challenge challenge) {
        String query = "INSERT INTO challenge (titre, description, niveau, duree, " + colCreatedBy + ") VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, challenge.getTitre());
            pst.setString(2, challenge.getDescription());
            pst.setString(3, challenge.getNiveau());
            pst.setInt(4, challenge.getDuree());
            pst.setInt(5, challenge.getCreatedBy());
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
        String query = "UPDATE challenge SET titre=?, description=?, niveau=?, duree=?, " + colCreatedBy + "=? WHERE id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setString(1, challenge.getTitre());
            pst.setString(2, challenge.getDescription());
            pst.setString(3, challenge.getNiveau());
            pst.setInt(4, challenge.getDuree());
            pst.setInt(5, challenge.getCreatedBy());
            pst.setInt(6, challenge.getId());
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
                Challenge c = mapRow(rs);
                challenges.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return challenges;
    }

    public Challenge getById(int id) {
        String query = "SELECT * FROM challenge WHERE id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Challenge mapRow(ResultSet rs) throws SQLException {
        Challenge c = new Challenge();
        c.setId(rs.getInt("id"));
        c.setTitre(rs.getString("titre"));
        c.setDescription(rs.getString("description"));
        c.setNiveau(rs.getString("niveau"));
        c.setDuree(rs.getInt("duree"));
        try { c.setCreatedBy(rs.getInt(colCreatedBy)); } catch (java.sql.SQLException ignored) {}

        // Charger les exercices liés
        try {
            c.setExerciceIds(getChallengeExercices(c.getId()));
        } catch (Exception ignored) {
            c.setExerciceIds(new ArrayList<>());
        }

        // CHARGER LES QUIZ LIÉS (au lieu de new ArrayList<>())
        try {
            c.setQuizIds(getChallengeQuizzes(c.getId()));
            System.out.println("Challenge " + c.getId() + " a " + c.getQuizIds().size() + " quiz associés");
        } catch (Exception ignored) {
            c.setQuizIds(new ArrayList<>());
        }

        return c;
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