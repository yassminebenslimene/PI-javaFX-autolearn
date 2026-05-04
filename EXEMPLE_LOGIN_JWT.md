# 🔐 EXEMPLE D'UTILISATION JWT DANS LoginController

## ✅ AVANT (avec SessionManager)

```java
// LoginController.java - ANCIENNE VERSION
@FXML
private void handleLogin() {
    String email = emailField.getText();
    String password = passwordField.getText();
    
    // Vérifier en base de données
    User user = userService.findByEmail(email);
    if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
        showError("Email ou mot de passe incorrect");
        return;
    }
    
    // Stocker en session
    SessionManager.login(user);
    
    // Rediriger
    if (user.getRole().equals("ADMIN")) {
        MainApp.showBackoffice();
    } else {
        MainApp.showFrontoffice();
    }
}
```

---

## 🚀 APRÈS (avec JWT)

```java
// LoginController.java - NOUVELLE VERSION AVEC JWT
import tn.esprit.services.AuthApiClient;
import tn.esprit.session.JwtManager;

@FXML
private void handleLogin() {
    String email = emailField.getText();
    String password = passwordField.getText();
    
    // Appeler l'API Symfony pour obtenir le JWT token
    AuthApiClient.LoginResponse response = AuthApiClient.login(email, password);
    
    if (!response.success()) {
        // Erreur de connexion
        if (response.reason() != null) {
            // Compte suspendu
            showError("Compte suspendu: " + response.reason());
        } else {
            showError(response.error());
        }
        return;
    }
    
    // Stocker le JWT token (remplace SessionManager.login)
    JwtManager.login(response.token());
    
    // Log l'activité
    User currentUser = JwtManager.getCurrentUser();
    ActivityApiClient.logAsync(currentUser.getId(), "user.login",
        java.util.Map.of("role", currentUser.getRole(), "email", currentUser.getEmail()));
    
    // Rediriger selon le rôle
    if (JwtManager.isAdmin()) {
        MainApp.showBackoffice();
    } else {
        MainApp.showFrontoffice();
    }
}
```

---

## 📝 CHANGEMENTS DANS TOUS LES CONTROLLERS

### 1. Remplacer SessionManager par JwtManager

**AVANT** :
```java
User user = SessionManager.getCurrentUser();
```

**APRÈS** :
```java
User user = JwtManager.getCurrentUser();
```

### 2. Vérifier si l'utilisateur est connecté

**AVANT** :
```java
if (SessionManager.getCurrentUser() == null) {
    MainApp.showLogin();
    return;
}
```

**APRÈS** :
```java
if (!JwtManager.isLoggedIn()) {
    MainApp.showLogin();
    return;
}
```

### 3. Vérifier le rôle

**AVANT** :
```java
User user = SessionManager.getCurrentUser();
if (user != null && user.getRole().equals("ADMIN")) {
    // Code admin
}
```

**APRÈS** :
```java
if (JwtManager.isAdmin()) {
    // Code admin
}
```

### 4. Déconnexion

**AVANT** :
```java
SessionManager.logout();
MainApp.showLanding();
```

**APRÈS** :
```java
JwtManager.logout();
MainApp.showLanding();
```

---

## 🔄 REFRESH AUTOMATIQUE DU TOKEN

Ajoute ce code dans `MainApp.java` pour rafraîchir automatiquement le token :

```java
// MainApp.java
public class MainApp extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // ... code existant ...
        
        // Démarrer le refresh automatique du token
        startTokenRefreshTimer();
    }
    
    /**
     * Rafraîchit automatiquement le JWT token toutes les heures
     */
    private void startTokenRefreshTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(60), event -> {
            if (JwtManager.shouldRefreshToken()) {
                String oldToken = JwtManager.getToken();
                if (oldToken != null) {
                    String newToken = AuthApiClient.refreshToken(oldToken);
                    if (newToken != null) {
                        JwtManager.login(newToken);
                        System.out.println("[MainApp] Token refreshed automatically");
                    } else {
                        // Refresh failed - logout user
                        JwtManager.logout();
                        showLanding();
                        showAlert(Alert.AlertType.WARNING, "Session expirée", 
                            "Votre session a expiré. Veuillez vous reconnecter.");
                    }
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
```

---

## ✅ AVANTAGES DU JWT

1. **Remember Me automatique** : Le token est sauvegardé sur disque
2. **Sécurité** : Expiration automatique après 24h
3. **Performance** : Pas besoin de requête DB pour récupérer l'utilisateur
4. **API-ready** : Le token peut être utilisé pour tous les appels API
5. **Stateless** : Symfony n'a pas besoin de gérer les sessions

---

## 🔧 CONFIGURATION SYMFONY

Le JWT est configuré dans :
- `JwtService.php` - Génération et validation des tokens
- `AuthApiController.php` - Endpoints d'authentification
- `ActivityApiController.php` - Utilise JWT au lieu de X-App-Token

**Secret key** : Change `autolearn-jwt-secret-2026-change-in-production` en production !

---

## 📊 FLUX COMPLET

```
1. User entre email/password dans JavaFX
   ↓
2. JavaFX → POST /api/auth/login → Symfony
   ↓
3. Symfony vérifie credentials, génère JWT token
   ↓
4. Symfony → JWT token → JavaFX
   ↓
5. JavaFX stocke JWT dans JwtManager (mémoire + disque)
   ↓
6. JavaFX décode JWT pour obtenir User info
   ↓
7. Toutes les requêtes API utilisent: Authorization: Bearer {JWT}
   ↓
8. Symfony valide JWT et extrait userId automatiquement
```

---

## 🎯 RÉSUMÉ DES CHANGEMENTS

| Fichier | Action |
|---------|--------|
| `JwtManager.java` | ✅ Créé (remplace SessionManager) |
| `AuthApiClient.java` | ✅ Créé (login/validate/refresh) |
| `ActivityApiClient.java` | ✅ Modifié (utilise JWT au lieu de X-App-Token) |
| `LoginController.java` | 🔄 À modifier (utiliser AuthApiClient) |
| `Tous les controllers` | 🔄 À modifier (SessionManager → JwtManager) |
| `MainApp.java` | 🔄 À modifier (ajouter refresh timer) |

---

**Voilà ! Tu as maintenant un système JWT complet** 🎉
