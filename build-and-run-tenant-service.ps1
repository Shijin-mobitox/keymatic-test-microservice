# Build and Run Script for Tenant Service
# This script stops existing service, builds, and runs tenant-service with cloud Keycloak configuration

param(
    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipStop = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BUILD AND RUN TENANT SERVICE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Function to forcefully kill processes on a specific port
function Stop-ProcessOnPort {
    param([int]$Port)
    
    $killed = $false
    $attempts = 0
    $maxAttempts = 3
    
    # Retry loop to ensure process is killed
    while ($attempts -lt $maxAttempts) {
        $attempts++
        $foundProcess = $false
        
        # Method 1: Use Get-NetTCPConnection
        try {
            $connections = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
            if ($connections) {
                $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
                foreach ($pid in $pids) {
                    try {
                        $process = Get-Process -Id $pid -ErrorAction Stop
                        Write-Host "  Killing process (PID: $pid, Name: $($process.ProcessName)) on port ${Port}..." -ForegroundColor Yellow
                        Stop-Process -Id $pid -Force -ErrorAction Stop
                        Start-Sleep -Milliseconds 300
                        $killed = $true
                        $foundProcess = $true
                    } catch {
                        # Process might already be stopped
                    }
                }
            }
        } catch {
            # Get-NetTCPConnection might not be available or failed
        }
        
        # Method 2: Use netstat as fallback
        try {
            $netstat = netstat -ano | Select-String ":${Port}.*LISTENING"
            if ($netstat) {
                foreach ($line in $netstat) {
                    $pid = ($line.Line -split '\s+')[-1]
                    if ($pid -match '^\d+$') {
                        try {
                            $process = Get-Process -Id $pid -ErrorAction Stop
                            Write-Host "  Killing process (PID: $pid, Name: $($process.ProcessName)) on port ${Port}..." -ForegroundColor Yellow
                            Stop-Process -Id $pid -Force -ErrorAction Stop
                            Start-Sleep -Milliseconds 300
                            $killed = $true
                            $foundProcess = $true
                        } catch {
                            # Process might already be stopped
                        }
                    }
                }
            }
        } catch {
            # netstat might fail
        }
        
        # If no process found, break out of retry loop
        if (-not $foundProcess) {
            break
        }
        
        # Wait a bit before retry
        if ($attempts -lt $maxAttempts) {
            Start-Sleep -Milliseconds 500
        }
    }
    
    return $killed
}

# Kill processes on port 8083 BEFORE anything else
if (-not $SkipStop) {
    Write-Host "[KILL] Killing any processes on port 8083..." -ForegroundColor Yellow
    Write-Host "  This will be done multiple times to ensure port is free..." -ForegroundColor Gray
    
    # Kill multiple times to ensure processes are stopped
    for ($i = 1; $i -le 3; $i++) {
        $killed = Stop-ProcessOnPort -Port 8083
        if ($killed) {
            Write-Host "  Attempt ${i}: Killed processes, waiting..." -ForegroundColor Yellow
            Start-Sleep -Seconds 1
        }
    }
    
    # Final verification
    Write-Host "  Verifying port is free..." -ForegroundColor Gray
    $finalCheck = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
    
    if ($finalCheck) {
        Write-Host "  [WARN] Port still in use, force killing all Java processes on port 8083..." -ForegroundColor Yellow
        # Last resort: kill all Java processes using port 8083
        Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
            try {
                $conns = Get-NetTCPConnection -OwningProcess $_.Id -ErrorAction SilentlyContinue | Where-Object {$_.LocalPort -eq 8083 -and $_.State -eq "Listen"}
                if ($conns) {
                    Write-Host "    Force killing Java process $($_.Id)..." -ForegroundColor Yellow
                    Stop-Process -Id $_.Id -Force -ErrorAction Stop
                }
            } catch {}
        }
        Start-Sleep -Seconds 2
    }
    
    # Final check
    $finalCheck = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
    if (-not $finalCheck) {
        Write-Host "  [OK] Port 8083 is now free" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] Port 8083 still in use! Please manually kill processes." -ForegroundColor Red
        $pids = $finalCheck | Select-Object -ExpandProperty OwningProcess -Unique
        Write-Host "    PIDs: $($pids -join ', ')" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
} else {
    Write-Host "[SKIP] Skipping port kill (SkipStop flag set)" -ForegroundColor Gray
    Write-Host ""
}

# Set up environment variables for cloud Keycloak
Write-Host "[ENV] Configuring environment variables..." -ForegroundColor Yellow
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

Write-Host "[SUCCESS] Environment configured:" -ForegroundColor Green
Write-Host "  KEYCLOAK_BASE_URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "  SERVER_PORT: $env:SERVER_PORT" -ForegroundColor White
Write-Host ""

# Build service
if (-not $SkipBuild) {
    Write-Host "[BUILD] Building tenant-service..." -ForegroundColor Yellow
    try {
        & ./gradlew :tenant-service:clean :tenant-service:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Tenant service build failed with exit code $LASTEXITCODE"
        }
        Write-Host "  [OK] Tenant service build completed" -ForegroundColor Green
    } catch {
        Write-Host "  [ERROR] Tenant service build failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
} else {
    Write-Host "[SKIP] Skipping build (SkipBuild flag set)" -ForegroundColor Gray
    Write-Host ""
}

# Test Keycloak connectivity
Write-Host "[TEST] Testing Keycloak connectivity..." -ForegroundColor Yellow
try {
    $kcResponse = Invoke-WebRequest -Uri "$env:KEYCLOAK_BASE_URL/realms/$env:KEYCLOAK_REALM" -UseBasicParsing -TimeoutSec 10 -ErrorAction Stop
    Write-Host "  [OK] Keycloak is accessible" -ForegroundColor Green
} catch {
    Write-Host "  [WARN] Keycloak connectivity test failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "      Service will attempt to connect during startup" -ForegroundColor Gray
}
Write-Host ""

# Final kill before starting
Write-Host "[FINAL KILL] Ensuring port 8083 is free before starting..." -ForegroundColor Yellow
for ($k = 1; $k -le 5; $k++) {
    Stop-ProcessOnPort -Port 8083 | Out-Null
    Start-Sleep -Milliseconds 500
}
Start-Sleep -Seconds 2

# Aggressive double-check
$finalCheck = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
if ($finalCheck) {
    Write-Host "  [WARN] Port 8083 still in use, force killing ALL Java processes..." -ForegroundColor Yellow
    $pids = $finalCheck | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $pids) {
        try {
            $proc = Get-Process -Id $pid -ErrorAction Stop
            Write-Host "    Killing process $pid ($($proc.ProcessName))..." -ForegroundColor Yellow
            Stop-Process -Id $pid -Force -ErrorAction Stop
        } catch {
            Write-Host "    Could not kill process ${pid}: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    # Kill ALL Java processes using port 8083
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
        $conns = Get-NetTCPConnection -OwningProcess $_.Id -ErrorAction SilentlyContinue | Where-Object {$_.LocalPort -eq 8083 -and $_.State -eq "Listen"}
        $conns
    } | ForEach-Object {
        Write-Host "    Force killing Java process $($_.Id) on port 8083..." -ForegroundColor Yellow
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    }
    
    Start-Sleep -Seconds 3
    
    # Final verification
    $finalCheck2 = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
    if ($finalCheck2) {
        Write-Host "  [ERROR] Port 8083 STILL in use! PIDs: $($finalCheck2.OwningProcess -join ', ')" -ForegroundColor Red
        Write-Host "  Please manually kill these processes or run: .\kill-ports.ps1" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  [OK] Port 8083 is now free" -ForegroundColor Green
Write-Host ""

# Start service
Write-Host "[START] Starting tenant-service on port 8083..." -ForegroundColor Yellow
Write-Host ""

$tenantEnvString = "`$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI='$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI'; `$env:KEYCLOAK_BASE_URL='$env:KEYCLOAK_BASE_URL'; `$env:KEYCLOAK_SERVER_URL='$env:KEYCLOAK_SERVER_URL'; `$env:KEYCLOAK_ISSUER_URI='$env:KEYCLOAK_ISSUER_URI'; `$env:KEYCLOAK_REALM='$env:KEYCLOAK_REALM'; `$env:KEYCLOAK_ADMIN_USERNAME='$env:KEYCLOAK_ADMIN_USERNAME'; `$env:KEYCLOAK_ADMIN_PASSWORD='$env:KEYCLOAK_ADMIN_PASSWORD'; `$env:KEYCLOAK_ORG_CLEANUP_ENABLED='$env:KEYCLOAK_ORG_CLEANUP_ENABLED'; `$env:SPRING_DATASOURCE_URL='$env:SPRING_DATASOURCE_URL'; `$env:SPRING_DATASOURCE_USERNAME='$env:SPRING_DATASOURCE_USERNAME'; `$env:SPRING_DATASOURCE_PASSWORD='$env:SPRING_DATASOURCE_PASSWORD'; `$env:SERVER_PORT='$env:SERVER_PORT'"

$tenantCommand = "-NoExit", "-Command", "cd '$PWD'; $tenantEnvString; Write-Host 'Starting Tenant Service on port 8083...' -ForegroundColor Cyan; ./gradlew :tenant-service:bootRun"
Start-Process powershell -ArgumentList $tenantCommand

Write-Host "[SUCCESS] Tenant service is starting!" -ForegroundColor Green
Write-Host ""
Write-Host "Service Endpoints:" -ForegroundColor Cyan
Write-Host "  Health: http://localhost:8083/actuator/health" -ForegroundColor White
Write-Host "  Swagger: http://localhost:8083/swagger-ui.html" -ForegroundColor White
Write-Host "  API: http://localhost:8083/api/tenants" -ForegroundColor White
Write-Host ""
Write-Host "Keycloak Configuration:" -ForegroundColor Cyan
Write-Host "  URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "  Realm: $env:KEYCLOAK_REALM" -ForegroundColor White
Write-Host ""
Write-Host "Waiting for service to be ready..." -ForegroundColor Yellow

# Wait for service to be ready
$maxAttempts = 60
$attempt = 0
$serviceReady = $false

while ($attempt -lt $maxAttempts -and -not $serviceReady) {
    Start-Sleep -Seconds 2
    $attempt++
    
    try {
        $health = Invoke-WebRequest -Uri "http://localhost:8083/actuator/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($health.StatusCode -eq 200) {
            $serviceReady = $true
            Write-Host "  [OK] Tenant service is ready!" -ForegroundColor Green
        }
    } catch {
        # Still starting
    }
    
    if ($attempt % 10 -eq 0 -and ($attempt -gt 0)) {
        Write-Host "  Checking... (attempt $attempt/$maxAttempts)" -ForegroundColor Gray
    }
}

Write-Host ""
if ($serviceReady) {
    Write-Host "[SUCCESS] Tenant service is ready!" -ForegroundColor Green
} else {
    Write-Host "[INFO] Service is still starting. Check the service window for details." -ForegroundColor Yellow
}
Write-Host ""
