# Build and Run Tenant Service with Cloud Keycloak Configuration
param(
    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$CleanBuild = $true,
    
    [Parameter(Mandatory=$false)]
    [switch]$Background = $false
)

Write-Host "🏗️  BUILDING AND RUNNING TENANT SERVICE" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host "Skip Build: $SkipBuild" -ForegroundColor White
Write-Host "Clean Build: $CleanBuild" -ForegroundColor White
Write-Host "Background: $Background" -ForegroundColor White
Write-Host ""

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

Write-Host "✅ Environment variables configured:" -ForegroundColor Green
Write-Host "   KEYCLOAK_BASE_URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "   SERVER_PORT: $env:SERVER_PORT" -ForegroundColor White
Write-Host "   DATABASE: PostgreSQL localhost:5432" -ForegroundColor White
Write-Host ""

# Check prerequisites
Write-Host "[1/5] Checking prerequisites..." -ForegroundColor Yellow

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "✅ Java: $($javaVersion.Line)" -ForegroundColor Green
} catch {
    Write-Host "❌ Java not found. Please install Java 21+" -ForegroundColor Red
    exit 1
}

# Check PostgreSQL
try {
    $pgProcess = Get-Process postgres -ErrorAction SilentlyContinue
    if ($pgProcess) {
        Write-Host "✅ PostgreSQL: Running ($($pgProcess.Count) processes)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  PostgreSQL: Not detected as running" -ForegroundColor Yellow
        Write-Host "   Make sure PostgreSQL is running on localhost:5432" -ForegroundColor White
    }
} catch {
    Write-Host "⚠️  Could not check PostgreSQL status" -ForegroundColor Yellow
}

# Check if port 8083 is available
try {
    $portCheck = Test-NetConnection -ComputerName localhost -Port 8083 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($portCheck) {
        Write-Host "⚠️  Port 8083: Already in use (stopping existing service...)" -ForegroundColor Yellow
        # Try to find and stop existing Java processes on port 8083
        $netstat = netstat -ano | Select-String ":8083.*LISTENING"
        if ($netstat) {
            $pid = ($netstat.Line -split '\s+')[-1]
            try {
                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 2
                Write-Host "✅ Stopped existing service on port 8083" -ForegroundColor Green
            } catch {
                Write-Host "⚠️  Could not stop existing service" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "✅ Port 8083: Available" -ForegroundColor Green
    }
} catch {
    Write-Host "✅ Port 8083: Available" -ForegroundColor Green
}

# Build the project
if (-not $SkipBuild) {
    Write-Host "[2/5] Building tenant service..." -ForegroundColor Yellow
    
    if ($CleanBuild) {
        Write-Host "🧹 Cleaning previous build..." -ForegroundColor White
        try {
            & ./gradlew clean
            if ($LASTEXITCODE -ne 0) {
                throw "Clean failed"
            }
            Write-Host "✅ Clean completed" -ForegroundColor Green
        } catch {
            Write-Host "❌ Clean failed: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
    }
    
    Write-Host "🔨 Compiling Java sources..." -ForegroundColor White
    try {
        & ./gradlew :tenant-service:compileJava
        if ($LASTEXITCODE -ne 0) {
            throw "Compilation failed"
        }
        Write-Host "✅ Compilation successful" -ForegroundColor Green
    } catch {
        Write-Host "❌ Build failed: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "🔍 Troubleshooting:" -ForegroundColor Yellow
        Write-Host "   1. Check for compilation errors above" -ForegroundColor White
        Write-Host "   2. Ensure all dependencies are available" -ForegroundColor White
        Write-Host "   3. Try: ./gradlew clean build" -ForegroundColor White
        exit 1
    }
    
    Write-Host "📦 Building JAR..." -ForegroundColor White
    try {
        & ./gradlew :tenant-service:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed"
        }
        Write-Host "✅ Build completed successfully" -ForegroundColor Green
    } catch {
        Write-Host "❌ Build failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[2/5] Skipping build (as requested)" -ForegroundColor Gray
}

# Test database connection
Write-Host "[3/5] Testing database connection..." -ForegroundColor Yellow
try {
    # Simple TCP connection test to PostgreSQL port
    $tcpTest = Test-NetConnection -ComputerName "localhost" -Port 5432 -InformationLevel Quiet -WarningAction SilentlyContinue
    if ($tcpTest) {
        Write-Host "✅ Database connection: PostgreSQL port 5432 is reachable" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Database connection: PostgreSQL port 5432 not reachable" -ForegroundColor Yellow
        Write-Host "   Make sure PostgreSQL is running on localhost:5432" -ForegroundColor White
    }
} catch {
    Write-Host "⚠️  Database connection test failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "   Service will attempt to connect during startup" -ForegroundColor White
}

# Test Keycloak connectivity
Write-Host "[4/5] Testing Keycloak connectivity..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$env:KEYCLOAK_BASE_URL/realms/$env:KEYCLOAK_REALM" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
    Write-Host "✅ Keycloak connectivity: Successful" -ForegroundColor Green
    Write-Host "   Realm '$env:KEYCLOAK_REALM' is accessible" -ForegroundColor White
} catch {
    Write-Host "❌ Keycloak connectivity: Failed" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor White
    Write-Host "   Service will attempt to connect during startup" -ForegroundColor Yellow
}

# Run the service
Write-Host "[5/5] Starting tenant service..." -ForegroundColor Yellow
Write-Host ""

if ($Background) {
    Write-Host "🚀 Starting tenant service in background..." -ForegroundColor Green
    Write-Host "   Port: 8083" -ForegroundColor White
    Write-Host "   Logs: Check console for startup messages" -ForegroundColor White
    Write-Host ""
    
    # Start in background
    Start-Process powershell -ArgumentList "-Command", "cd '$PWD'; ./gradlew :tenant-service:bootRun" -WindowStyle Minimized
    
    # Wait and check if service started
    Write-Host "⏳ Waiting for service to start..." -ForegroundColor Yellow
    for ($i = 1; $i -le 30; $i++) {
        Start-Sleep -Seconds 2
        try {
            $health = Invoke-WebRequest -Uri "http://localhost:8083/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
            if ($health.StatusCode -eq 200) {
                Write-Host "✅ Service started successfully!" -ForegroundColor Green
                Write-Host "   Health check: http://localhost:8083/actuator/health" -ForegroundColor White
                Write-Host "   Swagger UI: http://localhost:8083/swagger-ui.html" -ForegroundColor White
                Write-Host "   API Base: http://localhost:8083/api" -ForegroundColor White
                return
            }
        } catch {
            # Still starting up
        }
        Write-Host "   Attempt $i/30..." -ForegroundColor Gray
    }
    
    Write-Host "⚠️  Service may still be starting up. Check manually:" -ForegroundColor Yellow
    Write-Host "   curl http://localhost:8083/actuator/health" -ForegroundColor White
    
} else {
    Write-Host "🚀 Starting tenant service in foreground..." -ForegroundColor Green
    Write-Host "   Port: 8083" -ForegroundColor White
    Write-Host "   Press Ctrl+C to stop" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "📊 Useful endpoints:" -ForegroundColor Cyan
    Write-Host "   Health: http://localhost:8083/actuator/health" -ForegroundColor White
    Write-Host "   Swagger: http://localhost:8083/swagger-ui.html" -ForegroundColor White
    Write-Host "   API: http://localhost:8083/api/tenants" -ForegroundColor White
    Write-Host ""
    Write-Host "Starting service..." -ForegroundColor Green
    Write-Host "===================" -ForegroundColor Green
    
    try {
        & ./gradlew :tenant-service:bootRun
    } catch {
        Write-Host ""
        Write-Host "❌ Service startup failed: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "🔍 Troubleshooting:" -ForegroundColor Yellow
        Write-Host "   1. Check logs above for specific errors" -ForegroundColor White
        Write-Host "   2. Verify PostgreSQL is running: Get-Process postgres" -ForegroundColor White
        Write-Host "   3. Check port availability: Test-NetConnection localhost -Port 8083" -ForegroundColor White
        Write-Host "   4. Verify environment variables are set correctly" -ForegroundColor White
        exit 1
    }
}

Write-Host ""
Write-Host "🎉 Tenant service is ready!" -ForegroundColor Green
