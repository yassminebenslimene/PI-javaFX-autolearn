package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    Connection connection;

    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private final String URL = "jdbc:mysql://localhost:3306/autolearn_db";

    private static MyConnection instance;

    private MyConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // force le chargement du driver
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion Etablie!");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL introuvable: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Erreur connexion: " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
