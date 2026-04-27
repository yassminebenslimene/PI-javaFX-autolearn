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
import tn.esprit.services.ResourceRecommendationService;
import tn.esprit.services.TrendAnalyzerService;
import tn.esprit.services.GroqTopicService;
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
    @FXML private TextField  fieldTitre;
    @FXML private TextArea   fieldContenu;
    @FXML private TextField  fieldTags;
    @FXML private HBox       mediaPreviewBox;
    @FXML private Button     btnImprove;

    // Pending attachments for the next post
    private java.io.File pendingImageFile = null;
    private java.io.File pendingVideoFile = null;
    private java.io.File pendingDocFile   = null;

    private static final String UPLOAD_DIR = "uploads/posts/";
    @FXML private VBox      postsPane;
    @FXML private VBox      similarPostsBox;
    @FXML private VBox      similarPostsList;
    @FXML private VBox      resourcesBox;
    @FXML private VBox      resourcesList;
    @FXML private VBox      trendingBox;
    @FXML private VBox      trendingList;
    @FXML private ScrollPane mainScrollPane;

    private ResourceRecommendationService recommendationService;
    private TrendAnalyzerService          trendAnalyzerService;
    private GroqTopicService              groqService;
    /** Maps postId → its card VBox for scroll-to navigation */
    private final java.util.Map<Integer, VBox> postCardMap = new java.util.HashMap<>();

    private final ServicePost        servicePost        = new ServicePost();
    private final ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private final ServiceCommunaute  serviceCommunaute  = new ServiceCommunaute();
    private final UserService        userService        = new UserService();

    private Communaute communaute;
    private Runnable   onRetour;
    private Label      emptyLabel;

    // Navigation callbacks injected by parent
    private java.util.function.Consumer<Integer> onNavigateToCours;
    private java.util.function.Consumer<Integer> onNavigateToQuiz;

    public void setOnNavigateToCours(java.util.function.Consumer<Integer> cb) { this.onNavigateToCours = cb; }
    public void setOnNavigateToQuiz(java.util.function.Consumer<Integer> cb)  { this.onNavigateToQuiz  = cb; }

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
        loadTrends();
    }

    private void loadPosts() {
        postsPane.getChildren().clear();
        postCardMap.clear();
        emptyLabel = null;
        List<Post> posts = servicePost.getHotByCommunaute(communaute.getId());
        if (statPosts != null) statPosts.setText(String.valueOf(posts.size()));
        if (posts.isEmpty()) {
            emptyLabel = new Label("✨  Aucun post pour l'instant. Soyez le premier à publier !");
            emptyLabel.setStyle("-fx-text-fill:#c4b5fd; -fx-font-size:13; -fx-padding:16 0 0 0;");
            postsPane.getChildren().add(emptyLabel);
        } else {
            for (int i = 0; i < posts.size(); i++) {
                postsPane.getChildren().add(buildPostCard(posts.get(i), i));
            }
        }
    }

    @FXML
    public void onAttachImage() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        java.io.File f = fc.showOpenDialog(fieldTitre.getScene().getWindow());
        if (f != null) { pendingImageFile = f; addMediaChip("🖼  " + f.getName(), "#e0f2fe", "#0369a1"); }
    }

    @FXML
    public void onAttachVideo() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Choisir une vidéo");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Vidéos", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.webm"));
        java.io.File f = fc.showOpenDialog(fieldTitre.getScene().getWindow());
        if (f != null) { pendingVideoFile = f; addMediaChip("🎬  " + f.getName(), "#fef3c7", "#d97706"); }
    }

    @FXML
    public void onAttachFile() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Choisir un fichier");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter(
                "Fichiers", "*.pdf", "*.doc", "*.docx", "*.txt", "*.zip", "*.ppt", "*.pptx"));
        java.io.File f = fc.showOpenDialog(fieldTitre.getScene().getWindow());
        if (f != null) { pendingDocFile = f; addMediaChip("📎  " + f.getName(), "#f0fdf4", "#15803d"); }
    }

    private void addMediaChip(String name, String bg, String fg) {
        if (mediaPreviewBox == null) return;
        Label chip = new Label(name);
        chip.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                      "-fx-font-size:11; -fx-font-weight:700;" +
                      "-fx-padding:6 14; -fx-background-radius:20;");
        // X button to remove
        Button btnX = new Button("✕");
        btnX.setStyle("-fx-background-color:transparent; -fx-text-fill:" + fg + ";" +
                      "-fx-font-size:10; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 0 0 4;");
        HBox chipBox = new HBox(4, chip, btnX);
        chipBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        btnX.setOnAction(e -> {
            mediaPreviewBox.getChildren().remove(chipBox);
            // clear the right pending file based on chip text
            if (name.startsWith("🖼")) pendingImageFile = null;
            else if (name.startsWith("🎬")) pendingVideoFile = null;
            else pendingDocFile = null;
            if (mediaPreviewBox.getChildren().isEmpty()) {
                mediaPreviewBox.setVisible(false);
                mediaPreviewBox.setManaged(false);
            }
        });
        mediaPreviewBox.getChildren().add(chipBox);
        mediaPreviewBox.setVisible(true);
        mediaPreviewBox.setManaged(true);
    }

    /** Copies a file to UPLOAD_DIR and returns the stored filename */
    private String saveFile(java.io.File src) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(UPLOAD_DIR);
            java.nio.file.Files.createDirectories(dir);
            String name = System.currentTimeMillis() + "_" + src.getName();
            java.nio.file.Files.copy(src.toPath(), dir.resolve(name),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (Exception e) {
            System.err.println("[Media] saveFile: " + e.getMessage());
            return null;
        }
    }

    @FXML
    public void onImprovePost() {
        String contenu = fieldContenu.getText().trim();
        if (contenu.isEmpty()) return;

        // Show loading state
        if (btnImprove != null) {
            btnImprove.setText("⏳  En cours...");
            btnImprove.setDisable(true);
        }

        // Run Groq in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                if (groqService == null) groqService = new GroqTopicService();
                System.out.println("[Groq] improving text: " + contenu.substring(0, Math.min(50, contenu.length())));
                String improved = groqService.improvePost(contenu);
                System.out.println("[Groq] result: " + improved.substring(0, Math.min(80, improved.length())));

                javafx.application.Platform.runLater(() -> {
                    fieldContenu.setText(improved);
                    if (btnImprove != null) {
                        btnImprove.setText("✅  Amélioré !");
                        btnImprove.setStyle("-fx-background-color:#d1fae5; -fx-text-fill:#059669;" +
                                           "-fx-font-size:12; -fx-font-weight:800;" +
                                           "-fx-padding:13 22; -fx-background-radius:30; -fx-border-width:0;");
                        // Reset after 2s
                        javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
                                javafx.util.Duration.seconds(2));
                        pt.setOnFinished(e -> {
                            btnImprove.setText("✨  Améliorer");
                            btnImprove.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                                               "-fx-font-size:12; -fx-font-weight:800;" +
                                               "-fx-padding:13 22; -fx-background-radius:30; -fx-border-width:0;");
                            btnImprove.setDisable(false);
                        });
                        pt.play();
                    }
                });
            } catch (Exception e) {
                System.err.println("[Groq] onImprovePost error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    if (btnImprove != null) {
                        btnImprove.setText("✨  Améliorer");
                        btnImprove.setDisable(false);
                    }
                });
            }
        }).start();
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

        // Tags: manual input takes priority, else auto-extract
        String manualTags = fieldTags != null ? fieldTags.getText().trim() : "";
        if (!manualTags.isEmpty()) {
            p.setTags(manualTags.toLowerCase().replaceAll("\\s*,\\s*", ","));
        }

        // Save media attachments
        if (pendingImageFile != null) { p.setImageFile(saveFile(pendingImageFile)); pendingImageFile = null; }
        if (pendingVideoFile != null) { p.setVideoFile(saveFile(pendingVideoFile)); pendingVideoFile = null; }
        if (pendingDocFile   != null) { p.setSummary(saveFile(pendingDocFile));     pendingDocFile   = null; }

        servicePost.ajouter(p);
        if (emptyLabel != null) {
            postsPane.getChildren().remove(emptyLabel);
            emptyLabel = null;
        }
        postsPane.getChildren().add(0, buildPostCard(p, 0));
        fieldTitre.clear();
        fieldContenu.clear();
        if (fieldTags != null) fieldTags.clear();
        if (mediaPreviewBox != null) {
            mediaPreviewBox.getChildren().clear();
            mediaPreviewBox.setVisible(false);
            mediaPreviewBox.setManaged(false);
        }
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

    private VBox buildPostCard(Post p, int rank) {
        VBox card = new VBox(0);
        // Top post gets a hot border
        String borderColor = rank == 0 ? "#f59e0b" : "#ede9fe";
        card.setStyle("-fx-background-color:white; -fx-background-radius:24; " +
                      "-fx-border-color:" + borderColor + "; -fx-border-radius:24; -fx-border-width:1.5; " +
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

        // Hot badge for top 3
        HBox topRow = new HBox(14, avatar, authorInfo, spacer);
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (rank == 0) {
            Label hotBadge = new Label("🔥 Hot");
            hotBadge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                              "-fx-font-size:10; -fx-font-weight:900;" +
                              "-fx-padding:4 12; -fx-background-radius:20;");
            topRow.getChildren().add(hotBadge);
        } else if (rank == 1) {
            Label badge = new Label("📈 Trending");
            badge.setStyle("-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed;" +
                           "-fx-font-size:10; -fx-font-weight:900;" +
                           "-fx-padding:4 12; -fx-background-radius:20;");
            topRow.getChildren().add(badge);
        }

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
            itemSupprimer.setOnAction(e -> onSupprimerPost(p, card));            btnMenu.setOnAction(e -> menu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0));
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

        // Tags chips
        if (p.getTags() != null && !p.getTags().isBlank()) {
            HBox tagsRow = new HBox(6);
            tagsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            tagsRow.setStyle("-fx-padding:4 0 0 0;");
            for (String tag : p.getTags().split(",")) {
                String t = tag.trim();
                if (t.isEmpty()) continue;
                Label chip = new Label("# " + t);
                chip.setStyle("-fx-background-color:#f0eeff; -fx-text-fill:#7c3aed;" +
                              "-fx-font-size:10; -fx-font-weight:800;" +
                              "-fx-padding:3 10; -fx-background-radius:20;");
                tagsRow.getChildren().add(chip);
            }
            contentBox.getChildren().add(tagsRow);
        }

        // Media attachments
        if (p.getImageFile() != null && !p.getImageFile().isBlank()) {
            try {
                java.io.File imgFile = new java.io.File(UPLOAD_DIR + p.getImageFile());
                if (imgFile.exists()) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(imgFile.toURI().toString());
                    javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                    iv.setFitWidth(460); iv.setPreserveRatio(true);
                    iv.setStyle("-fx-background-radius:12;");
                    contentBox.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }
        if (p.getVideoFile() != null && !p.getVideoFile().isBlank()) {
            try {
                java.io.File vidFile = new java.io.File(UPLOAD_DIR + p.getVideoFile());
                if (vidFile.exists()) {
                    javafx.scene.media.Media media = new javafx.scene.media.Media(vidFile.toURI().toString());
                    javafx.scene.media.MediaPlayer mp = new javafx.scene.media.MediaPlayer(media);
                    javafx.scene.media.MediaView mv = new javafx.scene.media.MediaView(mp);
                    mv.setFitWidth(460); mv.setPreserveRatio(true);
                    // Play/pause on click
                    HBox videoBox = new HBox(mv);
                    videoBox.setStyle("-fx-background-color:#000; -fx-background-radius:12; -fx-cursor:hand;");
                    videoBox.setOnMouseClicked(e -> {
                        if (mp.getStatus() == javafx.scene.media.MediaPlayer.Status.PLAYING) mp.pause();
                        else mp.play();
                    });
                    contentBox.getChildren().add(videoBox);
                }
            } catch (Exception ignored) {}
        }
        if (p.getSummary() != null && !p.getSummary().isBlank()
                && !p.getSummary().contains(" ")) { // stored filename (no spaces)
            java.io.File docFile = new java.io.File(UPLOAD_DIR + p.getSummary());
            if (docFile.exists()) {
                Button btnDoc = new Button("📎  " + docFile.getName());
                btnDoc.setStyle("-fx-background-color:#f0fdf4; -fx-text-fill:#15803d;" +
                                "-fx-font-size:11; -fx-font-weight:700;" +
                                "-fx-padding:8 16; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
                btnDoc.setOnAction(e -> {
                    try { java.awt.Desktop.getDesktop().open(docFile); }
                    catch (Exception ex) { ex.printStackTrace(); }
                });
                contentBox.getChildren().add(btnDoc);
            }
        }

        body.getChildren().addAll(topRow, contentBox);

        // ── Reaction bar ─────────────────────────────────
        // Count comments for display
        int commentCount = serviceCommentaire.getByPost(p.getId()).size();
        int likeCount = 0;
        try {
            String aiR = p.getAiReaction();
            if (aiR != null && !aiR.isBlank()) likeCount = Integer.parseInt(aiR.trim());
        } catch (NumberFormatException ignored) {}

        HBox reactionBar = new HBox(16);
        reactionBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        reactionBar.setStyle("-fx-padding:14 0 4 0;");

        // Like button
        final int[] currentLikes = {likeCount};
        Label likeCountLbl = new Label(likeCount > 0 ? "  " + likeCount : "");
        likeCountLbl.setStyle("-fx-font-size:12; -fx-font-weight:700; -fx-text-fill:#7c3aed;");

        Button btnLikePost = new Button("♥  J'aime");
        btnLikePost.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                             "-fx-font-size:12; -fx-font-weight:700;" +
                             "-fx-padding:8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
        btnLikePost.setOnMouseEntered(e -> btnLikePost.setStyle(
                "-fx-background-color:#ede9fe; -fx-text-fill:#6d28d9;" +
                "-fx-font-size:12; -fx-font-weight:700;" +
                "-fx-padding:8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;"));
        btnLikePost.setOnMouseExited(e -> btnLikePost.setStyle(
                "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                "-fx-font-size:12; -fx-font-weight:700;" +
                "-fx-padding:8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;"));
        btnLikePost.setOnAction(e -> {
            currentLikes[0]++;
            p.setAiReaction(String.valueOf(currentLikes[0]));
            servicePost.modifier(p);
            likeCountLbl.setText("  " + currentLikes[0]);
            btnLikePost.setStyle("-fx-background-color:#e94560; -fx-text-fill:white;" +
                                 "-fx-font-size:12; -fx-font-weight:700;" +
                                 "-fx-padding:8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;");
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(700));
            pause.setOnFinished(ev -> btnLikePost.setStyle(
                    "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                    "-fx-font-size:12; -fx-font-weight:700;" +
                    "-fx-padding:8 18; -fx-background-radius:20; -fx-cursor:hand; -fx-border-width:0;"));
            pause.play();
            // ── Trigger similarity recommendation ──
            showSimilarPosts(p);
            showResourceRecommendations(p);
        });

        // Comment count chip
        Label commentChip = new Label("💬  " + commentCount + " commentaire" + (commentCount > 1 ? "s" : ""));
        commentChip.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#6b7280;" +
                             "-fx-font-size:12; -fx-font-weight:600;" +
                             "-fx-padding:8 18; -fx-background-radius:20;");

        reactionBar.getChildren().addAll(btnLikePost, likeCountLbl, commentChip);
        body.getChildren().add(reactionBar);

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
        postCardMap.put(p.getId(), card);
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
                if (idx >= 0) postsPane.getChildren().set(idx, buildPostCard(p, idx));
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

        // ── Like button + count ──────────────────────────
        Label likesCount = new Label(c.getLikes() > 0 ? String.valueOf(c.getLikes()) : "");
        likesCount.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:#7c3aed;");

        Button btnLike = new Button("♥");
        btnLike.setStyle("-fx-background-color:transparent; -fx-text-fill:#c4b5fd;" +
                         "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 4 0 0;");
        btnLike.setOnMouseEntered(e -> btnLike.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#7c3aed;" +
                "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 4 0 0;"));
        btnLike.setOnMouseExited(e -> btnLike.setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#c4b5fd;" +
                "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 4 0 0;"));
        btnLike.setOnAction(e -> {
            int newLikes = serviceCommentaire.likeCommentaire(c.getId());
            c.setLikes(newLikes);
            likesCount.setText(String.valueOf(newLikes));
            // animate: turn red briefly
            btnLike.setStyle("-fx-background-color:transparent; -fx-text-fill:#e94560;" +
                             "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 4 0 0;");
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(600));
            pause.setOnFinished(ev -> btnLike.setStyle(
                    "-fx-background-color:transparent; -fx-text-fill:#7c3aed;" +
                    "-fx-font-size:14; -fx-cursor:hand; -fx-border-width:0; -fx-padding:0 4 0 0;"));
            pause.play();
        });

        HBox likeRow = new HBox(4, btnLike, likesCount);
        likeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        likeRow.setStyle("-fx-padding:6 0 0 0;");

        VBox bubble = new VBox(4, lblNom, lblContenu, likeRow);
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

    /** Analyzes last-24h posts+comments and shows trending keywords in sidebar */
    private void loadTrends() {
        if (trendingBox == null || trendingList == null) return;
        try {
            if (trendAnalyzerService == null) trendAnalyzerService = new TrendAnalyzerService();
            List<TrendAnalyzerService.TrendWord> trends = trendAnalyzerService.getTrends(communaute.getId());
        if (trends.isEmpty()) return;

        trendingList.getChildren().clear();
        for (TrendAnalyzerService.TrendWord tw : trends) {
            // Tag chip
            Label chip = new Label("#" + tw.word());
            chip.setStyle("-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                          "-fx-font-size:12; -fx-font-weight:800;" +
                          "-fx-padding:6 14; -fx-background-radius:20; -fx-cursor:hand;");
            chip.setOnMouseEntered(e -> chip.setStyle(
                    "-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5); -fx-text-fill:white;" +
                    "-fx-font-size:12; -fx-font-weight:800;" +
                    "-fx-padding:6 14; -fx-background-radius:20; -fx-cursor:hand;"));
            chip.setOnMouseExited(e -> chip.setStyle(
                    "-fx-background-color:#f5f3ff; -fx-text-fill:#7c3aed;" +
                    "-fx-font-size:12; -fx-font-weight:800;" +
                    "-fx-padding:6 14; -fx-background-radius:20; -fx-cursor:hand;"));

            // Count badge
            Label countBadge = new Label(tw.count() + "×");
            countBadge.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                                "-fx-font-size:10; -fx-font-weight:800;" +
                                "-fx-padding:3 8; -fx-background-radius:20;");

            // Score bar
            double pct = Math.min(tw.score() / 10.0, 1.0);
            HBox barBg = new HBox();
            barBg.setStyle("-fx-background-color:#f0eeff; -fx-background-radius:4; -fx-pref-height:3;");
            HBox barFill = new HBox();
            barFill.setPrefWidth(pct * 220);
            barFill.setStyle("-fx-background-color:linear-gradient(to right,#f59e0b,#ef4444);" +
                             "-fx-background-radius:4; -fx-pref-height:3;");
            barBg.getChildren().add(barFill);

            HBox row = new HBox(8, chip, countBadge);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            VBox item = new VBox(6, row, barBg);
            trendingList.getChildren().add(item);
        }

        trendingBox.setVisible(true);
        trendingBox.setManaged(true);
        } catch (Exception e) {
            System.err.println("[Trends] loadTrends error: " + e.getMessage());
        }
    }

    /** Shows recommended courses & quizzes in the sidebar based on post keywords */
    private void showResourceRecommendations(Post source) {
        if (resourcesBox == null || resourcesList == null) return;
        try {
            if (recommendationService == null) recommendationService = new ResourceRecommendationService();
            var results = recommendationService.recommend(source, 5);
        if (results.isEmpty()) return;

        resourcesList.getChildren().clear();
        for (var r : results) {
            boolean isCours = r.type().equals("cours");

            // Icon + type badge
            Label typeBadge = new Label(isCours ? "📚 Cours" : "🧠 Quiz");
            typeBadge.setStyle(isCours
                ? "-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed;" +
                  "-fx-font-size:9; -fx-font-weight:800; -fx-padding:3 10; -fx-background-radius:20;"
                : "-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                  "-fx-font-size:9; -fx-font-weight:800; -fx-padding:3 10; -fx-background-radius:20;");

            Label lblTitle = new Label(r.titre());
            lblTitle.setWrapText(true);
            lblTitle.setStyle("-fx-font-size:12; -fx-font-weight:800; -fx-text-fill:#1e1b4b;");

            Label lblSub = new Label(r.subtitle());
            lblSub.setStyle("-fx-font-size:10; -fx-text-fill:#a78bfa;");

            // Score bar
            double pct = Math.min(r.score(), 1.0);
            HBox scoreBar = new HBox();
            scoreBar.setStyle("-fx-background-color:#f0eeff; -fx-background-radius:4; -fx-pref-height:4;");
            HBox fill = new HBox();
            fill.setPrefWidth(pct * 220);
            fill.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#4f46e5);" +
                          "-fx-background-radius:4; -fx-pref-height:4;");
            scoreBar.getChildren().add(fill);

            VBox card = new VBox(6, new HBox(6, typeBadge), lblTitle, lblSub, scoreBar);
            card.setStyle("-fx-background-color:#faf8ff; -fx-background-radius:14;" +
                          "-fx-border-color:#ede9fe; -fx-border-radius:14; -fx-border-width:1;" +
                          "-fx-padding:12 14; -fx-cursor:hand;");
            card.setOnMouseClicked(e -> {
                if (r.type().equals("cours") && onNavigateToCours != null) {
                    onNavigateToCours.accept(r.id());
                } else if (r.type().equals("quiz") && onNavigateToQuiz != null) {
                    onNavigateToQuiz.accept(r.id());
                } else {
                    try { tn.esprit.MainApp.showCommunauteFront(); } catch (Exception ex) { ex.printStackTrace(); }
                }
            });
            card.setOnMouseEntered(e -> card.setStyle(
                    "-fx-background-color:#f0eeff; -fx-background-radius:14;" +
                    "-fx-border-color:#7c3aed; -fx-border-radius:14; -fx-border-width:1.5;" +
                    "-fx-padding:12 14; -fx-cursor:hand;"));
            card.setOnMouseExited(e -> card.setStyle(
                    "-fx-background-color:#faf8ff; -fx-background-radius:14;" +
                    "-fx-border-color:#ede9fe; -fx-border-radius:14; -fx-border-width:1;" +
                    "-fx-padding:12 14; -fx-cursor:hand;"));

            resourcesList.getChildren().add(card);
        }

        resourcesBox.setVisible(true);
        resourcesBox.setManaged(true);
        } catch (Exception e) {
            System.err.println("[Recommend] error: " + e.getMessage());
        }
    }

    /** Shows top-3 similar posts in the sidebar when user likes a post */
    private void showSimilarPosts(Post source) {
        if (similarPostsBox == null || similarPostsList == null) return;
        List<Post> similar = servicePost.getSimilarPosts(source, communaute.getId(), 3);
        if (similar.isEmpty()) return;

        similarPostsList.getChildren().clear();
        for (Post p : similar) {
            VBox card = new VBox(5);
            card.setStyle("-fx-background-color:#faf8ff; -fx-background-radius:14;" +
                          "-fx-border-color:#ede9fe; -fx-border-radius:14; -fx-border-width:1;" +
                          "-fx-padding:12 14; -fx-cursor:hand;");

            String titleText = (p.getTitre() != null && !p.getTitre().isBlank())
                    ? p.getTitre() : p.getContenu();
            if (titleText.length() > 60) titleText = titleText.substring(0, 57) + "…";

            Label lblTitle = new Label(titleText);
            lblTitle.setWrapText(true);
            lblTitle.setStyle("-fx-font-size:12; -fx-font-weight:800; -fx-text-fill:#1e1b4b;");

            // Tags chips
            HBox tagsRow = new HBox(6);
            tagsRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            if (p.getTags() != null && !p.getTags().isBlank()) {
                String[] tags = p.getTags().split(",");
                int shown = 0;
                for (String tag : tags) {
                    if (shown >= 3) break;
                    Label chip = new Label(tag.trim());
                    chip.setStyle("-fx-background-color:#ede9fe; -fx-text-fill:#7c3aed;" +
                                  "-fx-font-size:9; -fx-font-weight:700;" +
                                  "-fx-padding:2 8; -fx-background-radius:10;");
                    tagsRow.getChildren().add(chip);
                    shown++;
                }
            }

            card.getChildren().addAll(lblTitle, tagsRow);
            // Click → scroll to post (just reload for now)
            card.setOnMouseClicked(e -> {
                // Scroll to the target post
                VBox targetCard = postCardMap.get(p.getId());
                if (targetCard != null && mainScrollPane != null) {
                    // compute relative Y position of the card inside the scrollPane content
                    javafx.application.Platform.runLater(() -> {
                        double cardY = targetCard.localToScene(0, 0).getY();
                        double contentH = mainScrollPane.getContent().getBoundsInLocal().getHeight();
                        double viewH   = mainScrollPane.getViewportBounds().getHeight();
                        double scrollY = (cardY - mainScrollPane.localToScene(0, 0).getY()
                                         + mainScrollPane.getVvalue() * (contentH - viewH))
                                         / (contentH - viewH);
                        mainScrollPane.setVvalue(Math.max(0, Math.min(1, scrollY)));
                    });
                }
                // highlight the card briefly
                String origStyle = card.getStyle();
                card.setStyle("-fx-background-color:white; -fx-background-radius:24;" +
                              "-fx-border-color:#7c3aed; -fx-border-radius:24; -fx-border-width:2;" +
                              "-fx-padding:12 14; -fx-cursor:hand;" +
                              "-fx-effect:dropshadow(gaussian,rgba(109,40,217,0.25),16,0,0,4);");
                javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(800));
                pt.setOnFinished(ev -> card.setStyle(origStyle));
                pt.play();
            });

            similarPostsList.getChildren().add(card);
        }

        similarPostsBox.setVisible(true);
        similarPostsBox.setManaged(true);
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

    @FXML public void onRefresh() { loadPosts(); loadTrends(); }
    @FXML public void onRetour() { if (onRetour != null) onRetour.run(); }
}
