# Script de test pour la synthèse vocale Windows SAPI
# Exécutez ce script pour vérifier que TTS fonctionne sur votre système

Write-Host "=== Test de Synthèse Vocale Windows SAPI ===" -ForegroundColor Cyan
Write-Host ""

try {
    # Charger l'assembly System.Speech
    Add-Type -AssemblyName System.Speech
    Write-Host "[OK] Assembly System.Speech chargée" -ForegroundColor Green
    
    # Créer le synthétiseur
    $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
    Write-Host "[OK] SpeechSynthesizer créé" -ForegroundColor Green
    Write-Host ""
    
    # Lister toutes les voix disponibles
    Write-Host "Voix installées sur votre système:" -ForegroundColor Yellow
    $voices = $synth.GetInstalledVoices()
    
    if ($voices.Count -eq 0) {
        Write-Host "[ERREUR] Aucune voix installée!" -ForegroundColor Red
        exit 1
    }
    
    foreach ($voice in $voices) {
        $info = $voice.VoiceInfo
        $enabled = if ($voice.Enabled) { "✓" } else { "✗" }
        Write-Host "  $enabled $($info.Name)" -ForegroundColor $(if ($voice.Enabled) { "Green" } else { "Gray" })
        Write-Host "     Culture: $($info.Culture.Name)" -ForegroundColor Gray
        Write-Host "     Genre: $($info.Gender), Age: $($info.Age)" -ForegroundColor Gray
        Write-Host ""
    }
    
    # Chercher une voix française
    $frenchVoice = $voices | Where-Object { $_.VoiceInfo.Culture.Name -like 'fr*' -and $_.Enabled } | Select-Object -First 1
    
    if ($frenchVoice) {
        Write-Host "[OK] Voix française trouvée: $($frenchVoice.VoiceInfo.Name)" -ForegroundColor Green
        $synth.SelectVoice($frenchVoice.VoiceInfo.Name)
    } else {
        Write-Host "[INFO] Aucune voix française trouvée, utilisation de la voix par défaut" -ForegroundColor Yellow
    }
    
    Write-Host ""
    Write-Host "Configuration du synthétiseur:" -ForegroundColor Yellow
    Write-Host "  Voix sélectionnée: $($synth.Voice.Name)" -ForegroundColor Cyan
    Write-Host "  Volume: 100" -ForegroundColor Cyan
    Write-Host "  Vitesse: 0 (normale)" -ForegroundColor Cyan
    Write-Host ""
    
    # Configurer le synthétiseur
    $synth.Volume = 100
    $synth.Rate = 0
    
    # Test de lecture
    Write-Host "Démarrage du test de lecture..." -ForegroundColor Yellow
    Write-Host "Vous devriez entendre: 'Bonjour, ceci est un test de synthèse vocale.'" -ForegroundColor Cyan
    Write-Host ""
    
    $synth.Speak("Bonjour, ceci est un test de synthèse vocale.")
    
    Write-Host ""
    Write-Host "[OK] Test terminé avec succès!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Si vous avez entendu le message, la synthèse vocale fonctionne correctement." -ForegroundColor Green
    Write-Host "Sinon, vérifiez:" -ForegroundColor Yellow
    Write-Host "  1. Que vos haut-parleurs/casque sont branchés et allumés" -ForegroundColor Gray
    Write-Host "  2. Que le volume Windows n'est pas à 0" -ForegroundColor Gray
    Write-Host "  3. Qu'au moins une voix est installée (voir liste ci-dessus)" -ForegroundColor Gray
    
    exit 0
    
} catch {
    Write-Host ""
    Write-Host "[ERREUR] Une erreur s'est produite:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host ""
    Write-Host "Stack trace:" -ForegroundColor Gray
    Write-Host $_.Exception.StackTrace -ForegroundColor Gray
    exit 1
}
