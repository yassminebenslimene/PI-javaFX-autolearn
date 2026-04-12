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
public class ServiceQuiz implements IService<Quiz> {

    // Connexion à la base de données (singleton partagé dans toute l'application)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── CREATE : Insérer un nouveau quiz en BDD ───────────────────────────────
    @Override
    public boolean ajouter(Quiz quiz) {
        String req = "INSERT INTO quiz (titre, description, etat, duree_max_minutes, seuil_reussite, max_tentatives, image_name, image_size, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            // On remplace chaque "?" par la valeur correspondante du quiz
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getEtat());
            statement.setObject(4, quiz.getDureeMaxMinutes());   // null si non renseigné
            statement.setObject(5, quiz.getSeuilReussite());     // null si non renseigné
            statement.setObject(6, quiz.getMaxTentatives());     // null si non renseigné
            statement.setString(7, quiz.getImageName());
            statement.setObject(8, quiz.getImageSize());
            statement.setTimestamp(9, quiz.getUpdatedAt() == null ? null : Timestamp.valueOf(quiz.getUpdatedAt()));
            int rows = statement.executeUpdate(); // nombre de lignes insérées
            return rows > 0; // true = insertion réussie
        } catch (SQLException e) {
            System.err.println("Erreur ajout quiz : " + e.getMessage());
            return false; // false = erreur SQL
        }
    }

    // ── DELETE : Supprimer un quiz par son id ─────────────────────────────────
    @Override
    public boolean supprimer(Quiz quiz) {
        String req = "DELETE FROM quiz WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, quiz.getId());
            int rows = statement.executeUpdate();
            return rows > 0; // true = suppression réussie
        } catch (SQLException e) {
            System.err.println("Erreur suppression quiz : " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE : Modifier un quiz existant ────────────────────────────────────
    @Override
    public boolean modifier(Quiz quiz) {
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
            statement.setInt(10, quiz.getId()); // condition WHERE id = ?
            int rows = statement.executeUpdate();
            return rows > 0; // true = modification réussie
        } catch (SQLException e) {
            System.err.println("Erreur modification quiz : " + e.getMessage());
            return false;
        }
    }

    // ── READ ALL (console) : Affiche tous les quiz dans la console ────────────
    @Override
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
    @Override
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
    // Appelée à chaque fois qu'on lit un résultat depuis la BDD
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
