package com.castagno.dev.login_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Test de roles", description = "Endpoints para probar restricciones por rol")
public class TestController {

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary     = "Panel de administración",
            description = "Solo accesible con rol ROLE_ADMIN",
            security    = @SecurityRequirement(name = "bearerAuth") 
    )
    public ResponseEntity<String> adminDashboard() {
        return ResponseEntity.ok("Bienvenido al panel de administración");
    }

    @GetMapping("/user/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary     = "Perfil de usuario",
            description = "Accesible con rol ROLE_USER o ROLE_ADMIN",
            security    = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<String> userProfile() {
        return ResponseEntity.ok("Bienvenido a tu perfil");
    }
}
