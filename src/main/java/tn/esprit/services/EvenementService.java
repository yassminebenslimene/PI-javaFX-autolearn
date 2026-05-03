package tn.esprit.services;

import tn.esprit.entities.Evenement;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IService<Evenement> {

    private final Connection connection = MyConnection.getInstance().getConnection();

    @Override
    public void ajouter(Evenement e) {
        String req = "INSERT INTO evenement (titre, lieu, description, type, date_debut, date_fin, status, is_canceled, workflow_status, nb_max) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getLieu());
            ps.setString(3, e.getDescription());
            ps.setString(4, e.getType());
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(6, Timestamp.valueOf(e.getDateFin()));
            ps.setString(7, e.getStatus() != null ? e.getStatus() : "Plannifié");
            ps.setBoolean(8, e.isIsCanceled());
            ps.setString(9, e.getWorkflowStatus() != null ? e.getWorkflowStatus() : "planifie");
            ps.setInt(10, e.getNbMax());
            ps.executeUpdate();
            System.out.println("Événement ajouté : " + e.getTitre());
        } catch (SQLException ex) {
            System.err.println("Erreur ajout événement: " + ex.getMessage());
        }
    }

    @Override
    public void modifier(Evenement e) {
        String req = "UPDATE evenement SET titre=?, lieu=?, description=?, type=?, date_debut=?, date_fin=?, status=?, is_canceled=?, workflow_status=?, nb_max=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getLieu());
            ps.setString(3, e.getDescription());
            ps.setString(4, e.getType());
            ps.setTimestamp(5, Timestamp.valueOf(e.getDateDebut()));
            ps.setTimestamp(6, Timestamp.valueOf(e.getDateFin()));
            ps.setString(7, e.getStatus());
            ps.setBoolean(8, e.isIsCanceled());
            ps.setString(9, e.getWorkflowStatus());
            ps.setInt(10, e.getNbMax());
            ps.setInt(11, e.getId());
            ps.executeUpdate();
            System.out.println("Événement modifié : " + e.getId());
        } catch (SQLException ex) {
            System.err.println("Erreur modification événement: " + ex.getMessage());
        }
    }

    @Override
    public void supprimer(int id) {
        // Suppression en cascade : membres équipes → équipes → participations → événement
        String[] cascade = {
            "DELETE ee FROM equipe_etudiant ee INNER JOIN equipe eq ON ee.equipe_id = eq.id WHERE eq.evenement_id = ?",
            "DELETE FROM equipe WHERE evenement_id = ?",
            "DELETE FROM participation WHERE evenement_id = ?",
            "DELETE FROM evenement WHERE id = ?"
        };
        try {
            connection.setAutoCommit(false);
            for (String req : cascade) {
                try (PreparedStatement ps = connection.prepareStatement(req)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            connection.commit();
            System.out.println("Événement supprimé (cascade) : " + id);
        } catch (SQLException ex) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            System.err.println("Erreur suppression événement: " + ex.getMessage());
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    @Override
    public List<Evenement> getAll() {
        List<Evenement> list = new ArrayList<>();
        String req = "SELECT * FROM evenement ORDER BY date_debut DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException ex) {
            System.err.println("Erreur getAll événements: " + ex.getMessage());
        }
        return list;
    }

    @Override
    public Evenement getById(int id) {
        String req = "SELECT * FROM evenement WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            System.err.println("Erreur getById événement: " + ex.getMessage());
        }
        return null;
    }

    private Evenement mapRow(ResultSet rs) throws SQLException {
        Evenement e = new Evenement();
        e.setId(rs.getInt("id"));
        e.setTitre(rs.getString("titre"));
        e.setLieu(rs.getString("lieu"));
        e.setDescription(rs.getString("description"));
        e.setType(rs.getString("type"));
        Timestamp debut = rs.getTimestamp("date_debut");
        Timestamp fin = rs.getTimestamp("date_fin");
        if (debut != null) e.setDateDebut(debut.toLocalDateTime());
        if (fin != null) e.setDateFin(fin.toLocalDateTime());
        e.setStatus(rs.getString("status"));
        e.setIsCanceled(rs.getBoolean("is_canceled"));
        e.setWorkflowStatus(rs.getString("workflow_status"));
        e.setNbMax(rs.getInt("nb_max"));
        return e;
    }
}
