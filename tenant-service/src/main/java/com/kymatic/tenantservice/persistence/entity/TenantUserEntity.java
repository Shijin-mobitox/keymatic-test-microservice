package com.kymatic.tenantservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_users")
public class TenantUserEntity {

	@Id
	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@ManyToOne
	@JoinColumn(name = "tenant_id", insertable = false, updatable = false)
	private TenantEntity tenant;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "role", nullable = false, length = 50)
	private String role;

	@Column(name = "is_active")
	private Boolean isActive;

	@Column(name = "last_login")
	private OffsetDateTime lastLogin;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	public TenantUserEntity() {
		this.userId = UUID.randomUUID();
		this.isActive = true;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UUID getTenantId() {
		return tenantId;
	}

	public void setTenantId(UUID tenantId) {
		this.tenantId = tenantId;
	}

	public TenantEntity getTenant() {
		return tenant;
	}

	public void setTenant(TenantEntity tenant) {
		this.tenant = tenant;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public OffsetDateTime getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(OffsetDateTime lastLogin) {
		this.lastLogin = lastLogin;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}

