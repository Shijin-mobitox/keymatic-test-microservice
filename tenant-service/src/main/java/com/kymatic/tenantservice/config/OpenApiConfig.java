package com.kymatic.tenantservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI tenantServiceOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("KyMatic Tenant Service API")
				.description("API for tenant provisioning, routing, and multi-tenant management (master DB + per-tenant DB).")
				.version("v1")
				.contact(new Contact().name("KyMatic").url("https://example.com").email("support@example.com"))
				.license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")))
			.servers(List.of(
				new Server().url("http://localhost:8083").description("Local Development Server"),
				new Server().url("/").description("Default server")
			))
			.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
			.components(new Components()
				.addSecuritySchemes("bearer-jwt", new SecurityScheme()
					.type(SecurityScheme.Type.HTTP)
					.scheme("bearer")
					.bearerFormat("JWT")
					.description("JWT token from Keycloak. Include 'tenant_id' claim in the token.")))
			.externalDocs(new ExternalDocumentation()
				.description("Project Docs")
				.url("https://example.com/docs"));
	}
}


