# 🎯 PLAN D'ACTION — CORRECTIONS DÉTAILLÉES

**Date:** 27 Avril 2026  
**Status:** 🔄 **PRÊT POUR IMPLÉMENTATION**

---

## ⚠️ IMPORTANT

**Commit git fait :** Sauvegarde avant corrections majeures

**Approche :** Une correction à la fois, très prudemment, sans générer d'erreurs

---

## 📋 CORRECTIONS À FAIRE (ORDRE PRIORITAIRE)

### CORRECTION 1 : Interface Espace Participant — Refonte Complète
**Fichier :** `EspaceParticipantController.java`

**Changements :**
- Remplacer modal scrollable par interface dédiée full-screen
- Ajouter animations impressionnantes (entrée, hover, etc.)
- Ajouter sounds d'ambiance
- Intégrer images pour chaque fonctionnalité
- Affichage très coloré et animé

**Risques :** Refonte majeure, tester complètement

---

### CORRECTION 2 : Machine Café — Image + Photos + Affichage Professionnel
**Fichier :** `CoinCafeController.java`

**Changements :**
- Intégrer image réelle machine à café
- Intégrer photos types de café
- Masquer noms API ("Open Food Facts", "Quotable")
- Affichage professionnel uniquement

**Risques :** Modification de l'affichage, tester complètement

---

### CORRECTION 3 : Affichage API — Professionnel Partout
**Fichiers :** `CoinCafeController.java`, `MemoryGameController.java`, `VendingMachineController.java`

**Changements :**
- Masquer tous les noms API
- Affichage uniquement données utiles
- Formatage professionnel

**Risques :** Modification d'affichage, tester complètement

---

### CORRECTION 4 : QR Code / PDF — Popup Après Confirmation
**Fichier :** `EmpruntMaterielController.java`

**Changements :**
- Restructurer affichage QR/PDF
- Affichage uniquement après confirmation du formulaire
- Popup séparé (pas dans la liste)
- Disparition lors du changement d'item

**Risques :** Restructuration majeure, tester complètement

---

### CORRECTION 5 : Vending Machine — Plus d'Items
**Fichier :** `VendingMachineController.java`

**Changements :**
- Augmenter nombre d'items (12-16)
- Remplir espace blanc
- Grille responsive

**Risques :** Modification de la grille, tester complètement

---

### CORRECTION 6 : Vending Machine — Quotes Amusantes
**Fichier :** `VendingMachineController.java`

**Changements :**
- Ajouter quotes amusantes
- Affichage avec le bouton "Obtenir un autre"
- Thème snacks et vending machine

**Risques :** Ajout de fonctionnalité, tester complètement

---

## 🔐 STRATÉGIE DE SÉCURITÉ

1. **Commit avant chaque correction majeure**
2. **Vérifier compilation après chaque changement**
3. **Tester complètement chaque correction**
4. **Ne pas générer d'erreurs**
5. **Rollback si problème**

---

## ✅ CHECKLIST

- [ ] Correction 1 : Interface Espace Participant
- [ ] Correction 2 : Machine Café
- [ ] Correction 3 : Affichage API
- [ ] Correction 4 : QR/PDF Popup
- [ ] Correction 5 : Vending Machine Plus d'Items
- [ ] Correction 6 : Vending Machine Quotes
- [ ] Vérification complète
- [ ] Commit final

---

**Prêt pour implémentation**
