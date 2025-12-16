package com.kymatic.workflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "workflow")
public class WorkflowProperties {

    private TenantService tenantService = new TenantService();

    public TenantService getTenantService() {
        return tenantService;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    public static class TenantService {
        /**
         * Base URL for tenant-service. Should point to load-balanced/gateway endpoint.
         */
        private String baseUrl = "http://tenant-service:8080";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}

