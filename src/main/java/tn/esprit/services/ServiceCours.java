package tn.esprit.services;

import tn.esprit.entities.Cours;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceCours — couche d'accès aux données pour l'entité Cours.
 * Toutes les opérations SQL sur la table "cours" passent par ici.
 * Implémente IService<Cours> (ajouter, modifier, supprimer, consulter, consulterParId).
 */
public class ServiceCours implements IService<Cours> {

    // Connexion unique à la BDD (pattern Singleton via MyConnection)
    private final Connection connection;

    public ServiceCours() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────
    /** Insère un nouveau cours dans la table "cours". */
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

    // ── UPDATE ────────────────────────────────────────────────────────────────
    /** Met à jour tous les champs d'un cours existant (identifié par son id). */
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

    // ── DELETE (avec cascade) ─────────────────────────────────────────────────
    /**
     * Supprime un cours ET tous ses chapitres liés.
     * On supprime d'abord les chapitres pour respecter la contrainte de clé étrangère (FK),
     * puis on supprime le cours. Le tout dans une transaction pour garantir la cohérence.
     */
    @Override
    public void supprimer(int id) {
        String deleteChapitres = "DELETE FROM chapitre WHERE cours_id = ?";
        String deleteCours     = "DELETE FROM cours WHERE id = ?";
        try {
            connection.setAutoCommit(false); // début de transaction

            // Étape 1 : supprimer les chapitres liés à ce cours
            try (PreparedStatement s1 = connection.prepareStatement(deleteChapitres)) {
                s1.setInt(1, id);
                s1.executeUpdate();
            }
            // Étape 2 : supprimer le cours lui-même
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
            // Toujours remettre l'auto-commit à true après la transaction
            try { connection.setAutoCommit(true); } catch (SQLException e) { System.err.println(e.getMessage()); }
        }
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────
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

    // ── READ ONE ──────────────────────────────────────────────────────────────
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

    // ── MAPPING ───────────────────────────────────────────────────────────────
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
