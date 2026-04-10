package tn.esprit.services;

import tn.esprit.entities.Quiz;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class ServiceQuiz implements IService<Quiz> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Quiz quiz) {
        String req = "INSERT INTO quiz (titre, description, etat, duree_max_minutes, seuil_reussite, max_tentatives, image_name, image_size, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());
            statement.setObject(5, quiz.getSeuilReussite());
            statement.setObject(6, quiz.getMaxTentatives());
            statement.setString(7, quiz.getImageName());
            statement.setObject(8, quiz.getImageSize());
            statement.setTimestamp(9, quiz.getUpdatedAt() == null ? null : Timestamp.valueOf(quiz.getUpdatedAt()));
            statement.executeUpdate();
            System.out.println("Quiz ajoute avec succes.");
        } catch (SQLException e) {
            System.err.println("Erreur ajout quiz : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Quiz quiz) {
        String req = "DELETE FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, quiz.getId());
            int rows = statement.executeUpdate();
            System.out.println(rows > 0 ? "Quiz supprime avec succes." : "Aucun quiz trouve pour suppression.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression quiz : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Quiz quiz) {
        String req = "UPDATE quiz SET titre = ?, description = ?, etat = ?, duree_max_minutes = ?, seuil_reussite = ?, max_tentatives = ?, image_name = ?, image_size = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());
            statement.setObject(5, quiz.getSeuilReussite());
            statement.setObject(6, quiz.getMaxTentatives());
            statement.setString(7, quiz.getImageName());
            statement.setObject(8, quiz.getImageSize());
            statement.setTimestamp(9, quiz.getUpdatedAt() == null ? null : Timestamp.valueOf(quiz.getUpdatedAt()));
            statement.setInt(10, quiz.getId());
            int rows = statement.executeUpdate();
            System.out.println(rows > 0 ? "Quiz modifie avec succes." : "Aucun quiz trouve pour modification.");
        } catch (SQLException e) {
            System.err.println("Erreur modification quiz : " + e.getMessage());
        }
    }

    @Override
    public void getAll() {
        String req = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                Quiz quiz = mapQuiz(rs);
                System.out.println(quiz);
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage quiz : " + e.getMessage());
        }
    }

    @Override
    public void getOneById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuiz(rs));
                } else {
                    System.out.println("Aucun quiz trouve avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche quiz : " + e.getMessage());
        }
    }

    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new Quiz(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("etat"),
                (Integer) rs.getObject("duree_max_minutes"),
                (Integer) rs.getObject("seuil_reussite"),
                (Integer) rs.getObject("max_tentatives"),
                rs.getString("image_name"),
                (Integer) rs.getObject("image_size"),
                updatedAt == null ? null : updatedAt.toLocalDateTime()
        );
    }
}
