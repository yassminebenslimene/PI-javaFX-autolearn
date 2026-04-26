# 🔐 EXPLICATION COMPLÈTE : JWT vs SESSION

## 🎯 QU'EST-CE QUE TU VEUX FAIRE ?

Tu veux remplacer le système de **session classique** (SessionManager) par un système **JWT (JSON Web Token)** pour gérer l'authentification dans ton application JavaFX.

---

## 📊 COMPARAISON : AVANT vs APRÈS

### ❌ AVANT (Session classique avec SessionManager)

```
┌─────────────────────────────────────────────────────────────┐
│                    JAVAFX APPLICATION                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  User login → Vérification DB → SessionManager.login(user)  │
│                                                              │
│  SessionManager stocke User en MÉMOIRE uniquement           │
│  ├─ private static User currentUser = ...                   │
│  └─ Perdu quand l'app se ferme                              │
│                                                              │
│  Chaque controller appelle:                                 │
│  User user = SessionManager.getCurrentUser();               │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Problèmes** :
- ❌ Pas de "Remember Me" (session perdue à la fermeture)
- ❌ Pas d'expiration automatique (sécurité)
- ❌ Pas de token pour les appels API
- ❌ Doit vérifier en DB à chaque login

---

### ✅ APRÈS (JWT avec JwtManager)

```
┌─────────────────────────────────────────────────────────────┐
│                    JAVAFX APPLICATION                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  User login → API Symfony → JWT Token reçu                  │
│                                                              │
│  JwtManager stocke JWT Token:                               │
│  ├─ En MÉMOIRE (currentToken)                               │
│  └─ Sur DISQUE (Java Preferences) → Remember Me automatique │
│                                                              │
│  JWT Token contient:                                        │
│  {                                                           │
│    "userId": 123,                                           │
│    "email": "ilef@example.com",                             │
│    "role": "ETUDIANT",                                      │
│    "prenom": "Ilef",                                        │
│    "nom": "Yousfi",                                         │
│    "exp": 1745678901  ← Expiration automatique             │
│  }                                                           │
│                                                              │
│  Chaque controller appelle:                                 │
│  User user = JwtManager.getCurrentUser();                   │
│  ↓                                                           │
│  JwtManager décode le JWT et retourne User                  │
│                                                              │
│  Tous les appels API utilisent:                             │
│  Authorization: Bearer {JWT_TOKEN}                          │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

**Avantages** :
- ✅ Remember Me automatique (token sauvegardé sur disque)
- ✅ Expiration automatique après 24h (sécurité)
- ✅ Token utilisable pour tous les appels API
- ✅ Pas besoin de requête DB (infos dans le token)
- ✅ Stateless (Symfony ne gère pas de sessions)

---

## 🔑 QU'EST-CE QU'UN JWT ?

### Structure d'un JWT

Un JWT est une chaîne de caractères en 3 parties séparées par des points :

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEyMywiZW1haWwiOiJpbGVmQGV4YW1wbGUuY29tIiwicm9sZSI6IkVUVURJQU5UIiwiZXhwIjoxNzQ1Njc4OTAxfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

│                                │                                │                                │
│           HEADER               │            PAYLOAD             │           SIGNATURE            │
│     (Base64 URL encoded)       │     (Base64 URL encoded)       │     (HMAC SHA256)              │
```

### 1. HEADER (En-tête)

```json
{
  "typ": "JWT",
  "alg": "HS256"
}
```
- **typ** : Type de token (JWT)
- **alg** : Algorithme de signature (HMAC SHA-256)

### 2. PAYLOAD (Données)

```json
{
  "iss": "autolearn-symfony",
  "aud": "autolearn-javafx",
  "iat": 1745592501,
  "exp": 1745678901,
  "userId": 123,
  "email": "ilef@example.com",
  "role": "ETUDIANT",
  "prenom": "Ilef",
  "nom": "Yousfi"
}
```
- **iss** : Issuer (qui a émis le token)
- **aud** : Audience (pour qui est le token)
- **iat** : Issued At (timestamp de création)
- **exp** : Expiration (timestamp d'expiration)
- **userId, email, role, etc.** : Données utilisateur

### 3. SIGNATURE (Sécurité)

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```
- Garantit que le token n'a pas été modifié
- Seul Symfony peut créer/vérifier la signature (secret key)

---

## 🔄 FLUX COMPLET D'AUTHENTIFICATION JWT

### 1️⃣ LOGIN

```
┌──────────┐                                    ┌──────────┐
│  JavaFX  │                                    │ Symfony  │
└────┬─────┘                                    └────┬─────┘
     │                                                │
     │  POST /api/auth/login                          │
     │  Body: {email, password}                       │
     ├───────────────────────────────────────────────>│
     │                                                │
     │                                                │ 1. Vérifie email/password en DB
     │                                                │ 2. Génère JWT token
     │                                                │ 3. Signe avec secret key
     │                                                │
     │  Response 200                                  │
     │  {                                             │
     │    "success": true,                            │
     │    "token": "eyJhbGc...",                      │
     │    "expiresIn": 86400,                         │
     │    "user": {...}                               │
     │  }                                             │
     │<───────────────────────────────────────────────┤
     │                                                │
     │ 4. Stocke token dans JwtManager                │
     │    - Mémoire (currentToken)                    │
     │    - Disque (Preferences)                      │
     │                                                │
     │ 5. Décode token pour obtenir User              │
     │                                                │
```

### 2️⃣ APPEL API (ex: Logger une activité)

```
┌──────────┐                                    ┌──────────┐
│  JavaFX  │                                    │ Symfony  │
└────┬─────┘                                    └────┬─────┘
     │                                                │
     │  POST /api/activity/log                        │
     │  Headers:                                      │
     │    Authorization: Bearer eyJhbGc...            │
     │  Body: {action, metadata}                      │
     ├───────────────────────────────────────────────>│
     │                                                │
     │                                                │ 1. Extrait token du header
     │                                                │ 2. Vérifie signature
     │                                                │ 3. Vérifie expiration
     │                                                │ 4. Décode payload
     │                                                │ 5. Extrait userId
     │                                                │ 6. Traite la requête
     │                                                │
     │  Response 201                                  │
     │  {                                             │
     │    "status": "logged",                         │
     │    "action": "user.login",                     │
     │    "userId": 123                               │
     │  }                                             │
     │<───────────────────────────────────────────────┤
     │                                                │
```

### 3️⃣ REFRESH TOKEN (avant expiration)

```
┌──────────┐                                    ┌──────────┐
│  JavaFX  │                                    │ Symfony  │
└────┬─────┘                                    └────┬─────┘
     │                                                │
     │  POST /api/auth/refresh                        │
     │  Headers:                                      │
     │    Authorization: Bearer eyJhbGc...            │
     ├───────────────────────────────────────────────>│
     │                                                │
     │                                                │ 1. Valide ancien token
     │                                                │ 2. Génère nouveau token
     │                                                │ 3. Nouvelle expiration (+24h)
     │                                                │
     │  Response 200                                  │
     │  {                                             │
     │    "success": true,                            │
     │    "token": "eyJNEW...",                       │
     │    "expiresIn": 86400                          │
     │  }                                             │
     │<───────────────────────────────────────────────┤
     │                                                │
     │ 4. Remplace ancien token par nouveau           │
     │                                                │
```

### 4️⃣ LOGOUT

```
┌──────────┐
│  JavaFX  │
└────┬─────┘
     │
     │ JwtManager.logout()
     │ ├─ Efface currentUser (mémoire)
     │ ├─ Efface currentToken (mémoire)
     │ └─ Efface token du disque (Preferences)
     │
     │ Redirection vers Landing Page
     │
```

---

## 📁 FICHIERS CRÉÉS/MODIFIÉS

### ✅ NOUVEAUX FICHIERS

| Fichier | Description |
|---------|-------------|
| `JwtManager.java` | Gère le JWT token (remplace SessionManager) |
| `AuthApiClient.java` | Communique avec l'API d'authentification Symfony |
| `JwtService.php` | Génère et valide les JWT tokens (Symfony) |
| `AuthApiController.php` | Endpoints d'authentification (Symfony) |

### 🔄 FICHIERS MODIFIÉS

| Fichier | Changement |
|---------|------------|
| `ActivityApiClient.java` | Utilise JWT au lieu de X-App-Token |
| `ActivityApiController.php` | Vérifie JWT au lieu de X-App-Token |
| `LoginController.java` | Appelle AuthApiClient.login() |
| `Tous les controllers` | SessionManager → JwtManager |

---

## 🎯 POURQUOI JWT EST MEILLEUR ?

### 1. **Sécurité**
- ✅ Expiration automatique (24h)
- ✅ Signature cryptographique (impossible à falsifier)
- ✅ Pas de session côté serveur (stateless)

### 2. **Performance**
- ✅ Pas de requête DB pour récupérer l'utilisateur
- ✅ Toutes les infos dans le token
- ✅ Symfony ne gère pas de sessions

### 3. **Expérience utilisateur**
- ✅ Remember Me automatique (token sur disque)
- ✅ Pas besoin de se reconnecter à chaque ouverture
- ✅ Refresh automatique avant expiration

### 4. **Architecture**
- ✅ API-First (prêt pour app mobile)
- ✅ Stateless (scalable)
- ✅ Standard industrie (OAuth 2.0, OpenID Connect)

---

## 🔧 COMMENT UTILISER ?

### Dans LoginController

```java
// Login avec JWT
AuthApiClient.LoginResponse response = AuthApiClient.login(email, password);
if (response.success()) {
    JwtManager.login(response.token());
    // Redirection...
}
```

### Dans n'importe quel controller

```java
// Récupérer l'utilisateur connecté
User user = JwtManager.getCurrentUser();

// Vérifier si connecté
if (!JwtManager.isLoggedIn()) {
    MainApp.showLogin();
    return;
}

// Vérifier le rôle
if (JwtManager.isAdmin()) {
    // Code admin
}
```

### Déconnexion

```java
JwtManager.logout();
MainApp.showLanding();
```

---

## 🎓 MOTS-CLÉS POUR TON PROF

- **JWT (JSON Web Token)** : Standard RFC 7519
- **Stateless authentication** : Pas de session côté serveur
- **Token-based authentication** : Authentification par token
- **HMAC SHA-256** : Algorithme de signature cryptographique
- **Base64 URL encoding** : Encodage pour transmission HTTP
- **Claims** : Données contenues dans le JWT (userId, email, role, etc.)
- **Expiration (exp)** : Timestamp d'expiration du token
- **Issuer (iss)** : Émetteur du token
- **Audience (aud)** : Destinataire du token
- **Bearer token** : Token envoyé dans le header Authorization
- **Token refresh** : Renouvellement du token avant expiration
- **Java Preferences** : Stockage persistant sur disque

---

## ✅ RÉSUMÉ

**Tu remplaces** :
- ❌ SessionManager (session en mémoire)
- ❌ Vérification DB à chaque login
- ❌ Pas de Remember Me
- ❌ Pas d'expiration

**Par** :
- ✅ JwtManager (JWT token)
- ✅ API Symfony pour login
- ✅ Remember Me automatique
- ✅ Expiration automatique (24h)
- ✅ Token pour tous les appels API
- ✅ Stateless (scalable)

**C'est un système d'authentification moderne et professionnel** 🎉
