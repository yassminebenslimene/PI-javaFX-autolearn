# 🎯 PLAN DE CORRECTIONS — ESPACE PARTICIPANT

**Date:** 27 Avril 2026  
**Status:** 🔄 **EN COURS DE CORRECTION**

---

## 📋 RÉSUMÉ DES 8 CORRECTIONS MAJEURES

### 1. Interface Espace Participant — Refonte Complète
**Problème :** Modal avec liste scrollable, pas d'interface dédiée
**Solution :** 
- Créer une interface dédiée complète (pas de modal)
- Affichage full-screen des 5 fonctionnalités (Vending, Café, Memory, Candy, Emprunt)
- Animations impressionnantes sur toute l'interface
- Modals au centre de cette interface (pas de navigation)
- Sounds d'ambiance cool et amusants

**Fichiers à modifier :**
- `EspaceParticipantController.java` — refonte complète

---

### 2. Candy Crush — Éléments Jouables
**Problème :** Éléments statiques, non-draggables, non-jouables
**Solution :**
- Implémenter drag-and-drop pour swapper les éléments
- Animations fluides lors du swap
- Détection des matches en temps réel
- Animations d'explosion des matches

**Fichiers à modifier :**
- `CandyGameController.java` — ajouter drag-and-drop

---

### 3. Sons Candy Crush — Réalistes
**Problème :** Sons synthétiques générés, pas réalistes
**Solution :**
- Rechercher et intégrer vrais sons Candy Crush
- Sons swap, match, explosion, victoire
- Pas de retard entre action et son

**Fichiers à modifier :**
- `CandyGameController.java` — remplacer sons synthétiques
- `SoundGenerator.java` — ajouter sons réalistes

---

### 4. Machine Café — Image + Sons + Photos
**Problème :** Pas d'image réelle, sons synthétiques, pas de photos types de café
**Solution :**
- Intégrer image réelle machine à café (via API ou ressource)
- Vrais sons café (grinder, steam, drip, ding)
- Photos types de café (via API ou ressources)
- Affichage professionnel

**Fichiers à modifier :**
- `CoinCafeController.java` — ajouter images et sons réalistes

---

### 5. Affichage API — Professionnel
**Problème :** Noms API visibles ("Open Food Facts", "Quotable", etc.)
**Solution :**
- Masquer les noms API
- Afficher uniquement les données utiles
- Formatage professionnel et cool

**Fichiers à modifier :**
- `CoinCafeController.java` — nettoyer affichage
- `MemoryGameController.java` — nettoyer affichage

---

### 6. QR Code / PDF — Popup Après Confirmation
**Problème :** Affichage dans la liste des items, persiste lors du changement d'item
**Solution :**
- Affichage QR/PDF uniquement après confirmation du formulaire
- Popup séparé, pas dans la liste
- Disparaît lors de la sélection d'un autre item

**Fichiers à modifier :**
- `EmpruntMaterielController.java` — restructurer affichage QR/PDF

---

### 7. Vending Machine — Plus d'Items
**Problème :** Beaucoup d'espace blanc vide
**Solution :**
- Augmenter le nombre d'items (de 8 à 12-16)
- Remplir l'espace disponible
- Grille responsive

**Fichiers à modifier :**
- `VendingMachineController.java` — augmenter items

---

### 8. Memory Game — Cartes Visibles + Sons
**Problème :** Cartes vides/invisibles, sons mauvais, retard
**Solution :**
- Cartes avec images visibles (via API TheCatAPI)
- Animation distribution des cartes avec sons
- Sons flip cool et amusants
- Pas de retard entre action et son

**Fichiers à modifier :**
- `MemoryGameController.java` — améliorer cartes et sons

---

## 🔧 PLAN D'IMPLÉMENTATION DÉTAILLÉ

### Phase 1 : Refonte Interface Espace Participant
1. Créer interface dédiée full-screen
2. Ajouter animations impressionnantes
3. Ajouter sounds d'ambiance
4. Intégrer images pour chaque fonctionnalité

### Phase 2 : Candy Crush Jouable
1. Implémenter drag-and-drop
2. Ajouter vrais sons Candy Crush
3. Tester jouabilité

### Phase 3 : Machine Café Réaliste
1. Intégrer image réelle machine
2. Ajouter vrais sons café
3. Ajouter photos types de café
4. Nettoyer affichage API

### Phase 4 : Memory Game Amélioré
1. Améliorer visibilité des cartes
2. Ajouter animation distribution
3. Ajouter sons flip réalistes
4. Tester synchronisation son/action

### Phase 5 : QR Code / PDF Popup
1. Restructurer affichage QR/PDF
2. Affichage uniquement après confirmation
3. Disparition lors du changement d'item

### Phase 6 : Vending Machine Plus d'Items
1. Augmenter nombre d'items
2. Ajouter quotes amusantes
3. Remplir espace blanc

---

## 📝 NOTES IMPORTANTES

- **Commit git fait** : sauvegarde avant modifications
- **Prudence** : tester chaque modification pour ne pas générer d'erreurs
- **Images** : utiliser APIs (TheCatAPI, Giphy) ou ressources locales
- **Sons** : rechercher vrais sons Candy Crush, café, memory
- **Pas de retard** : synchroniser action et son parfaitement

---

**Prêt pour implémentation**
