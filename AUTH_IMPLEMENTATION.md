# Authentication Flow Implementation Guide

## Overview

This document describes the complete authentication flow implementation for the Moovie backend, including OAuth callback handling, JWT token generation/validation, and protected endpoint middleware.

## Features Implemented

### 1. OAuth Callback Endpoint
- **Endpoint:** `POST /auth/oauth/callback`
- **Validates:** OAuth state parameter (CSRF protection)
- **Exchanges:** Authorization code for OAuth provider token
- **Retrieves:** User information from OAuth provider
- **Generates:** JWT access token with user claims
- **Returns:** Access token, token type, expiry, user profile

### 2. JWT Token Management
- **Generation:** HS256 symmetric key signing using HMAC-SHA256
- **Claims:** userId, email, username, avatar, iat, exp, iss
- **Validation:** Signature verification + constant-time comparison + expiry checking
- **Storage:** Optional; frontend responsible for secure storage

### 3. Token Refresh Endpoint
- **Endpoint:** `POST /auth/refresh`
- **Status:** Deferred to Phase 2 (returns 501 not_implemented)
- **Future:** Will support refresh token rotation

### 4. Authentication Middleware
- **Protection:** All write-action endpoints require valid JWT
- **Header Format:** `Authorization: Bearer <jwt_token>`
- **Validation:** Automatic token extraction and verification
- **Error Handling:** 401 Unauthorized with descriptive error messages

## Architecture

### Domain Layer
- **Models:**
  - `User` - user profile (id, email, username, avatar, createdAt)
  - `AuthToken` - token response (accessToken, tokenType, expiresIn, user)
  - `JWTClaims` - decoded token claims
  - `OAuthCallbackRequest` - OAuth callback request

- **Repositories:**
  - `AuthRepository` - OAuth + JWT operations
  - `UserRepository` - user persistence

- **Use Cases:**
  - `ProcessOAuthCallback` - handle OAuth callback
  - `ValidateToken` - validate JWT signature and claims
  - `RefreshToken` - refresh access token (Phase 2)

### Data Layer
- **OAuth Data Source:** HTTP client integration with OAuth provider
- **User Local Data Source:** In-memory user storage (Phase 1)
- **OAuth State Data Source:** CSRF state parameter management with TTL
- **JWT Utility:** Token generation and validation

### Presentation Layer
- **Routes:** `/auth/oauth/callback`, `/auth/refresh`
- **Middleware:** Custom JWT authentication (via `authenticateJWT` extension)
- **Error Responses:** Standardized error format with descriptive messages

## Configuration

### Environment Variables

**Required:**
```bash
JWT_SECRET="<32+ random bytes generated with: openssl rand -base64 32>"
OAUTH_CLIENT_ID="<your_oauth_provider_client_id>"
OAUTH_CLIENT_SECRET="<your_oauth_provider_client_secret>"
OAUTH_PROVIDER_URL="<https://your-oauth-provider.com/oauth>"
```

**Optional:**
```bash
JWT_EXPIRY_SECONDS=3600          # Default: 1 hour
JWT_ISSUER="moovie-backend"       # Default: moovie-backend
OAUTH_REDIRECT_URI="..."          # Default: http://localhost:8080/auth/oauth/callback
```

### Koin DI Setup

Auth dependencies are automatically registered in `DataModule`:
```kotlin
single<JWTUtil> { ... }
single<OAuthDataSource> { ... }
single<OAuthStateDataSource> { ... }
single<UserRepository> { ... }
single<AuthRepository> { ... }
```

And use cases in `AppModule`:
```kotlin
factory { ProcessOAuthCallback(...) }
factory { ValidateToken(...) }
factory { RefreshToken(...) }
```

## API Contracts

### OAuth Callback Request
```json
POST /auth/oauth/callback
{
  "code": "authorization_code_from_provider",
  "state": "csrf_protection_nonce",
  "codeVerifier": "optional_pkce_verifier"
}
```

### Success Response (200)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshToken": null,
  "user": {
    "id": "user123",
    "email": "user@example.com",
    "username": "username",
    "avatar": "https://...",
    "createdAt": "2026-05-18T10:00:00Z"
  }
}
```

### Error Response (400/401/500)
```json
{
  "error": "invalid_state",
  "message": "Invalid or expired state parameter"
}
```

Error codes:
- `invalid_state` - CSRF protection violation
- `invalid_code` - Expired or revoked OAuth code
- `provider_error` - OAuth provider returned error
- `user_creation_failed` - Database error
- `internal_error` - Unexpected server error

### Protected Endpoint Usage
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Returns:
- **401 Unauthorized** if token missing or invalid
- **403 Forbidden** if token valid but insufficient permissions (Phase 2)

## Testing

### Unit Tests
- `JWTUtilTest` - Token generation, validation, expiry, signature verification
- Claims extraction, malformed token handling, constant-time comparison

### Integration Tests (Phase 2)
- OAuth callback with real provider mock
- Protected endpoint access control
- Token refresh flow
- State parameter validation

### Running Tests
```bash
./gradlew test
```

## Security Considerations

### Implemented
- ✓ Constant-time signature comparison (prevents timing attacks)
- ✓ HMAC-SHA256 with secure secret
- ✓ CSRF protection via state parameter
- ✓ Token expiry validation
- ✓ No token/credential logging
- ✓ Authorization header validation

### Phase 2
- Token refresh with revocation tracking
- Rate limiting on OAuth callback endpoint
- PKCE validation
- Token rotation with dual-key acceptance
- Database persistence for users and refresh tokens

## Deployment

### Production Checklist
1. Generate secure JWT_SECRET: `openssl rand -base64 32`
2. Configure OAuth provider credentials in environment
3. Set HTTPS/TLS at load balancer level (HSTS headers)
4. Configure CORS origins appropriately
5. Enable request logging (excluding tokens)
6. Set up monitoring for auth failures and provider errors

### Docker
```dockerfile
ENV JWT_SECRET=...
ENV OAUTH_CLIENT_ID=...
ENV OAUTH_CLIENT_SECRET=...
ENV OAUTH_PROVIDER_URL=...
```

## Migration Notes

### From Phase 1 to Phase 2
1. Replace in-memory user storage with database persistence
2. Implement refresh token storage and revocation
3. Add rate limiting to OAuth callback
4. Enable PKCE validation
5. Implement dual-key JWT secret rotation
6. Add database schema migrations

## Known Limitations

### Phase 1
- Users stored in-memory (lost on restart)
- Single OAuth provider support
- No refresh token validation/revocation
- No rate limiting
- No PKCE validation

### Deferred
- Multi-provider OAuth support
- Social login linking
- Account deletion workflow
- Custom permission scopes
- Session management / device listing

## References

- [OAuth 2.0 Authorization Code Flow](https://datatracker.ietf.org/doc/html/rfc6749#section-1.3.1)
- [JWT Claims](https://datatracker.ietf.org/doc/html/rfc7519)
- [HMAC-SHA256](https://en.wikipedia.org/wiki/HMAC)
- [Constant Time Comparison](https://codahale.com/a-lesson-in-timing-attacks/)
