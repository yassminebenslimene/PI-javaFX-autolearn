package tn.esprit.services;

import tn.esprit.entities.Question;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class ServiceQuestion implements IService<Question> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Question question) {
        String req = "INSERT INTO question (texte_question, point, updated_at, quiz_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            statement.setTimestamp(3, question.getUpdatedAt() == null ? null : Timestamp.valueOf(question.getUpdatedAt()));
            statement.setInt(4, question.getQuizId());
            statement.executeUpdate();
            System.out.println("Question ajoutee avec succes.");
        } catch (SQLException e) {
            System.err.println("Erreur ajout question : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Question question) {
        String req = "DELETE FROM question WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, question.getId());
            int rows = statement.executeUpdate();
            System.out.println(rows > 0 ? "Question supprimee avec succes." : "Aucune question trouvee pour suppression.");
        } catch (SQLException e) {
            System.err.println("Erreur suppression question : " + e.getMessage());
        }
    }

    @Override
    public void modifier(Question question) {
        String req = "UPDATE question SET texte_question = ?, point = ?, updated_at = ?, quiz_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            statement.setTimestamp(3, question.getUpdatedAt() == null ? null : Timestamp.valueOf(question.getUpdatedAt()));
            statement.setInt(4, question.getQuizId());
            statement.setInt(5, question.getId());
            int rows = statement.executeUpdate();
            System.out.println(rows > 0 ? "Question modifiee avec succes." : "Aucune question trouvee pour modification.");
        } catch (SQLException e) {
            System.err.println("Erreur modification question : " + e.getMessage());
        }
    }

    @Override
    public void getAll() {
        String req = "SELECT * FROM question";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                Question question = mapQuestion(rs);
                System.out.println(question);
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage questions : " + e.getMessage());
        }
    }

    @Override
    public void getOneById(int id) {
        String req = "SELECT * FROM question WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuestion(rs));
                } else {
                    System.out.println("Aucune question trouvee avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche question : " + e.getMessage());
        }
    }

    private Question mapQuestion(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new Question(
                rs.getInt("id"),
                rs.getString("texte_question"),
                rs.getInt("point"),
                updatedAt == null ? null : updatedAt.toLocalDateTime(),
                rs.getInt("quiz_id")
        );
    }
}
