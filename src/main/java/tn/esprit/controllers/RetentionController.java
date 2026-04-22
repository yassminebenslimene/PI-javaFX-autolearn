package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.RetentionData;
import tn.esprit.services.RetentionService;
import tn.esprit.session.SessionManager;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RetentionController {

    @FXML private VBox retentionContainer;
    @FXML private ComboBox<String> cohorteFilter;
    @FXML private Label totalEtudiants;
    @FXML private Label retention1Mois;
    @FXML private Label retention3Mois;
    @FXML private Label retention6Mois;
    @FXML private Label emptyLabel;

    private RetentionService retentionService;
    private List<RetentionData> allData;
    private List<String> cohortes;

    @FXML
    public void initialize() {
        retentionService = new RetentionService();
        loadData();
    }

    private void loadData() {
        allData = retentionService.getRetentionData();
        cohortes = retentionService.getCohortes();

        RetentionService.RetentionSummary summary = retentionService.getRetentionSummary();
        totalEtudiants.setText(String.valueOf(summary.getTotalEtudiants()));
        retention1Mois.setText(summary.getRetention1Mois() + "%");
        retention3Mois.setText(summary.getRetention3Mois() + "%");
        retention6Mois.setText(summary.getRetention6Mois() + "%");

        // Initialiser le filtre
        cohorteFilter.setItems(FXCollections.observableArrayList(cohortes));
        cohorteFilter.getItems().add(0, "Toutes");
        cohorteFilter.setValue("Toutes");
        cohorteFilter.valueProperty().addListener((obs, oldVal, newVal) -> displayData());

        displayData();
    }

    private void displayData() {
        retentionContainer.getChildren().clear();

        String selectedCohorte = cohorteFilter.getValue();

        List<RetentionData> filteredData = allData;
        if (selectedCohorte != null && !selectedCohorte.equals("Toutes")) {
            filteredData = allData.stream()
                    .filter(d -> d.getCohorte().equals(selectedCohorte))
                    .collect(Collectors.toList());
        }

        // Grouper par cohorte
        Map<String, List<RetentionData>> dataByCohorte = filteredData.stream()
                .collect(Collectors.groupingBy(RetentionData::getCohorte));

        if (dataByCohorte.isEmpty()) {
            emptyLabel.setVisible(true);
            return;
        }

        emptyLabel.setVisible(false);

        // Pour chaque cohorte, créer une ligne
        for (String cohorte : cohortes) {
            List<RetentionData> cohorteData = dataByCohorte.get(cohorte);
            if (cohorteData != null) {
                HBox row = createRetentionRow(cohorte, cohorteData);
                retentionContainer.getChildren().add(row);
            }
        }
    }

    private HBox createRetentionRow(String cohorte, List<RetentionData> data) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;");
        row.setPrefHeight(50);

        row.setOnMouseEntered(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:rgba(255,255,255,0.03);"));
        row.setOnMouseExited(e -> row.setStyle("-fx-padding:12 20 12 20; -fx-background-color:transparent;"));

        // Label Cohorte
        Label cohorteLabel = new Label(cohorte);
        cohorteLabel.setStyle("-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:bold;");
        cohorteLabel.setPrefWidth(150);

        // Créer un map pour accéder rapidement aux données par mois
        Map<Integer, RetentionData> dataByMonth = data.stream()
                .collect(Collectors.toMap(RetentionData::getMoisRelatif, d -> d));

        // Créer les 6 cellules de mois (0 à 5)
        HBox moisBox = new HBox(8);
        moisBox.setAlignment(Pos.CENTER);

        for (int mois = 0; mois <= 5; mois++) {
            RetentionData monthData = dataByMonth.get(mois);
            Label moisLabel = new Label();
            moisLabel.setPrefWidth(100);
            moisLabel.setAlignment(Pos.CENTER);
            moisLabel.setStyle("-fx-font-size:13; -fx-font-weight:600;");

            if (monthData != null) {
                double taux = monthData.getTauxRetention();
                moisLabel.setText(String.format("%.1f%%", taux));

                // Couleur selon le taux de rétention
                if (taux >= 70) {
                    moisLabel.setStyle("-fx-text-fill:#34d399; -fx-font-size:13; -fx-font-weight:700;");
                } else if (taux >= 40) {
                    moisLabel.setStyle("-fx-text-fill:#fbbf24; -fx-font-size:13; -fx-font-weight:600;");
                } else if (taux >= 20) {
                    moisLabel.setStyle("-fx-text-fill:#f97316; -fx-font-size:13; -fx-font-weight:600;");
                } else {
                    moisLabel.setStyle("-fx-text-fill:#f87171; -fx-font-size:13; -fx-font-weight:600;");
                }
            } else {
                moisLabel.setText("-");
                moisLabel.setStyle("-fx-text-fill:rgba(245,245,244,0.3); -fx-font-size:13;");
            }

            moisBox.getChildren().add(moisLabel);
        }

        row.getChildren().addAll(cohorteLabel, moisBox);

        return row;
    }

    @FXML
    private void refreshData() {
        loadData();
    }
}