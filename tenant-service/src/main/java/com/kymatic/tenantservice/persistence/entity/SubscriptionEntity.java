package com.kymatic.tenantservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {

	@Id
	@Column(name = "subscription_id", nullable = false)
	private UUID subscriptionId;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@ManyToOne
	@JoinColumn(name = "tenant_id", insertable = false, updatable = false)
	private TenantEntity tenant;

	@Column(name = "plan_name", length = 100)
	private String planName;

	@Column(name = "billing_cycle", length = 50)
	private String billingCycle;

	@Column(name = "amount", precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", length = 3)
	private String currency;

	@Column(name = "status", length = 50)
	private String status;

	@Column(name = "current_period_start")
	private LocalDate currentPeriodStart;

	@Column(name = "current_period_end")
	private LocalDate currentPeriodEnd;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private OffsetDateTime createdAt;

	public SubscriptionEntity() {
		this.subscriptionId = UUID.randomUUID();
	}

	public UUID getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(UUID subscriptionId) {
		this.subscriptionId = subscriptionId;
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

	public String getPlanName() {
		return planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public String getBillingCycle() {
		return billingCycle;
	}

	public void setBillingCycle(String billingCycle) {
		this.billingCycle = billingCycle;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDate getCurrentPeriodStart() {
		return currentPeriodStart;
	}

	public void setCurrentPeriodStart(LocalDate currentPeriodStart) {
		this.currentPeriodStart = currentPeriodStart;
	}

	public LocalDate getCurrentPeriodEnd() {
		return currentPeriodEnd;
	}

	public void setCurrentPeriodEnd(LocalDate currentPeriodEnd) {
		this.currentPeriodEnd = currentPeriodEnd;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}
}

