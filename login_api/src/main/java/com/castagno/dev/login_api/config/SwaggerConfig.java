package com.castagno.dev.login_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "Login API",
                version     = "1.0",
                description = "API de autenticación con JWT y roles",
                contact     = @Contact(
                        name  = "Leonel Lopez",
                        email = "castagno.dev@gmail.com"
                )
        )
)
// Define el esquema de seguridad global — le dice a Swagger que esta API
// usa Bearer Token (JWT) en el header Authorization
@SecurityScheme(
        name            = "bearerAuth",       // nombre interno del esquema
        type            = SecuritySchemeType.HTTP,
        scheme          = "bearer",
        bearerFormat    = "JWT",
        description     = "Ingresá el token JWT obtenido en /api/auth/login"
)
public class SwaggerConfig {
}
