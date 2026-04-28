package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Cours;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCours;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CoursController — gère deux vues FXML :
 *   1. backoffice/cours/index.fxml  → tableau de liste des cours (backoffice admin)
 *   2. backoffice/cours/form.fxml   → formulaire création/modification d'un cours
 *
 * Ce controller est partagé entre les deux vues grâce aux annotations @FXML :
 * - Les champs de la table (tableCours, colTitre...) sont utilisés dans index.fxml
 * - Les champs du formulaire (fieldTitre, fieldMatiere...) sont utilisés dans form.fxml
 * - JavaFX ignore les @FXML null (champ absent dans la vue chargée)
 */
public class CoursController {

    // ── Composants de la vue INDEX (liste des cours) ──────────────────────────
    @FXML private TableView<Cours>         tableCours;
    @FXML private TableColumn<Cours, String> colTitre;
    @FXML private TableColumn<Cours, String> colMatiere;
    @FXML private TableColumn<Cours, String> colNiveau;
    @FXML private TableColumn<Cours, Number> colDuree;
    @FXML private TableColumn<Cours, Number> colChapitres; // nb chapitres par cours
    @FXML private TableColumn<Cours, Void>   colActions;   // boutons Edit/Supprimer/Chapitres
    @FXML private TextField                  searchField;
    @FXML private Label                      labelTotalCours;
    @FXML private Label                      labelTotalChapitres;

    // ── Composants de la vue FORM (formulaire cours) ──────────────────────────
    @FXML private Label        formTitle;       // "Nouveau cours" ou "Modifier cours"
    @FXML private TextField    fieldTitre;
    @FXML private TextField    fieldMatiere;
    @FXML private TextArea     areaDescription;
    @FXML private ComboBox<String> comboNiveau; // DEBUTANT / INTERMEDIAIRE / AVANCE
    @FXML private TextField    fieldDuree;
    @FXML private Label        errorTitre;      // messages d'erreur de validation
    @FXML private Label        errorMatiere;
    @FXML private Label        errorDescription;
    @FXML private Label        errorNiveau;
    @FXML private Label        errorDuree;

    // ── Services (accès BDD) ──────────────────────────────────────────────────
    private final ServiceCours    serviceCours    = new ServiceCours();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    // ── État interne ──────────────────────────────────────────────────────────
    // Map : coursId → nombre de chapitres (pour afficher la colonne "Nb chapitres")
    private Map<Integer, Integer> chapitreCountByCours = new HashMap<>();
    private Cours   editingCours; // cours en cours de modification (null si création)
    private boolean editMode;     // true = modification, false = création

    // ── INITIALISATION ────────────────────────────────────────────────────────
    /**
     * Appelée automatiquement par JavaFX après le chargement du FXML.
     * On détecte quelle vue est chargée selon les composants présents.
     */
    @FXML
    public void initialize() {
        // Si tableCours existe → on est dans index.fxml → initialiser la table
        if (tableCours != null) initTable();
        // Si comboNiveau existe → on est dans form.fxml → remplir la liste des niveaux
        if (comboNiveau != null) {
            comboNiveau.setItems(FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
        }
    }

    // ── INITIALISATION DE LA TABLE ────────────────────────────────────────────
    /**
     * Configure le style dark theme de la table et définit le contenu de chaque colonne.
     * Chaque colonne a un CellValueFactory (source de données) et un CellFactory (rendu visuel).
     */
    private void initTable() {
        // Style de fond sombre pour la table
        tableCours.setStyle(
            "-fx-background-color:#0f1a14; -fx-border-width:0;" +
            "-fx-table-cell-border-color:rgba(255,255,255,0.06);"
        );

        // Appliquer le thème sombre sur le header dès que le skin est prêt
        tableCours.skinProperty().addListener((obs, o, skin) ->
            javafx.application.Platform.runLater(this::applyTableDarkTheme));
        tableCours.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) javafx.application.Platform.runLater(this::applyTableDarkTheme);
        });

        // Style des lignes : fond sombre + highlight au survol et à la sélection
        tableCours.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Cours> row = new javafx.scene.control.TableRow<>();
            row.setStyle("-fx-background-color:#0f1a14;");
            row.selectedProperty().addListener((obs, was, is) ->
                row.setStyle(is ? "-fx-background-color:rgba(5,150,105,0.18);" : "-fx-background-color:#0f1a14;"));
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isSelected())
                    row.setStyle(is ? "-fx-background-color:rgba(255,255,255,0.04);" : "-fx-background-color:#0f1a14;");
            });
            return row;
        });

        // Colonne Titre : texte blanc gras
        colTitre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:600;");
                setText(empty || item == null ? null : item);
            }
        });

        // Colonne Matière : texte grisé centré
        colMatiere.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatiere()));
        colMatiere.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12;");
                setText(empty || item == null ? null : item);
            }
        });

        // Colonne Niveau : badge coloré selon le niveau (vert/jaune/rouge)
        colNiveau.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNiveau()));
        colNiveau.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                String base = "-fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20; -fx-text-fill:white;";
                switch (item) {
                    case "DEBUTANT"      -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#34d399,#059669);");
                    case "INTERMEDIAIRE" -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#fbbf24,#f59e0b); -fx-text-fill:#1a1a1a;");
                    case "AVANCE"        -> badge.setStyle(base + "-fx-background-color:linear-gradient(to right,#f87171,#dc2626);");
                    default              -> badge.setStyle(base + "-fx-background-color:rgba(255,255,255,0.15);");
                }
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        // Colonne Durée : nombre d'heures, texte grisé
        colDuree.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDuree()));
        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12;");
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
            }
        });

        // Colonne Nb chapitres : nombre calculé depuis la map chapitreCountByCours, en bleu
        colChapitres.setCellValueFactory(data -> new SimpleIntegerProperty(chapitreCountByCours.getOrDefault(data.getValue().getId(), 0)));
        colChapitres.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:#60a5fa; -fx-font-size:13; -fx-font-weight:700;");
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
            }
        });

        // Colonne Actions : 3 boutons par ligne (Edit / Supprimer / Chapitres)
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit      = new Button("Edit");
            private final Button btnDelete    = new Button("Supprimer");
            private final Button btnChapitres = new Button("Chapitres");
            private final HBox   box          = new HBox(8, btnEdit, btnDelete, btnChapitres);
            {
                String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                btnEdit.setStyle(base + "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;");
                btnDelete.setStyle(base + "-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af;");
                btnChapitres.setStyle(base + "-fx-background-color:rgba(52,211,153,0.25); -fx-text-fill:#34d399;");
                box.setAlignment(Pos.CENTER);
                // Chaque bouton récupère le cours de la ligne courante via getIndex()
                btnEdit.setOnAction(e -> onEditCours(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDeleteCours(getTableView().getItems().get(getIndex())));
                btnChapitres.setOnAction(e -> openChapitreWindow(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0;");
                setGraphic(empty ? null : box);
            }
        });

        loadTable(); // charger les données depuis la BDD
    }

    // ── THÈME SOMBRE SUR LE HEADER ────────────────────────────────────────────
    /**
     * Applique le style dark sur les éléments CSS internes de la TableView
     * (header, scrollbars, filler) qui ne peuvent pas être stylisés directement en FXML.
     */
    private void applyTableDarkTheme() {
        javafx.scene.Node header = tableCours.lookup("TableHeaderRow");
        if (header != null)
            header.setStyle("-fx-background-color:#0d1710; -fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        tableCours.lookupAll(".column-header").forEach(n -> n.setStyle("-fx-background-color:#0d1710; -fx-border-width:0;"));
        tableCours.lookupAll(".column-header .label").forEach(n ->
            ((Label) n).setStyle("-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-font-weight:700;"));
        tableCours.lookupAll(".filler").forEach(n -> n.setStyle("-fx-background-color:#0d1710;"));
        tableCours.lookupAll(".scroll-bar").forEach(n -> n.setStyle("-fx-background-color:transparent;"));
        tableCours.lookupAll(".scroll-bar .track").forEach(n -> n.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;"));
        tableCours.lookupAll(".scroll-bar .thumb").forEach(n -> n.setStyle("-fx-background-color:rgba(52,211,153,0.22); -fx-background-radius:4;"));
        tableCours.lookupAll(".increment-button, .decrement-button").forEach(n -> n.setStyle("-fx-background-color:transparent; -fx-pref-height:0; -fx-pref-width:0;"));
        tableCours.lookupAll(".corner").forEach(n -> n.setStyle("-fx-background-color:transparent;"));
    }

    // ── CHARGEMENT DES DONNÉES ────────────────────────────────────────────────
    /**
     * Charge tous les cours depuis la BDD et les affiche dans la table.
     * Calcule aussi le nombre de chapitres par cours pour la colonne "Nb chapitres".
     * Applique le filtre de recherche si le champ searchField n'est pas vide.
     */
    private void loadTable() {
        List<Cours> coursList = serviceCours.consulter();

        // Compter les chapitres par cours : on parcourt tous les chapitres
        // et on incrémente le compteur pour chaque cours_id trouvé
        chapitreCountByCours = new HashMap<>();
        serviceChapitre.consulter().forEach(ch -> chapitreCountByCours.merge(ch.getCoursId(), 1, Integer::sum));

        // Filtrer selon la recherche (titre, matière ou niveau)
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (!q.isEmpty()) {
            coursList = coursList.stream().filter(c ->
                c.getTitre().toLowerCase().contains(q)
                    || c.getMatiere().toLowerCase().contains(q)
                    || c.getNiveau().toLowerCase().contains(q)
            ).toList();
        }

        if (tableCours != null) tableCours.setItems(FXCollections.observableArrayList(coursList));
        // Mettre à jour les compteurs dans les stats cards
        if (labelTotalCours     != null) labelTotalCours.setText(String.valueOf(serviceCours.consulter().size()));
        if (labelTotalChapitres != null) labelTotalChapitres.setText(String.valueOf(serviceChapitre.consulter().size()));
    }

    // ── RECHERCHE ─────────────────────────────────────────────────────────────
    @FXML private void onSearch()      { loadTable(); }
    @FXML private void onClearSearch() { if (searchField != null) searchField.clear(); loadTable(); }

    // ── ACTIONS CRUD ──────────────────────────────────────────────────────────

    /** Ouvre le formulaire de création (cours = null → mode création). */
    @FXML
    private void onNewCours() {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les cours.");
            return;
        }
        openFormWindow(null);
    }

    /** Ouvre le formulaire pré-rempli pour modifier un cours existant. */
    private void onEditCours(Cours cours) {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les cours.");
            return;
        }
        openFormWindow(cours);
    }

    /** Demande confirmation puis supprime le cours ET ses chapitres (cascade). */
    private void onDeleteCours(Cours cours) {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les cours.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le cours");
        confirm.setContentText("Voulez-vous supprimer le cours '" + cours.getTitre() + "' ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                var admin = SessionManager.getCurrentUser();
                if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.deleted_cours",
                    java.util.Map.of("titre", cours.getTitre(), "id", String.valueOf(cours.getId())));
                serviceCours.supprimer(cours.getId());
                loadTable();
            }
        });
    }

    // ── OUVERTURE DES FENÊTRES MODALES ────────────────────────────────────────

    /**
     * Ouvre le formulaire cours dans une fenêtre modale.
     * Si cours == null → création, sinon → modification.
     * Après fermeture, recharge la table pour refléter les changements.
     */
    private void openFormWindow(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/cours/form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL); // bloque la fenêtre parente
            stage.setTitle(cours == null ? "Nouveau cours" : "Modifier cours");
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 560, 560));
            CoursController ctrl = loader.getController();
            ctrl.setEditingCours(cours); // injecter le cours à modifier (ou null)
            stage.showAndWait();         // attendre la fermeture avant de continuer
            loadTable();                 // rafraîchir la liste
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Ouvre la liste des chapitres d'un cours dans une fenêtre modale.
     * Utilise ChapitreController avec le cours sélectionné.
     */
    private void openChapitreWindow(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/chapitre/index.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chapitres - " + cours.getTitre());
            stage.setScene(new Scene(loader.load(), 1000, 640));
            ChapitreController ctrl = loader.getController();
            ctrl.setCours(cours); // injecter le cours pour filtrer ses chapitres
            stage.showAndWait();
            loadTable(); // rafraîchir le nb de chapitres après fermeture
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── INITIALISATION DU FORMULAIRE ──────────────────────────────────────────
    /**
     * Appelé depuis openFormWindow() pour pré-remplir le formulaire en mode modification.
     * En mode création (cours == null), le formulaire reste vide.
     */
    public void setEditingCours(Cours cours) {
        this.editingCours = cours;
        this.editMode     = cours != null;
        if (formTitle == null) return; // sécurité si appelé hors contexte form

        if (editMode) {
            formTitle.setText("Modifier cours");
            fieldTitre.setText(cours.getTitre());
            fieldMatiere.setText(cours.getMatiere());
            areaDescription.setText(cours.getDescription());
            comboNiveau.setValue(cours.getNiveau());
            fieldDuree.setText(String.valueOf(cours.getDuree()));
        } else {
            formTitle.setText("Nouveau cours");
        }
    }

    // ── SAUVEGARDE DU FORMULAIRE ──────────────────────────────────────────────
    /** Valide le formulaire puis crée ou modifie le cours en BDD. */
    @FXML
    private void onSave() {
        if (!validateForm()) return; // arrêter si validation échoue

        String titre       = fieldTitre.getText().trim();
        String matiere     = fieldMatiere.getText().trim();
        String description = areaDescription.getText().trim();
        String niveau      = comboNiveau.getValue();
        int    duree       = Integer.parseInt(fieldDuree.getText().trim());

        if (!editMode) {
            Cours cours = new Cours(titre, description, matiere, niveau, duree, LocalDateTime.now());
            serviceCours.ajouter(cours);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_cours",
                java.util.Map.of("titre", titre, "matiere", matiere));
        } else {
            editingCours.setTitre(titre);
            editingCours.setMatiere(matiere);
            editingCours.setDescription(description);
            editingCours.setNiveau(niveau);
            editingCours.setDuree(duree);
            serviceCours.modifier(editingCours);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_cours",
                java.util.Map.of("titre", titre, "id", String.valueOf(editingCours.getId())));
        }

        // Fermer la fenêtre modale après sauvegarde
        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    /** Ferme le formulaire sans sauvegarder. */
    @FXML
    private void onCancel() {
        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    // ── VALIDATION DU FORMULAIRE ──────────────────────────────────────────────
    /**
     * Vérifie chaque champ avec des règles métier significatives.
     * Affiche un message d'erreur sous chaque champ invalide.
     * Retourne true si tout est valide, false sinon.
     */
    private boolean validateForm() {
        // Réinitialiser tous les messages d'erreur
        errorTitre.setText(""); errorMatiere.setText(""); errorDescription.setText("");
        errorNiveau.setText(""); errorDuree.setText("");
        boolean valid = true;

        // Titre : 3 à 100 caractères, lettres/chiffres/ponctuation basique
        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) {
            errorTitre.setText("Le titre est obligatoire"); valid = false;
        } else if (titre.length() < 3) {
            errorTitre.setText("Le titre doit contenir au moins 3 caractères"); valid = false;
        } else if (titre.length() > 100) {
            errorTitre.setText("Le titre ne peut pas dépasser 100 caractères"); valid = false;
        } else if (!titre.matches("^[\\p{L}0-9 \\-_.,:'\"()]+$")) {
            errorTitre.setText("Caractères spéciaux non autorisés dans le titre"); valid = false;
        }

        // Matière : lettres et espaces uniquement (pas de chiffres ni symboles)
        String matiere = fieldMatiere.getText().trim();
        if (matiere.isEmpty()) {
            errorMatiere.setText("La matière est obligatoire"); valid = false;
        } else if (matiere.length() < 2) {
            errorMatiere.setText("La matière doit contenir au moins 2 caractères"); valid = false;
        } else if (matiere.length() > 50) {
            errorMatiere.setText("La matière ne peut pas dépasser 50 caractères"); valid = false;
        } else if (!matiere.matches("^[\\p{L} \\-]+$")) {
            errorMatiere.setText("La matière doit contenir uniquement des lettres"); valid = false;
        }

        // Description : min 20 chars (une description d'une seule phrase n'a pas de sens)
        String desc = areaDescription.getText().trim();
        if (desc.isEmpty()) {
            errorDescription.setText("La description est obligatoire"); valid = false;
        } else if (desc.length() < 20) {
            errorDescription.setText("La description doit contenir au moins 20 caractères"); valid = false;
        } else if (desc.length() > 500) {
            errorDescription.setText("La description ne peut pas dépasser 500 caractères"); valid = false;
        }

        // Niveau : doit être sélectionné dans la liste
        if (comboNiveau.getValue() == null || comboNiveau.getValue().isBlank()) {
            errorNiveau.setText("Veuillez sélectionner un niveau"); valid = false;
        }

        // Durée : entier entre 1 et 500 heures (un cours de 0h ou 1000h n'a pas de sens)
        try {
            int duree = Integer.parseInt(fieldDuree.getText().trim());
            if (duree < 1)   { errorDuree.setText("La durée doit être d'au moins 1 heure"); valid = false; }
            else if (duree > 500) { errorDuree.setText("La durée ne peut pas dépasser 500 heures"); valid = false; }
        } catch (NumberFormatException e) {
            errorDuree.setText("La durée doit être un nombre entier (ex: 12)"); valid = false;
        }

        return valid;
    }

    // ── GÉNÉRATION IA ─────────────────────────────────────────────────────────
    /**
     * Ouvre un dialogue pour générer un cours avec IA (Groq).
     * L'utilisateur saisit : sujet, niveau, nombre de chapitres.
     */
    @FXML
    private void onGenerateWithAI() {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Seul l'admin peut générer des cours avec IA.");
            return;
        }

        // Créer un dialogue personnalisé
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("🤖 Générer un cours avec IA");
        dialog.setHeaderText("Génération automatique d'un cours complet avec chapitres");

        // Boutons
        ButtonType btnGenerate = new ButtonType("Générer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnGenerate, ButtonType.CANCEL);

        // Formulaire
        VBox form = new VBox(12);
        form.setPadding(new javafx.geometry.Insets(20));
        form.setStyle("-fx-background-color:white;");

        Label lblSujet = new Label("Sujet du cours :");
        lblSujet.setStyle("-fx-font-weight:bold; -fx-font-size:12;");
        TextField fieldSujet = new TextField();
        fieldSujet.setPromptText("Ex: Introduction à Java, Bases de données SQL...");
        fieldSujet.setStyle("-fx-font-size:13; -fx-padding:8;");

        Label lblNiveau = new Label("Niveau :");
        lblNiveau.setStyle("-fx-font-weight:bold; -fx-font-size:12;");
        ComboBox<String> comboNiveau = new ComboBox<>(
            javafx.collections.FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE")
        );
        comboNiveau.setValue("INTERMEDIAIRE");
        comboNiveau.setStyle("-fx-font-size:13;");

        Label lblNbChapitres = new Label("Nombre de chapitres (1-8) :");
        lblNbChapitres.setStyle("-fx-font-weight:bold; -fx-font-size:12;");
        TextField fieldNbChapitres = new TextField("3");
        fieldNbChapitres.setStyle("-fx-font-size:13; -fx-padding:8;");

        Label lblInfo = new Label("ℹ️ L'IA générera automatiquement le cours avec ses chapitres et contenus.");
        lblInfo.setStyle("-fx-text-fill:#666; -fx-font-size:11; -fx-padding:10 0 0 0;");
        lblInfo.setWrapText(true);

        form.getChildren().addAll(
            lblSujet, fieldSujet,
            lblNiveau, comboNiveau,
            lblNbChapitres, fieldNbChapitres,
            lblInfo
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setStyle("-fx-background-color:white;");

        // Validation et génération
        dialog.showAndWait().ifPresent(btn -> {
            if (btn == btnGenerate) {
                String sujet = fieldSujet.getText().trim();
                String niveau = comboNiveau.getValue();
                int nbChapitres;

                try {
                    nbChapitres = Integer.parseInt(fieldNbChapitres.getText().trim());
                    if (nbChapitres < 1 || nbChapitres > 8) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le nombre de chapitres doit être entre 1 et 8.");
                    return;
                }

                if (sujet.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Le sujet est obligatoire.");
                    return;
                }

                // Afficher un dialogue de progression
                Alert progress = new Alert(Alert.AlertType.INFORMATION);
                progress.setTitle("Génération en cours");
                progress.setHeaderText("🤖 L'IA génère votre cours...");
                progress.setContentText("Veuillez patienter, cela peut prendre quelques secondes.");
                progress.show();

                // Appeler l'IA de manière asynchrone
                genererCoursAvecIA(sujet, niveau, nbChapitres)
                    .thenAccept(cours -> javafx.application.Platform.runLater(() -> {
                        progress.close();
                        if (cours != null) {
                            showAlert(Alert.AlertType.INFORMATION, "Succès",
                                "Le cours '" + cours.getTitre() + "' a été généré avec succès !");
                            loadTable();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Erreur",
                                "La génération a échoué. Vérifiez votre connexion et réessayez.");
                        }
                    }))
                    .exceptionally(ex -> {
                        javafx.application.Platform.runLater(() -> {
                            progress.close();
                            showAlert(Alert.AlertType.ERROR, "Erreur",
                                "Erreur lors de la génération : " + ex.getMessage());
                        });
                        return null;
                    });
            }
        });
    }

    /**
     * Génère un cours complet avec chapitres via l'API Groq.
     * Utilise le même modèle que la génération de challenges.
     */
    private java.util.concurrent.CompletableFuture<Cours> genererCoursAvecIA(
            String sujet, String niveau, int nbChapitres) {

        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                // Appeler l'API Groq pour générer le cours
                String prompt = """
                    Crée un cours pédagogique complet sur le sujet: "%s"
                    Niveau: %s
                    Nombre de chapitres: %d
                    
                    INSTRUCTIONS:
                    1. Le titre doit être accrocheur et professionnel (max 80 caractères)
                    2. La matière doit être le domaine principal (ex: Informatique, Mathématiques)
                    3. La description doit présenter les objectifs du cours (2-3 phrases)
                    4. Chaque chapitre doit avoir un titre clair et un contenu pédagogique détaillé
                    5. Le contenu de chaque chapitre doit être structuré et complet (minimum 200 mots)
                    6. Estime la durée totale du cours en heures (entre 10 et 100h)
                    
                    RÉPONDS UNIQUEMENT en JSON avec ce format EXACT:
                    {
                      "titre": "Titre du cours",
                      "matiere": "Matière principale",
                      "description": "Description complète du cours",
                      "niveau": "%s",
                      "duree": 30,
                      "chapitres": [
                        {
                          "titre": "Titre du chapitre",
                          "contenu": "Contenu pédagogique détaillé du chapitre",
                          "ordre": 1
                        }
                      ]
                    }
                    """.formatted(sujet, niveau, nbChapitres, niveau);

                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("model", "meta-llama/llama-4-scout-17b-16e-instruct");
                body.addProperty("temperature", 0.7);
                body.addProperty("max_tokens", 4000);

                com.google.gson.JsonObject responseFormat = new com.google.gson.JsonObject();
                responseFormat.addProperty("type", "json_object");
                body.add("response_format", responseFormat);

                com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
                com.google.gson.JsonObject system = new com.google.gson.JsonObject();
                system.addProperty("role", "system");
                system.addProperty("content",
                    "Tu es un expert pédagogique qui crée des cours de qualité professionnelle. " +
                    "Tu réponds UNIQUEMENT en JSON valide, sans texte avant ou après.");
                messages.add(system);

                com.google.gson.JsonObject user = new com.google.gson.JsonObject();
                user.addProperty("role", "user");
                user.addProperty("content", prompt);
                messages.add(user);
                body.add("messages", messages);

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(java.time.Duration.ofSeconds(60))
                    .header("Authorization", "Bearer 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")
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30))
                    .build();

                java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Groq API HTTP " + response.statusCode() + ": " + response.body());
                }

                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.google.gson.JsonObject responseJson = gson.fromJson(response.body(), com.google.gson.JsonObject.class);
                String content = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

                // Parser le JSON généré
                com.google.gson.JsonObject data = gson.fromJson(content, com.google.gson.JsonObject.class);

                String titre = data.has("titre") ? data.get("titre").getAsString().trim() : "Cours IA";
                final String titreFinal = titre.length() > 100 ? titre.substring(0, 97) + "..." : titre;

                String matiere = data.has("matiere") ? data.get("matiere").getAsString().trim() : "Général";
                String description = data.has("description") ? data.get("description").getAsString().trim() : "Cours généré par IA";
                String niveauFinal = data.has("niveau") ? data.get("niveau").getAsString().trim() : niveau;
                int duree = data.has("duree") ? data.get("duree").getAsInt() : 30;

                // Créer le cours en BDD
                Cours cours = new Cours(titreFinal, description, matiere, niveauFinal, duree, java.time.LocalDateTime.now());
                serviceCours.ajouter(cours);

                // Récupérer le cours créé
                Cours created = serviceCours.consulter().stream()
                    .filter(c -> titreFinal.equals(c.getTitre()))
                    .reduce((a, b) -> b)
                    .orElse(null);

                if (created == null) {
                    throw new RuntimeException("Cours créé introuvable en BDD.");
                }

                // Créer les chapitres
                if (data.has("chapitres") && data.get("chapitres").isJsonArray()) {
                    com.google.gson.JsonArray chapitres = data.getAsJsonArray("chapitres");
                    for (int i = 0; i < chapitres.size(); i++) {
                        com.google.gson.JsonObject chap = chapitres.get(i).getAsJsonObject();
                        String titreChapitre = chap.has("titre") ? chap.get("titre").getAsString().trim() : "Chapitre " + (i + 1);
                        String contenu = chap.has("contenu") ? chap.get("contenu").getAsString().trim() : "";
                        int ordre = chap.has("ordre") ? chap.get("ordre").getAsInt() : (i + 1);

                        tn.esprit.entities.Chapitre chapitre = new tn.esprit.entities.Chapitre();
                        chapitre.setTitre(titreChapitre);
                        chapitre.setContenu(contenu);
                        chapitre.setOrdre(ordre);
                        chapitre.setCoursId(created.getId());
                        serviceChapitre.ajouter(chapitre);
                    }
                }

                // Logger l'activité
                var admin = SessionManager.getCurrentUser();
                if (admin != null) {
                    ActivityApiClient.logAsync(admin.getId(), "admin.generated_cours_ai",
                        Map.of("titre", titreFinal, "sujet", sujet, "niveau", niveauFinal));
                }

                System.out.println("[CoursAI] Cours généré: " + created.getTitre() + " avec " + nbChapitres + " chapitres");
                return created;

            } catch (Exception e) {
                System.err.println("[CoursAI] Erreur génération: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    // ── UTILITAIRE ────────────────────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }
}
