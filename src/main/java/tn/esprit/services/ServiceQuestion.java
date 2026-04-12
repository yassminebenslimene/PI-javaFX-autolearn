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
 * Service Question — gère toutes les opérations SQL sur la table "question".
 * Implémente IService<Question> pour les 4 opérations CRUD de base.
 */
public class ServiceQuestion implements IService<Question> {

    // Connexion à la base de données (singleton partagé)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── CREATE : Insérer une nouvelle question en BDD ─────────────────────────
    @Override
    public boolean ajouter(Question question) {
        String req = "INSERT INTO question (texte_question, point, updated_at, quiz_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, question.getTexteQuestion());
            statement.setInt(2, question.getPoint());
            // updated_at peut être null (pas obligatoire)
            statement.setTimestamp(3, question.getUpdatedAt() == null ? null : Timestamp.valueOf(question.getUpdatedAt()));
            statement.setInt(4, question.getQuizId()); // lien vers le quiz parent
            int rows = statement.executeUpdate();
            return rows > 0; // true = insertion réussie
        } catch (SQLException e) {
            System.err.println("Erreur ajout question : " + e.getMessage());
            return false;
        }
    }

    // ── DELETE : Supprimer une question par son id ────────────────────────────
    @Override
    public boolean supprimer(Question question) {
        String req = "DELETE FROM question WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, question.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression question : " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE : Modifier une question existante ──────────────────────────────
    @Override
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

    // ── READ ALL (console) : Affiche toutes les questions dans la console ─────
    @Override
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

    // ── READ ONE (console) : Affiche une question par son id ─────────────────
    @Override
    public void getOneById(int id) {
        String req = "SELECT * FROM question WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuestion(rs));
                } else {
                    System.out.println("Aucune question trouvée avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche question : " + e.getMessage());
        }
    }

    // ── READ ALL (liste) : Retourne toutes les questions sous forme de liste ──
    // Utilisé par les controllers JavaFX
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

    // ── READ BY QUIZ : Retourne les questions d'un quiz spécifique ────────────
    // Utilisé pour afficher les questions quand on clique "Sélectionner" sur un quiz
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

    // ── Méthode privée : convertit une ligne SQL en objet Question ────────────
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
