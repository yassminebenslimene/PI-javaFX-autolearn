package tn.esprit.services;

import tn.esprit.entities.Option;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceOption implements IService<Option> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public boolean ajouter(Option option) {
        String req = "INSERT INTO `option` (texte_option, est_correcte, question_id) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte());
            statement.setInt(3, option.getQuestionId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur ajout option : " + e.getMessage());
            return false;
        }
    }

    @Override
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

    @Override
    public boolean modifier(Option option) {
        String req = "UPDATE `option` SET texte_option = ?, est_correcte = ?, question_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, option.getTexteOption());
            statement.setBoolean(2, option.isEstCorrecte());
            statement.setInt(3, option.getQuestionId());
            statement.setInt(4, option.getId());
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur modification option : " + e.getMessage());
            return false;
        }
    }

    @Override
    public void getAll() {
        String req = "SELECT * FROM `option`";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(req)) {
            while (rs.next()) {
                Option option = mapOption(rs);
                System.out.println(option);
            }
        } catch (SQLException e) {
            System.err.println("Erreur affichage options : " + e.getMessage());
        }
    }

    @Override
    public void getOneById(int id) {
        String req = "SELECT * FROM `option` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println(mapOption(rs));
                } else {
                    System.out.println("Aucune option trouvee avec l'id " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur recherche option : " + e.getMessage());
        }
    }

    // Returns all options as a list
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

    // Returns options for a specific question
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

    private Option mapOption(ResultSet rs) throws SQLException {
        return new Option(
                rs.getInt("id"),
                rs.getString("texte_option"),
                rs.getBoolean("est_correcte"),
                rs.getInt("question_id")
        );
    }
}
