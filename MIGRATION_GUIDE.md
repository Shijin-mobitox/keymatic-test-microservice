# Database Migration Guide

This guide explains how to run database migrations for all microservices in KyMaticServiceV1.

## Overview

All microservices use **Flyway** for database migrations:
- **tenant-service**: Has initial migration (`V1__init.sql`)
- **api-service**: Has placeholder migration (`V1__init.sql`)
- **auth-service**: Has placeholder migration (`V1__init.sql`)

## Prerequisites

1. **Docker Desktop must be running**
2. **PostgreSQL database must be accessible** (via docker-compose or standalone)
3. **Gradle must be installed** (or use `gradlew` wrapper)

## Quick Start

### Option 1: Using the Migration Script (Recommended)

#### Windows (PowerShell)
```powershell
# Run all migrations
.\migrate-db.ps1

# Show migration info
.\migrate-db.ps1 -Info

# Repair Flyway schema history (if corrupted)
.\migrate-db.ps1 -Repair

# Clean database (WARNING: deletes all data)
.\migrate-db.ps1 -Clean
```

#### Linux/Mac (Bash)
```bash
# Make script executable (first time only)
chmod +x migrate-db.sh

# Run all migrations
./migrate-db.sh

# Show migration info
./migrate-db.sh info

# Repair Flyway schema history (if corrupted)
./migrate-db.sh repair

# Clean database (WARNING: deletes all data)
./migrate-db.sh clean
```

### Option 2: Manual Migration (Service by Service)

#### Start Database First
```bash
# Start only the database
docker-compose up -d postgres-db

# Wait a few seconds for database to be ready
```

#### Run Migrations for Each Service

**Tenant Service:**
```bash
cd tenant-service
.\gradlew flywayMigrate
cd ..
```

**API Service:**
```bash
cd api-service
.\gradlew flywayMigrate
cd ..
```

**Auth Service:**
```bash
cd auth-service
.\gradlew flywayMigrate
cd ..
```

### Option 3: Automatic Migration on Service Startup

Migrations run automatically when services start if:
- Flyway is enabled in `application.yml` (it is by default)
- Database is accessible
- Service connects to database

This happens when you run:
```bash
docker-compose up -d
```

## Migration Commands

### Check Migration Status
```bash
# For a specific service
cd tenant-service
.\gradlew flywayInfo

# Shows:
# - Current version
# - Pending migrations
# - Migration history
```

### Repair Flyway Schema History
If Flyway schema history is corrupted:
```bash
cd tenant-service
.\gradlew flywayRepair
```

### Clean Database (WARNING: Deletes All Data)
```bash
cd tenant-service
.\gradlew flywayClean
```

### Validate Migrations
```bash
cd tenant-service
.\gradlew flywayValidate
```

## Environment Variables

You can override database connection settings:

**Windows PowerShell:**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/postgres"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="root"
.\migrate-db.ps1
```

**Linux/Mac:**
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/postgres"
export SPRING_DATASOURCE_USERNAME="postgres"
export SPRING_DATASOURCE_PASSWORD="root"
./migrate-db.sh
```

## Migration Files Location

- **tenant-service**: `tenant-service/src/main/resources/db/migration/`
- **api-service**: `api-service/src/main/resources/db/migration/`
- **auth-service**: `auth-service/src/main/resources/db/migration/`

## Creating New Migrations

### Naming Convention
Flyway migrations must follow this naming pattern:
```
V{version}__{description}.sql
```

Examples:
- `V1__init.sql` - Initial migration
- `V2__add_user_table.sql` - Add user table
- `V3__add_indexes.sql` - Add indexes

### Steps to Create a New Migration

1. **Create the migration file** in the appropriate service's `db/migration` directory:
   ```bash
   # Example: Add a new migration to api-service
   # File: api-service/src/main/resources/db/migration/V2__add_products_table.sql
   ```

2. **Write your SQL**:
   ```sql
   CREATE TABLE IF NOT EXISTS products (
       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
       name VARCHAR(255) NOT NULL,
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   );
   ```

3. **Run the migration**:
   ```bash
   cd api-service
   .\gradlew flywayMigrate
   ```

## Troubleshooting

### Issue: Migration Fails with "Connection Refused"
**Solution:** Ensure PostgreSQL is running:
```bash
docker-compose up -d postgres-db
# Wait 5-10 seconds for database to start
```

### Issue: "FlywayException: Validate failed"
**Solution:** This means database schema doesn't match migration files. Options:
1. Repair: `.\gradlew flywayRepair`
2. Clean and re-migrate (WARNING: deletes data): `.\gradlew flywayClean flywayMigrate`

### Issue: "Migration checksum mismatch"
**Solution:** Repair Flyway schema history:
```bash
.\gradlew flywayRepair
```

### Issue: Multiple Services Using Same Database
**Note:** All services share the same PostgreSQL database (`postgres-db`). Flyway uses a schema history table (`flyway_schema_history`) per service, so migrations won't conflict.

However, if you need separate databases, you'll need to:
1. Update `docker-compose.yml` to create multiple databases
2. Update each service's `application.yml` to use different database names
3. Update Flyway configuration in `build.gradle`

## Verification

After running migrations, verify they were applied:

```bash
# Connect to database
docker-compose exec postgres-db psql -U postgres -d postgres

# Check Flyway schema history
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

# Check tables
\dt

# Exit
\q
```

## Best Practices

1. **Always backup** before running migrations in production
2. **Test migrations** in development first
3. **Use transactions** in migration scripts when possible
4. **Never modify** existing migration files (create new ones instead)
5. **Review migrations** before applying to production
6. **Run migrations** before deploying new service versions

## Related Files

- `docker-compose.yml` - Database configuration
- `migrate-db.ps1` - Windows migration script
- `migrate-db.sh` - Linux/Mac migration script
- Service `build.gradle` files - Flyway plugin configuration
- Service `application.yml` files - Flyway settings

