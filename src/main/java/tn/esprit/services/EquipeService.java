package tn.esprit.services;

import tn.esprit.entities.Equipe;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EquipeService implements IService<Equipe> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Equipe e) {
        String req = "INSERT INTO equipe (nom, evenement_id) VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, e.getNom());
            ps.setInt(2, e.getEvenementId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur ajout equipe: " + ex.getMessage());
        }
    }

    @Override
    public void modifier(Equipe e) {
        String req = "UPDATE equipe SET nom=?, evenement_id=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, e.getNom());
            ps.setInt(2, e.getEvenementId());
            ps.setInt(3, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur modification equipe: " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        String req = "DELETE FROM equipe WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur suppression equipe: " + ex.getMessage());
        }
    }

    @Override
    public List<Equipe> getAll() {
        List<Equipe> list = new ArrayList<>();
        String req = "SELECT * FROM equipe";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("Erreur getAll equipes: " + ex.getMessage());
        }
        return list;
    }

    @Override
    public Equipe getById(int id) {
        String req = "SELECT * FROM equipe WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            System.err.println("Erreur getById equipe: " + ex.getMessage());
        }
        return null;
    }

    public List<Equipe> getByEvenement(int evenementId) {
        List<Equipe> list = new ArrayList<>();
        String req = "SELECT * FROM equipe WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, evenementId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("Erreur getByEvenement equipe: " + ex.getMessage());
        }
        return list;
    }

    public int countByEvenement(int evenementId) {
        String req = "SELECT COUNT(*) FROM equipe WHERE evenement_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, evenementId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Erreur countByEvenement equipe: " + ex.getMessage());
        }
        return 0;
    }

    public void supprimerAvecParticipations(int equipeId) {
        // Delete participations of this equipe first
        String delPart = "DELETE FROM participation WHERE equipe_id=?";
        try (PreparedStatement ps = connection.prepareStatement(delPart)) {
            ps.setInt(1, equipeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur suppression participations equipe: " + ex.getMessage());
        }
        supprimerEtudiantsEquipe(equipeId);
        supprimer(equipeId);
    }

    public int ajouterEtRetournerId(Equipe e) {
        String req = "INSERT INTO equipe (nom, evenement_id) VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNom());
            ps.setInt(2, e.getEvenementId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Erreur ajout equipe: " + ex.getMessage());
        }
        return -1;
    }

    public void ajouterEtudiantEquipe(int equipeId, int etudiantId) {
        String req = "INSERT IGNORE INTO equipe_etudiant (equipe_id, etudiant_id) VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, etudiantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur ajout etudiant equipe: " + ex.getMessage());
        }
    }

    public void supprimerEtudiantsEquipe(int equipeId) {
        String req = "DELETE FROM equipe_etudiant WHERE equipe_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, equipeId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Erreur suppression etudiants equipe: " + ex.getMessage());
        }
    }

    public List<tn.esprit.entities.Etudiant> getEtudiantsByEquipe(int equipeId) {
        List<tn.esprit.entities.Etudiant> list = new ArrayList<>();
        String req = "SELECT u.* FROM user u JOIN equipe_etudiant ee ON u.userId=ee.etudiant_id WHERE ee.equipe_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, equipeId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tn.esprit.entities.Etudiant et = new tn.esprit.entities.Etudiant();
                et.setId(rs.getInt("userId"));
                et.setNom(rs.getString("nom"));
                et.setPrenom(rs.getString("prenom"));
                et.setEmail(rs.getString("email"));
                et.setNiveau(rs.getString("niveau"));
                list.add(et);
            }
        } catch (SQLException ex) {
            System.err.println("Erreur getEtudiantsByEquipe: " + ex.getMessage());
        }
        return list;
    }

    public List<tn.esprit.entities.Etudiant> getAllEtudiants() {
        List<tn.esprit.entities.Etudiant> list = new ArrayList<>();
        String req = "SELECT * FROM user WHERE discr='etudiant' ORDER BY prenom, nom";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tn.esprit.entities.Etudiant et = new tn.esprit.entities.Etudiant();
                et.setId(rs.getInt("userId"));
                et.setNom(rs.getString("nom"));
                et.setPrenom(rs.getString("prenom"));
                et.setEmail(rs.getString("email"));
                et.setNiveau(rs.getString("niveau"));
                list.add(et);
            }
        } catch (SQLException ex) {
            System.err.println("Erreur getAllEtudiants: " + ex.getMessage());
        }
        return list;
    }

    public List<Equipe> getEquipesByEtudiant(int etudiantId) {
        List<Equipe> list = new ArrayList<>();
        String req = "SELECT e.* FROM equipe e JOIN equipe_etudiant ee ON e.id=ee.equipe_id WHERE ee.etudiant_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, etudiantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            System.err.println("Erreur getEquipesByEtudiant: " + ex.getMessage());
        }
        return list;
    }

    public int countMembres(int equipeId) {
        String req = "SELECT COUNT(*) FROM equipe_etudiant WHERE equipe_id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, equipeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException ex) {
            System.err.println("Erreur countMembres: " + ex.getMessage());
        }
        return 0;
    }

    private Equipe mapRow(ResultSet rs) throws SQLException {
        Equipe e = new Equipe();
        e.setId(rs.getInt("id"));
        e.setNom(rs.getString("nom"));
        e.setEvenementId(rs.getInt("evenement_id"));
        return e;
    }
}
