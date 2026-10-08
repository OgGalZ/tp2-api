package fr.efrei.tp2.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métadonnées de la documentation OpenAPI + schéma d'authentification Bearer JWT
 * (bouton « Authorize » dans Swagger UI).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tp2OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TP2 - API REST avancées (Spring Boot)")
                        .version("1.0.0")
                        .description("Calculatrice, conversion de devises, suivi du BMI avec JWT. "
                                + "Pour les endpoints BMI : POST /api/auth/login, puis « Authorize » avec l'accessToken."))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
