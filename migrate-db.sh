#!/bin/bash
# Database Migration Script for KyMaticServiceV1
# This script runs Flyway migrations for all microservices

set -e

echo "========================================"
echo "KyMaticServiceV1 Database Migration"
echo "========================================"
echo ""

# Database connection details
DB_URL=${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/postgres}
DB_USER=${SPRING_DATASOURCE_USERNAME:-postgres}
DB_PASSWORD=${SPRING_DATASOURCE_PASSWORD:-root}

echo "Database: $DB_URL"
echo "User: $DB_USER"
echo ""

# Services to migrate
SERVICES=("tenant-service" "api-service" "auth-service")

run_flyway() {
    local service_name=$1
    local service_path=$2
    local command=${3:-migrate}
    
    echo "----------------------------------------"
    echo "Migrating: $service_name"
    echo "----------------------------------------"
    
    cd "$service_path"
    
    if [ "$command" = "info" ]; then
        ./gradlew flywayInfo
    elif [ "$command" = "repair" ]; then
        ./gradlew flywayRepair
    elif [ "$command" = "clean" ]; then
        echo "WARNING: This will clean the database!"
        read -p "Are you sure? (yes/no): " confirm
        if [ "$confirm" = "yes" ]; then
            ./gradlew flywayClean
        else
            echo "Skipped cleaning for $service_name"
        fi
    else
        ./gradlew flywayMigrate
    fi
    
    if [ $? -eq 0 ]; then
        echo "✓ $service_name migration completed successfully"
    else
        echo "✗ $service_name migration failed"
        return 1
    fi
    
    echo ""
    cd ..
}

# Main execution
if [ "$1" = "info" ]; then
    echo "Showing migration info for all services..."
    echo ""
    for service in "${SERVICES[@]}"; do
        run_flyway "$service" "$service" "info"
    done
elif [ "$1" = "repair" ]; then
    echo "Repairing Flyway schema history..."
    echo ""
    for service in "${SERVICES[@]}"; do
        run_flyway "$service" "$service" "repair"
    done
elif [ "$1" = "clean" ]; then
    echo "WARNING: This will clean all databases!"
    echo ""
    for service in "${SERVICES[@]}"; do
        run_flyway "$service" "$service" "clean"
    done
else
    echo "Running migrations for all services..."
    echo ""
    
    all_success=true
    for service in "${SERVICES[@]}"; do
        if ! run_flyway "$service" "$service" "migrate"; then
            all_success=false
        fi
    done
    
    echo "========================================"
    if [ "$all_success" = true ]; then
        echo "All migrations completed successfully!"
    else
        echo "Some migrations failed. Please check the errors above."
        exit 1
    fi
    echo "========================================"
fi

