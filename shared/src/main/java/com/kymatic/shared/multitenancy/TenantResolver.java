package com.kymatic.shared.multitenancy;


public interface TenantResolver {
       String resolveTenantId(Object requestContext);
}
