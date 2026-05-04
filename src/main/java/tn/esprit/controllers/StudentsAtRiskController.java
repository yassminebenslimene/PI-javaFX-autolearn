package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.StudentRisk;
import tn.esprit.services.StudentRiskService;

public class StudentsAtRiskController {

    @FXML private VBox studentsContainer;
    @FXML private Label critiqueCount;
    @FXML private Label attentionCount;
    @FXML private Label surveillanceCount;
    @FXML private Label emptyLabel;

    private StudentRiskService studentRiskService;
    private ObservableList<StudentRisk> studentsList;

    @FXML
    public void initialize() {
        studentRiskService = new StudentRiskService();
        loadData();
    }

    private void loadData() {
        studentsList = FXCollections.observableArrayList(studentRiskService.getStudentsAtRisk());
        displayStudents();

        critiqueCount.setText(String.valueOf(studentRiskService.getCritiqueCount()));
        attentionCount.setText(String.valueOf(studentRiskService.getAttentionCount()));
        surveillanceCount.setText(String.valueOf(studentRiskService.getSurveillanceCount()));
    }

    private void displayStudents() {
        studentsContainer.getChildren().clear();

        if (studentsList.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        for (StudentRisk student : studentsList) {
            HBox row = createStudentRow(student);
            studentsContainer.getChildren().add(row);
        }
    }

    private HBox createStudentRow(StudentRisk student) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;");
        row.setPrefHeight(50);

        row.setOnMouseEntered(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:rgba(255,255,255,0.03);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;"));

        // Nom
        Label nomLabel = new Label(student.getNomComplet());
        nomLabel.setStyle("-fx-text-fill:white; -fx-font-size:13;");
        nomLabel.setPrefWidth(200);

        // Niveau avec badge
        Label niveauLabel = new Label(student.getNiveau());
        String niveauColor = student.getNiveau().equals("Débutant") ? "#34d399" :
                (student.getNiveau().equals("Intermédiaire") ? "#fbbf24" : "#f87171");
        niveauLabel.setStyle("-fx-text-fill:" + niveauColor + "; -fx-font-size:13; -fx-font-weight:bold;");
        niveauLabel.setPrefWidth(100);

        // Statut
        Label statutLabel = new Label(student.getStatutRisque());
        String statutColor = student.getStatutRisque().equals("CRITIQUE") ? "#ef4444" :
                (student.getStatutRisque().equals("ATTENTION") ? "#f59e0b" : "#3b82f6");
        statutLabel.setStyle("-fx-text-fill:" + statutColor + "; -fx-font-size:13; -fx-font-weight:bold;");
        statutLabel.setPrefWidth(120);

        // Jours d'inactivité
        Label inactiviteLabel = new Label(student.getJoursInactivite() + " jours");
        String inactiviteColor = student.getJoursInactivite() > 30 ? "#ef4444" :
                (student.getJoursInactivite() > 14 ? "#f59e0b" : "rgba(245,245,244,0.7)");
        inactiviteLabel.setStyle("-fx-text-fill:" + inactiviteColor + "; -fx-font-size:13;");
        inactiviteLabel.setPrefWidth(100);

        // Score moyen
        Label scoreLabel = new Label(String.format("%.1f%%", student.getScoreMoyen()));
        String scoreColor = student.getScoreMoyen() < 30 ? "#ef4444" :
                (student.getScoreMoyen() < 50 ? "#f59e0b" : "rgba(245,245,244,0.7)");
        scoreLabel.setStyle("-fx-text-fill:" + scoreColor + "; -fx-font-size:13; -fx-font-weight:bold;");
        scoreLabel.setPrefWidth(120);

        // Taux d'abandon
        Label abandonLabel = new Label(String.format("%.1f%%", student.getTauxAbandon()));
        String abandonColor = student.getTauxAbandon() > 50 ? "#ef4444" :
                (student.getTauxAbandon() > 25 ? "#f59e0b" : "rgba(245,245,244,0.7)");
        abandonLabel.setStyle("-fx-text-fill:" + abandonColor + "; -fx-font-size:13;");
        abandonLabel.setPrefWidth(100);

        row.getChildren().addAll(nomLabel, niveauLabel, statutLabel, inactiviteLabel, scoreLabel, abandonLabel);

        return row;
    }
}