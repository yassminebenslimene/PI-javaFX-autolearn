# 🎙️ Guide d'Utilisation - Synthèse Vocale

## 🎨 Nouvelles Couleurs

L'interface de l'Assistant IA a été complètement redessinée avec des couleurs claires et modernes:

- ✨ Fond blanc au lieu de noir
- 🎨 Violet moderne (#8b5cf6) pour les éléments actifs
- 🟢 Vert vif pour le bouton "Lire"
- 🟠 Orange pour "Pause"
- 🔴 Rouge pour "Stop"
- 🔵 Bleu pour "Test"

## 📖 Comment Utiliser la Synthèse Vocale

### Étape 1: Ouvrir l'Assistant IA
1. Naviguez vers un chapitre de cours
2. Cliquez sur le bouton **🤖 Assistant IA** dans la barre latérale

### Étape 2: Générer l'Explication
1. Choisissez votre niveau: **🟢 Débutant** ou **🔴 Avancé**
2. Cliquez sur **✨ Générer l'explication**
3. Attendez quelques secondes que l'IA génère le contenu

### Étape 3: Tester la Synthèse Vocale (Recommandé)
1. Une fois le contenu généré, la section **🔊 Synthèse Vocale** apparaît
2. Cliquez d'abord sur **🔧 Test** pour vérifier que TTS fonctionne
3. Vous devriez entendre: "Bonjour, ceci est un test de synthèse vocale"
4. Si vous entendez le message, passez à l'étape suivante

### Étape 4: Lire le Contenu
1. Cliquez sur **▶ Lire** pour démarrer la lecture du contenu complet
2. Le statut affichera: **🔊 Lecture en cours...**
3. Utilisez le curseur **Vitesse** pour ajuster la vitesse de lecture (0.5x à 2.0x)

### Étape 5: Contrôles de Lecture
- **⏸ Pause**: Arrête temporairement la lecture
- **⏹ Stop**: Arrête complètement la lecture
- **Vitesse**: Ajustez entre 0.5x (lent) et 2.0x (rapide)

## 🔧 Dépannage

### Le bouton Test ne produit aucun son

**Vérifications:**
1. ✅ Vos haut-parleurs/casque sont-ils branchés et allumés?
2. ✅ Le volume Windows est-il supérieur à 0?
3. ✅ Avez-vous des voix Windows installées?

**Pour vérifier les voix installées:**
```
1. Ouvrez PowerShell
2. Exécutez: .\test_tts.ps1
3. Regardez la liste des voix disponibles
```

### Le test fonctionne mais pas la lecture du contenu

**Cause probable:** Le texte est trop long ou contient des caractères spéciaux

**Solution:** Le service limite automatiquement à 2000 caractères et nettoie le texte

### Message d'erreur dans le statut

**Regardez la console Java** pour voir les logs détaillés:
```
[TTS] Texte à lire: ...
[TTS] Vitesse: 0
[TTS Output] Voix disponibles:
[TTS Output] Démarrage de la lecture...
```

## 📊 Indicateurs de Statut

L'application affiche des messages clairs:

- **🔊 Démarrage de la lecture...** - TTS initialise
- **🔊 Lecture en cours...** - Le texte est en train d'être lu
- **✅ Lecture terminée** - Lecture complétée avec succès
- **⏸ Lecture arrêtée** - Vous avez cliqué sur Pause
- **⏹ Lecture stoppée** - Vous avez cliqué sur Stop
- **✅ Test réussi ! TTS fonctionne.** - Le test a fonctionné
- **❌ Erreur: ...** - Une erreur s'est produite (voir console)

## 🎯 Conseils d'Utilisation

1. **Testez d'abord**: Utilisez toujours le bouton **🔧 Test** avant la première utilisation
2. **Ajustez la vitesse**: Commencez à 1.0x et ajustez selon votre préférence
3. **Texte court**: Pour de meilleurs résultats, générez des explications concises
4. **Voix française**: L'application sélectionne automatiquement une voix française si disponible

## 🌟 Fonctionnalités

- ✅ Détection automatique des voix françaises
- ✅ Fallback vers la voix par défaut si pas de voix française
- ✅ Nettoyage automatique du texte (suppression HTML, caractères spéciaux)
- ✅ Limitation à 2000 caractères pour éviter les lectures trop longues
- ✅ Logs détaillés pour le débogage
- ✅ Bouton de test intégré
- ✅ Contrôle de la vitesse de lecture
- ✅ Interface moderne avec couleurs claires

## 📞 Besoin d'Aide?

Si vous rencontrez des problèmes:

1. Consultez **TTS_DEBUG.md** pour le guide de débogage complet
2. Exécutez le script de test: `.\test_tts.ps1`
3. Vérifiez les logs dans la console Java
4. Assurez-vous d'utiliser Windows (requis pour cette implémentation)

---

**Profitez de votre expérience d'apprentissage améliorée! 🎓✨**
