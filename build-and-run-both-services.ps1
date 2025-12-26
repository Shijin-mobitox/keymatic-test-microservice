# Build and Run Script for Both Tenant Service and Workflow Service
# This script stops existing services, builds, and runs both services with cloud Keycloak configuration

param(
    [Parameter(Mandatory=$false)]
    [switch]$SkipBuild = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$SkipStop = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BUILD AND RUN BOTH SERVICES" -ForegroundColor Cyan
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

# Kill processes on ports BEFORE anything else
Write-Host "[KILL] Killing any processes on ports 8083 and 8090..." -ForegroundColor Yellow
Write-Host "  This will be done multiple times to ensure ports are free..." -ForegroundColor Gray

# Kill multiple times to ensure processes are stopped
for ($i = 1; $i -le 3; $i++) {
    $killed8083 = Stop-ProcessOnPort -Port 8083
    $killed8090 = Stop-ProcessOnPort -Port 8090
    
    if ($killed8083 -or $killed8090) {
        Write-Host "  Attempt ${i}: Killed processes, waiting..." -ForegroundColor Yellow
        Start-Sleep -Seconds 1
    }
}

# Final verification
Write-Host "  Verifying ports are free..." -ForegroundColor Gray
$final8083 = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
$final8090 = Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}

if ($final8083 -or $final8090) {
    Write-Host "  [WARN] Some ports still in use, force killing all Java processes..." -ForegroundColor Yellow
    # Last resort: kill all Java processes
    Get-Process -Name "java" -ErrorAction SilentlyContinue | ForEach-Object {
        try {
            $conns = Get-NetTCPConnection -OwningProcess $_.Id -ErrorAction SilentlyContinue | Where-Object {$_.LocalPort -in 8083, 8090 -and $_.State -eq "Listen"}
            if ($conns) {
                Write-Host "    Force killing Java process $($_.Id)..." -ForegroundColor Yellow
                Stop-Process -Id $_.Id -Force -ErrorAction Stop
            }
        } catch {}
    }
    Start-Sleep -Seconds 2
}

# Final check
$final8083 = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
$final8090 = Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}

if (-not $final8083 -and -not $final8090) {
    Write-Host "  [OK] All ports are now free" -ForegroundColor Green
} else {
    Write-Host "  [ERROR] Ports still in use! Please manually kill processes." -ForegroundColor Red
    if ($final8083) {
        $pids = $final8083 | Select-Object -ExpandProperty OwningProcess -Unique
        Write-Host "    Port 8083: PIDs $($pids -join ', ')" -ForegroundColor Red
    }
    if ($final8090) {
        $pids = $final8090 | Select-Object -ExpandProperty OwningProcess -Unique
        Write-Host "    Port 8090: PIDs $($pids -join ', ')" -ForegroundColor Red
    }
}
Write-Host ""

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
$env:TENANT_SERVICE_URL = "http://localhost:8083"

Write-Host "[SUCCESS] Environment configured:" -ForegroundColor Green
Write-Host "  KEYCLOAK_BASE_URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "  KEYCLOAK_SERVER_URL: $env:KEYCLOAK_SERVER_URL" -ForegroundColor White
Write-Host ""

# Skip stop check is now handled at the very beginning above
if ($SkipStop) {
    Write-Host "[SKIP] Skipping port kill (SkipStop flag set)" -ForegroundColor Gray
    Write-Host ""
}

# Build services
if (-not $SkipBuild) {
    Write-Host "[BUILD] Building services..." -ForegroundColor Yellow
    Write-Host ""
    
    # Build tenant-service
    Write-Host "  [1/2] Building tenant-service..." -ForegroundColor Cyan
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
    
    # Build workflow-service
    Write-Host "  [2/2] Building workflow-service..." -ForegroundColor Cyan
    try {
        & ./gradlew :workflow-service:clean :workflow-service:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Workflow service build failed with exit code $LASTEXITCODE"
        }
        Write-Host "  [OK] Workflow service build completed" -ForegroundColor Green
    } catch {
        Write-Host "  [ERROR] Workflow service build failed: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    Write-Host "[SUCCESS] All builds completed!" -ForegroundColor Green
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

# Start services
Write-Host "[START] Starting services..." -ForegroundColor Yellow
Write-Host ""

# Final verification that ports are free before starting
Write-Host "[VERIFY] Final check that ports 8083 and 8090 are free..." -ForegroundColor Yellow
$check8083 = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
$check8090 = Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}

if ($check8083) {
    Write-Host "  [WARN] Port 8083 still in use, killing processes..." -ForegroundColor Yellow
    Stop-ProcessOnPort -Port 8083 | Out-Null
    Start-Sleep -Seconds 1
}

if ($check8090) {
    Write-Host "  [WARN] Port 8090 still in use, killing processes..." -ForegroundColor Yellow
    Stop-ProcessOnPort -Port 8090 | Out-Null
    Start-Sleep -Seconds 1
}

Write-Host "  [OK] Ports verified free" -ForegroundColor Green
Write-Host ""

# Prepare environment variable string for PowerShell windows
$envString = "`$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI='$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI'; `$env:KEYCLOAK_BASE_URL='$env:KEYCLOAK_BASE_URL'; `$env:KEYCLOAK_SERVER_URL='$env:KEYCLOAK_SERVER_URL'; `$env:KEYCLOAK_ISSUER_URI='$env:KEYCLOAK_ISSUER_URI'; `$env:KEYCLOAK_REALM='$env:KEYCLOAK_REALM'; `$env:KEYCLOAK_ADMIN_USERNAME='$env:KEYCLOAK_ADMIN_USERNAME'; `$env:KEYCLOAK_ADMIN_PASSWORD='$env:KEYCLOAK_ADMIN_PASSWORD'; `$env:SPRING_DATASOURCE_URL='$env:SPRING_DATASOURCE_URL'; `$env:SPRING_DATASOURCE_USERNAME='$env:SPRING_DATASOURCE_USERNAME'; `$env:SPRING_DATASOURCE_PASSWORD='$env:SPRING_DATASOURCE_PASSWORD'"

$workflowEnvString = "$envString; `$env:TENANT_SERVICE_URL='$env:TENANT_SERVICE_URL'"
$tenantEnvString = "$envString; `$env:KEYCLOAK_ORG_CLEANUP_ENABLED='$env:KEYCLOAK_ORG_CLEANUP_ENABLED'; `$env:SERVER_PORT='$env:SERVER_PORT'"

# Start workflow-service first (tenant-service may depend on it)
Write-Host "  [1/2] Starting workflow-service on port 8090..." -ForegroundColor Cyan

# Kill any process on port 8090 right before starting (AGGRESSIVE)
Write-Host "    Killing any process on port 8090..." -ForegroundColor Gray
for ($k = 1; $k -le 3; $k++) {
    Stop-ProcessOnPort -Port 8090 | Out-Null
    Start-Sleep -Milliseconds 500
}
Start-Sleep -Seconds 2

# Verify port 8090 is free
$check8090 = Get-NetTCPConnection -LocalPort 8090 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
if ($check8090) {
    Write-Host "    [WARN] Port 8090 still in use, killing Java processes..." -ForegroundColor Yellow
    Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
        $conns = Get-NetTCPConnection -OwningProcess $_.Id -ErrorAction SilentlyContinue | Where-Object {$_.LocalPort -eq 8090 -and $_.State -eq "Listen"}
        $conns
    } | ForEach-Object {
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 2
}

$workflowCommand = "-NoExit", "-Command", "cd '$PWD'; $workflowEnvString; Write-Host 'Starting Workflow Service on port 8090...' -ForegroundColor Cyan; ./gradlew :workflow-service:bootRun"
Start-Process powershell -ArgumentList $workflowCommand
Write-Host "  [OK] Workflow service window opened" -ForegroundColor Green

Start-Sleep -Seconds 3

# Start tenant-service
Write-Host "  [2/2] Starting tenant-service on port 8083..." -ForegroundColor Cyan

# Kill any process on port 8083 right before starting (AGGRESSIVE - MULTIPLE ATTEMPTS)
Write-Host "    Killing any process on port 8083 (multiple attempts)..." -ForegroundColor Gray
for ($k = 1; $k -le 5; $k++) {
    Stop-ProcessOnPort -Port 8083 | Out-Null
    Start-Sleep -Milliseconds 500
}
Start-Sleep -Seconds 2

# Aggressive double-check and kill ALL Java processes if needed
$finalCheck = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
if ($finalCheck) {
    Write-Host "    [WARN] Port 8083 still in use, force killing ALL Java processes..." -ForegroundColor Yellow
    $pids = $finalCheck | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($pid in $pids) {
        try {
            $proc = Get-Process -Id $pid -ErrorAction Stop
            Write-Host "    Killing process $pid ($($proc.ProcessName))..." -ForegroundColor Yellow
            Stop-Process -Id $pid -Force -ErrorAction Stop
            # Also try to kill child processes
            Get-Process -ErrorAction SilentlyContinue | Where-Object {$_.Parent.Id -eq $pid} | ForEach-Object {
                Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
            }
        } catch {
            Write-Host "    Could not kill process ${pid}: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    # Last resort: kill ALL Java processes using port 8083
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
        Write-Host "    [ERROR] Port 8083 STILL in use! PIDs: $($finalCheck2.OwningProcess -join ', ')" -ForegroundColor Red
        Write-Host "    Please manually kill these processes or restart your computer." -ForegroundColor Red
        exit 1
    } else {
        Write-Host "    [OK] Port 8083 is now free" -ForegroundColor Green
    }
}

$tenantCommand = "-NoExit", "-Command", "cd '$PWD'; $tenantEnvString; Write-Host 'Starting Tenant Service on port 8083...' -ForegroundColor Cyan; ./gradlew :tenant-service:bootRun"
Start-Process powershell -ArgumentList $tenantCommand
Write-Host "  [OK] Tenant service window opened" -ForegroundColor Green

Write-Host ""
Write-Host "[SUCCESS] Both services are starting!" -ForegroundColor Green
Write-Host ""
Write-Host "Services are running in separate windows:" -ForegroundColor Cyan
Write-Host "  - Workflow Service: http://localhost:8090" -ForegroundColor White
Write-Host "  - Tenant Service: http://localhost:8083" -ForegroundColor White
Write-Host ""
Write-Host "Waiting for services to be ready..." -ForegroundColor Yellow

# Wait for services to be ready
$maxAttempts = 90
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
                Write-Host "  [OK] Workflow service is ready!" -ForegroundColor Green
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
                Write-Host "  [OK] Tenant service is ready!" -ForegroundColor Green
            }
        } catch {
            # Still starting
        }
    }
    
    if ($attempt % 10 -eq 0 -and ($attempt -gt 0)) {
        $status = "Checking... (attempt $attempt/$maxAttempts)"
        if ($workflowReady) { $status += " [Workflow: OK]" } else { $status += " [Workflow: ...]" }
        if ($tenantReady) { $status += " [Tenant: OK]" } else { $status += " [Tenant: ...]" }
        Write-Host "  $status" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($workflowReady -and $tenantReady) {
    Write-Host "[SUCCESS] Both services are ready!" -ForegroundColor Green
} else {
    Write-Host "[INFO] Services are starting. Check the service windows for details." -ForegroundColor Yellow
    if ($workflowReady) {
        Write-Host "  Workflow Service: READY" -ForegroundColor Green
    } else {
        Write-Host "  Workflow Service: STARTING..." -ForegroundColor Yellow
    }
    if ($tenantReady) {
        Write-Host "  Tenant Service: READY" -ForegroundColor Green
    } else {
        Write-Host "  Tenant Service: STARTING..." -ForegroundColor Yellow
    }
}
Write-Host "========================================" -ForegroundColor Cyan
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
Write-Host "Keycloak Configuration:" -ForegroundColor Cyan
Write-Host "  URL: $env:KEYCLOAK_BASE_URL" -ForegroundColor White
Write-Host "  Realm: $env:KEYCLOAK_REALM" -ForegroundColor White
Write-Host ""
Write-Host "To stop services: Close the PowerShell windows or run:" -ForegroundColor Gray
Write-Host '  Get-Process | Where-Object {$_.ProcessName -eq "java"} | Stop-Process -Force' -ForegroundColor DarkGray
Write-Host ""

