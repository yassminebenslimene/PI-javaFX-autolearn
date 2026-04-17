package tn.esprit.entities;

import java.util.ArrayList;
import java.util.List;

public class Communaute {
    private int id;
    private String nom;
    private String description;
    private int ownerId;
    private int coursId; // OneToOne avec Cours (cours.communaute_id)

    // ManyToMany : membres approuvés et en attente
    private List<Integer> memberIds        = new ArrayList<>();
    private List<Integer> pendingMemberIds = new ArrayList<>();

    // OneToMany : posts de la communauté
    private List<Post> posts = new ArrayList<>();

    public Communaute() {}

    public Communaute(String nom, String description, int ownerId) {
        this.nom = nom;
        this.description = description;
        this.ownerId = ownerId;
    }

    public Communaute(int id, String nom, String description, int ownerId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.ownerId = ownerId;
    }

    public Communaute(int id, String nom, String description, int ownerId, int coursId) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.ownerId = ownerId;
        this.coursId = coursId;
    }

    // Vérifie si un user peut poster (owner ou membre approuvé)
    public boolean canPost(int userId) {
        return userId == ownerId || memberIds.contains(userId);
    }

    // Vérifie si un user est membre approuvé
    public boolean isMember(int userId) {
        return memberIds.contains(userId);
    }

    // Vérifie si un user a une demande en attente
    public boolean isPending(int userId) {
        return pendingMemberIds.contains(userId);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public int getCoursId() { return coursId; }
    public void setCoursId(int coursId) { this.coursId = coursId; }

    public List<Integer> getMemberIds() { return memberIds; }
    public void setMemberIds(List<Integer> memberIds) { this.memberIds = memberIds; }

    public List<Integer> getPendingMemberIds() { return pendingMemberIds; }
    public void setPendingMemberIds(List<Integer> pendingMemberIds) { this.pendingMemberIds = pendingMemberIds; }

    public List<Post> getPosts() { return posts; }
    public void setPosts(List<Post> posts) { this.posts = posts; }

    @Override
    public String toString() {
        return "Communaute{id=" + id + ", nom='" + nom + "', ownerId=" + ownerId + ", coursId=" + coursId + '}';
    }
}
