package com.kymatic.tenantservice.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantEntity {

	@Id
	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "tenant_name", nullable = false, length = 255)
	private String tenantName;

	@Column(name = "slug", nullable = false, unique = true, length = 100)
	private String slug;

	@Column(name = "status", nullable = false, length = 50)
	private String status;

	@Column(name = "subscription_tier", length = 50)
	private String subscriptionTier;

	@Column(name = "database_connection_string", nullable = false)
	private String databaseConnectionString;

	@Column(name = "database_name", nullable = false, length = 100)
	private String databaseName;

	@Column(name = "max_users")
	private Integer maxUsers;

	@Column(name = "max_storage_gb")
	private Integer maxStorageGb;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", columnDefinition = "jsonb")
	private JsonNode metadata;

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public String getSlug() {
		return slug;
	}

	public void setSlug(String slug) {
		this.slug = slug;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSubscriptionTier() {
		return subscriptionTier;
	}

	public void setSubscriptionTier(String subscriptionTier) {
		this.subscriptionTier = subscriptionTier;
	}

	public String getDatabaseConnectionString() {
		return databaseConnectionString;
	}

	public void setDatabaseConnectionString(String databaseConnectionString) {
		this.databaseConnectionString = databaseConnectionString;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public Integer getMaxUsers() {
		return maxUsers;
	}

	public void setMaxUsers(Integer maxUsers) {
		this.maxUsers = maxUsers;
	}

	public Integer getMaxStorageGb() {
		return maxStorageGb;
	}

	public void setMaxStorageGb(Integer maxStorageGb) {
		this.maxStorageGb = maxStorageGb;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public JsonNode getMetadata() {
		return metadata;
	}

	public void setMetadata(JsonNode metadata) {
		this.metadata = metadata;
	}

	@PrePersist
	public void onCreate() {
		if (tenantId == null) {
			tenantId = UUID.randomUUID();
		}
		if (status == null) {
			status = "active";
		}
	}

	@PreUpdate
	public void onUpdate() {
		if (status == null) {
			status = "active";
		}
	}
}


