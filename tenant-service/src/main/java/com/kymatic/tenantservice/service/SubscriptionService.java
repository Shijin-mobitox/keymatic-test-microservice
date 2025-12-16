package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.SubscriptionRequest;
import com.kymatic.tenantservice.dto.SubscriptionResponse;
import com.kymatic.tenantservice.persistence.entity.SubscriptionEntity;
import com.kymatic.tenantservice.persistence.entity.TenantEntity;
import com.kymatic.tenantservice.persistence.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubscriptionService {

	private final SubscriptionRepository subscriptionRepository;
	private final TenantIdentifierResolver tenantIdentifierResolver;

	public SubscriptionService(
		SubscriptionRepository subscriptionRepository,
		TenantIdentifierResolver tenantIdentifierResolver
	) {
		this.subscriptionRepository = subscriptionRepository;
		this.tenantIdentifierResolver = tenantIdentifierResolver;
	}

	@Transactional
	public SubscriptionResponse createSubscription(SubscriptionRequest request) {
		// Verify tenant exists and resolve identifier
		TenantEntity tenant = tenantIdentifierResolver.resolveTenantEntity(request.tenantId());
		UUID tenantId = tenant.getTenantId();

		SubscriptionEntity entity = new SubscriptionEntity();
		entity.setTenantId(tenantId);
		entity.setPlanName(request.planName());
		entity.setBillingCycle(request.billingCycle());
		entity.setAmount(request.amount());
		entity.setCurrency(request.currency() != null ? request.currency() : "USD");
		entity.setStatus(request.status() != null ? request.status() : "active");
		entity.setCurrentPeriodStart(request.currentPeriodStart());
		entity.setCurrentPeriodEnd(request.currentPeriodEnd());

		SubscriptionEntity saved = subscriptionRepository.save(entity);
		return toResponse(saved);
	}

	public List<SubscriptionResponse> getSubscriptionsByTenant(String tenantIdentifier) {
		UUID tenantId = tenantIdentifierResolver.resolveTenantIdentifier(tenantIdentifier);
		return subscriptionRepository.findByTenantId(tenantId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public SubscriptionResponse getSubscription(UUID subscriptionId) {
		SubscriptionEntity entity = subscriptionRepository.findById(subscriptionId)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));
		return toResponse(entity);
	}

	@Transactional
	public SubscriptionResponse updateSubscription(UUID subscriptionId, SubscriptionRequest request) {
		SubscriptionEntity entity = subscriptionRepository.findById(subscriptionId)
			.orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subscriptionId));

		entity.setPlanName(request.planName());
		entity.setBillingCycle(request.billingCycle());
		entity.setAmount(request.amount());
		entity.setCurrency(request.currency());
		entity.setStatus(request.status());
		entity.setCurrentPeriodStart(request.currentPeriodStart());
		entity.setCurrentPeriodEnd(request.currentPeriodEnd());

		SubscriptionEntity saved = subscriptionRepository.save(entity);
		return toResponse(saved);
	}

	@Transactional
	public void deleteSubscription(UUID subscriptionId) {
		if (!subscriptionRepository.existsById(subscriptionId)) {
			throw new IllegalArgumentException("Subscription not found: " + subscriptionId);
		}
		subscriptionRepository.deleteById(subscriptionId);
	}

	private SubscriptionResponse toResponse(SubscriptionEntity entity) {
		return new SubscriptionResponse(
			entity.getSubscriptionId(),
			entity.getTenantId(),
			entity.getPlanName(),
			entity.getBillingCycle(),
			entity.getAmount(),
			entity.getCurrency(),
			entity.getStatus(),
			entity.getCurrentPeriodStart(),
			entity.getCurrentPeriodEnd(),
			entity.getCreatedAt()
		);
	}
}

