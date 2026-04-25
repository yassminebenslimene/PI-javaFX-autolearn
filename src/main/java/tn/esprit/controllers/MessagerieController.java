package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.User;
import tn.esprit.services.MessagerieService;
import tn.esprit.session.SessionManager;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * MessagerieController — Contrôleur de la messagerie en temps réel.
 *
 * Fonctionnalités :
 *   - Liste des étudiants avec bouton "Suivre"
 *   - Demandes de follow reçues (accepter / refuser)
 *   - Chat privé avec les contacts (polling toutes les 2s)
 */
public class MessagerieController {

    // ── FXML ─────────────────────────────────────────────────────────────────

    @FXML private TabPane   tabPane;
    @FXML private Tab       tabContacts, tabEtudiants, tabDemandes;

    // Contacts
    @FXML private ListView<Map<String, Object>> listContacts;
    @FXML private TextField searchContacts;

    // Étudiants
    @FXML private ListView<Map<String, Object>> listEtudiants;
    @FXML private TextField searchEtudiants;

    // Demandes
    @FXML private ListView<Map<String, Object>> listDemandes;

    // Badge non lus
    @FXML private Label badgeNonLus;

    // Chat
    @FXML private VBox  panneauVide;
    @FXML private VBox  panneauChat;
    @FXML private Label labelAvatarChat;
    @FXML private Label labelNomContact;
    @FXML private Label labelStatutContact;
    @FXML private Label labelRefresh;
    @FXML private ScrollPane scrollMessages;
    @FXML private VBox  containerMessages;
    @FXML private TextField champMessage;
    @FXML private Button btnEnvoyer;

    // ── État interne ──────────────────────────────────────────────────────────

    private final MessagerieService service = new MessagerieService();
    private User currentUser;

    /** Contact actuellement ouvert dans le chat. */
    private Map<String, Object> contactActif = null;

    /** ID du dernier message affiché (pour le polling incrémental). */
    private int dernierMessageId = 0;

    /** Timeline de polling (rafraîchissement toutes les 2 secondes). */
    private Timeline pollingTimeline;

    /** Formatter pour l'heure des messages. */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) return;

        configurerListViews();
        chargerContacts();
        chargerEtudiants();
        chargerDemandes();
        mettreAJourBadge();
        demarrerPolling();

        // Recherche en temps réel dans les contacts
        searchContacts.textProperty().addListener((obs, old, val) -> filtrerContacts(val));
        searchEtudiants.textProperty().addListener((obs, old, val) -> filtrerEtudiants(val));

        // Touche Entrée pour envoyer
        champMessage.setOnAction(e -> onEnvoyerMessage());
    }

    // ── Configuration des ListViews ───────────────────────────────────────────

    private void configurerListViews() {
        // ListView Contacts
        listContacts.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                setGraphic(buildContactCell(item, true));
                setStyle("-fx-background-color:transparent; -fx-padding:0;");
            }
        });
        listContacts.setOnMouseClicked(e -> {
            Map<String, Object> sel = listContacts.getSelectionModel().getSelectedItem();
            if (sel != null) ouvrirChat(sel);
        });

        // ListView Étudiants
        listEtudiants.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                setGraphic(buildEtudiantCell(item));
                setStyle("-fx-background-color:transparent; -fx-padding:0;");
            }
        });

        // ListView Demandes
        listDemandes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                setGraphic(buildDemandeCell(item));
                setStyle("-fx-background-color:transparent; -fx-padding:0;");
            }
        });
    }

    // ── Chargement des données ────────────────────────────────────────────────

    private List<Map<String, Object>> tousContacts   = List.of();
    private List<Map<String, Object>> tousEtudiants  = List.of();

    private void chargerContacts() {
        tousContacts = service.getContacts(currentUser.getId());
        listContacts.getItems().setAll(tousContacts);
    }

    private void chargerEtudiants() {
        tousEtudiants = service.getTousLesEtudiants(currentUser.getId());
        listEtudiants.getItems().setAll(tousEtudiants);
    }

    private void chargerDemandes() {
        List<Map<String, Object>> demandes = service.getDemandesFollowEnAttente(currentUser.getId());
        listDemandes.getItems().setAll(demandes);
        // Mettre à jour le texte de l'onglet
        tabDemandes.setText("Demandes" + (demandes.isEmpty() ? "" : " (" + demandes.size() + ")"));
    }

    private void mettreAJourBadge() {
        int nb = service.getNombreMessagesNonLus(currentUser.getId());
        if (nb > 0) {
            badgeNonLus.setText(String.valueOf(nb));
            badgeNonLus.setVisible(true);
            badgeNonLus.setManaged(true);
        } else {
            badgeNonLus.setVisible(false);
            badgeNonLus.setManaged(false);
        }
    }

    private void filtrerContacts(String query) {
        if (query == null || query.isBlank()) {
            listContacts.getItems().setAll(tousContacts);
            return;
        }
        String q = query.toLowerCase();
        listContacts.getItems().setAll(
            tousContacts.stream()
                .filter(c -> fullName(c).toLowerCase().contains(q))
                .toList()
        );
    }

    private void filtrerEtudiants(String query) {
        if (query == null || query.isBlank()) {
            listEtudiants.getItems().setAll(tousEtudiants);
            return;
        }
        String q = query.toLowerCase();
        listEtudiants.getItems().setAll(
            tousEtudiants.stream()
                .filter(e -> fullName(e).toLowerCase().contains(q))
                .toList()
        );
    }

    // ── Cellules personnalisées ───────────────────────────────────────────────

    /** Cellule pour un contact (avec qui on peut chatter). */
    private HBox buildContactCell(Map<String, Object> contact, boolean isContact) {
        String name = fullName(contact);
        String initials = initiales(name);

        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color:#7c3aed; -fx-text-fill:white; -fx-font-weight:700;" +
                        "-fx-font-size:13; -fx-background-radius:50%; -fx-padding:7 9 7 9;" +
                        "-fx-min-width:36; -fx-min-height:36;");

        Label nom = new Label(name);
        nom.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#0f172a;");

        Label statut = new Label("● Connecté");
        statut.setStyle("-fx-font-size:10; -fx-text-fill:#22c55e;");

        VBox info = new VBox(2, nom, statut);
        info.setAlignment(Pos.CENTER_LEFT);

        HBox cell = new HBox(10, avatar, info);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10, 14, 10, 14));
        cell.setStyle("-fx-cursor:hand;");

        // Hover
        cell.setOnMouseEntered(e -> cell.setStyle("-fx-background-color:#f3f0ff; -fx-cursor:hand;"));
        cell.setOnMouseExited(e  -> cell.setStyle("-fx-background-color:transparent; -fx-cursor:hand;"));

        return cell;
    }

    /** Cellule pour un étudiant (avec bouton Suivre). */
    private HBox buildEtudiantCell(Map<String, Object> etudiant) {
        String name = fullName(etudiant);
        int otherId = (int) etudiant.get("userId");

        Label avatar = new Label(initiales(name));
        avatar.setStyle("-fx-background-color:#e0d9ff; -fx-text-fill:#7c3aed; -fx-font-weight:700;" +
                        "-fx-font-size:13; -fx-background-radius:50%; -fx-padding:7 9 7 9;" +
                        "-fx-min-width:36; -fx-min-height:36;");

        Label nom = new Label(name);
        nom.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Déterminer l'état du bouton
        boolean seSuivent = service.seSuivent(currentUser.getId(), otherId);
        boolean demandeEnvoyee = service.demandeFollowExiste(currentUser.getId(), otherId);

        Button btnFollow;
        if (seSuivent) {
            btnFollow = new Button("✓ Contacts");
            btnFollow.setStyle("-fx-background-color:#dcfce7; -fx-text-fill:#16a34a;" +
                               "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:16;" +
                               "-fx-padding:5 12 5 12; -fx-border-width:0; -fx-cursor:default;");
            btnFollow.setDisable(true);
        } else if (demandeEnvoyee) {
            btnFollow = new Button("⏳ En attente");
            btnFollow.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                               "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:16;" +
                               "-fx-padding:5 12 5 12; -fx-border-width:0; -fx-cursor:default;");
            btnFollow.setDisable(true);
        } else {
            btnFollow = new Button("+ Suivre");
            btnFollow.setStyle("-fx-background-color:#7c3aed; -fx-text-fill:white;" +
                               "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:16;" +
                               "-fx-padding:5 12 5 12; -fx-border-width:0; -fx-cursor:hand;");
            btnFollow.setOnAction(e -> {
                String senderName = currentUser.getPrenom() + " " + currentUser.getNom();
                boolean ok = service.envoyerDemandeFollow(currentUser.getId(), otherId, senderName);
                if (ok) {
                    btnFollow.setText("⏳ En attente");
                    btnFollow.setStyle("-fx-background-color:#fef3c7; -fx-text-fill:#d97706;" +
                                       "-fx-font-size:11; -fx-font-weight:700; -fx-background-radius:16;" +
                                       "-fx-padding:5 12 5 12; -fx-border-width:0; -fx-cursor:default;");
                    btnFollow.setDisable(true);
                    showInfo("Demande envoyée à " + name + " !");
                }
            });
        }

        HBox cell = new HBox(10, avatar, nom, spacer, btnFollow);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10, 14, 10, 14));

        return cell;
    }

    /** Cellule pour une demande de follow reçue. */
    private HBox buildDemandeCell(Map<String, Object> demande) {
        int notifId  = (int) demande.get("id");
        int senderId = (int) demande.getOrDefault("senderId", 0);
        String name  = (String) demande.getOrDefault("senderName", "Inconnu");

        Label avatar = new Label(initiales(name));
        avatar.setStyle("-fx-background-color:#fde68a; -fx-text-fill:#92400e; -fx-font-weight:700;" +
                        "-fx-font-size:13; -fx-background-radius:50%; -fx-padding:7 9 7 9;" +
                        "-fx-min-width:36; -fx-min-height:36;");

        Label nom = new Label(name);
        nom.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#0f172a;");

        Label sousTitre = new Label("veut vous suivre");
        sousTitre.setStyle("-fx-font-size:11; -fx-text-fill:#94a3b8;");

        VBox info = new VBox(2, nom, sousTitre);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAccepter = new Button("✓");
        btnAccepter.setStyle("-fx-background-color:#22c55e; -fx-text-fill:white;" +
                             "-fx-font-size:13; -fx-font-weight:700; -fx-background-radius:50%;" +
                             "-fx-min-width:32; -fx-min-height:32; -fx-border-width:0; -fx-cursor:hand;");

        Button btnRefuser = new Button("✕");
        btnRefuser.setStyle("-fx-background-color:#ef4444; -fx-text-fill:white;" +
                            "-fx-font-size:13; -fx-font-weight:700; -fx-background-radius:50%;" +
                            "-fx-min-width:32; -fx-min-height:32; -fx-border-width:0; -fx-cursor:hand;");

        btnAccepter.setOnAction(e -> {
            String receiverName = currentUser.getPrenom() + " " + currentUser.getNom();
            service.accepterFollow(notifId, senderId, receiverName, currentUser.getId());
            chargerDemandes();
            chargerContacts();
            showInfo("Vous suivez maintenant " + name + " !");
        });

        btnRefuser.setOnAction(e -> {
            service.refuserFollow(notifId);
            chargerDemandes();
        });

        HBox btns = new HBox(6, btnAccepter, btnRefuser);
        btns.setAlignment(Pos.CENTER);

        HBox cell = new HBox(10, avatar, info, spacer, btns);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setPadding(new Insets(10, 14, 10, 14));

        return cell;
    }

    // ── Chat ─────────────────────────────────────────────────────────────────

    /** Ouvre le chat avec un contact. */
    private void ouvrirChat(Map<String, Object> contact) {
        contactActif = contact;
        dernierMessageId = 0;

        String name = fullName(contact);
        labelNomContact.setText(name);
        labelAvatarChat.setText(initiales(name));

        // Afficher le panneau chat
        panneauVide.setVisible(false);
        panneauVide.setManaged(false);
        panneauChat.setVisible(true);
        panneauChat.setManaged(true);

        // Charger la conversation complète
        chargerConversation();
    }

    /** Charge toute la conversation avec le contact actif. */
    private void chargerConversation() {
        if (contactActif == null) return;
        int otherId = (int) contactActif.get("userId");

        List<Map<String, Object>> messages = service.getConversation(currentUser.getId(), otherId);
        containerMessages.getChildren().clear();
        dernierMessageId = 0;

        for (Map<String, Object> msg : messages) {
            ajouterBulleMessage(msg);
            int id = (int) msg.get("id");
            if (id > dernierMessageId) dernierMessageId = id;
        }

        scrollerEnBas();
        mettreAJourBadge();
    }

    /** Ajoute une bulle de message dans le conteneur. */
    private void ajouterBulleMessage(Map<String, Object> msg) {
        boolean isOwn = (boolean) msg.getOrDefault("isOwn", false);
        String texte  = (String) msg.getOrDefault("texte", "");
        Timestamp ts  = (Timestamp) msg.get("sentAt");
        String heure  = ts != null ? ts.toLocalDateTime().format(TIME_FMT) : "";

        // Bulle
        Label bulle = new Label(texte);
        bulle.setWrapText(true);
        bulle.setMaxWidth(380);
        bulle.setPadding(new Insets(10, 14, 10, 14));

        if (isOwn) {
            bulle.setStyle("-fx-background-color:#7c3aed; -fx-text-fill:white;" +
                           "-fx-font-size:13; -fx-background-radius:18 18 4 18;");
        } else {
            bulle.setStyle("-fx-background-color:white; -fx-text-fill:#0f172a;" +
                           "-fx-font-size:13; -fx-background-radius:18 18 18 4;" +
                           "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);");
        }

        // Heure
        Label heureLabel = new Label(heure);
        heureLabel.setStyle("-fx-font-size:10; -fx-text-fill:#94a3b8;");

        VBox bulleBox = new VBox(3, bulle, heureLabel);
        bulleBox.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox ligne = new HBox(bulleBox);
        ligne.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        ligne.setPadding(new Insets(2, 0, 2, 0));

        containerMessages.getChildren().add(ligne);
    }

    /** Action du bouton Envoyer. */
    @FXML
    private void onEnvoyerMessage() {
        if (contactActif == null) return;
        String texte = champMessage.getText().trim();
        if (texte.isEmpty()) return;

        int otherId = (int) contactActif.get("userId");
        String senderName = currentUser.getPrenom() + " " + currentUser.getNom();

        service.envoyerMessage(currentUser.getId(), otherId, senderName, texte);
        champMessage.clear();

        // Afficher immédiatement le message envoyé
        Map<String, Object> msgLocal = new java.util.HashMap<>();
        msgLocal.put("id", ++dernierMessageId);
        msgLocal.put("texte", texte);
        msgLocal.put("sentAt", new Timestamp(System.currentTimeMillis()));
        msgLocal.put("isOwn", true);
        ajouterBulleMessage(msgLocal);
        scrollerEnBas();
    }

    // ── Polling temps réel ────────────────────────────────────────────────────

    /** Démarre le polling toutes les 2 secondes. */
    private void demarrerPolling() {
        pollingTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            Platform.runLater(() -> {
                // Rafraîchir les nouveaux messages si chat ouvert
                if (contactActif != null) {
                    int otherId = (int) contactActif.get("userId");
                    List<Map<String, Object>> nouveaux =
                        service.getNouveauxMessages(currentUser.getId(), otherId, dernierMessageId);
                    if (!nouveaux.isEmpty()) {
                        for (Map<String, Object> msg : nouveaux) {
                            ajouterBulleMessage(msg);
                            int id = (int) msg.get("id");
                            if (id > dernierMessageId) dernierMessageId = id;
                        }
                        scrollerEnBas();
                    }
                }
                // Rafraîchir les demandes et le badge
                chargerDemandes();
                mettreAJourBadge();
                // Rafraîchir les contacts si un nouveau follow a été accepté
                chargerContacts();
            });
        }));
        pollingTimeline.setCycleCount(Timeline.INDEFINITE);
        pollingTimeline.play();
    }

    /** Arrête le polling (appelé quand on quitte la vue). */
    public void arreterPolling() {
        if (pollingTimeline != null) pollingTimeline.stop();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void scrollerEnBas() {
        Platform.runLater(() -> scrollMessages.setVvalue(1.0));
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

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Messagerie");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
