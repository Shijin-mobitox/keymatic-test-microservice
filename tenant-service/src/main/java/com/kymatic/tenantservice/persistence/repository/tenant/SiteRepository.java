package com.kymatic.tenantservice.persistence.repository.tenant;

import com.kymatic.tenantservice.persistence.entity.tenant.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<SiteEntity, UUID> {
    Optional<SiteEntity> findByTenantIdAndSiteCode(String tenantId, String siteCode);
    List<SiteEntity> findByTenantIdAndDeletedAtIsNull(String tenantId);
    Optional<SiteEntity> findByTenantIdAndSiteId(String tenantId, UUID siteId);
}

