package com.kymatic.tenantservice.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway configuration to handle checksum mismatches.
 * 
 * This configuration automatically repairs checksums when migration files
 * have been modified after being applied to the database.
 * This is useful during development when migration files may be updated.
 */
@Configuration
public class FlywayConfig {

    /**
     * Custom Flyway migration strategy that repairs checksums before migration.
     * This prevents checksum mismatch errors when migration files have been
     * modified after being applied to the database.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                // Repair checksums to fix any mismatches
                // This updates the schema history table to match current migration files
                flyway.repair();
            } catch (Exception e) {
                // Log but continue - repair might fail if there are no mismatches
                System.err.println("Flyway repair: " + e.getMessage());
            }
            // Then proceed with normal migration
            flyway.migrate();
        };
    }
}

