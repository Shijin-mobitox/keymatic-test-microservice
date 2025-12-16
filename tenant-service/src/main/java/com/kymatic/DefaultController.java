package com.kymatic;

import com.kymatic.shared.multitenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultController {
    private static final Logger logger = LoggerFactory.getLogger(DefaultController.class);
    
    public DefaultController() {
        logger.info("DefaultController initialized and registered!");
        System.out.println("DefaultController initialized");
    }

    @GetMapping("/")
    public String home() {
        logger.info("DefaultController: Root URL accessed");
        System.out.println("DefaultController: Root URL accessed");
        
        // Example: Check if tenant ID is present (optional for this endpoint)
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return "Tenant Service is running! (Tenant: " + tenantId + ")";
        }
        return "Tenant Service is running!";
    }

    @GetMapping("/test")
    public String test() {
        logger.info("DefaultController: Test URL accessed");
        System.out.println("DefaultController: Test URL accessed");
        return "Test endpoint is working!";
    }
    
    @GetMapping("/health")
    public String health() {
        logger.info("Health check endpoint accessed");
        return "OK";
    }
}