package com.kymatic.tenantservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_migrations")
public class TenantMigrationEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "migration_id")
	private Long migrationId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "version", nullable = false)
	private String version;

	@Column(name = "applied_at")
	private OffsetDateTime appliedAt;

	@Column(name = "status")
	private String status;

	public Long getMigrationId() {
		return migrationId;
	}

	public void setMigrationId(Long migrationId) {
		this.migrationId = migrationId;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public OffsetDateTime getAppliedAt() {
		return appliedAt;
	}

	public void setAppliedAt(OffsetDateTime appliedAt) {
		this.appliedAt = appliedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}


