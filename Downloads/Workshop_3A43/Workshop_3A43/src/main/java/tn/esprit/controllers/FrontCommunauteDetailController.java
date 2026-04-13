package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Commentaire;
import tn.esprit.entities.Communaute;
import tn.esprit.entities.Post;
import tn.esprit.entities.User;
import tn.esprit.services.ServiceCommentaire;
import tn.esprit.services.ServiceCommunaute;
import tn.esprit.services.ServicePost;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

public class FrontCommunauteDetailController {

    @FXML private Label  labelNom;
    @FXML private Label  labelDescription;
    @FXML private Button btnGererMembres;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldContenu;
    @FXML private VBox postsPane;

    private final ServicePost        servicePost        = new ServicePost();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private final ServiceCommunaute  serviceCommunaute  = new ServiceCommunaute();
    private final UserService        userService        = new UserService();

    private Communaute communaute;
    private Runnable   onRetour;
    private Label      emptyLabel;

    // ── Initialisation ───────────────────────────────────────────────────────

    public void setCommunaute(Communaute c, Runnable retour) {
        this.communaute = c;
        this.onRetour   = retour;

        labelNom.setText(c.getNom());
        labelDescription.setText(c.getDescription() != null ? c.getDescription() : "");

        // Afficher le bouton "Gérer les membres" uniquement pour le owner
        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        if (currentUserId == c.getOwnerId()) {
            btnGererMembres.setVisible(true);
            btnGererMembres.setManaged(true);
        }

        loadPosts();
    }

    // ── Posts ────────────────────────────────────────────────────────────────

    private void loadPosts() {
        postsPane.getChildren().clear();
        emptyLabel = null;

        System.out.println("[DEBUG] loadPosts communauteId=" + communaute.getId());
        List<Post> posts = servicePost.getByCommunaute(communaute.getId());
        System.out.println("[DEBUG] posts trouvés: " + posts.size());
        if (posts.isEmpty()) {
            emptyLabel = new Label("Aucun post pour l'instant. Soyez le premier !");
            emptyLabel.setStyle("-fx-text-fill:#aaa; -fx-font-size:13; -fx-padding:8 0 0 0;");
            postsPane.getChildren().add(emptyLabel);
        } else {
            for (Post p : posts) postsPane.getChildren().add(buildPostCard(p));
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
        servicePost.ajouter(p);

        if (emptyLabel != null) {
            postsPane.getChildren().remove(emptyLabel);
            emptyLabel = null;
        }

        postsPane.getChildren().add(0, buildPostCard(p));
        fieldTitre.clear();
        fieldContenu.clear();
    }

    private VBox buildPostCard(Post p) {
        VBox card = new VBox(10);
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:12; " +
            "-fx-border-color:#eeeeee; -fx-border-radius:12; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2); -fx-padding:18;");

        Label lblTitre = new Label(p.getTitre() != null && !p.getTitre().isEmpty()
                ? p.getTitre() : "(sans titre)");
        lblTitre.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");

        Label lblContenu = new Label(p.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13; -fx-text-fill:#444;");

        String dateStr = p.getCreatedAt() != null
                ? p.getCreatedAt().toString().substring(0, 16).replace("T", " à ") : "";
        Label lblDate = new Label(dateStr);
        lblDate.setStyle("-fx-font-size:11; -fx-text-fill:#aaa;");

        VBox commentsBox = new VBox(6);
        commentsBox.setStyle("-fx-padding:8 0 0 0;");
        for (Commentaire c : serviceCommentaire.getByPost(p.getId()))
            commentsBox.getChildren().add(buildCommentLabel(c.getContenu()));

        HBox addComment = new HBox(8);
        TextField commentField = new TextField();
        commentField.setPromptText("Ajouter un commentaire...");
        commentField.setStyle("-fx-background-color:#f5f5f5; -fx-background-radius:8; " +
                              "-fx-border-width:0; -fx-padding:8 12 8 12; -fx-font-size:12;");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button btnComment = new Button("Envoyer");
        btnComment.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:12; " +
                            "-fx-padding:8 16 8 16; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
        btnComment.setOnAction(e -> {
            String txt = commentField.getText().trim();
            if (txt.isEmpty()) return;
            int uid = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
            serviceCommentaire.ajouter(new Commentaire(txt, p.getId(), uid));
            commentField.clear();
            commentsBox.getChildren().add(buildCommentLabel(txt));
        });

        addComment.getChildren().addAll(commentField, btnComment);
        card.getChildren().addAll(lblTitre, lblContenu, lblDate, commentsBox, addComment);
        return card;
    }

    private Label buildCommentLabel(String text) {
        Label lbl = new Label("💬  " + text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size:12; -fx-text-fill:#555; " +
                     "-fx-background-color:#f5f5f5; -fx-background-radius:8; -fx-padding:6 10 6 10;");
        return lbl;
    }

    // ── Gestion des membres (owner uniquement) ───────────────────────────────

    @FXML
    public void onGererMembres() {
        // Recharger la communauté pour avoir les membres à jour
        Communaute fresh = serviceCommunaute.getById(communaute.getId());
        if (fresh != null) communaute = fresh;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Gérer les membres — " + communaute.getNom());
        dialog.setMinWidth(500);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#f9f9f9;");

        // ── Membres actuels ──
        Label lblMembres = new Label("Membres actuels");
        lblMembres.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");

        VBox membresBox = new VBox(6);
        refreshMembresBox(membresBox, dialog);

        // ── Ajouter un étudiant ──
        Label lblAjouter = new Label("Ajouter un étudiant");
        lblAjouter.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1e1e1e; -fx-padding:8 0 0 0;");

        // Liste de tous les étudiants non encore membres
        ListView<User> listView = new ListView<>();
        listView.setPrefHeight(180);
        listView.setStyle("-fx-background-radius:10; -fx-border-radius:10;");
        refreshStudentList(listView);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label name = new Label(u.getPrenom() + " " + u.getNom());
                name.setStyle("-fx-font-size:13; -fx-text-fill:#1e1e1e;");
                Label email = new Label(u.getEmail());
                email.setStyle("-fx-font-size:11; -fx-text-fill:#888;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Button btnAdd = new Button("+ Ajouter");
                btnAdd.setStyle("-fx-background-color:#059669; -fx-text-fill:white; -fx-font-size:11; " +
                                "-fx-padding:5 12 5 12; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;");
                btnAdd.setOnAction(e -> {
                    serviceCommunaute.ajouterMembre(communaute.getId(), u.getId());
                    communaute.getMemberIds().add(u.getId());
                    refreshStudentList(listView);
                    refreshMembresBox(membresBox, dialog);
                });
                row.getChildren().addAll(new VBox(2, name, email), spacer, btnAdd);
                setGraphic(row);
                setText(null);
            }
        });

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:#7a6ad8; -fx-text-fill:white; -fx-font-size:13; " +
                           "-fx-padding:9 24 9 24; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");
        btnFermer.setOnAction(e -> dialog.close());

        root.getChildren().addAll(lblMembres, membresBox, lblAjouter, listView,
                new HBox(btnFermer) {{ setAlignment(javafx.geometry.Pos.CENTER_RIGHT); }});

        dialog.setScene(new Scene(new ScrollPane(root) {{
            setFitToWidth(true);
            setStyle("-fx-background-color:#f9f9f9; -fx-background:transparent; -fx-border-width:0;");
        }}, 520, 560));
        dialog.showAndWait();
    }

    private void refreshMembresBox(VBox membresBox, Stage dialog) {
        membresBox.getChildren().clear();
        if (communaute.getMemberIds().isEmpty()) {
            Label none = new Label("Aucun membre pour l'instant.");
            none.setStyle("-fx-text-fill:#aaa; -fx-font-size:12;");
            membresBox.getChildren().add(none);
            return;
        }
        for (int uid : communaute.getMemberIds()) {
            User u = userService.trouver(uid);
            if (u == null) continue;
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:white; -fx-background-radius:8; " +
                         "-fx-border-color:#eeeeee; -fx-border-radius:8; -fx-padding:8 12 8 12;");
            Label name = new Label(u.getPrenom() + " " + u.getNom());
            name.setStyle("-fx-font-size:13; -fx-text-fill:#1e1e1e;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button btnRetirer = new Button("Retirer");
            btnRetirer.setStyle("-fx-background-color:rgba(233,69,96,0.1); -fx-text-fill:#e94560; " +
                                "-fx-font-size:11; -fx-padding:4 10 4 10; -fx-background-radius:8; " +
                                "-fx-cursor:hand; -fx-border-width:0;");
            btnRetirer.setOnAction(e -> {
                serviceCommunaute.retirerMembre(communaute.getId(), uid);
                communaute.getMemberIds().remove(Integer.valueOf(uid));
                refreshMembresBox(membresBox, dialog);
            });
            row.getChildren().addAll(name, spacer, btnRetirer);
            membresBox.getChildren().add(row);
        }
    }

    private void refreshStudentList(ListView<User> listView) {
        listView.getItems().clear();
        List<User> tous = userService.afficher();
        int ownerId = communaute.getOwnerId();
        for (User u : tous) {
            // Exclure le owner, les admins et les membres déjà ajoutés
            if (u.getId() == ownerId) continue;
            if (!(u instanceof tn.esprit.entities.Etudiant)) continue;
            if (communaute.getMemberIds().contains(u.getId())) continue;
            listView.getItems().add(u);
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    @FXML
    public void onRefresh() {
        loadPosts();
    }

    @FXML
    public void onRetour() {
        if (onRetour != null) onRetour.run();
    }
}
