# 📊 Documentation Complète: Système de Tracking d'Activités JavaFX ↔ Symfony

**Développeur:** Ilef Yousfi  
**Sprint:** Sprint 2 - GestionUser (JavaFX)  
**Date:** Avril 2026

---

## 🎯 Vue d'Ensemble

Ce système permet de **tracker toutes les actions des utilisateurs** dans l'application JavaFX et de les **envoyer à Symfony** pour stockage et analyse. Il inclut également des **APIs externes** pour enrichir les données (géolocalisation, vérification de mots de passe compromis, avatars).

### Architecture Globale

```
┌─────────────────┐         HTTP REST API        ┌──────────────────┐
│   JavaFX App    │ ────────────────────────────> │  Symfony Backend │
│  (Desktop)      │  POST /api/activity/log       │   (Web Server)   │
│                 │  GET  /api/activity/recent    │                  │
│                 │  GET  /api/activity/user/:id  │                  │
└─────────────────┘ <──────────────────────────── └──────────────────┘
        │                                                    │
        │                                                    ▼
        │                                          ┌──────────────────┐
        │                                          │  MySQL Database  │
        │                                          │  user_activity   │
        │                                          └──────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  APIs Externes (Enrichissement des données)                 │
├─────────────────────────────────────────────────────────────┤
│  • ip-api.com       → Géolocalisation (pays, ville, ISP)   │
│  • Gravatar         → Avatar utilisateur                     │
│  • HaveIBeenPwned   → Vérification mots de passe            │
│  • Discord Webhook  → Alertes admin en temps réel           │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Structure des Fichiers

### Fichiers JavaFX (Client)

```
src/main/java/tn/esprit/services/
├── ActivityApiClient.java    ← Client HTTP pour Symfony (tracking)
├── ApiService.java            ← APIs externes (Gravatar, HIBP, GeoIP, Webhook)
└── EmailService.java          ← Envoi d'emails (notifications)
```

### Fichiers Symfony (Serveur)

```
symfony/autolearn/
├── src/
│   ├── Controller/
│   │   └── ActivityApiController.php    ← API REST endpoints
│   ├── Entity/
│   │   └── UserActivity.php             ← Entité Doctrine
│   └── Repository/
│       └── UserActivityRepository.php   ← Requêtes DB
└── config/
    └── packages/
        └── security.yaml                 ← Config authentification API
```

---

## 🔧 Partie 1: ActivityApiClient.java (Client JavaFX)

### Rôle
Client HTTP qui **envoie les actions utilisateur à Symfony** et **récupère l'historique** pour affichage dans le dashboard admin.

### Configuration

```java
private static final String BASE_URL   = "http://localhost:8000";  // URL Symfony
private static final String APP_TOKEN  = "autolearn-javafx-2026";  // Token d'authentification
```

### Méthodes Principales

#### 1. `logAsync()` - Enregistrer une Action

**Signature:**
```java
public static void logAsync(int userId, String action, Map<String, Object> metadata)
```

**Fonctionnement:**
1. Récupère la géolocalisation via `ApiService.getMyGeoInfo()`
2. Construit un JSON avec:
   - `userId`: ID de l'utilisateur
   - `action`: Type d'action (ex: "user.login")
   - `success`: true/false
   - `ipAddress`: IP publique
   - `location`: "Ville, Pays"
   - `metadata`: Données supplémentaires (rôle, email, etc.)
3. Envoie en **POST** à `/api/activity/log`
4. **Asynchrone** → n'bloque jamais l'UI

**Exemple d'utilisation:**
```java
// Login
ActivityApiClient.logAsync(user.getId(), "user.login", 
    Map.of("role", user.getRole(), "email", user.getEmail()));

// Création de cours
ActivityApiClient.logAsync(adminId, "admin.created_cours",
    Map.of("coursId", cours.getId(), "titre", cours.getTitre()));

// Suspension d'étudiant
ActivityApiClient.logAsync(adminId, "admin.suspended_student",
    Map.of("studentId", student.getId(), "reason", "Inactivité"));
```

#### 2. `fetchRecentActivities()` - Récupérer l'Historique

**Signature:**
```java
public static CompletableFuture<List<ActivityEntry>> fetchRecentActivities(int limit)
```

**Fonctionnement:**
1. Envoie **GET** à `/api/activity/recent?limit=50`
2. Parse la réponse JSON
3. Retourne une liste d'`ActivityEntry`
4. Si Symfony est offline → retourne liste vide

**Exemple:**
```java
ActivityApiClient.fetchRecentActivities(50).thenAccept(activities -> {
    javafx.application.Platform.runLater(() -> {
        // Afficher dans le tableau
        tableView.getItems().setAll(activities);
    });
});
```

#### 3. `fetchFromDbDirect()` - Fallback Direct MySQL

**Signature:**
```java
public static List<ActivityEntry> fetchFromDbDirect(int limit)
```

**Fonctionnement:**
- Si Symfony est **offline**, lit directement depuis MySQL
- Requête SQL sur la table `user_activity`
- Retourne le même format `ActivityEntry`
- **Synchrone** (bloquant)

**Utilisation:**
```java
// Essayer Symfony d'abord
ActivityApiClient.fetchRecentActivities(50).thenAccept(activities -> {
    if (activities.isEmpty()) {
        // Fallback: lire directement depuis MySQL
        List<ActivityEntry> dbActivities = ActivityApiClient.fetchFromDbDirect(50);
        javafx.application.Platform.runLater(() -> {
            tableView.getItems().setAll(dbActivities);
        });
    }
});
```

### Record `ActivityEntry`

Structure de données retournée par les APIs:

```java
public record ActivityEntry(
    int    id,              // ID de l'activité
    int    userId,          // ID utilisateur
    String userName,        // "Prénom Nom"
    String userEmail,       // Email
    String userRole,        // "ADMIN" ou "ETUDIANT"
    String action,          // "user.login", "admin.created_cours", etc.
    boolean success,        // true/false
    String ipAddress,       // "196.187.137.141"
    String location,        // "Tunis, Tunisia"
    String createdAt,       // "20/04/2026 18:30"
    Map<String, Object> metadata  // Données supplémentaires
)
```

**Méthodes utilitaires:**
- `actionLabel()` → Libellé en français ("Connexion", "Cours créé", etc.)
- `actionIcon()` → Emoji correspondant ("🔑", "✅", "🗑️", etc.)

---

## 🌐 Partie 2: ApiService.java (APIs Externes)

### 1. Gravatar - Avatar Utilisateur

**API:** `https://www.gravatar.com/avatar/{hash}`

**Méthode:**
```java
public static String getGravatarUrl(String email, int size)
```

**Fonctionnement:**
1. Hash MD5 de l'email (en minuscules)
2. Construit l'URL Gravatar
3. Si pas d'avatar → génère un identicon

**Exemple:**
```java
String avatarUrl = ApiService.getGravatarUrl("ilef@example.com", 80);
// → https://www.gravatar.com/avatar/abc123...?s=80&d=identicon&r=pg

// Charger l'image
ApiService.fetchGravatarBytes("ilef@example.com", 80).thenAccept(bytes -> {
    if (bytes != null) {
        Image img = new Image(new ByteArrayInputStream(bytes));
        imageView.setImage(img);
    }
});
```

### 2. HaveIBeenPwned - Vérification Mot de Passe Compromis

**API:** `https://api.pwnedpasswords.com/range/{prefix}`

**Méthode:**
```java
public static int checkPasswordBreached(String plainPassword)
```

**Fonctionnement (k-anonymity):**
1. Hash SHA-1 du mot de passe
2. Envoie **seulement les 5 premiers caractères** à l'API
3. Reçoit une liste de suffixes + nombre d'occurrences
4. Compare localement pour trouver le mot de passe
5. **Le mot de passe complet ne quitte JAMAIS l'appareil**

**Exemple:**
```java
// Lors de l'inscription
String password = "password123";
int breachCount = ApiService.checkPasswordBreached(password);

if (breachCount > 0) {
    System.out.println("⚠️ Ce mot de passe a été trouvé dans " 
        + breachCount + " fuites de données!");
    // Envoyer email d'avertissement
    EmailService.sendAsync_BreachedPasswordWarning(email, prenom, breachCount);
}
```

**Version Asynchrone:**
```java
ApiService.checkPasswordBreachedAsync(password).thenAccept(count -> {
    if (count > 0) {
        javafx.application.Platform.runLater(() -> {
            showWarning("Mot de passe compromis: " + count + " fuites détectées");
        });
    }
});
```

### 3. IP Geolocation - Localisation Utilisateur

**API:** `http://ip-api.com/json/`

**Méthode:**
```java
public static GeoInfo getMyGeoInfo()
```

**Fonctionnement:**
1. Appelle ip-api.com (gratuit, pas de clé API)
2. Récupère: IP, pays, ville, ISP
3. Retourne un `record GeoInfo`

**Exemple:**
```java
GeoInfo geo = ApiService.getMyGeoInfo();
if (geo != null) {
    System.out.println("Connexion depuis: " + geo.city() + ", " + geo.country());
    System.out.println("IP: " + geo.ip() + " (ISP: " + geo.isp() + ")");
}

// Utilisé automatiquement dans ActivityApiClient.logAsync()
```

**Record GeoInfo:**
```java
public record GeoInfo(String ip, String country, String city, String isp) {
    @Override public String toString() {
        return city + ", " + country + " (" + ip + ")";
    }
}
```

### 4. Discord Webhook - Alertes Admin

**API:** Discord Incoming Webhook

**Méthode:**
```java
public static void sendAdminAlert(String title, String message)
```

**Configuration:**
```java
private static final String WEBHOOK_URL = 
    "https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_TOKEN";
```

**Fonctionnement:**
1. Envoie un JSON `{"text": "message"}` au webhook
2. Compatible Slack ET Discord
3. Asynchrone (non-bloquant)

**Exemple:**
```java
// Nouvelle inscription
ApiService.sendAdminAlert(
    "Nouvelle inscription",
    "Ilef Yousfi (ilef@example.com) vient de créer un compte ETUDIANT"
);

// Suspension automatique
ApiService.sendAdminAlert(
    "Suspension automatique",
    "Ahmed Ben Ali (ahmed@example.com) suspendu après 65 jours d'inactivité"
);

// Connexion suspecte
ApiService.sendAdminAlert(
    "Connexion détectée",
    "Amira Nefzi (amira@example.com) s'est connectée depuis Tunis, Tunisia (196.187.137.141)"
);
```

---

## 🔌 Partie 3: Symfony Backend (API REST)

### Structure de la Base de Données

**Table: `user_activity`**

```sql
CREATE TABLE user_activity (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    action VARCHAR(100) NOT NULL,
    success TINYINT(1) DEFAULT 1,
    ip_address VARCHAR(45),
    location VARCHAR(255),
    metadata JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(userId) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
);
```

### Endpoints API

#### 1. POST `/api/activity/log` - Enregistrer une Activité

**Headers:**
```
Content-Type: application/json
X-App-Token: autolearn-javafx-2026
```

**Body:**
```json
{
  "userId": 5,
  "action": "user.login",
  "success": true,
  "ipAddress": "196.187.137.141",
  "location": "Tunis, Tunisia",
  "metadata": {
    "source": "JavaFX Desktop App",
    "role": "ETUDIANT",
    "email": "ilef@example.com",
    "country": "Tunisia",
    "city": "Tunis",
    "isp": "Topnet"
  }
}
```

**Réponse:**
```json
{
  "success": true,
  "id": 123,
  "message": "Activity logged successfully"
}
```

#### 2. GET `/api/activity/recent?limit=50` - Récupérer l'Historique

**Headers:**
```
X-App-Token: autolearn-javafx-2026
```

**Réponse:**
```json
[
  {
    "id": 123,
    "userId": 5,
    "userName": "Ilef Yousfi",
    "userEmail": "ilef@example.com",
    "userRole": "ETUDIANT",
    "action": "user.login",
    "success": true,
    "ipAddress": "196.187.137.141",
    "location": "Tunis, Tunisia",
    "createdAt": "20/04/2026 18:30",
    "metadata": {
      "role": "ETUDIANT",
      "country": "Tunisia"
    }
  },
  ...
]
```

#### 3. GET `/api/activity/user/{userId}` - Historique d'un Utilisateur

**Headers:**
```
X-App-Token: autolearn-javafx-2026
```

**Réponse:** Même format que `/recent`, filtré par userId

### Fichier: ActivityApiController.php

```php
<?php
namespace App\Controller;

use App\Entity\UserActivity;
use App\Repository\UserActivityRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\Routing\Annotation\Route;

#[Route('/api/activity')]
class ActivityApiController extends AbstractController
{
    private const APP_TOKEN = 'autolearn-javafx-2026';

    #[Route('/log', methods: ['POST'])]
    public function log(Request $request, EntityManagerInterface $em): JsonResponse
    {
        // Vérifier le token
        if ($request->headers->get('X-App-Token') !== self::APP_TOKEN) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $data = json_decode($request->getContent(), true);

        $activity = new UserActivity();
        $activity->setUserId($data['userId']);
        $activity->setAction($data['action']);
        $activity->setSuccess($data['success'] ?? true);
        $activity->setIpAddress($data['ipAddress'] ?? null);
        $activity->setLocation($data['location'] ?? null);
        $activity->setMetadata($data['metadata'] ?? []);
        $activity->setCreatedAt(new \DateTime());

        $em->persist($activity);
        $em->flush();

        return $this->json([
            'success' => true,
            'id' => $activity->getId(),
            'message' => 'Activity logged successfully'
        ]);
    }

    #[Route('/recent', methods: ['GET'])]
    public function recent(
        Request $request, 
        UserActivityRepository $repo
    ): JsonResponse {
        if ($request->headers->get('X-App-Token') !== self::APP_TOKEN) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $limit = $request->query->getInt('limit', 50);
        $activities = $repo->findRecent($limit);

        return $this->json($activities);
    }

    #[Route('/user/{userId}', methods: ['GET'])]
    public function userActivities(
        int $userId,
        Request $request,
        UserActivityRepository $repo
    ): JsonResponse {
        if ($request->headers->get('X-App-Token') !== self::APP_TOKEN) {
            return $this->json(['error' => 'Unauthorized'], 401);
        }

        $activities = $repo->findByUser($userId);

        return $this->json($activities);
    }
}
```

---

## 📊 Partie 4: Utilisation dans l'Application

### Où et Quand Logger les Actions?

#### 1. **LoginController** - Connexion

```java
@FXML
private void onLogin() {
    // ... validation ...
    
    User found = service.trouverParEmail(email);
    
    // Mettre à jour last_login
    found.setLastLoginAt(Timestamp.valueOf(LocalDateTime.now()));
    service.modifier(found);
    
    // Login session
    SessionManager.login(found);
    
    // ✅ LOGGER L'ACTIVITÉ
    ActivityApiClient.logAsync(found.getId(), "user.login",
        Map.of("role", found.getRole(), "email", found.getEmail()));
    
    // Géolocalisation + alerte admin (async)
    CompletableFuture.runAsync(() -> {
        ApiService.GeoInfo geo = ApiService.getMyGeoInfo();
        String location = geo != null ? geo.toString() : "Localisation inconnue";
        
        ApiService.sendAdminAlert(
            "Connexion détectée",
            found.getPrenom() + " " + found.getNom() + 
            " (" + found.getEmail() + ") s'est connecté depuis " + location
        );
    });
    
    // Naviguer
    if ("ADMIN".equals(found.getRole())) MainApp.showBackoffice();
    else MainApp.showFrontoffice();
}
```

#### 2. **RegisterController** - Inscription

```java
@FXML
private void onRegister() {
    // ... validation ...
    
    User newUser = new Etudiant(nom, prenom, email, hashedPassword, niveau);
    service.ajouter(newUser);
    
    // ✅ LOGGER L'ACTIVITÉ
    ActivityApiClient.logAsync(newUser.getId(), "user.created",
        Map.of("role", newUser.getRole(), "email", newUser.getEmail()));
    
    // Vérifier mot de passe compromis (async)
    ApiService.checkPasswordBreachedAsync(plainPassword).thenAccept(count -> {
        if (count > 0) {
            EmailService.sendAsync_BreachedPasswordWarning(email, prenom, count);
        }
    });
    
    // Email de confirmation
    EmailService.sendRegistrationConfirmation(email, prenom, nom);
    
    // Naviguer
    MainApp.showFrontoffice();
}
```

#### 3. **BackofficeController** - Actions Admin

```java
// Création d'étudiant
@FXML
private void onCreateStudent() {
    // ... création ...
    
    ActivityApiClient.logAsync(adminId, "admin.created_student",
        Map.of("studentId", newStudent.getId(), 
               "studentEmail", newStudent.getEmail()));
}

// Suspension
@FXML
private void onSuspendStudent() {
    // ... suspension ...
    
    ActivityApiClient.logAsync(adminId, "admin.suspended_student",
        Map.of("studentId", student.getId(), 
               "reason", suspensionReason));
}

// Création de cours
@FXML
private void onCreateCours() {
    // ... création ...
    
    ActivityApiClient.logAsync(adminId, "admin.created_cours",
        Map.of("coursId", cours.getId(), 
               "titre", cours.getTitre()));
}
```

#### 4. **FrontofficeController** - Navigation Étudiant

```java
@FXML
public void onCours() {
    setActiveNav(btnNavCours);
    
    // ✅ LOGGER LA CONSULTATION
    var u = SessionManager.getCurrentUser();
    if (u != null) {
        ActivityApiClient.logAsync(u.getId(), "user.view_cours",
            Map.of("email", u.getEmail()));
    }
    
    naviguerVersCours();
}

@FXML
public void onChallenges() {
    setActiveNav(btnNavChallenges);
    
    var u = SessionManager.getCurrentUser();
    if (u != null) {
        ActivityApiClient.logAsync(u.getId(), "user.view_challenges",
            Map.of("email", u.getEmail()));
    }
    
    // ... navigation ...
}
```

### Dashboard Admin - Affichage de l'Historique

```java
public class ActivityDashboardController {
    
    @FXML private TableView<ActivityEntry> tableActivities;
    @FXML private TableColumn<ActivityEntry, String> colIcon;
    @FXML private TableColumn<ActivityEntry, String> colAction;
    @FXML private TableColumn<ActivityEntry, String> colUser;
    @FXML private TableColumn<ActivityEntry, String> colLocation;
    @FXML private TableColumn<ActivityEntry, String> colDate;
    
    @FXML
    public void initialize() {
        // Configuration des colonnes
        colIcon.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().actionIcon()));
        colAction.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().actionLabel()));
        colUser.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().userName()));
        colLocation.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().location()));
        colDate.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().createdAt()));
        
        // Charger les données
        loadActivities();
    }
    
    private void loadActivities() {
        // Essayer Symfony d'abord
        ActivityApiClient.fetchRecentActivities(100).thenAccept(activities -> {
            javafx.application.Platform.runLater(() -> {
                if (activities.isEmpty()) {
                    // Fallback: MySQL direct
                    List<ActivityEntry> dbActivities = 
                        ActivityApiClient.fetchFromDbDirect(100);
                    tableActivities.getItems().setAll(dbActivities);
                    showInfo("Données chargées depuis MySQL (Symfony offline)");
                } else {
                    tableActivities.getItems().setAll(activities);
                    showInfo("Données chargées depuis Symfony API");
                }
            });
        });
    }
    
    @FXML
    private void onRefresh() {
        loadActivities();
    }
    
    @FXML
    private void onFilterByUser() {
        // Filtrer par utilisateur sélectionné
        int userId = selectedUser.getId();
        ActivityApiClient.fetchUserActivities(userId).thenAccept(activities -> {
            javafx.application.Platform.runLater(() -> {
                tableActivities.getItems().setAll(activities);
            });
        });
    }
}
```

---

## 🔐 Sécurité

### 1. Authentification API

**Token partagé** entre JavaFX et Symfony:
```java
// JavaFX
private static final String APP_TOKEN = "autolearn-javafx-2026";

// Symfony
private const APP_TOKEN = 'autolearn-javafx-2026';
```

**Envoyé dans chaque requête:**
```
X-App-Token: autolearn-javafx-2026
```

### 2. Protection des Mots de Passe

- **Jamais en clair** dans la base de données
- **BCrypt** pour le hashing (côté JavaFX)
- **k-anonymity** pour HaveIBeenPwned (seuls 5 chars du hash SHA-1 sont envoyés)

### 3. Données Sensibles

- **IP et localisation** stockées pour audit de sécurité
- **Metadata JSON** peut contenir des infos supplémentaires
- **Pas de mots de passe** dans les logs

---

## 🚀 Déploiement

### Prérequis

**Symfony:**
```bash
cd symfony/autolearn
composer install
php bin/console doctrine:migrations:migrate
symfony server:start
```

**JavaFX:**
```bash
mvn clean compile
mvn javafx:run
```

### Configuration Production

**1. Changer l'URL Symfony:**
```java
// ActivityApiClient.java
private static final String BASE_URL = "https://autolearn.tn";
```

**2. Sécuriser le token:**
```java
// Utiliser variable d'environnement
private static final String APP_TOKEN = System.getenv("AUTOLEARN_API_TOKEN");
```

**3. Configurer le webhook Discord:**
```java
// ApiService.java
private static final String WEBHOOK_URL = System.getenv("DISCORD_WEBHOOK_URL");
```

---

## 📈 Actions Trackées

### Actions Utilisateur (ETUDIANT)
- `user.login` - Connexion
- `user.logout` - Déconnexion
- `user.created` - Inscription
- `user.updated` - Modification profil
- `user.view_cours` - Consultation cours
- `user.view_challenges` - Consultation challenges
- `user.view_evenements` - Consultation événements
- `user.view_communaute` - Consultation communauté
- `user.view_profile` - Consultation profil

### Actions Admin
- `admin.created_student` - Création étudiant
- `admin.updated_student` - Modification étudiant
- `admin.suspended_student` - Suspension étudiant
- `admin.reactivated_student` - Réactivation étudiant
- `admin.created_cours` - Création cours
- `admin.updated_cours` - Modification cours
- `admin.deleted_cours` - Suppression cours
- `admin.created_chapitre` - Création chapitre
- `admin.updated_chapitre` - Modification chapitre
- `admin.deleted_chapitre` - Suppression chapitre
- `admin.created_quiz` - Création quiz
- `admin.updated_quiz` - Modification quiz
- `admin.deleted_quiz` - Suppression quiz
- `admin.created_challenge` - Création challenge
- `admin.updated_challenge` - Modification challenge
- `admin.deleted_challenge` - Suppression challenge
- `admin.created_evenement` - Création événement
- `admin.updated_evenement` - Modification événement
- `admin.created_communaute` - Création communauté
- `admin.updated_communaute` - Modification communauté
- `admin.view_dashboard` - Consultation dashboard
- `admin.view_users` - Consultation utilisateurs

---

## 🎓 Résumé des Étapes de Développement

### Étape 1: Création de la Table MySQL
```sql
CREATE TABLE user_activity (...);
```

### Étape 2: Entité Symfony (UserActivity.php)
- Mapping Doctrine
- Getters/Setters
- Serialization JSON

### Étape 3: Repository Symfony (UserActivityRepository.php)
- `findRecent(int $limit)`
- `findByUser(int $userId)`
- Requêtes DQL avec JOIN sur user

### Étape 4: Controller Symfony (ActivityApiController.php)
- POST `/api/activity/log`
- GET `/api/activity/recent`
- GET `/api/activity/user/{userId}`
- Authentification par token

### Étape 5: Client JavaFX (ActivityApiClient.java)
- HttpClient Java 11+
- Méthodes async avec CompletableFuture
- Parsing JSON avec Gson
- Fallback MySQL direct

### Étape 6: APIs Externes (ApiService.java)
- Gravatar (avatars)
- HaveIBeenPwned (mots de passe)
- ip-api.com (géolocalisation)
- Discord webhook (alertes)

### Étape 7: Intégration dans les Controllers
- LoginController
- RegisterController
- BackofficeController
- FrontofficeController

### Étape 8: Dashboard Admin
- TableView avec colonnes
- Filtres (par utilisateur, par action, par date)
- Refresh automatique
- Export CSV

---

## 📞 Support

**Développeur:** Ilef Yousfi  
**Email:** ilefyousfi7@gmail.com  
**Projet:** AutoLearn - PI JavaFX  
**Sprint:** Sprint 2 - GestionUser

---

**Dernière mise à jour:** 20 avril 2026
