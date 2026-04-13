package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Commentaire;
import tn.esprit.entities.Communaute;
import tn.esprit.entities.Post;
import tn.esprit.services.ServiceCommentaire;
import tn.esprit.services.ServicePost;
import tn.esprit.session.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class FrontCommunauteDetailController {

    @FXML private Label labelNom;
    @FXML private Label labelDescription;
    @FXML private TextField fieldTitre;
    @FXML private TextArea fieldContenu;
    @FXML private VBox postsPane;

    private final ServicePost servicePost = new ServicePost();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

    private Communaute communaute;
    private Runnable onRetour;

    // Label "aucun post" — on le garde en référence pour le retirer au besoin
    private Label emptyLabel;

    public void setCommunaute(Communaute c, Runnable retour) {
        this.communaute = c;
        this.onRetour = retour;
        labelNom.setText(c.getNom());
        labelDescription.setText(c.getDescription() != null ? c.getDescription() : "");
        loadPosts();
    }

    private void loadPosts() {
        postsPane.getChildren().clear();
        emptyLabel = null;

        List<Post> posts = servicePost.getByCommunaute(communaute.getId());
        if (posts.isEmpty()) {
            emptyLabel = new Label("Aucun post pour l'instant. Soyez le premier !");
            emptyLabel.setStyle("-fx-text-fill:#aaa; -fx-font-size:13; -fx-padding:8 0 0 0;");
            postsPane.getChildren().add(emptyLabel);
        } else {
            for (Post p : posts) {
                postsPane.getChildren().add(buildPostCard(p));
            }
        }
    }

    @FXML
    public void onPublier() {
        String titre   = fieldTitre.getText().trim();
        String contenu = fieldContenu.getText().trim();
        if (contenu.isEmpty()) return;

        int userId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : 0;

        Post p = new Post(contenu, titre, communaute.getId(), userId);
        p.setCreatedAt(LocalDateTime.now());
        servicePost.ajouter(p);  // insère en DB et récupère l'id généré

        // Retirer le label "aucun post" s'il est affiché
        if (emptyLabel != null) {
            postsPane.getChildren().remove(emptyLabel);
            emptyLabel = null;
        }

        // Ajouter la card du nouveau post directement en haut de la liste
        VBox card = buildPostCard(p);
        postsPane.getChildren().add(0, card);

        // Vider les champs
        fieldTitre.clear();
        fieldContenu.clear();
    }

    private VBox buildPostCard(Post p) {
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:12; " +
            "-fx-border-color:#eeeeee; -fx-border-radius:12; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2); " +
            "-fx-padding:18;");

        // Titre
        Label lblTitre = new Label(p.getTitre() != null && !p.getTitre().isEmpty()
                ? p.getTitre() : "(sans titre)");
        lblTitre.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");

        // Contenu
        Label lblContenu = new Label(p.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13; -fx-text-fill:#444;");

        // Date
        String dateStr = p.getCreatedAt() != null
                ? p.getCreatedAt().toString().substring(0, 16).replace("T", " à ")
                : "";
        Label lblDate = new Label(dateStr);
        lblDate.setStyle("-fx-font-size:11; -fx-text-fill:#aaa;");

        // Zone commentaires
        VBox commentsBox = new VBox(6);
        commentsBox.setStyle("-fx-padding:8 0 0 0;");

        List<Commentaire> comments = serviceCommentaire.getByPost(p.getId());
        for (Commentaire c : comments) {
            commentsBox.getChildren().add(buildCommentLabel(c.getContenu()));
        }

        // Champ + bouton commentaire
        HBox addComment = new HBox(8);
        TextField commentField = new TextField();
        commentField.setPromptText("Ajouter un commentaire...");
        commentField.setStyle(
            "-fx-background-color:#f5f5f5; -fx-background-radius:8; " +
            "-fx-border-width:0; -fx-padding:8 12 8 12; -fx-font-size:12;");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button btnComment = new Button("Envoyer");
        btnComment.setStyle(
            "-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12; " +
            "-fx-padding:8 16 8 16; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");

        btnComment.setOnAction(e -> {
            String txt = commentField.getText().trim();
            if (txt.isEmpty()) return;
            int userId = SessionManager.getCurrentUser() != null
                    ? SessionManager.getCurrentUser().getId() : 0;
            Commentaire c = new Commentaire(txt, p.getId(), userId);
            serviceCommentaire.ajouter(c);
            commentField.clear();
            // Afficher immédiatement le commentaire
            commentsBox.getChildren().add(buildCommentLabel(txt));
        });

        addComment.getChildren().addAll(commentField, btnComment);
        card.getChildren().addAll(lblTitre, lblContenu, lblDate, commentsBox, addComment);
        return card;
    }

    private Label buildCommentLabel(String text) {
        Label lbl = new Label("💬  " + text);
        lbl.setWrapText(true);
        lbl.setStyle(
            "-fx-font-size:12; -fx-text-fill:#555; " +
            "-fx-background-color:#f5f5f5; -fx-background-radius:8; " +
            "-fx-padding:6 10 6 10;");
        return lbl;
    }

    @FXML
    public void onRetour() {
        if (onRetour != null) onRetour.run();
    }
}
