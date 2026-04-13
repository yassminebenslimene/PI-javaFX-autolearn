package tn.esprit.services;

import tn.esprit.entities.Chapitre;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceChapitre ÔÇö couche d'acc├¿s aux donn├®es pour l'entit├® Chapitre.
 * Toutes les op├®rations SQL sur la table "chapitre" passent par ici.
 * Un chapitre appartient toujours ├á un cours (via cours_id = cl├® ├®trang├¿re).
 */
public class ServiceChapitre implements IService<Chapitre> {

    // Connexion unique ├á la BDD (pattern Singleton via MyConnection)
    private final Connection connection;

    public ServiceChapitre() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // ÔöÇÔöÇ CREATE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Ins├¿re un nouveau chapitre dans la table "chapitre". */
    @Override
    public void ajouter(Chapitre chapitre) {
        String req = "INSERT INTO chapitre (titre, contenu, ordre, ressources, cours_id, ressource_type, ressource_fichier) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, chapitre.getTitre());
            statement.setString(2, chapitre.getContenu());
            statement.setInt(3, chapitre.getOrdre());       // position du chapitre dans le cours
            statement.setString(4, chapitre.getRessources()); // lien URL optionnel
            statement.setInt(5, chapitre.getCoursId());     // FK vers la table cours
            statement.setString(6, chapitre.getRessourceType()); // VIDEO, PDF, LIEN, AUTRE
            statement.setString(7, chapitre.getRessourceFichier()); // nom du fichier optionnel
            statement.executeUpdate();
            System.out.println("Chapitre ajoute.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // ÔöÇÔöÇ UPDATE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Met ├á jour tous les champs d'un chapitre existant (identifi├® par son id). */
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
            statement.setInt(8, chapitre.getId()); // WHERE id = ?
            statement.executeUpdate();
            System.out.println("Chapitre modifie.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // ÔöÇÔöÇ DELETE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Supprime un chapitre par son id. */
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

    // ÔöÇÔöÇ READ ALL ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Retourne tous les chapitres de la BDD (tous cours confondus). */
    @Override
    public List<Chapitre> consulter() {
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

    // ÔöÇÔöÇ READ ONE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Retourne un chapitre par son id, ou null s'il n'existe pas. */
    @Override
    public Chapitre consulterParId(int id) {
        String req = "SELECT * FROM chapitre WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return mapResultSetToChapitre(resultSet);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    // ÔöÇÔöÇ READ BY COURS ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /**
     * Retourne tous les chapitres d'un cours donn├®, tri├®s par ordre croissant.
     * Utilis├® pour afficher les chapitres dans l'ordre logique du cours.
     */
    public List<Chapitre> consulterParCoursId(int coursId) {
        List<Chapitre> chapitres = new ArrayList<>();
        // ORDER BY ordre ASC garantit l'affichage dans le bon ordre
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

    // ÔöÇÔöÇ MAPPING ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Convertit une ligne SQL (ResultSet) en objet Java Chapitre. */
    private Chapitre mapResultSetToChapitre(ResultSet resultSet) throws SQLException {
        return new Chapitre(
                resultSet.getInt("id"),
                resultSet.getString("titre"),
                resultSet.getString("contenu"),
                resultSet.getInt("ordre"),
                resultSet.getString("ressources"),
                resultSet.getInt("cours_id"),       // lien vers le cours parent
                resultSet.getString("ressource_type"),
                resultSet.getString("ressource_fichier")
        );
    }
}