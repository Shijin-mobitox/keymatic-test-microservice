package com.kymatic.tenantservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequest(
	@NotBlank(message = "Tenant ID is required")
	String tenantId,
	
	@NotBlank(message = "Plan name is required")
	String planName,
	
	String billingCycle, // monthly, annual
	
	@Positive(message = "Amount must be positive")
	BigDecimal amount,
	
	String currency, // USD, EUR, etc.
	
	String status, // active, cancelled, past_due
	
	LocalDate currentPeriodStart,
	
	LocalDate currentPeriodEnd
) {}

