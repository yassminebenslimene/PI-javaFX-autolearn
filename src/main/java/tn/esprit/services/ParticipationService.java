package tn.esprit.services;

import tn.esprit.entities.Participation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements IService<Participation> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Participation p) {
        String req = "INSERT INTO participation (equipe_id, evenement_id, statut) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, p.getEquipeId());
            ps.setInt(2, p.getEvenementId());
            ps.setString(3, p.getStatut());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur ajout participation: " + ex.getMessage());
        }
    }

    @Override
    public void modifier(Participation p) {
        String req = "UPDATE participation SET statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, p.getStatut());
            ps.setInt(2, p.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur modification participation: " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM participation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur suppression participation: " + ex.getMessage());
        }
    }

    @Override
    public List<Participation> getAll() {
        List<Participation> list = new ArrayList<>();
        String req = "SELECT * FROM participation";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("Erreur getAll participations: " + ex.getMessage());
        }
        return list;
    }

    @Override
    public Participation getById(int id) {
        String req = "SELECT * FROM participation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            System.err.println("Erreur getById participation: " + ex.getMessage());
        }
        return null;
    }

    public List<Participation> getByEvenement(int evenementId) {
        List<Participation> list = new ArrayList<>();
        String req = "SELECT * FROM participation WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, evenementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("Erreur getByEvenement participation: " + ex.getMessage());
        }
        return list;
    }

    public int countByEvenement(int evenementId) {
        String req = "SELECT COUNT(*) FROM participation WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, evenementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Erreur countByEvenement participation: " + ex.getMessage());
        }
        return 0;
    }

    public int ajouterEtRetournerId(Participation p) {
        String req = "INSERT INTO participation (equipe_id, evenement_id, statut) VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getEquipeId());
            ps.setInt(2, p.getEvenementId());
            ps.setString(3, p.getStatut() != null ? p.getStatut() : "ACCEPTE");
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Erreur ajout participation: " + ex.getMessage());
        }
        return -1;
    }

    public void modifierComplet(Participation p) {
        String req = "UPDATE participation SET equipe_id=?, evenement_id=?, statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, p.getEquipeId());
            ps.setInt(2, p.getEvenementId());
            ps.setString(3, p.getStatut());
            ps.setInt(4, p.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur modifierComplet participation: " + ex.getMessage());
        }
    }

    public List<Participation> getByEtudiant(int etudiantId) {
        List<Participation> list = new ArrayList<>();
        String req = "SELECT p.* FROM participation p JOIN equipe_etudiant ee ON p.equipe_id=ee.equipe_id WHERE ee.etudiant_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("Erreur getByEtudiant participation: " + ex.getMessage());
        }
        return list;
    }

    public Participation getByEquipeAndEvenement(int equipeId, int evenementId) {
        String req = "SELECT * FROM participation WHERE equipe_id=? AND evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, evenementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            System.err.println("Erreur getByEquipeAndEvenement: " + ex.getMessage());
        }
        return null;
    }

    public void supprimerAvecEquipe(int participationId) {
        // Get equipe_id before deleting
        String getEq = "SELECT equipe_id FROM participation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(getEq)) {
            ps.setInt(1, participationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int equipeId = rs.getInt("equipe_id");
                // Delete participation first
                supprimer(participationId);
                // Delete equipe members then equipe
                new tn.esprit.services.EquipeService().supprimerEtudiantsEquipe(equipeId);
                new tn.esprit.services.EquipeService().supprimer(equipeId);
            }
        } catch (SQLException ex) {
            System.err.println("Erreur supprimerAvecEquipe: " + ex.getMessage());
        }
    }

    private Participation mapRow(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getInt("id"));
        p.setEquipeId(rs.getInt("equipe_id"));
        p.setEvenementId(rs.getInt("evenement_id"));
        p.setStatut(rs.getString("statut"));
        p.setFeedbacks(rs.getString("feedbacks"));
        return p;
    }
}
