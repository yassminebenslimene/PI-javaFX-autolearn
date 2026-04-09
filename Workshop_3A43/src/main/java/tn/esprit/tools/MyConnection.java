package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyConnection {

    private Connection connection;

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static final String URL = "jdbc:mysql://localhost:3306/autolearn_db";

    private static MyConnection instance;

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion a la base etablie avec succes.");
            initializeSchema();
        } catch (SQLException e) {
            System.err.println("Echec de connexion a la base : " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("La connexion a la base n'est pas disponible.");
        }
        return connection;
    }

    private void initializeSchema() throws SQLException {
        String createQuizTable = "CREATE TABLE IF NOT EXISTS quiz (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "titre VARCHAR(255) NOT NULL," +
                "description LONGTEXT NOT NULL," +
                "etat VARCHAR(50) NOT NULL," +
                "duree_max_minutes INT NULL," +
                "seuil_reussite INT NULL," +
                "max_tentatives INT NULL," +
                "image_name VARCHAR(255) NULL," +
                "image_size INT NULL," +
                "updated_at DATETIME NULL" +
                ")";

        String createQuestionTable = "CREATE TABLE IF NOT EXISTS question (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "texte_question LONGTEXT NOT NULL," +
                "point INT NOT NULL," +
                "updated_at DATETIME NULL," +
                "quiz_id INT NOT NULL," +
                "CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz(id) ON DELETE CASCADE" +
                ")";

        String createOptionTable = "CREATE TABLE IF NOT EXISTS `option` (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "texte_option VARCHAR(255) NOT NULL," +
                "est_correcte TINYINT(1) NOT NULL," +
                "question_id INT NOT NULL," +
                "CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES question(id) ON DELETE CASCADE" +
                ")";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createQuizTable);
            statement.executeUpdate(createQuestionTable);
            statement.executeUpdate(createOptionTable);
            System.out.println("Tables quiz, question et option pretes.");
        }
    }
}
