package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePost {

    private Connection connection = MyConnection.getInstance().getConnection();

    private Post fromRs(ResultSet rs) throws SQLException {
        return new Post(
            rs.getInt("id"), rs.getString("contenu"), rs.getString("titre"),
            rs.getString("ai_reaction"), rs.getString("ai_reaction_data"),
            rs.getString("summary"), rs.getString("image_file"), rs.getString("video_file"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
            rs.getInt("communeute_id"), rs.getInt("user_id"));
    }

    public List<Post> getByCommunaute(int communauteId) {
        List<Post> list = new ArrayList<>();
        String req = "SELECT * FROM post WHERE communeute_id=? ORDER BY created_at DESC";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, communauteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(fromRs(rs));
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return list;
    }

    public void ajouter(Post p) {
        String req = "INSERT INTO post (contenu, titre, ai_reaction, ai_reaction_data, summary, image_file, video_file, created_at, communeute_id, user_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, p.getContenu());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getAiReaction());
            ps.setString(4, p.getAiReactionData());
            ps.setString(5, p.getSummary());
            ps.setString(6, p.getImageFile());
            ps.setString(7, p.getVideoFile());
            ps.setTimestamp(8, p.getCreatedAt() != null ? Timestamp.valueOf(p.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setInt(9, p.getCommunauteId());
            ps.setInt(10, p.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void modifier(Post p) {
        String req = "UPDATE post SET contenu=?, titre=?, ai_reaction=?, ai_reaction_data=?, summary=?, image_file=?, video_file=?, communeute_id=?, user_id=? WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setString(1, p.getContenu());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getAiReaction());
            ps.setString(4, p.getAiReactionData());
            ps.setString(5, p.getSummary());
            ps.setString(6, p.getImageFile());
            ps.setString(7, p.getVideoFile());
            ps.setInt(8, p.getCommunauteId());
            ps.setInt(9, p.getUserId());
            ps.setInt(10, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void supprimer(Post p) {
        String req = "DELETE FROM post WHERE id=?";
        try {
            PreparedStatement ps = connection.prepareStatement(req);
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }
}
