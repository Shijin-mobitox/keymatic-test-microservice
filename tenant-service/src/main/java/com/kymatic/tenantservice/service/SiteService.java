package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.rbac.CreateSiteRequest;
import com.kymatic.tenantservice.dto.rbac.SiteResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.SiteEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final TenantScopeService tenantScopeService;

    public SiteService(SiteRepository siteRepository, TenantScopeService tenantScopeService) {
        this.siteRepository = siteRepository;
        this.tenantScopeService = tenantScopeService;
    }

    @Transactional
    public SiteResponse createSite(CreateSiteRequest request) {
        String tenantId = tenantScopeService.requireTenantId();
        siteRepository.findByTenantIdAndSiteCode(tenantId, request.siteCode())
            .ifPresent(site -> {
                throw new IllegalArgumentException("Site code already exists: " + request.siteCode());
            });

        SiteEntity entity = new SiteEntity();
        entity.setTenantId(tenantId);
        entity.setSiteName(request.siteName());
        entity.setSiteCode(request.siteCode());
        entity.setAddress(request.address());
        entity.setCity(request.city());
        entity.setState(request.state());
        entity.setCountry(request.country());
        entity.setPostalCode(request.postalCode());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setHeadquarters(Boolean.TRUE.equals(request.headquarters()));
        entity.setActive(true);

        return toResponse(siteRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<SiteResponse> listSites() {
        String tenantId = tenantScopeService.requireTenantId();
        return siteRepository.findByTenantIdAndDeletedAtIsNull(tenantId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SiteEntity getTenantSiteOrThrow(String tenantId, java.util.UUID siteId) {
        return siteRepository.findByTenantIdAndSiteId(tenantId, siteId)
            .orElseThrow(() -> new IllegalArgumentException("Site not found: " + siteId));
    }

    private SiteResponse toResponse(SiteEntity entity) {
        return new SiteResponse(
            entity.getSiteId(),
            entity.getSiteName(),
            entity.getSiteCode(),
            entity.getAddress(),
            entity.getCity(),
            entity.getState(),
            entity.getCountry(),
            entity.getPostalCode(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getHeadquarters(),
            entity.getActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}

