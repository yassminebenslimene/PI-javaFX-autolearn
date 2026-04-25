# Guide d'installation — AutoLearn JavaFX (Module GestionUser)
### Pour les membres de l'équipe qui veulent récupérer le travail d'Ilef

---

## ÉTAPE 1 — Prérequis à installer

### Java 17
- Télécharger : https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- Installer et configurer `JAVA_HOME` :
  - Windows : `C:\Program Files\Java\jdk-17`
  - Vérifier : `java -version` → doit afficher `17.x.x`

### Maven
- Télécharger : https://maven.apache.org/download.cgi
- Ajouter au PATH
- Vérifier : `mvn -version`

### MySQL
- Installer MySQL 8.0+
- Créer la base de données :
```sql
CREATE DATABASE autolearn_db;
```
- Importer le schéma depuis Symfony (migrations Doctrine)

### Ollama (pour le chatbot IA)
- Télécharger : https://ollama.com
- Installer et lancer
- Télécharger le modèle :
```bash
ollama pull gemma3:4b
```
- Vérifier : `ollama list` → doit afficher `gemma3:4b`

---

## ÉTAPE 2 — Cloner le projet

```bash
git clone https://github.com/yassminebenslimene/PI-javaFX-autolearn.git
cd PI-javaFX-autolearn
git checkout GestionUser
```

---

## ÉTAPE 3 — Configuration de la base de données

Fichier : `src/main/java/tn/esprit/tools/MyConnection.java`

```java
private static final String USERNAME = "root";      // ← ton user MySQL
private static final String PASSWORD = "";           // ← ton mot de passe MySQL
private static final String URL = "jdbc:mysql://localhost:3306/autolearn_db";
```

**Si ton MySQL a un mot de passe**, change la ligne `PASSWORD`.

---

## ÉTAPE 4 — Configuration Email (Gmail SMTP)

Fichier : `src/main/java/tn/esprit/services/EmailService.java`

```java
private static final String FROM_EMAIL   = "autolearn66@gmail.com";
private static final String APP_PASSWORD = "nnna xrkp hrsv ynci";
```

**Option A** : Utiliser le compte existant (demander le mot de passe à Ilef)

**Option B** : Utiliser ton propre Gmail
1. Activer la validation en 2 étapes sur ton compte Google
2. Aller sur : https://myaccount.google.com/apppasswords
3. Créer un "App Password" pour "Mail"
4. Remplacer `FROM_EMAIL` et `APP_PASSWORD`

**Option C** : Désactiver les emails (pour tester sans email)
- Commenter les appels `EmailService.send*()` dans les controllers

---

## ÉTAPE 5 — Configuration OAuth (Connexion sociale)

### GitHub OAuth ✅ (déjà configuré)
Fichier : `src/main/java/tn/esprit/services/GitHubOAuthService.java`
```java
private static final String CLIENT_ID     = "Ov23liaGRyNv6Q340ANg";
private static final String CLIENT_SECRET = "83cf8926b7e97be668ec646ef08ad7d226c81684";
```
→ Fonctionne directement, rien à changer.

### Google OAuth ⚠️ (nécessite tes propres credentials)
Fichier : `src/main/java/tn/esprit/services/GoogleOAuthService.java`
```java
private static final String CLIENT_ID     = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";
private static final String CLIENT_SECRET = "YOUR_GOOGLE_CLIENT_SECRET";
```
→ Créer une app sur https://console.cloud.google.com
→ Ou laisser tel quel (le bouton Google ne fonctionnera pas mais l'app tourne)

### Facebook OAuth ⚠️ (nécessite tes propres credentials)
Fichier : `src/main/java/tn/esprit/services/FacebookOAuthService.java`
```java
private static final String APP_ID     = "YOUR_FACEBOOK_APP_ID";
private static final String APP_SECRET = "YOUR_FACEBOOK_APP_SECRET";
```
→ Créer une app sur https://developers.facebook.com
→ Ou laisser tel quel (le bouton Facebook ne fonctionnera pas mais l'app tourne)

---

## ÉTAPE 5.5 — Face ID (Reconnaissance faciale)

### Ce qui est inclus automatiquement ✅
- **OpenCV 4.7** — dans `pom.xml`, Maven le télécharge automatiquement
- **haarcascade_frontalface_default.xml** — dans `src/main/resources/`, inclus dans le projet

### Ce qui est nécessaire sur la machine
- **Une webcam** — intégrée ou externe (USB)
  - Sans webcam → le bouton Face ID affiche "Impossible d'accéder à la webcam"
  - L'app fonctionne quand même, juste le Face ID est désactivé

### Comment ça marche
1. **Enregistrer son visage** : Profil → bouton "Activer Face ID" → regarder la webcam
2. **Se connecter** : Page Login → bouton "Face ID" → entrer son email → regarder la webcam
3. Les photos sont sauvegardées localement dans :
   - Windows : `C:\Users\{ton_nom}\.autolearn\faces\user_{id}\`
   - Linux/Mac : `~/.autolearn/faces/user_{id}/`

### Problèmes fréquents Face ID

| Erreur | Cause | Solution |
|---|---|---|
| "Detecteur non disponible" | Fichier cascade manquant | Vérifier que `haarcascade_frontalface_default.xml` est dans `src/main/resources/` |
| "Impossible d'accéder à la webcam" | Pas de webcam ou accès refusé | Brancher une webcam ou autoriser l'accès |
| "Aucun visage détecté" | Mauvais éclairage | S'assurer d'être bien éclairé face à la caméra |
| "Visage non reconnu" | Enregistrement insuffisant | Re-enregistrer son visage dans de meilleures conditions |
| OpenCV ne charge pas | Problème de drivers | Mettre à jour les drivers de la webcam |

### Note importante
Les données Face ID sont **locales** — elles ne sont pas dans la base de données et ne sont pas partagées. Chaque utilisateur doit enregistrer son propre visage sur sa propre machine.

---

Fichier : `src/main/java/tn/esprit/services/ApiService.java`

```java
private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/...";
```

→ Si tu n'as pas de serveur Discord, remplace par `""` pour désactiver :
```java
private static final String WEBHOOK_URL = "";
```

---

## ÉTAPE 7 — Configuration Symfony (Tracking d'activités)

Le tracking d'activités envoie des données à Symfony sur `http://localhost:8000`.

**Si Symfony n'est pas lancé** : pas de problème, l'app fonctionne quand même.
Les logs d'activité afficheront `[ActivityAPI] Failed to log: null` dans la console — c'est normal.

**Pour activer le tracking** :
```bash
cd symfony/autolearn
composer install
symfony server:start
```

---

## ÉTAPE 8 — Fichier .hf_token (Chatbot IA - optionnel)

Le chatbot utilise Ollama en local. Le fichier `.hf_token` n'est plus nécessaire.

Si tu vois une erreur liée à `.hf_token`, crée un fichier vide :
```bash
echo "" > .hf_token
```

---

## ÉTAPE 9 — Lancer l'application

```bash
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
mvn clean javafx:run
```

---

## RÉSUMÉ — Ce qui est OBLIGATOIRE vs OPTIONNEL

| Composant | Obligatoire ? | Sans ça |
|---|---|---|
| Java 17 | ✅ OUI | L'app ne compile pas |
| Maven | ✅ OUI | L'app ne compile pas |
| MySQL + `autolearn_db` | ✅ OUI | L'app crashe au démarrage |
| Mot de passe MySQL | ✅ Si ton MySQL en a un | Erreur de connexion DB |
| Webcam | ⚠️ Pour Face ID | Face ID désactivé, app fonctionne |
| Ollama + gemma3:4b | ⚠️ RECOMMANDÉ | Chatbot en mode basique (sans IA) |
| Email Gmail | ⚠️ OPTIONNEL | Les emails ne s'envoient pas |
| Symfony | ⚠️ OPTIONNEL | Tracking d'activités désactivé |
| GitHub OAuth | ✅ Déjà configuré | Fonctionne directement |
| Google OAuth | ❌ OPTIONNEL | Bouton Google ne fonctionne pas |
| Facebook OAuth | ❌ OPTIONNEL | Bouton Facebook ne fonctionne pas |
| Discord Webhook | ❌ OPTIONNEL | Pas d'alertes Discord |

---

## PROBLÈMES FRÉQUENTS

### "invalid target release: 17"
→ JAVA_HOME n'est pas configuré correctement
```bash
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
```

### "Connexion DB échouée"
→ MySQL n'est pas lancé ou le mot de passe est incorrect
→ Vérifier dans `MyConnection.java`

### "[ActivityAPI] Failed to log: null"
→ Normal si Symfony n'est pas lancé. Ignorer.

### "[Chatbot] Ollama not running"
→ Lancer Ollama : `ollama serve`
→ Ou installer : https://ollama.com

### "Erreur Face ID : Detecteur non disponible"
→ Le fichier `haarcascade_frontalface_default.xml` est dans `src/main/resources/`
→ Il est inclus dans le projet, rien à faire

### "[Webhook] Status: 400"
→ Le webhook Discord a expiré. Mettre `""` dans `ApiService.java`

---

## ARCHITECTURE RAPIDE

```
JavaFX App (Java 17)
├── MySQL (localhost:3306/autolearn_db)     ← Base de données
├── Symfony (localhost:8000)                ← Tracking activités (optionnel)
├── Ollama (localhost:11434)                ← IA Chatbot (optionnel)
├── Gmail SMTP                              ← Emails (optionnel)
└── APIs externes (gratuites, sans clé)
    ├── ip-api.com                          ← Géolocalisation
    ├── Gravatar                            ← Photos de profil
    └── HaveIBeenPwned                      ← Sécurité mots de passe
```

---

*Guide créé par Ilef Yousfi — Sprint 2 — AutoLearn JavaFX*
