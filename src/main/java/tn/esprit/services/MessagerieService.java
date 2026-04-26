package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MessagerieService — gère la messagerie privée et le système de follow entre étudiants.
 * Stocke tout dans la table "notification" existante (pas de nouvelle table).
 *
 * Types utilisés dans le champ "type" :
 *   "follow_request"  → demande de suivi envoyée
 *   "follow_accepted" → demande acceptée
 *   "follow_rejected" → demande refusée
 *   "chat_message"    → message de chat privé
 */
public class MessagerieService {

    // Connexion BDD via singleton
    private Connection conn() {
        return MyConnection.getInstance().getConnection();
    }

    // ── FOLLOW — Demandes de suivi ────────────────────────────────────────────

    // Envoie une demande de follow (ne fait rien si déjà envoyée)
    public boolean envoyerDemandeFollow(int senderId, int receiverId, String senderName) {
        // Vérifier si une demande pending existe déjà
        if (demandeFollowExiste(senderId, receiverId)) return false;

        String sql = "INSERT INTO notification (type, title, message, is_read, created_at, user_id) " +
                     "VALUES ('follow_request', ?, ?, 0, NOW(), ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, "Demande de suivi de " + senderName);
            ps.setString(2, buildFollowJson(senderId, senderName, "pending"));
            ps.setInt(3, receiverId);
            ps.executeUpdate();
            System.out.println("[Messagerie] Follow request: " + senderId + " → " + receiverId);
            return true;
        } catch (SQLException e) {
            System.err.println("[Messagerie] envoyerDemandeFollow: " + e.getMessage());
            return false;
        }
    }

    // Vérifie si une demande de follow existe déjà entre deux utilisateurs
    public boolean demandeFollowExiste(int senderId, int receiverId) {
        String sql = "SELECT COUNT(*) FROM notification " +
                     "WHERE type IN ('follow_request','follow_accepted') " +
                     "AND user_id = ? AND message LIKE ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setString(2, "%\"senderId\":" + senderId + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[Messagerie] demandeFollowExiste: " + e.getMessage());
        }
        return false;
    }

    // Vérifie si deux utilisateurs se suivent mutuellement
    public boolean seSuivent(int userId1, int userId2) {
        // Chercher une notification follow_accepted dans les deux sens
        String sql = "SELECT COUNT(*) FROM notification " +
                     "WHERE type = 'follow_accepted' " +
                     "AND ((user_id = ? AND message LIKE ?) OR (user_id = ? AND message LIKE ?))";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId2);
            ps.setString(2, "%\"senderId\":" + userId1 + "%");
            ps.setInt(3, userId1);
            ps.setString(4, "%\"senderId\":" + userId2 + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[Messagerie] seSuivent: " + e.getMessage());
        }
        return false;
    }

    // Retourne les demandes de follow en attente pour un utilisateur
    public List<Map<String, Object>> getDemandesFollowEnAttente(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT id, message, created_at FROM notification " +
                     "WHERE type = 'follow_request' AND user_id = ? AND is_read = 0 " +
                     "ORDER BY created_at DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("createdAt", rs.getTimestamp("created_at"));
                parseFollowJson(rs.getString("message"), m);
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getDemandesFollowEnAttente: " + e.getMessage());
        }
        return list;
    }

    // Accepte une demande de follow et notifie l'expéditeur
    public void accepterFollow(int notifId, int senderId, String receiverName, int receiverId) {
        // Mettre à jour la demande → accepted
        String upd = "UPDATE notification SET type='follow_accepted', is_read=1, read_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(upd)) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] accepterFollow update: " + e.getMessage());
        }

        // Notifier l'expéditeur que sa demande a été acceptée
        String ins = "INSERT INTO notification (type, title, message, is_read, created_at, user_id) " +
                     "VALUES ('follow_accepted', ?, ?, 0, NOW(), ?)";
        try (PreparedStatement ps = conn().prepareStatement(ins)) {
            ps.setString(1, receiverName + " a accepté votre demande de suivi");
            ps.setString(2, buildFollowJson(receiverId, receiverName, "accepted"));
            ps.setInt(3, senderId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] accepterFollow notif: " + e.getMessage());
        }
    }

    // Refuse une demande de follow
    public void refuserFollow(int notifId) {
        String sql = "UPDATE notification SET type='follow_rejected', is_read=1, read_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] refuserFollow: " + e.getMessage());
        }
    }

    // Retourne la liste des utilisateurs suivis (follow accepté)
    public List<Map<String, Object>> getFollowing(int userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        // Cas 1 : userId a envoyé la demande → chercher follow_accepted chez le receiver
        String sql = "SELECT DISTINCT n.user_id as otherId, u.nom, u.prenom " +
                     "FROM notification n " +
                     "JOIN user u ON u.userId = n.user_id " +
                     "WHERE n.type = 'follow_accepted' " +
                     "AND n.message LIKE ? " +
                     "UNION " +
                     "SELECT DISTINCT CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(n.message,'\"senderId\":',−1),',',1) AS UNSIGNED) as otherId, " +
                     "u.nom, u.prenom " +
                     "FROM notification n " +
                     "JOIN user u ON u.userId = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(n.message,'\"senderId\":',−1),',',1) AS UNSIGNED) " +
                     "WHERE n.type = 'follow_accepted' AND n.user_id = ?";
        // Approche simplifiée : récupérer tous les follow_accepted liés à userId
        return getContacts(userId);
    }

    // Retourne tous les contacts avec qui on peut chatter (follow accepté des deux côtés)
    public List<Map<String, Object>> getContacts(int userId) {
        List<Map<String, Object>> contacts = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();

        // Chercher toutes les notifications follow_accepted où userId est impliqué
        String sql = "SELECT n.user_id, n.message, u.nom, u.prenom " +
                     "FROM notification n " +
                     "JOIN user u ON u.userId = n.user_id " +
                     "WHERE n.type = 'follow_accepted' " +
                     "AND n.message LIKE ? ";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, "%\"senderId\":" + userId + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int otherId = rs.getInt("user_id");
                if (!seen.contains(otherId)) {
                    seen.add(otherId);
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", otherId);
                    m.put("nom", rs.getString("nom"));
                    m.put("prenom", rs.getString("prenom"));
                    contacts.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getContacts (sent): " + e.getMessage());
        }

        // Chercher aussi les cas où userId a reçu la demande et l'a acceptée
        String sql2 = "SELECT DISTINCT u.userId, u.nom, u.prenom " +
                      "FROM notification n " +
                      "JOIN user u ON u.userId = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(n.message,'\"senderId\":',−1),',',1) AS UNSIGNED) " +
                      "WHERE n.type = 'follow_accepted' AND n.user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql2)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int otherId = rs.getInt("userId");
                if (!seen.contains(otherId)) {
                    seen.add(otherId);
                    Map<String, Object> m = new HashMap<>();
                    m.put("userId", otherId);
                    m.put("nom", rs.getString("nom"));
                    m.put("prenom", rs.getString("prenom"));
                    contacts.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getContacts (received): " + e.getMessage());
        }

        return contacts;
    }

    // ── CHAT — Messages privés ────────────────────────────────────────────────

    // Envoie un message texte simple
    public void envoyerMessage(int senderId, int receiverId, String senderName, String texte) {
        envoyerMessageComplet(senderId, receiverId, senderName, texte, null, null);
    }

    // Envoie un message avec fichier ou image joint
    public void envoyerMessageComplet(int senderId, int receiverId, String senderName,
                                       String texte, String fileType, String filePath) {
        String json = buildChatJsonFull(senderId, senderName,
            texte.isEmpty() && filePath != null ? "[fichier]" : texte,
            receiverId, fileType, filePath);
        String title = senderName + (filePath != null ? " vous a envoyé un fichier" : " vous a envoyé un message");
        String sql = "INSERT INTO notification (type, title, message, is_read, created_at, user_id) " +
                     "VALUES ('chat_message', ?, ?, 0, NOW(), ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, json);
            ps.setInt(3, receiverId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] envoyerMessageComplet: " + e.getMessage());
        }
    }

    // Marque un message comme lu
    public void marquerVu(int notifId) {
        String sql = "UPDATE notification SET is_read=1, read_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] marquerVu: " + e.getMessage());
        }
    }

    // Vérifie si un message a été lu
    public boolean estVu(int notifId) {
        String sql = "SELECT is_read FROM notification WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBoolean("is_read");
        } catch (SQLException e) {
            System.err.println("[Messagerie] estVu: " + e.getMessage());
        }
        return false;
    }

    // Retourne tous les messages entre deux utilisateurs, triés par date
    public List<Map<String, Object>> getConversation(int userId1, int userId2) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // Messages envoyés par userId1 à userId2
        String sql = "SELECT id, message, created_at FROM notification " +
                     "WHERE type = 'chat_message' AND user_id = ? AND message LIKE ? " +
                     "ORDER BY created_at ASC";

        // Messages reçus par userId1 de userId2
        String sql2 = "SELECT id, message, created_at FROM notification " +
                      "WHERE type = 'chat_message' AND user_id = ? AND message LIKE ? " +
                      "ORDER BY created_at ASC";

        try {
            // Messages où userId2 est le receiver et userId1 est le sender
            PreparedStatement ps1 = conn().prepareStatement(sql);
            ps1.setInt(1, userId2);
            ps1.setString(2, "%\"senderId\":" + userId1 + "%\"conversationWith\":" + userId2 + "%");
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs1.getInt("id"));
                m.put("sentAt", rs1.getTimestamp("created_at"));
                parseChatJson(rs1.getString("message"), m);
                m.put("isOwn", true); // envoyé par userId1
                messages.add(m);
            }

            // Messages où userId1 est le receiver et userId2 est le sender
            PreparedStatement ps2 = conn().prepareStatement(sql2);
            ps2.setInt(1, userId1);
            ps2.setString(2, "%\"senderId\":" + userId2 + "%\"conversationWith\":" + userId1 + "%");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs2.getInt("id"));
                m.put("sentAt", rs2.getTimestamp("created_at"));
                parseChatJson(rs2.getString("message"), m);
                m.put("isOwn", false); // reçu par userId1
                messages.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getConversation: " + e.getMessage());
        }

        // Trier par date
        messages.sort((a, b) -> {
            Timestamp ta = (Timestamp) a.get("sentAt");
            Timestamp tb = (Timestamp) b.get("sentAt");
            if (ta == null || tb == null) return 0;
            return ta.compareTo(tb);
        });

        // Marquer les messages reçus comme lus
        marquerMessagesLus(userId1, userId2);

        return messages;
    }

    // Compte les messages non lus pour un utilisateur
    public int getNombreMessagesNonLus(int userId) {
        String sql = "SELECT COUNT(*) FROM notification " +
                     "WHERE type = 'chat_message' AND user_id = ? AND is_read = 0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[Messagerie] getNombreMessagesNonLus: " + e.getMessage());
        }
        return 0;
    }

    // Marque tous les messages d'une conversation comme lus (appelé automatiquement dans getConversation)
    private void marquerMessagesLus(int receiverId, int senderId) {
        String sql = "UPDATE notification SET is_read=1, read_at=NOW() " +
                     "WHERE type='chat_message' AND user_id=? AND message LIKE ? AND is_read=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setString(2, "%\"senderId\":" + senderId + "%");
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] marquerMessagesLus: " + e.getMessage());
        }
    }

    // Retourne les nouveaux messages depuis un ID donné (polling temps réel)
    public List<Map<String, Object>> getNouveauxMessages(int receiverId, int senderId, int dernierId) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String sql = "SELECT id, message, created_at FROM notification " +
                     "WHERE type = 'chat_message' AND user_id = ? " +
                     "AND message LIKE ? AND id > ? " +
                     "ORDER BY created_at ASC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, receiverId);
            ps.setString(2, "%\"senderId\":" + senderId + "%");
            ps.setInt(3, dernierId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("id"));
                m.put("sentAt", rs.getTimestamp("created_at"));
                parseChatJson(rs.getString("message"), m);
                m.put("isOwn", false);
                messages.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getNouveauxMessages: " + e.getMessage());
        }
        return messages;
    }

    // ── UTILISATEURS ─────────────────────────────────────────────────────────

    // Retourne tous les étudiants actifs sauf l'utilisateur courant
    public List<Map<String, Object>> getTousLesEtudiants(int currentUserId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT userId, nom, prenom FROM user " +
                     "WHERE discr = 'etudiant' AND userId != ? AND isSuspended = 0 " +
                     "ORDER BY prenom, nom";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("userId", rs.getInt("userId"));
                m.put("nom", rs.getString("nom"));
                m.put("prenom", rs.getString("prenom"));
                list.add(m);
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getTousLesEtudiants: " + e.getMessage());
        }
        return list;
    }

    // ── HELPERS JSON — Construction et parsing du champ "message" ────────────

    private String buildFollowJson(int senderId, String senderName, String status) {
        return "{\"senderId\":" + senderId +
               ",\"senderName\":\"" + senderName.replace("\"", "'") + "\"" +
               ",\"status\":\"" + status + "\"}";
    }

    private String buildChatJson(int senderId, String senderName, String texte, int conversationWith) {
        return buildChatJsonFull(senderId, senderName, texte, conversationWith, null, null);
    }

    // Construit le JSON d'un message avec support fichier/image optionnel
    public String buildChatJsonFull(int senderId, String senderName, String texte,
                                     int conversationWith, String fileType, String filePath) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"senderId\":").append(senderId)
          .append(",\"senderName\":\"").append(senderName.replace("\"", "'")).append("\"")
          .append(",\"text\":\"").append(texte.replace("\"", "'").replace("\n", " ")).append("\"")
          .append(",\"conversationWith\":").append(conversationWith);
        if (fileType != null && filePath != null) {
            sb.append(",\"fileType\":\"").append(fileType).append("\"");
            sb.append(",\"filePath\":\"").append(filePath.replace("\\", "/").replace("\"", "'")).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private void parseFollowJson(String json, Map<String, Object> out) {
        if (json == null) return;
        out.put("senderId",   extractInt(json, "senderId"));
        out.put("senderName", extractStr(json, "senderName"));
        out.put("status",     extractStr(json, "status"));
    }

    private void parseChatJson(String json, Map<String, Object> out) {
        if (json == null) return;
        out.put("senderId",   extractInt(json, "senderId"));
        out.put("senderName", extractStr(json, "senderName"));
        out.put("texte",      extractStr(json, "text"));
        String ft = extractStr(json, "fileType");
        String fp = extractStr(json, "filePath");
        if (!ft.isEmpty()) out.put("fileType", ft);
        if (!fp.isEmpty()) out.put("filePath", fp);
        // Detect deleted flag
        if (json.contains("\"deleted\":true")) out.put("deleted", true);
    }

    /** Modifie le texte d'un message existant (ajoute " (modifié)" dans le JSON). */
    public boolean modifierMessage(int notifId, String nouveauTexte) {
        // Récupérer le message actuel
        String sql = "SELECT message FROM notification WHERE id=? AND type='chat_message'";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;
            String json = rs.getString("message");
            // Remplacer le texte dans le JSON
            // Le champ "text" est entre "text":" et le prochain "
            String newJson = json.replaceAll("\"text\":\"[^\"]*\"",
                "\"text\":\"" + nouveauTexte.replace("\"","'") + " (modifié)\"");
            String upd = "UPDATE notification SET message=? WHERE id=?";
            try (PreparedStatement ps2 = conn().prepareStatement(upd)) {
                ps2.setString(1, newJson);
                ps2.setInt(2, notifId);
                return ps2.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] modifierMessage: " + e.getMessage());
            return false;
        }
    }

    /** Supprime un message (soft delete : remplace le texte par "[Message supprimé]"). */
    public boolean supprimerMessage(int notifId) {
        String sql = "SELECT message FROM notification WHERE id=? AND type='chat_message'";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return false;
            String json = rs.getString("message");
            String newJson = json.replaceAll("\"text\":\"[^\"]*\"", "\"text\":\"[Message supprimé]\"");
            // Ajouter un flag deleted
            newJson = newJson.replace("}", ",\"deleted\":true}");
            String upd = "UPDATE notification SET message=? WHERE id=?";
            try (PreparedStatement ps2 = conn().prepareStatement(upd)) {
                ps2.setString(1, newJson);
                ps2.setInt(2, notifId);
                return ps2.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] supprimerMessage: " + e.getMessage());
            return false;
        }
    }

    /** Récupère la dernière activité d'un utilisateur (lastActivityAt). */
    public String getStatutEnLigne(int userId) {
        String sql = "SELECT lastActivityAt FROM user WHERE userId=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Timestamp last = rs.getTimestamp("lastActivityAt");
                if (last == null) return "Hors ligne";
                long diffMs = System.currentTimeMillis() - last.getTime();
                long diffMin = diffMs / 60000;
                if (diffMin < 2)  return "Actif maintenant";
                if (diffMin < 60) return "Il y a " + diffMin + " min";
                long diffH = diffMin / 60;
                if (diffH < 24)   return "Il y a " + diffH + "h";
                return "Il y a " + (diffH / 24) + "j";
            }
        } catch (SQLException e) {
            System.err.println("[Messagerie] getStatutEnLigne: " + e.getMessage());
        }
        return "Hors ligne";
    }

    /** Met à jour lastActivityAt de l'utilisateur courant. */
    public void mettreAJourActivite(int userId) {
        String sql = "UPDATE user SET lastActivityAt=NOW() WHERE userId=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] mettreAJourActivite: " + e.getMessage());
        }
    }

    private int extractInt(String json, String key) {
        try {
            String marker = "\"" + key + "\":";
            int start = json.indexOf(marker);
            if (start < 0) return 0;
            start += marker.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            return Integer.parseInt(json.substring(start, end));
        } catch (Exception e) { return 0; }
    }

    private String extractStr(String json, String key) {
        try {
            String marker = "\"" + key + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return "";
            start += marker.length();
            int end = json.indexOf("\"", start);
            return end > start ? json.substring(start, end) : "";
        } catch (Exception e) { return ""; }
    }
}
