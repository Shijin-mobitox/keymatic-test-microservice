package com.kymatic.tenantservice.service.tenant;

import com.kymatic.shared.multitenancy.TenantContext;
import com.kymatic.tenantservice.dto.tenant.UserRequest;
import com.kymatic.tenantservice.dto.tenant.UserResponse;
import com.kymatic.tenantservice.persistence.entity.tenant.UserEntity;
import com.kymatic.tenantservice.persistence.repository.tenant.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public UserResponse createUser(UserRequest request) {
		String tenantId = TenantContext.getTenantId();
		if (tenantId == null) {
			throw new IllegalStateException("Tenant ID is required");
		}

		// Check if email already exists
		if (userRepository.findByEmail(request.email()).isPresent()) {
			throw new IllegalArgumentException("User with email already exists: " + request.email());
		}

	UserEntity entity = new UserEntity();
	entity.setTenantId(tenantId);
	entity.setEmail(request.email());
	entity.setFirstName(request.firstName());
	entity.setLastName(request.lastName());
	// Note: Role is managed through RBAC system (user_roles table), not as a user property
	// entity.setRole(request.role());
	entity.setIsActive(request.isActive());

		UserEntity saved = userRepository.save(entity);
		return toResponse(saved);
	}

	@org.springframework.cache.annotation.Cacheable(value = "users", key = "'all-' + T(com.kymatic.shared.multitenancy.TenantContext).getTenantId()")
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	public UserResponse getUser(UUID userId) {
		UserEntity entity = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		return toResponse(entity);
	}

	public UserResponse getUserByEmail(String email) {
		UserEntity entity = userRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
		return toResponse(entity);
	}

	@Transactional
	public UserResponse updateUser(UUID userId, UserRequest request) {
		UserEntity entity = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		if (request.email() != null && !request.email().equals(entity.getEmail())) {
			if (userRepository.findByEmail(request.email()).isPresent()) {
				throw new IllegalArgumentException("User with email already exists: " + request.email());
			}
			entity.setEmail(request.email());
		}

		if (request.firstName() != null) {
			entity.setFirstName(request.firstName());
		}
	if (request.lastName() != null) {
		entity.setLastName(request.lastName());
	}
	// Note: Role is managed through RBAC system (user_roles table), not as a user property
	// if (request.role() != null) {
	// 	entity.setRole(request.role());
	// }
	if (request.isActive() != null) {
		entity.setIsActive(request.isActive());
	}

		UserEntity saved = userRepository.save(entity);
		return toResponse(saved);
	}

	@Transactional
	public void deleteUser(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
		userRepository.deleteById(userId);
	}

	private UserResponse toResponse(UserEntity entity) {
		return new UserResponse(
			entity.getUserId(),
			entity.getEmail(),
			entity.getFirstName(),
			entity.getLastName(),
			null, // Role is managed through RBAC system, not stored in user entity
			entity.getIsActive(),
			entity.getCreatedAt(),
			entity.getUpdatedAt()
		);
	}
}

