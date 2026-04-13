package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Commentaire;
import tn.esprit.services.ServiceCommentaire;
import tn.esprit.services.UserService;

import java.util.List;

public class CommentaireAdminController {

    @FXML private VBox tableContainer;

    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        load();
    }

    private void load() {
        tableContainer.getChildren().clear();

        HBox header = buildRow("ID", "Contenu", "Auteur", "Post ID", "Date", "Actions", true);
        tableContainer.getChildren().add(header);

        List<Commentaire> list = serviceCommentaire.getAll();
        for (Commentaire c : list) {
            String auteur = getUserName(c.getUserId());
            String date   = c.getCreatedAt() != null
                    ? c.getCreatedAt().toString().substring(0, 16).replace("T", " ") : "-";

            HBox row = new HBox(0);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:white; -fx-border-color:transparent transparent #f0f0f0 transparent; -fx-border-width:0 0 1 0;");
            row.setPadding(new Insets(12, 16, 12, 16));

            row.getChildren().addAll(
                cell(String.valueOf(c.getId()), 50),
                cell(c.getContenu(), 250),
                cell(auteur, 150),
                cell(String.valueOf(c.getPostId()), 80),
                cell(date, 150),
                buildActions(c)
            );
            tableContainer.getChildren().add(row);
        }
    }

    private HBox buildActions(Commentaire c) {
        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle("-fx-background-color:#fff0f0; -fx-text-fill:#e94560; -fx-font-size:11; " +
                        "-fx-padding:5 12 5 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnDel.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    serviceCommentaire.supprimer(c);
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
        row.setStyle(isHeader
                ? "-fx-background-color:#f8f8f8; -fx-border-color:transparent transparent #e0e0e0 transparent; -fx-border-width:0 0 1 0;"
                : "-fx-background-color:white;");
        row.getChildren().addAll(cell(c1,50), cell(c2,250), cell(c3,150), cell(c4,80), cell(c5,150), cell(c6,140));
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
