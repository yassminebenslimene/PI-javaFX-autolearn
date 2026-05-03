package tn.esprit;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import tn.esprit.controllers.BackofficeController;
import tn.esprit.controllers.evenement.front.*;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Participation;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static BackofficeController backofficeController;
    private static javafx.application.HostServices hostServices;

    /** Ouvre une URL dans le navigateur par défaut du système. */
    public static void openUrl(String url) {
        if (url == null || url.isBlank() || "#".equals(url)) return;
        try {
            if (hostServices != null) {
                hostServices.showDocument(url);
            } else {
                // Fallback : java.awt.Desktop
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            System.err.println("[MainApp] Impossible d'ouvrir l'URL : " + e.getMessage());
        }
    }

    public static void setBackofficeController(BackofficeController c) {
        backofficeController = c;
    }

    public static void showBackofficeProfile() throws Exception {
        if (backofficeController != null) backofficeController.navigateToProfile();
        else showBackoffice();
    }

    public static void showBackofficeView(String fxml, String title) throws Exception {
        if (backofficeController != null) {
            backofficeController.loadView(fxml);
        } else showBackoffice();
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        hostServices = getHostServices();
        // Vider les caches des services API au démarrage
        tn.esprit.services.TechNewsService.clearCache();
        tn.esprit.services.GeoLocationService.clearCache();
        primaryStage.setTitle("AutoLearn");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setMaximized(true);

        // Auto-start Face ID Python server in background
        startFaceIdServer();

        showLanding();
        primaryStage.show();
    }

    /**
     * DEPRECATED: Old Python Face ID server - now using Face++ API directly
     * Starts the Python Face ID server automatically if not already running.
     * Looks for faceid_server.py in the project directory.
     */
    private static Process faceIdProcess;

    private static void startFaceIdServer() {
        // DISABLED: Now using Face++ API directly in FaceIdService
        // No need to start Python server anymore
        System.out.println("[FaceID] Using Face++ API (Python server disabled)");
        return;
        
        /* OLD CODE - COMMENTED OUT
        // Check if already running
        if (tn.esprit.services.FaceIdService.isServerRunning()) {
            System.out.println("[FaceID] Server already running");
            return;
        }

        Thread t = new Thread(() -> {
            try {
                // Find faceid_server.py — try project root first, then user home
                java.nio.file.Path serverScript = null;
                String[] searchPaths = {
                    "faceid_server.py",
                    System.getProperty("user.dir") + "/faceid_server.py",
                    System.getProperty("user.home") + "/faceid_server.py"
                };
                for (String path : searchPaths) {
                    if (java.nio.file.Files.exists(java.nio.file.Path.of(path))) {
                        serverScript = java.nio.file.Path.of(path);
                        break;
                    }
                }

                if (serverScript == null) {
                    System.err.println("[FaceID] faceid_server.py not found - Face ID disabled");
                    return;
                }

                System.out.println("[FaceID] Starting server: " + serverScript);

                ProcessBuilder pb = new ProcessBuilder("python", serverScript.toString());
                pb.directory(serverScript.getParent().toFile());
                pb.redirectErrorStream(true);
                faceIdProcess = pb.start();

                // Wait up to 5 seconds for server to start
                for (int i = 0; i < 10; i++) {
                    Thread.sleep(500);
                    if (tn.esprit.services.FaceIdService.isServerRunning()) {
                        System.out.println("[FaceID] Server started successfully");
                        return;
                    }
                }
                System.err.println("[FaceID] Server did not start in time");

            } catch (Exception e) {
                System.err.println("[FaceID] Could not start server: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.setName("faceid-server-starter");
        t.start();
        */
    }

    /** Stop the Face ID server when app closes */
    @Override
    public void stop() throws Exception {
        if (faceIdProcess != null && faceIdProcess.isAlive()) {
            faceIdProcess.destroy();
            System.out.println("[FaceID] Server stopped");
        }
        super.stop();
    }

    public static void showLanding() throws Exception {
        load("/views/landing.fxml");
        primaryStage.setTitle("AutoLearn — Bienvenue");
        primaryStage.setMaximized(true);
    }

    public static void showRegister() throws Exception {
        load("/views/auth/register.fxml");
        primaryStage.setTitle("AutoLearn — Inscription");
        primaryStage.setMaximized(true);
    }

    public static void showLogin() throws Exception {
        load("/views/auth/login.fxml");
        primaryStage.setTitle("AutoLearn — Connexion");
        primaryStage.setMaximized(true);
    }

    public static void showResetPassword() throws Exception {
        load("/views/auth/reset_password.fxml");
        primaryStage.setTitle("AutoLearn — Réinitialisation");
    }

    public static void showBackoffice() throws Exception {
        load("/views/backoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Backoffice");
    }

    public static void showFrontoffice() throws Exception {
        load("/views/frontoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Espace Étudiant");
    }

    public static void showProfile() throws Exception {
        load("/views/profile.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Mon Profil");
    }

    /**
     * Opens Face ID login dialog as a modal popup.
     */
    public static void showFaceIdLogin(String prefillEmail) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            MainApp.class.getResource("/views/auth/face_id.fxml"));
        javafx.scene.Parent root = loader.load();
        tn.esprit.controllers.FaceIdController ctrl = loader.getController();
        ctrl.setMode(tn.esprit.controllers.FaceIdController.Mode.LOGIN);
        if (prefillEmail != null && !prefillEmail.isEmpty()) {
            ctrl.prefillEmail(prefillEmail);
        }

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Face ID — Connexion");
        dialog.setResizable(false);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    /**
     * Opens Face ID register dialog as a modal popup (from profile page).
     */
    public static void showFaceIdRegister() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            MainApp.class.getResource("/views/auth/face_id.fxml"));
        javafx.scene.Parent root = loader.load();
        tn.esprit.controllers.FaceIdController ctrl = loader.getController();
        ctrl.setMode(tn.esprit.controllers.FaceIdController.Mode.REGISTER);

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.initOwner(primaryStage);
        dialog.setTitle("Face ID — Enregistrement");
        dialog.setResizable(false);
        dialog.setScene(new Scene(root));
        dialog.show();
    }

    /** Stub — GestionEvenement module fills this in */
    public static void showEvenements() throws Exception {
        load("/views/frontoffice/evenements.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Événements");
    }

    public static void showEvenementsFront() throws Exception {
        load("/views/frontoffice/evenements.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Événements");
    }

    public static void showJoinEvent(Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/join_event.fxml");
        setScene(loader);
        JoinEventController ctrl = loader.getController();
        ctrl.setEvenement(ev);
        primaryStage.setTitle("AutoLearn — Rejoindre l'événement");
    }

    public static void showCreateTeam(Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/create_team.fxml");
        setScene(loader);
        CreateTeamController ctrl = loader.getController();
        ctrl.setEvenement(ev);
        primaryStage.setTitle("AutoLearn — Créer une équipe");
    }

    public static void showTeamDetails(Equipe eq, Evenement ev, boolean showSuccess) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/team_details.fxml");
        setScene(loader);
        TeamDetailsController ctrl = loader.getController();
        ctrl.setData(eq, ev, showSuccess);
        primaryStage.setTitle("AutoLearn — Détails équipe");
    }

    public static void showEditTeam(Equipe eq, Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/edit_team.fxml");
        setScene(loader);
        EditTeamController ctrl = loader.getController();
        ctrl.setData(eq, ev);
        primaryStage.setTitle("AutoLearn — Modifier l'équipe");
    }

    public static void showMesParticipations(String successMsg) throws Exception {
        if (successMsg != null) MesParticipationsController.setPendingSuccess(successMsg);
        load("/views/frontoffice/mes_participations.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Mes Participations");
    }

    public static void showParticipationDetails(Participation p, Equipe eq, Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/participation_details.fxml");
        setScene(loader);
        ParticipationDetailsController ctrl = loader.getController();
        ctrl.setData(p, eq, ev);
        primaryStage.setTitle("AutoLearn — Détails participation");
    }

    public static void showEditParticipation(Participation p) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/edit_participation.fxml");
        setScene(loader);
        EditParticipationController ctrl = loader.getController();
        ctrl.setParticipation(p);
        primaryStage.setTitle("AutoLearn — Modifier participation");
    }

    public static void showMesEquipes(String successMsg) throws Exception {
        if (successMsg != null) MesEquipesController.setPendingSuccess(successMsg);
        load("/views/frontoffice/mes_equipes.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Mes Equipes");
    }

    public static void showSelectEvent() throws Exception {
        load("/views/frontoffice/select_event.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Choisir un evenement");
    }

    public static void showChallengesFront() throws Exception {
        load("/views/frontoffice/showchallenges.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Challenges");
    }

    public static void showLeaderboard() throws Exception {
        load("/views/frontoffice/leaderboard.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Classement");
    }

    public static void showCommunauteFront() throws Exception {
        load("/views/frontoffice/communaute/index.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Communauté");
    }

    public static void showGitHubExamples() throws Exception {
        load("/views/frontoffice/github_examples.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — GitHub Code Explorer");
    }

    public static void showTodoList() throws Exception {
        load("/views/frontoffice/todo.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Ma Liste");
    }

    public static void showCoursPage() throws Exception {
        load("/views/frontoffice/layout.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Cours");
        // Après le chargement du layout, naviguer vers la page cours
        javafx.application.Platform.runLater(() -> {
            tn.esprit.controllers.FrontofficeController.navigateToCoursPage();
        });
    }

    public static void showFeedback(Participation p, Evenement ev) throws Exception {
        FXMLLoader loader = getLoader("/views/frontoffice/feedback.fxml");
        setScene(loader);
        FeedbackController ctrl = loader.getController();
        ctrl.setData(p, ev);
        primaryStage.setTitle("AutoLearn — Feedback");
    }

    public static void showCalendrierEvenements() throws Exception {
        load("/views/frontoffice/calendrier_evenements.fxml");
        primaryStage.setMaximized(true);
        primaryStage.setTitle("AutoLearn — Calendrier des Événements");
    }

    private static FXMLLoader getLoader(String fxml) throws Exception {
        java.net.URL resource = MainApp.class.getResource(fxml);
        if (resource == null) throw new Exception("FXML not found: " + fxml);
        FXMLLoader loader = new FXMLLoader(resource);
        loader.load();
        return loader;
    }

    private static void setScene(FXMLLoader loader) {
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        primaryStage.setScene(new Scene(loader.getRoot(), screen.getWidth(), screen.getHeight()));
        primaryStage.setMaximized(true);
    }

    private static void load(String fxml) throws Exception {
        // Use screen size so the scene always fills the window
        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        java.net.URL resource = MainApp.class.getResource(fxml);
        if (resource == null) {
            // fallback: try without leading slash
            resource = MainApp.class.getResource(fxml.startsWith("/") ? fxml.substring(1) : fxml);
        }
        if (resource == null) throw new Exception("FXML not found: " + fxml);
        FXMLLoader loader = new FXMLLoader(resource);
        primaryStage.setScene(new Scene(loader.load(), screen.getWidth(), screen.getHeight()));
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    public static void main(String[] args) { launch(args); }
}
