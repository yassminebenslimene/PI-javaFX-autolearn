package tn.esprit.controllers;

import javafx.animation.*;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;import javafx.util.Duration;
import tn.esprit.entities.User;
import tn.esprit.services.MessagerieService;
import tn.esprit.session.SessionManager;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * MessagerieController — Interface de messagerie moderne (WhatsApp/Messenger style).
 *
 * Fonctionnalités :
 *   - Sidebar : Contacts / Étudiants / Demandes de follow
 *   - Chat : bulles gauche/droite, heure, "Vu", "est en train d'écrire"
 *   - Polling toutes les 2s pour le temps réel
 *   - Recherche dans les messages
 *   - Mode sombre
 *   - Animations d'envoi
 */
public class MessagerieController {

    // ── FXML ─────────────────────────────────────────────────────────────────

    // Sidebar
    @FXML private Label   badgeNonLus;
    @FXML private Label   badgeDemandes;
    @FXML private TextField searchGlobal;

    // Onglets
    @FXML private Button btnTabContacts, btnTabEtudiants, btnTabDemandes;
    @FXML private VBox   paneContacts, paneEtudiants, paneDemandes;
    @FXML private VBox   listContacts, listEtudiants, listDemandes;

    // Chat
    @FXML private VBox      panneauVide;
    @FXML private BorderPane panneauChat;
    @FXML private Label     labelAvatarChat, labelNomContact, labelStatutContact;
    @FXML private ScrollPane scrollMessages;
    @FXML private VBox      containerMessages;
    @FXML private TextField champMessage;
    @FXML private Button    btnEnvoyer;
    @FXML private Button    btnEmoji;
    @FXML private Button    btnFichier;
    @FXML private HBox      indicateurEcriture;
    @FXML private Label     labelEcriture;
    @FXML private HBox      barreRechercheMsg;
    @FXML private TextField champRechercheMsg;

    // ── État ─────────────────────────────────────────────────────────────────

    private final MessagerieService service = new MessagerieService();
    private User currentUser;
    private Map<String, Object> contactActif = null;
    private int dernierMessageId = 0;
    private Timeline pollingTimeline;
    private Timeline typingTimeline;   // timer pour masquer "est en train d'écrire"
    private boolean modeSombre = false;
    private boolean typingEnCours = false;

    // Couleurs mode clair / sombre
    private static final String BG_CLAIR  = "#F5F6FA";
    private static final String BG_SOMBRE = "#1A1D23";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Cache des messages affichés (pour la recherche)
    private final List<Map<String, Object>> messagesAffiches = new ArrayList<>();

    // Map notifId → Label "Vu" pour mise à jour dynamique
    private final Map<Integer, Label> labelVuMap = new HashMap<>();

    // Map notifId → HBox ligne (pour modifier/supprimer visuellement)
    private final Map<Integer, HBox> ligneMap = new HashMap<>();
    // Map notifId → Label bulle texte (pour modifier visuellement)
    private final Map<Integer, Label> bulleTextMap = new HashMap<>();

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        chargerContacts();
        chargerEtudiants();
        chargerDemandes();
        mettreAJourBadge();
        demarrerPolling();

        // Recherche globale
        searchGlobal.textProperty().addListener((obs, old, val) -> filtrerContacts(val));

        // Recherche dans messages
        champRechercheMsg.textProperty().addListener((obs, old, val) -> rechercherDansMessages(val));

        // Entrée pour envoyer
        champMessage.setOnAction(e -> onEnvoyerMessage());
        containerMessages.setFillWidth(true);

        // ── Binding de hauteur : avec AnchorPane, les anchors gèrent tout automatiquement ──
        // Aucun binding manuel nécessaire — AnchorPane.bottomAnchor=0 force le bon comportement

        // Indicateur "est en train d'écrire" — s'affiche quand on tape, disparaît après 2s d'inactivité
        champMessage.textProperty().addListener((obs, old, val) -> {
            if (contactActif == null) return;
            if (!val.isEmpty()) {
                // Afficher l'indicateur côté local (simulation)
                // En vrai multi-user, on enverrait un signal "typing" en BDD
                if (typingTimeline != null) typingTimeline.stop();
                typingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                    // Masquer après 2s sans frappe
                }));
                typingTimeline.play();
            }
        });
    }

    // ── Onglets ───────────────────────────────────────────────────────────────

    private static final String TAB_ACTIVE =
        "-fx-background-color:transparent; -fx-text-fill:#7C3AED;" +
        "-fx-font-size:13; -fx-font-weight:700; -fx-cursor:hand;" +
        "-fx-border-color:transparent transparent #7C3AED transparent;" +
        "-fx-border-width:0 0 2 0; -fx-padding:10 16 10 16; -fx-background-radius:0;";

    private static final String TAB_INACTIVE =
        "-fx-background-color:transparent; -fx-text-fill:#6B7280;" +
        "-fx-font-size:13; -fx-font-weight:600; -fx-cursor:hand;" +
        "-fx-border-width:0; -fx-padding:10 16 10 16; -fx-background-radius:0;";

    @FXML private void onTabContacts() {
        afficherOnglet(paneContacts, paneEtudiants, paneDemandes);
        btnTabContacts.setStyle(TAB_ACTIVE);
        btnTabEtudiants.setStyle(TAB_INACTIVE);
        btnTabDemandes.setStyle(TAB_INACTIVE);
    }

    @FXML private void onTabEtudiants() {
        afficherOnglet(paneEtudiants, paneContacts, paneDemandes);
        btnTabEtudiants.setStyle(TAB_ACTIVE);
        btnTabContacts.setStyle(TAB_INACTIVE);
        btnTabDemandes.setStyle(TAB_INACTIVE);
    }

    @FXML private void onTabDemandes() {
        afficherOnglet(paneDemandes, paneContacts, paneEtudiants);
        btnTabDemandes.setStyle(TAB_ACTIVE);
        btnTabContacts.setStyle(TAB_INACTIVE);
        btnTabEtudiants.setStyle(TAB_INACTIVE);
    }

    private void afficherOnglet(VBox show, VBox... hide) {
        show.setVisible(true); show.setManaged(true);
        for (VBox h : hide) { h.setVisible(false); h.setManaged(false); }
    }

    // ── Chargement données ────────────────────────────────────────────────────

    private List<Map<String, Object>> tousContacts  = new ArrayList<>();
    private List<Map<String, Object>> tousEtudiants = new ArrayList<>();

    private void chargerContacts() {
        tousContacts = service.getContacts(currentUser.getId());
        listContacts.getChildren().clear();
        for (Map<String, Object> c : tousContacts)
            listContacts.getChildren().add(buildContactRow(c));
    }

    private void chargerEtudiants() {
        tousEtudiants = service.getTousLesEtudiants(currentUser.getId());
        listEtudiants.getChildren().clear();
        for (Map<String, Object> e : tousEtudiants)
            listEtudiants.getChildren().add(buildEtudiantRow(e));
    }

    private void chargerDemandes() {
        List<Map<String, Object>> demandes = service.getDemandesFollowEnAttente(currentUser.getId());
        listDemandes.getChildren().clear();
        for (Map<String, Object> d : demandes)
            listDemandes.getChildren().add(buildDemandeRow(d));

        if (!demandes.isEmpty()) {
            badgeDemandes.setText(String.valueOf(demandes.size()));
            badgeDemandes.setVisible(true); badgeDemandes.setManaged(true);
            btnTabDemandes.setText("Demandes (" + demandes.size() + ")");
        } else {
            badgeDemandes.setVisible(false); badgeDemandes.setManaged(false);
            btnTabDemandes.setText("Demandes");
        }
    }

    private void mettreAJourBadge() {
        int nb = service.getNombreMessagesNonLus(currentUser.getId());
        if (nb > 0) {
            badgeNonLus.setText(String.valueOf(nb));
            badgeNonLus.setVisible(true); badgeNonLus.setManaged(true);
        } else {
            badgeNonLus.setVisible(false); badgeNonLus.setManaged(false);
        }
    }

    private void filtrerContacts(String query) {
        // Filtrer les contacts
        listContacts.getChildren().clear();
        List<Map<String, Object>> sourceContacts = (query == null || query.isBlank())
            ? tousContacts
            : tousContacts.stream().filter(c -> fullName(c).toLowerCase().contains(query.toLowerCase())).toList();
        for (Map<String, Object> c : sourceContacts)
            listContacts.getChildren().add(buildContactRow(c));
        
        // Filtrer les étudiants
        listEtudiants.getChildren().clear();
        List<Map<String, Object>> sourceEtudiants = (query == null || query.isBlank())
            ? tousEtudiants
            : tousEtudiants.stream().filter(e -> fullName(e).toLowerCase().contains(query.toLowerCase())).toList();
        for (Map<String, Object> e : sourceEtudiants)
            listEtudiants.getChildren().add(buildEtudiantRow(e));
    }

    // ── Constructeurs de lignes ───────────────────────────────────────────────

    /** Ligne contact dans la sidebar. */
    private HBox buildContactRow(Map<String, Object> contact) {
        String name = fullName(contact);
        boolean isActive = contactActif != null &&
            contactActif.get("userId").equals(contact.get("userId"));

        // Avatar
        StackPane avatar = buildAvatar(name, "#7C3AED", "#A78BFA", 20);

        // Infos
        Label nom = new Label(name);
        nom.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:" +
                     (modeSombre ? "#F9FAFB" : "#1A1D23") + ";");

        Label statut = new Label("Disponible");
        statut.setStyle("-fx-font-size:11; -fx-text-fill:#22C55E;");

        VBox info = new VBox(2, nom, statut);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox row = new HBox(12, avatar, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle(isActive
            ? "-fx-background-color:#F0EBFF; -fx-cursor:hand;"
            : "-fx-background-color:transparent; -fx-cursor:hand;");

        row.setOnMouseEntered(e -> {
            if (!isActive) row.setStyle("-fx-background-color:#F9F8FF; -fx-cursor:hand;");
        });
        row.setOnMouseExited(e -> {
            if (!isActive) row.setStyle("-fx-background-color:transparent; -fx-cursor:hand;");
        });
        row.setOnMouseClicked(e -> ouvrirChat(contact));

        return row;
    }

    /** Ligne étudiant avec bouton Suivre. */
    private HBox buildEtudiantRow(Map<String, Object> etudiant) {
        String name = fullName(etudiant);
        int otherId = (int) etudiant.get("userId");

        StackPane avatar = buildAvatar(name, "#6366F1", "#818CF8", 20);

        Label nom = new Label(name);
        nom.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1A1D23;");

        Label niveau = new Label("Étudiant");
        niveau.setStyle("-fx-font-size:11; -fx-text-fill:#9CA3AF;");

        VBox info = new VBox(2, nom, niveau);
        HBox.setHgrow(info, Priority.ALWAYS);

        boolean seSuivent    = service.seSuivent(currentUser.getId(), otherId);
        boolean demandeEnvoyee = service.demandeFollowExiste(currentUser.getId(), otherId);

        Button btn;
        if (seSuivent) {
            btn = styledBtn("✓ Contacts", "#DCFCE7", "#16A34A", false);
        } else if (demandeEnvoyee) {
            btn = styledBtn("⏳ En attente", "#FEF3C7", "#D97706", false);
        } else {
            btn = styledBtn("+ Suivre", "#7C3AED", "white", true);
            btn.setOnAction(e -> {
                String senderName = currentUser.getPrenom() + " " + currentUser.getNom();
                boolean ok = service.envoyerDemandeFollow(currentUser.getId(), otherId, senderName);
                if (ok) {
                    btn.setText("⏳ En attente");
                    btn.setStyle(btn.getStyle().replace("#7C3AED", "#FEF3C7").replace("white", "#D97706"));
                    btn.setDisable(true);
                }
            });
        }

        HBox row = new HBox(12, avatar, info, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#F9F8FF;"));
        row.setOnMouseExited(e  -> row.setStyle("-fx-background-color:transparent;"));

        return row;
    }

    /** Ligne demande de follow. */
    private HBox buildDemandeRow(Map<String, Object> demande) {
        int notifId  = (int) demande.get("id");
        int senderId = (int) demande.getOrDefault("senderId", 0);
        String name  = (String) demande.getOrDefault("senderName", "Inconnu");

        StackPane avatar = buildAvatar(name, "#F59E0B", "#FCD34D", 20);

        Label nom = new Label(name);
        nom.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#1A1D23;");
        Label sub = new Label("veut vous suivre");
        sub.setStyle("-fx-font-size:11; -fx-text-fill:#9CA3AF;");
        VBox info = new VBox(2, nom, sub);
        HBox.setHgrow(info, Priority.ALWAYS);

        Button btnOk  = roundBtn("✓", "#22C55E");
        Button btnNon = roundBtn("✕", "#EF4444");

        btnOk.setOnAction(e -> {
            String receiverName = currentUser.getPrenom() + " " + currentUser.getNom();
            service.accepterFollow(notifId, senderId, receiverName, currentUser.getId());
            chargerDemandes(); chargerContacts();
        });
        btnNon.setOnAction(e -> { service.refuserFollow(notifId); chargerDemandes(); });

        HBox btns = new HBox(6, btnOk, btnNon);
        btns.setAlignment(Pos.CENTER);

        HBox row = new HBox(12, avatar, info, btns);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));

        return row;
    }

    // ── Chat ─────────────────────────────────────────────────────────────────

    private void ouvrirChat(Map<String, Object> contact) {
        contactActif = contact;
        dernierMessageId = 0;
        messagesAffiches.clear();
        labelVuMap.clear();

        String name = fullName(contact);
        labelNomContact.setText(name);
        labelAvatarChat.setText(initiales(name));
        int otherId = (int) contact.get("userId");
        String statut = service.getStatutEnLigne(otherId);
        labelStatutContact.setText(statut);
        // Color based on status
        if ("Actif maintenant".equals(statut)) {
            labelStatutContact.setStyle("-fx-font-size:11; -fx-text-fill:#22C55E; -fx-font-weight:600;");
        } else {
            labelStatutContact.setStyle("-fx-font-size:11; -fx-text-fill:#9CA3AF; -fx-font-weight:600;");
        }

        panneauVide.setVisible(false); panneauVide.setManaged(false);
        panneauChat.setVisible(true);  panneauChat.setManaged(true);

        chargerConversation();
        chargerContacts();
    }

    private void chargerConversation() {
        if (contactActif == null) return;
        int otherId = (int) contactActif.get("userId");

        List<Map<String, Object>> messages = service.getConversation(currentUser.getId(), otherId);
        containerMessages.getChildren().clear();
        messagesAffiches.clear();
        dernierMessageId = 0;

        // Séparateur de date
        ajouterSeparateurDate("Aujourd'hui");

        for (Map<String, Object> msg : messages) {
            ajouterBulle(msg);
            messagesAffiches.add(msg);
            int id = (int) msg.get("id");
            if (id > dernierMessageId) dernierMessageId = id;
        }

        scrollerEnBas();
        mettreAJourBadge();
    }

    /** Ajoute une bulle de message avec animation, support image/fichier, statut vu. */
    private void ajouterBulle(Map<String, Object> msg) {
        boolean isOwn    = (boolean) msg.getOrDefault("isOwn", false);
        String texte     = (String)  msg.getOrDefault("texte", "");
        String fileType  = (String)  msg.getOrDefault("fileType", null);
        String filePath  = (String)  msg.getOrDefault("filePath", null);
        int    msgId     = (int)     msg.getOrDefault("id", 0);
        Timestamp ts     = (Timestamp) msg.get("sentAt");
        String heure     = ts != null ? ts.toLocalDateTime().format(TIME_FMT) : "";

        // ── Contenu de la bulle ──────────────────────────────────────────────
        javafx.scene.Node contenuNode;

        if ("image".equals(fileType) && filePath != null) {
            // Afficher l'image
            try {
                javafx.scene.image.Image img = new javafx.scene.image.Image(
                    "file:///" + filePath.replace("\\", "/"), 260, 200, true, true);
                javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                iv.setFitWidth(260); iv.setPreserveRatio(true);
                iv.setStyle("-fx-background-radius:12;");
                // Clic pour agrandir
                iv.setOnMouseClicked(e -> agrandirImage(filePath));
                iv.setStyle("-fx-cursor:hand;");
                contenuNode = iv;
            } catch (Exception ex) {
                Label fallback = new Label("🖼 " + java.nio.file.Paths.get(filePath).getFileName());
                fallback.setStyle("-fx-text-fill:" + (isOwn ? "white" : "#1A1D23") + "; -fx-font-size:13;");
                contenuNode = fallback;
            }
        } else if ("file".equals(fileType) && filePath != null) {
            // Afficher le fichier comme carte
            String fileName = java.nio.file.Paths.get(filePath).getFileName().toString();
            HBox fileCard = new HBox(10);
            fileCard.setAlignment(Pos.CENTER_LEFT);
            fileCard.setPadding(new Insets(10, 14, 10, 14));
            fileCard.setStyle(isOwn
                ? "-fx-background-color:rgba(255,255,255,0.15); -fx-background-radius:12; -fx-cursor:hand;"
                : "-fx-background-color:#F3F4F6; -fx-background-radius:12; -fx-cursor:hand;");
            Label icone = new Label("📎");
            icone.setStyle("-fx-font-size:20;");
            VBox fileInfo = new VBox(2);
            Label nomFichier = new Label(fileName);
            nomFichier.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:" +
                                (isOwn ? "white" : "#1A1D23") + ";");
            Label ouvrir = new Label("Cliquer pour ouvrir");
            ouvrir.setStyle("-fx-font-size:10; -fx-text-fill:" + (isOwn ? "rgba(255,255,255,0.7)" : "#9CA3AF") + ";");
            fileInfo.getChildren().addAll(nomFichier, ouvrir);
            fileCard.getChildren().addAll(icone, fileInfo);
            fileCard.setOnMouseClicked(e -> ouvrirFichier(filePath));
            contenuNode = fileCard;
        } else {
            // Message texte normal
            boolean isDeleted = (boolean) msg.getOrDefault("deleted", false);
            if (isDeleted) {
                texte = "[Message supprimé]";
            }
            Label bulle = new Label(texte.isEmpty() ? "..." : texte);
            bulle.setWrapText(true);
            bulle.setMaxWidth(400);
            bulle.setPadding(new Insets(10, 14, 10, 14));
            if (isDeleted) {
                bulle.setStyle("-fx-background-color:#F3F4F6; -fx-text-fill:#9CA3AF; -fx-font-size:13;" +
                               "-fx-font-style:italic; -fx-background-radius:18 18 4 18;");
            } else {
                bulle.setStyle(isOwn
                    ? "-fx-background-color:linear-gradient(to bottom right,#7C3AED,#6D28D9);" +
                      "-fx-text-fill:white; -fx-font-size:13;" +
                      "-fx-background-radius:18 18 4 18;" +
                      "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.25),8,0,0,3);"
                    : "-fx-background-color:white; -fx-text-fill:#1A1D23; -fx-font-size:13;" +
                      "-fx-background-radius:18 18 18 4;" +
                      "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");
            }
            contenuNode = bulle;
            // Store reference for edit/delete
            if (isOwn && msgId > 0) {
                bulleTextMap.put(msgId, bulle);
                // Context menu on right-click
                javafx.scene.control.ContextMenu ctxMenu = new javafx.scene.control.ContextMenu();
                javafx.scene.control.MenuItem itemModifier = new javafx.scene.control.MenuItem("✏️  Modifier");
                javafx.scene.control.MenuItem itemSupprimer = new javafx.scene.control.MenuItem("🗑️  Supprimer");
                itemModifier.setOnAction(ev -> modifierMessageUI(msgId, bulle));
                itemSupprimer.setOnAction(ev -> supprimerMessageUI(msgId, bulle));
                ctxMenu.getItems().addAll(itemModifier, new javafx.scene.control.SeparatorMenuItem(), itemSupprimer);
                bulle.setOnContextMenuRequested(ev -> ctxMenu.show(bulle, ev.getScreenX(), ev.getScreenY()));
            }
        }

        // ── Métadonnées : heure + statut ────────────────────────────────────
        HBox meta = new HBox(5);
        meta.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label heureLabel = new Label(heure);
        heureLabel.setStyle("-fx-font-size:10; -fx-text-fill:#9CA3AF;");
        meta.getChildren().add(heureLabel);

        if (isOwn) {
            // Statut : ✔ envoyé → ✔✔ reçu → ✔✔ vu (violet)
            boolean vu = service.estVu(msgId);
            Label labelStatut = new Label(vu ? "✔✔ Vu" : "✔ Envoyé");
            labelStatut.setStyle("-fx-font-size:10; -fx-text-fill:" + (vu ? "#A78BFA" : "#9CA3AF") + ";");
            meta.getChildren().add(labelStatut);
            // Garder référence pour mise à jour dynamique
            if (msgId > 0) labelVuMap.put(msgId, labelStatut);
        }

        // ── Assemblage ───────────────────────────────────────────────────────
        VBox bulleBox = new VBox(4, contenuNode, meta);
        bulleBox.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bulleBox.setMaxWidth(460);

        HBox ligne;
        if (!isOwn && contactActif != null) {
            StackPane avatarSmall = buildAvatar(fullName(contactActif), "#7C3AED", "#A78BFA", 14);
            ligne = new HBox(8, avatarSmall, bulleBox);
            ligne.setAlignment(Pos.BOTTOM_LEFT);
        } else {
            ligne = new HBox(bulleBox);
            ligne.setAlignment(Pos.CENTER_RIGHT);
        }
        ligne.setPadding(new Insets(3, 0, 3, 0));

        // ── Animation d'apparition ───────────────────────────────────────────
        ligne.setOpacity(0);
        containerMessages.getChildren().add(ligne);

        FadeTransition ft = new FadeTransition(Duration.millis(220), ligne);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(220), ligne);
        tt.setFromY(isOwn ? 12 : -12); tt.setToY(0);
        new ParallelTransition(ft, tt).play();

        // ── Notification sonore pour messages reçus ──────────────────────────
        if (!isOwn) {
            java.awt.Toolkit.getDefaultToolkit().beep();
        }
    }

    /** Agrandit une image dans une nouvelle fenêtre. */
    private void agrandirImage(String filePath) {
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image("file:///" + filePath.replace("\\", "/"));
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitWidth(Math.min(img.getWidth(), 900));
            iv.setFitHeight(Math.min(img.getHeight(), 700));

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(java.nio.file.Paths.get(filePath).getFileName().toString());
            stage.setScene(new javafx.scene.Scene(new StackPane(iv)));
            stage.show();
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir l'image : " + e.getMessage());
        }
    }

    /** Ouvre un fichier avec l'application par défaut du système. */
    private void ouvrirFichier(String filePath) {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(filePath));
        } catch (Exception e) {
            System.err.println("Impossible d'ouvrir le fichier : " + e.getMessage());
        }
    }

    /** Séparateur de date entre les messages. */
    private void ajouterSeparateurDate(String texte) {
        Label sep = new Label(texte);
        sep.setStyle("-fx-font-size:11; -fx-text-fill:#9CA3AF; -fx-font-weight:600;" +
                     "-fx-background-color:#E5E7EB; -fx-background-radius:20;" +
                     "-fx-padding:4 14 4 14;");
        HBox wrapper = new HBox(sep);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPadding(new Insets(8, 0, 8, 0));
        containerMessages.getChildren().add(wrapper);
    }

    @FXML
    private void onEnvoyerMessage() {
        if (contactActif == null) return;
        String texte = champMessage.getText().trim();
        if (texte.isEmpty()) return;

        int otherId = (int) contactActif.get("userId");
        String senderName = currentUser.getPrenom() + " " + currentUser.getNom();

        service.envoyerMessage(currentUser.getId(), otherId, senderName, texte);
        service.mettreAJourActivite(currentUser.getId());
        champMessage.clear();

        // Afficher immédiatement avec statut "✔ Envoyé"
        Map<String, Object> msgLocal = new HashMap<>();
        msgLocal.put("id", ++dernierMessageId);
        msgLocal.put("texte", texte);
        msgLocal.put("sentAt", new Timestamp(System.currentTimeMillis()));
        msgLocal.put("isOwn", true);
        ajouterBulle(msgLocal);
        messagesAffiches.add(msgLocal);
        scrollerEnBas();

        // Animation bouton envoyer
        ScaleTransition st = new ScaleTransition(Duration.millis(100), btnEnvoyer);
        st.setFromX(1); st.setToX(0.85);
        st.setAutoReverse(true); st.setCycleCount(2);
        st.play();
    }

    /** Ouvre le FileChooser pour envoyer une image ou un fichier. */
    @FXML
    private void onEnvoyerFichier() {
        if (contactActif == null) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Choisir un fichier à envoyer");
        fc.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"),
            new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.xlsx"),
            new javafx.stage.FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        java.io.File file = fc.showOpenDialog(btnEnvoyer.getScene().getWindow());
        if (file == null) return;

        String filePath = file.getAbsolutePath();
        String fileName = file.getName().toLowerCase();
        boolean isImage = fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                          fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                          fileName.endsWith(".bmp") || fileName.endsWith(".webp");

        if (isImage) {
            // Show preview dialog before sending
            afficherPreviewImage(filePath);
        } else {
            // Send file directly
            envoyerFichierConfirme(filePath, "file");
        }
    }

    /** Affiche une prévisualisation de l'image avant envoi. */
    private void afficherPreviewImage(String filePath) {
        try {
            javafx.scene.image.Image img = new javafx.scene.image.Image(
                "file:///" + filePath.replace("\\", "/"), 500, 400, true, true);
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
            iv.setPreserveRatio(true);
            iv.setFitWidth(400);
            iv.setFitHeight(300);

            // Caption field
            javafx.scene.control.TextField captionField = new javafx.scene.control.TextField();
            captionField.setPromptText("Ajouter une légende (optionnel)...");
            captionField.setStyle("-fx-background-color:#F5F6FA; -fx-border-color:#E5E7EB;" +
                                  "-fx-border-radius:8; -fx-background-radius:8;" +
                                  "-fx-padding:8 12 8 12; -fx-font-size:13;");

            // File name label
            javafx.scene.control.Label fileNameLabel = new javafx.scene.control.Label(
                "📷  " + java.nio.file.Paths.get(filePath).getFileName().toString());
            fileNameLabel.setStyle("-fx-font-size:12; -fx-text-fill:#6B7280; -fx-font-weight:600;");

            VBox content = new VBox(12,
                fileNameLabel,
                iv,
                captionField
            );
            content.setAlignment(javafx.geometry.Pos.CENTER);
            content.setPadding(new javafx.geometry.Insets(16));
            content.setStyle("-fx-background-color:white;");

            // Custom dialog
            javafx.scene.control.Dialog<Boolean> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Aperçu avant envoi");
            dialog.setHeaderText(null);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setStyle("-fx-background-color:white; -fx-padding:0;");

            // Buttons
            javafx.scene.control.ButtonType btnEnvoyerType = new javafx.scene.control.ButtonType(
                "📤  Envoyer", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            javafx.scene.control.ButtonType btnAnnulerType = new javafx.scene.control.ButtonType(
                "Annuler", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(btnEnvoyerType, btnAnnulerType);

            // Style the send button
            javafx.scene.Node sendBtn = dialog.getDialogPane().lookupButton(btnEnvoyerType);
            sendBtn.setStyle("-fx-background-color:#7C3AED; -fx-text-fill:white; -fx-font-weight:700;" +
                             "-fx-background-radius:8; -fx-padding:8 20 8 20; -fx-cursor:hand;");

            dialog.setResultConverter(btn -> btn == btnEnvoyerType);

            dialog.showAndWait().ifPresent(confirmed -> {
                if (confirmed) {
                    String caption = captionField.getText().trim();
                    envoyerFichierConfirme(filePath, "image");
                    // If caption, send as additional text message
                    if (!caption.isEmpty()) {
                        int otherId = (int) contactActif.get("userId");
                        String senderName = currentUser.getPrenom() + " " + currentUser.getNom();
                        service.envoyerMessage(currentUser.getId(), otherId, senderName, caption);
                        Map<String, Object> captionMsg = new HashMap<>();
                        captionMsg.put("id", ++dernierMessageId);
                        captionMsg.put("texte", caption);
                        captionMsg.put("sentAt", new java.sql.Timestamp(System.currentTimeMillis()));
                        captionMsg.put("isOwn", true);
                        ajouterBulle(captionMsg);
                        messagesAffiches.add(captionMsg);
                    }
                    scrollerEnBas();
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur preview image: " + e.getMessage());
            envoyerFichierConfirme(filePath, "image");
        }
    }

    /** Envoie effectivement le fichier après confirmation. */
    private void envoyerFichierConfirme(String filePath, String fileType) {
        int otherId = (int) contactActif.get("userId");
        String senderName = currentUser.getPrenom() + " " + currentUser.getNom();

        service.envoyerMessageComplet(currentUser.getId(), otherId, senderName, "", fileType, filePath);
        service.mettreAJourActivite(currentUser.getId());

        Map<String, Object> msgLocal = new HashMap<>();
        msgLocal.put("id", ++dernierMessageId);
        msgLocal.put("texte", "");
        msgLocal.put("fileType", fileType);
        msgLocal.put("filePath", filePath);
        msgLocal.put("sentAt", new java.sql.Timestamp(System.currentTimeMillis()));
        msgLocal.put("isOwn", true);
        ajouterBulle(msgLocal);
        messagesAffiches.add(msgLocal);
        scrollerEnBas();
    }

    // ── Recherche dans messages ───────────────────────────────────────────────

    @FXML private void onRechercheMessages() {
        barreRechercheMsg.setVisible(true); barreRechercheMsg.setManaged(true);
        champRechercheMsg.requestFocus();
    }

    @FXML private void onFermerRechercheMessages() {
        barreRechercheMsg.setVisible(false); barreRechercheMsg.setManaged(false);
        champRechercheMsg.clear();
        afficherTousMessages();
    }

    private void rechercherDansMessages(String query) {
        if (query == null || query.isBlank()) { 
            afficherTousMessages(); 
            return; 
        }
        containerMessages.getChildren().clear();
        ajouterSeparateurDate("Résultats pour \"" + query + "\"");
        String q = query.toLowerCase();
        int count = 0;
        for (Map<String, Object> msg : messagesAffiches) {
            String texte = (String) msg.getOrDefault("texte", "");
            if (texte.toLowerCase().contains(q)) {
                ajouterBulle(msg);
                count++;
            }
        }
        
        // Si aucun résultat trouvé
        if (count == 0) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(javafx.geometry.Pos.CENTER);
            emptyBox.setPadding(new javafx.geometry.Insets(40));
            
            Label emptyIcon = new Label("🔍");
            emptyIcon.setStyle("-fx-font-size:48;");
            
            Label emptyText = new Label("Aucun message trouvé");
            emptyText.setStyle("-fx-font-size:15; -fx-font-weight:700; -fx-text-fill:#6B7280;");
            
            Label emptyHint = new Label("Essayez avec d'autres mots-clés");
            emptyHint.setStyle("-fx-font-size:12; -fx-text-fill:#9CA3AF;");
            
            emptyBox.getChildren().addAll(emptyIcon, emptyText, emptyHint);
            containerMessages.getChildren().add(emptyBox);
        }
        
        scrollerEnBas();
    }

    private void afficherTousMessages() {
        containerMessages.getChildren().clear();
        ajouterSeparateurDate("Aujourd'hui");
        for (Map<String, Object> msg : messagesAffiches) ajouterBulle(msg);
        scrollerEnBas();
    }

    // ── Mode sombre ───────────────────────────────────────────────────────────

    @FXML private void onToggleModeSombre() {
        modeSombre = !modeSombre;
        String bg = modeSombre ? BG_SOMBRE : BG_CLAIR;
        containerMessages.setStyle("-fx-padding:16 20 16 20; -fx-background-color:" + bg + ";");
        scrollMessages.setStyle("-fx-background-color:" + bg + "; -fx-background:" + bg + "; -fx-border-width:0;");
        panneauChat.setStyle("-fx-background-color:" + bg + ";");
        panneauVide.setStyle("-fx-background-color:" + bg + ";");
    }

    // ── Emoji Picker ──────────────────────────────────────────────────────────

    /** Emojis organisés par catégorie. */
    private static final String[][] EMOJIS = {
        // Visages
        {"😊","😂","😍","🥰","😎","😢","😡","🤔","😴","🤩","😇","🥳","😅","🤣","😆"},
        // Gestes
        {"👍","👎","👏","🙌","🤝","✌️","🤞","👋","🙏","💪","🤜","🤛","👊","✊","🖐"},
        // Cœurs
        {"❤️","🧡","💛","💚","💙","💜","🖤","🤍","💕","💞","💓","💗","💖","💝","💘"},
        // Objets
        {"🎉","🎊","🎁","🏆","⭐","🔥","💡","📚","💻","📱","🎮","🎵","🎶","🌟","✨"},
        // Nature
        {"🌸","🌺","🌻","🌹","🍀","🌈","☀️","🌙","⭐","🌊","🏔️","🌴","🦋","🐶","🐱"},
    };

    private javafx.stage.Popup emojiPopup;

    @FXML
    private void onOuvrirEmojis() {
        if (emojiPopup != null && emojiPopup.isShowing()) {
            emojiPopup.hide();
            return;
        }

        emojiPopup = new javafx.stage.Popup();
        emojiPopup.setAutoHide(true);

        // Conteneur principal du picker
        VBox picker = new VBox(8);
        picker.setStyle(
            "-fx-background-color:white; -fx-background-radius:16;" +
            "-fx-border-color:#E5E7EB; -fx-border-radius:16; -fx-border-width:1;" +
            "-fx-padding:12; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.18),20,0,0,6);"
        );
        picker.setPrefWidth(320);

        // Titre
        Label titre = new Label("😊  Emojis");
        titre.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#374151; -fx-padding:0 0 4 0;");
        picker.getChildren().add(titre);

        // Grille d'emojis par catégorie
        String[] categories = {"Visages", "Gestes", "Cœurs", "Objets", "Nature"};
        for (int cat = 0; cat < EMOJIS.length; cat++) {
            Label catLabel = new Label(categories[cat]);
            catLabel.setStyle("-fx-font-size:10; -fx-text-fill:#9CA3AF; -fx-font-weight:700; -fx-padding:4 0 2 0;");
            picker.getChildren().add(catLabel);

            javafx.scene.layout.FlowPane grille = new javafx.scene.layout.FlowPane();
            grille.setHgap(2); grille.setVgap(2);
            grille.setPrefWrapLength(300);

            for (String emoji : EMOJIS[cat]) {
                Button btnE = new Button(emoji);
                btnE.setStyle(
                    "-fx-background-color:transparent; -fx-font-size:20;" +
                    "-fx-cursor:hand; -fx-border-width:0; -fx-padding:4 5 4 5;" +
                    "-fx-background-radius:8;"
                );
                // Hover
                btnE.setOnMouseEntered(e ->
                    btnE.setStyle("-fx-background-color:#F3F0FF; -fx-font-size:20;" +
                                  "-fx-cursor:hand; -fx-border-width:0; -fx-padding:4 5 4 5;" +
                                  "-fx-background-radius:8;"));
                btnE.setOnMouseExited(e ->
                    btnE.setStyle("-fx-background-color:transparent; -fx-font-size:20;" +
                                  "-fx-cursor:hand; -fx-border-width:0; -fx-padding:4 5 4 5;" +
                                  "-fx-background-radius:8;"));
                // Clic → insérer l'emoji dans le champ
                btnE.setOnAction(e -> {
                    int pos = champMessage.getCaretPosition();
                    String current = champMessage.getText();
                    champMessage.setText(current.substring(0, pos) + emoji + current.substring(pos));
                    champMessage.positionCaret(pos + emoji.length());
                    champMessage.requestFocus();
                    emojiPopup.hide();
                });
                grille.getChildren().add(btnE);
            }
            picker.getChildren().add(grille);
        }

        emojiPopup.getContent().add(picker);

        // Positionner le popup au-dessus du bouton emoji
        javafx.geometry.Bounds bounds = btnEmoji.localToScreen(btnEmoji.getBoundsInLocal());
        emojiPopup.show(
            btnEmoji.getScene().getWindow(),
            bounds.getMinX() - 10,
            bounds.getMinY() - picker.getPrefHeight() - 340
        );
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    @FXML private void onNotifications() {
        int nb = service.getNombreMessagesNonLus(currentUser.getId());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("🔔 Notifications");
        alert.setContentText(nb > 0
            ? "Vous avez " + nb + " message(s) non lu(s)."
            : "Aucune nouvelle notification.");
        alert.showAndWait();
    }

    // ── Polling temps réel ────────────────────────────────────────────────────

    private void demarrerPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e ->
            Platform.runLater(() -> {
                if (contactActif != null) {
                    int otherId = (int) contactActif.get("userId");

                    // Refresh contact status
                    String statut = service.getStatutEnLigne(otherId);
                    Platform.runLater(() -> {
                        if (labelStatutContact != null) {
                            labelStatutContact.setText(statut);
                            if ("Actif maintenant".equals(statut)) {
                                labelStatutContact.setStyle("-fx-font-size:11; -fx-text-fill:#22C55E; -fx-font-weight:600;");
                            } else {
                                labelStatutContact.setStyle("-fx-font-size:11; -fx-text-fill:#9CA3AF; -fx-font-weight:600;");
                            }
                        }
                    });

                    // Nouveaux messages reçus
                    List<Map<String, Object>> nouveaux =
                        service.getNouveauxMessages(currentUser.getId(), otherId, dernierMessageId);
                    if (!nouveaux.isEmpty()) {
                        for (Map<String, Object> msg : nouveaux) {
                            ajouterBulle(msg);
                            messagesAffiches.add(msg);
                            int id = (int) msg.get("id");
                            if (id > dernierMessageId) dernierMessageId = id;
                        }
                        scrollerEnBas();
                        // Masquer "est en train d'écrire" après réception
                        indicateurEcriture.setVisible(false);
                        indicateurEcriture.setManaged(false);
                        typingEnCours = false;
                    }

                    // Simuler "est en train d'écrire" si le contact a tapé récemment
                    // (détection : message en BDD créé il y a < 5s mais pas encore dans notre liste)
                    // → On vérifie si un nouveau message est en cours d'écriture côté contact
                    // Pour la simulation : on affiche l'indicateur aléatoirement si pas de nouveaux msgs
                    // En production réelle, on utiliserait un signal "typing" en BDD

                    // Mise à jour des statuts "vu" pour les messages envoyés
                    for (Map.Entry<Integer, Label> entry : labelVuMap.entrySet()) {
                        int notifId = entry.getKey();
                        Label lbl = entry.getValue();
                        if (!"✔✔ Vu".equals(lbl.getText())) {
                            boolean vu = service.estVu(notifId);
                            if (vu) {
                                lbl.setText("✔✔ Vu");
                                lbl.setStyle("-fx-font-size:10; -fx-text-fill:#A78BFA;");
                            }
                        }
                    }
                }

                chargerDemandes();
                mettreAJourBadge();

                // Rafraîchir contacts si nouveau follow accepté
                List<Map<String, Object>> newContacts = service.getContacts(currentUser.getId());
                if (newContacts.size() != tousContacts.size()) {
                    tousContacts = newContacts;
                    chargerContacts();
                }
            })
        ));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    public void arreterPolling() {
        if (pollingTimeline != null) pollingTimeline.stop();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void scrollerEnBas() {
        Platform.runLater(() -> {
            scrollMessages.applyCss();
            scrollMessages.layout();
            scrollMessages.setVvalue(scrollMessages.getVmax());
            // Smooth scroll style WhatsApp
            Timeline t = new Timeline(
                new KeyFrame(Duration.millis(200),
                    new KeyValue(scrollMessages.vvalueProperty(), 1.0,
                        Interpolator.EASE_OUT))
            );
            t.play();
        });
    }

    /** Construit un avatar circulaire avec dégradé et initiales. */
    private StackPane buildAvatar(String name, String color1, String color2, double radius) {
        Circle bg = new Circle(radius);
        bg.setStyle("-fx-fill:linear-gradient(to bottom right," + color1 + "," + color2 + ");");

        Label initLabel = new Label(initiales(name));
        initLabel.setStyle("-fx-text-fill:white; -fx-font-weight:800; -fx-font-size:" +
                           (int)(radius * 0.7) + ";");

        StackPane sp = new StackPane(bg, initLabel);
        sp.setMinWidth(radius * 2); sp.setMinHeight(radius * 2);
        sp.setMaxWidth(radius * 2); sp.setMaxHeight(radius * 2);
        return sp;
    }

    private Button styledBtn(String text, String bg, String fg, boolean cursor) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + ";" +
                     "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:16;" +
                     "-fx-padding:5 14 5 14; -fx-border-width:0;" +
                     (cursor ? "-fx-cursor:hand;" : "-fx-cursor:default;"));
        if (!cursor) btn.setDisable(true);
        return btn;
    }

    private Button roundBtn(String text, String bg) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:white;" +
                     "-fx-font-size:13; -fx-font-weight:700; -fx-background-radius:50%;" +
                     "-fx-min-width:32; -fx-min-height:32; -fx-border-width:0; -fx-cursor:hand;");
        return btn;
    }

    private String fullName(Map<String, Object> m) {
        String p = (String) m.getOrDefault("prenom", "");
        String n = (String) m.getOrDefault("nom", "");
        return (p + " " + n).trim();
    }

    private String initiales(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase() +
                   String.valueOf(parts[1].charAt(0)).toUpperCase();
        return String.valueOf(name.charAt(0)).toUpperCase();
    }

    /** Modifier un message — ouvre un dialog de saisie. */
    private void modifierMessageUI(int notifId, Label bulleLabel) {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog(
            bulleLabel.getText().replace(" (modifié)", "")
        );
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText("✏️  Modifier votre message");
        dialog.setContentText("Nouveau texte :");
        // Style du dialog
        dialog.getDialogPane().setStyle("-fx-font-size:13;");
        dialog.showAndWait().ifPresent(nouveauTexte -> {
            if (!nouveauTexte.isBlank()) {
                boolean ok = service.modifierMessage(notifId, nouveauTexte);
                if (ok) {
                    bulleLabel.setText(nouveauTexte + " (modifié)");
                }
            }
        });
    }

    /** Supprimer un message — confirmation puis soft delete. */
    private void supprimerMessageUI(int notifId, Label bulleLabel) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION
        );
        confirm.setTitle("Supprimer le message");
        confirm.setHeaderText("🗑️  Supprimer ce message ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                boolean ok = service.supprimerMessage(notifId);
                if (ok) {
                    bulleLabel.setText("[Message supprimé]");
                    bulleLabel.setStyle("-fx-background-color:#F3F4F6; -fx-text-fill:#9CA3AF;" +
                                        "-fx-font-size:13; -fx-font-style:italic;" +
                                        "-fx-background-radius:18 18 4 18; -fx-padding:10 14 10 14;");
                }
            }
        });
    }
}
