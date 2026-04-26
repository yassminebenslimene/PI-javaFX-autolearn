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

    @FXML private Label     labelNom;
    @FXML private Label     labelDescription;
    @FXML private Label     labelAvatar;
    @FXML private Label     labelAvatarPost;
    @FXML private Label     labelMembresCount;
    @FXML private Label     labelSidebarMembres;
    @FXML private Label     statPosts;
    @FXML private Label     statMembres;
    @FXML private Button    btnGererMembres;
    @FXML private TextField fieldTitre;
    @FXML private TextArea  fieldContenu;
    @FXML private VBox      postsPane;

    private final ServicePost        servicePost        = new ServicePost();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private final ServiceCommunaute  serviceCommunaute  = new ServiceCommunaute();
    private final UserService        userService        = new UserService();

    private Communaute communaute;
    private Runnable   onRetour;
    private Label      emptyLabel;

    public void setCommunaute(Communaute c, Runnable retour) {
        this.communaute = c;
        this.onRetour   = retour;
        labelNom.setText(c.getNom());
        labelDescription.setText(c.getDescription() != null ? c.getDescription() : "");

        // Avatar communauté (initiale)
        String initCom = c.getNom() != null && !c.getNom().isEmpty()
                ? String.valueOf(c.getNom().charAt(0)).toUpperCase() : "C";
        if (labelAvatar != null) labelAvatar.setText(initCom);

        // Avatar post (initiale utilisateur connecté)
        User currentUser = SessionManager.getCurrentUser();
        if (labelAvatarPost != null && currentUser != null) {
            String initUser = currentUser.getPrenom().substring(0,1).toUpperCase()
                            + currentUser.getNom().substring(0,1).toUpperCase();
            labelAvatarPost.setText(initUser);
        }

        // Compteur membres
        int nbMembres = c.getMemberIds() != null ? c.getMemberIds().size() : 0;
        if (labelMembresCount != null)
            labelMembresCount.setText("👥  " + nbMembres + " membre" + (nbMembres > 1 ? "s" : ""));
        if (labelSidebarMembres != null)
            labelSidebarMembres.setText(nbMembres + " membre" + (nbMembres > 1 ? "s" : ""));
        if (statMembres != null)
            statMembres.setText(String.valueOf(nbMembres));

        int currentUserId = currentUser != null ? currentUser.getId() : -1;
        if (currentUserId == c.getOwnerId()) {
            btnGererMembres.setVisible(true);
            btnGererMembres.setManaged(true);
        }
        loadPosts();
    }

    private void loadPosts() {
        postsPane.getChildren().clear();
        emptyLabel = null;
        List<Post> posts = servicePost.getByCommunaute(communaute.getId());
        if (statPosts != null) statPosts.setText(String.valueOf(posts.size()));
        if (posts.isEmpty()) {
            emptyLabel = new Label("✨  Aucun post pour l'instant. Soyez le premier à publier !");
            emptyLabel.setStyle("-fx-text-fill:rgba(255,255,255,0.35); -fx-font-size:13; -fx-padding:16 0 0 0;");
            postsPane.getChildren().add(emptyLabel);
        } else {
            for (Post p : posts) postsPane.getChildren().add(buildPostCard(p));
        }
    }

    @FXML
    public void onPublier() {
        String titre   = fieldTitre.getText().trim();
        String contenu = fieldContenu.getText().trim();

        // Validation
        if (contenu.isEmpty()) {
            showFieldError(fieldContenu, "Le contenu est obligatoire.");
            return;
        }
        if (contenu.length() < 10) {
            showFieldError(fieldContenu, "Le contenu doit contenir au moins 10 caractères.");
            return;
        }
        if (contenu.length() > 2000) {
            showFieldError(fieldContenu, "Le contenu ne peut pas dépasser 2000 caractères.");
            return;
        }
        if (!titre.isEmpty() && titre.length() > 100) {
            showFieldError(fieldTitre, "Le titre ne peut pas dépasser 100 caractères.");
            return;
        }

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
        clearFieldError(fieldTitre);
        clearFieldError(fieldContenu);
    }

    private void showFieldError(javafx.scene.control.Control field, String msg) {
        field.setStyle(field.getStyle().replace("-fx-border-color:#ddd;", "")
                + "-fx-border-color:#e94560; -fx-border-width:1.5; -fx-border-radius:8;");
        field.setTooltip(new Tooltip(msg));
        // Show an inline alert
        Alert alert = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Saisie invalide");
        alert.showAndWait();
    }

    private void clearFieldError(javafx.scene.control.Control field) {
        field.setStyle("");
        field.setTooltip(null);
    }

    private VBox buildPostCard(Post p) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:#1a1a2e; -fx-background-radius:20; " +
                      "-fx-border-color:rgba(162,155,254,0.15); -fx-border-radius:20; -fx-border-width:1; " +
                      "-fx-effect:dropshadow(gaussian,rgba(108,92,231,0.12),18,0,0,4);");

        VBox body = new VBox(14);
        body.setStyle("-fx-padding:22 22 16 22;");

        String auteur  = getUserName(p.getUserId());
        String dateStr = tempsRelatif(p.getCreatedAt());

        // Avatar gradient
        String initiale = auteur.isEmpty() ? "?" : String.valueOf(auteur.charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setMinSize(46, 46);
        avatar.setMaxSize(46, 46);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#a29bfe,#6c5ce7); " +
                        "-fx-background-radius:50; -fx-text-fill:white; " +
                        "-fx-font-size:17; -fx-font-weight:900; " +
                        "-fx-effect:dropshadow(gaussian,rgba(108,92,231,0.5),8,0,0,0);");

        Label lblAuteur = new Label(auteur);
        lblAuteur.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:white;");
        Label lblDate = new Label("🕐  " + dateStr);
        lblDate.setStyle("-fx-font-size:11; -fx-text-fill:rgba(255,255,255,0.4);");
        VBox authorInfo = new VBox(3, lblAuteur, lblDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(14, avatar, authorInfo, spacer);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        if (p.getUserId() == currentUserId) {
            Button btnMenu = new Button("⋮");
            btnMenu.setStyle("-fx-background-color:rgba(255,255,255,0.07); -fx-text-fill:rgba(255,255,255,0.6); " +
                             "-fx-font-size:16; -fx-cursor:hand; -fx-border-width:0; " +
                             "-fx-padding:4 12 4 12; -fx-background-radius:50;");
            ContextMenu menu = new ContextMenu();
            menu.setStyle("-fx-background-color:#1a1a2e; -fx-border-color:rgba(162,155,254,0.2); -fx-border-width:1;");
            MenuItem itemModifier  = new MenuItem("✏  Modifier");
            MenuItem itemSupprimer = new MenuItem("🗑  Supprimer");
            menu.getItems().addAll(itemModifier, itemSupprimer);
            itemModifier.setOnAction(e -> onModifierPost(p, card, null, null));
            itemSupprimer.setOnAction(e -> onSupprimerPost(p, card));
            btnMenu.setOnAction(e -> menu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0));
            topRow.getChildren().add(btnMenu);
        }

        // Content
        VBox contentBox = new VBox(8);
        if (p.getTitre() != null && !p.getTitre().isEmpty()) {
            Label lblTitre = new Label(p.getTitre());
            lblTitre.setStyle("-fx-font-size:17; -fx-font-weight:900; -fx-text-fill:white; -fx-line-spacing:2;");
            lblTitre.setWrapText(true);
            contentBox.getChildren().add(lblTitre);
        }
        Label lblContenu = new Label(p.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13; -fx-text-fill:rgba(255,255,255,0.72); -fx-line-spacing:3;");
        contentBox.getChildren().add(lblContenu);

        body.getChildren().addAll(topRow, contentBox);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.06);");

        // Comments section
        VBox commentsSection = new VBox(0);
        commentsSection.setStyle("-fx-padding:0 22 0 22;");

        VBox commentsBox = new VBox(6);
        commentsBox.setStyle("-fx-padding:14 0 10 0;");
        List<Commentaire> comments = serviceCommentaire.getByPost(p.getId());
        for (Commentaire c : comments)
            commentsBox.getChildren().add(buildCommentRow(c));

        // Comment input bar
        HBox addComment = new HBox(10);
        addComment.setStyle("-fx-padding:8 0 18 0;");
        addComment.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String userInit = "?";
        if (SessionManager.getCurrentUser() != null) {
            User u = SessionManager.getCurrentUser();
            userInit = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
        }
        Label miniAvatar = new Label(userInit);
        miniAvatar.setMinSize(34, 34);
        miniAvatar.setMaxSize(34, 34);
        miniAvatar.setAlignment(javafx.geometry.Pos.CENTER);
        miniAvatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#a29bfe,#6c5ce7); " +
                            "-fx-background-radius:50; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:800;");

        TextField commentField = new TextField();
        commentField.setPromptText("Écrire un commentaire...");
        commentField.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:24; " +
                              "-fx-border-width:1; -fx-border-color:rgba(255,255,255,0.1); " +
                              "-fx-border-radius:24; -fx-padding:10 18 10 18; -fx-font-size:12; " +
                              "-fx-text-fill:white; -fx-prompt-text-fill:rgba(255,255,255,0.3);");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button btnSend = new Button("➤");
        btnSend.setStyle("-fx-background-color:linear-gradient(to right,#a29bfe,#6c5ce7); -fx-text-fill:white; " +
                         "-fx-font-size:13; -fx-padding:9 16 9 16; -fx-background-radius:50; " +
                         "-fx-cursor:hand; -fx-border-width:0; " +
                         "-fx-effect:dropshadow(gaussian,rgba(108,92,231,0.5),8,0,0,0);");

        btnSend.setOnAction(e -> {
            String txt = commentField.getText().trim();
            if (txt.isEmpty() || txt.length() < 2) {
                commentField.setStyle("-fx-background-color:rgba(233,69,96,0.15); -fx-background-radius:24; " +
                                      "-fx-border-width:1; -fx-border-color:rgba(233,69,96,0.5); " +
                                      "-fx-border-radius:24; -fx-padding:10 18 10 18; -fx-font-size:12; " +
                                      "-fx-text-fill:white; -fx-prompt-text-fill:rgba(255,255,255,0.3);");
                return;
            }
            commentField.setStyle("-fx-background-color:rgba(255,255,255,0.06); -fx-background-radius:24; " +
                                  "-fx-border-width:1; -fx-border-color:rgba(255,255,255,0.1); " +
                                  "-fx-border-radius:24; -fx-padding:10 18 10 18; -fx-font-size:12; " +
                                  "-fx-text-fill:white; -fx-prompt-text-fill:rgba(255,255,255,0.3);");
            int uid = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
            Commentaire newC = new Commentaire(txt, p.getId(), uid);
            serviceCommentaire.ajouter(newC);
            commentField.clear();
            commentsBox.getChildren().add(buildCommentRow(newC));
        });
        commentField.setOnAction(e -> btnSend.fire());

        addComment.getChildren().addAll(miniAvatar, commentField, btnSend);
        commentsSection.getChildren().addAll(commentsBox, addComment);

        card.getChildren().addAll(body, sep, commentsSection);
        return card;
    }

    private void onModifierPost(Post p, VBox card, Label lblTitre, Label lblContenu, HBox actionRow) {
        // Toggle visibility
        lblTitre.setVisible(false);
        lblTitre.setManaged(false);
        lblContenu.setVisible(false);
        lblContenu.setManaged(false);
        actionRow.setVisible(false);
        actionRow.setManaged(false);

        VBox editBox = new VBox(10);
        editBox.setStyle("-fx-padding: 5 0 10 0;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        TextField fTitre = new TextField(p.getTitre() != null ? p.getTitre() : "");
        fTitre.setPromptText("Titre (max 100 caractères)");
        TextArea  fContenu = new TextArea(p.getContenu());
        fContenu.setPromptText("Contenu * (10–2000 caractères)");
        fContenu.setPrefRowCount(4);
        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill:#e94560; -fx-font-size:11;");
        content.getChildren().addAll(new Label("Titre :"), fTitre, new Label("Contenu :"), fContenu, errLabel);
        dialog.getDialogPane().setContent(content);

        TextArea fContenu = new TextArea(p.getContenu());
        fContenu.setPromptText("Contenu...");
        fContenu.setPrefRowCount(3);
        fContenu.setWrapText(true);
        fContenu.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; " +
                          "-fx-border-radius: 10; -fx-padding: 8 12; -fx-font-size: 13;");

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11;");

        Button btnSave = new Button("Enregistrer");
        btnSave.setStyle("-fx-background-color: #7a6ad8; -fx-text-fill: white; -fx-font-weight: 800; " +
                         "-fx-padding: 8 20; -fx-background-radius: 10; -fx-cursor: hand;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: white; -fx-text-fill: #64748b; -fx-font-weight: 700; " +
                           "-fx-padding: 8 20; -fx-background-radius: 10; -fx-cursor: hand; -fx-border-color: #e2e8f0; -fx-border-radius: 10;");

        HBox btns = new HBox(8, btnCancel, btnSave);
        btns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        editBox.getChildren().addAll(new Label("Modifier le titre :") {{ setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;"); }},
                fTitre, new Label("Modifier le contenu :") {{ setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;"); }},
                fContenu, errLabel, btns);

        // Add editBox after lblContenu
        int idx = card.getChildren().indexOf(lblContenu);
        card.getChildren().add(idx + 1, editBox);

        btnCancel.setOnAction(e -> {
            card.getChildren().remove(editBox);
            lblTitre.setVisible(true);
            lblTitre.setManaged(true);
            lblContenu.setVisible(true);
            lblContenu.setManaged(true);
            actionRow.setVisible(true);
            actionRow.setManaged(true);
        });

        btnSave.setOnAction(e -> {
            String titre   = fTitre.getText().trim();
            String contenu = fContenu.getText().trim();

            if (contenu.length() < 10) {
                errLabel.setText("Le contenu doit contenir au moins 10 caractères.");
            } else if (contenu.length() > 2000) {
                errLabel.setText("Le contenu ne peut pas dépasser 2000 caractères.");
            } else if (!titre.isEmpty() && titre.length() > 100) {
                errLabel.setText("Le titre ne peut pas dépasser 100 caractères.");
            } else {
                p.setTitre(titre);
                p.setContenu(contenu);
                servicePost.modifier(p);
                // Rebuild the card in place
                int idx = postsPane.getChildren().indexOf(card);
                if (idx >= 0) postsPane.getChildren().set(idx, buildPostCard(p));
            }
        });
    }

    private void onSupprimerPost(Post p, VBox card) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce post ?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                servicePost.supprimer(p);
                postsPane.getChildren().remove(card);
            }
        });
    }

    // Style Facebook : avatar rond + bulle grise avec nom en gras
    private HBox buildCommentRow(Commentaire c) {
        String nom = getUserName(c.getUserId());

        String initiale = nom.isEmpty() ? "?" : String.valueOf(nom.charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#a29bfe,#6c5ce7); " +
                        "-fx-background-radius:50; -fx-text-fill:white; -fx-font-size:11; -fx-font-weight:800;");

        Label lblNom = new Label(nom);
        lblNom.setStyle("-fx-font-size:12; -fx-font-weight:800; -fx-text-fill:white;");

        Label lblContenu = new Label(c.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:12; -fx-text-fill:rgba(255,255,255,0.75);");

        VBox bubble = new VBox(3, lblNom, lblContenu);
        bubble.setStyle("-fx-background-color:rgba(255,255,255,0.07); -fx-background-radius:16; " +
                        "-fx-border-color:rgba(255,255,255,0.08); -fx-border-radius:16; -fx-border-width:1; " +
                        "-fx-padding:9 14 9 14;");
        HBox.setHgrow(bubble, Priority.ALWAYS);

        HBox row = new HBox(10, avatar, bubble);
        row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        row.setPadding(new Insets(3, 0, 3, 0));

        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        if (c.getUserId() == currentUserId) {
            Button btnMenu = new Button("⋮");
            btnMenu.setStyle("-fx-background-color:transparent; -fx-text-fill:rgba(255,255,255,0.4); " +
                             "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 6 0 6;");

            btnEdit.setOnAction(e -> onModifierComment(c, bubble, lblContenu));
            btnDel.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
                confirm.setHeaderText(null);
                confirm.showAndWait().ifPresent(btn -> {
                    if (btn == ButtonType.YES) {
                        serviceCommentaire.supprimer(c);
                        ((VBox) row.getParent()).getChildren().remove(row);
                    }
                });
            });
            
            HBox commActions = new HBox(2, btnEdit, btnDel);
            commActions.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            row.getChildren().add(commActions);
        }
        return row;
    }
    private void onModifierComment(Commentaire c, VBox bubble, Label lblContenu) {
        lblContenu.setVisible(false);
        lblContenu.setManaged(false);

        TextField editField = new TextField(c.getContenu());
        editField.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 6; -fx-font-size: 12;");
        
        Button btnSave = new Button("✔");
        btnSave.setStyle("-fx-background-color: transparent; -fx-text-fill: #10b981; -fx-font-weight: 800; -fx-cursor: hand; -fx-padding: 0 4;");
        
        Button btnCancel = new Button("✖");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: 800; -fx-cursor: hand; -fx-padding: 0 4;");
        
        HBox editRow = new HBox(4, editField, btnCancel, btnSave);
        editRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox.setHgrow(editField, Priority.ALWAYS);
        
        bubble.getChildren().add(editRow);

        btnCancel.setOnAction(e -> {
            bubble.getChildren().remove(editRow);
            lblContenu.setVisible(true);
            lblContenu.setManaged(true);
        });

        btnSave.setOnAction(e -> {
            String txt = editField.getText().trim();
            if (txt.length() < 2 || txt.length() > 500) {
                editField.setStyle("-fx-background-color: #fef2f2; -fx-border-color: #ef4444; -fx-border-radius: 10; -fx-padding: 6; -fx-font-size: 12;");
                return;
            }
            if (tn.esprit.tools.BadWordFilter.containsBadWord(txt)) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Votre modification contient des mots inappropriés.", ButtonType.OK);
                alert.setHeaderText("Contenu inapproprié");
                alert.showAndWait();
                return;
            }
            c.setContenu(txt);
            serviceCommentaire.modifier(c);
            lblContenu.setText(txt);
            bubble.getChildren().remove(editRow);
            lblContenu.setVisible(true);
            lblContenu.setManaged(true);
        });
    }

    // Garde la compatibilité pour les appels existants
    private Label buildCommentLabel(String text, int userId) {
        String nom = getUserName(userId);
        Label lbl = new Label("💬  " + nom + " : " + text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size:12; -fx-text-fill:#555; " +
                     "-fx-background-color:#f5f5f5; -fx-background-radius:8; -fx-padding:6 10 6 10;");
        return lbl;
    }

    private String tempsRelatif(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        long minutes = java.time.Duration.between(dt, java.time.LocalDateTime.now()).toMinutes();
        if (minutes < 1)   return "à l'instant";
        if (minutes < 60)  return "il y a " + minutes + " min";
        long heures = minutes / 60;
        if (heures < 24)   return "il y a " + heures + " heure" + (heures > 1 ? "s" : "");
        long jours = heures / 24;
        if (jours < 7)     return "il y a " + jours + " jour" + (jours > 1 ? "s" : "");
        long semaines = jours / 7;
        if (semaines < 4)  return "il y a " + semaines + " semaine" + (semaines > 1 ? "s" : "");
        long mois = jours / 30;
        if (mois < 12)     return "il y a " + mois + " mois";
        long ans = jours / 365;
        return "il y a " + ans + " an" + (ans > 1 ? "s" : "");
    }

    // Retourne "Prenom Nom" ou "Utilisateur #id" si introuvable
    private String getUserName(int userId) {
        User u = userService.trouver(userId);
        if (u != null) return u.getPrenom() + " " + u.getNom();
        return "Utilisateur #" + userId;
    }

    @FXML
    public void onGererMembres() {
        Communaute fresh = serviceCommunaute.getById(communaute.getId());
        if (fresh != null) communaute = fresh;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Gerer les membres — " + communaute.getNom());
        dialog.setMinWidth(500);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#f9f9f9;");

        Label lblMembres = new Label("Membres actuels");
        lblMembres.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1e1e1e;");
        VBox membresBox = new VBox(6);
        refreshMembresBox(membresBox, dialog);

        Label lblAjouter = new Label("Ajouter un etudiant");
        lblAjouter.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1e1e1e; -fx-padding:8 0 0 0;");
        ListView<User> listView = new ListView<>();
        listView.setPrefHeight(180);
        refreshStudentList(listView);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); setGraphic(null); return; }
                HBox row = new HBox(10);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                Label name  = new Label(u.getPrenom() + " " + u.getNom());
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
                setGraphic(row); setText(null);
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
        int ownerId = communaute.getOwnerId();
        for (User u : userService.afficher()) {
            if (u.getId() == ownerId) continue;
            if (!(u instanceof tn.esprit.entities.Etudiant)) continue;
            if (communaute.getMemberIds().contains(u.getId())) continue;
            listView.getItems().add(u);
        }
    }

    @FXML public void onRefresh() { loadPosts(); }

    @FXML
    public void onRetour() {
        if (onRetour != null) onRetour.run();
    }
}
