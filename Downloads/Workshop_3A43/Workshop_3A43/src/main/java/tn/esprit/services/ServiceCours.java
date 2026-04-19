package tn.esprit.services;

import tn.esprit.entities.Cours;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceCours ÔÇö couche d'acc├¿s aux donn├®es pour l'entit├® Cours.
 * Toutes les op├®rations SQL sur la table "cours" passent par ici.
 * Impl├®mente IService<Cours> (ajouter, modifier, supprimer, consulter, consulterParId).
 */
public class ServiceCours implements IService<Cours> {

    // Connexion unique ├á la BDD (pattern Singleton via MyConnection)
    private final Connection connection;

    public ServiceCours() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // ÔöÇÔöÇ CREATE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Ins├¿re un nouveau cours dans la table "cours". */
    @Override
    public void ajouter(Cours cours) {
        String req = "INSERT INTO cours (titre, description, matiere, niveau, duree, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setString(1, cours.getTitre());
            statement.setString(2, cours.getDescription());
            statement.setString(3, cours.getMatiere());
            statement.setString(4, cours.getNiveau());
            statement.setInt(5, cours.getDuree());
            // Si createdAt est null, on utilise la date/heure actuelle
            LocalDateTime createdAt = cours.getCreatedAt() != null ? cours.getCreatedAt() : LocalDateTime.now();
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            statement.executeUpdate();
            System.out.println("Cours ajoute.");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    // ÔöÇÔöÇ UPDATE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Met ├á jour tous les champs d'un cours existant (identifi├® par son id). */
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

    // ÔöÇÔöÇ DELETE (avec cascade) ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /**
     * Supprime un cours ET tous ses chapitres li├®s.
     * On supprime d'abord les chapitres pour respecter la contrainte de cl├® ├®trang├¿re (FK),
     * puis on supprime le cours. Le tout dans une transaction pour garantir la coh├®rence.
     */
    @Override
    public void supprimer(int id) {
        String deleteChapitres = "DELETE FROM chapitre WHERE cours_id = ?";
        String deleteCours     = "DELETE FROM cours WHERE id = ?";
        try {
            connection.setAutoCommit(false); // d├®but de transaction

            // ├ëtape 1 : supprimer les chapitres li├®s ├á ce cours
            try (PreparedStatement s1 = connection.prepareStatement(deleteChapitres)) {
                s1.setInt(1, id);
                s1.executeUpdate();
            }
            // ├ëtape 2 : supprimer le cours lui-m├¬me
            try (PreparedStatement s2 = connection.prepareStatement(deleteCours)) {
                s2.setInt(1, id);
                s2.executeUpdate();
            }

            connection.commit(); // valider la transaction
            System.out.println("Cours et ses chapitres supprimes.");
        } catch (SQLException e) {
            // En cas d'erreur, annuler toutes les suppressions
            try { connection.rollback(); } catch (SQLException ex) { System.err.println(ex.getMessage()); }
            System.err.println(e.getMessage());
        } finally {
            // Toujours remettre l'auto-commit ├á true apr├¿s la transaction
            try { connection.setAutoCommit(true); } catch (SQLException e) { System.err.println(e.getMessage()); }
        }
    }

    // ÔöÇÔöÇ READ ALL ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Retourne la liste de tous les cours de la BDD. */
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

    // ÔöÇÔöÇ READ ONE ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Retourne un cours par son id, ou null s'il n'existe pas. */
    @Override
    public Cours consulterParId(int id) {
        String req = "SELECT * FROM cours WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(req)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) return mapResultSetToCours(resultSet);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    // ÔöÇÔöÇ MAPPING ÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇÔöÇ
    /** Convertit une ligne SQL (ResultSet) en objet Java Cours. */
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