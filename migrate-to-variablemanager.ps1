# Bulk Migration Script for VariableManager
# This script updates all remaining test files to use VariableManager

$sourceDir = "src\test\java\com\automation\tests"
$filesProcessed = 0
$filesUpdated = 0

Write-Host "Starting bulk migration to VariableManager..." -ForegroundColor Cyan
Write-Host ""

# Pattern 1: Replace LoginApiTest.bombToken references
Write-Host "Phase 1: Updating BOMB token references..." -ForegroundColor Yellow

$bombFiles = Get-ChildItem -Path $sourceDir -Recurse -Filter "*.java" | 
    Where-Object { (Get-Content $_.FullName -Raw) -match "LoginApiTest\.bombToken" }

foreach ($file in $bombFiles) {
    $filesProcessed++
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Replace import statement
    $content = $content -replace "import com\.automation\.tests\.bomb\.Login\.LoginApiTest;", "import com.automation.utils.VariableManager;"
    
    # Replace token check pattern in @BeforeClass
    $pattern1 = "if \(LoginApiTest\.bombToken != null\) \{\s+authToken = LoginApiTest\.bombToken;"
    $replacement1 = "authToken = VariableManager.getToken();`n        if (authToken == null || authToken.isEmpty()) {`n            throw new RuntimeException(""Login token not available. Please run LoginApiTest first."");`n        }"
    $content = $content -replace $pattern1, $replacement1
    
    # Handle the closing brace and else clause
    $content = $content -replace "logger\.info\(""Using BOMB token from LoginApiTest""\);\s+\} else \{\s+throw new RuntimeException\(""Login token not available\. Please run LoginApiTest first\.""\);\s+\}", "logger.info(""Using BOMB token from VariableManager"");"
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        $filesUpdated++
        Write-Host "  ✓ Updated: $($file.Name)" -ForegroundColor Green
    }
}

Write-Host "  BOMB token updates: $filesUpdated files" -ForegroundColor Cyan
Write-Host ""

# Pattern 2: Replace LoginTest.VariableManager.getBuyerAppToken() references  
Write-Host "Phase 2: Updating Buyer App token references..." -ForegroundColor Yellow

$buyerFiles = Get-ChildItem -Path $sourceDir -Recurse -Filter "*.java" |
    Where-Object { (Get-Content $_.FullName -Raw) -match "LoginTest\.VariableManager.getBuyerAppToken()|import static com\.automation\.tests\.buyerapp\.Login\.LoginTest\.VariableManager.getBuyerAppToken()" }

foreach ($file in $buyerFiles) {
    $filesProcessed++
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Replace static import
    $content = $content -replace "import static com\.automation\.tests\.buyerapp\.Login\.LoginTest\.VariableManager.getBuyerAppToken();", "import com.automation.utils.VariableManager;"
    
    # Replace direct token usage
    $content = $content -replace "VariableManager.getBuyerAppToken()", "VariableManager.getVariableManager.getBuyerAppToken()()"
    
    # Add import if not already present and we made changes
    if ($content -ne $originalContent -and $content -notmatch "import com\.automation\.utils\.VariableManager;") {
        # Find the last import statement and add VariableManager import after it
        $content = $content -replace "(import [^;]+;)(\s+@)", "`$1`nimport com.automation.utils.VariableManager;`$2"
    }
    
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        $filesUpdated++
        Write-Host "  ✓ Updated: $($file.Name)" -ForegroundColor Green
    }
}

Write-Host "  Buyer App token updates: $($filesUpdated - $bombFiles.Count) files" -ForegroundColor Cyan
Write-Host ""

# Summary
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host "Migration Complete!" -ForegroundColor Green
Write-Host "  Files processed: $filesProcessed" -ForegroundColor White
Write-Host "  Files updated: $filesUpdated" -ForegroundColor White
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next step: Run 'mvn clean compile' to verify" -ForegroundColor Yellow
