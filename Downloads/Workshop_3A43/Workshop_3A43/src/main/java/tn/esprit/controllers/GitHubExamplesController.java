package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.services.GitHubService;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

public class GitHubExamplesController {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> languageComboBox;

    @FXML
    private Button searchButton;

    @FXML
    private ListView<String> resultsListView;

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
        // Charger le token depuis le fichier de configuration
        String token = tn.esprit.tools.ConfigLoader.getGitHubToken();
        if (token != null) {
            githubService = new GitHubService(token);
        } else {
            githubService = new GitHubService();
        }
        currentResults = FXCollections.observableArrayList();

        // Remplir le ComboBox des langages
        languageComboBox.setItems(FXCollections.observableArrayList(
            "Java", "Python", "JavaScript", "C++", "C#", "PHP", "Ruby", "Go", "Swift", "Kotlin"
        ));
        languageComboBox.setValue("Java");

        // Configurer les événements
        searchButton.setOnAction(e -> searchCode());
        openInBrowserButton.setOnAction(e -> openInBrowser());
        
        resultsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int index = resultsListView.getSelectionModel().getSelectedIndex();
                if (index >= 0 && index < currentResults.size()) {
                    loadCodePreview(currentResults.get(index));
                }
            }
        });

        openInBrowserButton.setDisable(true);
        statusLabel.setText("Entrez un mot-clé pour rechercher des exemples de code");
    }

    @FXML
    private void searchCode() {
        String query = searchField.getText().trim();
        String language = languageComboBox.getValue();

        if (query.isEmpty()) {
            showAlert("Erreur", "Veuillez entrer un mot-clé de recherche");
            return;
        }

        statusLabel.setText("Recherche en cours...");
        searchButton.setDisable(true);

        // Exécuter la recherche dans un thread séparé
        new Thread(() -> {
            try {
                List<GitHubService.CodeExample> examples = githubService.searchCode(language, query, 10);
                
                javafx.application.Platform.runLater(() -> {
                    currentResults.clear();
                    currentResults.addAll(examples);
                    
                    ObservableList<String> displayList = FXCollections.observableArrayList();
                    for (GitHubService.CodeExample example : examples) {
                        displayList.add(example.getFileName() + " - " + example.getRepository());
                    }
                    
                    resultsListView.setItems(displayList);
                    statusLabel.setText(examples.size() + " exemples trouvés");
                    searchButton.setDisable(false);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("Erreur lors de la recherche");
                    searchButton.setDisable(false);
                    showAlert("Erreur", "Impossible de rechercher les exemples: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadCodePreview(GitHubService.CodeExample example) {
        selectedUrl = example.getUrl();
        openInBrowserButton.setDisable(false);
        
        codePreviewArea.setText("Chargement du code...");
        
        new Thread(() -> {
            try {
                // Extraire owner et repo du repository
                String[] parts = example.getRepository().split("/");
                String owner = parts[0];
                String repo = parts[1];
                
                String content = githubService.getFileContent(owner, repo, example.getPath());
                
                javafx.application.Platform.runLater(() -> {
                    codePreviewArea.setText(content);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    codePreviewArea.setText("Erreur lors du chargement du fichier.\n\nCliquez sur 'Ouvrir dans le navigateur' pour voir le code sur GitHub.");
                });
            }
        }).start();
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
