package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import com.kymatic.tenantservice.persistence.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility service that resolves tenant identifiers provided by the frontend.
 *
 * The UI can only infer a tenant slug (e.g. "tenant1") from the URL or token,
 * but the master database relies on tenant UUIDs. This helper accepts either a
 * UUID or a slug and always returns the canonical tenant UUID that can be used
 * when talking to repositories.
 * 
 * PERFORMANCE: This service uses a simple in-memory cache to avoid repeated 
 * database lookups. We use a manual cache instead of @Cacheable to avoid 
 * circular dependency issues with the routing datasource.
 */
@Service
public class TenantIdentifierResolver {

	private final TenantRepository tenantRepository;
	
	// Manual cache to avoid circular dependency with Spring Cache + Routing DataSource
	private final Map<String, TenantEntity> tenantCache = new ConcurrentHashMap<>();

	public TenantIdentifierResolver(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
	}

    public UUID resolveTenantIdentifier(String tenantIdentifier) {
        return resolveTenantEntity(tenantIdentifier).getTenantId();
    }

    public TenantEntity resolveTenantEntity(String tenantIdentifier) {
        // Check cache first
        TenantEntity cached = tenantCache.get(tenantIdentifier);
        if (cached != null) {
            return cached;
        }
        
        // Not in cache, query database and cache result
        TenantEntity tenant = queryTenantEntity(tenantIdentifier);
        tenantCache.put(tenantIdentifier, tenant);
        return tenant;
    }
    
    private TenantEntity queryTenantEntity(String tenantIdentifier) {
        if (tenantIdentifier == null || tenantIdentifier.isBlank()) {
            throw new IllegalArgumentException("Tenant identifier is required");
        }

        try {
            UUID tenantId = UUID.fromString(tenantIdentifier);
            return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantIdentifier));
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, fall through to slug lookup
        }

        return tenantRepository.findBySlug(tenantIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantIdentifier));
    }
}


