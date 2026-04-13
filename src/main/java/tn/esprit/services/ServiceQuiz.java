package tn.esprit.services;

import tn.esprit.entities.Quiz;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Service Quiz — gère toutes les opérations SQL sur la table "quiz".
 * Implémente IService<Quiz> pour les 4 opérations CRUD de base.
 */
public class ServiceQuiz {

    // Connexion à la base de données (singleton partagé dans toute l'application)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── CREATE : Insérer un nouveau quiz en BDD ───────────────────────────────
    public boolean ajouter(Quiz quiz) {
        String req = "INSERT INTO quiz (titre, description, etat, duree_max_minutes, seuil_reussite, max_tentatives, image_name, image_size, updated_at, chapitre_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            statement.setObject(10, quiz.getChapitreId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajout quiz : " + e.getMessage());
            return false;
        }
    }

    // ── DELETE : Supprimer un quiz et toutes ses questions/options en cascade ──
    public boolean supprimer(Quiz quiz) {
        try {
            // Étape 1 : supprimer toutes les options des questions de ce quiz
            String delOptions = "DELETE FROM `option` WHERE question_id IN (SELECT id FROM question WHERE quiz_id = ?)";
            try (PreparedStatement st = connection.prepareStatement(delOptions)) {
                st.setInt(1, quiz.getId());
                st.executeUpdate();
            }
            // Étape 2 : supprimer toutes les questions du quiz
            String delQuestions = "DELETE FROM question WHERE quiz_id = ?";
            try (PreparedStatement st = connection.prepareStatement(delQuestions)) {
                st.setInt(1, quiz.getId());
                st.executeUpdate();
            }
            // Étape 3 : supprimer le quiz lui-même
            String delQuiz = "DELETE FROM quiz WHERE id = ?";
            try (PreparedStatement st = connection.prepareStatement(delQuiz)) {
                st.setInt(1, quiz.getId());
                return st.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur suppression quiz : " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE : Modifier un quiz existant ────────────────────────────────────
    public boolean modifier(Quiz quiz) {
        String req = "UPDATE quiz SET titre = ?, description = ?, etat = ?, duree_max_minutes = ?, seuil_reussite = ?, max_tentatives = ?, image_name = ?, image_size = ?, updated_at = ?, chapitre_id = ? WHERE id = ?";
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
            statement.setObject(10, quiz.getChapitreId());
            statement.setInt(11, quiz.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification quiz : " + e.getMessage());
            return false;
        }
    }

    // ── READ ALL (console) : Affiche tous les quiz dans la console ────────────
    public void getAll() {
        String req = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                System.out.println(mapQuiz(rs)); // affiche chaque quiz
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage quiz : " + e.getMessage());
        }
    }

    // ── READ ONE (console) : Affiche un quiz par son id dans la console ───────
    public void getOneById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuiz(rs));
                } else {
                    System.out.println("Aucun quiz trouvé avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche quiz : " + e.getMessage());
        }
    }

    // ── READ ALL (liste) : Retourne tous les quiz sous forme de liste ─────────
    // Utilisé par les controllers JavaFX pour afficher la liste dans l'interface
    public java.util.List<Quiz> afficher() {
        java.util.List<Quiz> quizzes = new java.util.ArrayList<>();
        String req = "SELECT * FROM quiz";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) quizzes.add(mapQuiz(rs));
        } catch (SQLException e) {
            System.err.println("Erreur affichage quiz : " + e.getMessage());
        }
        return quizzes;
    }

    // ── READ ONE (objet) : Retourne un quiz par son id ────────────────────────
    // Utilisé après une modification pour rafraîchir l'affichage
    public Quiz findById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) return mapQuiz(rs);
            }
        } catch (SQLException e) {
            System.err.println("Erreur findById quiz : " + e.getMessage());
        }
        return null; // null si aucun quiz trouvé
    }

    // ── Méthode privée : convertit une ligne SQL en objet Quiz ────────────────
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
                updatedAt == null ? null : updatedAt.toLocalDateTime(),
                (Integer) rs.getObject("chapitre_id")
        );
    }

    // ── READ BY CHAPITRE : Retourne les quiz d'un chapitre spécifique ─────────
    public java.util.List<Quiz> findByChapitreId(int chapitreId) {
        java.util.List<Quiz> quizzes = new java.util.ArrayList<>();
        String req = "SELECT * FROM quiz WHERE chapitre_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, chapitreId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) quizzes.add(mapQuiz(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByChapitreId : " + e.getMessage());
        }
        return quizzes;
    }
}

