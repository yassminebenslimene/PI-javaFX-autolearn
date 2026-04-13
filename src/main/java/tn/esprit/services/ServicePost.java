package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePost {

    // Toujours récupérer la connexion fraîche (auto-reconnect)
    private Connection conn() {
        return MyConnection.getInstance().getConnection();
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try {
            ResultSet rs = conn().createStatement().executeQuery("SELECT * FROM post ORDER BY created_at DESC");
            while (rs.next()) list.add(fromRs(rs));
        } catch (SQLException e) { System.err.println("[ServicePost] getAll: " + e.getMessage()); }
        return list;
    }

    public List<Post> getByCommunaute(int communauteId) {
        List<Post> list = new ArrayList<>();
        String req = "SELECT * FROM post WHERE communaute_id=? ORDER BY created_at DESC";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, communauteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(fromRs(rs));
            System.out.println("[ServicePost] getByCommunaute(" + communauteId + ") -> " + list.size() + " posts");
        } catch (SQLException e) { System.err.println("[ServicePost] getByCommunaute: " + e.getMessage()); }
        return list;
    }

    public Post getById(int id) {
        String req = "SELECT * FROM post WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return fromRs(rs);
        } catch (SQLException e) { System.err.println("[ServicePost] getById: " + e.getMessage()); }
        return null;
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    public void ajouter(Post p) {
        String req = "INSERT INTO post (contenu, titre, ai_reaction, ai_reaction_data, summary, " +
                     "image_file, video_file, created_at, communaute_id, user_id) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ps = conn().prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getContenu());
            ps.setString(2, p.getTitre());
            ps.setString(3, p.getAiReaction());
            ps.setString(4, p.getAiReactionData());
            ps.setString(5, p.getSummary());
            ps.setString(6, p.getImageFile());
            ps.setString(7, p.getVideoFile());
            ps.setTimestamp(8, p.getCreatedAt() != null
                ? Timestamp.valueOf(p.getCreatedAt())
                : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setInt(9, p.getCommunauteId());
            ps.setInt(10, p.getUserId());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                p.setId(keys.getInt(1));
                System.out.println("[ServicePost] ajouter OK id=" + p.getId() + " communauteId=" + p.getCommunauteId());
            }
        } catch (SQLException e) { System.err.println("[ServicePost] ajouter: " + e.getMessage()); }
    }

    public void modifier(Post p) {
        String req = "UPDATE post SET contenu=?, titre=?, ai_reaction=?, ai_reaction_data=?, " +
                     "summary=?, image_file=?, video_file=?, communaute_id=?, user_id=? WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
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
        } catch (SQLException e) { System.err.println("[ServicePost] modifier: " + e.getMessage()); }
    }

    public void supprimer(Post p) {
        String req = "DELETE FROM post WHERE id=?";
        try {
            PreparedStatement ps = conn().prepareStatement(req);
            ps.setInt(1, p.getId());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("[ServicePost] supprimer: " + e.getMessage()); }
    }

    // ── Helper privé ─────────────────────────────────────────────────────────

    private Post fromRs(ResultSet rs) throws SQLException {
        return new Post(
            rs.getInt("id"),
            rs.getString("contenu"),
            rs.getString("titre"),
            rs.getString("ai_reaction"),
            rs.getString("ai_reaction_data"),
            rs.getString("summary"),
            rs.getString("image_file"),
            rs.getString("video_file"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
            rs.getInt("communaute_id"),
            rs.getInt("user_id")
        );
    }
}
