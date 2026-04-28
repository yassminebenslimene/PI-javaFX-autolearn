package tn.esprit.entities;

public class Etudiant extends User {

    private String niveau;

    public Etudiant() {
        super();
        this.role = "ETUDIANT";
    }

    public Etudiant(String nom, String prenom, String email, String password, String niveau) {
        super(nom, prenom, email, password);
        this.role = "ETUDIANT";
        this.niveau = niveau;
    }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    @Override
    public String toString() {
        return "[ETUDIANT] " + super.toString() + " | Niveau: " + niveau;
    }
}
