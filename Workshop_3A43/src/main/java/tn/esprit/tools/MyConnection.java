package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MyConnection {

    private Connection connection;

    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private final String URL = "jdbc:mysql://localhost:3306/autolearn_db";

    private static MyConnection instance;

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion Etablie!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
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

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    public void testConnection() {
        if (!isConnected()) {
            System.err.println("Connexion a la base impossible.");
            return;
        }

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            if (resultSet.next()) {
                System.out.println("Test SQL reussi.");
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
