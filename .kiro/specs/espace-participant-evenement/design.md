# Design Technique — Espace Participant Événement

## Vue d'ensemble

L'**Espace Participant Événement** est une fonctionnalité modale du module événement d'AutoLearn (JavaFX 17, pattern MVC programmatique). Elle s'active uniquement quand `computeStatus()` retourne `"En cours"` sur une card d'événement. L'ensemble est implémenté sans FXML, sans nouvelle table DB, en réutilisant exactement le pattern `showDetailsModal()` de `EvenementFrontController`.

### Périmètre strict
- **Module** : `tn.esprit.controllers.evenement.front` + `tn.esprit.services`
- **Seul fichier existant modifié** : `EvenementFrontController.java` (ajout d'un bouton conditionnel dans `buildEventCard()`)
- **Zéro nouvelle dépendance Maven** : Apache HttpClient5, Gson, iText 5, ZXing déjà présents
- **Zéro modification DB** : toutes les données sont in-memory

### APIs intégrées
| API | Usage | Auth | Fallback |
|-----|-------|------|---------|
| Groq AI (`GroqService`) | Génération dynamique items vending | Clé existante | Liste hardcodée 8 items |
| OpenWeatherMap (`WeatherService`) | Météo + conseil vestimentaire | Clé existante | Bloc masqué |
| Open Food Facts | Infos nutritionnelles items vending | Aucune | Calories/sucre = 0 |
| Exchange Rate API | Conversion TND→EUR/USD | Aucune | Affichage TND uniquement |
| ZXing | QR code confirmation emprunt | Lib locale | N/A |
| iText 5 | Reçu PDF emprunt | Lib locale | N/A |

---

## Architecture

### Diagramme de composants

```
EvenementFrontController
  └── buildEventCard()
        └── [si computeStatus()=="En cours" && user != null]
              └── Button "🎯 Espace Participant"
                    └── EspaceParticipantController.show(ev, ownerWindow)
                          │
                          ├── ModalOverlay (Stage transparent + StackPane + VBox)
                          │     Header: gradient #667eea→#764ba2
                          │     Body: 3 cards hub
                          │
                          ├── [Card 1] VendingMachineController.show(ev, ownerWindow)
                          │     ├── GroqService.ask() → items JSON
                          │     ├── OpenFoodFactsService.getNutrition(nom)
                          │     ├── ExchangeRateService.getRates()
                          │     └── AudioClip (son_selection.wav, son_revelation.wav)
                          │
                          ├── [Card 2] MenuDejeunerController.show(ownerWindow)
                          │     └── Données hardcodées in-memory
                          │
                          └── [Card 3] EmpruntMaterielController.show(ev, ownerWindow)
                                ├── Map<String, ItemMateriel> (in-memory)
                                ├── ZXing → QR code BufferedImage
                                ├── iText 5 → PDF reçu
                                └── AudioClip (son_confirmation.wav)
```

### Pattern Modal (identique à showDetailsModal)

```java
Stage dialog = new Stage();
dialog.initModality(Modality.APPLICATION_MODAL);
dialog.initStyle(StageStyle.TRANSPARENT);
dialog.initOwner(ownerWindow);

StackPane root = new StackPane(modal);
root.setStyle("-fx-background-color:rgba(0,0,0,0.62);");

Scene scene = new Scene(root, ownerWindow.getWidth(), ownerWindow.getHeight());
scene.setFill(Color.TRANSPARENT);
dialog.setScene(scene);
dialog.setX(ownerWindow.getX());
dialog.setY(ownerWindow.getY());

// Animation entrée
root.setOpacity(0);
modal.setTranslateY(45);
dialog.show();
new ParallelTransition(
    new FadeTransition(Duration.millis(220), root),   // 0→1
    new TranslateTransition(Duration.millis(260), modal) // 45→0, EASE_OUT
).play();
```

---

## Composants et Interfaces

### 1. `EspaceParticipantController`
**Package** : `tn.esprit.controllers.evenement.front`

Point d'entrée unique. Méthode statique `show(Evenement ev, Window owner)`.

```java
public class EspaceParticipantController {
    public static void show(Evenement ev, Window owner) { ... }
    private static VBox buildHubModal(Evenement ev, Stage dialog) { ... }
    private static VBox buildFeatureCard(String emoji, String titre, String desc,
                                          String color, Runnable onClick) { ... }
}
```

**Responsabilités** :
- Créer le Stage transparent + StackPane overlay
- Afficher le header gradient avec titre de l'événement + météo résumée
- Présenter 3 cards cliquables (VendingMachine, MenuDejeuner, EmpruntMateriel)
- Gérer fermeture (bouton ✕, clic overlay, touche Escape)

**Header** : gradient `linear-gradient(to right, #667eea, #764ba2)`, titre événement, météo inline (appel `WeatherService` en `Task<>` background)

---

### 2. `VendingMachineController`
**Package** : `tn.esprit.controllers.evenement.front`

```java
public class VendingMachineController {
    public static void show(Evenement ev, Window owner) { ... }
    private static List<ItemVending> loadItems(String eventType) { ... }
    private static void showReveal(ItemVending item, VBox container, Stage dialog) { ... }
    private static String buildGroqPrompt(String eventType) { ... }
    private static List<ItemVending> parseGroqResponse(String json) { ... }
    private static List<ItemVending> fallbackItems() { ... }
}
```

**Flux** :
1. Ouverture → spinner "Chargement du menu..."
2. `Task<List<ItemVending>>` en background :
   - Appel `GroqService.ask()` avec prompt structuré
   - Parse JSON → `List<ItemVending>`
   - Pour chaque item : appel `OpenFoodFactsService.getNutrition()` (enrichissement silencieux)
   - Appel `ExchangeRateService.getRates()` pour conversion
3. Affichage grille 4×2 de badges starburst
4. Clic item → `AudioClip(son_selection.wav)` + animation révélation
5. Bouton "🔄 Rejouer" → retour grille

**Prompt Groq exact** :
```
System: "Tu es un assistant qui génère des listes de produits pour distributeurs automatiques d'événements. Réponds UNIQUEMENT en JSON valide, sans markdown, sans explication."

User: "Génère exactement 8 boissons/snacks pour un événement de type {type}. 
Retourne un tableau JSON avec exactement ce format:
[{\"nom\":\"Café Espresso\",\"emoji\":\"☕\",\"prixTND\":1.5},{\"nom\":\"Eau minérale\",\"emoji\":\"💧\",\"prixTND\":0.8}]
Chaque item doit avoir: nom (string), emoji (string), prixTND (number).
Type d'événement: {type}"
```

**Starburst badge** : `Polygon` JavaFX avec 16 points alternant rayon interne/externe, fond `RadialGradient` `#ff6b9d → #c44dff`.

---

### 3. `MenuDejeunerController`
**Package** : `tn.esprit.controllers.evenement.front`

```java
public class MenuDejeunerController {
    public static void show(Window owner) { ... }
    private static List<MenuItem> getDejeunerItems() { ... }
    private static List<MenuItem> getCafeItems() { ... }
    private static void animateItems(List<Node> nodes) { ... }
}
```

**Données hardcodées** (in-memory, initialisées à chaque ouverture) :

*Déjeuner (6 items)* :
- 🥗 Salade César — Salade fraîche, croûtons, parmesan
- 🍕 Pizza Margherita — Tomate, mozzarella, basilic
- 🥙 Wrap Poulet — Poulet grillé, légumes, sauce
- 🍝 Pasta Carbonara — Pâtes, lardons, crème
- 🥪 Sandwich Club — Jambon, fromage, tomate
- 🍱 Bento Végétarien — Riz, légumes sautés, tofu

*Pause Café (5 items)* :
- ☕ Café Espresso — Arabica sélectionné
- 🍵 Thé Menthe — Menthe fraîche, sucre
- 🧁 Muffin Chocolat — Fondant, pépites choco
- 🥐 Croissant Beurre — Feuilleté, doré au four
- 🍊 Jus d'Orange — Pressé frais

**Animation** : `FadeTransition(300ms) + TranslateTransition(300ms, fromY=20)` avec délai `i * 60ms` par item.

---

### 4. `EmpruntMaterielController`
**Package** : `tn.esprit.controllers.evenement.front`

```java
public class EmpruntMaterielController {
    public static void show(Evenement ev, Window owner) { ... }
    private static Map<String, ItemMateriel> initItems() { ... }
    private static VBox buildItemRow(ItemMateriel item, VBox listContainer,
                                      Label bannerLabel, Stage dialog) { ... }
    private static VBox buildEmpruntForm(ItemMateriel item, VBox listContainer,
                                          Label bannerLabel) { ... }
    private static javafx.scene.image.Image generateQRCode(String content) { ... }
    private static void generatePDF(ItemMateriel item, String userName,
                                     int duree, String outputPath) { ... }
}
```

**12 items matériel** (Map initialisée à chaque `show()`) :
```
🔌 Chargeur laptop    ✅  |  🔌 Multiprise          ✅
📽️ Vidéoprojecteur   ✅  |  🔗 Câble HDMI           ✅
🔌 Adaptateur USB-C  ✅  |  🖊️ Marqueurs (set)      ✅
📝 Post-its (bloc)   ✅  |  📡 Extension WiFi       ✅
🎧 Casque audio      ✅  |  📷 Webcam HD            ✅
🖱️ Pointeur laser    ✅  |  🔌 Rallonge électrique  ✅
```

**Flux emprunt** :
1. Clic item disponible → formulaire inline (même VBox, pas de nouveau Stage)
2. Champ nom pré-rempli depuis `SessionManager.getCurrentUser()`
3. Sélecteur durée : `Label` + boutons `−`/`+`, range 1–8h
4. Confirmation → `Map` mis à jour → banner vert → QR code affiché → bouton PDF

**Contenu QR code** :
```
EMPRUNT MATERIEL - AutoLearn
Item: {nom}
Emprunteur: {prenom} {nom}
Durée: {duree}h
Heure: {HH:mm dd/MM/yyyy}
Événement: {titre}
```

**Contenu PDF (iText 5)** :
- Header gradient simulé (rectangle coloré `#667eea`)
- Logo texte "AutoLearn — Reçu d'Emprunt"
- Tableau : Item | Emprunteur | Durée | Heure | Événement
- QR code intégré (via `Image` iText depuis `BufferedImage` ZXing)
- Footer : "Document généré automatiquement"

---

### 5. `OpenFoodFactsService`
**Package** : `tn.esprit.services`

```java
public class OpenFoodFactsService {
    private static final String BASE_URL =
        "https://world.openfoodfacts.org/cgi/search.pl?search_terms=%s&json=1&page_size=1";

    /** Retourne {calories, sucreG} ou {0, 0} si indisponible */
    public NutritionInfo getNutrition(String productName) { ... }

    public record NutritionInfo(int calories, int sucreG) {
        public static NutritionInfo empty() { return new NutritionInfo(0, 0); }
    }
}
```

**Implémentation** : `HttpClient` (java.net.http ou Apache HttpClient5), GET, parse JSON Gson, champ `nutriments.energy-kcal_100g` et `sugars_100g`. Timeout 3s. Tout échec → `NutritionInfo.empty()` silencieux.

---

### 6. `ExchangeRateService`
**Package** : `tn.esprit.services`

```java
public class ExchangeRateService {
    private static final String URL = "https://open.er-api.com/v6/latest/TND";

    /** Retourne Map{"EUR": 0.29, "USD": 0.32} ou Map vide si indisponible */
    public Map<String, Double> getRates() { ... }

    /** Cache en mémoire valide 1h */
    private static Map<String, Double> cachedRates = null;
    private static long cacheTimestamp = 0;
}
```

**Cache** : résultat mis en cache 1h (timestamp `System.currentTimeMillis()`). Fallback : `Map.of()` → affichage TND uniquement dans la VendingMachine.

---

## Modèles de Données

### Classes in-memory (aucune persistance DB)

```java
// tn.esprit.controllers.evenement.front ou package dédié
package tn.esprit.controllers.evenement.front;

// ItemVending — record Java 17
public record ItemVending(
    String nom,
    String emoji,
    double prixTND,
    int calories,    // enrichi par OpenFoodFacts, 0 si indisponible
    int sucreG       // enrichi par OpenFoodFacts, 0 si indisponible
) {}

// ItemMateriel — classe mutable (disponibilité change)
public class ItemMateriel {
    public String nom;
    public String emoji;
    public boolean disponible;
    public String emprunteurNom;  // null si disponible
    public int dureeHeures;       // 0 si disponible
}

// MenuItem — record Java 17
public record MenuItem(
    String nom,
    String emoji,
    String description,
    String categorie  // "dejeuner" | "cafe"
) {}
```

### Cycle de vie des données
- `ItemMateriel` : initialisé à chaque appel de `EmpruntMaterielController.show()` (tous disponibles)
- `ItemVending` : chargé une fois par ouverture de `VendingMachineController.show()` (Groq ou fallback)
- `MenuItem` : constantes statiques dans `MenuDejeunerController`
- Aucune donnée ne survit à la fermeture du modal

---

## Ressources Sons

**Chemin** : `src/main/resources/sounds/`

| Fichier | Déclencheur | Durée recommandée |
|---------|-------------|-------------------|
| `son_selection.wav` | Clic sur item vending | ~0.3s |
| `son_revelation.wav` | Animation révélation vending | ~0.8s |
| `son_confirmation.wav` | Confirmation emprunt matériel | ~0.5s |

**Chargement** :
```java
private static AudioClip loadSound(String name) {
    try {
        var url = EspaceParticipantController.class
            .getResource("/sounds/" + name);
        if (url == null) return null;
        return new AudioClip(url.toExternalForm());
    } catch (Exception e) {
        return null; // silencieux
    }
}

// Usage
AudioClip clip = loadSound("son_selection.wav");
if (clip != null) clip.play();
```

---

## Modification de EvenementFrontController

**Seule modification** : dans `buildEventCard()`, après la création du bouton `expandBtn` et avant `card.getChildren().addAll(...)`, ajouter :

```java
// ── Bouton Espace Participant (événement En cours uniquement) ──
String status = ev.computeStatus();
tn.esprit.session.SessionManager sm = tn.esprit.session.SessionManager.getInstance();
// (ou via SessionManager.getCurrentUser() selon l'API existante)
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

Ce bouton remplace visuellement le bas de la card (même `background-radius:0 0 18 18` que `expandBtn`). Il est ajouté **après** `expandBtn` dans la VBox card.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*


### Property 1 : Visibilité conditionnelle du bouton Espace Participant

*Pour tout* événement, le bouton `"🎯 Espace Participant"` doit apparaître dans la card si et seulement si `computeStatus()` retourne `"En cours"` ET `SessionManager.getCurrentUser()` est non-null. Pour tout autre statut (`"Plannifié"`, `"Passé"`, `"Annulé"`) ou si l'utilisateur est null, le bouton ne doit pas apparaître.

**Validates: Requirements 1.1, 1.7, 5.6**

---

### Property 2 : Complétude des items de la Vending Machine

*Pour tout* type d'événement passé à `loadItems(eventType)`, la liste retournée doit contenir au minimum 8 éléments, et chaque `ItemVending` doit avoir un `nom` non-null non-vide, un `emoji` non-null non-vide, et un `prixTND` strictement positif.

**Validates: Requirements 2.2, 2.3**

---

### Property 3 : Gestion silencieuse des ressources audio manquantes

*Pour tout* nom de fichier audio (existant ou non), `loadSound(filename)` doit retourner `null` sans lever d'exception si le fichier est introuvable, et retourner un `AudioClip` valide si le fichier existe.

**Validates: Requirements 2.7, 5.7**

---

### Property 4 : Complétude et séparation des sections du Menu Déjeuner

*Pour tout* appel à `getDejeunerItems()`, la liste retournée doit contenir au minimum 4 éléments avec `categorie == "dejeuner"`. *Pour tout* appel à `getCafeItems()`, la liste retournée doit contenir au minimum 4 éléments avec `categorie == "cafe"`. Chaque `MenuItem` doit avoir `nom`, `emoji`, et `description` non-null non-vides.

**Validates: Requirements 3.2, 3.3**

---

### Property 5 : Complétude de la liste de matériel empruntable

*Pour tout* appel à `initItems()`, la `Map<String, ItemMateriel>` retournée doit contenir au minimum 12 entrées, et chaque `ItemMateriel` doit avoir `disponible == true` à l'initialisation (état frais à chaque ouverture du modal).

**Validates: Requirements 4.2**

---

### Property 6 : Cohérence badge disponibilité / état in-memory

*Pour tout* `ItemMateriel`, le badge affiché doit correspondre exactement à son champ `disponible` : `true` → badge `"✅ Disponible"` (fond `#d1fae5`), `false` → badge `"🔴 Occupé"` (fond `#fee2e2`). Cette correspondance doit tenir après toute modification de l'état.

**Validates: Requirements 4.3**

---

### Property 7 : Pré-remplissage du formulaire d'emprunt

*Pour tout* utilisateur connecté (non-null), le champ nom du formulaire d'emprunt doit être exactement égal à `user.getPrenom() + " " + user.getNom()`.

**Validates: Requirements 4.5**

---

### Property 8 : Invariant de la durée d'emprunt

*Pour toute* valeur courante `d` dans `[1, 8]`, appuyer sur `+` doit donner `min(d+1, 8)` et appuyer sur `−` doit donner `max(d-1, 1)`. La valeur ne peut jamais sortir de l'intervalle `[1, 8]`.

**Validates: Requirements 4.6**

---

### Property 9 : Round-trip emprunt → état Occupé

*Pour tout* item initialement disponible (`disponible == true`), après confirmation d'un emprunt valide (utilisateur non-null, durée dans [1,8]), l'item doit avoir `disponible == false` dans le `Map` in-memory, et cliquer à nouveau sur cet item ne doit pas ouvrir le formulaire.

**Validates: Requirements 4.7, 4.9**

---

### Property 10 : Isolation de l'état en cas d'utilisateur null

*Pour tout* item disponible, si `SessionManager.getCurrentUser()` retourne `null` au moment de la confirmation, le `Map<String, ItemMateriel>` doit rester inchangé (item toujours `disponible == true`) et aucune exception ne doit se propager à l'UI.

**Validates: Requirements 4.10**

---

### Property 11 : Titre de l'événement dans le header du modal

*Pour tout* événement `ev`, le header du modal `EspaceParticipantController` doit contenir une `Label` dont le texte inclut `ev.getTitre()`.

**Validates: Requirements 1.3**

---

## Gestion des Erreurs

### Stratégie générale : fail-silent avec fallback visible

| Composant | Erreur | Comportement |
|-----------|--------|--------------|
| `GroqService` timeout/erreur | Items vending non générés | Fallback liste hardcodée 8 items |
| `OpenFoodFactsService` erreur | Nutrition non disponible | `calories=0, sucreG=0`, pas d'affichage nutrition |
| `ExchangeRateService` erreur | Taux non disponibles | Affichage TND uniquement, toggle EUR/USD masqué |
| `WeatherService` erreur | Météo indisponible | Bloc météo masqué (même comportement que `showDetailsModal`) |
| `AudioClip` fichier manquant | Son non joué | `null` check silencieux, animation continue |
| `ZXing` erreur génération QR | QR non affiché | Message "QR indisponible" à la place de l'image |
| `iText` erreur PDF | PDF non généré | Toast d'erreur, pas de crash |
| `SessionManager.getCurrentUser()` null | Utilisateur non connecté | Bouton masqué (1.7) ou message erreur (4.10) |

### Pattern de gestion des erreurs API (Task JavaFX)

```java
Task<List<ItemVending>> task = new Task<>() {
    @Override protected List<ItemVending> call() {
        try {
            String response = groqService.ask(SYSTEM_PROMPT, buildGroqPrompt(ev.getType()));
            List<ItemVending> items = parseGroqResponse(response);
            if (items != null && items.size() >= 8) return items;
        } catch (Exception ignored) {}
        return fallbackItems(); // toujours 8 items
    }
};
task.setOnSucceeded(e -> Platform.runLater(() -> renderItems(task.getValue())));
task.setOnFailed(e -> Platform.runLater(() -> renderItems(fallbackItems())));
new Thread(task, "vending-loader").start();
```

---

## Stratégie de Tests

### Approche duale : tests unitaires + tests basés sur les propriétés

Les tests unitaires couvrent les exemples concrets et les cas limites. Les tests basés sur les propriétés (PBT) vérifient les invariants universels sur des entrées générées aléatoirement.

**Bibliothèque PBT recommandée** : `junit-quickcheck` (compatible Java 17 + JUnit 5) ou `jqwik` (natif JUnit 5, recommandé).

```xml
<!-- Ajouter dans pom.xml scope test uniquement -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.1</version>
    <scope>test</scope>
</dependency>
```

### Tests unitaires (exemples et cas limites)

```java
// Exemple 1.2 — ouverture modal (test d'intégration JavaFX)
@Test void espaceParticipantOpensModal() { /* TestFX */ }

// Exemple 1.4 — exactement 3 cards dans le hub
@Test void hubHasExactlyThreeFeatureCards() {
    VBox hub = EspaceParticipantController.buildHubContent(mockEvent);
    long cardCount = hub.getChildren().stream()
        .filter(n -> n instanceof VBox).count();
    assertEquals(3, cardCount);
}

// Exemple 4.1 — ouverture modal emprunt
@Test void empruntMaterielOpensModal() { /* TestFX */ }
```

### Tests basés sur les propriétés (minimum 100 itérations chacun)

```java
// Feature: espace-participant-evenement, Property 1: button visibility
@Property(tries = 100)
void buttonVisibleOnlyWhenEnCoursAndUserNotNull(
    @ForAll("enCoursEvents") Evenement ev,
    @ForAll boolean userConnected) {
    // setup SessionManager mock
    // build card, check button presence == (userConnected)
}

// Feature: espace-participant-evenement, Property 2: vending items completeness
@Property(tries = 200)
void vendingItemsAlwaysCompleteAndValid(
    @ForAll @From("eventTypes") String type) {
    List<ItemVending> items = VendingMachineController.loadItems(type);
    assertThat(items).hasSizeGreaterThanOrEqualTo(8);
    items.forEach(item -> {
        assertThat(item.nom()).isNotBlank();
        assertThat(item.emoji()).isNotBlank();
        assertThat(item.prixTND()).isGreaterThan(0);
    });
}

// Feature: espace-participant-evenement, Property 3: silent audio error
@Property(tries = 100)
void loadSoundNeverThrows(@ForAll String filename) {
    assertDoesNotThrow(() -> {
        AudioClip clip = EspaceParticipantController.loadSound(filename);
        // clip may be null, that's fine
    });
}

// Feature: espace-participant-evenement, Property 4: menu sections completeness
@Property(tries = 50)
void menuSectionsAlwaysComplete() {
    List<MenuItem> dejeuner = MenuDejeunerController.getDejeunerItems();
    List<MenuItem> cafe = MenuDejeunerController.getCafeItems();
    assertThat(dejeuner).hasSizeGreaterThanOrEqualTo(4);
    assertThat(cafe).hasSizeGreaterThanOrEqualTo(4);
    dejeuner.forEach(i -> assertEquals("dejeuner", i.categorie()));
    cafe.forEach(i -> assertEquals("cafe", i.categorie()));
}

// Feature: espace-participant-evenement, Property 5: materiel init state
@Property(tries = 50)
void materielInitAlwaysFresh() {
    Map<String, ItemMateriel> items = EmpruntMaterielController.initItems();
    assertThat(items).hasSizeGreaterThanOrEqualTo(12);
    items.values().forEach(item -> assertTrue(item.disponible));
}

// Feature: espace-participant-evenement, Property 8: duration invariant
@Property(tries = 500)
void durationStaysInBounds(
    @ForAll @IntRange(min = 1, max = 8) int initial,
    @ForAll @IntRange(min = 0, max = 20) int increments,
    @ForAll @IntRange(min = 0, max = 20) int decrements) {
    int d = initial;
    for (int i = 0; i < increments; i++) d = Math.min(d + 1, 8);
    for (int i = 0; i < decrements; i++) d = Math.max(d - 1, 1);
    assertThat(d).isBetween(1, 8);
}

// Feature: espace-participant-evenement, Property 9: borrow round-trip
@Property(tries = 100)
void borrowMakesItemUnavailable(
    @ForAll @From("availableItems") ItemMateriel item,
    @ForAll @AlphaChars @StringLength(min=3, max=30) String userName,
    @ForAll @IntRange(min=1, max=8) int duree) {
    assertTrue(item.disponible);
    EmpruntMaterielController.confirmEmprunt(item, userName, duree);
    assertFalse(item.disponible);
}

// Feature: espace-participant-evenement, Property 10: null user isolation
@Property(tries = 100)
void nullUserDoesNotModifyState(
    @ForAll @From("availableItems") ItemMateriel item) {
    boolean before = item.disponible;
    EmpruntMaterielController.confirmEmprunt(item, null, 2);
    assertEquals(before, item.disponible);
}
```

### Équilibre tests unitaires / PBT

- **Tests unitaires** : cas d'intégration UI (TestFX), exemples concrets (hub 3 cards), cas limites (durée = 1 puis `−`, durée = 8 puis `+`)
- **Tests PBT** : toutes les propriétés universelles listées ci-dessus (Properties 1–11)
- Chaque test PBT référence sa propriété de design via le tag commentaire `// Feature: espace-participant-evenement, Property N`
- Configuration minimale : 100 itérations par propriété (`@Property(tries = 100)`)
