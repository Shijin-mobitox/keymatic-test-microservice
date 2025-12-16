package com.kymatic.tenantservice.service;

import com.kymatic.tenantservice.dto.TenantUserRequest;
import com.kymatic.tenantservice.dto.TenantUserResponse;
import com.kymatic.tenantservice.persistence.entity.TenantUserEntity;
import com.kymatic.tenantservice.persistence.repository.TenantUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TenantUserService {

	private final TenantUserRepository tenantUserRepository;
	private final TenantIdentifierResolver tenantIdentifierResolver;
	private final PasswordEncoder passwordEncoder;

	public TenantUserService(
		TenantUserRepository tenantUserRepository,
		TenantIdentifierResolver tenantIdentifierResolver,
		PasswordEncoder passwordEncoder
	) {
		this.tenantUserRepository = tenantUserRepository;
		this.tenantIdentifierResolver = tenantIdentifierResolver;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public TenantUserResponse createTenantUser(TenantUserRequest request) {
		UUID tenantId = tenantIdentifierResolver.resolveTenantIdentifier(request.tenantId());

		// Check if user already exists
		if (tenantUserRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
			throw new IllegalArgumentException("User with email already exists for this tenant: " + request.email());
		}

		TenantUserEntity entity = new TenantUserEntity();
		entity.setTenantId(tenantId);
		entity.setEmail(request.email());
		entity.setPasswordHash(passwordEncoder.encode(request.password()));
		entity.setRole(request.role());
		entity.setIsActive(request.isActive());

		TenantUserEntity saved = tenantUserRepository.save(entity);
		return toResponse(saved);
	}

	public List<TenantUserResponse> getUsersByTenant(String tenantIdentifier) {
		UUID tenantId = tenantIdentifierResolver.resolveTenantIdentifier(tenantIdentifier);
		return tenantUserRepository.findByTenantId(tenantId).stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public TenantUserResponse getTenantUser(UUID userId) {
		TenantUserEntity entity = tenantUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		return toResponse(entity);
	}

	public TenantUserResponse getTenantUserByEmail(String tenantIdentifier, String email) {
		UUID tenantId = tenantIdentifierResolver.resolveTenantIdentifier(tenantIdentifier);
		TenantUserEntity entity = tenantUserRepository.findByTenantIdAndEmail(tenantId, email)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
		return toResponse(entity);
	}

	@Transactional
	public TenantUserResponse updateTenantUser(UUID userId, TenantUserRequest request) {
		TenantUserEntity entity = tenantUserRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		if (request.email() != null && !request.email().equals(entity.getEmail())) {
			// Check if new email already exists
			if (tenantUserRepository.existsByTenantIdAndEmail(entity.getTenantId(), request.email())) {
				throw new IllegalArgumentException("User with email already exists: " + request.email());
			}
			entity.setEmail(request.email());
		}

		if (request.password() != null && !request.password().isEmpty()) {
			entity.setPasswordHash(passwordEncoder.encode(request.password()));
		}

		if (request.role() != null) {
			entity.setRole(request.role());
		}

		if (request.isActive() != null) {
			entity.setIsActive(request.isActive());
		}

		TenantUserEntity saved = tenantUserRepository.save(entity);
		return toResponse(saved);
	}

	@Transactional
	public void deleteTenantUser(UUID userId) {
		if (!tenantUserRepository.existsById(userId)) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
		tenantUserRepository.deleteById(userId);
	}

	private TenantUserResponse toResponse(TenantUserEntity entity) {
		return new TenantUserResponse(
			entity.getUserId(),
			entity.getTenantId(),
			entity.getEmail(),
			entity.getRole(),
			entity.getIsActive(),
			entity.getLastLogin(),
			entity.getCreatedAt()
		);
	}
}

