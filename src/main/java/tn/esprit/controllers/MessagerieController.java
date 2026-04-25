package tn.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
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
    @FXML private VBox      panneauVide, panneauChat;
    @FXML private Label     labelAvatarChat, labelNomContact, labelStatutContact;
    @FXML private ScrollPane scrollMessages;
    @FXML private VBox      containerMessages;
    @FXML private TextField champMessage;
    @FXML private Button    btnEnvoyer;
    @FXML private Button    btnEmoji;
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
    private boolean modeSombre = false;

    // Couleurs mode clair / sombre
    private static final String BG_CLAIR  = "#F5F6FA";
    private static final String BG_SOMBRE = "#1A1D23";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // Cache des messages affichés (pour la recherche)
    private final List<Map<String, Object>> messagesAffiches = new ArrayList<>();

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

        // Indicateur "est en train d'écrire" simulé
        champMessage.textProperty().addListener((obs, old, val) -> {
            if (contactActif != null && !val.isEmpty()) {
                // On pourrait envoyer un signal "typing" — ici on simule juste l'affichage
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
        listContacts.getChildren().clear();
        List<Map<String, Object>> source = (query == null || query.isBlank())
            ? tousContacts
            : tousContacts.stream().filter(c -> fullName(c).toLowerCase().contains(query.toLowerCase())).toList();
        for (Map<String, Object> c : source)
            listContacts.getChildren().add(buildContactRow(c));
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

        String name = fullName(contact);
        labelNomContact.setText(name);
        labelAvatarChat.setText(initiales(name));
        labelStatutContact.setText("Actif maintenant");

        panneauVide.setVisible(false); panneauVide.setManaged(false);
        panneauChat.setVisible(true);  panneauChat.setManaged(true);

        chargerConversation();
        chargerContacts(); // Rafraîchir la sélection active
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

    /** Ajoute une bulle de message avec animation. */
    private void ajouterBulle(Map<String, Object> msg) {
        boolean isOwn = (boolean) msg.getOrDefault("isOwn", false);
        String texte  = (String) msg.getOrDefault("texte", "");
        Timestamp ts  = (Timestamp) msg.get("sentAt");
        String heure  = ts != null ? ts.toLocalDateTime().format(TIME_FMT) : "";

        // Bulle principale
        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(420);
        bulle.setPadding(new Insets(10, 14, 10, 14));

        if (isOwn) {
            bulle.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#7C3AED,#6D28D9);" +
                "-fx-text-fill:white; -fx-font-size:13;" +
                "-fx-background-radius:18 18 4 18;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.25),8,0,0,3);");
        } else {
            bulle.setStyle(
                "-fx-background-color:white; -fx-text-fill:#1A1D23; -fx-font-size:13;" +
                "-fx-background-radius:18 18 18 4;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),8,0,0,2);");
        }

        // Heure + statut "Vu"
        HBox meta = new HBox(6);
        meta.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label heureLabel = new Label(heure);
        heureLabel.setStyle("-fx-font-size:10; -fx-text-fill:#9CA3AF;");
        meta.getChildren().add(heureLabel);

        if (isOwn) {
            Label vu = new Label("Vu ✓");
            vu.setStyle("-fx-font-size:10; -fx-text-fill:#A78BFA;");
            meta.getChildren().add(vu);
        }

        VBox bulleBox = new VBox(4, bulle, meta);
        bulleBox.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bulleBox.setMaxWidth(460);

        // Avatar pour les messages reçus
        HBox ligne;
        if (!isOwn) {
            StackPane avatarSmall = buildAvatar(fullName(contactActif), "#7C3AED", "#A78BFA", 14);
            ligne = new HBox(8, avatarSmall, bulleBox);
            ligne.setAlignment(Pos.BOTTOM_LEFT);
        } else {
            ligne = new HBox(bulleBox);
            ligne.setAlignment(Pos.CENTER_RIGHT);
        }
        ligne.setPadding(new Insets(3, 0, 3, 0));

        // Animation d'apparition
        ligne.setOpacity(0);
        containerMessages.getChildren().add(ligne);
        FadeTransition ft = new FadeTransition(Duration.millis(200), ligne);
        ft.setFromValue(0); ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(200), ligne);
        tt.setFromY(isOwn ? 10 : -10); tt.setToY(0);

        new ParallelTransition(ft, tt).play();
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
        champMessage.clear();

        // Afficher immédiatement
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
        if (query == null || query.isBlank()) { afficherTousMessages(); return; }
        containerMessages.getChildren().clear();
        ajouterSeparateurDate("Résultats pour \"" + query + "\"");
        String q = query.toLowerCase();
        for (Map<String, Object> msg : messagesAffiches) {
            String texte = (String) msg.getOrDefault("texte", "");
            if (texte.toLowerCase().contains(q)) ajouterBulle(msg);
        }
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
        // Appliquer le fond sur les conteneurs principaux
        String bg = modeSombre ? BG_SOMBRE : BG_CLAIR;
        containerMessages.setStyle("-fx-padding:20 24 12 24; -fx-background-color:" + bg + ";");
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
                        // Simuler "est en train d'écrire" → masquer après réception
                        indicateurEcriture.setVisible(false);
                        indicateurEcriture.setManaged(false);
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
            scrollMessages.setVvalue(1.0);
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
}
