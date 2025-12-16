package com.kymatic.tenantservice.multitenancy;

import com.kymatic.shared.multitenancy.TenantResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class HeaderTenantResolver implements TenantResolver {
    @Override
    public String resolveTenantId(Object requestContext) {
        if (requestContext instanceof HttpServletRequest req) {
            return req.getHeader("X-Tenant-ID");
        }
        return null;
    }
}
