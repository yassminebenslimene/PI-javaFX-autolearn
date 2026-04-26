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
import tn.esprit.entities.Chapitre;
import tn.esprit.entities.Cours;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ServiceChapitre;
import tn.esprit.session.SessionManager;

import java.io.IOException;
import java.util.List;

public class ChapitreController {

    @FXML private Label labelCourseTitle;
    @FXML private Label labelCourseSubtitle;
    @FXML private TableView<Chapitre> tableChapitres;
    @FXML private TableColumn<Chapitre, Number> colOrdre;
    @FXML private TableColumn<Chapitre, String> colTitre;
    @FXML private TableColumn<Chapitre, String> colType;
    @FXML private TableColumn<Chapitre, Void> colActions;
    @FXML private TextField searchField;

    @FXML private Label formTitle;
    @FXML private Label labelFormCourse;
    @FXML private TextField fieldTitre;
    @FXML private TextArea areaContenu;
    @FXML private TextField fieldOrdre;
    @FXML private TextField fieldRessources;
    @FXML private ComboBox<String> comboRessourceType;
    @FXML private TextField fieldRessourceFichier;
    @FXML private Label errorTitre;
    @FXML private Label errorContenu;
    @FXML private Label errorOrdre;
    @FXML private Label errorRessources;
    @FXML private Label errorFichier;

    private final ServiceChapitre serviceChapitre = new ServiceChapitre();

    private Cours cours;
    private Chapitre editingChapitre;
    private boolean editMode;

    @FXML
    public void initialize() {
        if (comboRessourceType != null) {
            comboRessourceType.setItems(FXCollections.observableArrayList("VIDEO", "PDF", "LIEN", "AUTRE"));
        }

        if (tableChapitres != null) initTable();
    }

    public void setCours(Cours cours) {
        this.cours = cours;

        if (labelCourseTitle != null) {
            labelCourseTitle.setText("Chapitres - " + cours.getTitre());
        }
        if (labelCourseSubtitle != null) {
            labelCourseSubtitle.setText("Matiere: " + cours.getMatiere() + " | Niveau: " + cours.getNiveau());
        }
        if (labelFormCourse != null) {
            labelFormCourse.setText(cours.getTitre());
        }

        if (tableChapitres != null) loadTable();
    }

    private void initTable() {
        tableChapitres.setStyle(
            "-fx-background-color:#0f1a14; -fx-border-width:0;" +
            "-fx-table-cell-border-color:rgba(255,255,255,0.06);"
        );

        tableChapitres.skinProperty().addListener((obs, o, skin) ->
            javafx.application.Platform.runLater(this::applyTableDarkTheme));
        tableChapitres.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) javafx.application.Platform.runLater(this::applyTableDarkTheme);
        });

        tableChapitres.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Chapitre> row = new javafx.scene.control.TableRow<>();
            row.setStyle("-fx-background-color:#0f1a14;");
            row.selectedProperty().addListener((obs, was, is) ->
                row.setStyle(is ? "-fx-background-color:rgba(37,99,235,0.18);" : "-fx-background-color:#0f1a14;"));
            row.hoverProperty().addListener((obs, was, is) -> {
                if (!row.isSelected())
                    row.setStyle(is ? "-fx-background-color:rgba(255,255,255,0.04);" : "-fx-background-color:#0f1a14;");
            });
            return row;
        });

        colOrdre.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getOrdre()));
        colOrdre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER; -fx-text-fill:rgba(245,245,244,0.7); -fx-font-size:13;");
                setText(empty || item == null ? null : String.valueOf(item.intValue()));
            }
        });

        colTitre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        colTitre.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-text-fill:white; -fx-font-size:13; -fx-font-weight:600;");
                setText(empty || item == null ? null : item);
            }
        });

        colType.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getRessourceType() == null ? "-" : data.getValue().getRessourceType()
        ));
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color:#0f1a14; -fx-border-color:transparent transparent rgba(255,255,255,0.06) transparent; -fx-border-width:0 0 1 0; -fx-alignment:CENTER;");
                if (empty || item == null || "-".equals(item)) { setText("—"); setGraphic(null); return; }
                javafx.scene.control.Label badge = new javafx.scene.control.Label(item);
                String base = "-fx-font-size:11; -fx-font-weight:700; -fx-padding:4 12 4 12; -fx-background-radius:20; -fx-text-fill:white;";
                switch (item) {
                    case "VIDEO" -> badge.setStyle(base + "-fx-background-color:rgba(96,165,250,0.25); -fx-text-fill:#60a5fa;");
                    case "PDF"   -> badge.setStyle(base + "-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af;");
                    case "LIEN"  -> badge.setStyle(base + "-fx-background-color:rgba(52,211,153,0.25); -fx-text-fill:#34d399;");
                    default      -> badge.setStyle(base + "-fx-background-color:rgba(255,255,255,0.12); -fx-text-fill:rgba(245,245,244,0.7);");
                }
                HBox wrap = new HBox(badge);
                wrap.setAlignment(Pos.CENTER);
                setGraphic(wrap); setText(null);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit   = new Button("Edit");
            private final Button btnDelete = new Button("Supprimer");
            private final HBox   box       = new HBox(8, btnEdit, btnDelete);
            {
                String base = "-fx-font-size:11; -fx-font-weight:600; -fx-padding:5 10 5 10; -fx-background-radius:8; -fx-cursor:hand; -fx-border-width:0;";
                btnEdit.setStyle(base + "-fx-background-color:rgba(99,102,241,0.25); -fx-text-fill:#a5b4fc;");
                btnDelete.setStyle(base + "-fx-background-color:rgba(248,113,113,0.25); -fx-text-fill:#fda4af;");
                box.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> onEditChapitre(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> onDeleteChapitre(getTableView().getItems().get(getIndex())));
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
        javafx.scene.Node header = tableChapitres.lookup("TableHeaderRow");
        if (header != null)
            header.setStyle("-fx-background-color:#0d1710; -fx-border-color:transparent transparent rgba(255,255,255,0.08) transparent; -fx-border-width:0 0 1 0;");
        tableChapitres.lookupAll(".column-header").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710; -fx-border-width:0;"));
        tableChapitres.lookupAll(".column-header .label").forEach(n ->
            ((javafx.scene.control.Label) n).setStyle(
                "-fx-text-fill:rgba(245,245,244,0.55); -fx-font-size:12; -fx-font-weight:700;"));
        tableChapitres.lookupAll(".filler").forEach(n ->
            n.setStyle("-fx-background-color:#0d1710;"));
        tableChapitres.lookupAll(".scroll-bar").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
        tableChapitres.lookupAll(".scroll-bar .track").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;"));
        tableChapitres.lookupAll(".scroll-bar .thumb").forEach(n ->
            n.setStyle("-fx-background-color:rgba(96,165,250,0.22); -fx-background-radius:4;"));
        tableChapitres.lookupAll(".increment-button, .decrement-button").forEach(n ->
            n.setStyle("-fx-background-color:transparent; -fx-pref-height:0; -fx-pref-width:0;"));
        tableChapitres.lookupAll(".corner").forEach(n ->
            n.setStyle("-fx-background-color:transparent;"));
    }

    private void loadTable() {
        List<Chapitre> chapitres = cours == null
            ? serviceChapitre.consulter()
            : serviceChapitre.consulterParCoursId(cours.getId());

        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (!q.isEmpty()) {
            chapitres = chapitres.stream().filter(ch ->
                ch.getTitre().toLowerCase().contains(q) ||
                ch.getContenu().toLowerCase().contains(q)
            ).toList();
        }

        tableChapitres.setItems(FXCollections.observableArrayList(chapitres));
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
    private void onNewChapitre() {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les chapitres.");
            return;
        }
        openFormWindow(null);
    }

    private void onEditChapitre(Chapitre chapitre) {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les chapitres.");
            return;
        }
        openFormWindow(chapitre);
    }

    private void onDeleteChapitre(Chapitre chapitre) {
        if (!SessionManager.isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Acces refuse", "Seul l'admin peut gerer les chapitres.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer chapitre");
        confirm.setContentText("Voulez-vous supprimer '" + chapitre.getTitre() + "' ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                var admin = SessionManager.getCurrentUser();
                if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.deleted_chapitre",
                    java.util.Map.of("titre", chapitre.getTitre()));
                serviceChapitre.supprimer(chapitre.getId());
                loadTable();
            }
        });
    }

    private void openFormWindow(Chapitre chapitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/backoffice/chapitre/form.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(chapitre == null ? "Nouveau chapitre" : "Modifier chapitre");
            stage.setResizable(false);
            stage.setScene(new Scene(loader.load(), 560, 650));
            ChapitreController ctrl = loader.getController();
            ctrl.setCours(cours);
            ctrl.setEditingChapitre(chapitre);
            stage.showAndWait();
            loadTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setEditingChapitre(Chapitre chapitre) {
        this.editingChapitre = chapitre;
        this.editMode = chapitre != null;

        if (formTitle == null) return;

        if (editMode) {
            formTitle.setText("Modifier chapitre");
            fieldTitre.setText(chapitre.getTitre());
            areaContenu.setText(chapitre.getContenu());
            fieldOrdre.setText(String.valueOf(chapitre.getOrdre()));
            fieldRessources.setText(chapitre.getRessources());
            comboRessourceType.setValue(chapitre.getRessourceType());
            fieldRessourceFichier.setText(chapitre.getRessourceFichier());
        } else {
            formTitle.setText("Nouveau chapitre");
        }
    }

    @FXML
    private void onSave() {
        if (!validateForm()) return;
        if (cours == null) {
            showAlert(Alert.AlertType.ERROR, "Cours manquant", "Aucun cours selectionne.");
            return;
        }

        String titre = fieldTitre.getText().trim();
        String contenu = areaContenu.getText().trim();
        int ordre = Integer.parseInt(fieldOrdre.getText().trim());
        String ressources = fieldRessources.getText().trim();
        String type = comboRessourceType.getValue();
        String fichier = fieldRessourceFichier.getText().trim();

        if (!editMode) {
            Chapitre chapitre = new Chapitre(titre, contenu, ordre, ressources, cours.getId(), type, fichier);
            serviceChapitre.ajouter(chapitre);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.created_chapitre",
                java.util.Map.of("titre", titre, "cours_id", String.valueOf(cours.getId())));
        } else {
            editingChapitre.setTitre(titre);
            editingChapitre.setContenu(contenu);
            editingChapitre.setOrdre(ordre);
            editingChapitre.setRessources(ressources);
            editingChapitre.setRessourceType(type);
            editingChapitre.setRessourceFichier(fichier);
            editingChapitre.setCoursId(cours.getId());
            serviceChapitre.modifier(editingChapitre);
            var admin = SessionManager.getCurrentUser();
            if (admin != null) ActivityApiClient.logAsync(admin.getId(), "admin.updated_chapitre",
                java.util.Map.of("titre", titre));
        }

        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    @FXML
    private void onCancel() {
        ((Stage) fieldTitre.getScene().getWindow()).close();
    }

    @FXML
    private void onCloseWindow() {
        ((Stage) tableChapitres.getScene().getWindow()).close();
    }

    private boolean validateForm() {
        errorTitre.setText("");
        errorContenu.setText("");
        errorOrdre.setText("");
        if (errorRessources != null) errorRessources.setText("");
        if (errorFichier    != null) errorFichier.setText("");

        boolean valid = true;

        // Titre : 3 à 100 caractères
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

        // Contenu : minimum 10 caractères pour être significatif
        String contenu = areaContenu.getText().trim();
        if (contenu.isEmpty()) {
            errorContenu.setText("Le contenu est obligatoire");
            valid = false;
        } else if (contenu.length() < 10) {
            errorContenu.setText("Le contenu doit contenir au moins 10 caractères");
            valid = false;
        } else if (contenu.length() > 1000) {
            errorContenu.setText("Le contenu ne peut pas dépasser 1000 caractères");
            valid = false;
        }

        // Ordre : entier entre 1 et 99
        try {
            int ordre = Integer.parseInt(fieldOrdre.getText().trim());
            if (ordre < 1) {
                errorOrdre.setText("L'ordre doit être au moins 1");
                valid = false;
            } else if (ordre > 99) {
                errorOrdre.setText("L'ordre ne peut pas dépasser 99");
                valid = false;
            }
        } catch (NumberFormatException e) {
            errorOrdre.setText("L'ordre doit être un nombre entier (ex: 1)");
            valid = false;
        }

        // Ressource URL : si renseignée, doit commencer par http:// ou https://
        String ressource = fieldRessources.getText().trim();
        if (!ressource.isEmpty() && !ressource.matches("^https?://.*")) {
            if (errorRessources != null) errorRessources.setText("Le lien doit commencer par http:// ou https://");
            valid = false;
        }

        // Fichier : si renseigné, doit avoir une extension reconnue
        String fichier = fieldRessourceFichier.getText().trim();
        if (!fichier.isEmpty() && !fichier.matches("(?i).*\\.(pdf|mp4|avi|mkv|png|jpg|jpeg|docx|pptx|zip)$")) {
            if (errorFichier != null) errorFichier.setText("Extension non reconnue (pdf, mp4, png, docx...)");
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
