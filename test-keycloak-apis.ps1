# Test Keycloak APIs using Newman (Postman CLI)
# This script runs automated tests against your Keycloak setup

param(
    [Parameter(Mandatory=$false)]
    [string]$Environment = "cloud",
    
    [Parameter(Mandatory=$false)]
    [string]$KeycloakUrl = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
)

Write-Host "🧪 TESTING KEYCLOAK APIS WITH NEWMAN" -ForegroundColor Cyan
Write-Host "====================================" -ForegroundColor Cyan
Write-Host "Environment: $Environment" -ForegroundColor White
Write-Host "Keycloak URL: $KeycloakUrl" -ForegroundColor White
Write-Host ""

# Check if Newman is installed
Write-Host "[1/4] Checking Newman installation..." -ForegroundColor Yellow
try {
    $newmanVersion = newman --version 2>$null
    if ($newmanVersion) {
        Write-Host "[SUCCESS] Newman is installed: $newmanVersion" -ForegroundColor Green
    } else {
        throw "Newman not found"
    }
} catch {
    Write-Host "[ERROR] Newman is not installed" -ForegroundColor Red
    Write-Host ""
    Write-Host "To install Newman:" -ForegroundColor Yellow
    Write-Host "  npm install -g newman" -ForegroundColor White
    Write-Host "  npm install -g newman-reporter-html" -ForegroundColor White
    Write-Host ""
    exit 1
}

# Check if collection files exist
Write-Host "[2/4] Checking collection files..." -ForegroundColor Yellow
$collectionFile = "KyMatic-Keycloak-APIs.postman_collection.json"
$environmentFile = "KyMatic-Keycloak.postman_environment.json"

if (!(Test-Path $collectionFile)) {
    Write-Host "[ERROR] Collection file not found: $collectionFile" -ForegroundColor Red
    exit 1
}

if (!(Test-Path $environmentFile)) {
    Write-Host "[ERROR] Environment file not found: $environmentFile" -ForegroundColor Red
    exit 1
}

Write-Host "[SUCCESS] Collection files found" -ForegroundColor Green

# Test Keycloak connectivity
Write-Host "[3/4] Testing Keycloak connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$KeycloakUrl/health" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
    Write-Host "[SUCCESS] Keycloak is accessible" -ForegroundColor Green
} catch {
    try {
        # Try alternative health endpoint
        $response = Invoke-WebRequest -Uri "$KeycloakUrl/realms/kymatic" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
        Write-Host "[SUCCESS] Keycloak is accessible (via realm endpoint)" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Could not verify Keycloak connectivity" -ForegroundColor Yellow
        Write-Host "Proceeding anyway..." -ForegroundColor White
    }
}

# Run Newman tests
Write-Host "[4/4] Running API tests with Newman..." -ForegroundColor Yellow
Write-Host ""

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportDir = "test-results"
$htmlReport = "$reportDir/keycloak-api-test-$timestamp.html"

# Create reports directory
if (!(Test-Path $reportDir)) {
    New-Item -ItemType Directory -Path $reportDir -Force | Out-Null
}

Write-Host "🚀 Starting Newman collection run..." -ForegroundColor Green
Write-Host "Report will be saved to: $htmlReport" -ForegroundColor White
Write-Host ""

try {
    # Run specific folders for core functionality
    $folders = @(
        "🔐 Authentication & Tokens",
        "👥 User Management", 
        "🏢 Organization Management (Keycloak 26+)",
        "🚀 Application Services"
    )
    
    foreach ($folder in $folders) {
        Write-Host "📁 Testing folder: $folder" -ForegroundColor Cyan
        
        & newman run $collectionFile `
            --environment $environmentFile `
            --folder "$folder" `
            --reporters cli,html `
            --reporter-html-export "$reportDir/test-$($folder.Replace(' ', '-').Replace('(', '').Replace(')', '').Replace('🔐', 'auth').Replace('👥', 'users').Replace('🏢', 'orgs').Replace('🚀', 'services'))-$timestamp.html" `
            --timeout-request 30000 `
            --delay-request 1000 `
            --bail `
            --color on
            
        if ($LASTEXITCODE -ne 0) {
            Write-Host "❌ Tests failed in folder: $folder" -ForegroundColor Red
        } else {
            Write-Host "✅ Tests passed in folder: $folder" -ForegroundColor Green
        }
        Write-Host ""
    }
    
    Write-Host "🎉 Newman test run completed!" -ForegroundColor Green
    
} catch {
    Write-Host "❌ Newman test run failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Summary
Write-Host ""
Write-Host "📊 TEST SUMMARY" -ForegroundColor Cyan
Write-Host "===============" -ForegroundColor Cyan
Write-Host "Collection: $collectionFile" -ForegroundColor White
Write-Host "Environment: $environmentFile" -ForegroundColor White
Write-Host "Reports: $reportDir/" -ForegroundColor White
Write-Host ""
Write-Host "📁 View detailed HTML reports in the $reportDir directory" -ForegroundColor Yellow
Write-Host ""

# Open report directory
if (Test-Path $reportDir) {
    Write-Host "Opening reports directory..." -ForegroundColor Green
    Start-Process explorer.exe -ArgumentList $reportDir
}
