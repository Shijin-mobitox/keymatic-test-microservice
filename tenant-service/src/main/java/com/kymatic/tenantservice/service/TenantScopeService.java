package com.kymatic.tenantservice.service;

import com.kymatic.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Component;

@Component
public class TenantScopeService {

    public String requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalStateException("Tenant context is not available");
        }
        return tenantId;
    }
}

