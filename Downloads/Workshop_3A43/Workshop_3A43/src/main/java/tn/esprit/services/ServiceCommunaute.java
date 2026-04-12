package tn.esprit.services;

import tn.esprit.entities.Communaute;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommunaute {

    private Connection connection = MyConnection.getInstance().getConnection();

    public List<Communaute> getList() {
        List<Communaute> list = new ArrayList<>();
        String req = "SELECT * FROM communaute";
        try {
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(req);
            while (rs.next()) {
                list.add(new Communaute(rs.getInt("id"), rs.getString("nom"),
                    rs.getString("description"), rs.getInt("owner_id")));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public void ajouter(Communaute c) {
        String req = "INSERT INTO communaute (nom, description, owner_id) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getOwnerId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void modifier(Communaute c) {
        String req = "UPDATE communaute SET nom=?, description=?, owner_id=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, c.getNom());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getOwnerId());
            ps.setInt(4, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void supprimer(Communaute c) {
        String req = "DELETE FROM communaute WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }
}
