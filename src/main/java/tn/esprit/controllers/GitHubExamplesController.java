package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.services.GitHubService;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * CONTROLLER : EXPLORATEUR DE CODE GITHUB
 * ═══════════════════════════════════════════════════════════════
 * Permet à l'étudiant de rechercher des repositories GitHub
 * par langage de programmation et mot-clé.
 *
 * FONCTIONNALITÉS :
 *   - Recherche de repositories via l'API GitHub
 *   - Affichage des résultats sous forme de cartes (nom, stars, langage)
 *   - Aperçu des détails du repository sélectionné
 *   - Ouverture du repository dans le navigateur
 *   - Bouton "Retour aux Cours"
 *
 * API UTILISÉE : GitHub REST API v3
 *   - Sans token : 60 requêtes/heure
 *   - Avec token : 5000 requêtes/heure (configuré dans config.properties)
 *
 * DÉPENDANCES :
 *   - GitHubService : fait les appels HTTP à l'API GitHub
 *   - ConfigLoader : lit le token GitHub depuis config.properties
 * ═══════════════════════════════════════════════════════════════
 */
public class GitHubExamplesController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private Button searchButton;

    @FXML
    private ListView<GitHubService.GitHubRepository> resultsListView;

    @FXML
    private TextArea codePreviewArea;

    @FXML
    private Button openInBrowserButton;

    @FXML
    private Label statusLabel;

    private GitHubService githubService;
    private ObservableList<GitHubService.CodeExample> currentResults;
    private String selectedUrl;

    @FXML
    public void initialize() {
        // Charger le token GitHub depuis config.properties (optionnel)
        String token = tn.esprit.tools.ConfigLoader.getGitHubToken();
        if (token != null) {
            githubService = new GitHubService(token); // Avec token : 5000 req/h
        } else {
            githubService = new GitHubService(); // Sans token : 60 req/h
        }
        currentResults = FXCollections.observableArrayList();

        // Remplir le ComboBox des langages
        languageComboBox.setItems(FXCollections.observableArrayList(
            "Java", "Python", "JavaScript", "C++", "C#", "PHP", "Ruby", "Go", "Swift", "Kotlin"
        ));
        languageComboBox.setValue("Java");

        // Configurer la cellule personnalisée pour la ListView
        resultsListView.setCellFactory(lv -> new javafx.scene.control.ListCell<GitHubService.GitHubRepository>() {
            @Override
            protected void updateItem(GitHubService.GitHubRepository repo, boolean empty) {
                super.updateItem(repo, empty);
                if (empty || repo == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(createRepoCard(repo));
                    setText(null);
                }
                setStyle("-fx-background-color: transparent; -fx-padding: 5;");
            }
        });

        // Configurer les événements
        searchButton.setOnAction(e -> searchCode());
        openInBrowserButton.setOnAction(e -> openInBrowser());
        
        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                GitHubService.CodeExample example = new GitHubService.CodeExample(
                    newVal.getName(),
                    "README.md",
                    newVal.getUrl(),
                    newVal.getFullName()
                );
                loadCodePreview(example);
            }
        });

        openInBrowserButton.setDisable(true);
        statusLabel.setText("Entrez un mot-clé pour rechercher des repositories GitHub");
    }

    @FXML
    private void searchCode() {
        String query = searchField.getText().trim();
        String language = languageComboBox.getValue();

        if (query.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un mot-clé de recherche");
            return;
        }

        statusLabel.setText("🔍 Recherche en cours...");
        searchButton.setDisable(true);

        // Recherche dans un thread séparé pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                // Appel à l'API GitHub pour chercher des repositories
                List<GitHubService.GitHubRepository> repos = githubService.searchRepositories(language, query, 10);
                
                javafx.application.Platform.runLater(() -> {
                    currentResults.clear();
                    resultsListView.getItems().clear();
                    
                    for (GitHubService.GitHubRepository repo : repos) {
                        resultsListView.getItems().add(repo);
                        currentResults.add(new GitHubService.CodeExample(
                            repo.getName(),
                            "README.md",
                            repo.getUrl(),
                            repo.getFullName()
                        ));
                    }
                    
                    statusLabel.setText("✅ " + repos.size() + " repositories trouvés");
                    searchButton.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur lors de la recherche");
                    searchButton.setDisable(false);
                    showAlert("Erreur", "Impossible de rechercher : " + e.getMessage());
                });
            }
        }).start();
    }

    private VBox createRepoCard(GitHubService.GitHubRepository repo) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8; " +
                     "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1; " +
                     "-fx-cursor: hand;");
        
        // Nom du repository avec icône
        HBox nameBox = new HBox(8);
        nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label icon = new Label("📦");
        icon.setStyle("-fx-font-size: 16px;");
        Label name = new Label(repo.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #212529;");
        name.setWrapText(true);
        nameBox.getChildren().addAll(icon, name);
        
        // Owner avec icône
        HBox ownerBox = new HBox(6);
        ownerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ownerIcon = new Label("👤");
        ownerIcon.setStyle("-fx-font-size: 12px;");
        String[] parts = repo.getFullName().split("/");
        Label owner = new Label(parts.length > 0 ? parts[0] : "Unknown");
        owner.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
        ownerBox.getChildren().addAll(ownerIcon, owner);
        
        // Description
        Label desc = new Label(repo.getDescription());
        desc.setWrapText(true);
        desc.setMaxWidth(340);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057; -fx-line-spacing: 2;");
        
        // Footer avec stats
        HBox footer = new HBox(15);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        // Stars
        HBox starsBox = new HBox(4);
        starsBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label starIcon = new Label("⭐");
        starIcon.setStyle("-fx-font-size: 11px;");
        Label stars = new Label(String.valueOf(repo.getStars()));
        stars.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #f59e0b;");
        starsBox.getChildren().addAll(starIcon, stars);
        
        // Language badge
        Label langBadge = new Label(repo.getLanguage());
        langBadge.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                          "-fx-font-size: 10px; -fx-font-weight: bold; " +
                          "-fx-padding: 3 8; -fx-background-radius: 10;");
        
        footer.getChildren().addAll(starsBox, langBadge);
        
        card.getChildren().addAll(nameBox, ownerBox, desc, footer);
        
        // Effet hover
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 12; -fx-background-radius: 8; " +
                         "-fx-border-color: #667eea; -fx-border-radius: 8; -fx-border-width: 2; " +
                         "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 8, 0, 0, 2);");
        });
        
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 8; " +
                         "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1; " +
                         "-fx-cursor: hand;");
        });
        
        return card;
    }

    private void loadCodePreview(GitHubService.CodeExample example) {
        selectedUrl = example.getUrl();
        openInBrowserButton.setDisable(false);
        
        // Extraire les informations du repository
        String[] parts = example.getRepository().split("/");
        String owner = parts.length > 0 ? parts[0] : "Unknown";
        String repoName = parts.length > 1 ? parts[1] : "Unknown";
        
        // Créer un affichage structuré et coloré
        StringBuilder preview = new StringBuilder();
        preview.append("╔══════════════════════════════════════════════════════════════╗\n");
        preview.append("║                    REPOSITORY DETAILS                        ║\n");
        preview.append("╚══════════════════════════════════════════════════════════════╝\n\n");
        
        preview.append("📦 Repository: ").append(repoName).append("\n");
        preview.append("👤 Owner: ").append(owner).append("\n");
        preview.append("🔗 Full Name: ").append(example.getRepository()).append("\n");
        preview.append("📄 File: ").append(example.getFileName()).append("\n\n");
        
        preview.append("─────────────────────────────────────────────────────────────\n\n");
        
        preview.append("💡 COMMENT UTILISER CE REPOSITORY :\n\n");
        preview.append("1️⃣  Cliquez sur '🌐 Ouvrir sur GitHub' pour voir le code complet\n");
        preview.append("2️⃣  Explorez les fichiers et la documentation\n");
        preview.append("3️⃣  Clonez le repository pour l'utiliser localement\n");
        preview.append("4️⃣  Étudiez le code pour apprendre de nouvelles techniques\n\n");
        
        preview.append("─────────────────────────────────────────────────────────────\n\n");
        
        preview.append("🔧 COMMANDES GIT :\n\n");
        preview.append("# Cloner le repository\n");
        preview.append("git clone https://github.com/").append(example.getRepository()).append(".git\n\n");
        preview.append("# Naviguer dans le dossier\n");
        preview.append("cd ").append(repoName).append("\n\n");
        preview.append("# Voir les branches disponibles\n");
        preview.append("git branch -a\n\n");
        
        preview.append("─────────────────────────────────────────────────────────────\n\n");
        
        preview.append("📚 RESSOURCES UTILES :\n\n");
        preview.append("• README: https://github.com/").append(example.getRepository()).append("#readme\n");
        preview.append("• Issues: https://github.com/").append(example.getRepository()).append("/issues\n");
        preview.append("• Wiki: https://github.com/").append(example.getRepository()).append("/wiki\n\n");
        
        preview.append("─────────────────────────────────────────────────────────────\n\n");
        
        preview.append("💎 ASTUCE PRO :\n");
        preview.append("Créez un token GitHub pour accéder aux fichiers directement ici !\n");
        preview.append("👉 https://github.com/settings/tokens\n");
        
        codePreviewArea.setText(preview.toString());
    }

    @FXML
    private void openInBrowser() {
        if (selectedUrl != null) {
            try {
                Desktop.getDesktop().browse(new URI(selectedUrl));
            } catch (Exception e) {
                showAlert("Erreur", "Impossible d'ouvrir le navigateur: " + e.getMessage());
            }
        }
    }

    public void setApiToken(String token) {
        if (token != null && !token.isEmpty()) {
            this.githubService = new GitHubService(token);
            statusLabel.setText("API Token configuré - Limite augmentée à 5000 requêtes/heure");
        }
    }

    @FXML
    private void onRetourCours() {
        try {
            tn.esprit.MainApp.showCoursPage();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de retourner aux cours: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
