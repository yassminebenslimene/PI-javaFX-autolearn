package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire {

    private Connection conn() {
        return MyConnection.getInstance().getConnection();
    }

    // Nom exact de la colonne date dans la table commentaire
    // (certaines DB ont "created_at", d'autres "creaed_at" par faute de frappe)
    private static final String DATE_COL = detectDateColumn();

    private static String detectDateColumn() {
        try {
            Connection c = MyConnection.getInstance().getConnection();
            ResultSet rs = c.getMetaData().getColumns(null, null, "commentaire", null);
            while (rs.next()) {
                String col = rs.getString("COLUMN_NAME");
                if (col.equalsIgnoreCase("created_at") || col.equalsIgnoreCase("creaed_at")) {
                    System.out.println("[ServiceCommentaire] date column = " + col);
                    return col;
                }
            }
        } catch (Exception e) { System.err.println("[ServiceCommentaire] detectDateColumn: " + e.getMessage()); }
        return "created_at"; // fallback
    }

    public List<Commentaire> getAll() {
        List<Commentaire> list = new ArrayList<>();
        String req = "SELECT * FROM commentaire ORDER BY " + DATE_COL + " DESC";
        try {
            ResultSet rs = conn().createStatement().executeQuery(req);
            while (rs.next()) list.add(fromRs(rs));
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] getAll: " + e.getMessage()); }
        return list;
    }

    public List<Commentaire> getByPost(int postId) {
        List<Commentaire> list = new ArrayList<>();
        String req = "SELECT * FROM commentaire WHERE post_id=? ORDER BY " + DATE_COL + " ASC";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(fromRs(rs));
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] getByPost: " + e.getMessage()); }
        return list;
    }

    public Commentaire getById(int id) {
        try {
            PreparedStatement ps = conn().prepareStatement("SELECT * FROM commentaire WHERE id=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return fromRs(rs);
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] getById: " + e.getMessage()); }
        return null;
    }

    public void ajouter(Commentaire c) {
        String req = "INSERT INTO commentaire (contenu, " + DATE_COL + ", sentiment, sentiment_score, post_id, user_id) VALUES (?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn().prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, c.getContenu());
            ps.setTimestamp(2, c.getCreatedAt() != null
                ? Timestamp.valueOf(c.getCreatedAt())
                : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(3, c.getSentiment());
            ps.setDouble(4, c.getSentimentScore());
            ps.setInt(5, c.getPostId());
            ps.setInt(6, c.getUserId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setId(keys.getInt(1));
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] ajouter: " + e.getMessage()); }
    }

    public void modifier(Commentaire c) {
        String req = "UPDATE commentaire SET contenu=?, sentiment=?, sentiment_score=?, post_id=?, user_id=? WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setString(1, c.getContenu());
            ps.setString(2, c.getSentiment());
            ps.setDouble(3, c.getSentimentScore());
            ps.setInt(4, c.getPostId());
            ps.setInt(5, c.getUserId());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] modifier: " + e.getMessage()); }
    }

    public void supprimer(Commentaire c) {
        try {
            PreparedStatement ps = conn().prepareStatement("DELETE FROM commentaire WHERE id=?");
            ps.setInt(1, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] supprimer: " + e.getMessage()); }
    }

    public void supprimerByPost(int postId) {
        try {
            PreparedStatement ps = conn().prepareStatement("DELETE FROM commentaire WHERE post_id=?");
            ps.setInt(1, postId);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("[ServiceCommentaire] supprimerByPost: " + e.getMessage()); }
    }

    private Commentaire fromRs(ResultSet rs) throws SQLException {
        Timestamp ts = null;
        try { ts = rs.getTimestamp(DATE_COL); } catch (SQLException ignored) {}
        return new Commentaire(
            rs.getInt("id"),
            rs.getString("contenu"),
            ts != null ? ts.toLocalDateTime() : null,
            rs.getString("sentiment"),
            rs.getDouble("sentiment_score"),
            rs.getInt("post_id"),
            rs.getInt("user_id")
        );
    }
}
