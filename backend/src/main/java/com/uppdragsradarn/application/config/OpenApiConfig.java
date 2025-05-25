package com.uppdragsradarn.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
        .info(
            new Info()
                .title("UppdragsRadarn API")
                .description("Backend API for UppdragsRadarn platform")
                .version("v0.1.0")
                .contact(
                    new Contact()
                        .name("UppdragsRadarn Team")
                        .url("https://uppdragsradarn.se")
                        .email("info@uppdragsradarn.com"))
                .license(
                    new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        // Define servers
        .addServersItem(new Server().url("/").description("Current server"))
        .addServersItem(
            new Server().url("https://dev.uppdragsradarn.se").description("Development server"))
        // Add API tags for organization
        .addTagsItem(new Tag().name("Assignments").description("Assignment management operations"))
        .addTagsItem(new Tag().name("Auth").description("Authentication and session operations"))
        .addTagsItem(new Tag().name("Admin").description("Administrative operations"))
        .addTagsItem(
            new Tag().name("Crawler").description("Web crawler operations and configuration"))
        // Security setup
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
