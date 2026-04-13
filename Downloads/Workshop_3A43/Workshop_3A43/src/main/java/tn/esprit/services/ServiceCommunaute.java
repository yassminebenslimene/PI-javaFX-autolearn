package tn.esprit.services;

import tn.esprit.entities.Communaute;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommunaute {

    private Connection conn() {
        return MyConnection.getInstance().getConnection();
    }

    public List<Communaute> getList() {
        List<Communaute> list = new ArrayList<>();
        try {
            Statement st = conn().createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM communaute");
            while (rs.next()) {
                Communaute c = buildFromRs(rs);
                loadMembers(c);
                list.add(c);
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public Communaute getById(int id) {
        try {
            PreparedStatement ps = conn().prepareStatement("SELECT * FROM communaute WHERE id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Communaute c = buildFromRs(rs);
                loadMembers(c);
                return c;
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return null;
    }

    public Communaute getByCours(int coursId) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "SELECT c.* FROM communaute c JOIN cours co ON co.communaute_id = c.id WHERE co.id=?");
            ps.setInt(1, coursId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Communaute c = buildFromRs(rs);
                loadMembers(c);
                return c;
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return null;
    }

    public int ajouter(Communaute c) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "INSERT INTO communaute (nom, description, owner_id) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getOwnerId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                int newId = keys.getInt(1);
                c.setId(newId);
                return newId;
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return -1;
    }

    public void modifier(Communaute c) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "UPDATE communaute SET nom=?, description=?, owner_id=? WHERE id=?");
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getOwnerId());
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void supprimer(Communaute c) {
        try {
            PreparedStatement ps = conn().prepareStatement("DELETE FROM communaute WHERE id=?");
            ps.setInt(1, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void ajouterMembre(int communauteId, int userId) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "INSERT IGNORE INTO communaute_members (communaute_id, user_id) VALUES (?,?)");
            ps.setInt(1, communauteId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            retirerPending(communauteId, userId);
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void retirerMembre(int communauteId, int userId) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM communaute_members WHERE communaute_id=? AND user_id=?");
            ps.setInt(1, communauteId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void ajouterPending(int communauteId, int userId) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "INSERT IGNORE INTO communaute_pending_members (communaute_id, user_id) VALUES (?,?)");
            ps.setInt(1, communauteId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void retirerPending(int communauteId, int userId) {
        try {
            PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM communaute_pending_members WHERE communaute_id=? AND user_id=?");
            ps.setInt(1, communauteId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    private Communaute buildFromRs(ResultSet rs) throws SQLException {
        Communaute c = new Communaute();
        c.setId(rs.getInt("id"));
        c.setNom(rs.getString("nom"));
        c.setDescription(rs.getString("description"));
        c.setOwnerId(rs.getInt("owner_id"));
        try { c.setCoursId(rs.getInt("cours_id")); } catch (SQLException ignored) {}
        return c;
    }

    private void loadMembers(Communaute c) {
        List<Integer> members = new ArrayList<>();
        try {
            PreparedStatement ps = conn().prepareStatement(
                "SELECT user_id FROM communaute_members WHERE communaute_id=?");
            ps.setInt(1, c.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) members.add(rs.getInt("user_id"));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        c.setMemberIds(members);

        List<Integer> pending = new ArrayList<>();
        try {
            PreparedStatement ps = conn().prepareStatement(
                "SELECT user_id FROM communaute_pending_members WHERE communaute_id=?");
            ps.setInt(1, c.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) pending.add(rs.getInt("user_id"));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        c.setPendingMemberIds(pending);
    }
}
