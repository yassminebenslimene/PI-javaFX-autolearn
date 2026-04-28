package tn.esprit.entities;

import java.util.Date;

public abstract class






 User {

    protected int id;
    protected String nom;
    protected String prenom;
    protected String email;
    protected String password;
    protected String role;
    protected Date createdAt;
    protected boolean isSuspended;
    protected Date suspendedAt;
    protected String suspensionReason;
    protected Integer suspendedBy;
    protected Date lastLoginAt;
    protected Date lastActivityAt;

    public User() {
        this.createdAt = new Date();
        this.isSuspended = false;
        this.lastLoginAt = new Date();
        this.lastActivityAt = new Date();
    }

    public User(String nom, String prenom, String email, String password) {
        this();
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public boolean isIsSuspended() { return isSuspended; }
    public void setIsSuspended(boolean isSuspended) { this.isSuspended = isSuspended; }

    public Date getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(Date suspendedAt) { this.suspendedAt = suspendedAt; }

    public String getSuspensionReason() { return suspensionReason; }
    public void setSuspensionReason(String suspensionReason) { this.suspensionReason = suspensionReason; }

    public Integer getSuspendedBy() { return suspendedBy; }
    public void setSuspendedBy(Integer suspendedBy) { this.suspendedBy = suspendedBy; }

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Date getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(Date lastActivityAt) { this.lastActivityAt = lastActivityAt; }

    @Override
    public String toString() {
        return prenom + " " + nom + " | " + email + " | " + role;
    }
}
