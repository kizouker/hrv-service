package com.hrv.backend.web;

import com.hrv.backend.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Reads JWT from cookie (for Thymeleaf pages) in addition to
 * the Authorization header (handled by JwtAuthenticationFilter).
 */
@Component
@RequiredArgsConstructor
public class CookieJwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response); return;
        }
        if (request.getCookies() == null) { chain.doFilter(request, response); return; }

        Arrays.stream(request.getCookies())
                .filter(c -> "hrv_token".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .ifPresent(token -> {
                    try {
                        String username = jwtUtil.extractUsername(token);
                        UserDetails user = userDetailsService.loadUserByUsername(username);
                        if (jwtUtil.isTokenValid(token, user)) {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    user, null, user.getAuthorities());
                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    } catch (Exception ignored) {}
                });
        chain.doFilter(request, response);
    }
}
