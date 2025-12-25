# Simple Build and Run Script for Tenant Service
param(
    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$Background = $false
)

Write-Host "BUILDING AND RUNNING TENANT SERVICE" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan

# Set up environment variables for cloud Keycloak
Write-Host "[ENV] Setting up cloud Keycloak environment..." -ForegroundColor Yellow
$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic"
$env:KEYCLOAK_BASE_URL = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
$env:KEYCLOAK_ISSUER_URI = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io/realms/kymatic"
$env:KEYCLOAK_REALM = "kymatic"
$env:KEYCLOAK_ADMIN_USERNAME = "admin-gG7X0T1x"
$env:KEYCLOAK_ADMIN_PASSWORD = "blEzm8bnafcGnv50"
$env:KEYCLOAK_ORG_CLEANUP_ENABLED = "false"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "root"
$env:SERVER_PORT = "8083"

Write-Host "[SUCCESS] Environment configured:" -ForegroundColor Green
Write-Host "  KEYCLOAK_BASE_URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "  SERVER_PORT: $env:SERVER_PORT" -ForegroundColor White
Write-Host ""

# Check if port 8083 is in use
Write-Host "[CHECK] Checking port 8083..." -ForegroundColor Yellow
try {
    $portCheck = Test-NetConnection -ComputerName localhost -Port 8083 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($portCheck) {
        Write-Host "[WARNING] Port 8083 is already in use" -ForegroundColor Yellow
        # Try to stop existing service
        $netstat = netstat -ano | Select-String ":8083.*LISTENING"
        if ($netstat) {
            $pid = ($netstat.Line -split '\s+')[-1]
            try {
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 2
                Write-Host "[SUCCESS] Stopped existing service on port 8083" -ForegroundColor Green
            } catch {
                Write-Host "[WARNING] Could not stop existing service" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "[SUCCESS] Port 8083 is available" -ForegroundColor Green
    }
} catch {
    Write-Host "[SUCCESS] Port 8083 appears available" -ForegroundColor Green
}

# Build if requested
if (-not $SkipBuild) {
    Write-Host "[BUILD] Building tenant service..." -ForegroundColor Yellow
    
    try {
        Write-Host "  Cleaning..." -ForegroundColor White
        & ./gradlew clean
        if ($LASTEXITCODE -ne 0) {
            throw "Clean failed"
        }
        
        Write-Host "  Compiling..." -ForegroundColor White
        & ./gradlew :tenant-service:compileJava
        if ($LASTEXITCODE -ne 0) {
            throw "Compilation failed"
        }
        
        Write-Host "  Building JAR..." -ForegroundColor White
        & ./gradlew :tenant-service:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed"
        }
        
        Write-Host "[SUCCESS] Build completed" -ForegroundColor Green
    } catch {
        Write-Host "[ERROR] Build failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[SKIP] Skipping build as requested" -ForegroundColor Gray
}

# Test connectivity
Write-Host "[TEST] Testing connectivity..." -ForegroundColor Yellow

# Check PostgreSQL
try {
    $pgTest = Test-NetConnection -ComputerName localhost -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($pgTest) {
        Write-Host "[SUCCESS] PostgreSQL port 5432 is reachable" -ForegroundColor Green
    } else {
        Write-Host "[WARNING] PostgreSQL port 5432 not reachable" -ForegroundColor Yellow
    }
} catch {
    Write-Host "[WARNING] Could not test PostgreSQL connectivity" -ForegroundColor Yellow
}

# Check Keycloak
try {
    $kcResponse = Invoke-WebRequest -Uri "$env:KEYCLOAK_BASE_URL/realms/$env:KEYCLOAK_REALM" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
    Write-Host "[SUCCESS] Keycloak realm '$env:KEYCLOAK_REALM' is accessible" -ForegroundColor Green
} catch {
    Write-Host "[WARNING] Keycloak connectivity test failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "          Service will attempt to connect during startup" -ForegroundColor White
}

# Run the service
Write-Host "[RUN] Starting tenant service..." -ForegroundColor Yellow
Write-Host ""

if ($Background) {
    Write-Host "Starting in background mode..." -ForegroundColor Green
    Write-Host "Port: 8083" -ForegroundColor White
    Write-Host ""
    
    Start-Process powershell -ArgumentList "-Command", "cd '$PWD'; ./gradlew :tenant-service:bootRun" -WindowStyle Minimized
    
    Write-Host "Waiting for service to start..." -ForegroundColor Yellow
    for ($i = 1; $i -le 30; $i++) {
        Start-Sleep -Seconds 2
        try {
            $health = Invoke-WebRequest -Uri "http://localhost:8083/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($health.StatusCode -eq 200) {
                Write-Host ""
                Write-Host "[SUCCESS] Service started successfully!" -ForegroundColor Green
                Write-Host ""
                Write-Host "Available endpoints:" -ForegroundColor Cyan
                Write-Host "  Health: http://localhost:8083/actuator/health" -ForegroundColor White
                Write-Host "  Swagger: http://localhost:8083/swagger-ui.html" -ForegroundColor White
                Write-Host "  API: http://localhost:8083/api/tenants" -ForegroundColor White
                Write-Host ""
                Write-Host "Service is ready for use!" -ForegroundColor Green
                return
            }
        } catch {
            # Still starting up
        }
        Write-Host "  Attempt $i/30..." -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "[WARNING] Service may still be starting up. Check manually:" -ForegroundColor Yellow
    Write-Host "  curl http://localhost:8083/actuator/health" -ForegroundColor White
    
} else {
    Write-Host "Starting in foreground mode..." -ForegroundColor Green
    Write-Host "Port: 8083 | Press Ctrl+C to stop" -ForegroundColor White
    Write-Host ""
    Write-Host "Available endpoints:" -ForegroundColor Cyan
    Write-Host "  Health: http://localhost:8083/actuator/health" -ForegroundColor White
    Write-Host "  Swagger: http://localhost:8083/swagger-ui.html" -ForegroundColor White
    Write-Host "  API: http://localhost:8083/api/tenants" -ForegroundColor White
    Write-Host ""
    Write-Host "Starting service..." -ForegroundColor Green
    Write-Host "==================" -ForegroundColor Green
    
    try {
        & ./gradlew :tenant-service:bootRun
    } catch {
        Write-Host ""
        Write-Host "[ERROR] Service startup failed" -ForegroundColor Red
        Write-Host ""
        Write-Host "Troubleshooting:" -ForegroundColor Yellow
        Write-Host "  1. Check logs above for errors" -ForegroundColor White
        Write-Host "  2. Verify PostgreSQL: Get-Process postgres" -ForegroundColor White
        Write-Host "  3. Check port: Test-NetConnection localhost -Port 8083" -ForegroundColor White
        Write-Host "  4. Verify Keycloak connectivity" -ForegroundColor White
        exit 1
    }
}
