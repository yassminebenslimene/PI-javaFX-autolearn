package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Communaute;
import tn.esprit.services.ServiceCommunaute;

import java.util.List;

public class CommunauteController {

    @FXML private TableView<Communaute>            tableView;
    @FXML private TableColumn<Communaute, Integer> colId;
    @FXML private TableColumn<Communaute, String>  colNom;
    @FXML private TableColumn<Communaute, String>  colDescription;
    @FXML private TableColumn<Communaute, Integer> colOwner;
    @FXML private TableColumn<Communaute, Void>    colActions;
    @FXML private TextField                        searchField;
    @FXML private Label                            labelTotal;

    private final ServiceCommunaute service = new ServiceCommunaute();
    private ObservableList<Communaute> data  = FXCollections.observableArrayList();

    private static final String CELL_STYLE =
        "-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;";

    @FXML
    public void initialize() {
        applyDarkTable();

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colId.setCellFactory(col -> darkCell(60));

        colNom.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNom()));
        colNom.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-text-fill:white; -fx-font-weight:600; -fx-font-size:13;");
                setText(empty ? null : v);
            }
        });

        colDescription.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        colDescription.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-text-fill:rgba(245,245,244,0.65); -fx-font-size:12;");
                setText(empty ? null : v);
            }
        });

        colOwner.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getOwnerId()).asObject());
        colOwner.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-alignment:CENTER; -fx-text-fill:#60a5fa; -fx-font-weight:700;");
                setText(empty || v == null ? null : "#" + v);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("✏ Modifier");
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                btnEdit.setStyle(base + "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;");
                btnDelete.setStyle(base + "-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af;");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> openForm(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> supprimer(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE);
                setGraphic(empty ? null : box);
            }
        });

        loadData();
    }

    public void loadData() {
        data.clear();
        List<Communaute> list = service.getList();
        data.addAll(list);
        tableView.setItems(data);
        if (labelTotal != null) labelTotal.setText(String.valueOf(list.size()));
        javafx.application.Platform.runLater(this::applyDarkHeader);
    }

    @FXML private void onSearch()      { filtrer(searchField.getText()); }
    @FXML private void onClearSearch() { searchField.clear(); filtrer(""); }

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
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane)
                tableView.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void supprimer(Communaute c) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer la communaute \"" + c.getNom() + "\" ?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { service.supprimer(c); loadData(); }
        });
    }

    private void applyDarkTable() {
        tableView.setStyle("-fx-background-color:#0f1a14; -fx-border-width:0; -fx-table-cell-border-color:rgba(255,255,255,0.06);");
        tableView.setRowFactory(tv -> {
            TableRow<Communaute> row = new TableRow<>();
            row.setStyle("-fx-background-color:#0f1a14;");
            row.selectedProperty().addListener((obs, was, is) ->
                row.setStyle(is ? "-fx-background-color:rgba(5,150,105,0.18);" : "-fx-background-color:#0f1a14;"));
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isSelected())
                    row.setStyle(is ? "-fx-background-color:rgba(255,255,255,0.04);" : "-fx-background-color:#0f1a14;");
            });
            return row;
        });
        tableView.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) javafx.application.Platform.runLater(this::applyDarkHeader);
        });
    }

    private void applyDarkHeader() {
        javafx.scene.Node header = tableView.lookup("TableHeaderRow");
        if (header != null) header.setStyle("-fx-background-color:#0d1710; -fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        tableView.lookupAll(".column-header").forEach(n -> n.setStyle("-fx-background-color:#0d1710; -fx-border-width:0;"));
        tableView.lookupAll(".column-header .label").forEach(n -> ((Label) n).setStyle("-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-font-weight:700;"));
        tableView.lookupAll(".filler").forEach(n -> n.setStyle("-fx-background-color:#0d1710;"));
        tableView.lookupAll(".scroll-bar .thumb").forEach(n -> n.setStyle("-fx-background-color:rgba(52,211,153,0.22); -fx-background-radius:4;"));
    }

    private <T> TableCell<Communaute, T> darkCell(double w) {
        return new TableCell<>() {
            @Override protected void updateItem(T v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12; -fx-alignment:CENTER;");
                setText(empty || v == null ? null : String.valueOf(v));
            }
        };
    }
}
