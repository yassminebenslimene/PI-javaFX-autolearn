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
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Cours;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.services.ServiceCours;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoursController {

    @FXML private TableView<Cours> tableCours;
    @FXML private TableColumn<Cours, String> colTitre;
    @FXML private TableColumn<Cours, String> colMatiere;
    @FXML private TableColumn<Cours, String> colNiveau;
    @FXML private TableColumn<Cours, Number> colDuree;
    @FXML private TableColumn<Cours, Number> colChapitres;
    @FXML private TableColumn<Cours, Void> colActions;
    @FXML private TextField searchField;
    @FXML private Label labelTotalCours;
    @FXML private Label labelTotalChapitres;

    @FXML private Label formTitle;
    @FXML private TextField fieldTitre;
    @FXML private TextField fieldMatiere;
    @FXML private TextArea areaDescription;
    @FXML private ComboBox<String> comboNiveau;
    @FXML private TextField fieldDuree;
    @FXML private Label errorTitre;
    @FXML private Label errorMatiere;
    @FXML private Label errorDescription;
    @FXML private Label errorNiveau;
    @FXML private Label errorDuree;

    private final ServiceCours serviceCours = new ServiceCours();
    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    private Map<Integer, Integer> chapitreCountByCours = new HashMap<>();
    private Cours editingCours;
    private boolean editMode;

    @FXML
    public void initialize() {
        if (tableCours != null) initTable();
        if (comboNiveau != null) {
            comboNiveau.setItems(FXCollections.observableArrayList("DEBUTANT", "INTERMEDIAIRE", "AVANCE"));
        }
    }

    private void initTable() {
        tableCours.setStyle(
            "-fx-background-color:#0f1a14; -fx-border-width:0;" +
            "-fx-table-cell-border-color:rgba(255,255,255,0.06);"
        );

        tableCours.skinProperty().addListener((obs, o, skin) ->
            javafx.application.Platform.runLater(this::applyTableDarkTheme));
        tableCours.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) javafx.application.Platform.runLater(this::applyTableDarkTheme);
        });

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

        // Titre
        colTitre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:600;");
                setText(empty || item == null ? null : item);
            }
        });

        // Matiere
        colMatiere.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMatiere()));
        colMatiere.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:12;");
                setText(empty || item == null ? null : item);
            }
        });

        // Niveau badge
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

        // Duree
        colDuree.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDuree()));
        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.6); -fx-font-size:12;");
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
            }
        });

        // Nb chapitres
        colChapitres.setCellValueFactory(data -> new SimpleIntegerProperty(chapitreCountByCours.getOrDefault(data.getValue().getId(), 0)));
        colChapitres.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:#60a5fa; -fx-font-size:13; -fx-font-weight:700;");
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
            }
        });

        // Actions
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

        loadTable();
    }

    private void applyTableDarkTheme() {
        javafx.scene.Node header = tableCours.lookup("TableHeaderRow");
        if (header != null)
            header.setStyle("-fx-background-color:#0d1710; -fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        tableCours.lookupAll(".column-header").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710; -fx-border-width:0;"));
        tableCours.lookupAll(".column-header .label").forEach(n ->
            ((javafx.scene.control.Label) n).setStyle(
                "-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-font-weight:700;"));
        tableCours.lookupAll(".filler").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710;"));
        tableCours.lookupAll(".scroll-bar").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
        tableCours.lookupAll(".scroll-bar .track").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;"));
        tableCours.lookupAll(".scroll-bar .thumb").forEach(n ->
            n.setStyle("-fx-background-color:rgba(52,211,153,0.22); -fx-background-radius:4;"));
        tableCours.lookupAll(".increment-button, .decrement-button").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-pref-height:0; -fx-pref-width:0;"));
        tableCours.lookupAll(".corner").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
    }

    private void loadTable() {
        List<Cours> coursList = serviceCours.consulter();
        chapitreCountByCours = new HashMap<>();
        serviceChapitre.consulter().forEach(ch -> chapitreCountByCours.merge(ch.getCoursId(), 1, Integer::sum));

        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (!q.isEmpty()) {
            coursList = coursList.stream().filter(c ->
                c.getTitre().toLowerCase().contains(q)
                    || c.getMatiere().toLowerCase().contains(q)
                    || c.getNiveau().toLowerCase().contains(q)
            ).toList();
        }

        if (tableCours != null) tableCours.setItems(FXCollections.observableArrayList(coursList));
        if (labelTotalCours != null) labelTotalCours.setText(String.valueOf(serviceCours.consulter().size()));
        if (labelTotalChapitres != null) labelTotalChapitres.setText(String.valueOf(serviceChapitre.consulter().size()));
    }

    @FXML
    private void onSearch() {
        loadTable();
    }

    @FXML
    private void onClearSearch() {
        if (searchField != null) searchField.clear();
        loadTable();
    }

    @FXML
    private void onNewCours() {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les cours.");
            return;
        }
        openFormWindow(null);
    }

    private void onEditCours(Cours cours) {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les cours.");
            return;
        }
        openFormWindow(cours);
    }

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
                serviceCours.supprimer(cours.getId());
                loadTable();
            }
        });
    }

    private void openFormWindow(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/cours/form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(cours == null ? "Nouveau cours" : "Modifier cours");
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 560, 560));
            CoursController ctrl = loader.getController();
            ctrl.setEditingCours(cours);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openChapitreWindow(Cours cours) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/chapitre/index.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chapitres - " + cours.getTitre());
            stage.setScene(new Scene(loader.load(), 1000, 640));
            ChapitreController ctrl = loader.getController();
            ctrl.setCours(cours);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setEditingCours(Cours cours) {
        this.editingCours = cours;
        this.editMode = cours != null;

        if (formTitle == null) return;

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

    @FXML
    private void onSave() {
        if (!validateForm()) return;

        String titre = fieldTitre.getText().trim();
        String matiere = fieldMatiere.getText().trim();
        String description = areaDescription.getText().trim();
        String niveau = comboNiveau.getValue();
        int duree = Integer.parseInt(fieldDuree.getText().trim());

        if (!editMode) {
            Cours cours = new Cours(titre, description, matiere, niveau, duree, LocalDateTime.now());
            serviceCours.ajouter(cours);
        } else {
            editingCours.setTitre(titre);
            editingCours.setMatiere(matiere);
            editingCours.setDescription(description);
            editingCours.setNiveau(niveau);
            editingCours.setDuree(duree);
            serviceCours.modifier(editingCours);
        }

        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    @FXML
    private void onCancel() {
        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    private boolean validateForm() {
        errorTitre.setText("");
        errorMatiere.setText("");
        errorDescription.setText("");
        errorNiveau.setText("");
        errorDuree.setText("");

        boolean valid = true;

        // Titre : 3 à 100 caractères, lettres/chiffres/espaces autorisés
        String titre = fieldTitre.getText().trim();
        if (titre.isEmpty()) {
            errorTitre.setText("Le titre est obligatoire");
            valid = false;
        } else if (titre.length() < 3) {
            errorTitre.setText("Le titre doit contenir au moins 3 caractères");
            valid = false;
        } else if (titre.length() > 100) {
            errorTitre.setText("Le titre ne peut pas dépasser 100 caractères");
            valid = false;
        } else if (!titre.matches("^[\\p{L}0-9 \\-_.,:'\"()]+$")) {
            errorTitre.setText("Caractères spéciaux non autorisés dans le titre");
            valid = false;
        }

        // Matière : lettres et espaces uniquement, 2 à 50 caractères
        String matiere = fieldMatiere.getText().trim();
        if (matiere.isEmpty()) {
            errorMatiere.setText("La matière est obligatoire");
            valid = false;
        } else if (matiere.length() < 2) {
            errorMatiere.setText("La matière doit contenir au moins 2 caractères");
            valid = false;
        } else if (matiere.length() > 50) {
            errorMatiere.setText("La matière ne peut pas dépasser 50 caractères");
            valid = false;
        } else if (!matiere.matches("^[\\p{L} \\-]+$")) {
            errorMatiere.setText("La matière doit contenir uniquement des lettres");
            valid = false;
        }

        // Description : minimum 20 caractères pour être significative
        String desc = areaDescription.getText().trim();
        if (desc.isEmpty()) {
            errorDescription.setText("La description est obligatoire");
            valid = false;
        } else if (desc.length() < 20) {
            errorDescription.setText("La description doit contenir au moins 20 caractères");
            valid = false;
        } else if (desc.length() > 500) {
            errorDescription.setText("La description ne peut pas dépasser 500 caractères");
            valid = false;
        }

        // Niveau : sélection obligatoire
        if (comboNiveau.getValue() == null || comboNiveau.getValue().isBlank()) {
            errorNiveau.setText("Veuillez sélectionner un niveau");
            valid = false;
        }

        // Durée : nombre entier entre 1 et 500 heures
        try {
            int duree = Integer.parseInt(fieldDuree.getText().trim());
            if (duree < 1) {
                errorDuree.setText("La durée doit être d'au moins 1 heure");
                valid = false;
            } else if (duree > 500) {
                errorDuree.setText("La durée ne peut pas dépasser 500 heures");
                valid = false;
            }
        } catch (NumberFormatException e) {
            errorDuree.setText("La durée doit être un nombre entier (ex: 12)");
            valid = false;
        }

        return valid;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
