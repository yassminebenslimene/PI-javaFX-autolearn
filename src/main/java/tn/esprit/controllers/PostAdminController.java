package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Post;
import tn.esprit.services.ServicePost;
import tn.esprit.services.UserService;

import java.util.List;

public class PostAdminController {

    @FXML private TableView<Post>            tableView;
    @FXML private TableColumn<Post, Integer> colId;
    @FXML private TableColumn<Post, String>  colTitre;
    @FXML private TableColumn<Post, String>  colAuteur;
    @FXML private TableColumn<Post, Integer> colCommunaute;
    @FXML private TableColumn<Post, String>  colDate;
    @FXML private TableColumn<Post, Void>    colActions;
    @FXML private TextField                  searchField;
    @FXML private Label                      labelTotal;

    private final ServicePost servicePost = new ServicePost();
    private final UserService userService = new UserService();
    private ObservableList<Post> data     = FXCollections.observableArrayList();

    private static final String CELL_STYLE =
        "-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;";

    @FXML
    public void initialize() {
        applyDarkTable();

        colId.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getId()).asObject());
        colId.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-text-fill:rgba(245,245,244,0.5); -fx-alignment:CENTER;");
                setText(empty || v == null ? null : String.valueOf(v));
            }
        });

        colTitre.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getTitre() != null ? c.getValue().getTitre() : "(sans titre)"));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-text-fill:white; -fx-font-weight:600; -fx-font-size:13;");
                setText(empty ? null : v);
            }
        });

        colAuteur.setCellValueFactory(c -> {
            var u = userService.trouver(c.getValue().getUserId());
            return new SimpleStringProperty(u != null ? u.getPrenom() + " " + u.getNom() : "#" + c.getValue().getUserId());
        });
        colAuteur.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12;");
                setText(empty ? null : v);
            }
        });

        colCommunaute.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getCommunauteId()).asObject());
        colCommunaute.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-alignment:CENTER; -fx-text-fill:#60a5fa; -fx-font-weight:700;");
                setText(empty || v == null ? null : "#" + v);
            }
        });

        colDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCreatedAt() != null
                ? c.getValue().getCreatedAt().toString().substring(0, 16).replace("T", " ") : "-"));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setStyle(CELL_STYLE + "-fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.5); -fx-font-size:11;");
                setText(empty ? null : v);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = new Button("🗑 Supprimer");
            private final HBox   box       = new HBox(btnDelete);
            {
                btnDelete.setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; " +
                    "-fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0; " +
                    "-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af;");
                box.setAlignment(Pos.CENTER);
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

    private void loadData() {
        data.clear();
        List<Post> list = servicePost.getAll();
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
        tableView.setItems(data.filtered(p ->
            (p.getTitre() != null && p.getTitre().toLowerCase().contains(q)) ||
            (p.getContenu() != null && p.getContenu().toLowerCase().contains(q))));
    }

    private void supprimer(Post p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer ce post ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) { servicePost.supprimer(p); loadData(); }
        });
    }

    private void applyDarkTable() {
        tableView.setStyle("-fx-background-color:#0f1a14; -fx-border-width:0; -fx-table-cell-border-color:rgba(255,255,255,0.06);");
        tableView.setRowFactory(tv -> {
            TableRow<Post> row = new TableRow<>();
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
}
