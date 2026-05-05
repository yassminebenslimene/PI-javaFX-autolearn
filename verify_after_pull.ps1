# Script de Vérification Après Pull
# Vérifie que votre travail est toujours intact après le pull

Write-Host "🔍 Vérification de l'Intégrité du Projet" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

$errors = 0
$warnings = 0

# 1. Vérifier que config.properties existe localement
Write-Host "📋 1. Vérification config.properties..." -ForegroundColor Yellow
if (Test-Path "src/main/resources/config.properties") {
    Write-Host "   ✅ config.properties existe" -ForegroundColor Green
    
    # Vérifier que les clés sont présentes
    $config = Get-Content "src/main/resources/config.properties" -Raw
    if ($config -match "groq.api.key=gsk_") {
        Write-Host "   ✅ Groq API key présente" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Groq API key manquante" -ForegroundColor Yellow
        $warnings++
    }
    
    if ($config -match "google.oauth.client.id=") {
        Write-Host "   ✅ Google OAuth configuré" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  Google OAuth manquant" -ForegroundColor Yellow
        $warnings++
    }
} else {
    Write-Host "   ❌ config.properties manquant!" -ForegroundColor Red
    Write-Host "   → Copiez config.properties.example vers config.properties" -ForegroundColor Yellow
    $errors++
}

# 2. Vérifier les fichiers clés du dashboard
Write-Host ""
Write-Host "📋 2. Vérification Dashboard 3D..." -ForegroundColor Yellow
$dashboardFiles = @(
    "src/main/java/tn/esprit/controllers/DashboardController.java",
    "src/main/java/tn/esprit/services/UserAiInsightService.java",
    "src/main/resources/views/backoffice/dashboard.fxml"
)

foreach ($file in $dashboardFiles) {
    if (Test-Path $file) {
        Write-Host "   ✅ $($file.Split('/')[-1])" -ForegroundColor Green
    } else {
        Write-Host "   ❌ $($file.Split('/')[-1]) manquant!" -ForegroundColor Red
        $errors++
    }
}

# 3. Vérifier que le code utilise ConfigLoader
Write-Host ""
Write-Host "📋 3. Vérification ConfigLoader..." -ForegroundColor Yellow
$userAiService = Get-Content "src/main/java/tn/esprit/services/UserAiInsightService.java" -Raw
if ($userAiService -match "ConfigLoader\.getProperty") {
    Write-Host "   ✅ UserAiInsightService utilise ConfigLoader" -ForegroundColor Green
} else {
    Write-Host "   ❌ UserAiInsightService n'utilise pas ConfigLoader!" -ForegroundColor Red
    $errors++
}

$googleService = Get-Content "src/main/java/tn/esprit/services/GoogleOAuthService.java" -Raw
if ($googleService -match "ConfigLoader\.getProperty") {
    Write-Host "   ✅ GoogleOAuthService utilise ConfigLoader" -ForegroundColor Green
} else {
    Write-Host "   ❌ GoogleOAuthService n'utilise pas ConfigLoader!" -ForegroundColor Red
    $errors++
}

# 4. Vérifier les fichiers FXML avec padding
Write-Host ""
Write-Host "📋 4. Vérification Padding 150px..." -ForegroundColor Yellow
$fxmlFiles = Get-ChildItem -Path "src/main/resources/views" -Filter "*.fxml" -Recurse
$fxmlWithPadding = 0
$totalFxml = $fxmlFiles.Count

foreach ($fxml in $fxmlFiles) {
    $content = Get-Content $fxml.FullName -Raw
    if ($content -match "prefHeight=`"150\.0`"" -or $content -match "minHeight=`"150\.0`"") {
        $fxmlWithPadding++
    }
}

Write-Host "   ✅ $fxmlWithPadding/$totalFxml fichiers FXML avec padding" -ForegroundColor Green
if ($fxmlWithPadding -lt 70) {
    Write-Host "   ⚠️  Certains fichiers FXML n'ont peut-être pas le padding" -ForegroundColor Yellow
    $warnings++
}

# 5. Vérifier que .hf_token n'existe pas
Write-Host ""
Write-Host "📋 5. Vérification Sécurité..." -ForegroundColor Yellow
if (-not (Test-Path ".hf_token")) {
    Write-Host "   ✅ .hf_token supprimé (sécurité OK)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  .hf_token existe encore" -ForegroundColor Yellow
    $warnings++
}

# 6. Vérifier .gitignore
if (Test-Path ".gitignore") {
    $gitignore = Get-Content ".gitignore" -Raw
    if ($gitignore -match "config\.properties") {
        Write-Host "   ✅ config.properties dans .gitignore" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  config.properties pas dans .gitignore" -ForegroundColor Yellow
        $warnings++
    }
}

# 7. Vérifier l'état Git
Write-Host ""
Write-Host "📋 6. État Git..." -ForegroundColor Yellow
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Host "   ⚠️  Fichiers modifiés non commités:" -ForegroundColor Yellow
    $gitStatus | ForEach-Object { Write-Host "      $_" -ForegroundColor Gray }
    $warnings++
} else {
    Write-Host "   ✅ Aucun fichier modifié" -ForegroundColor Green
}

# Résumé
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "📊 RÉSUMÉ" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

if ($errors -eq 0 -and $warnings -eq 0) {
    Write-Host "✅ Tout est parfait! Votre projet est prêt." -ForegroundColor Green
} elseif ($errors -eq 0) {
    Write-Host "⚠️  $warnings avertissement(s) - Le projet devrait fonctionner" -ForegroundColor Yellow
} else {
    Write-Host "❌ $errors erreur(s) et $warnings avertissement(s)" -ForegroundColor Red
    Write-Host "   → Corrigez les erreurs avant de lancer l'application" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "🚀 Pour lancer l'application:" -ForegroundColor Cyan
Write-Host "   mvn clean javafx:run" -ForegroundColor White
Write-Host ""
