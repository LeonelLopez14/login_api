package com.castagno.dev.login_api.config;

import com.castagno.dev.login_api.security.JwtAuthFilter;
import com.castagno.dev.login_api.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // ─── Define qué endpoints son públicos y cuáles requieren auth ───
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                //  Deshabilita CSRF porque no usamos cookies/sesiones
                .csrf(AbstractHttpConfigurer::disable)

                //  Define reglas de acceso por endpoint
                .authorizeHttpRequests(auth -> auth

                        // Públicos — cualquiera puede acceder sin token
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()

                        // Solo ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // USER y ADMIN
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                        // Cualquier otro endpoint requiere estar autenticado
                        .anyRequest().authenticated()
                )

                //  Sin sesiones — cada request se autentica con el JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                //  Le dice a Spring qué AuthenticationProvider usar
                .authenticationProvider(authenticationProvider())

                //    Agrega el JwtAuthFilter ANTES del filtro estándar de Spring
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─── BCrypt para encriptar contraseñas ───────────────────────────
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ─── Conecta UserDetailsService + PasswordEncoder ────────────────
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ─── AuthenticationManager ───────────────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}