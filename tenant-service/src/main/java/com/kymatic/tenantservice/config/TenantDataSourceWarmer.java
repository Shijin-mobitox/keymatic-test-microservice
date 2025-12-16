package com.kymatic.tenantservice.config;

import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import com.kymatic.tenantservice.persistence.repository.TenantRepository;
import com.kymatic.tenantservice.service.TenantDatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Pre-warms tenant database connections on application startup.
 * This eliminates the cold-start delay on first API call to tenant databases.
 */
@Component
public class TenantDataSourceWarmer {

    private static final Logger logger = LoggerFactory.getLogger(TenantDataSourceWarmer.class);

    private final TenantRepository tenantRepository;
    private final TenantDatabaseManager tenantDatabaseManager;
    private final String username;
    private final String password;

    public TenantDataSourceWarmer(
            TenantRepository tenantRepository,
            TenantDatabaseManager tenantDatabaseManager,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.username}") String username,
            @org.springframework.beans.factory.annotation.Value("${spring.datasource.password}") String password) {
        this.tenantRepository = tenantRepository;
        this.tenantDatabaseManager = tenantDatabaseManager;
        this.username = username;
        this.password = password;
    }

    /**
     * Pre-warm tenant database connections after application startup.
     * Runs asynchronously to not block application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpTenantConnections() {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting tenant database connection pre-warming...");
                List<TenantEntity> tenants = tenantRepository.findAll();
                
                int warmedUp = 0;
                for (TenantEntity tenant : tenants) {
                    try {
                        String jdbcUrl = tenantDatabaseManager.buildTenantJdbcUrl(tenant.getDatabaseName());
                        
                        // Test connection to warm up the pool
                        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                            conn.createStatement().execute("SELECT 1");
                            warmedUp++;
                            logger.debug("Pre-warmed connection for tenant: {} ({})", tenant.getSlug(), tenant.getDatabaseName());
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to pre-warm connection for tenant {}: {}", tenant.getSlug(), e.getMessage());
                    }
                }
                
                logger.info("Completed tenant database pre-warming. Successfully warmed {} out of {} tenant(s).", 
                    warmedUp, tenants.size());
            } catch (Exception e) {
                logger.error("Error during tenant database pre-warming", e);
            }
        });
    }
}

