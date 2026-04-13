package tn.esprit.services;

import tn.esprit.entities.Question;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Service Question ÔÇö g├¿re toutes les op├®rations SQL sur la table "question".
 * Impl├®mente IService<Question> pour les 4 op├®rations CRUD de base.
 */
public class ServiceQuestion {

    // Connexion ├á la base de donn├®es (singleton partag├®)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ÔöÇÔöÇ CREATE : Ins├®rer une nouvelle question en BDD ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public boolean ajouter(Question question) {
        String req = "INSERT INTO question (texte_question, point, updated_at, quiz_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            // updated_at peut ├¬tre null (pas obligatoire)
            statement.setTimestamp(3, question.getUpdatedAt() == null ? null : Timestamp.valueOf(question.getUpdatedAt()));
            statement.setInt(4, question.getQuizId()); // lien vers le quiz parent
            int rows = statement.executeUpdate();
            return rows > 0; // true = insertion r├®ussie
        } catch (SQLException e) {
            System.err.println("Erreur ajout question : " + e.getMessage());
            return false;
        }
    }

    // ÔöÇÔöÇ DELETE : Supprimer une question et toutes ses options en cascade ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public boolean supprimer(Question question) {
        try {
            // ├ëtape 1 : supprimer toutes les options de cette question
            String delOptions = "DELETE FROM `option` WHERE question_id = ?";
            try (PreparedStatement st = connection.prepareStatement(delOptions)) {
                st.setInt(1, question.getId());
                st.executeUpdate();
            }
            // ├ëtape 2 : supprimer la question elle-m├¬me
            String delQuestion = "DELETE FROM question WHERE id = ?";
            try (PreparedStatement st = connection.prepareStatement(delQuestion)) {
                st.setInt(1, question.getId());
                return st.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur suppression question : " + e.getMessage());
            return false;
        }
    }

    // ÔöÇÔöÇ UPDATE : Modifier une question existante ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public boolean modifier(Question question) {
        String req = "UPDATE question SET texte_question = ?, point = ?, updated_at = ?, quiz_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            statement.setTimestamp(3, question.getUpdatedAt() == null ? null : Timestamp.valueOf(question.getUpdatedAt()));
            statement.setInt(4, question.getQuizId());
            statement.setInt(5, question.getId()); // condition WHERE id = ?
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification question : " + e.getMessage());
            return false;
        }
    }

    // ÔöÇÔöÇ READ ALL (console) : Affiche toutes les questions dans la console ÔöÇÔöÇÔöÇÔöÇÔöÇ
    public void getAll() {
        String req = "SELECT * FROM question";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                System.out.println(mapQuestion(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage questions : " + e.getMessage());
        }
    }

    // ÔöÇÔöÇ READ ONE (console) : Affiche une question par son id ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public void getOneById(int id) {
        String req = "SELECT * FROM question WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuestion(rs));
                } else {
                    System.out.println("Aucune question trouv├®e avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche question : " + e.getMessage());
        }
    }

    // ÔöÇÔöÇ READ ALL (liste) : Retourne toutes les questions sous forme de liste ÔöÇÔöÇ
    // Utilis├® par les controllers JavaFX
    public java.util.List<Question> afficher() {
        java.util.List<Question> questions = new java.util.ArrayList<>();
        String req = "SELECT * FROM question";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) questions.add(mapQuestion(rs));
        } catch (SQLException e) {
            System.err.println("Erreur affichage questions : " + e.getMessage());
        }
        return questions;
    }

    // ÔöÇÔöÇ READ BY QUIZ : Retourne les questions d'un quiz sp├®cifique ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    // Utilis├® pour afficher les questions quand on clique "S├®lectionner" sur un quiz
    public java.util.List<Question> findByQuizId(int quizId) {
        java.util.List<Question> questions = new java.util.ArrayList<>();
        String req = "SELECT * FROM question WHERE quiz_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, quizId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) questions.add(mapQuestion(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByQuizId : " + e.getMessage());
        }
        return questions;
    }

    // ÔöÇÔöÇ M├®thode priv├®e : convertit une ligne SQL en objet Question ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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
