package com.kymatic.tenantservice.config;

import com.kymatic.tenantservice.service.TenantDatabaseManager;
import com.kymatic.tenantservice.service.TenantDatabaseResolver;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Configuration for dynamic tenant database routing.
 * 
 * This configuration:
 * - Creates a master datasource for Flyway migrations (non-primary)
 * - Creates a routing datasource that switches databases based on tenant
 * - Routes tenant-specific queries to tenant databases automatically
 * - Falls back to master database for tenant management operations
 */
@Configuration
public class TenantDataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(TenantDataSourceConfig.class);

    @Value("${spring.datasource.url}")
    private String masterDataSourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    /**
     * Master datasource for Flyway migrations and tenant management.
     * This is a non-primary datasource used by Flyway to run master database migrations.
     */
    @Bean(name = "masterDataSource")
    public DataSource masterDataSource() {
        return createMasterDataSource();
    }

    /**
     * Primary routing datasource that dynamically routes to tenant databases.
     * 
     * This datasource will:
     * - Use master database for tenant management operations (when no tenant ID in context)
     * - Automatically route to tenant-specific database when tenant ID is present
     * - Create tenant datasources on-demand and cache them
     * 
     * @DependsOn ensures Flyway runs first (using masterDataSource), then repositories are initialized,
     * then this routing datasource is created.
     * @Lazy breaks the circular dependency by deferring the creation of TenantDatabaseResolver
     * until it's actually needed.
     */
    @Bean
    @Primary
    public DataSource dataSource(
            @Lazy TenantDatabaseResolver tenantDatabaseResolver,
            TenantDatabaseManager tenantDatabaseManager) {
        
        // Create master datasource
        DataSource masterDataSource = masterDataSource();
        
        // Create routing datasource
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(
            tenantDatabaseResolver,
            tenantDatabaseManager,
            masterDataSourceUrl,
            username,
            password,
            driverClassName,
            masterDataSource
        );
        java.util.Map<Object, Object> targets = new java.util.concurrent.ConcurrentHashMap<>();
        targets.put(TenantRoutingDataSource.MASTER_DATABASE_KEY, masterDataSource);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targets);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
    
    /**
     * Creates the master datasource for tenant management operations.
     */
    private DataSource createMasterDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(masterDataSourceUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        // Add connection validation
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setValidationTimeout(5000);
        dataSource.setLeakDetectionThreshold(60000);
        
        // Test connection on startup
        try (var connection = dataSource.getConnection()) {
            logger.info("Successfully connected to master database: {}", masterDataSourceUrl);
        } catch (Exception e) {
            logger.error("Failed to connect to master database: {}. Please ensure PostgreSQL is running and accessible.", masterDataSourceUrl, e);
            throw new IllegalStateException("Cannot connect to database: " + e.getMessage(), e);
        }
        
        return dataSource;
    }
}

