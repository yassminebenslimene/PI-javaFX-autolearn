package tn.esprit.services;

import tn.esprit.entities.Exercice;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExerciceService {

    private Connection connection;

    public ExerciceService() {
        connection = MyConnection.getInstance().getConnection();
    }

    public void add(Exercice exercice) {
        String query = "INSERT INTO exercice (question, reponse, points) VALUES (?, ?, ?)";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setString(1, exercice.getQuestion());
            pst.setString(2, exercice.getReponse());
            pst.setInt(3, exercice.getPoints());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void update(Exercice exercice) {
        String query = "UPDATE exercice SET question=?, reponse=?, points=? WHERE id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setString(1, exercice.getQuestion());
            pst.setString(2, exercice.getReponse());
            pst.setInt(3, exercice.getPoints());
            pst.setInt(4, exercice.getId());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(int id) {
        String query = "DELETE FROM exercice WHERE id=?";
        try {
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Exercice> getAll() {
        List<Exercice> exercices = new ArrayList<>();
        String query = "SELECT * FROM exercice";
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Exercice e = new Exercice(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("reponse"),
                        rs.getInt("points")
                );
                exercices.add(e);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }
}
