package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.MainApp;
import tn.esprit.entities.LeaderboardEntry;
import tn.esprit.services.LeaderboardService;
import tn.esprit.session.JwtManager;

import java.io.IOException;

public class LeaderboardController {

    @FXML private ListView<LeaderboardEntry> leaderboardList;
    @FXML private ComboBox<String> niveauFilter;
    @FXML private Label statsLabel;

    // Navbar labels
    @FXML private Label labelAvatarNav;
    @FXML private Label labelCurrentUser;
    @FXML private Label labelNiveauUser;
    @FXML private MenuButton menuUser;

    private LeaderboardService leaderboardService;
    private ObservableList<LeaderboardEntry> leaderboardData;

    @FXML
    public void initialize() {
        leaderboardService = new LeaderboardService();

        // Afficher les infos utilisateur dans la navbar
        var u = JwtManager.getCurrentUser();
        if (u != null) {
            String name = u.getPrenom() + " " + u.getNom();
            String initials = u.getPrenom().substring(0,1).toUpperCase() + u.getNom().substring(0,1).toUpperCase();
            if (labelCurrentUser != null) labelCurrentUser.setText(name);
            if (labelAvatarNav != null) labelAvatarNav.setText(initials);
            if (menuUser != null) menuUser.setText(initials + " \u25be");
            if (u instanceof tn.esprit.entities.Etudiant e && e.getNiveau() != null)
                if (labelNiveauUser != null) labelNiveauUser.setText("Niveau : " + e.getNiveau());
        }

        // Style de la liste
        leaderboardList.setCellFactory(param -> new LeaderboardCell());

        // Initialiser le filtre de niveau
        niveauFilter.setItems(FXCollections.observableArrayList("Tous", "Débutant", "Intermédiaire", "Avancé"));
        niveauFilter.setValue("Tous");
        niveauFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadLeaderboard());

        // Charger les données
        loadLeaderboard();
    }

    private void loadLeaderboard() {
        String selectedNiveau = niveauFilter.getValue();
        String niveau = (selectedNiveau != null && !selectedNiveau.equals("Tous")) ? selectedNiveau : null;

        leaderboardData = FXCollections.observableArrayList(leaderboardService.getLeaderboard(niveau));
        leaderboardList.setItems(leaderboardData);

        statsLabel.setText(leaderboardService.getGlobalStats());
    }

    // Cellule personnalisée pour chaque ligne du classement
    private class LeaderboardCell extends ListCell<LeaderboardEntry> {
        private final HBox content;
        private final Label rangLabel;
        private final Label nomLabel;
        private final Label niveauLabel;
        private final Label challengesLabel;
        private final Label scoreLabel;
        private final Label moyenneLabel;
        private final Label medailleLabel;

        public LeaderboardCell() {
            rangLabel = new Label();
            rangLabel.setPrefWidth(70);
            rangLabel.setStyle("-fx-font-size:14; -fx-font-weight:700;");

            nomLabel = new Label();
            nomLabel.setPrefWidth(250);
            nomLabel.setStyle("-fx-font-size:14; -fx-text-fill:#1e1e1e; -fx-font-weight:600;");

            niveauLabel = new Label();
            niveauLabel.setPrefWidth(100);
            niveauLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-background-radius:12; -fx-padding:4 8 4 8;");

            challengesLabel = new Label();
            challengesLabel.setPrefWidth(100);
            challengesLabel.setStyle("-fx-font-size:13; -fx-text-fill:#666;");

            scoreLabel = new Label();
            scoreLabel.setPrefWidth(120);
            scoreLabel.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#7a6ad8;");

            moyenneLabel = new Label();
            moyenneLabel.setPrefWidth(80);
            moyenneLabel.setStyle("-fx-font-size:13; -fx-font-weight:600; -fx-text-fill:#f59e0b;");

            medailleLabel = new Label();
            medailleLabel.setPrefWidth(100);
            medailleLabel.setStyle("-fx-font-size:13; -fx-font-weight:700;");

            content = new HBox(15);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(12, 15, 12, 15));
            content.getChildren().addAll(rangLabel, nomLabel, niveauLabel, challengesLabel,
                    scoreLabel, moyenneLabel, medailleLabel);

            // Style alterné pour les lignes
            content.setStyle("-fx-background-color:white; -fx-border-color:transparent transparent #f0f0f0 transparent; -fx-border-width:0 0 1 0;");
        }

        @Override
        protected void updateItem(LeaderboardEntry entry, boolean empty) {
            super.updateItem(entry, empty);
            if (empty || entry == null) {
                setGraphic(null);
                return;
            }

            // Rang avec médailles
            if (entry.getRang() == 1) {
                rangLabel.setText("🥇 " + entry.getRang());
                rangLabel.setStyle("-fx-font-size:16; -fx-text-fill:#f1c40f;");
            } else if (entry.getRang() == 2) {
                rangLabel.setText("🥈 " + entry.getRang());
                rangLabel.setStyle("-fx-font-size:16; -fx-text-fill:#bdc3c7;");
            } else if (entry.getRang() == 3) {
                rangLabel.setText("🥉 " + entry.getRang());
                rangLabel.setStyle("-fx-font-size:16; -fx-text-fill:#cd7f32;");
            } else {
                rangLabel.setText(String.valueOf(entry.getRang()));
                rangLabel.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#999;");
            }

            // Nom de l'étudiant
            nomLabel.setText(entry.getNomComplet());

            // Niveau avec badge coloré
            String niveau = entry.getNiveau();
            niveauLabel.setText(niveau);
            if (niveau.equals("Débutant")) {
                niveauLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#059669; -fx-background-color:rgba(5,150,105,0.1); -fx-background-radius:12; -fx-padding:4 8 4 8;");
            } else if (niveau.equals("Intermédiaire")) {
                niveauLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#f59e0b; -fx-background-color:rgba(245,158,11,0.1); -fx-background-radius:12; -fx-padding:4 8 4 8;");
            } else if (niveau.equals("Avancé")) {
                niveauLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#ef4444; -fx-background-color:rgba(239,68,68,0.1); -fx-background-radius:12; -fx-padding:4 8 4 8;");
            } else {
                niveauLabel.setStyle("-fx-font-size:12; -fx-font-weight:600; -fx-text-fill:#888; -fx-background-color:rgba(0,0,0,0.05); -fx-background-radius:12; -fx-padding:4 8 4 8;");
            }

            // Challenges complétés
            challengesLabel.setText(String.valueOf(entry.getChallengesCompletes()));

            // Score total
            scoreLabel.setText(String.valueOf(entry.getTotalPoints()));
            if (entry.getTotalPoints() >= 1000) {
                scoreLabel.setStyle("-fx-font-size:14; -fx-font-weight:800; -fx-text-fill:#7a6ad8;");
            } else if (entry.getTotalPoints() >= 500) {
                scoreLabel.setStyle("-fx-font-size:14; -fx-font-weight:700; -fx-text-fill:#34d399;");
            } else {
                scoreLabel.setStyle("-fx-font-size:14; -fx-font-weight:600; -fx-text-fill:#888;");
            }

            // Moyenne
            moyenneLabel.setText(String.format("%.1f%%", entry.getMoyenne()));

            // Médailles
            medailleLabel.setText(entry.getMedailles());
            if (entry.getMedailles().contains("OR")) {
                medailleLabel.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#f1c40f;");
            } else if (entry.getMedailles().contains("ARGENT")) {
                medailleLabel.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#bdc3c7;");
            } else if (entry.getMedailles().contains("BRONZE")) {
                medailleLabel.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#cd7f32;");
            } else {
                medailleLabel.setStyle("-fx-font-size:13; -fx-font-weight:600; -fx-text-fill:#aaa;");
            }

            setGraphic(content);
        }
    }

    // ========== MÉTHODES DE NAVIGATION ==========

    @FXML private void onHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/layout.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onCours() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/cours/index.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onChallenges() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/showchallenges.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onEvenements() {
        try { MainApp.showEvenementsFront(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onCommunaute() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/frontoffice/communaute/index.fxml"));
            MainApp.getPrimaryStage().getScene().setRoot(loader.load());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML private void onProfile() {
        try { MainApp.showProfile(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onMesParticipations() {
        try { MainApp.showMesParticipations(null); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onMesEquipes() {
        try { MainApp.showMesEquipes(null); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onLogout() {
        JwtManager.logout();
        try { MainApp.showLogin(); } catch (Exception e) { e.printStackTrace(); }
    }
}