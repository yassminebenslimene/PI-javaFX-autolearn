package tn.esprit.services;

import tn.esprit.entities.Admin;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IUserService {

    private Connection getConn() {
        return MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(User user) {
        Connection cnx = getConn();
        if (cnx == null) { System.err.println("Pas de connexion DB."); return; }
        if (user instanceof Etudiant e && (e.getNiveau() == null || e.getNiveau().isBlank())) {
            System.err.println("Niveau obligatoire pour étudiant."); return;
        }
        String discr  = (user instanceof Admin) ? "admin" : "etudiant";
        String niveau = (user instanceof Etudiant e) ? e.getNiveau() : null;
        String sql = "INSERT INTO user (nom, prenom, email, password, role, discr, niveau, createdAt) VALUES (?,?,?,?,?,?,?,NOW())";
        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole());
            ps.setString(6, discr);
            ps.setString(7, niveau);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setId(keys.getInt(1));
            }
            System.out.println("✔ Ajouté ID=" + user.getId());
        } catch (SQLException e) { System.err.println("Erreur ajouter: " + e.getMessage()); }
    }

    @Override
    public List<User> afficher() {
        List<User> users = new ArrayList<>();
        Connection cnx = getConn();
        if (cnx == null) return users;
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM user")) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("Erreur afficher: " + e.getMessage()); }
        return users;
    }

    @Override
    public User trouver(int id) {
        Connection cnx = getConn();
        if (cnx == null) return null;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM user WHERE userId=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { System.err.println("Erreur trouver: " + e.getMessage()); }
        return null;
    }

    /** Find by email — used for login */
    public User trouverParEmail(String email) {
        Connection cnx = getConn();
        if (cnx == null) return null;
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM user WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) { System.err.println("Erreur trouverParEmail: " + e.getMessage()); }
        return null;
    }

    @Override
    public void modifier(User updated) {
        Connection cnx = getConn();
        if (cnx == null) return;
        if (updated instanceof Etudiant e && (e.getNiveau() == null || e.getNiveau().isBlank())) {
            System.err.println("Niveau obligatoire pour étudiant."); return;
        }
        String sql = "UPDATE user SET nom=?,prenom=?,email=?,password=?,isSuspended=?," +
                     "suspendedAt=?,suspensionReason=?,suspendedBy=?,niveau=? WHERE userId=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, updated.getNom());
            ps.setString(2, updated.getPrenom());
            ps.setString(3, updated.getEmail());
            ps.setString(4, updated.getPassword());
            ps.setBoolean(5, updated.isIsSuspended());
            ps.setTimestamp(6, updated.getSuspendedAt() != null ? new Timestamp(updated.getSuspendedAt().getTime()) : null);
            ps.setString(7, updated.getSuspensionReason());
            ps.setObject(8, updated.getSuspendedBy());
            ps.setString(9, (updated instanceof Etudiant e) ? e.getNiveau() : null);
            ps.setInt(10, updated.getId());
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "✔ Modifié." : "✘ ID introuvable.");
        } catch (SQLException e) { System.err.println("Erreur modifier: " + e.getMessage()); }
    }

    @Override
    public void supprimer(int id) {
        Connection cnx = getConn();
        if (cnx == null) return;
        try (PreparedStatement ps = cnx.prepareStatement("DELETE FROM user WHERE userId=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            System.out.println(rows > 0 ? "✔ Supprimé id=" + id : "✘ ID introuvable.");
        } catch (SQLException e) { System.err.println("Erreur supprimer: " + e.getMessage()); }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user;
        if ("admin".equals(rs.getString("discr"))) {
            user = new Admin();
        } else {
            Etudiant e = new Etudiant();
            e.setNiveau(rs.getString("niveau"));
            user = e;
        }
        user.setId(rs.getInt("userId"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("createdAt"));
        user.setIsSuspended(rs.getBoolean("isSuspended"));
        Timestamp sat = rs.getTimestamp("suspendedAt");
        if (sat != null) user.setSuspendedAt(sat);
        user.setSuspensionReason(rs.getString("suspensionReason"));
        int sb = rs.getInt("suspendedBy");
        if (!rs.wasNull()) user.setSuspendedBy(sb);
        try {
            Timestamp ll = rs.getTimestamp("lastLoginAt");
            if (ll != null) user.setLastLoginAt(ll);
        } catch (SQLException ignored) {}
        return user;
    }
}
