package com.kymatic.tenantservice.config;

import com.kymatic.shared.multitenancy.TenantContext;
import com.kymatic.tenantservice.service.TenantDatabaseManager;
import com.kymatic.tenantservice.service.TenantDatabaseResolver;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routing DataSource that dynamically selects the tenant database based on TenantContext.
 * 
 * This datasource:
 * - Gets tenant ID from TenantContext (set by JwtTenantResolver filter)
 * - Resolves tenant ID to database name using TenantDatabaseResolver
 * - Creates datasources dynamically for each tenant database
 * - Routes all JPA queries to the tenant-specific database
 * 
 * This enables dynamic database routing where each tenant's queries go to their own database.
 */
public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger logger = LoggerFactory.getLogger(TenantRoutingDataSource.class);
    
    public static final String MASTER_DATABASE_KEY = "master";
    
    private final TenantDatabaseResolver tenantDatabaseResolver;
    private final TenantDatabaseManager tenantDatabaseManager;
    private final String masterDataSourceUrl;
    private final String username;
    private final String password;
    private final String driverClassName;
    
    // Cache of tenant datasources to avoid recreating them
    private final Map<String, DataSource> tenantDataSourceCache = new ConcurrentHashMap<>();
    
    private DataSource masterDataSource;

    public TenantRoutingDataSource(
            TenantDatabaseResolver tenantDatabaseResolver,
            TenantDatabaseManager tenantDatabaseManager,
            String masterDataSourceUrl,
            String username,
            String password,
            String driverClassName,
            DataSource masterDataSource) {
        this.tenantDatabaseResolver = tenantDatabaseResolver;
        this.tenantDatabaseManager = tenantDatabaseManager;
        this.masterDataSourceUrl = masterDataSourceUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.masterDataSource = masterDataSource;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String tenantId = TenantContext.getTenantId();
        
        // If no tenant ID in context, use master database (for tenant management operations)
        if (tenantId == null || tenantId.isBlank()) {
            logger.debug("No tenant ID in context, using master database");
            return MASTER_DATABASE_KEY;
        }

        try {
            // Resolve tenant ID to database name
            String databaseName = tenantDatabaseResolver.getCurrentTenantDatabaseName();
            logger.debug("Routing to tenant database: {} for tenant: {}", databaseName, tenantId);
            return databaseName;
        } catch (Exception e) {
            logger.error("Failed to resolve tenant database for tenant: {}", tenantId, e);
            // Fallback to master database on error
            return MASTER_DATABASE_KEY;
        }
    }

    @Override
    protected DataSource determineTargetDataSource() {
        Object lookupKey = determineCurrentLookupKey();
        
        if (MASTER_DATABASE_KEY.equals(lookupKey)) {
            return masterDataSource;
        }
        
        String databaseName = (String) lookupKey;
        
        // Get or create tenant datasource
        return tenantDataSourceCache.computeIfAbsent(databaseName, dbName -> {
            String tenantJdbcUrl = tenantDatabaseManager.buildTenantJdbcUrl(dbName);
            logger.info("Creating datasource for tenant database: {} (URL: {})", dbName, tenantJdbcUrl);
            return createTenantDataSource(tenantJdbcUrl, dbName);
        });
    }

    private DataSource createTenantDataSource(String jdbcUrl, String databaseKey) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        
        // Optimized settings for faster connections
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(5000);  // Reduced from 30000
        dataSource.setIdleTimeout(300000);       // Reduced from 600000
        dataSource.setMaxLifetime(900000);       // Reduced from 1800000
        dataSource.setKeepaliveTime(30000);
        dataSource.setInitializationFailTimeout(-1);
        
        // Pre-warm the connection pool
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setValidationTimeout(3000);
        
        logger.info("Created and initialized datasource for tenant database: {}", databaseKey);
        return dataSource;
    }
}

