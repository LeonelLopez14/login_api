# AGENTS.md - AI Coding Agent Guide

## Project Overview
**Login API** is a Spring Boot 3.4.1 REST authentication service with JWT token-based security and role-based access control. Built with Java 21, MySQL, and containerized with Docker. Primary use case: user registration/login with stateless session management.

## Architecture Layers

### 1. **Security Layer** (`src/main/java/.../security/`)
- **JwtService**: Generates and validates JWT tokens using JJWT 0.12.6
  - Token claims include username and roles
  - Secret key from `jwt.secret` property (application.properties)
  - Expiration from `jwt.expiration` property (86400000ms = 24 hours default)
- **JwtAuthFilter**: Pre-authentication filter that validates JWT before reaching controllers
- **UserDetailsServiceImpl**: Custom Spring Security integration for User entity loading

### 2. **Configuration Layer** (`src/main/java/.../config/`)
- **SecurityConfig**: Defines endpoint access rules:
  - `/api/auth/**` → PUBLIC (login/register)
  - `/swagger-ui/**`, `/api-docs/**` → PUBLIC (Swagger documentation)
  - `/api/admin/**` → ADMIN role only
  - `/api/user/**` → USER or ADMIN roles
  - All other endpoints → Authenticated required
- **CorsConfig**: Cross-origin resource sharing configuration
- **SwaggerConfig**: OpenAPI 3.0 documentation setup (accessible at `/swagger-ui.html`)

### 3. **Service Layer** (`src/main/java/.../service/`)
- **AuthServiceImpl** (implements AuthService interface):
  - `register()`: Creates new user with ROLE_USER, checks username/email uniqueness, returns JWT
  - `login()`: Authenticates via AuthenticationManager, returns JWT
  - Both methods use BCryptPasswordEncoder for password hashing (set in SecurityConfig)
  - `@Transactional` ensures atomicity on register

### 4. **Controller Layer** (`src/main/java/.../controller/`)
- **AuthController** (`/api/auth`):
  - POST `/register` → 201 CREATED or 409 CONFLICT
  - POST `/login` → 200 OK or 401 UNAUTHORIZED
  - Full Swagger documentation via `@Operation`, `@ApiResponses` annotations

### 5. **Data Layer** (`src/main/java/.../model/` + `repository/`)
- **User** entity: ManyToMany relationship with Role via `user_roles` join table
  - Fields: id, username, email, password, enabled, roles
  - Unique constraints on username and email
- **Role** entity: Contains enum `RoleName` (ROLE_USER, ROLE_ADMIN)
- **Repositories**: IRoleRepository, IUserRepository (Spring Data JPA auto-implementation)

### 6. **Exception Handling** (`src/main/java/.../exception/`)
- **GlobalExceptionHandler**: Centralized `@RestControllerAdvice`
  - Custom exceptions: UserAlreadyExistsException (409), InvalidCredentialsException (401)
  - Spring validation errors → 400 with field-level messages
  - AccessDeniedException → 403
  - Unhandled exceptions → 500 with generic message (no stack traces exposed)

### 7. **DTO Pattern** (`src/main/java/.../dto/`)
- **Request DTOs**: LoginRequest, RegisterRequest with `@Valid` annotation
- **Response DTOs**: AuthResponse, ErrorResponse with builder pattern

## Build & Deployment

### Local Development
```bash
mvn clean package          # Builds login_api-0.0.1-SNAPSHOT.jar
mvn spring-boot:run        # Runs with dev profile (localhost:3306 MySQL required)
```

### Docker Deployment
```bash
docker-compose up          # Starts MySQL + Spring Boot app
# MySQL: port 3306 (admin/admin)
# App: port 8080, profile: docker
```
**Key**: Dockerfile uses multi-stage build (Maven builder + JRE runtime), excludes Lombok from final JAR

### MySQL Configuration
- **Dev** (application.properties): localhost:3306, creates schema on startup (ddl-auto=update)
- **Docker** (application-docker.properties): Container networking, persistent volume

## Critical Patterns

### 1. **Password Security**
- Always use `passwordEncoder.encode()` before saving User to DB (see AuthServiceImpl line 68)
- Never expose raw password in responses

### 2. **JWT Token Flow**
- AuthenticationManager validates credentials → JwtService generates token → Token returned in AuthResponse
- Client includes token in `Authorization: Bearer <token>` header for authenticated requests
- JwtAuthFilter validates token on every request (stateless)

### 3. **Role-Based Access Control**
- Roles stored as enum (ROLE_USER, ROLE_ADMIN) in database
- Extracted from User.roles collection and mapped to Spring GrantedAuthority
- SecurityConfig defines path-level authorization; use `@PreAuthorize("hasRole('ADMIN')")` on individual methods

### 4. **Transactional Consistency**
- Register method marked `@Transactional` → rollback on any error
- Repository calls within transaction ensure atomicity

### 5. **Error Response Standardization**
- All errors return ErrorResponse JSON with fields: status, error, message, timestamp
- Validation errors return flat Map<field, message> structure instead

## Technology Stack
| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.4.1 | Framework core |
| Java | 21 | Language runtime |
| MySQL | 8.0 | Persistence |
| JJWT | 0.12.6 | JWT generation/validation |
| Lombok | Latest | Boilerplate elimination (@Data, @Builder, @RequiredArgsConstructor) |
| SpringDoc OpenAPI | 2.7.0 | Swagger UI |
| Spring Security | 3.4.1 | Authentication/authorization |
| BCrypt | Spring default | Password hashing |

## Common Tasks for Agents

1. **Add New Endpoint**: Create method in controller → implement in service → add SecurityConfig rule
2. **Modify JWT Claims**: Edit JwtService.generateToken() claims, update extractClaim() if needed
3. **Add New Role**: Add to Role.RoleName enum → update SecurityConfig rules → update AuthServiceImpl role assignment
4. **Debug Auth Failures**: Check JwtAuthFilter logs → validate token in JwtService → inspect UserDetailsServiceImpl
5. **Add Validation**: Use Jakarta validation annotations on DTO fields → GlobalExceptionHandler auto-handles errors

## Testing Entry Point
- `LoginApiApplicationTests.java`: Main test class
- Use `@SpringBootTest` for integration tests with real context
- Use Spring Security test utilities (spring-security-test dependency included)

