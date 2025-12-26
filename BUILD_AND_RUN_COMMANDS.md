# Build and Run Commands for Services

This document provides commands and scripts to build and run both tenant-service and workflow-service.

## Quick Start

### Using Individual Service Scripts (Recommended)

```powershell
# Build and run tenant-service only
.\build-and-run-tenant-service.ps1

# Build and run workflow-service only
.\build-and-run-workflow-service.ps1

# Skip build if already built
.\build-and-run-tenant-service.ps1 -SkipBuild
.\build-and-run-workflow-service.ps1 -SkipBuild

# Skip stopping existing services
.\build-and-run-tenant-service.ps1 -SkipStop
.\build-and-run-workflow-service.ps1 -SkipStop
```

### Using the All-in-One Script

```powershell
# Build and run both services (stops existing services first)
.\build-and-run-both-services.ps1

# Skip build if already built
.\build-and-run-both-services.ps1 -SkipBuild

# Skip stopping existing services
.\build-and-run-both-services.ps1 -SkipStop
```

## Individual Service Commands

### Tenant Service

#### Build Only
```powershell
# Clean and build tenant-service
.\gradlew :tenant-service:clean :tenant-service:build -x test
```

#### Run Only (with environment variables)
```powershell
# Set environment variables
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

# Run tenant-service
.\gradlew :tenant-service:bootRun
```

#### Using the Tenant Service Script
```powershell
# Build and run
.\run-tenant-service-simple.ps1

# Skip build
.\run-tenant-service-simple.ps1 -SkipBuild

# Run in background
.\run-tenant-service-simple.ps1 -Background
```

### Workflow Service

#### Build Only
```powershell
# Clean and build workflow-service
.\gradlew :workflow-service:clean :workflow-service:build -x test
```

#### Run Only (with environment variables)
```powershell
# Set environment variables
$env:KEYCLOAK_BASE_URL = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
$env:KEYCLOAK_SERVER_URL = "https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io"
$env:KEYCLOAK_REALM = "kymatic"
$env:KEYCLOAK_ADMIN_USERNAME = "admin-gG7X0T1x"
$env:KEYCLOAK_ADMIN_PASSWORD = "blEzm8bnafcGnv50"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/postgres"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "root"
$env:TENANT_SERVICE_URL = "http://localhost:8083"

# Run workflow-service
.\gradlew :workflow-service:bootRun
```

## Stop Services

### Stop by Port
```powershell
# Stop tenant-service (port 8083)
$port8083 = netstat -ano | Select-String ":8083.*LISTENING"
if ($port8083) {
    $pid = ($port8083.Line -split '\s+')[-1]
    Stop-Process -Id $pid -Force
}

# Stop workflow-service (port 8090)
$port8090 = netstat -ano | Select-String ":8090.*LISTENING"
if ($port8090) {
    $pid = ($port8090.Line -split '\s+')[-1]
    Stop-Process -Id $pid -Force
}
```

### Stop All Java Processes
```powershell
# Stop all Java processes (WARNING: This stops ALL Java processes)
Get-Process | Where-Object {$_.ProcessName -eq 'java'} | Stop-Process -Force

# Stop Java processes on specific ports
$processes = Get-NetTCPConnection -LocalPort 8083,8090 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
$processes | ForEach-Object { Stop-Process -Id $_ -Force }
```

## Build All Services

```powershell
# Build all services
.\gradlew clean build -x test

# Build specific service
.\gradlew :tenant-service:clean :tenant-service:build -x test
.\gradlew :workflow-service:clean :workflow-service:build -x test
```

## Check Service Status

```powershell
# Check if ports are in use
netstat -ano | Select-String ":8083.*LISTENING"
netstat -ano | Select-String ":8090.*LISTENING"

# Check service health
Invoke-WebRequest -Uri "http://localhost:8083/actuator/health" -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:8090/actuator/health" -UseBasicParsing
```

## Using Environment File

You can also load environment variables from the cloud config file:

```powershell
# Load environment variables from file (PowerShell 7+)
Get-Content keycloak-config-cloud.env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        $name = $matches[1]
        $value = $matches[2]
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}
```

## Service Endpoints

### Tenant Service (Port 8083)
- Health: http://localhost:8083/actuator/health
- Swagger UI: http://localhost:8083/swagger-ui.html
- API Docs: http://localhost:8083/v3/api-docs
- API: http://localhost:8083/api/tenants

### Workflow Service (Port 8090)
- Health: http://localhost:8090/actuator/health
- Camunda: http://localhost:8090/camunda

## Keycloak Configuration

Both services are configured to use the cloud Keycloak instance:
- **URL**: `https://f12e2153-e40c-4615-b9c9-3a4e6f5fd782.us.skycloak.io`
- **Realm**: `kymatic`

The configuration is controlled via environment variables:
- `KEYCLOAK_BASE_URL` - Primary Keycloak URL (used by both services)
- `KEYCLOAK_SERVER_URL` - Alternative Keycloak URL (fallback for workflow-service)
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI` - JWT issuer URI for tenant-service

