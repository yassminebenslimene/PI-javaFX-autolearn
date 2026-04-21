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
        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
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
        if (posts.isEmpty()) {
            emptyLabel = new Label("Aucune publication pour le moment. Lancez la premiere discussion.");
            emptyLabel.setStyle("-fx-text-fill:#64748b; -fx-font-size:13; -fx-font-weight:600; " +
                    "-fx-background-color:white; -fx-background-radius:10; -fx-border-color:#dbe3ee; " +
                    "-fx-border-radius:10; -fx-padding:12 14 12 14;");
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
        VBox card = new VBox(11);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                      "-fx-border-color: #e2e8f0; -fx-border-radius: 20; -fx-border-width: 1; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5); -fx-padding: 20;");

        String auteur  = getUserName(p.getUserId());
        String dateStr = tempsRelatif(p.getCreatedAt());

        // Avatar avec initiale
        String initiale = auteur.isEmpty() ? "?" : String.valueOf(auteur.charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#2563eb,#7c3aed); " +
                        "-fx-background-radius:50; -fx-text-fill:white; " +
                        "-fx-font-size:15; -fx-font-weight:700;");

        // Nom + date empilés
        Label lblAuteur = new Label(auteur);
        lblAuteur.setStyle("-fx-font-size:13; -fx-font-weight:800; -fx-text-fill:#0f172a;");
        Label lblDate = new Label("🕐  " + dateStr);
        lblDate.setStyle("-fx-font-size:11; -fx-text-fill:#64748b;");
        VBox authorInfo = new VBox(2, lblAuteur, lblDate);

        Label badge = new Label("NOUVEAU");
        badge.setStyle("-fx-background-color:linear-gradient(to right,#dbeafe,#ede9fe); -fx-text-fill:#3730a3; -fx-font-size:10; " +
                "-fx-font-weight:800; -fx-padding:4 8 4 8; -fx-background-radius:12;");

        // Spacer + bouton ⋮
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topRow = new HBox(10, avatar, authorInfo, badge, spacer);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Titre
        Label lblTitre = new Label(p.getTitre() != null && !p.getTitre().isEmpty()
                ? p.getTitre() : "(sans titre)");
        lblTitre.setStyle("-fx-font-size:16; -fx-font-weight:800; -fx-text-fill:#0f172a; " +
                          "-fx-padding:6 0 0 0;");

        // Contenu
        Label lblContenu = new Label(p.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:13; -fx-text-fill:#334155; -fx-padding:2 0 4 0;");

        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;

        // Commentaires
        VBox commentsBox = new VBox(6);
        commentsBox.setStyle("-fx-padding:6 0 0 0;");
        for (Commentaire c : serviceCommentaire.getByPost(p.getId()))
            commentsBox.getChildren().add(buildCommentRow(c));

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(javafx.geometry.Pos.CENTER);
        actionRow.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: #f1f5f9; -fx-border-width: 1 0 1 0;");
        
        Button likeBtn = new Button("👍 J'aime");
        Button replyBtn = new Button("💬 Commenter");
        Button shareBtn = new Button("↗ Partager");
        
        String actionStyle = "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 13; -fx-font-weight: 700; " +
                             "-fx-padding: 8 12; -fx-background-radius: 10; -fx-cursor: hand;";
        
        likeBtn.setStyle(actionStyle);
        replyBtn.setStyle(actionStyle);
        shareBtn.setStyle(actionStyle);
        
        likeBtn.setMaxWidth(Double.MAX_VALUE);
        replyBtn.setMaxWidth(Double.MAX_VALUE);
        shareBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(likeBtn, Priority.ALWAYS);
        HBox.setHgrow(replyBtn, Priority.ALWAYS);
        HBox.setHgrow(shareBtn, Priority.ALWAYS);
        
        likeBtn.setAlignment(javafx.geometry.Pos.CENTER);
        replyBtn.setAlignment(javafx.geometry.Pos.CENTER);
        shareBtn.setAlignment(javafx.geometry.Pos.CENTER);

        // Hover effects
        String hoverStyle = "-fx-background-color: #f1f5f9; -fx-text-fill: #2563eb;";
        likeBtn.setOnMouseEntered(e -> likeBtn.setStyle(actionStyle + hoverStyle));
        likeBtn.setOnMouseExited(e -> likeBtn.setStyle(actionStyle));
        replyBtn.setOnMouseEntered(e -> replyBtn.setStyle(actionStyle + hoverStyle));
        replyBtn.setOnMouseExited(e -> replyBtn.setStyle(actionStyle));
        shareBtn.setOnMouseEntered(e -> shareBtn.setStyle(actionStyle + hoverStyle));
        shareBtn.setOnMouseExited(e -> shareBtn.setStyle(actionStyle));

        actionRow.getChildren().addAll(likeBtn, replyBtn, shareBtn);

        if (p.getUserId() == currentUserId) {
            Button btnEdit = new Button("✏");
            Button btnDel = new Button("🗑");
            
            String btnBase = "-fx-background-color: transparent; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 14; -fx-padding: 4 8;";
            btnEdit.setStyle(btnBase + "-fx-text-fill: #3b82f6;");
            btnDel.setStyle(btnBase + "-fx-text-fill: #ef4444;");
            
            btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnBase + "-fx-text-fill: #2563eb; -fx-background-color: #eff6ff;"));
            btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnBase + "-fx-text-fill: #3b82f6; -fx-background-color: transparent;"));
            btnDel.setOnMouseEntered(e -> btnDel.setStyle(btnBase + "-fx-text-fill: #dc2626; -fx-background-color: #fef2f2;"));
            btnDel.setOnMouseExited(e -> btnDel.setStyle(btnBase + "-fx-text-fill: #ef4444; -fx-background-color: transparent;"));

            btnEdit.setOnAction(e -> onModifierPost(p, card, lblTitre, lblContenu, actionRow));
            btnDel.setOnAction(e -> onSupprimerPost(p, card));
            
            HBox postActions = new HBox(5, btnEdit, btnDel);
            postActions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            topRow.getChildren().add(postActions);
        }

        // Champ commentaire
        HBox addComment = new HBox(8);
        TextField commentField = new TextField();
        commentField.setPromptText("Ajouter un commentaire...");
        commentField.setStyle("-fx-background-color:#f8fbff; -fx-background-radius:20; " +
                              "-fx-border-color:#cfdbef; -fx-border-radius:20; -fx-border-width:1; " +
                              "-fx-padding:9 16 9 16; -fx-font-size:12;");
        HBox.setHgrow(commentField, Priority.ALWAYS);

        Button btnComment = new Button("Envoyer");
        btnComment.setStyle("-fx-background-color:linear-gradient(to right,#2563eb,#7c3aed); -fx-text-fill:white; -fx-font-size:12; " +
                            "-fx-font-weight:700; -fx-padding:8 16 8 16; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0; " +
                            "-fx-effect:dropshadow(gaussian,rgba(99,102,241,0.30),10,0,0,2);");
        btnComment.setOnAction(e -> {
            String txt = commentField.getText().trim();
            if (txt.isEmpty()) {
                commentField.setStyle("-fx-background-color:#ffe4eb; -fx-background-radius:20; " +
                                      "-fx-border-color:transparent; -fx-border-width:0; -fx-padding:9 16 9 16; -fx-font-size:12;");
                commentField.setPromptText("Le commentaire ne peut pas être vide !");
                return;
            }
            if (txt.length() < 2) {
                commentField.setStyle("-fx-background-color:#ffe4eb; -fx-background-radius:20; " +
                                      "-fx-border-color:transparent; -fx-border-width:0; -fx-padding:9 16 9 16; -fx-font-size:12;");
                commentField.setPromptText("Minimum 2 caractères.");
                return;
            }
            if (txt.length() > 500) {
                commentField.setStyle("-fx-background-color:#ffe4eb; -fx-background-radius:20; " +
                                      "-fx-border-color:transparent; -fx-border-width:0; -fx-padding:9 16 9 16; -fx-font-size:12;");
                commentField.setPromptText("Maximum 500 caractères.");
                return;
            }
                commentField.setStyle("-fx-background-color:#f8fbff; -fx-background-radius:20; " +
                                  "-fx-border-color:#cfdbef; -fx-border-width:1; -fx-padding:9 16 9 16; -fx-font-size:12;");
            int uid = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getId() : 0;
            Commentaire newC = new Commentaire(txt, p.getId(), uid);
            serviceCommentaire.ajouter(newC);
            commentField.clear();
            commentsBox.getChildren().add(buildCommentRow(newC));
        });

        addComment.getChildren().addAll(commentField, btnComment);
        card.getChildren().addAll(topRow, lblTitre, lblContenu, actionRow, commentsBox, addComment);
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

        TextField fTitre = new TextField(p.getTitre());
        fTitre.setPromptText("Titre de la publication");
        fTitre.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-border-color: #e2e8f0; " +
                         "-fx-border-radius: 10; -fx-padding: 8 12; -fx-font-size: 14; -fx-font-weight: 700;");

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
                
                lblTitre.setText(titre.isEmpty() ? "(sans titre)" : titre);
                lblContenu.setText(contenu);
                
                card.getChildren().remove(editBox);
                lblTitre.setVisible(true);
                lblTitre.setManaged(true);
                lblContenu.setVisible(true);
                lblContenu.setManaged(true);
                actionRow.setVisible(true);
                actionRow.setManaged(true);
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

        // Avatar cercle avec initiale
        String initiale = nom.isEmpty() ? "?" : String.valueOf(nom.charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setMinSize(32, 32);
        avatar.setMaxSize(32, 32);
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setStyle("-fx-background-color:linear-gradient(to bottom right,#3b82f6,#7c3aed); -fx-background-radius:50; " +
                        "-fx-text-fill:white; -fx-font-size:13; -fx-font-weight:700;");

        // Bulle : nom en gras + contenu
        Label lblNom = new Label(nom);
        lblNom.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#0f172a;");

        Label lblContenu = new Label(c.getContenu());
        lblContenu.setWrapText(true);
        lblContenu.setStyle("-fx-font-size:12; -fx-text-fill:#334155;");

        VBox bubble = new VBox(2, lblNom, lblContenu);
        bubble.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 18; " +
                        "-fx-padding: 10 16;");
        HBox.setHgrow(bubble, Priority.ALWAYS);

        HBox row = new HBox(8, avatar, bubble);
        row.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        int currentUserId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getId() : -1;
        if (c.getUserId() == currentUserId) {
            Button btnEdit = new Button("✏");
            Button btnDel = new Button("🗑");
            
            String btnBase = "-fx-background-color: transparent; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 11; -fx-padding: 2 6;";
            btnEdit.setStyle(btnBase + "-fx-text-fill: #64748b;");
            btnDel.setStyle(btnBase + "-fx-text-fill: #94a3b8;");
            
            btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(btnBase + "-fx-text-fill: #3b82f6; -fx-background-color: #eff6ff;"));
            btnEdit.setOnMouseExited(e -> btnEdit.setStyle(btnBase + "-fx-text-fill: #64748b; -fx-background-color: transparent;"));
            btnDel.setOnMouseEntered(e -> btnDel.setStyle(btnBase + "-fx-text-fill: #ef4444; -fx-background-color: #fef2f2;"));
            btnDel.setOnMouseExited(e -> btnDel.setStyle(btnBase + "-fx-text-fill: #94a3b8; -fx-background-color: transparent;"));

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
