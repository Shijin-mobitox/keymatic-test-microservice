# Script to run both tenant-service and workflow-service with cloud Keycloak configuration
param(
    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$Background = $true
)

Write-Host "STARTING BOTH SERVICES WITH CLOUD KEYCLOAK" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Set up environment variables for cloud Keycloak
Write-Host "[ENV] Setting up cloud Keycloak environment..." -ForegroundColor Yellow
$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic"
$env:KEYCLOAK_BASE_URL = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
$env:KEYCLOAK_SERVER_URL = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
$env:KEYCLOAK_ISSUER_URI = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic"
$env:KEYCLOAK_REALM = "kymatic"
$env:KEYCLOAK_ADMIN_USERNAME = "admin-gG7X0T1x"
$env:KEYCLOAK_ADMIN_PASSWORD = "blEzm8bnafcGnv50"
$env:KEYCLOAK_ORG_CLEANUP_ENABLED = "false"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "root"
$env:SERVER_PORT = "8083"
$env:TENANT_SERVICE_URL = "http://localhost:8083"

Write-Host "[SUCCESS] Environment configured:" -ForegroundColor Green
Write-Host "  KEYCLOAK_BASE_URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "  KEYCLOAK_SERVER_URL: $env:KEYCLOAK_SERVER_URL" -ForegroundColor White
Write-Host "  Tenant Service Port: 8083" -ForegroundColor White
Write-Host "  Workflow Service Port: 8090" -ForegroundColor White
Write-Host ""

# Stop existing services if running
Write-Host "[CHECK] Checking for existing services..." -ForegroundColor Yellow
try {
    $port8083 = netstat -ano | Select-String ":8083.*LISTENING"
    if ($port8083) {
        $pid8083 = ($port8083.Line -split '\s+')[-1]
        Stop-Process -Id $pid8083 -Force -ErrorAction SilentlyContinue
        Write-Host "[STOPPED] Service on port 8083" -ForegroundColor Yellow
    }
    
    $port8090 = netstat -ano | Select-String ":8090.*LISTENING"
    if ($port8090) {
        $pid8090 = ($port8090.Line -split '\s+')[-1]
        Stop-Process -Id $pid8090 -Force -ErrorAction SilentlyContinue
        Write-Host "[STOPPED] Service on port 8090" -ForegroundColor Yellow
    }
    
    if ($port8083 -or $port8090) {
        Start-Sleep -Seconds 2
    }
} catch {
    Write-Host "[INFO] No existing services found" -ForegroundColor Gray
}

# Build if requested
if (-not $SkipBuild) {
    Write-Host "[BUILD] Building services..." -ForegroundColor Yellow
    
    try {
        Write-Host "  Building tenant-service..." -ForegroundColor White
        & ./gradlew :tenant-service:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Tenant service build failed"
        }
        
        Write-Host "  Building workflow-service..." -ForegroundColor White
        & ./gradlew :workflow-service:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Workflow service build failed"
        }
        
        Write-Host "[SUCCESS] Build completed" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Build failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[SKIP] Skipping build as requested" -ForegroundColor Gray
}

Write-Host ""
Write-Host "[START] Starting services..." -ForegroundColor Yellow
Write-Host ""

# Start workflow-service first (tenant-service depends on it)
Write-Host "[1/2] Starting workflow-service on port 8090..." -ForegroundColor Cyan
$workflowProcess = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:KEYCLOAK_BASE_URL='$env:KEYCLOAK_BASE_URL'; `$env:KEYCLOAK_SERVER_URL='$env:KEYCLOAK_SERVER_URL'; `$env:KEYCLOAK_REALM='$env:KEYCLOAK_REALM'; `$env:KEYCLOAK_ADMIN_USERNAME='$env:KEYCLOAK_ADMIN_USERNAME'; `$env:KEYCLOAK_ADMIN_PASSWORD='$env:KEYCLOAK_ADMIN_PASSWORD'; `$env:SPRING_DATASOURCE_URL='$env:SPRING_DATASOURCE_URL'; `$env:SPRING_DATASOURCE_USERNAME='$env:SPRING_DATASOURCE_USERNAME'; `$env:SPRING_DATASOURCE_PASSWORD='$env:SPRING_DATASOURCE_PASSWORD'; `$env:TENANT_SERVICE_URL='$env:TENANT_SERVICE_URL'; ./gradlew :workflow-service:bootRun" -PassThru

Start-Sleep -Seconds 3

# Start tenant-service
Write-Host "[2/2] Starting tenant-service on port 8083..." -ForegroundColor Cyan
$tenantProcess = Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; `$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI='$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI'; `$env:KEYCLOAK_BASE_URL='$env:KEYCLOAK_BASE_URL'; `$env:KEYCLOAK_SERVER_URL='$env:KEYCLOAK_SERVER_URL'; `$env:KEYCLOAK_ISSUER_URI='$env:KEYCLOAK_ISSUER_URI'; `$env:KEYCLOAK_REALM='$env:KEYCLOAK_REALM'; `$env:KEYCLOAK_ADMIN_USERNAME='$env:KEYCLOAK_ADMIN_USERNAME'; `$env:KEYCLOAK_ADMIN_PASSWORD='$env:KEYCLOAK_ADMIN_PASSWORD'; `$env:KEYCLOAK_ORG_CLEANUP_ENABLED='$env:KEYCLOAK_ORG_CLEANUP_ENABLED'; `$env:SPRING_DATASOURCE_URL='$env:SPRING_DATASOURCE_URL'; `$env:SPRING_DATASOURCE_USERNAME='$env:SPRING_DATASOURCE_USERNAME'; `$env:SPRING_DATASOURCE_PASSWORD='$env:SPRING_DATASOURCE_PASSWORD'; `$env:SERVER_PORT='$env:SERVER_PORT'; ./gradlew :tenant-service:bootRun" -PassThru

Write-Host ""
Write-Host "[SUCCESS] Both services are starting!" -ForegroundColor Green
Write-Host ""
Write-Host "Services are running in separate windows:" -ForegroundColor Cyan
Write-Host "  - Workflow Service: http://localhost:8090" -ForegroundColor White
Write-Host "  - Tenant Service: http://localhost:8083" -ForegroundColor White
Write-Host ""
Write-Host "Waiting for services to start..." -ForegroundColor Yellow

# Wait for services to be ready
$maxAttempts = 60
$attempt = 0
$workflowReady = $false
$tenantReady = $false

while ($attempt -lt $maxAttempts -and (-not $workflowReady -or -not $tenantReady)) {
    Start-Sleep -Seconds 2
    $attempt++
    
    if (-not $workflowReady) {
        try {
            $health = Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($health.StatusCode -eq 200) {
                $workflowReady = $true
                Write-Host "[✓] Workflow service is ready!" -ForegroundColor Green
            }
        } catch {
            # Still starting
        }
    }
    
    if (-not $tenantReady) {
        try {
            $health = Invoke-WebRequest -Uri "http://localhost:8083/actuator/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
            if ($health.StatusCode -eq 200) {
                $tenantReady = $true
                Write-Host "[✓] Tenant service is ready!" -ForegroundColor Green
            }
        } catch {
            # Still starting
        }
    }
    
    if ($attempt % 5 -eq 0) {
        Write-Host "  Checking... (attempt $attempt/$maxAttempts)" -ForegroundColor Gray
    }
}

Write-Host ""
if ($workflowReady -and $tenantReady) {
    Write-Host "[SUCCESS] Both services are ready!" -ForegroundColor Green
} else {
    Write-Host "[INFO] Services are starting. Check the service windows for details." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Service Endpoints:" -ForegroundColor Cyan
Write-Host "  Workflow Service:" -ForegroundColor White
Write-Host "    Health: http://localhost:8090/actuator/health" -ForegroundColor Gray
Write-Host "    Camunda: http://localhost:8090/camunda" -ForegroundColor Gray
Write-Host ""
Write-Host "  Tenant Service:" -ForegroundColor White
Write-Host "    Health: http://localhost:8083/actuator/health" -ForegroundColor Gray
Write-Host "    Swagger: http://localhost:8083/swagger-ui.html" -ForegroundColor Gray
Write-Host "    API: http://localhost:8083/api/tenants" -ForegroundColor Gray
Write-Host ""
Write-Host "Keycloak URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor Cyan
Write-Host ""

