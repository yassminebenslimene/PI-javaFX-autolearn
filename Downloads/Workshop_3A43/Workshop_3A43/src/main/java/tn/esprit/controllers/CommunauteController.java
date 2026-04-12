package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Communaute;
import tn.esprit.services.ServiceCommunaute;

import java.util.List;

public class CommunauteController {

    @FXML private TableView<Communaute> tableView;
    @FXML private TableColumn<Communaute, Integer> colId;
    @FXML private TableColumn<Communaute, String>  colNom;
    @FXML private TableColumn<Communaute, String>  colDescription;
    @FXML private TableColumn<Communaute, Integer> colOwner;
    @FXML private TableColumn<Communaute, Void>    colActions;
    @FXML private TextField searchField;

    private final ServiceCommunaute service = new ServiceCommunaute();
    private ObservableList<Communaute> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colOwner.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getOwnerId()).asObject());
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            {
                btnEdit.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0; -fx-padding:5 10 5 10;");
                btnDelete.setStyle("-fx-background-color:#e94560; -fx-text-fill:white; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0; -fx-padding:5 10 5 10;");
                btnEdit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> supprimer(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : new HBox(6, btnEdit, btnDelete));
            }
        });

        searchField.textProperty().addListener((obs, o, n) -> filtrer(n));
        loadData();
    }

    public void loadData() {
        data.clear();
        data.addAll(service.getList());
        tableView.setItems(data);
    }

    private void filtrer(String query) {
        if (query == null || query.isBlank()) { tableView.setItems(data); return; }
        String q = query.toLowerCase();
        tableView.setItems(data.filtered(c -> c.getNom().toLowerCase().contains(q)));
    }

    @FXML
    public void onAjouter() { openForm(null); }

    private void openForm(Communaute communaute) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/communaute/form.fxml"));
            javafx.scene.Parent view = loader.load();
            CommunauteFormController ctrl = loader.getController();
            ctrl.setCommunaute(communaute, this);
            // Charger dans le contentArea du backoffice
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane)
                tableView.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void supprimer(Communaute c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer la communauté \"" + c.getNom() + "\" ?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.supprimer(c); loadData(); }
        });
    }
}
