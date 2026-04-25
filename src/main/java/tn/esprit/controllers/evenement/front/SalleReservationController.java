package tn.esprit.controllers.evenement.front;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import tn.esprit.entities.Equipe;
import tn.esprit.entities.Evenement;
import tn.esprit.services.ParticipationService;

import java.net.URL;
import java.util.Map;

/**
 * Contrôleur de la salle 3D — utilise WebView + Three.js (HTML local).
 * La logique de réservation reste en Java, communique via JSBridge.
 */
public class SalleReservationController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private StackPane scene3DContainer;
    @FXML private Label labelEventName;
    @FXML private Label labelStatus;
    @FXML private Label labelSalleInfo;
    @FXML private Button btnReserver;
    @FXML private Button btnLiberer;

    private Evenement evenement;
    private Equipe equipe;
    private final ParticipationService participationService = new ParticipationService();

    private WebEngine webEngine;
    private int pendingSalleIdx = -1;
    private int pendingTableNum = -1;
    private String pendingKey = null;

    @FXML
    public void initialize() {
        if (btnReserver != null) btnReserver.setDisable(true);
        if (btnLiberer  != null) btnLiberer.setDisable(true);
    }

    public void setData(Evenement ev, Equipe eq) {
        this.evenement = ev;
        this.equipe = eq;
        if (labelEventName != null)
            labelEventName.setText(ev.getTitre() + "  —  " + ev.getLieu());
        buildWebView();
        updateButtons();
    }

    // ── WebView ──────────────────────────────────────────────────
    private void buildWebView() {
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webView.setContextMenuEnabled(false);

        // Activer JavaScript et les requêtes réseau
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        // Logger les erreurs JS pour debug
        webEngine.setOnError(e -> System.err.println("WebEngine error: " + e.getMessage()));
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, old, ex) -> {
            if (ex != null) System.err.println("WebView exception: " + ex.getMessage());
        });

        // Charger le fichier HTML local depuis les ressources
        URL htmlUrl = getClass().getResource("/views/frontoffice/salle3d.html");
        if (htmlUrl == null) {
            showError("Fichier salle3d.html introuvable dans les ressources.");
            return;
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                injectJavaBridge();
                injectReservations();
            } else if (newState == Worker.State.FAILED) {
                System.err.println("WebView FAILED to load: " + htmlUrl);
            }
        });

        webEngine.load(htmlUrl.toExternalForm());
        scene3DContainer.getChildren().setAll(webView);
        webView.prefWidthProperty().bind(scene3DContainer.widthProperty());
        webView.prefHeightProperty().bind(scene3DContainer.heightProperty());
    }

    /** Injecte le pont Java → JavaScript */
    private void injectJavaBridge() {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaBridge", new JavaBridge());
        } catch (Exception e) {
            System.err.println("Erreur injection JSBridge: " + e.getMessage());
        }
    }

    /** Envoie les réservations existantes au JavaScript */
    private void injectReservations() {
        try {
            // Construire le JSON des réservations
            Map<Integer, Integer> res = participationService.getReservationsTable(evenement.getId());
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<Integer, Integer> entry : res.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
                first = false;
            }
            json.append("}");

            webEngine.executeScript("setReservations('" + json + "')");

            if (equipe != null) {
                webEngine.executeScript("setMyEquipe(" + equipe.getId() + ")");
            }
        } catch (Exception e) {
            System.err.println("Erreur injection réservations: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        if (labelStatus != null) {
            labelStatus.setText(msg);
            labelStatus.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12;");
        }
    }

    // ── Pont JavaScript → Java ───────────────────────────────────
    /**
     * Cette classe est exposée à JavaScript via window.javaBridge.
     * Appelée quand l'étudiant clique sur une table disponible.
     */
    public class JavaBridge {
        public void onTableSelected(int salleIdx, int tableNum, String key) {
            // Exécuté sur le thread JS — on passe sur le thread JavaFX
            javafx.application.Platform.runLater(() -> {
                pendingSalleIdx = salleIdx;
                pendingTableNum = tableNum;
                pendingKey = key;

                if (labelStatus != null) {
                    labelStatus.setText("Table " + (tableNum + 1) + " sélectionnée — Cliquez Réserver");
                    labelStatus.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#22c55e;");
                }
                if (btnReserver != null) btnReserver.setDisable(equipe == null);
            });
        }
    }

    // ── Actions boutons ──────────────────────────────────────────
    @FXML
    private void onReserver() {
        if (pendingSalleIdx < 0 || equipe == null) return;
        int keyInt = pendingSalleIdx * 100 + pendingTableNum;
        boolean ok = participationService.reserverTable(evenement.getId(), equipe.getId(), keyInt);
        if (ok) {
            if (labelStatus != null) {
                labelStatus.setText("Table " + (pendingTableNum + 1) + " réservée !");
                labelStatus.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#3b82f6;");
            }
            try {
                webEngine.executeScript("confirmReservation('" + pendingSalleIdx + "_" + pendingTableNum + "'," + equipe.getId() + ")");
            } catch (Exception e) { e.printStackTrace(); }
            if (btnReserver != null) btnReserver.setDisable(true);
            if (btnLiberer  != null) btnLiberer.setDisable(false);
            pendingKey = null; pendingSalleIdx = -1;
        } else {
            if (labelStatus != null) {
                labelStatus.setText("Table déjà prise !");
                labelStatus.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#ef4444;");
            }
        }
    }

    @FXML
    private void onLiberer() {
        if (equipe == null) return;
        participationService.libererTable(evenement.getId(), equipe.getId());
        if (labelStatus != null) {
            labelStatus.setText("Réservation libérée.");
            labelStatus.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#888;");
        }
        btnLiberer.setDisable(true);
        btnReserver.setDisable(true);
        pendingKey = null;
        injectReservations(); // Rafraîchir la vue 3D
    }

    private void updateButtons() {
        if (equipe == null) {
            if (btnReserver != null) btnReserver.setDisable(true);
            if (btnLiberer  != null) btnLiberer.setDisable(true);
            return;
        }
        int myTable = participationService.getTableByEquipe(evenement.getId(), equipe.getId());
        if (btnLiberer != null) btnLiberer.setDisable(myTable < 0);
        if (myTable >= 0 && labelStatus != null) {
            int salle = myTable / 100, table = myTable % 100;
            String[] noms = {"Hall", "Salle A", "Salle B", "Salle C"};
            labelStatus.setText("Réservé : Table " + (table + 1) + " — " + (salle < noms.length ? noms[salle] : "Salle " + salle));
            labelStatus.setStyle("-fx-font-size:13; -fx-font-weight:700; -fx-text-fill:#3b82f6;");
        }
    }

    // ── Navigation ───────────────────────────────────────────────
    @FXML private void onGoSalleA() { if (webEngine != null) webEngine.executeScript("goRoom(1)"); }
    @FXML private void onGoSalleB() { if (webEngine != null) webEngine.executeScript("goRoom(2)"); }
    @FXML private void onGoSalleC() { if (webEngine != null) webEngine.executeScript("goRoom(3)"); }
    @FXML private void onRetour()   { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onHome()     { FrontNavHelper.goHome(); }
    @FXML private void onProfile()  { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout()   { FrontNavHelper.goLogout(); }
}
