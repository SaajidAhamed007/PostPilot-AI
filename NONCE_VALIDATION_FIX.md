# LinkedIn OAuth NONCE Validation Fix - Summary

## Problem

`[invalid_nonce]` error during LinkedIn OAuth2 callback indicates Spring Security couldn't validate the NONCE from LinkedIn's ID token.

## Root Causes Fixed

### 1. **Missing OAuth Authorization Request Repository** ✅

**Issue**: Spring wasn't storing the authorization state (including NONCE) in the session.
**Fix**: Added `HttpSessionOAuth2AuthorizationRequestRepository` bean to [SecurityConfig.java](SecurityConfig.java)

```java
@Bean
public HttpSessionOAuth2AuthorizationRequestRepository oAuth2AuthorizationRequestRepository() {
    return new HttpSessionOAuth2AuthorizationRequestRepository();
}
```

### 2. **Missing Session Configuration** ✅

**Issue**: OAuth2 flow requires stateful sessions, but session cookies weren't properly configured.
**Fix**: Added to [application.properties](application.properties):

```properties
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
server.servlet.session.cookie.same-site=lax
server.servlet.session.timeout=30m
```

### 3. **Incorrect OIDC User Info Endpoint** ✅

**Issue**: Was using `/v2/me` instead of `/v2/userinfo`
**Fix**: Updated in [application.properties](application.properties):

```properties
spring.security.oauth2.client.provider.linkedin.user-info-uri=https://api.linkedin.com/v2/userinfo
```

### 4. **Wrong User Name Attribute** ✅

**Issue**: Was using `id` instead of standard OpenID `sub` claim
**Fix**: Updated in [application.properties](application.properties):

```properties
spring.security.oauth2.client.provider.linkedin.user-name-attribute=sub
```

### 5. **Missing OpenID Connect Configuration** ✅

**Issue**: Modern LinkedIn OAuth requires OIDC issuer URI
**Fix**: Added to [application.properties](application.properties):

```properties
spring.security.oauth2.client.provider.linkedin.issuer-uri=https://www.linkedin.com/oauth
```

## Next Steps to Verify

1. **Rebuild the application**:

   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

2. **Test LinkedIn login**:
   - Navigate to http://localhost:5173/login
   - Click "Sign in with LinkedIn"
   - Check if you get past the NONCE validation error

3. **If NONCE still fails**:
   - Verify redirect URI in LinkedIn Developer Portal exactly matches: `http://localhost:8080/login/oauth2/code/linkedin`
   - Check that "Sign In with LinkedIn using OpenID Connect" is enabled in app settings
   - Clear browser cookies and try again

4. **Important**: Monitor application logs for:
   ```
   Loading OIDC user from provider: linkedin
   ```
   If you see this, the NONCE issue should be resolved.

## Critical Security Notes

- `server.servlet.session.cookie.secure=false` is only for localhost development.
- Change to `true` in production (requires HTTPS)
- The `same-site=lax` setting protects against CSRF while allowing OAuth redirects

## LinkedIn w_member_social Warning

The scope `w_member_social` is **restricted access**. Even if OAuth succeeds, posting to LinkedIn will fail without explicit approval from LinkedIn. Check your app's Products section.
