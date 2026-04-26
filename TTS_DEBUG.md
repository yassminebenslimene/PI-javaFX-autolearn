# Guide de Débogage - Synthèse Vocale (TTS)

## 🔧 Test Rapide

Pour vérifier si la synthèse vocale fonctionne sur votre système Windows:

1. Ouvrez PowerShell
2. Naviguez vers le dossier du projet
3. Exécutez: `.\test_tts.ps1`

Ce script va:
- ✅ Vérifier que System.Speech est disponible
- 📋 Lister toutes les voix installées sur votre système
- 🔊 Tester la lecture vocale avec un message simple
- 📊 Afficher des informations de diagnostic

## 🎯 Dans l'Application

Quand vous ouvrez l'Assistant IA:

1. Cliquez sur "✨ Générer l'explication"
2. Attendez que le contenu soit généré
3. Cliquez sur le bouton "🔧 Test" pour tester TTS avec un message simple
4. Si le test fonctionne, cliquez sur "▶ Lire" pour lire le contenu complet

## 📝 Logs de Débogage

L'application affiche maintenant des logs détaillés dans la console:

```
[TTS] Texte à lire: ...
[TTS] Vitesse: 0
[TTS] Script créé: C:\Users\...\tts_xxx.ps1
[TTS Output] Voix disponibles:
[TTS Output]   - Microsoft David Desktop [en-US]
[TTS Output]   - Microsoft Hortense Desktop [fr-FR]
[TTS Output] Utilisation de la voix: Microsoft Hortense Desktop
[TTS Output] Démarrage de la lecture...
[TTS Output] Lecture terminée
[TTS] Code de sortie: 0
```

## ❌ Problèmes Courants

### Problème: Aucun son
**Solutions:**
- Vérifiez que vos haut-parleurs/casque sont branchés
- Vérifiez le volume Windows (icône haut-parleur dans la barre des tâches)
- Testez avec un autre logiciel (YouTube, lecteur audio) pour confirmer que le son fonctionne

### Problème: "Aucune voix installée"
**Solutions:**
- Installez des voix Windows:
  1. Paramètres Windows → Heure et langue → Langue
  2. Ajoutez le français (si pas déjà fait)
  3. Cliquez sur Français → Options
  4. Téléchargez "Synthèse vocale"

### Problème: Code de sortie différent de 0
**Solutions:**
- Vérifiez les logs dans la console
- Exécutez `.\test_tts.ps1` pour plus de détails
- Vérifiez que PowerShell peut exécuter des scripts:
  ```powershell
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
  ```

## 🌐 Voix Françaises Recommandées

Pour Windows 10/11:
- **Microsoft Hortense Desktop** (fr-FR) - Voix féminine française
- **Microsoft Paul Desktop** (fr-FR) - Voix masculine française (si disponible)

Pour installer plus de voix:
1. Paramètres → Heure et langue → Langue
2. Français → Options → Synthèse vocale
3. Télécharger les voix disponibles

## 🔍 Vérification Manuelle

Vous pouvez aussi tester TTS directement dans PowerShell:

```powershell
Add-Type -AssemblyName System.Speech
$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.Speak("Bonjour, ceci est un test")
```

Si cette commande fonctionne, alors l'application devrait aussi fonctionner.

## 📞 Support

Si le problème persiste après avoir suivi ce guide:
1. Exécutez `.\test_tts.ps1` et copiez la sortie complète
2. Vérifiez les logs de la console Java
3. Vérifiez que vous utilisez Windows (TTS ne fonctionne pas sur Mac/Linux avec cette implémentation)
