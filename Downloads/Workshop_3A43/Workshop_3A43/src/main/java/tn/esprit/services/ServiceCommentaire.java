package tn.esprit.services;

import tn.esprit.entities.Commentaire;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire {

    private Connection connection = MyConnection.getInstance().getConnection();

    private Commentaire fromRs(ResultSet rs) throws SQLException {
        return new Commentaire(
            rs.getInt("id"), rs.getString("contenu"),
            rs.getTimestamp("creaed_at") != null ? rs.getTimestamp("creaed_at").toLocalDateTime() : null,
            rs.getString("sentiment"), rs.getDouble("sentiment_score"),
            rs.getInt("post_id"), rs.getInt("user_id"));
    }

    public List<Commentaire> getByPost(int postId) {
        List<Commentaire> list = new ArrayList<>();
        String req = "SELECT * FROM commentaire WHERE post_id=? ORDER BY creaed_at ASC";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, postId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(fromRs(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public void ajouter(Commentaire c) {
        String req = "INSERT INTO commentaire (contenu, creaed_at, sentiment, sentiment_score, post_id, user_id) VALUES (?,?,?,?,?,?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, c.getContenu());
            ps.setTimestamp(2, c.getCreatedAt() != null ? Timestamp.valueOf(c.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(3, c.getSentiment());
            ps.setDouble(4, c.getSentimentScore());
            ps.setInt(5, c.getPostId());
            ps.setInt(6, c.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void modifier(Commentaire c) {
        String req = "UPDATE commentaire SET contenu=?, sentiment=?, sentiment_score=?, post_id=?, user_id=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, c.getContenu());
            ps.setString(2, c.getSentiment());
            ps.setDouble(3, c.getSentimentScore());
            ps.setInt(4, c.getPostId());
            ps.setInt(5, c.getUserId());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void supprimer(Commentaire c) {
        String req = "DELETE FROM commentaire WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }
}
