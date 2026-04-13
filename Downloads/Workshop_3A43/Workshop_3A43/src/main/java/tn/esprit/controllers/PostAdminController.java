package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Post;
import tn.esprit.services.ServicePost;
import tn.esprit.services.UserService;

import java.util.List;

public class PostAdminController {

    @FXML private VBox tableContainer;

    private final ServicePost servicePost = new ServicePost();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        load();
    }

    private void load() {
        tableContainer.getChildren().clear();

        // Header row
        HBox header = buildRow("ID", "Titre", "Auteur", "Communauté", "Date", "Actions", true);
        tableContainer.getChildren().add(header);

        List<Post> posts = servicePost.getAll();
        for (Post p : posts) {
            String auteur = getUserName(p.getUserId());
            String date   = p.getCreatedAt() != null
                    ? p.getCreatedAt().toString().substring(0, 16).replace("T", " ") : "-";

            HBox row = new HBox(0);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:white; -fx-border-color:transparent transparent #f0f0f0 transparent; -fx-border-width:0 0 1 0;");
            row.setPadding(new Insets(12, 16, 12, 16));

            row.getChildren().addAll(
                cell(String.valueOf(p.getId()), 50),
                cell(p.getTitre() != null ? p.getTitre() : "-", 200),
                cell(auteur, 150),
                cell(String.valueOf(p.getCommunauteId()), 100),
                cell(date, 150),
                buildActions(p, row)
            );
            tableContainer.getChildren().add(row);
        }
    }

    private HBox buildActions(Post p, HBox row) {
        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle("-fx-background-color:#fff0f0; -fx-text-fill:#e94560; -fx-font-size:11; " +
                        "-fx-padding:5 12 5 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnDel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer ce post ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    servicePost.supprimer(p);
                    load();
                }
            });
        });
        HBox box = new HBox(btnDel);
        box.setPrefWidth(140);
        return box;
    }

    private HBox buildRow(String c1, String c2, String c3, String c4, String c5, String c6, boolean isHeader) {
        HBox row = new HBox(0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        String bg = isHeader ? "-fx-background-color:#f8f8f8; -fx-border-color:transparent transparent #e0e0e0 transparent; -fx-border-width:0 0 1 0;"
                             : "-fx-background-color:white;";
        row.setStyle(bg);
        row.getChildren().addAll(cell(c1,50), cell(c2,200), cell(c3,150), cell(c4,100), cell(c5,150), cell(c6,140));
        return row;
    }

    private Label cell(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size:12; -fx-text-fill:#333;");
        return l;
    }

    private String getUserName(int userId) {
        var u = userService.trouver(userId);
        return u != null ? u.getPrenom() + " " + u.getNom() : "#" + userId;
    }
}
