package tn.esprit.services;

import tn.esprit.entities.Cours;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCours implements IService<Cours> {

    private final Connection connection;

    public ServiceCours() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Cours cours) {
        String req = "INSERT INTO cours (titre, description, matiere, niveau, duree, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, cours.getTitre());
            statement.setString(2, cours.getDescription());
            statement.setString(3, cours.getMatiere());
            statement.setString(4, cours.getNiveau());
            statement.setInt(5, cours.getDuree());
            LocalDateTime createdAt = cours.getCreatedAt() != null ? cours.getCreatedAt() : LocalDateTime.now();
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            System.out.println("Cours ajoute.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Cours cours) {
        String req = "UPDATE cours SET titre = ?, description = ?, matiere = ?, niveau = ?, duree = ?, created_at = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, cours.getTitre());
            statement.setString(2, cours.getDescription());
            statement.setString(3, cours.getMatiere());
            statement.setString(4, cours.getNiveau());
            statement.setInt(5, cours.getDuree());
            LocalDateTime createdAt = cours.getCreatedAt() != null ? cours.getCreatedAt() : LocalDateTime.now();
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            statement.setInt(7, cours.getId());
            statement.executeUpdate();
            System.out.println("Cours modifie.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        // Supprimer d'abord les chapitres liés (contrainte FK)
        String deleteChapitres = "DELETE FROM chapitre WHERE cours_id = ?";
        String deleteCours = "DELETE FROM cours WHERE id = ?";
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement s1 = connection.prepareStatement(deleteChapitres)) {
                s1.setInt(1, id);
                s1.executeUpdate();
            }
            try (PreparedStatement s2 = connection.prepareStatement(deleteCours)) {
                s2.setInt(1, id);
                s2.executeUpdate();
            }
            connection.commit();
            System.out.println("Cours et ses chapitres supprimes.");
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            System.err.println(e.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException e) { System.err.println(e.getMessage()); }
        }
    }

    @Override
    public List<Cours> consulter() {
        List<Cours> coursList = new ArrayList<>();
        String req = "SELECT * FROM cours";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(req)) {
            while (resultSet.next()) {
                coursList.add(mapResultSetToCours(resultSet));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return coursList;
    }

    @Override
    public Cours consulterParId(int id) {
        String req = "SELECT * FROM cours WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToCours(resultSet);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private Cours mapResultSetToCours(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");

        return new Cours(
                resultSet.getInt("id"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                resultSet.getString("matiere"),
                resultSet.getString("niveau"),
                resultSet.getInt("duree"),
                createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }
}
