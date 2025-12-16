package com.kymatic.tenantservice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SubscriptionResponse(
	UUID subscriptionId,
	UUID tenantId,
	String planName,
	String billingCycle,
	BigDecimal amount,
	String currency,
	String status,
	LocalDate currentPeriodStart,
	LocalDate currentPeriodEnd,
	OffsetDateTime createdAt
) {}

