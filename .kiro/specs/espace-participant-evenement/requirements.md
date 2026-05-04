# Document de Exigences

## Introduction

L'**Espace Participant Événement** est une fonctionnalité du module événement de la plateforme AutoLearn (JavaFX, pattern MVC). Elle est accessible uniquement lorsqu'un événement a le statut dynamique `"En cours"` (calculé via `computeStatus()`). Elle s'affiche sous forme de popup/overlay centré — sans navigation vers une nouvelle vue — et propose trois sous-fonctionnalités interactives et gamifiées : une Vending Machine, un Menu Déjeuner & Pause Café, et un système d'Emprunt de Matériel. Toutes les données sont gérées en mémoire (in-memory), sans modification de la base de données.

---

## Glossaire

- **EspaceParticipant** : Le popup/overlay principal affiché quand un événement est "En cours", contenant les 3 fonctionnalités.
- **VendingMachine** : Sous-fonctionnalité gamifiée permettant de "tirer" un item (boisson/snack) avec animation et son.
- **MenuDejeuner** : Sous-fonctionnalité affichant le menu déjeuner et les snacks de pause café.
- **EmpruntMateriel** : Sous-fonctionnalité permettant d'emprunter du matériel disponible pendant l'événement.
- **EvenementFrontController** : Contrôleur JavaFX gérant la liste des événements côté front-office.
- **SessionManager** : Singleton fournissant l'utilisateur connecté via `getCurrentUser()`.
- **computeStatus()** : Méthode de l'entité `Evenement` calculant dynamiquement le statut selon les dates.
- **ModalOverlay** : Fenêtre `Stage` transparente avec `StackPane` overlay et `VBox` modal, créée programmatiquement en Java (même pattern que `showDetailsModal()`).
- **ItemMateriel** : Objet représentant un équipement empruntable (nom, emoji, disponibilité).
- **ItemVending** : Objet représentant une boisson ou un snack dans la VendingMachine (nom, emoji, prix).

---

## Exigences

### Exigence 1 : Accès à l'Espace Participant

**User Story :** En tant que participant, je veux accéder à un espace dédié pendant un événement en cours, afin de profiter des services disponibles sans quitter la page des événements.

#### Critères d'Acceptation

1. WHEN `computeStatus()` retourne `"En cours"` pour un événement, THE `EvenementFrontController` SHALL afficher un bouton `"🎯 Espace Participant"` sur la card de cet événement.
2. WHEN l'utilisateur clique sur le bouton `"🎯 Espace Participant"`, THE `EvenementFrontController` SHALL ouvrir un `ModalOverlay` centré sur la fenêtre principale sans naviguer vers une nouvelle vue FXML.
3. THE `EspaceParticipant` SHALL afficher le titre de l'événement en cours dans l'en-tête du modal.
4. THE `EspaceParticipant` SHALL présenter 3 cards/boutons d'accès aux sous-fonctionnalités : VendingMachine, MenuDejeuner, EmpruntMateriel.
5. WHEN l'utilisateur clique en dehors du `ModalOverlay` ou sur le bouton "✕", THE `EspaceParticipant` SHALL se fermer avec une animation de fondu.
6. THE `EspaceParticipant` SHALL utiliser la palette de couleurs violet/rose (`#667eea`, `#764ba2`, fond `#f5f3ff`) cohérente avec la plateforme.
7. IF `SessionManager.getCurrentUser()` retourne `null`, THEN THE `EvenementFrontController` SHALL ne pas afficher le bouton `"🎯 Espace Participant"`.

---

### Exigence 2 : Vending Machine Gamifiée

**User Story :** En tant que participant, je veux interagir avec une vending machine virtuelle fun, afin de me divertir pendant les pauses de l'événement.

#### Critères d'Acceptation

1. WHEN l'utilisateur clique sur la card `"🎰 Vending Machine"` dans l'EspaceParticipant, THE `VendingMachine` SHALL ouvrir un `ModalOverlay` secondaire affichant la liste des items disponibles.
2. THE `VendingMachine` SHALL afficher au minimum 8 items (boissons et snacks) avec pour chacun : un emoji, un nom, et un prix en DT, stockés en mémoire (données hardcodées).
3. THE `VendingMachine` SHALL afficher chaque item dans un badge de style "starburst" (forme étoilée ou burst) avec fond coloré dégradé violet/rose.
4. WHEN l'utilisateur clique sur un item, THE `VendingMachine` SHALL jouer un effet sonore de style jeu/surprise via `javafx.scene.media.AudioClip`.
5. WHEN l'utilisateur clique sur un item, THE `VendingMachine` SHALL afficher une animation de révélation (style cadeau/surprise) avec l'emoji et le nom de l'item sélectionné.
6. THE `VendingMachine` SHALL afficher un bouton `"🔄 Rejouer"` après la révélation permettant de revenir à la liste des items.
7. IF le fichier audio est introuvable dans les ressources, THEN THE `VendingMachine` SHALL continuer l'animation de révélation sans son, sans lever d'exception visible à l'utilisateur.

---

### Exigence 3 : Menu Déjeuner & Pause Café

**User Story :** En tant que participant, je veux consulter le menu déjeuner et les snacks de pause café, afin de savoir ce qui est proposé pendant l'événement.

#### Critères d'Acceptation

1. WHEN l'utilisateur clique sur la card `"🍽️ Menu & Pause Café"` dans l'EspaceParticipant, THE `MenuDejeuner` SHALL ouvrir un `ModalOverlay` secondaire affichant le menu.
2. THE `MenuDejeuner` SHALL afficher deux sections distinctes : `"🍽️ Menu Déjeuner"` et `"☕ Pause Café & Snacks"`.
3. THE `MenuDejeuner` SHALL afficher au minimum 4 plats pour le déjeuner et 4 items pour la pause café, avec pour chacun : un emoji, un nom, et une description courte, stockés en mémoire (données hardcodées).
4. THE `MenuDejeuner` SHALL afficher chaque item dans une card animée avec un effet d'apparition séquentielle (FadeTransition + TranslateTransition) au chargement du modal.
5. THE `MenuDejeuner` SHALL utiliser un fond dégradé violet/rose cohérent avec la palette de la plateforme pour les en-têtes de section.
6. WHILE le `ModalOverlay` du MenuDejeuner est ouvert, THE `MenuDejeuner` SHALL permettre le défilement vertical si le contenu dépasse la hauteur visible.

---

### Exigence 4 : Emprunt de Matériel

**User Story :** En tant que participant, je veux emprunter du matériel pendant l'événement, afin de disposer des équipements nécessaires à ma participation.

#### Critères d'Acceptation

1. WHEN l'utilisateur clique sur la card `"🔌 Emprunt Matériel"` dans l'EspaceParticipant, THE `EmpruntMateriel` SHALL ouvrir un `ModalOverlay` secondaire affichant la liste des items.
2. THE `EmpruntMateriel` SHALL afficher au minimum 12 items de matériel (chargeur, multiprise, vidéoprojecteur, câble HDMI, adaptateur USB-C, marqueurs, post-its, extension WiFi, casque audio, webcam, pointeur laser, rallonge électrique), chacun avec : un emoji, un nom, et un badge de disponibilité.
3. THE `EmpruntMateriel` SHALL afficher un badge `"✅ Disponible"` (fond vert) pour les items disponibles et un badge `"🔴 Occupé"` (fond rouge/orange) pour les items non disponibles, basé sur un `Map<String, Boolean>` en mémoire.
4. WHEN l'utilisateur clique sur un item avec le badge `"✅ Disponible"`, THE `EmpruntMateriel` SHALL afficher un formulaire d'emprunt dans le même `ModalOverlay` sans navigation.
5. THE `EmpruntMateriel` SHALL pré-remplir le champ "Nom" du formulaire avec `SessionManager.getCurrentUser().getPrenom() + " " + SessionManager.getCurrentUser().getNom()`.
6. THE `EmpruntMateriel` SHALL proposer un sélecteur de durée incrémental (boutons `−` et `+`) avec une valeur minimale de 1 heure et une valeur maximale de 8 heures, par pas de 1 heure.
7. WHEN l'utilisateur confirme l'emprunt, THE `EmpruntMateriel` SHALL mettre à jour le `Map<String, Boolean>` en mémoire pour marquer l'item comme `false` (Occupé) et revenir à la liste des items.
8. WHEN l'utilisateur confirme l'emprunt, THE `EmpruntMateriel` SHALL afficher un message de confirmation visuel (banner vert) dans la liste des items.
9. WHEN l'utilisateur clique sur un item avec le badge `"🔴 Occupé"`, THE `EmpruntMateriel` SHALL ne pas ouvrir de formulaire et afficher un message `"Cet item est actuellement utilisé."`.
10. IF `SessionManager.getCurrentUser()` retourne `null` au moment de la confirmation, THEN THE `EmpruntMateriel` SHALL afficher un message d'erreur `"Utilisateur non connecté."` sans modifier l'état en mémoire.

---

### Exigence 5 : Cohérence Technique et Non-Régression

**User Story :** En tant que développeur, je veux que la nouvelle fonctionnalité respecte les patterns existants et n'altère pas le module événement, afin de garantir la stabilité de la plateforme.

#### Critères d'Acceptation

1. THE `EspaceParticipant` SHALL utiliser exclusivement le pattern `ModalOverlay` (Stage transparent + StackPane + VBox) identique à `EvenementFrontController.showDetailsModal()`, sans créer de nouveaux fichiers FXML.
2. THE `EspaceParticipant` SHALL être implémenté dans un fichier Java dédié `EspaceParticipantController.java` dans le package `tn.esprit.controllers.evenement.front`.
3. THE `EvenementFrontController` SHALL appeler `EspaceParticipantController.show(ev, ownerWindow)` pour ouvrir l'espace participant, sans dupliquer la logique de modal.
4. THE `EspaceParticipant` SHALL ne créer aucune nouvelle table ni modifier la structure de la base de données existante.
5. THE `EspaceParticipant` SHALL ne modifier aucune méthode existante de `EvenementFrontController` autre que `buildEventCard()` pour ajouter le bouton conditionnel.
6. WHEN `computeStatus()` ne retourne pas `"En cours"`, THE `EvenementFrontController` SHALL ne pas afficher le bouton `"🎯 Espace Participant"` sur la card de l'événement.
7. THE `EspaceParticipant` SHALL utiliser `javafx.scene.media.AudioClip` pour les effets sonores, avec gestion silencieuse des erreurs de chargement (try/catch sans propagation).
