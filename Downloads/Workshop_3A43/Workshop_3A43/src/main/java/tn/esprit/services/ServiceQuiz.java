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
 * Service Quiz ÔÇö g├¿re toutes les op├®rations SQL sur la table "quiz".
 * Impl├®mente IService<Quiz> pour les 4 op├®rations CRUD de base.
 */
public class ServiceQuiz {

    // Connexion ├á la base de donn├®es (singleton partag├® dans toute l'application)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ÔöÇÔöÇ CREATE : Ins├®rer un nouveau quiz en BDD ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public boolean ajouter(Quiz quiz) {
        String req = "INSERT INTO quiz (titre, description, etat, duree_max_minutes, seuil_reussite, max_tentatives, chapitre_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());
            statement.setObject(5, quiz.getSeuilReussite());
            statement.setObject(6, quiz.getMaxTentatives());
            statement.setObject(7, quiz.getChapitreId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajout quiz : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ÔöÇÔöÇ DELETE : Supprimer un quiz et toutes ses questions/options en cascade ÔöÇÔöÇ
    public boolean supprimer(Quiz quiz) {
        try {
            // ├ëtape 1 : supprimer toutes les options des questions de ce quiz
            String delOptions = "DELETE FROM `option` WHERE question_id IN (SELECT id FROM question WHERE quiz_id = ?)";
            try (PreparedStatement st = connection.prepareStatement(delOptions)) {
                st.setInt(1, quiz.getId());
                st.executeUpdate();
            }
            // ├ëtape 2 : supprimer toutes les questions du quiz
            String delQuestions = "DELETE FROM question WHERE quiz_id = ?";
            try (PreparedStatement st = connection.prepareStatement(delQuestions)) {
                st.setInt(1, quiz.getId());
                st.executeUpdate();
            }
            // ├ëtape 3 : supprimer le quiz lui-m├¬me
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

    // ÔöÇÔöÇ UPDATE : Modifier un quiz existant ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public boolean modifier(Quiz quiz) {
        String req = "UPDATE quiz SET titre = ?, description = ?, etat = ?, duree_max_minutes = ?, seuil_reussite = ?, max_tentatives = ?, chapitre_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());
            statement.setObject(5, quiz.getSeuilReussite());
            statement.setObject(6, quiz.getMaxTentatives());
            statement.setObject(7, quiz.getChapitreId());
            statement.setInt(8, quiz.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification quiz : " + e.getMessage());
            return false;
        }
    }

    // ÔöÇÔöÇ READ ALL (console) : Affiche tous les quiz dans la console ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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

    // ÔöÇÔöÇ READ ONE (console) : Affiche un quiz par son id dans la console ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public void getOneById(int id) {
        String req = "SELECT * FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapQuiz(rs));
                } else {
                    System.out.println("Aucun quiz trouv├® avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche quiz : " + e.getMessage());
        }
    }

    // ÔöÇÔöÇ READ ALL (liste) : Retourne tous les quiz sous forme de liste ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    // Utilis├® par les controllers JavaFX pour afficher la liste dans l'interface
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

    // ÔöÇÔöÇ READ ONE (objet) : Retourne un quiz par son id ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    // Utilis├® apr├¿s une modification pour rafra├«chir l'affichage
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
        return null; // null si aucun quiz trouv├®
    }

    // ÔöÇÔöÇ M├®thode priv├®e : convertit une ligne SQL en objet Quiz ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    private Quiz mapQuiz(ResultSet rs) throws SQLException {
        return new Quiz(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("etat"),
                (Integer) rs.getObject("duree_max_minutes"),
                (Integer) rs.getObject("seuil_reussite"),
                (Integer) rs.getObject("max_tentatives"),
                null, null, null,
                (Integer) rs.getObject("chapitre_id")
        );
    }

    // ÔöÇÔöÇ READ BY CHAPITRE : Retourne les quiz d'un chapitre sp├®cifique ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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
