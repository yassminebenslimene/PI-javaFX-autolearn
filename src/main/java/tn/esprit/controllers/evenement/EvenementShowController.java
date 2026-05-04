package tn.esprit.controllers.evenement;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Evenement;
import tn.esprit.services.EvenementService;
import tn.esprit.services.EquipeService;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.WeatherService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EvenementShowController {

    @FXML private Label labelTitre;
    @FXML private Label labelType;
    @FXML private Label labelDescription;
    @FXML private Label labelLieu;
    @FXML private Label labelDateDebut;
    @FXML private Label labelDateFin;
    @FXML private Label labelStatut;
    @FXML private Label labelAnnule;
    @FXML private Label labelNbMax;
    @FXML private Label labelNbEquipes;
    @FXML private Label labelNbParticipations;
    @FXML private VBox weatherContainer;

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private final ParticipationService participationService = new ParticipationService();
    private final WeatherService weatherService = new WeatherService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Evenement evenement;

    public void setEvenement(Evenement e) {
        this.evenement = e;
        populate();
        loadWeather();
    }

    private void populate() {
        labelTitre.setText(evenement.getTitre());
        labelType.setText(evenement.getType());
        labelType.setStyle(getTypeStyle(evenement.getType()));
        labelDescription.setText(evenement.getDescription());
        labelLieu.setText(evenement.getLieu());
        labelDateDebut.setText(evenement.getDateDebut() != null ? evenement.getDateDebut().format(FMT) : "—");
        labelDateFin.setText(evenement.getDateFin() != null ? evenement.getDateFin().format(FMT) : "—");

        String statut = evenement.computeStatus();
        labelStatut.setText("● " + capitalize(statut));
        labelStatut.setStyle(getStatutBadgeStyle(statut));

        labelAnnule.setText(evenement.isIsCanceled() ? "Oui" : "Non");
        labelNbMax.setText(String.valueOf(evenement.getNbMax()));

        // Compter équipes et participations depuis la BD
        int nbEquipes = equipeService.countByEvenement(evenement.getId());
        int nbParticipations = participationService.countByEvenement(evenement.getId());
        labelNbEquipes.setText(String.valueOf(nbEquipes));
        labelNbParticipations.setText(String.valueOf(nbParticipations));
    }

    private void loadWeather() {
        if (weatherContainer == null || evenement.getLieu() == null) return;
        
        CompletableFuture.supplyAsync(() -> {
            try {
                return weatherService.getWeatherForEvent(evenement.getLieu() + ",TN", evenement.getDateDebut());
            } catch (Exception e) {
                System.err.println("Erreur météo: " + e.getMessage());
                return null;
            }
        }).thenAccept(weather -> Platform.runLater(() -> {
            if (weather != null && (boolean) weather.getOrDefault("available", false)) {
                displayWeather(weather);
            } else {
                weatherContainer.getChildren().clear();
                Label noWeather = new Label("⚠️ Données météo indisponibles");
                noWeather.setStyle("-fx-text-fill:rgba(255,255,255,0.5); -fx-font-size:11;");
                weatherContainer.getChildren().add(noWeather);
            }
        }));
    }

    private void displayWeather(Map<String, Object> weather) {
        weatherContainer.getChildren().clear();
        
        String emoji = weatherService.getWeatherEmoji((String) weather.get("icon"));
        String temp = weather.get("temperature").toString();
        String description = (String) weather.get("description");
        String humidity = weather.get("humidity").toString();
        String windSpeed = weather.get("wind_speed").toString();
        boolean isForecast = (boolean) weather.getOrDefault("is_forecast", false);
        
        Label weatherLabel = new Label(emoji + " " + temp + "°C — " + description);
        weatherLabel.setStyle("-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:bold;");
        
        Label detailsLabel = new Label("💧 " + humidity + "% | 💨 " + windSpeed + " km/h" + (isForecast ? " (Prévision)" : ""));
        detailsLabel.setStyle("-fx-text-fill:rgba(255,255,255,0.7); -fx-font-size:10;");
        
        weatherContainer.getChildren().addAll(weatherLabel, detailsLabel);
    }

    @FXML
    private void onModifier() {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/form.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent view = loader.load();
            EvenementFormController ctrl = loader.getController();
            ctrl.setEvenement(evenement);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onRetour() {
        try {
            URL resource = getClass().getResource("/views/backoffice/evenement/index.fxml");
            Parent view = FXMLLoader.load(resource);
            getContentArea().getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String getTypeStyle(String type) {
        if (type == null) return "-fx-text-fill:white;";
        return switch (type.toLowerCase()) {
            case "hackathon" -> "-fx-text-fill:#10b981; -fx-background-color:rgba(16,185,129,0.15); -fx-padding:4 12 4 12; -fx-background-radius:20;";
            case "conference" -> "-fx-text-fill:#6366f1; -fx-background-color:rgba(99,102,241,0.15); -fx-padding:4 12 4 12; -fx-background-radius:20;";
            case "workshop" -> "-fx-text-fill:#f59e0b; -fx-background-color:rgba(245,158,11,0.15); -fx-padding:4 12 4 12; -fx-background-radius:20;";
            default -> "-fx-text-fill:white;";
        };
    }

    private String getStatutBadgeStyle(String statut) {
        return switch (statut) {
            case "Plannifié" -> "-fx-text-fill:#60a5fa; -fx-background-color:rgba(96,165,250,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            case "En cours"  -> "-fx-text-fill:#34d399; -fx-background-color:rgba(52,211,153,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            case "Passé"     -> "-fx-text-fill:#4ade80; -fx-background-color:rgba(74,222,128,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            case "Annulé"    -> "-fx-text-fill:#fbbf24; -fx-background-color:rgba(251,191,36,0.15); -fx-font-size:12; -fx-font-weight:bold; -fx-background-radius:20; -fx-padding:4 12 4 12;";
            default          -> "-fx-text-fill:white; -fx-font-size:12;";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase().replace("_", " ");
    }

    private StackPane getContentArea() {
        return (StackPane) labelTitre.getScene().lookup("#contentArea");
    }
}
