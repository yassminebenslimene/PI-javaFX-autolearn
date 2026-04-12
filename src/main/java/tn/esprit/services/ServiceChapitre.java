package tn.esprit.services;

import tn.esprit.entities.Chapitre;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceChapitre implements IService<Chapitre> {

    private final Connection connection;

    public ServiceChapitre() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Chapitre chapitre) {
        String req = "INSERT INTO chapitre (titre, contenu, ordre, ressources, cours_id, ressource_type, ressource_fichier) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, chapitre.getTitre());
            statement.setString(2, chapitre.getContenu());
            statement.setInt(3, chapitre.getOrdre());
            statement.setString(4, chapitre.getRessources());
            statement.setInt(5, chapitre.getCoursId());
            statement.setString(6, chapitre.getRessourceType());
            statement.setString(7, chapitre.getRessourceFichier());
            statement.executeUpdate();
            System.out.println("Chapitre ajoute.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void modifier(Chapitre chapitre) {
        String req = "UPDATE chapitre SET titre = ?, contenu = ?, ordre = ?, ressources = ?, cours_id = ?, ressource_type = ?, ressource_fichier = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, chapitre.getTitre());
            statement.setString(2, chapitre.getContenu());
            statement.setInt(3, chapitre.getOrdre());
            statement.setString(4, chapitre.getRessources());
            statement.setInt(5, chapitre.getCoursId());
            statement.setString(6, chapitre.getRessourceType());
            statement.setString(7, chapitre.getRessourceFichier());
            statement.setInt(8, chapitre.getId());
            statement.executeUpdate();
            System.out.println("Chapitre modifie.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM chapitre WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            statement.executeUpdate();
            System.out.println("Chapitre supprime.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public List<Chapitre> getAll() {
        List<Chapitre> chapitres = new ArrayList<>();
        String req = "SELECT * FROM chapitre";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(req)) {
            while (resultSet.next()) {
                chapitres.add(mapResultSetToChapitre(resultSet));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return chapitres;
    }

    @Override
    public Chapitre getById(int id) {
        String req = "SELECT * FROM chapitre WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToChapitre(resultSet);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    public List<Chapitre> consulterParCoursId(int coursId) {
        List<Chapitre> chapitres = new ArrayList<>();
        String req = "SELECT * FROM chapitre WHERE cours_id = ? ORDER BY ordre ASC, id ASC";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, coursId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    chapitres.add(mapResultSetToChapitre(resultSet));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return chapitres;
    }

    private Chapitre mapResultSetToChapitre(ResultSet resultSet) throws SQLException {
        return new Chapitre(
                resultSet.getInt("id"),
                resultSet.getString("titre"),
                resultSet.getString("contenu"),
                resultSet.getInt("ordre"),
                resultSet.getString("ressources"),
                resultSet.getInt("cours_id"),
                resultSet.getString("ressource_type"),
                resultSet.getString("ressource_fichier")
        );
    }
}
