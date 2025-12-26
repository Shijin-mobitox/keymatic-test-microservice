package com.kymatic.workflow.persistence.repository;

import com.kymatic.workflow.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

	Optional<TenantEntity> findBySlug(String slug);

	List<TenantEntity> findByStatus(String status);

	@Query("SELECT t FROM TenantEntity t WHERE t.tenantName ILIKE %:name%")
	List<TenantEntity> findByTenantNameContainingIgnoreCase(@Param("name") String name);

	@Query("SELECT COUNT(t) > 0 FROM TenantEntity t WHERE t.slug = :slug")
	boolean existsBySlug(@Param("slug") String slug);

	@Query("SELECT COUNT(t) > 0 FROM TenantEntity t WHERE t.databaseName = :databaseName")
	boolean existsByDatabaseName(@Param("databaseName") String databaseName);
}
