package com.castagno.dev.login_api.service;

import com.castagno.dev.login_api.dto.request.LoginRequest;
import com.castagno.dev.login_api.dto.request.RegisterRequest;
import com.castagno.dev.login_api.dto.response.AuthResponse;
import com.castagno.dev.login_api.exception.custom.InvalidCredentialsException;
import com.castagno.dev.login_api.exception.custom.UserAlreadyExistsException;
import com.castagno.dev.login_api.model.Role;
import com.castagno.dev.login_api.model.User;
import com.castagno.dev.login_api.repository.IRoleRepository;
import com.castagno.dev.login_api.repository.IUserRepository;
import com.castagno.dev.login_api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    // ─── REGISTER ────────────────────────────────────────────────────
    @Override
    @Transactional  // Si algo falla en el medio, rollback completo
    public AuthResponse register(RegisterRequest request) {

        //  Verificar que el username no esté tomado
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(
                    "El username '" + request.getUsername() + "' ya está en uso"
            );
        }

        //  Verificar que el email no esté tomado
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "El email '" + request.getEmail() + "' ya está registrado"
            );
        }

        //  Buscar el rol ROLE_USER en la DB
        Role userRole = roleRepository.findByName(Role.RoleName.ROLE_USER)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(Role.RoleName.ROLE_USER)
                                .build()
                ));

        //  Construir el usuario con la contraseña encriptada
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(Set.of(userRole))
                .build();

        //  Guardar en la DB
        userRepository.save(user);

        //  Cargar el UserDetails para generar el token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        //  Generar el JWT
        String token = jwtService.generateToken(userDetails);

        //  Armar y devolver la respuesta
        return buildAuthResponse(token, user);
    }

    // ─── LOGIN ───────────────────────────────────────────────────────
    @Override
    public AuthResponse login(LoginRequest request) {

        try {
            //  AuthenticationManager valida usuario + contraseña contra la DB
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }


        //   Cargamos el UserDetails para generar el token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());

        //  Generar el JWT
        String token = jwtService.generateToken(userDetails);

        //  Buscar el User completo para armar la respuesta
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Usuario no encontrado"
                ));

        //  Armar y devolver la respuesta
        return buildAuthResponse(token, user);
    }

    // ─── Método privado para no repetir la construcción del response ─
    private AuthResponse buildAuthResponse(String token, User user) {

        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }
}
