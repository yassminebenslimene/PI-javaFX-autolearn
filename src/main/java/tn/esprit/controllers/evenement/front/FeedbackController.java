package tn.esprit.controllers.evenement.front;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.Etudiant;
import tn.esprit.entities.Evenement;
import tn.esprit.entities.Participation;
import tn.esprit.services.ParticipationService;
import tn.esprit.session.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FeedbackController {

    @FXML private tn.esprit.controllers.NavbarController navbarController;
    @FXML private Label labelEventName;
    @FXML private Label labelEventMeta;
    @FXML private HBox starsGlobal;
    @FXML private Label labelGlobalRating;
    @FXML private HBox starsOrga;
    @FXML private Label badgeOrga;
    @FXML private HBox starsContenu;
    @FXML private Label badgeContenu;
    @FXML private HBox starsLieu;
    @FXML private Label badgeLieu;
    @FXML private HBox starsAnimation;
    @FXML private Label badgeAnimation;
    @FXML private HBox sentimentBox;
    @FXML private TextArea fieldComment;
    @FXML private Label labelCharCount;
    @FXML private Label labelError;

    private final ParticipationService participationService = new ParticipationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Participation participation;
    private Evenement evenement;

    private int ratingGlobal = 0;
    private int ratingOrga = 0;
    private int ratingContenu = 0;
    private int ratingLieu = 0;
    private int ratingAnimation = 0;
    private String sentiment = "";

    private static final String[] GLOBAL_LABELS = {"", "Mauvais", "Passable", "Moyen", "Tres bien", "Excellent"};
    private static final String[][] SENTIMENTS = {
        {"Tres satisfait", "^^",  "#059669", "#d1fae5", "\u2665"},
        {"Satisfait",      "^_^", "#059669", "#d1fae5", "\u263A"},
        {"Neutre",         "-_-", "#6b7280", "#f3f4f6", "\u25CB"},
        {"Decu",           "v_v", "#d97706", "#fef3c7", "\u2639"},
        {"Tres decu",      "T_T", "#dc2626", "#fee2e2", "\u2620"}
    };

    public void setData(Participation p, Evenement ev) {
        this.participation = p;
        this.evenement = ev;
        if (labelEventName != null) labelEventName.setText(ev.getTitre());
        if (labelEventMeta != null && ev.getDateDebut() != null)
            labelEventMeta.setText(ev.getDateDebut().format(FMT) + " - " + (ev.getLieu() != null ? ev.getLieu() : ""));
        // Pre-fill if feedback already exists — done after initialize() via Platform.runLater
        if (p.getFeedbacks() != null && !p.getFeedbacks().isBlank()) {
            javafx.application.Platform.runLater(this::prefillFromExisting);
        }
    }

    private void prefillFromExisting() {
        // Parse existing JSON to pre-fill ratings
        String json = participation.getFeedbacks();
        try {
            // Simple extraction without external JSON lib
            ratingGlobal = extractInt(json, "rating_global");
            ratingOrga = extractInt(json, "organisation");
            ratingContenu = extractInt(json, "contenu");
            ratingLieu = extractInt(json, "lieu");
            ratingAnimation = extractInt(json, "animation");
            String existingSentiment = extractString(json, "sentiment");
            if (!existingSentiment.isEmpty()) sentiment = existingSentiment;
            String existingComment = extractString(json, "comment");
            if (!existingComment.isEmpty() && fieldComment != null) fieldComment.setText(existingComment);

            refreshStars(starsGlobal, ratingGlobal);
            if (ratingGlobal > 0) labelGlobalRating.setText(GLOBAL_LABELS[ratingGlobal]);
            refreshStars(starsOrga, ratingOrga);
            if (ratingOrga > 0) badgeOrga.setText(ratingOrga + "/5");
            refreshStars(starsContenu, ratingContenu);
            if (ratingContenu > 0) badgeContenu.setText(ratingContenu + "/5");
            refreshStars(starsLieu, ratingLieu);
            if (ratingLieu > 0) badgeLieu.setText(ratingLieu + "/5");
            refreshStars(starsAnimation, ratingAnimation);
            if (ratingAnimation > 0) badgeAnimation.setText(ratingAnimation + "/5");
        } catch (Exception ignored) {}
    }

    private void refreshStars(HBox container, int rating) {
        for (int i = 0; i < container.getChildren().size(); i++) {
            Label star = (Label) container.getChildren().get(i);
            star.setStyle("-fx-font-size:28; -fx-cursor:hand; -fx-text-fill:"
                    + (i < rating ? "#f59e0b" : "#d1d5db") + ";");
        }
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int start = idx + search.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        try { return Integer.parseInt(json.substring(start, end)); } catch (Exception e) { return 0; }
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return "";
        return json.substring(start, end).replace("\\\"", "\"");
    }

    @FXML
    public void initialize() {
                buildStars(starsGlobal, 5, val -> {
            ratingGlobal = val;
            labelGlobalRating.setText(GLOBAL_LABELS[val]);
            labelGlobalRating.setStyle("-fx-font-size:12; -fx-text-fill:#7a6ad8; -fx-font-weight:700;");
        });
        buildStars(starsOrga, 5, val -> { ratingOrga = val; badgeOrga.setText(val + "/5"); });
        buildStars(starsContenu, 5, val -> { ratingContenu = val; badgeContenu.setText(val + "/5"); });
        buildStars(starsLieu, 5, val -> { ratingLieu = val; badgeLieu.setText(val + "/5"); });
        buildStars(starsAnimation, 5, val -> { ratingAnimation = val; badgeAnimation.setText(val + "/5"); });
        buildSentiments();

        fieldComment.textProperty().addListener((obs, old, nv) -> {
            int len = nv.length();
            if (len > 1000) {
                fieldComment.setText(old);
                return;
            }
            labelCharCount.setText(len + "/1000 caracteres");
        });
    }

    private void buildStars(HBox container, int count, java.util.function.IntConsumer onSelect) {
        Label[] stars = new Label[count];
        for (int i = 0; i < count; i++) {
            Label star = new Label("\u2605"); // ★
            star.setStyle("-fx-font-size:32; -fx-text-fill:#d1d5db; -fx-cursor:hand;");
            final int val = i + 1;
            star.setOnMouseClicked(e -> {
                onSelect.accept(val);
                for (int j = 0; j < count; j++) {
                    stars[j].setStyle("-fx-font-size:32; -fx-cursor:hand; -fx-text-fill:"
                            + (j < val ? "#f59e0b" : "#d1d5db") + ";");
                }
            });
            star.setOnMouseEntered(e -> {
                for (int j = 0; j < count; j++) {
                    stars[j].setStyle("-fx-font-size:32; -fx-cursor:hand; -fx-text-fill:"
                            + (j < val ? "#fbbf24" : "#d1d5db") + ";");
                }
            });
            star.setOnMouseExited(e -> {
                int current = getCurrentRating(container);
                for (int j = 0; j < count; j++) {
                    stars[j].setStyle("-fx-font-size:32; -fx-cursor:hand; -fx-text-fill:"
                            + (j < current ? "#f59e0b" : "#d1d5db") + ";");
                }
            });
            stars[i] = star;
            container.getChildren().add(star);
        }
    }

    private int getCurrentRating(HBox container) {
        if (container == starsGlobal) return ratingGlobal;
        if (container == starsOrga) return ratingOrga;
        if (container == starsContenu) return ratingContenu;
        if (container == starsLieu) return ratingLieu;
        if (container == starsAnimation) return ratingAnimation;
        return 0;
    }

    private void buildSentiments() {
        Button[] btns = new Button[SENTIMENTS.length];
        for (int i = 0; i < SENTIMENTS.length; i++) {
            String[] s = SENTIMENTS[i];
            final String color = s[2];
            final String bg = s[3];
            final String icon = s[4];
            final String sentimentKey = s[0].toLowerCase().replace(" ", "_");

            javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(6);
            content.setAlignment(javafx.geometry.Pos.CENTER);

            Label iconLbl = new Label(icon);
            iconLbl.setStyle("-fx-font-size:26; -fx-text-fill:" + color + ";");

            Label textLbl = new Label(s[0]);
            textLbl.setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-text-fill:#555;");

            content.getChildren().addAll(iconLbl, textLbl);

            Button btn = new Button();
            btn.setGraphic(content);
            btn.setStyle("-fx-background-color:white;"
                    + "-fx-padding:14 18 14 18; -fx-background-radius:12;"
                    + "-fx-cursor:hand; -fx-border-color:#e5e7eb; -fx-border-width:2; -fx-border-radius:12;"
                    + "-fx-min-width:110;");

            btn.setOnAction(e -> {
                sentiment = sentimentKey;
                for (Button b : btns) {
                    b.setStyle("-fx-background-color:white;"
                            + "-fx-padding:14 18 14 18; -fx-background-radius:12;"
                            + "-fx-cursor:hand; -fx-border-color:#e5e7eb; -fx-border-width:2; -fx-border-radius:12;"
                            + "-fx-min-width:110;");
                    javafx.scene.layout.VBox vc = (javafx.scene.layout.VBox) b.getGraphic();
                    ((Label) vc.getChildren().get(1)).setStyle("-fx-font-size:11; -fx-font-weight:600; -fx-text-fill:#555;");
                }
                btn.setStyle("-fx-background-color:" + bg + ";"
                        + "-fx-padding:14 18 14 18; -fx-background-radius:12;"
                        + "-fx-cursor:hand; -fx-border-color:" + color + "; -fx-border-width:2; -fx-border-radius:12;"
                        + "-fx-min-width:110;");
                textLbl.setStyle("-fx-font-size:11; -fx-font-weight:700; -fx-text-fill:" + color + ";");
            });

            btns[i] = btn;
            sentimentBox.getChildren().add(btn);
        }
    }

    @FXML
    private void onSubmit() {
        labelError.setText("");
        if (ratingGlobal == 0) {
            labelError.setText("Veuillez donner une evaluation globale.");
            return;
        }
        if (sentiment.isEmpty()) {
            labelError.setText("Veuillez selectionner votre ressenti.");
            return;
        }

        var user = SessionManager.getCurrentUser();
        String etudiantName = user != null ? user.getPrenom() + " " + user.getNom() : "";
        int etudiantId = user != null ? user.getId() : 0;

        String emoji = SENTIMENTS[getSentimentIndex()][1];
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String comment = fieldComment.getText().trim().replace("\"", "\\\"");

        String json = "[{\"etudiant_id\":" + etudiantId
                + ",\"etudiant_name\":\"" + etudiantName + "\""
                + ",\"rating_global\":" + ratingGlobal
                + ",\"rating_categories\":{\"organisation\":" + ratingOrga
                + ",\"contenu\":" + ratingContenu
                + ",\"lieu\":" + ratingLieu
                + ",\"animation\":" + ratingAnimation + "}"
                + ",\"sentiment\":\"" + sentiment + "\""
                + ",\"emoji\":\"" + emoji + "\""
                + ",\"comment\":\"" + comment + "\""
                + ",\"created_at\":\"" + now + "\"}]";

        participation.setFeedbacks(json);
        participationService.saveFeedback(participation.getId(), json);

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Merci pour votre feedback ! Votre avis nous aide a ameliorer nos evenements.");
        alert.setTitle("Feedback envoye");
        alert.setHeaderText(null);
        alert.showAndWait();
        FrontNavHelper.goEvenements();
    }

    private int getSentimentIndex() {
        for (int i = 0; i < SENTIMENTS.length; i++) {
            if (SENTIMENTS[i][0].toLowerCase().replace(" ", "_").equals(sentiment)) return i;
        }
        return 1;
    }

    @FXML private void onBack() { FrontNavHelper.goEvenements(); }
    @FXML private void onHome() { FrontNavHelper.goHome(); }
    @FXML private void onProfile() { FrontNavHelper.goProfile(); }
    @FXML private void onMesParticipations() { FrontNavHelper.goMesParticipations(null); }
    @FXML private void onLogout() { FrontNavHelper.goLogout(); }
}
