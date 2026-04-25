package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MessagerieService — Messagerie en temps réel + système de follow.
 *
 * Utilise UNIQUEMENT la table "notification" existante (aucune nouvelle table).
 *
 * Convention des types :
 *   "follow_request"  → demande de follow envoyée
 *   "follow_accepted" → demande acceptée
 *   "follow_rejected" → demande refusée
 *   "chat_message"    → message de chat privé
 *
 * Format du champ "message" (JSON simplifié) :
 *   follow_request  : {"senderId":5,"senderName":"Zarrouk Nour","status":"pending"}
 *   chat_message    : {"senderId":5,"senderName":"Zarrouk Nour","text":"Salut !","conversationWith":9}
 */
public class MessagerieService {

    private Connection conn() {
        return MyConnection.getInstance().getConnection();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FOLLOW — Demandes de suivi
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Envoie une demande de follow à un autre étudiant.
     * Ne fait rien si une demande pending existe déjà.
     *
     * @param senderId   ID de celui qui envoie
     * @param receiverId ID de celui qui reçoit
     * @param senderName Nom complet de l'expéditeur (pour l'affichage)
     * @return true si envoyée, false si déjà existante
     */
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

    /**
     * Vérifie si une demande de follow pending existe entre deux utilisateurs.
     */
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

    /**
     * Vérifie si deux utilisateurs se suivent mutuellement (follow accepté).
     */
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

    /**
     * Récupère les demandes de follow en attente pour un utilisateur.
     *
     * @param userId ID du destinataire
     * @return Liste de Maps avec {id, senderId, senderName, createdAt}
     */
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

    /**
     * Accepte une demande de follow.
     * Met à jour le type → "follow_accepted" et marque comme lu.
     * Crée aussi une notification inverse pour informer l'expéditeur.
     *
     * @param notifId    ID de la notification follow_request
     * @param senderId   ID de l'expéditeur original
     * @param receiverName Nom du destinataire (pour la notif inverse)
     */
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

    /**
     * Refuse une demande de follow.
     */
    public void refuserFollow(int notifId) {
        String sql = "UPDATE notification SET type='follow_rejected', is_read=1, read_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, notifId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] refuserFollow: " + e.getMessage());
        }
    }

    /**
     * Retourne la liste des utilisateurs que userId suit (follow accepté).
     * @return Liste de Maps {userId, nom, prenom}
     */
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

    /**
     * Retourne tous les contacts (utilisateurs avec qui on peut chatter).
     * = tous les utilisateurs avec un follow_accepted dans les deux sens.
     */
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

    // ══════════════════════════════════════════════════════════════════════════
    // CHAT — Messages privés
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Envoie un message de chat à un autre utilisateur.
     * Stocké dans notification avec type = "chat_message".
     *
     * @param senderId   ID de l'expéditeur
     * @param receiverId ID du destinataire
     * @param senderName Nom complet de l'expéditeur
     * @param texte      Contenu du message
     */
    public void envoyerMessage(int senderId, int receiverId, String senderName, String texte) {
        String sql = "INSERT INTO notification (type, title, message, is_read, created_at, user_id) " +
                     "VALUES ('chat_message', ?, ?, 0, NOW(), ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, senderName + " vous a envoyé un message");
            ps.setString(2, buildChatJson(senderId, senderName, texte, receiverId));
            ps.setInt(3, receiverId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[Messagerie] envoyerMessage: " + e.getMessage());
        }
    }

    /**
     * Récupère la conversation entre deux utilisateurs.
     * Retourne les messages dans les deux sens, triés par date.
     *
     * @param userId1 Premier utilisateur
     * @param userId2 Deuxième utilisateur
     * @return Liste de Maps {senderId, senderName, texte, sentAt, isOwn}
     */
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

    /**
     * Compte les messages non lus pour un utilisateur.
     */
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

    /**
     * Marque tous les messages d'une conversation comme lus.
     */
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

    /**
     * Récupère les nouveaux messages depuis un certain ID (pour le polling).
     * Utilisé pour rafraîchir le chat en temps réel.
     */
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

    // ══════════════════════════════════════════════════════════════════════════
    // UTILISATEURS — Liste des étudiants connectés
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Retourne tous les étudiants (sauf l'utilisateur courant).
     */
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

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS JSON — Construction et parsing du champ "message"
    // ══════════════════════════════════════════════════════════════════════════

    private String buildFollowJson(int senderId, String senderName, String status) {
        return "{\"senderId\":" + senderId +
               ",\"senderName\":\"" + senderName.replace("\"", "'") + "\"" +
               ",\"status\":\"" + status + "\"}";
    }

    private String buildChatJson(int senderId, String senderName, String texte, int conversationWith) {
        return "{\"senderId\":" + senderId +
               ",\"senderName\":\"" + senderName.replace("\"", "'") + "\"" +
               ",\"text\":\"" + texte.replace("\"", "'").replace("\n", " ") + "\"" +
               ",\"conversationWith\":" + conversationWith + "}";
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
