package com.kymatic.tenantservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caching configuration to improve performance.
 * 
 * Caches tenant lookups to avoid repeated database queries on every API request.
 * This significantly reduces response times for tenant-aware endpoints.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Simple in-memory cache for tenant data and query results
        // In production, consider using Redis or Caffeine for distributed caching
        return new ConcurrentMapCacheManager(
            "tenantIds", 
            "tenantEntities",
            "users",
            "projects",
            "tasks",
            "sites",
            "roles"
        );
    }
}

