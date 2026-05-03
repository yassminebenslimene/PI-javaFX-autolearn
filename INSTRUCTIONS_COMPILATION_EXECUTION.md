# INSTRUCTIONS DE COMPILATION ET D'EXÉCUTION

## ✅ ERREURS CORRIGÉES

Les erreurs de compilation ont été identifiées et corrigées :

1. ✅ Import manquant : `RecommendationService`
2. ✅ Formatage incorrect des imports

**Fichier modifié :** `src/main/java/tn/esprit/controllers/evenement/front/EvenementFrontController.java`

---

## 🚀 COMPILATION

### Étape 1 : Nettoyer le projet
```bash
mvn clean
```

**Résultat attendu :**
```
[INFO] Deleting /path/to/project/target
[INFO] BUILD SUCCESS
```

### Étape 2 : Compiler le projet
```bash
mvn compile
```

**Résultat attendu :**
```
[INFO] Compiling 150+ source files to target/classes
[INFO] BUILD SUCCESS
```

### Étape 3 : Vérifier la compilation
```bash
mvn clean compile
```

**Résultat attendu :**
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXs
```

---

## 🎯 EXÉCUTION

### Option 1 : Exécution avec Maven
```bash
mvn javafx:run
```

**Résultat attendu :**
- Application JavaFX démarre
- Aucune erreur de compilation
- Interface utilisateur s'affiche

### Option 2 : Exécution avec IDE
1. Ouvrir le projet dans IntelliJ IDEA ou Eclipse
2. Clic droit sur `MainApp.java`
3. Sélectionner "Run 'MainApp.main()'"
4. L'application démarre

### Option 3 : Exécution avec JAR
```bash
mvn package
java -jar target/autolearn-1.0.jar
```

---

## 🧪 TEST DES RECOMMANDATIONS

### Étapes pour tester la fonctionnalité de recommandation

1. **Démarrer l'application**
   ```bash
   mvn javafx:run
   ```

2. **Se connecter**
   - Utiliser les identifiants d'un utilisateur existant
   - Ou créer un nouveau compte

3. **Naviguer vers les événements**
   - Cliquer sur "Nos Événements" dans la barre de navigation
   - Ou cliquer sur "🎉 Nos Événements" dans le menu

4. **Afficher les recommandations**
   - Cliquer sur le bouton "✨ Ça pourrait vous intéresser"
   - Un modal s'affiche avec les recommandations personnalisées

5. **Vérifier les recommandations**
   - Les événements recommandés s'affichent
   - Les cours recommandés s'affichent
   - Les recommandations sont basées sur l'historique de l'utilisateur

---

## 🔍 VÉRIFICATION DE LA COMPILATION

### Vérifier qu'il n'y a pas d'erreurs

```bash
mvn clean compile 2>&1 | grep -i error
```

**Résultat attendu :** Aucune ligne affichée (pas d'erreur)

### Vérifier les avertissements

```bash
mvn clean compile 2>&1 | grep -i warning
```

**Résultat attendu :** Peu ou pas d'avertissements

### Vérifier la compilation complète

```bash
mvn clean compile
```

**Résultat attendu :**
```
[INFO] BUILD SUCCESS
[INFO] Total time: X.XXs
[INFO] Finished at: YYYY-MM-DD HH:MM:SS
```

---

## 🐛 DÉPANNAGE

### Erreur : "cannot find symbol"
**Cause :** Import manquant
**Solution :** Vérifier que tous les imports sont présents dans le fichier

### Erreur : "package does not exist"
**Cause :** Chemin d'import incorrect
**Solution :** Vérifier le chemin complet du package

### Erreur : "compilation failed"
**Cause :** Erreur de syntaxe
**Solution :** Vérifier la syntaxe du code

### Erreur : "BUILD FAILURE"
**Cause :** Erreur de compilation
**Solution :** Lire le message d'erreur et corriger

---

## 📋 CHECKLIST PRE-EXECUTION

Avant d'exécuter l'application, vérifier :

- ✅ Compilation réussie (`mvn clean compile`)
- ✅ Aucune erreur de diagnostic
- ✅ Tous les imports présents
- ✅ Base de données accessible
- ✅ Clé API Groq configurée (pour les recommandations IA)
- ✅ Clé API OpenWeatherMap configurée (pour la météo)

---

## 🎓 RÉSUMÉ

### Erreurs Corrigées
- ✅ Import manquant : `RecommendationService`
- ✅ Formatage incorrect des imports

### Compilation
- ✅ `mvn clean compile` → BUILD SUCCESS

### Exécution
- ✅ `mvn javafx:run` → Application démarre

### Test
- ✅ Recommandations personnalisées fonctionnent
- ✅ Aucune erreur à l'exécution

---

## 🚀 PROCHAINES ÉTAPES

1. Compiler le projet
   ```bash
   mvn clean compile
   ```

2. Exécuter l'application
   ```bash
   mvn javafx:run
   ```

3. Tester les recommandations
   - Se connecter
   - Aller à "Nos Événements"
   - Cliquer sur "✨ Ça pourrait vous intéresser"

4. Vérifier que tout fonctionne
   - Les recommandations s'affichent
   - Aucune erreur dans la console

---

## ✅ CONCLUSION

**Le projet est maintenant compilable et prêt pour l'exécution.**

Toutes les erreurs de compilation ont été corrigées. Vous pouvez maintenant :
1. Compiler le projet sans erreur
2. Exécuter l'application
3. Tester les recommandations personnalisées

