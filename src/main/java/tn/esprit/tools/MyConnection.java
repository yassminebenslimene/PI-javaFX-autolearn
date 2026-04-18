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
            System.out.println("Connexion Etablie!");
            initializeSchema();
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

        String createExerciceTable = "CREATE TABLE IF NOT EXISTS exercice (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "question LONGTEXT NOT NULL," +
                "reponse VARCHAR(255) NOT NULL," +
                "points INT NOT NULL DEFAULT 10" +
                ")";

        String createChallengeTable = "CREATE TABLE IF NOT EXISTS challenge (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "titre VARCHAR(255) NOT NULL," +
                "description LONGTEXT," +
                "date_debut DATE NOT NULL," +
                "date_fin DATE NOT NULL," +
                "niveau VARCHAR(50)," +
                "duree INT DEFAULT 30," +
                "created_by INT DEFAULT 0" +
                ")";

        String createChallengeExerciceTable = "CREATE TABLE IF NOT EXISTS challenge_exercice (" +
                "challenge_id INT NOT NULL," +
                "exercice_id INT NOT NULL," +
                "PRIMARY KEY (challenge_id, exercice_id)" +
                ")";

        String createUserChallengeTable = "CREATE TABLE IF NOT EXISTS user_challenge (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "user_id INT NOT NULL," +
                "challenge_id INT NOT NULL," +
                "current_index INT DEFAULT 0," +
                "answers TEXT," +
                "score INT DEFAULT 0," +
                "total_points INT DEFAULT 0," +
                "completed TINYINT(1) DEFAULT 0," +
                "completed_at DATETIME NULL" +
                ")";

        String createVoteTable = "CREATE TABLE IF NOT EXISTS vote (" +
                "id INT PRIMARY KEY AUTO_INCREMENT," +
                "user_id INT NOT NULL," +
                "challenge_id INT NOT NULL," +
                "valeur INT NOT NULL," +
                "createdvote_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createQuizTable);
            statement.executeUpdate(createQuestionTable);
            statement.executeUpdate(createOptionTable);
            statement.executeUpdate(createExerciceTable);
            statement.executeUpdate(createChallengeTable);
            statement.executeUpdate(createChallengeExerciceTable);
            statement.executeUpdate(createUserChallengeTable);
            statement.executeUpdate(createVoteTable);
        }
    }
}
