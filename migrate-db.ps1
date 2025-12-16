# Database Migration Script for KyMaticServiceV1
# This script runs Flyway migrations for all microservices

param(
    [switch]$Info,
    [switch]$Repair,
    [switch]$Clean
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "KyMaticServiceV1 Database Migration" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Database connection details
$DB_URL = $env:SPRING_DATASOURCE_URL
if (-not $DB_URL) {
    $DB_URL = "jdbc:postgresql://localhost:5432/postgres"
}
$DB_USER = $env:SPRING_DATASOURCE_USERNAME
if (-not $DB_USER) {
    $DB_USER = "postgres"
}
$DB_PASSWORD = $env:SPRING_DATASOURCE_PASSWORD
if (-not $DB_PASSWORD) {
    $DB_PASSWORD = "root"
}

Write-Host "Database: $DB_URL" -ForegroundColor Yellow
Write-Host "User: $DB_USER" -ForegroundColor Yellow
Write-Host ""

# Services to migrate (using Gradle project names)
$services = @(
    @{Name="tenant-service"; Path="tenant-service"},
    @{Name="api-service"; Path="api-service"},
    @{Name="auth-service"; Path="auth-service"}
)

function Run-FlywayCommand {
    param(
        [string]$ServiceName,
        [string]$ServicePath,
        [string]$Command
    )
    
    Write-Host "----------------------------------------" -ForegroundColor Green
    Write-Host "Migrating: $ServiceName" -ForegroundColor Green
    Write-Host "----------------------------------------" -ForegroundColor Green
    
    $gradlew = if (Test-Path "gradlew.bat") { ".\gradlew.bat" } else { ".\gradlew" }
    $taskPrefix = ":$ServiceName"
    
    try {
        if ($Command -eq "info") {
            & $gradlew "$taskPrefix:flywayInfo"
        }
        elseif ($Command -eq "repair") {
            & $gradlew "$taskPrefix:flywayRepair"
        }
        elseif ($Command -eq "clean") {
            Write-Host "WARNING: This will clean the database!" -ForegroundColor Red
            $confirm = Read-Host "Are you sure? (yes/no)"
            if ($confirm -eq "yes") {
                & $gradlew "$taskPrefix:flywayClean"
            } else {
                Write-Host "Skipped cleaning for $ServiceName" -ForegroundColor Yellow
            }
        }
        else {
            & $gradlew "$taskPrefix:flywayMigrate"
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "[OK] $ServiceName migration completed successfully" -ForegroundColor Green
        } else {
            Write-Host "[FAILED] $ServiceName migration failed" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "[ERROR] Error migrating $ServiceName : $_" -ForegroundColor Red
        return $false
    }
    
    Write-Host ""
    return $true
}

# Main execution
if ($Info) {
    Write-Host "Showing migration info for all services..." -ForegroundColor Cyan
    Write-Host ""
    foreach ($service in $services) {
        Run-FlywayCommand -ServiceName $service.Name -ServicePath $service.Path -Command "info"
    }
}
elseif ($Repair) {
    Write-Host "Repairing Flyway schema history..." -ForegroundColor Cyan
    Write-Host ""
    foreach ($service in $services) {
        Run-FlywayCommand -ServiceName $service.Name -ServicePath $service.Path -Command "repair"
    }
}
elseif ($Clean) {
    Write-Host "WARNING: This will clean all databases!" -ForegroundColor Red
    Write-Host ""
    foreach ($service in $services) {
        Run-FlywayCommand -ServiceName $service.Name -ServicePath $service.Path -Command "clean"
    }
}
else {
    Write-Host "Running migrations for all services..." -ForegroundColor Cyan
    Write-Host ""
    
    $allSuccess = $true
    foreach ($service in $services) {
        $success = Run-FlywayCommand -ServiceName $service.Name -ServicePath $service.Path -Command "migrate"
        if (-not $success) {
            $allSuccess = $false
        }
    }
    
    Write-Host "========================================" -ForegroundColor Cyan
    if ($allSuccess) {
        Write-Host "All migrations completed successfully!" -ForegroundColor Green
    } else {
        Write-Host "Some migrations failed. Please check the errors above." -ForegroundColor Red
        exit 1
    }
    Write-Host "========================================" -ForegroundColor Cyan
}

