# Standalone script to kill processes on ports 8083 and 8090
# Usage: .\kill-ports.ps1

param(
    [int[]]$Ports = @(8083, 8090)
)

Write-Host "Killing processes on ports: $($Ports -join ', ')" -ForegroundColor Yellow

function Kill-ProcessOnPort {
    param([int]$Port)
    
    $killed = $false
    
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
                    $killed = $true
                } catch {
                    Write-Host "  Could not kill process ${pid}: $($_.Exception.Message)" -ForegroundColor Red
                }
            }
        }
    } catch {
        Write-Host "  Get-NetTCPConnection failed: $($_.Exception.Message)" -ForegroundColor Red
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
                        $killed = $true
                    } catch {
                        Write-Host "  Could not kill process ${pid}: $($_.Exception.Message)" -ForegroundColor Red
                    }
                }
            }
        }
    } catch {
        Write-Host "  netstat failed: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    return $killed
}

foreach ($port in $Ports) {
    $killed = Kill-ProcessOnPort -Port $port
    if ($killed) {
        Write-Host "  [OK] Port ${port} processes killed" -ForegroundColor Green
    } else {
        Write-Host "  [OK] Port ${port} is already free" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Waiting 2 seconds for ports to be released..." -ForegroundColor Gray
Start-Sleep -Seconds 2

# Verify
Write-Host "Verifying ports are free..." -ForegroundColor Yellow
foreach ($port in $Ports) {
    $check = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Where-Object {$_.State -eq "Listen"}
    if ($check) {
        Write-Host "  [WARN] Port ${port} is still in use!" -ForegroundColor Red
    } else {
        Write-Host "  [OK] Port ${port} is free" -ForegroundColor Green
    }
}

