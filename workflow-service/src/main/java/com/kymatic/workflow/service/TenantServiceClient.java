package com.kymatic.workflow.service;

import com.kymatic.workflow.config.WorkflowProperties;
import com.kymatic.workflow.dto.TenantRequest;
import com.kymatic.workflow.dto.TenantResponse;
import com.kymatic.workflow.dto.TenantStatusUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class TenantServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(TenantServiceClient.class);

    private final RestTemplate restTemplate;
    private final WorkflowProperties properties;

    public TenantServiceClient(RestTemplate restTemplate, WorkflowProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public TenantResponse createTenant(TenantRequest request) {
        String url = properties.getTenantService().getBaseUrl() + "/api/tenants";
        logger.info("Calling tenant-service to create tenant at {}", url);
        return post(url, request, TenantResponse.class);
    }

    public void updateTenantStatus(UUID tenantId, TenantStatusUpdateRequest request) {
        String url = properties.getTenantService().getBaseUrl() + "/api/tenants/" + tenantId + "/status";
        logger.info("Calling tenant-service to update status for tenant {}", tenantId);
        post(url, request, Void.class);
    }

    private <T> T post(String url, Object body, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            return restTemplate.postForObject(url, entity, responseType);
        } catch (RestClientException ex) {
            logger.error("Call to tenant-service failed: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}

