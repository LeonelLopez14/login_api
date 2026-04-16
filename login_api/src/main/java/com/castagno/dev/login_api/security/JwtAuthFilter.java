package com.castagno.dev.login_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    // OncePerRequestFilter garantiza que el filtro se ejecuta UNA sola vez por request

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Busca el header Authorization
        final String authHeader = request.getHeader("Authorization");

        // 2. Si no hay token o no empieza con "Bearer ", deja pasar el request
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extrae el token removiendo el prefijo "Bearer "
        final String token = authHeader.substring(7);

        // 4. Extrae el username del token
        final String username = jwtService.extractUsername(token);

        // 5. Si hay username y todavía no hay autenticación en el contexto
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Carga el usuario desde la DB
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 7. Valida que el token sea válido para ese usuario
            if (jwtService.isTokenValid(token, userDetails)) {

                // 8. Crea el objeto de autenticación con los roles
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,           // credentials null porque ya autenticamos
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 9. Registra la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 10. Continúa con el siguiente filtro o llega al controller
        filterChain.doFilter(request, response);
    }
}
