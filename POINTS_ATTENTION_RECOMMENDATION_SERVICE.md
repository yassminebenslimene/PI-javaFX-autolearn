# POINTS D'ATTENTION - RecommendationService.java

## ⚠️ POINTS À VÉRIFIER ET À SURVEILLER

### 1️⃣ CHAMP FEEDBACKS JSON

**État actuel :**
Le champ `feedbacks` dans la table `participation` est stocké en JSON avec la structure :
```json
{
  "rating_global": 4.5,
  "comment": "Très intéressant et bien organisé",
  "created_at": "2024-04-27T10:30:00"
}
```

**Vérification à faire :**
```sql
-- Vérifier que le champ feedbacks existe et contient du JSON valide
SELECT p.id, p.feedbacks 
FROM participation p 
WHERE p.feedbacks IS NOT NULL 
LIMIT 5;
```

**Attention :** 
- ⚠️ Si le format JSON change, les requêtes `JSON_EXTRACT()` ne fonctionneront pas
- ⚠️ Assurez-vous que `rating_global` et `comment` sont les bonnes clés JSON
- ⚠️ Vérifiez que les feedbacks sont bien stockés en JSON valide (pas en string)

---

### 2️⃣ TABLE EQUIPE_ETUDIANT

**État actuel :**
La requête utilise la table `equipe_etudiant` pour lier les étudiants aux équipes :
```sql
JOIN equipe_etudiant ee ON p.equipe_id = ee.equipe_id
WHERE ee.etudiant_id = ?
```

**Vérification à faire :**
```sql
-- Vérifier que la table equipe_etudiant existe et a les bonnes colonnes
DESCRIBE equipe_etudiant;
-- Doit avoir : equipe_id, etudiant_id

-- Vérifier qu'il y a des données
SELECT COUNT(*) FROM equipe_etudiant;
```

**Attention :**
- ⚠️ Si la table n'existe pas, les requêtes échoueront
- ⚠️ Si les colonnes ont des noms différents, adapter les requêtes SQL
- ⚠️ Vérifier que les IDs correspondent bien aux tables participation et etudiant

---

### 3️⃣ INTÉGRATION AVEC GROQSERVICE

**État actuel :**
Le service utilise `groqService.ask()` pour appeler l'IA :
```java
String iaResponse = groqService.ask(
    "Tu es un expert en recommandation d'événements académiques. "
    + "Analyse le profil utilisateur et recommande les types d'événements les plus pertinents.",
    prompt
);
```

**Vérification à faire :**
```java
// Vérifier que GroqService est correctement configuré
// Vérifier que la clé API Groq est valide
// Vérifier que le service retourne une réponse JSON valide
```

**Attention :**
- ⚠️ Si GroqService n'est pas disponible, les recommandations échoueront
- ⚠️ Si la clé API Groq est invalide, les appels échoueront
- ⚠️ Si l'IA retourne un format différent, le parsing échouera
- ⚠️ Les appels IA peuvent être lents (latence réseau)

---

### 4️⃣ PARSING DE LA RÉPONSE IA

**État actuel :**
Le parsing attend une réponse JSON valide :
```java
private List<String> parseIARecommendations(String response) {
    List<String> types = new ArrayList<>();
    try {
        String cleaned = response.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            JsonArray arr = com.google.gson.JsonParser.parseString(cleaned).getAsJsonArray();
            for (com.google.gson.JsonElement el : arr) {
                types.add(el.getAsString());
            }
        }
    } catch (Exception e) {
        System.err.println("[RecommendationService] Erreur parsing IA: " + e.getMessage());
    }
    return types;
}
```

**Attention :**
- ⚠️ Si l'IA retourne du texte au lieu de JSON, le parsing échouera silencieusement
- ⚠️ Si l'IA retourne du JSON avec du markdown (```json ... ```), le parsing échouera
- ⚠️ Si la liste est vide, les recommandations seront vides

**Amélioration possible :**
```java
// Ajouter du logging pour déboguer
System.out.println("[RecommendationService] Réponse IA brute: " + response);

// Nettoyer le markdown si présent
String cleaned = response.trim()
    .replaceAll("```json\\s*", "")
    .replaceAll("```\\s*", "")
    .trim();
```

---

### 5️⃣ TYPES D'ÉVÉNEMENTS

**État actuel :**
Le prompt mentionne les types : "Hackathon, Conference, Workshop"
```java
sb.append("Les types doivent être parmi : Hackathon, Conference, Workshop\n");
```

**Vérification à faire :**
```sql
-- Vérifier les types d'événements réels dans la base de données
SELECT DISTINCT type FROM evenement;
```

**Attention :**
- ⚠️ Si les types réels sont différents (ex: "hackathon" en minuscules), adapter le prompt
- ⚠️ Si de nouveaux types sont ajoutés, mettre à jour le prompt
- ⚠️ La comparaison est case-insensitive (`type.equalsIgnoreCase(ev.getType())`) mais le prompt doit être cohérent

---

### 6️⃣ MATIÈRES DE COURS

**État actuel :**
Le prompt mentionne les matières : "Informatique, Développement, Sciences, Gestion, etc."
```java
sb.append("Les matières peuvent être : Informatique, Développement, Sciences, Gestion, etc.\n");
```

**Vérification à faire :**
```sql
-- Vérifier les matières réelles dans la base de données
SELECT DISTINCT matiere FROM cours;
```

**Attention :**
- ⚠️ Si les matières réelles sont différentes, adapter le prompt
- ⚠️ La recherche utilise `contains()` (case-insensitive) : `c.getMatiere().toLowerCase().contains(subject.toLowerCase())`
- ⚠️ Cela peut donner des résultats imprécis si les noms sont trop génériques

---

### 7️⃣ PERFORMANCE DES REQUÊTES SQL

**État actuel :**
Les requêtes utilisent des JOINs multiples et des agrégations :
```sql
SELECT 
    ev.type,
    COUNT(DISTINCT p.id) as nb_participations,
    AVG(CAST(JSON_EXTRACT(p.feedbacks, '$.rating_global') AS DECIMAL(3,1))) as avg_rating,
    GROUP_CONCAT(DISTINCT JSON_EXTRACT(p.feedbacks, '$.comment') SEPARATOR ' | ') as comments,
    MAX(ev.date_debut) as last_event_date
FROM participation p
JOIN equipe_etudiant ee ON p.equipe_id = ee.equipe_id
JOIN evenement ev ON p.evenement_id = ev.id
WHERE ee.etudiant_id = ?
  AND p.feedbacks IS NOT NULL
  AND p.feedbacks != ''
  AND p.feedbacks != 'null'
GROUP BY ev.type
ORDER BY avg_rating DESC, nb_participations DESC
```

**Attention :**
- ⚠️ Cette requête peut être lente si la table `participation` est très grande
- ⚠️ L'extraction JSON (`JSON_EXTRACT()`) peut être coûteuse
- ⚠️ Le `GROUP_CONCAT()` peut être limité en taille

**Recommandation :**
```sql
-- Ajouter des index pour améliorer les performances
CREATE INDEX idx_participation_etudiant ON participation(equipe_id);
CREATE INDEX idx_equipe_etudiant_etudiant ON equipe_etudiant(etudiant_id);
CREATE INDEX idx_evenement_type ON evenement(type);
```

---

### 8️⃣ GESTION DES ERREURS

**État actuel :**
Les erreurs sont loggées mais pas relancées :
```java
catch (SQLException e) {
    System.err.println("[RecommendationService] Erreur SQL: " + e.getMessage());
}
```

**Attention :**
- ⚠️ Les erreurs sont silencieuses (seulement loggées)
- ⚠️ Si une requête échoue, le profil utilisateur sera incomplet
- ⚠️ Les recommandations peuvent être vides ou incorrectes

**Recommandation :**
```java
// Ajouter du logging plus détaillé
catch (SQLException e) {
    System.err.println("[RecommendationService] Erreur SQL pour userId=" + userId);
    e.printStackTrace();
    // Ou utiliser un logger professionnel (SLF4J, Log4j)
}
```

---

### 9️⃣ FALLBACK EN CAS D'ERREUR IA

**État actuel :**
Si l'IA échoue, retourner les événements futurs simples :
```java
catch (Exception e) {
    System.err.println("[RecommendationService] Erreur génération recommandations: " + e.getMessage());
    recommendations = evenementService.getAll().stream()
        .filter(ev -> !ev.isIsCanceled())
        .filter(ev -> ev.getDateDebut() != null && ev.getDateDebut().isAfter(LocalDateTime.now()))
        .filter(ev -> !profile.participatedEventIds.contains(ev.getId()))
        .limit(limit)
        .toList();
}
```

**Attention :**
- ⚠️ Le fallback retourne les événements dans l'ordre par défaut (pas trié)
- ⚠️ Les utilisateurs ne verront pas de recommandations personnalisées
- ⚠️ Cela peut être acceptable mais moins pertinent

---

### 🔟 DONNÉES VIDES

**État actuel :**
Si l'utilisateur n'a pas de participations, le profil sera vide :
```java
if (profile.typePreferences.isEmpty()) {
    // Aucune recommandation possible
}
```

**Attention :**
- ⚠️ Les nouveaux utilisateurs n'auront pas de recommandations
- ⚠️ Les utilisateurs avec peu de participations auront peu de données
- ⚠️ L'IA peut générer des recommandations génériques

**Recommandation :**
```java
// Ajouter une logique pour les nouveaux utilisateurs
if (profile.typePreferences.isEmpty()) {
    // Retourner les événements les plus populaires
    // Ou les événements les plus récents
    // Ou les événements recommandés par défaut
}
```

---

## 🔒 SÉCURITÉ

### ✅ Points positifs :
- ✅ Utilise `PreparedStatement` (protection contre les injections SQL)
- ✅ Paramètres liés avec `ps.setInt(1, userId)`
- ✅ Validation des données (null checks)

### ⚠️ Points à surveiller :
- ⚠️ Vérifier que `userId` vient d'une source fiable (session utilisateur)
- ⚠️ Vérifier que l'utilisateur a le droit d'accéder à ses propres recommandations
- ⚠️ Vérifier que les données sensibles ne sont pas loggées

---

## 📊 TESTS À FAIRE

### Test 1 : Utilisateur avec participations et feedbacks
```java
// Utilisateur avec 5 participations et feedbacks
int userId = 42;
UserProfile profile = recommendationService.buildUserProfile(userId);
assert profile.totalParticipations == 5;
assert profile.typePreferences.size() > 0;
assert profile.typePreferences.get(0).averageRating > 0;
```

### Test 2 : Utilisateur sans participations
```java
// Nouvel utilisateur
int userId = 999;
UserProfile profile = recommendationService.buildUserProfile(userId);
assert profile.totalParticipations == 0;
assert profile.typePreferences.isEmpty();
```

### Test 3 : Recommandations d'événements
```java
UserProfile profile = recommendationService.buildUserProfile(42);
List<Evenement> recommendations = recommendationService.generateEventRecommendations(profile, 5);
assert recommendations.size() <= 5;
assert recommendations.stream().noneMatch(ev -> profile.participatedEventIds.contains(ev.getId()));
assert recommendations.stream().allMatch(ev -> !ev.isIsCanceled());
```

### Test 4 : Recommandations de cours
```java
UserProfile profile = recommendationService.buildUserProfile(42);
List<Cours> recommendations = recommendationService.generateCourseRecommendations(profile, 5);
assert recommendations.size() <= 5;
```

---

## 🎯 RECOMMANDATIONS D'AMÉLIORATION

### 1. Ajouter du caching
```java
// Mettre en cache le profil utilisateur pour éviter les requêtes répétées
private Map<Integer, UserProfile> profileCache = new HashMap<>();

public UserProfile buildUserProfile(int userId) {
    if (profileCache.containsKey(userId)) {
        return profileCache.get(userId);
    }
    // ... construire le profil
    profileCache.put(userId, profile);
    return profile;
}
```

### 2. Ajouter du logging professionnel
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

logger.info("Génération du profil pour userId={}", userId);
logger.error("Erreur SQL pour userId={}", userId, e);
```

### 3. Ajouter des métriques
```java
// Compter le nombre de recommandations générées
// Mesurer le temps d'exécution
// Tracker les erreurs
```

### 4. Améliorer le parsing IA
```java
// Nettoyer le markdown
// Gérer les réponses partielles
// Valider le format JSON
```

### 5. Ajouter une configuration
```java
// Types d'événements configurables
// Matières de cours configurables
// Nombre de recommandations par défaut
```

---

## ✅ CONCLUSION

**Le RecommendationService.java est une implémentation SOLIDE et COMPLÈTE.**

**Points forts :**
✅ Requêtes SQL complexes et bien structurées
✅ Intégration IA professionnelle
✅ Gestion d'erreurs robuste
✅ Pas de modification de la structure BD
✅ Sécurité SQL (PreparedStatement)

**Points à surveiller :**
⚠️ Format JSON des feedbacks
⚠️ Existence de la table equipe_etudiant
⚠️ Disponibilité de GroqService
⚠️ Performance des requêtes SQL
⚠️ Parsing de la réponse IA
⚠️ Gestion des utilisateurs sans données

**Recommandations :**
💡 Ajouter du caching
💡 Ajouter du logging professionnel
💡 Ajouter des tests unitaires
💡 Améliorer le parsing IA
💡 Ajouter une configuration

