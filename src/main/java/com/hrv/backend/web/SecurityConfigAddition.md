# Lägg till i SecurityConfig.java

## 1. Tillåt /thyme/login och /react i filterChain:

.requestMatchers(
    "/api/auth/**",
    "/thyme/login",
    "/react/**",          // React SPA statisk fil
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/api-docs/**",
    "/v3/api-docs/**"
).permitAll()

## 2. Lägg till CookieJwtFilter BEFORE JwtAuthenticationFilter:

.addFilterBefore(cookieJwtFilter, JwtAuthenticationFilter.class)
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

## 3. Injektera CookieJwtFilter i SecurityConfig:
private final CookieJwtFilter cookieJwtFilter;
