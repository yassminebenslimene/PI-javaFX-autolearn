# Script to remove secrets from Git history
# This will rewrite history to remove API keys from old commits

Write-Host "🔒 Git History Cleanup Script" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

# Check if we're in a git repository
if (-not (Test-Path .git)) {
    Write-Host "❌ Error: Not in a git repository!" -ForegroundColor Red
    exit 1
}

Write-Host "⚠️  WARNING: This will rewrite Git history!" -ForegroundColor Yellow
Write-Host "Make sure you have a backup before proceeding." -ForegroundColor Yellow
Write-Host ""
Write-Host "This script will:" -ForegroundColor White
Write-Host "  1. Remove .hf_token from all commits" -ForegroundColor White
Write-Host "  2. Replace API keys in GoogleOAuthService.java" -ForegroundColor White
Write-Host "  3. Replace API keys in UserAiInsightService.java" -ForegroundColor White
Write-Host ""

$response = Read-Host "Do you want to continue? (yes/no)"
if ($response -ne "yes") {
    Write-Host "❌ Aborted by user" -ForegroundColor Red
    exit 0
}

Write-Host ""
Write-Host "📦 Creating backup branch..." -ForegroundColor Cyan
git branch backup-before-history-rewrite 2>$null

Write-Host "🔧 Step 1: Removing .hf_token from history..." -ForegroundColor Cyan
git filter-branch --force --index-filter `
    "git rm --cached --ignore-unmatch .hf_token" `
    --prune-empty --tag-name-filter cat -- --all

Write-Host "🔧 Step 2: Replacing Google OAuth secrets..." -ForegroundColor Cyan
git filter-branch --force --tree-filter `
    "if [ -f src/main/java/tn/esprit/services/GoogleOAuthService.java ]; then sed -i 's/700826827550-n3rrg9o10j91ngvhrsq70ljcvdoqdqtu.apps.googleusercontent.com/YOUR_GOOGLE_CLIENT_ID_HERE/g' src/main/java/tn/esprit/services/GoogleOAuthService.java; sed -i 's/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/YOUR_GOOGLE_CLIENT_SECRET_HERE/g' src/main/java/tn/esprit/services/GoogleOAuthService.java; fi" `
    --prune-empty --tag-name-filter cat -- --all

Write-Host "🔧 Step 3: Replacing Groq API key..." -ForegroundColor Cyan
git filter-branch --force --tree-filter `
    "if [ -f src/main/java/tn/esprit/services/UserAiInsightService.java ]; then sed -i 's/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/YOUR_GROQ_API_KEY_HERE/g' src/main/java/tn/esprit/services/UserAiInsightService.java; fi" `
    --prune-empty --tag-name-filter cat -- --all

Write-Host "🧹 Cleaning up..." -ForegroundColor Cyan
git reflog expire --expire=now --all
git gc --prune=now --aggressive

Write-Host ""
Write-Host "✅ History rewritten successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Verify the changes: git log --oneline -5" -ForegroundColor White
Write-Host "  2. Force push: git push origin integration --force" -ForegroundColor White
Write-Host ""
Write-Host "⚠️  If something goes wrong, restore from backup:" -ForegroundColor Yellow
Write-Host "     git reset --hard backup-before-history-rewrite" -ForegroundColor Yellow
