package tn.esprit.services;

import tn.esprit.entities.Option;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service Option — gère toutes les opérations SQL sur la table "`option`".
 * ATTENTION : "option" est un mot réservé en SQL, on utilise des backticks : `option`
 * Implémente IService<Option> pour les 4 opérations CRUD de base.
 */
public class ServiceOption {

    // Connexion à la base de données (singleton partagé)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ── CREATE : Insérer une nouvelle option en BDD ───────────────────────────
    public boolean ajouter(Option option) {
        // Backticks autour de `option` car c'est un mot réservé SQL
        String req = "INSERT INTO `option` (texte_option, est_correcte, question_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte()); // true/false → 1/0 en BDD
            statement.setInt(3, option.getQuestionId());     // lien vers la question parente
            int rows = statement.executeUpdate();
            return rows > 0; // true = insertion réussie
        } catch (SQLException e) {
            System.err.println("Erreur ajout option : " + e.getMessage());
            return false;
        }
    }

    // ── DELETE : Supprimer une option par son id ──────────────────────────────
    public boolean supprimer(Option option) {
        String req = "DELETE FROM `option` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, option.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur suppression option : " + e.getMessage());
            return false;
        }
    }

    // ── UPDATE : Modifier une option existante ────────────────────────────────
    public boolean modifier(Option option) {
        String req = "UPDATE `option` SET texte_option = ?, est_correcte = ?, question_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte());
            statement.setInt(3, option.getQuestionId());
            statement.setInt(4, option.getId()); // condition WHERE id = ?
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification option : " + e.getMessage());
            return false;
        }
    }

    // ── READ ALL (console) : Affiche toutes les options dans la console ───────
    public void getAll() {
        String req = "SELECT * FROM `option`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                System.out.println(mapOption(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage options : " + e.getMessage());
        }
    }

    // ── READ ONE (console) : Affiche une option par son id ───────────────────
    public void getOneById(int id) {
        String req = "SELECT * FROM `option` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapOption(rs));
                } else {
                    System.out.println("Aucune option trouvée avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche option : " + e.getMessage());
        }
    }

    // ── READ ALL (liste) : Retourne toutes les options sous forme de liste ────
    public java.util.List<Option> afficher() {
        java.util.List<Option> options = new java.util.ArrayList<>();
        String req = "SELECT * FROM `option`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) options.add(mapOption(rs));
        } catch (SQLException e) {
            System.err.println("Erreur affichage options : " + e.getMessage());
        }
        return options;
    }

    // ── READ BY QUESTION : Retourne les options d'une question spécifique ─────
    // Utilisé pour afficher les options quand on clique "Sélectionner" sur une question
    public java.util.List<Option> findByQuestionId(int questionId) {
        java.util.List<Option> options = new java.util.ArrayList<>();
        String req = "SELECT * FROM `option` WHERE question_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, questionId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) options.add(mapOption(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByQuestionId : " + e.getMessage());
        }
        return options;
    }

    // ── Méthode privée : convertit une ligne SQL en objet Option ─────────────
    private Option mapOption(ResultSet rs) throws SQLException {
        return new Option(
                rs.getInt("id"),
                rs.getString("texte_option"),
                rs.getBoolean("est_correcte"), // 1 → true, 0 → false
                rs.getInt("question_id")
        );
    }
}

