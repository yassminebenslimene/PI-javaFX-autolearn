package tn.esprit;

import tn.esprit.tools.MyConnection;

public class Main {
    public static void main(String[] args) {
        MyConnection myConnection = MyConnection.getInstance();

        if (myConnection.isConnected()) {
            System.out.println("Application connectee a la base de donnees.");
            myConnection.testConnection();
        } else {
            System.err.println("Verifie MySQL, le nom de la base, le port, l'utilisateur et le mot de passe.");
        }
    }
}
