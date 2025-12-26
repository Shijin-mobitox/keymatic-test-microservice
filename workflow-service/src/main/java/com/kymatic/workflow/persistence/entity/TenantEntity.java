package com.kymatic.workflow.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class TenantEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "tenant_id")
	private UUID tenantId;

	@Column(name = "tenant_name", nullable = false)
	private String tenantName;

	@Column(name = "slug", unique = true, nullable = false)
	private String slug;

	@Column(name = "status", nullable = false)
	private String status = "active";

	@Column(name = "subscription_tier")
	private String subscriptionTier;

	@Column(name = "database_connection_string", nullable = false, columnDefinition = "TEXT")
	private String databaseConnectionString;

	@Column(name = "database_name", nullable = false)
	private String databaseName;

	@Column(name = "max_users")
	private Integer maxUsers;

	@Column(name = "max_storage_gb")
	private Integer maxStorageGb;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", columnDefinition = "JSONB")
	private JsonNode metadata;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	// Constructors
	public TenantEntity() {
	}

	public TenantEntity(String tenantName, String slug, String subscriptionTier, 
					   String databaseConnectionString, String databaseName, 
					   Integer maxUsers, Integer maxStorageGb, JsonNode metadata) {
		this.tenantName = tenantName;
		this.slug = slug;
		this.subscriptionTier = subscriptionTier;
		this.databaseConnectionString = databaseConnectionString;
		this.databaseName = databaseName;
		this.maxUsers = maxUsers;
		this.maxStorageGb = maxStorageGb;
		this.metadata = metadata;
	}

	// Getters and Setters
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public JsonNode getMetadata() {
		return metadata;
	}

	public void setMetadata(JsonNode metadata) {
		this.metadata = metadata;
	}
}
