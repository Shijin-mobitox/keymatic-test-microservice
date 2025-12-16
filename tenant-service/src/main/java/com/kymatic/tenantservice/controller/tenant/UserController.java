package com.kymatic.tenantservice.controller.tenant;

import com.kymatic.tenantservice.dto.tenant.UserRequest;
import com.kymatic.tenantservice.dto.tenant.UserResponse;
import com.kymatic.tenantservice.service.tenant.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "User Management", description = "Manage tenant-specific users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "Create a new user")
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
		UserResponse response = userService.createUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Get all users")
	@GetMapping
	public ResponseEntity<List<UserResponse>> getAllUsers() {
		List<UserResponse> users = userService.getAllUsers();
		return ResponseEntity.ok(users);
	}

	@Operation(summary = "Get user by ID")
	@GetMapping("/{userId}")
	public ResponseEntity<UserResponse> getUser(
		@Parameter(description = "User ID") @PathVariable UUID userId
	) {
		UserResponse response = userService.getUser(userId);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Get user by email")
	@GetMapping("/email")
	public ResponseEntity<UserResponse> getUserByEmail(
		@Parameter(description = "Email address") @RequestParam String email
	) {
		UserResponse response = userService.getUserByEmail(email);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Update user")
	@PutMapping("/{userId}")
	public ResponseEntity<UserResponse> updateUser(
		@Parameter(description = "User ID") @PathVariable UUID userId,
		@Valid @RequestBody UserRequest request
	) {
		UserResponse response = userService.updateUser(userId, request);
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Delete user")
	@DeleteMapping("/{userId}")
	@ApiResponse(responseCode = "204", description = "User deleted")
	public ResponseEntity<Void> deleteUser(
		@Parameter(description = "User ID") @PathVariable UUID userId
	) {
		userService.deleteUser(userId);
		return ResponseEntity.noContent().build();
	}
}

