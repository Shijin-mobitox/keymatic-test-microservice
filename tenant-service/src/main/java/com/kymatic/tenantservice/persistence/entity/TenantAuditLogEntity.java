package com.kymatic.tenantservice.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_audit_log")
public class TenantAuditLogEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "log_id")
	private Long logId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@ManyToOne
	@JoinColumn(name = "tenant_id", insertable = false, updatable = false)
	private TenantEntity tenant;

	@Column(name = "action", nullable = false, length = 100)
	private String action;

	@Column(name = "performed_by")
	private UUID performedBy;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "details", columnDefinition = "jsonb")
	private JsonNode details;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	public Long getLogId() {
		return logId;
	}

	public void setLogId(Long logId) {
		this.logId = logId;
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

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public UUID getPerformedBy() {
		return performedBy;
	}

	public void setPerformedBy(UUID performedBy) {
		this.performedBy = performedBy;
	}

	public JsonNode getDetails() {
		return details;
	}

	public void setDetails(JsonNode details) {
		this.details = details;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}

