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
        card.setStyle("-fx-background-color:white; -fx-background-radius:24; " +
                      "-fx-border-color:#ede9fe; -fx-border-radius:24; -fx-border-width:1.5; " +
                      "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.1),24,0,0,8);");

        VBox body = new VBox(18);
        body.setStyle("-fx-padding:26 26 20 26;");

        String auteur  = getUserName(p.getUserId());
        String dateStr = tempsRelatif(p.getCreatedAt());

        String initiale = auteur.isEmpty() ? "?" : String.valueOf(auteur.charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setMinSize(50, 50);
        avatar.setMaxSize(50, 50);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5); " +
                        "-fx-background-radius:50; -fx-text-fill:white; " +
                        "-fx-font-size:18; -fx-font-weight:900; " +
                        "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.4),12,0,0,0);");

        Label lblAuteur = new Label(auteur);
        lblAuteur.setStyle("-fx-font-size:15; -fx-font-weight:900; -fx-text-fill:#1e1b4b;");
        Label lblDate = new Label("🕐  " + dateStr);
        lblDate.setStyle("-fx-font-size:11; -fx-text-fill:#c4b5fd; -fx-font-weight:600;");
        VBox authorInfo = new VBox(3, lblAuteur, lblDate);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(14, avatar, authorInfo, spacer);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        if (p.getUserId() == currentUserId) {
            Button btnMenu = new Button("⋯");
            btnMenu.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed; -fx-font-size:16; " +
                             "-fx-cursor:hand; -fx-border-width:0; -fx-padding:6 14 6 14; -fx-background-radius:30;");
            ContextMenu menu = new ContextMenu();
            MenuItem itemModifier  = new MenuItem("✏  Modifier");
            MenuItem itemSupprimer = new MenuItem("🗑  Supprimer");
            menu.getItems().addAll(itemModifier, itemSupprimer);
            itemModifier.setOnAction(e -> onModifierPost(p, card, null, null, null));
            itemSupprimer.setOnAction(e -> onSupprimerPost(p, card));
            btnMenu.setOnAction(e -> menu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0));
            topRow.getChildren().add(btnMenu);
        }

        VBox contentBox = new VBox(10);
        if (p.getTitre() != null && !p.getTitre().isEmpty()) {
            Label lblTitre = new Label(p.getTitre());
            lblTitre.setStyle("-fx-font-size:19; -fx-font-weight:900; -fx-text-fill:#1e1b4b; -fx-line-spacing:2;");
            lblTitre.setWrapText(true);
            contentBox.getChildren().add(lblTitre);
        }
        Label lblContenu = new Label(p.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13; -fx-text-fill:#4b5563; -fx-line-spacing:5;");
        contentBox.getChildren().add(lblContenu);

        body.getChildren().addAll(topRow, contentBox);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#f3f0ff;");

        VBox commentsSection = new VBox(0);
        commentsSection.setStyle("-fx-padding:0 26 0 26;");

        VBox commentsBox = new VBox(10);
        commentsBox.setStyle("-fx-padding:18 0 12 0;");
        List<Commentaire> comments = serviceCommentaire.getByPost(p.getId());
        for (Commentaire c : comments)
            commentsBox.getChildren().add(buildCommentRow(c));

        // Comment input
        HBox addComment = new HBox(12);
        addComment.setStyle("-fx-padding:8 0 22 0;");
        addComment.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        String userInit = "?";
        if (SessionManager.getCurrentUser() != null) {
            User u = SessionManager.getCurrentUser();
            userInit = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
        }
        Label miniAvatar = new Label(userInit);
        miniAvatar.setMinSize(38, 38);
        miniAvatar.setMaxSize(38, 38);
        miniAvatar.setAlignment(javafx.geometry.Pos.CENTER);
        miniAvatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5); " +
                            "-fx-background-radius:50; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:800;");

        TextField commentField = new TextField();
        commentField.setPromptText("Écrire un commentaire...");
        commentField.setStyle("-fx-background-color:#faf8ff; -fx-background-radius:30; " +
                              "-fx-border-width:1.5; -fx-border-color:#e9e4ff; " +
                              "-fx-border-radius:30; -fx-padding:12 20 12 20; -fx-font-size:12; " +
                              "-fx-text-fill:#1e1b4b;");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button btnSend = new Button("➤");
        btnSend.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white; " +
                         "-fx-font-size:13; -fx-padding:11 20 11 20; -fx-background-radius:50; " +
                         "-fx-cursor:hand; -fx-border-width:0; " +
                         "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.45),12,0,0,3);");

        btnSend.setOnAction(e -> {
            String txt = commentField.getText().trim();
            if (txt.isEmpty() || txt.length() < 2) {
                commentField.setStyle("-fx-background-color:#fff1f2; -fx-background-radius:30; " +
                                      "-fx-border-width:1.5; -fx-border-color:#fca5a5; " +
                                      "-fx-border-radius:30; -fx-padding:12 20 12 20; -fx-font-size:12;");
                return;
            }
            commentField.setStyle("-fx-background-color:#faf8ff; -fx-background-radius:30; " +
                                  "-fx-border-width:1.5; -fx-border-color:#e9e4ff; " +
                                  "-fx-border-radius:30; -fx-padding:12 20 12 20; -fx-font-size:12; " +
                                  "-fx-text-fill:#1e1b4b;");
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
        // Toggle visibility of original elements
        if (lblTitre != null) {
            lblTitre.setVisible(false);
            lblTitre.setManaged(false);
        }
        if (lblContenu != null) {
            lblContenu.setVisible(false);
            lblContenu.setManaged(false);
        }
        if (actionRow != null) {
            actionRow.setVisible(false);
            actionRow.setManaged(false);
        }

        // Create edit fields
        TextField fTitre = new TextField(p.getTitre() != null ? p.getTitre() : "");
        fTitre.setPromptText("Titre (max 100 caractères)");
        fTitre.setStyle("-fx-background-color:#faf9ff; -fx-background-radius:10; -fx-border-color:#ede9fe; " +
                        "-fx-border-radius:10; -fx-padding:10 14; -fx-font-size:13; -fx-text-fill:#1e1b4b;");

        TextArea fContenu = new TextArea(p.getContenu());
        fContenu.setPromptText("Contenu...");
        fContenu.setPrefRowCount(3);
        fContenu.setWrapText(true);
        fContenu.setStyle("-fx-background-color:#faf9ff; -fx-background-radius:10; -fx-border-color:#ede9fe; " +
                          "-fx-border-radius:10; -fx-padding:10 14; -fx-font-size:13; -fx-text-fill:#1e1b4b;");

        Label errLabel = new Label();
        errLabel.setStyle("-fx-text-fill:#e94560; -fx-font-size:11;");

        Button btnSave = new Button("Enregistrer");
        btnSave.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#6d28d9); -fx-text-fill:white; -fx-font-weight:800; " +
                         "-fx-padding:9 22; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed; -fx-font-weight:700; " +
                           "-fx-padding:9 22; -fx-background-radius:10; -fx-cursor:hand; -fx-border-width:0;");

        HBox btns = new HBox(8, btnCancel, btnSave);
        btns.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox editBox = new VBox(10);
        editBox.getChildren().addAll(new Label("Modifier le titre :") {{ setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;"); }},
                fTitre, new Label("Modifier le contenu :") {{ setStyle("-fx-font-size: 11; -fx-text-fill: #94a3b8;"); }},
                fContenu, errLabel, btns);

        // Add editBox after lblContenu
        if (lblContenu != null) {
            int idx = card.getChildren().indexOf(lblContenu);
            card.getChildren().add(idx + 1, editBox);
        } else {
            card.getChildren().add(editBox);
        }

        btnCancel.setOnAction(e -> {
            card.getChildren().remove(editBox);
            if (lblTitre != null) {
                lblTitre.setVisible(true);
                lblTitre.setManaged(true);
            }
            if (lblContenu != null) {
                lblContenu.setVisible(true);
                lblContenu.setManaged(true);
            }
            if (actionRow != null) {
                actionRow.setVisible(true);
                actionRow.setManaged(true);
            }
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

    // Style light premium : avatar rond + bulle avec nom en gras
    private HBox buildCommentRow(Commentaire c) {
        String nom = getUserName(c.getUserId());

        String initiale = nom.isEmpty() ? "?" : String.valueOf(nom.charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5); " +
                        "-fx-background-radius:50; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:800;");

        Label lblNom = new Label(nom);
        lblNom.setStyle("-fx-font-size:12; -fx-font-weight:900; -fx-text-fill:#1e1b4b;");

        Label lblContenu = new Label(c.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:12; -fx-text-fill:#4b5563; -fx-line-spacing:3;");

        VBox bubble = new VBox(5, lblNom, lblContenu);
        bubble.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:18; " +
                        "-fx-border-color:#ede9fe; -fx-border-radius:18; -fx-border-width:1; " +
                        "-fx-padding:11 16 11 16;");
        HBox.setHgrow(bubble, Priority.ALWAYS);

        HBox row = new HBox(12, avatar, bubble);
        row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        if (c.getUserId() == currentUserId) {
            Button btnEdit = new Button("✏");
            btnEdit.setStyle("-fx-background-color:transparent; -fx-text-fill:#7c3aed; -fx-font-size:13; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 6 0 6;");
            btnEdit.setOnAction(e -> onModifierComment(c, bubble, lblContenu));

            Button btnDel = new Button("🗑");
            btnDel.setStyle("-fx-background-color:transparent; -fx-text-fill:#e94560; -fx-font-size:13; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 6 0 6;");
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
        editField.setStyle("-fx-background-color:#faf9ff; -fx-background-radius:10; -fx-border-color:#ede9fe; " +
                           "-fx-border-radius:10; -fx-padding:8; -fx-font-size:12; -fx-text-fill:#1e1b4b;");
        
        Button btnSave = new Button("✔");
        btnSave.setStyle("-fx-background-color:transparent; -fx-text-fill:#059669; -fx-font-weight:800; -fx-cursor:hand; -fx-padding:0 4;");
        
        Button btnCancel = new Button("✖");
        btnCancel.setStyle("-fx-background-color:transparent; -fx-text-fill:#e94560; -fx-font-weight:800; -fx-cursor:hand; -fx-padding:0 4;");
        
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
                editField.setStyle("-fx-background-color:#fff1f2; -fx-border-color:#fca5a5; -fx-border-radius:10; -fx-padding:8; -fx-font-size:12; -fx-text-fill:#1e1b4b;");
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
        dialog.setTitle("Membres — " + communaute.getNom());
        dialog.setMinWidth(560);
        dialog.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#ffffff;");

        // ── HEADER ──────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color:#ffffff;" +
                        "-fx-padding:28 32 22 32;" +
                        "-fx-border-color:transparent transparent #f0eeff transparent;" +
                        "-fx-border-width:0 0 1.5 0;");

        Label iconBadge = new Label("👥");
        iconBadge.setStyle("-fx-background-color:#f5f3ff; -fx-background-radius:12;" +
                           "-fx-padding:10 12; -fx-font-size:18;");

        VBox titleBox = new VBox(3);
        Label titleLbl = new Label("Gérer les membres");
        titleLbl.setStyle("-fx-font-size:18; -fx-font-weight:900; -fx-text-fill:#1e1b4b;");
        Label subLbl = new Label(communaute.getNom());
        subLbl.setStyle("-fx-font-size:12; -fx-text-fill:#a78bfa; -fx-font-weight:600;");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        int nbM = communaute.getMemberIds() != null ? communaute.getMemberIds().size() : 0;
        Label countBadge = new Label(nbM + " membre" + (nbM > 1 ? "s" : ""));
        countBadge.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                            "-fx-font-size:11; -fx-font-weight:800;" +
                            "-fx-padding:5 14; -fx-background-radius:20;");

        header.getChildren().addAll(iconBadge, titleBox, hSpacer, countBadge);

        // ── BODY ────────────────────────────────────
        VBox body = new VBox(26);
        body.setStyle("-fx-padding:26 32 30 32; -fx-background-color:#ffffff;");

        // Section membres actuels
        VBox membresSection = new VBox(12);
        HBox secH1 = new HBox(8);
        secH1.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dot1 = new Label();
        dot1.setMinSize(6,6); dot1.setMaxSize(6,6);
        dot1.setStyle("-fx-background-color:#7c3aed; -fx-background-radius:50;");
        Label lbl1 = new Label("Membres actuels");
        lbl1.setStyle("-fx-font-size:13; -fx-font-weight:800; -fx-text-fill:#374151;");
        secH1.getChildren().addAll(dot1, lbl1);
        VBox membresBox = new VBox(8);
        refreshMembresBox(membresBox, dialog);
        membresSection.getChildren().addAll(secH1, membresBox);

        Separator divider = new Separator();
        divider.setStyle("-fx-background-color:#f0eeff; -fx-opacity:1;");

        // Section ajouter
        VBox ajouterSection = new VBox(12);
        HBox secH2 = new HBox(8);
        secH2.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label dot2 = new Label();
        dot2.setMinSize(6,6); dot2.setMaxSize(6,6);
        dot2.setStyle("-fx-background-color:#7c3aed; -fx-background-radius:50;");
        Label lbl2 = new Label("Ajouter un étudiant");
        lbl2.setStyle("-fx-font-size:13; -fx-font-weight:800; -fx-text-fill:#374151;");
        secH2.getChildren().addAll(dot2, lbl2);

        ListView<User> listView = new ListView<>();
        listView.setPrefHeight(210);
        listView.setStyle("-fx-background-color:#fafafa; -fx-background-radius:14;" +
                          "-fx-border-color:#ede9fe; -fx-border-radius:14; -fx-border-width:1.5;");
        refreshStudentList(listView);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); setGraphic(null); return; }

                String init = u.getPrenom().substring(0,1).toUpperCase()
                            + u.getNom().substring(0,1).toUpperCase();
                Label av = new Label(init);
                av.setMinSize(36,36); av.setMaxSize(36,36);
                av.setAlignment(javafx.geometry.Pos.CENTER);
                av.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
                            "-fx-background-radius:50; -fx-text-fill:white; -fx-font-size:12; -fx-font-weight:800;");

                Label name  = new Label(u.getPrenom() + " " + u.getNom());
                name.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1b4b;");
                Label email = new Label(u.getEmail());
                email.setStyle("-fx-font-size:11; -fx-text-fill:#a78bfa;");
                VBox info = new VBox(2, name, email);
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

                Button btnAdd = new Button("+ Ajouter");
                btnAdd.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                                "-fx-font-size:11; -fx-font-weight:800;" +
                                "-fx-padding:7 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
                btnAdd.setOnMouseEntered(e2 -> btnAdd.setStyle(
                        "-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white;" +
                        "-fx-font-size:11; -fx-font-weight:800; -fx-padding:7 18; -fx-background-radius:20;" +
                        "-fx-cursor:hand; -fx-border-width:0;" +
                        "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.3),8,0,0,2);"));
                btnAdd.setOnMouseExited(e2 -> btnAdd.setStyle(
                        "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                        "-fx-font-size:11; -fx-font-weight:800;" +
                        "-fx-padding:7 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;"));
                btnAdd.setOnAction(e -> {
                    serviceCommunaute.ajouterMembre(communaute.getId(), u.getId());
                    communaute.getMemberIds().add(u.getId());
                    refreshStudentList(listView);
                    refreshMembresBox(membresBox, dialog);
                    int nb = communaute.getMemberIds().size();
                    countBadge.setText(nb + " membre" + (nb > 1 ? "s" : ""));
                });

                HBox row = new HBox(12, av, info, sp, btnAdd);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setStyle("-fx-padding:10 14;");
                setGraphic(row); setText(null);
                setStyle("-fx-background-color:transparent;");
            }
        });

        ajouterSection.getChildren().addAll(secH2, listView);
        body.getChildren().addAll(membresSection, divider, ajouterSection);

        // ── FOOTER ──────────────────────────────────
        HBox footer = new HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setStyle("-fx-padding:18 32 26 32;" +
                        "-fx-border-color:#f0eeff transparent transparent transparent;" +
                        "-fx-border-width:1.5 0 0 0; -fx-background-color:#ffffff;");

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color:#1e1b4b; -fx-text-fill:white;" +
                           "-fx-font-size:13; -fx-font-weight:800;" +
                           "-fx-padding:12 36; -fx-background-radius:30; -fx-cursor:hand; -fx-border-width:0;" +
                           "-fx-effect:dropshadow(gaussian,rgba(30,27,75,0.22),12,0,0,4);");
        btnFermer.setOnMouseEntered(e -> btnFermer.setStyle(
                "-fx-background-color:#312e81; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:800;" +
                "-fx-padding:12 36; -fx-background-radius:30; -fx-cursor:hand; -fx-border-width:0;" +
                "-fx-effect:dropshadow(gaussian,rgba(30,27,75,0.32),14,0,0,5);"));
        btnFermer.setOnMouseExited(e -> btnFermer.setStyle(
                "-fx-background-color:#1e1b4b; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:800;" +
                "-fx-padding:12 36; -fx-background-radius:30; -fx-cursor:hand; -fx-border-width:0;" +
                "-fx-effect:dropshadow(gaussian,rgba(30,27,75,0.22),12,0,0,4);"));
        btnFermer.setOnAction(e -> dialog.close());
        footer.getChildren().add(btnFermer);

        root.getChildren().addAll(header, body, footer);

        ScrollPane sp2 = new ScrollPane(root);
        sp2.setFitToWidth(true);
        sp2.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp2.setStyle("-fx-background-color:white; -fx-background:white; -fx-border-width:0;");

        dialog.setScene(new Scene(sp2, 560, 640));
        dialog.showAndWait();
    }

    private void refreshMembresBox(VBox membresBox, Stage dialog) {
        membresBox.getChildren().clear();
        if (communaute.getMemberIds().isEmpty()) {
            Label none = new Label("Aucun membre pour l'instant.");
            none.setStyle("-fx-text-fill:#c4b5fd; -fx-font-size:12; -fx-padding:8 0 4 0;");
            membresBox.getChildren().add(none);
            return;
        }
        for (int uid : communaute.getMemberIds()) {
            User u = userService.trouver(uid);
            if (u == null) continue;

            // Avatar initiales
            String init = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
            Label avatar = new Label(init);
            avatar.setMinSize(40, 40); avatar.setMaxSize(40, 40);
            avatar.setAlignment(javafx.geometry.Pos.CENTER);
            avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#7c3aed,#4f46e5);" +
                            "-fx-background-radius:50; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:800;");

            Label name = new Label(u.getPrenom() + " " + u.getNom());
            name.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#1e1b4b;");
            Label emailLbl = new Label(u.getEmail());
            emailLbl.setStyle("-fx-font-size:11; -fx-text-fill:#a78bfa;");
            VBox info = new VBox(2, name, emailLbl);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnRetirer = new Button("Retirer");
            btnRetirer.setStyle("-fx-background-color:#fff1f2; -fx-text-fill:#e94560;" +
                                "-fx-font-size:11; -fx-font-weight:700; -fx-padding:7 16;" +
                                "-fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
            btnRetirer.setOnMouseEntered(e -> btnRetirer.setStyle(
                    "-fx-background-color:#e94560; -fx-text-fill:white;" +
                    "-fx-font-size:11; -fx-font-weight:700; -fx-padding:7 16;" +
                    "-fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;"));
            btnRetirer.setOnMouseExited(e -> btnRetirer.setStyle(
                    "-fx-background-color:#fff1f2; -fx-text-fill:#e94560;" +
                    "-fx-font-size:11; -fx-font-weight:700; -fx-padding:7 16;" +
                    "-fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;"));
            btnRetirer.setOnAction(e -> {
                serviceCommunaute.retirerMembre(communaute.getId(), uid);
                communaute.getMemberIds().remove(Integer.valueOf(uid));
                refreshMembresBox(membresBox, dialog);
            });

            HBox row = new HBox(14, avatar, info, spacer, btnRetirer);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:white; -fx-background-radius:16;" +
                         "-fx-border-color:#f0eeff; -fx-border-radius:16; -fx-border-width:1.5;" +
                         "-fx-padding:12 16;" +
                         "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.06),8,0,0,2);");
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
