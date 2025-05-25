# Admin Authentication Setup

## Problem

The admin application was redirecting to `https://admin.dev.uppdragsradarn.se/login/` which doesn't exist. The correct URL should be `https://admin.dev.uppdragsradarn.se/`.

## Root Cause

The backend SecurityConfig was redirecting admin authentication to `{frontendUrl}/admin/auth/callback` but the admin frontend is a separate application with its own domain, not a subpath of the main frontend.

## Solution

1. **Updated SecurityConfig** to support a separate admin frontend URL:
   - Added `@Value("${app.frontend.admin-url:#{null}}")` to inject admin URL
   - Modified the success handler to use the admin URL when available

2. **Updated application configuration**:
   - Added `admin-url` property to `application.yml`
   - Set default to `${ADMIN_FRONTEND_URL:${FRONTEND_URL}/admin}` for backward compatibility
   - Added allowed origins configuration to include admin URL

## Configuration

### Development
```bash
export FRONTEND_URL=http://dev.uppdragsradarn.se:3000
export ADMIN_FRONTEND_URL=http://admin.dev.uppdragsradarn.se:3001
```

### Production
```bash
export FRONTEND_URL=https://uppdragsradarn.se
export ADMIN_FRONTEND_URL=https://admin.uppdragsradarn.se
```

## Authentication Flow

1. User visits `https://admin.uppdragsradarn.se/`
2. Admin auth middleware checks if user is authenticated
3. If not, redirects to `/login` (on admin domain)
4. Login page redirects to `/oauth2/authorization/cognito-admin`
5. After successful auth, backend redirects to `https://admin.uppdragsradarn.se/auth/callback`
6. Callback page verifies user has admin role and redirects to home

## Key Files Modified

- `/backend/src/main/java/com/uppdragsradarn/application/config/SecurityConfig.java`
- `/backend/src/main/resources/application.yml`

## Testing

1. Set environment variables using `setup-admin-env.sh`
2. Start the backend with these variables
3. Access the admin app and verify the authentication flow works correctly