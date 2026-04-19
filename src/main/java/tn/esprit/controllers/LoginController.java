package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import tn.esprit.MainApp;
import tn.esprit.entities.User;
import tn.esprit.services.ActivityApiClient;
import tn.esprit.services.ApiService;
import tn.esprit.services.EmailService;
import tn.esprit.services.UserService;
import tn.esprit.session.SessionManager;
import tn.esprit.tools.PasswordUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML private TextField     fieldEmail;
    @FXML private PasswordField fieldPassword;
    @FXML private TextField     fieldPasswordVisible;
    @FXML private Button        btnTogglePassword;
    @FXML private CheckBox      checkRememberMe;
    @FXML private Label         errorLabel;

    // Right panel animated elements
    @FXML private ImageView bgImage;
    @FXML private javafx.scene.layout.VBox quoteCard;
    @FXML private javafx.scene.layout.HBox statsRow;
    @FXML private javafx.scene.layout.VBox featuresList;

    private boolean passwordVisible = false;

    private final UserService service = new UserService();
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    private final Map<String, String> savedCredentials = new LinkedHashMap<>();
    private final List<String> emailHistory = new ArrayList<>();
    private final ContextMenu suggestionMenu = new ContextMenu();

    @FXML
    public void initialize() {
        loadHistory();

        String savedEmail = prefs.get("remembered_email", "");
        String savedPass  = prefs.get("remembered_pass", "");
        if (!savedEmail.isEmpty()) {
            fieldEmail.setText(savedEmail);
            fieldPassword.setText(savedPass);
            checkRememberMe.setSelected(true);
        }

        fieldEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                suggestionMenu.hide();
                return;
            }
            showSuggestions(newVal.trim());
        });

        fieldEmail.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) suggestionMenu.hide();
        });

        // Animate right panel on load
        javafx.application.Platform.runLater(this::animateRightPanel);
    }

    private void animateRightPanel() {
        if (bgImage != null) {
            // Load random course image
            String[] imgs = {"/images/course1.jpg", "/images/course2.jpg", "/images/course3.jpg"};
            try {
                var url = getClass().getResource(imgs[new Random().nextInt(imgs.length)]);
                if (url != null) bgImage.setImage(new Image(url.toExternalForm()));
            } catch (Exception ignored) {}
        }

        // Slide in quote card
        if (quoteCard != null) {
            quoteCard.setOpacity(0);
            quoteCard.setTranslateY(30);
            FadeTransition ft = new FadeTransition(Duration.millis(600), quoteCard);
            ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(200));
            TranslateTransition tt = new TranslateTransition(Duration.millis(600), quoteCard);
            tt.setFromY(30); tt.setToY(0); tt.setDelay(Duration.millis(200));
            ft.play(); tt.play();
        }

        // Fade in stats
        if (statsRow != null) {
            statsRow.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(500), statsRow);
            ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(400));
            ft.play();
        }

        // Slide in features
        if (featuresList != null) {
            featuresList.setOpacity(0);
            featuresList.setTranslateY(20);
            FadeTransition ft = new FadeTransition(Duration.millis(500), featuresList);
            ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(600));
            TranslateTransition tt = new TranslateTransition(Duration.millis(500), featuresList);
            tt.setFromY(20); tt.setToY(0); tt.setDelay(Duration.millis(600));
            ft.play(); tt.play();
        }
    }

    private void showSuggestions(String typed) {
        String lower = typed.toLowerCase();
        List<String> matches = emailHistory.stream()
            .filter(e -> e.toLowerCase().startsWith(lower))
            .toList();

        if (matches.isEmpty()) {
            suggestionMenu.hide();
            return;
        }

        suggestionMenu.getItems().clear();
        for (String email : matches) {
            MenuItem item = new MenuItem(email);
            item.setStyle("-fx-font-size:13; -fx-padding:8 14 8 14;");
            item.setOnAction(e -> {
                fieldEmail.setText(email);
                fieldEmail.positionCaret(email.length());
                suggestionMenu.hide();

                String pass = savedCredentials.get(email);
                if (pass != null && !pass.isEmpty()) {
                    fieldPassword.setText(pass);
                    checkRememberMe.setSelected(true);
                } else {
                    fieldPassword.clear();
                    checkRememberMe.setSelected(false);
                }
            });
            suggestionMenu.getItems().add(item);
        }

        suggestionMenu.show(fieldEmail, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    @FXML
    private void onTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            fieldPasswordVisible.setText(fieldPassword.getText());
            fieldPasswordVisible.setVisible(true);  fieldPasswordVisible.setManaged(true);
            fieldPassword.setVisible(false);         fieldPassword.setManaged(false);
            btnTogglePassword.setText("\uD83D\uDE48");
        } else {
            fieldPassword.setText(fieldPasswordVisible.getText());
            fieldPassword.setVisible(true);          fieldPassword.setManaged(true);
            fieldPasswordVisible.setVisible(false);  fieldPasswordVisible.setManaged(false);
            btnTogglePassword.setText("\uD83D\uDC41");
        }
    }

    private String getPassword() {
        return passwordVisible ? fieldPasswordVisible.getText() : fieldPassword.getText();
    }

    @FXML
    private void onLogin() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String email    = fieldEmail.getText().trim();
        String password = getPassword();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        User found = service.trouverParEmail(email);
        if (found == null) {
            showError("Aucun compte trouve avec cet email.");
            return;
        }
        if (!PasswordUtil.verify(password, found.getPassword())) {
            showError("Mot de passe incorrect.");
            return;
        }

        if (!found.isIsSuspended()) {
            java.util.Date lastActivityDate = found.getLastLoginAt() != null
                ? found.getLastLoginAt()
                : found.getCreatedAt();

            LocalDateTime lastActivity = lastActivityDate != null
                ? lastActivityDate.toInstant()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDateTime()
                : LocalDateTime.now();

            long daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastActivity, LocalDateTime.now());
            if (daysSince >= 60) {
                found.setIsSuspended(true);
                found.setSuspendedAt(new java.util.Date());
                found.setSuspensionReason("Inactivite prolongee (plus de 60 jours sans connexion)");
                service.modifier(found);
                EmailService.sendSuspensionNotification(found.getEmail(), found.getPrenom(),
                    "Votre compte n'a pas ete utilise depuis plus de 60 jours.");
                ApiService.sendAdminAlert(
                    "Suspension automatique",
                    found.getPrenom() + " " + found.getNom() + " (" + found.getEmail() +
                    ") a ete suspendu automatiquement apres " + daysSince + " jours d'inactivite."
                );
            }
        }

        if (found.isIsSuspended()) {
            showError("Compte suspendu : " +
                (found.getSuspensionReason() != null ? found.getSuspensionReason() : "") +
                "\nContactez autolearn66@gmail.com pour plus d'informations.");
            return;
        }

        found.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now()));
        service.modifier(found);

        addToHistory(email);

        if (checkRememberMe.isSelected()) {
            prefs.put("remembered_email", email);
            prefs.put("remembered_pass", password);
            savedCredentials.put(email, password);
            saveCredentials();
        } else {
            prefs.remove("remembered_email");
            prefs.remove("remembered_pass");
            savedCredentials.remove(email);
            saveCredentials();
        }

        SessionManager.login(found);

        ActivityApiClient.logAsync(found.getId(), "user.login",
            java.util.Map.of("role", found.getRole(), "email", found.getEmail()));

        final User loggedUser = found;
        CompletableFuture.runAsync(() -> {
            ApiService.GeoInfo geo = ApiService.getMyGeoInfo();
            String location = geo != null ? geo.toString() : "Localisation inconnue";
            System.out.println("[Login] " + loggedUser.getEmail() + " from " + location);
            ApiService.sendAdminAlert(
                "Connexion detectee",
                loggedUser.getPrenom() + " " + loggedUser.getNom() +
                " (" + loggedUser.getEmail() + ") s'est connecte depuis " + location
            );
        });

        try {
            if ("ADMIN".equals(found.getRole())) MainApp.showBackoffice();
            else                                  MainApp.showFrontoffice();
        } catch (Exception e) {
            showError("Erreur de navigation: " + e.getMessage());
        }
    }

    private void loadHistory() {
        String history = prefs.get("email_history", "");
        if (!history.isEmpty())
            emailHistory.addAll(Arrays.asList(history.split("\\|")));

        String creds = prefs.get("saved_credentials", "");
        if (!creds.isEmpty()) {
            for (String entry : creds.split("\\|")) {
                String[] parts = entry.split(":::", 2);
                if (parts.length == 2)
                    savedCredentials.put(parts[0], parts[1]);
            }
        }
    }

    private void addToHistory(String email) {
        emailHistory.remove(email);
        emailHistory.add(0, email);
        if (emailHistory.size() > 10) emailHistory.subList(10, emailHistory.size()).clear();
        prefs.put("email_history", String.join("|", emailHistory));
    }

    private void saveCredentials() {
        StringBuilder sb = new StringBuilder();
        savedCredentials.forEach((email, pass) -> {
            if (sb.length() > 0) sb.append("|");
            sb.append(email).append(":::").append(pass);
        });
        prefs.put("saved_credentials", sb.toString());
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    @FXML private void onGoToRegister() {
        try { MainApp.showRegister(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onBackToLanding() {
        try { MainApp.showLanding(); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void onForgotPassword() {
        try { MainApp.showResetPassword(); } catch (Exception e) { e.printStackTrace(); }
    }
}
