# 🚀 Guide Rapide: API de Tracking d'Activités

## 📋 En 3 Minutes

### Qu'est-ce que c'est?
Un système qui **enregistre toutes les actions** des utilisateurs (connexion, création de cours, consultation de challenges, etc.) et les **envoie à Symfony** pour analyse.

### Comment ça marche?

```
Utilisateur fait une action → JavaFX enregistre → Envoie à Symfony → Stocké dans MySQL
                                                                    ↓
                                                          Admin voit dans Dashboard
```

---

## 🎯 Utilisation Simple

### 1. Logger une Action (1 ligne de code!)

```java
// Connexion
ActivityApiClient.logAsync(userId, "user.login");

// Avec métadonnées
ActivityApiClient.logAsync(userId, "admin.created_cours", 
    Map.of("coursId", 123, "titre", "Python Débutant"));
```

### 2. Récupérer l'Historique

```java
// Charger les 50 dernières activités
ActivityApiClient.fetchRecentActivities(50).thenAccept(activities -> {
    // Afficher dans le tableau
    tableView.getItems().setAll(activities);
});
```

### 3. Filtrer par Utilisateur

```java
// Historique d'un utilisateur spécifique
ActivityApiClient.fetchUserActivities(userId).thenAccept(activities -> {
    // Afficher
    tableView.getItems().setAll(activities);
});
```

---

## 📊 Types d'Actions Disponibles

### Utilisateur (ETUDIANT)
| Action | Description | Icône |
|--------|-------------|-------|
| `user.login` | Connexion | 🔑 |
| `user.logout` | Déconnexion | 🚪 |
| `user.view_cours` | Consultation cours | 📚 |
| `user.view_challenges` | Consultation challenges | 🏆 |
| `user.view_evenements` | Consultation événements | 📅 |
| `user.view_communaute` | Consultation communauté | 👥 |

### Admin
| Action | Description | Icône |
|--------|-------------|-------|
| `admin.created_cours` | Création cours | ✅ |
| `admin.updated_cours` | Modification cours | ✏️ |
| `admin.deleted_cours` | Suppression cours | 🗑️ |
| `admin.suspended_student` | Suspension étudiant | ⛔ |
| `admin.created_challenge` | Création challenge | ✅ |

---

## 🔧 Configuration

### JavaFX (Client)

**Fichier:** `ActivityApiClient.java`

```java
private static final String BASE_URL   = "http://localhost:8000";  // URL Symfony
private static final String APP_TOKEN  = "autolearn-javafx-2026";  // Token auth
```

### Symfony (Serveur)

**Fichier:** `ActivityApiController.php`

```php
private const APP_TOKEN = 'autolearn-javafx-2026';  // Même token!
```

**Démarrer Symfony:**
```bash
cd symfony/autolearn
symfony server:start
```

---

## 💡 Exemples Concrets

### Exemple 1: Login avec Géolocalisation

```java
@FXML
private void onLogin() {
    User user = service.trouverParEmail(email);
    SessionManager.login(user);
    
    // ✅ Logger la connexion
    ActivityApiClient.logAsync(user.getId(), "user.login",
        Map.of("role", user.getRole(), "email", user.getEmail()));
    
    // Géolocalisation + alerte Discord (async)
    CompletableFuture.runAsync(() -> {
        ApiService.GeoInfo geo = ApiService.getMyGeoInfo();
        if (geo != null) {
            System.out.println("Connexion depuis: " + geo.city() + ", " + geo.country());
            
            ApiService.sendAdminAlert(
                "Connexion détectée",
                user.getPrenom() + " " + user.getNom() + 
                " s'est connecté depuis " + geo.toString()
            );
        }
    });
    
    MainApp.showFrontoffice();
}
```

### Exemple 2: Création de Cours par Admin

```java
@FXML
private void onCreateCours() {
    Cours cours = new Cours(titre, description, niveau);
    serviceCours.ajouter(cours);
    
    // ✅ Logger l'action admin
    var admin = SessionManager.getCurrentUser();
    ActivityApiClient.logAsync(admin.getId(), "admin.created_cours",
        Map.of(
            "coursId", cours.getId(),
            "titre", cours.getTitre(),
            "niveau", cours.getNiveau()
        ));
    
    showSuccess("Cours créé avec succès!");
}
```

### Exemple 3: Dashboard Admin

```java
public class ActivityDashboardController {
    
    @FXML private TableView<ActivityEntry> tableActivities;
    
    @FXML
    public void initialize() {
        loadActivities();
    }
    
    private void loadActivities() {
        ActivityApiClient.fetchRecentActivities(100).thenAccept(activities -> {
            javafx.application.Platform.runLater(() -> {
                if (activities.isEmpty()) {
                    // Symfony offline → fallback MySQL
                    var dbActivities = ActivityApiClient.fetchFromDbDirect(100);
                    tableActivities.getItems().setAll(dbActivities);
                } else {
                    tableActivities.getItems().setAll(activities);
                }
            });
        });
    }
    
    @FXML
    private void onRefresh() {
        loadActivities();
    }
}
```

---

## 🌐 APIs Externes Intégrées

### 1. Gravatar - Avatar Utilisateur

```java
String avatarUrl = ApiService.getGravatarUrl("ilef@example.com", 80);
// → https://www.gravatar.com/avatar/abc123...?s=80&d=identicon
```

### 2. HaveIBeenPwned - Mot de Passe Compromis

```java
int breachCount = ApiService.checkPasswordBreached("password123");
if (breachCount > 0) {
    System.out.println("⚠️ Trouvé dans " + breachCount + " fuites!");
}
```

### 3. ip-api.com - Géolocalisation

```java
ApiService.GeoInfo geo = ApiService.getMyGeoInfo();
System.out.println(geo.city() + ", " + geo.country());  // "Tunis, Tunisia"
```

### 4. Discord Webhook - Alertes Admin

```java
ApiService.sendAdminAlert(
    "Nouvelle inscription",
    "Ilef Yousfi vient de créer un compte"
);
```

---

## 🔐 Sécurité

### Token d'Authentification
Toutes les requêtes incluent un header:
```
X-App-Token: autolearn-javafx-2026
```

### Données Stockées
- ✅ User ID, action, date/heure
- ✅ IP, localisation (pour audit)
- ✅ Métadonnées (rôle, email, etc.)
- ❌ **Jamais de mots de passe!**

---

## 🚨 Troubleshooting

### Problème: "Symfony offline"
**Solution:** Vérifier que Symfony tourne sur `http://localhost:8000`
```bash
symfony server:start
```

### Problème: "401 Unauthorized"
**Solution:** Vérifier que le token est identique dans JavaFX et Symfony

### Problème: "Aucune donnée"
**Solution:** Le système utilise automatiquement le fallback MySQL direct

---

## 📈 Statistiques Disponibles

Le dashboard admin peut afficher:
- ✅ Nombre total d'activités
- ✅ Activités par utilisateur
- ✅ Activités par type (login, création, etc.)
- ✅ Activités par date
- ✅ Localisations des connexions
- ✅ Graphiques de tendances

---

## 🎓 Checklist d'Intégration

- [ ] Symfony installé et démarré
- [ ] Table `user_activity` créée
- [ ] `ActivityApiController.php` configuré
- [ ] Token identique dans JavaFX et Symfony
- [ ] `ActivityApiClient.java` importé
- [ ] Actions loggées dans les controllers
- [ ] Dashboard admin créé
- [ ] Tests effectués

---

## 📞 Besoin d'Aide?

**Documentation complète:** `DOCUMENTATION_API_TRACKING.md`

**Développeur:** Ilef Yousfi  
**Email:** ilefyousfi7@gmail.com

---

**C'est tout! Vous êtes prêt à tracker toutes les activités! 🚀**
