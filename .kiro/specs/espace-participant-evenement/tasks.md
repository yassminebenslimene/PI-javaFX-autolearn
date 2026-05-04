# Plan d'Implémentation : Espace Participant Événement

## Vue d'ensemble

Implémentation de l'espace participant événement en JavaFX pur (zéro FXML, zéro nouvelle dépendance Maven, zéro modification DB).
Tous les nouveaux fichiers sont dans `tn.esprit.controllers.evenement.front` ou `tn.esprit.services`.
Le seul fichier existant modifié est `EvenementFrontController.java` (uniquement `buildEventCard()`).

> ⚠️ **Note AudioClip** : `javafx.scene.media.AudioClip` requiert le module `javafx-media` absent du `pom.xml`.
> Utiliser `javax.sound.sampled` (JDK standard) pour les sons, ou ajouter `javafx-media` au `pom.xml` si autorisé.
> Les sons WAV seront générés programmatiquement via `javax.sound.sampled` (zéro fichier externe, zéro dépendance).

---

## Tâches

- [x] 1. Créer les modèles de données in-memory
  - [x] 1.1 Créer `ItemVending.java` (record Java 17)
    - Champs : `String nom`, `String emoji`, `double prixTND`, `int calories`, `int sucreG`
    - Package : `tn.esprit.controllers.evenement.front`
    - _Requirements: 2.2_

  - [x] 1.2 Créer `ItemMateriel.java` (classe mutable)
    - Champs publics : `String nom`, `String emoji`, `boolean disponible`, `String emprunteurNom`, `int dureeHeures`
    - Package : `tn.esprit.controllers.evenement.front`
    - _Requirements: 4.2, 4.3_

  - [x] 1.3 Créer `MenuItem.java` (record Java 17)
    - Champs : `String nom`, `String emoji`, `String description`, `String categorie` (`"dejeuner"` | `"cafe"`)
    - Package : `tn.esprit.controllers.evenement.front`
    - _Requirements: 3.3_

  - [ ]* 1.4 Écrire les tests de propriété pour les modèles de données
    - **Property 4 : Complétude et séparation des sections du Menu Déjeuner**
    - **Validates: Requirements 3.2, 3.3**
    - **Property 5 : Complétude de la liste de matériel empruntable**
    - **Validates: Requirements 4.2**

- [x] 2. Créer les services API indépendants
  - [x] 2.1 Créer `OpenFoodFactsService.java`
    - Package : `tn.esprit.services`
    - Méthode `getNutrition(String productName)` → `NutritionInfo(int calories, int sucreG)`
    - Utiliser `org.apache.hc.client5` (déjà dans pom.xml) pour GET `https://world.openfoodfacts.org/cgi/search.pl?search_terms=%s&json=1&page_size=1`
    - Parser `nutriments.energy-kcal_100g` et `sugars_100g` via Gson (déjà dans pom.xml)
    - Timeout 3s — tout échec retourne `NutritionInfo.empty()` silencieusement (try/catch sans propagation)
    - Inclure le record interne `NutritionInfo` avec méthode statique `empty()`
    - _Requirements: 2.2 (enrichissement nutritionnel)_

  - [x] 2.2 Créer `ExchangeRateService.java`
    - Package : `tn.esprit.services`
    - Méthode `getRates()` → `Map<String, Double>` (ex: `{"EUR": 0.29, "USD": 0.32}`)
    - GET `https://open.er-api.com/v6/latest/TND` via `org.apache.hc.client5`
    - Cache in-memory 1h via `System.currentTimeMillis()` (champs statiques `cachedRates` + `cacheTimestamp`)
    - Fallback : `Map.of()` si erreur ou timeout
    - _Requirements: 2.2 (affichage prix multi-devises)_

  - [ ]* 2.3 Écrire les tests de propriété pour les services API
    - **Property 3 : Gestion silencieuse des ressources audio manquantes** (applicable au pattern try/catch des services)
    - **Validates: Requirements 2.7, 5.7**

- [x] 3. Implémenter `MenuDejeunerController.java`
  - [x] 3.1 Créer `MenuDejeunerController.java`
    - Package : `tn.esprit.controllers.evenement.front`
    - Méthode statique `show(Window owner)` — ouvre un ModalOverlay (Stage transparent + StackPane + VBox, pattern identique à `showDetailsModal()`)
    - Méthodes statiques `getDejeunerItems()` et `getCafeItems()` retournant des listes de `MenuItem` hardcodées
    - Déjeuner (6 items) : 🥗 Salade César, 🍕 Pizza Margherita, 🥙 Wrap Poulet, 🍝 Pasta Carbonara, 🥪 Sandwich Club, 🍱 Bento Végétarien
    - Pause Café (5 items) : ☕ Café Espresso, 🍵 Thé Menthe, 🧁 Muffin Chocolat, 🥐 Croissant Beurre, 🍊 Jus d'Orange
    - Deux sections avec en-têtes gradient `#667eea→#764ba2`
    - Animation séquentielle par item : `FadeTransition(300ms) + TranslateTransition(300ms, fromY=20)`, délai `i * 60ms`
    - `ScrollPane` vertical si contenu dépasse la hauteur visible
    - Fermeture : bouton ✕, clic overlay, touche Escape — animation fondu sortie
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 5.1_

  - [ ]* 3.2 Écrire les tests de propriété pour MenuDejeunerController
    - **Property 4 : Complétude et séparation des sections du Menu Déjeuner**
    - **Validates: Requirements 3.2, 3.3**

- [x] 4. Implémenter `VendingMachineController.java`
  - [x] 4.1 Générer les fichiers sons WAV programmatiquement
    - Créer une méthode utilitaire `SoundGenerator` (ou méthode privée statique dans `VendingMachineController`) qui génère un fichier WAV via `javax.sound.sampled` (AudioFormat 44100Hz, 16bit, mono)
    - Générer 3 tons synthétiques : `son_selection.wav` (bip court 300ms, 880Hz), `son_revelation.wav` (mélodie 800ms), `son_confirmation.wav` (bip 500ms, 660Hz)
    - Écrire les fichiers dans `src/main/resources/sounds/` au premier lancement si absents (via `getClass().getResourceAsStream` check + `Files.write`)
    - Alternative : générer le son directement en mémoire via `SourceDataLine` sans fichier externe
    - _Requirements: 2.4, 2.7, 5.7_

  - [x] 4.2 Créer `VendingMachineController.java`
    - Package : `tn.esprit.controllers.evenement.front`
    - Méthode statique `show(Evenement ev, Window owner)` — ModalOverlay secondaire
    - Méthode statique `loadItems(String eventType)` → `List<ItemVending>` :
      - Appel `GroqService.ask()` dans un `Task<>` background avec le prompt exact du design
      - Parse JSON Gson → `List<ItemVending>` (champs `nom`, `emoji`, `prixTND`)
      - Enrichissement silencieux via `OpenFoodFactsService.getNutrition()` pour chaque item
      - Conversion via `ExchangeRateService.getRates()` (affichage EUR/USD si disponible)
      - Fallback `fallbackItems()` : liste hardcodée 8 items si Groq échoue ou retourne < 8 items
    - Méthode statique `fallbackItems()` → liste hardcodée 8 items (boissons/snacks variés avec emoji, nom, prixTND > 0)
    - Affichage : spinner "Chargement du menu..." pendant le `Task<>`, puis grille 4×2 de badges starburst
    - Badge starburst : `Polygon` JavaFX 16 points alternant rayon interne/externe, fond `RadialGradient` `#ff6b9d→#c44dff`
    - Clic item → son via `javax.sound.sampled` (ou `AudioClip` si javafx-media ajouté) + animation révélation (FadeTransition + ScaleTransition)
    - Révélation : affiche emoji (grand) + nom + prix + calories/sucre si disponibles
    - Bouton "🔄 Rejouer" → retour grille items
    - Fermeture : bouton ✕, clic overlay
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 5.1_

  - [ ]* 4.3 Écrire les tests de propriété pour VendingMachineController
    - **Property 2 : Complétude des items de la Vending Machine**
    - **Validates: Requirements 2.2, 2.3**
    - **Property 3 : Gestion silencieuse des ressources audio manquantes**
    - **Validates: Requirements 2.7, 5.7**

- [x] 5. Checkpoint — Vérifier les composants indépendants
  - S'assurer que les 3 modèles compilent sans erreur, que `OpenFoodFactsService` et `ExchangeRateService` compilent, et que `MenuDejeunerController` et `VendingMachineController` compilent. Demander à l'utilisateur si des questions se posent.

- [x] 6. Implémenter `EmpruntMaterielController.java`
  - [x] 6.1 Créer `EmpruntMaterielController.java`
    - Package : `tn.esprit.controllers.evenement.front`
    - Méthode statique `show(Evenement ev, Window owner)` — ModalOverlay secondaire
    - Méthode statique `initItems()` → `Map<String, ItemMateriel>` avec 12 items tous `disponible=true` à l'initialisation :
      🔌 Chargeur laptop, 🔌 Multiprise, 📽️ Vidéoprojecteur, 🔗 Câble HDMI, 🔌 Adaptateur USB-C, 🖊️ Marqueurs (set), 📝 Post-its (bloc), 📡 Extension WiFi, 🎧 Casque audio, 📷 Webcam HD, 🖱️ Pointeur laser, 🔌 Rallonge électrique
    - Affichage liste : chaque item dans une `HBox` avec emoji, nom, badge disponibilité (`"✅ Disponible"` fond `#d1fae5` / `"🔴 Occupé"` fond `#fee2e2`)
    - Clic item disponible → `buildEmpruntForm()` inline dans le même ModalOverlay (pas de nouveau Stage)
    - Formulaire : champ nom pré-rempli `SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom()`, sélecteur durée (Label + boutons `−`/`+`, range 1–8h, pas 1h)
    - Méthode statique `confirmEmprunt(ItemMateriel item, String userName, int duree)` : si `userName == null` → message erreur sans modifier l'état ; sinon `item.disponible = false`, `item.emprunteurNom = userName`, `item.dureeHeures = duree`
    - Après confirmation : banner vert, génération QR code via ZXing (`com.google.zxing` déjà dans pom.xml), affichage `ImageView` du QR, bouton "📄 Télécharger PDF"
    - Contenu QR : `"EMPRUNT MATERIEL - AutoLearn\nItem: {nom}\nEmprunteur: {prenom} {nom}\nDurée: {duree}h\nHeure: {HH:mm dd/MM/yyyy}\nÉvénement: {titre}"`
    - Génération PDF via iText 5 (`com.itextpdf` déjà dans pom.xml) : header rectangle coloré `#667eea`, titre "AutoLearn — Reçu d'Emprunt", tableau Item/Emprunteur/Durée/Heure/Événement, QR code intégré, footer
    - Clic item occupé → message `"Cet item est actuellement utilisé."` (pas de formulaire)
    - Son confirmation via `javax.sound.sampled` après confirmation réussie
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 5.1_

  - [ ]* 6.2 Écrire les tests de propriété pour EmpruntMaterielController
    - **Property 5 : Complétude de la liste de matériel empruntable**
    - **Validates: Requirements 4.2**
    - **Property 6 : Cohérence badge disponibilité / état in-memory**
    - **Validates: Requirements 4.3**
    - **Property 7 : Pré-remplissage du formulaire d'emprunt**
    - **Validates: Requirements 4.5**
    - **Property 8 : Invariant de la durée d'emprunt**
    - **Validates: Requirements 4.6**
    - **Property 9 : Round-trip emprunt → état Occupé**
    - **Validates: Requirements 4.7, 4.9**
    - **Property 10 : Isolation de l'état en cas d'utilisateur null**
    - **Validates: Requirements 4.10**

- [x] 7. Implémenter `EspaceParticipantController.java`
  - [x] 7.1 Créer `EspaceParticipantController.java`
    - Package : `tn.esprit.controllers.evenement.front`
    - Méthode statique `show(Evenement ev, Window owner)` — point d'entrée unique
    - Méthode statique `loadSound(String name)` → retourne `null` sans exception si fichier introuvable (try/catch silencieux)
    - Créer le ModalOverlay principal : Stage transparent + StackPane overlay + VBox modal (pattern identique à `showDetailsModal()`)
    - Header gradient `linear-gradient(to right, #667eea, #764ba2)`, titre `"🎯 " + ev.getTitre()`, bouton ✕
    - Body : 3 cards hub via `buildFeatureCard(emoji, titre, desc, color, onClick)` :
      - Card 1 : `"🎰"`, `"Vending Machine"`, `"Boissons & snacks gamifiés"` → `VendingMachineController.show(ev, owner)`
      - Card 2 : `"🍽️"`, `"Menu & Pause Café"`, `"Déjeuner et snacks de pause"` → `MenuDejeunerController.show(owner)`
      - Card 3 : `"🔌"`, `"Emprunt Matériel"`, `"Équipements disponibles"` → `EmpruntMaterielController.show(ev, owner)`
    - Animation entrée : `FadeTransition(220ms, 0→1)` + `TranslateTransition(260ms, fromY=45→0, EASE_OUT)` en parallèle
    - Fermeture : bouton ✕, clic overlay, touche Escape → animation fondu sortie `FadeTransition(180ms, 1→0)`
    - Palette `#667eea`, `#764ba2`, fond `#f5f3ff`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 5.1, 5.2, 5.3, 5.7_

  - [ ]* 7.2 Écrire les tests de propriété pour EspaceParticipantController
    - **Property 1 : Visibilité conditionnelle du bouton Espace Participant**
    - **Validates: Requirements 1.1, 1.7, 5.6**
    - **Property 11 : Titre de l'événement dans le header du modal**
    - **Validates: Requirements 1.3**

- [x] 8. Checkpoint — Vérifier l'assemblage des contrôleurs
  - S'assurer que `EspaceParticipantController` compile et que les 3 sous-contrôleurs sont correctement appelés. Demander à l'utilisateur si des questions se posent.

- [x] 9. Modifier `EvenementFrontController.java` — ajout bouton conditionnel
  - [x] 9.1 Ajouter le bouton `"🎯 Espace Participant"` dans `buildEventCard()` uniquement
    - Localiser la fin de `buildEventCard()`, après la création de `expandBtn` et avant `card.getChildren().addAll(...)`
    - Ajouter le bloc conditionnel :
      ```java
      String status = ev.computeStatus();
      if ("En cours".equals(status) && tn.esprit.session.SessionManager.getCurrentUser() != null) {
          Button espaceBtn = new Button("🎯 Espace Participant");
          espaceBtn.setMaxWidth(Double.MAX_VALUE);
          espaceBtn.setStyle(
              "-fx-background-color:linear-gradient(to right,#667eea,#764ba2);"
              + "-fx-text-fill:white; -fx-font-size:12; -fx-font-weight:700;"
              + "-fx-padding:10 16 10 16; -fx-background-radius:0 0 18 18;"
              + "-fx-cursor:hand; -fx-border-width:0; -fx-max-width:Infinity;");
          espaceBtn.setOnAction(e ->
              EspaceParticipantController.show(ev, card.getScene().getWindow()));
          card.getChildren().add(espaceBtn);
      }
      ```
    - Ne modifier aucune autre méthode de `EvenementFrontController`
    - Ne pas toucher : LoginController, MainApp, user, quiz, cours, communauté
    - _Requirements: 1.1, 1.7, 5.5, 5.6_

  - [ ]* 9.2 Écrire les tests de propriété pour la visibilité du bouton
    - **Property 1 : Visibilité conditionnelle du bouton Espace Participant**
    - **Validates: Requirements 1.1, 1.7, 5.6**

- [x] 10. Checkpoint final — Vérifier la non-régression
  - S'assurer que tous les fichiers compilent sans erreur, que le bouton n'apparaît que pour les événements "En cours" avec utilisateur connecté, et que les autres fonctionnalités du module événement sont intactes. Demander à l'utilisateur si des questions se posent.

---

## Notes

- Tâches marquées `*` sont optionnelles (tests PBT) — peuvent être sautées pour un MVP rapide
- Chaque tâche référence les exigences spécifiques pour la traçabilité
- **Contrainte absolue** : module événement uniquement — ne jamais toucher user, quiz, cours, communauté, LoginController, MainApp (sauf navigation strictement nécessaire)
- **Zéro modification DB** — toutes les données sont in-memory
- **Zéro nouvelle dépendance Maven** — tout est déjà dans pom.xml (sauf `javafx-media` pour AudioClip : utiliser `javax.sound.sampled` à la place)
- Sons générés programmatiquement via `javax.sound.sampled` (JDK standard, zéro dépendance externe)
- Les tests PBT référencent chacun leur propriété de design via le commentaire `// Feature: espace-participant-evenement, Property N`
