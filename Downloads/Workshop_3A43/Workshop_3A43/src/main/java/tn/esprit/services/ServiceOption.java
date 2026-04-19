package tn.esprit.services;

import tn.esprit.entities.Option;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Service Option ÔÇö g├¿re toutes les op├®rations SQL sur la table "`option`".
 * ATTENTION : "option" est un mot r├®serv├® en SQL, on utilise des backticks : `option`
 * Impl├®mente IService<Option> pour les 4 op├®rations CRUD de base.
 */
public class ServiceOption {

    // Connexion ├á la base de donn├®es (singleton partag├®)
    private final Connection connection = MyConnection.getInstance().getConnection();

    // ÔöÇÔöÇ CREATE : Ins├®rer une nouvelle option en BDD ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public boolean ajouter(Option option) {
        // Backticks autour de `option` car c'est un mot r├®serv├® SQL
        String req = "INSERT INTO `option` (texte_option, est_correcte, question_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte()); // true/false ÔåÆ 1/0 en BDD
            statement.setInt(3, option.getQuestionId());     // lien vers la question parente
            int rows = statement.executeUpdate();
            return rows > 0; // true = insertion r├®ussie
        } catch (SQLException e) {
            System.err.println("Erreur ajout option : " + e.getMessage());
            return false;
        }
    }

    // ÔöÇÔöÇ DELETE : Supprimer une option par son id ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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

    // ÔöÇÔöÇ UPDATE : Modifier une option existante ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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

    // ÔöÇÔöÇ READ ALL (console) : Affiche toutes les options dans la console ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
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

    // ÔöÇÔöÇ READ ONE (console) : Affiche une option par son id ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    public void getOneById(int id) {
        String req = "SELECT * FROM `option` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapOption(rs));
                } else {
                    System.out.println("Aucune option trouv├®e avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche option : " + e.getMessage());
        }
    }

    // ÔöÇÔöÇ READ ALL (liste) : Retourne toutes les options sous forme de liste ÔöÇÔöÇÔöÇÔöÇ
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

    // ÔöÇÔöÇ READ BY QUESTION : Retourne les options d'une question sp├®cifique ÔöÇÔöÇÔöÇÔöÇÔöÇ
    // Utilis├® pour afficher les options quand on clique "S├®lectionner" sur une question
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

    // ÔöÇÔöÇ M├®thode priv├®e : convertit une ligne SQL en objet Option ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    private Option mapOption(ResultSet rs) throws SQLException {
        return new Option(
                rs.getInt("id"),
                rs.getString("texte_option"),
                rs.getBoolean("est_correcte"), // 1 ÔåÆ true, 0 ÔåÆ false
                rs.getInt("question_id")
        );
    }
}
