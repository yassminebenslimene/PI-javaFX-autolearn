package tn.esprit.entities;

public class Admin extends User {

    public Admin() {
        super();
        this.role = "ADMIN";
    }

    public Admin(String nom, String prenom, String email, String password) {
        super(nom, prenom, email, password);
        this.role = "ADMIN";
    }

    @Override
    public String toString() {
        return "[ADMIN] " + super.toString();
    }
}