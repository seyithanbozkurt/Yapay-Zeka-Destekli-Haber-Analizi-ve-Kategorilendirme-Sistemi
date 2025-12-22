package com.bitirme.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token ile yetkilendirme. Login endpoint'inden aldığınız token'ı girin (Bearer otomatik eklenir).");
        
        Components components = new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme);
        
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(SECURITY_SCHEME_NAME);
        
        return new OpenAPI()
                .info(new Info()
                        .title("Bitirme Projesi API")
                        .version("1.0.0")
                        .description("Haber Sınıflandırma Projesi REST API Dokümantasyonu. Tüm endpoint'ler için JWT token gereklidir. Önce /api/auth/login endpoint'inden token alın.")
                        .contact(new Contact()
                                .name("Bitirme Projesi")
                                .email("support@bitirme.com"))
                        .termsOfService(null)
                        .license(null))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8989")
                                .description("Local Development Server")))
                .components(components)
                .addSecurityItem(securityRequirement);
    }
}


