package com.get401.auth.spring;

import com.get401.auth.core.JwtPublicKeyProvider;
import com.get401.auth.core.annotation.AuthGet401;
import com.get401.auth.core.annotation.VerifyRoles;
import com.get401.auth.core.annotation.VerifyScope;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationInterceptor.class);
    private final JwtPublicKeyProvider publicKeyProvider;

    public JwtAuthenticationInterceptor(JwtPublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = publicKeyProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // Determine if authentication or verification is required
        boolean authGet401 = hasAnnotation(handlerMethod, AuthGet401.class);
        VerifyRoles verifyRoles = getAnnotation(handlerMethod, VerifyRoles.class);
        VerifyScope verifyScope = getAnnotation(handlerMethod, VerifyScope.class);

        boolean requiresAuth = authGet401 || verifyRoles != null || verifyScope != null;

        if (!requiresAuth) {
            return true;
        }
        // 1. Extract the `aact` cookie
        String token = extractCookie(request, "aact");
        if (token == null) {
            log.info("Missing aact cookie");
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing aact cookie");
            return false;
        }

        // 2 & 3. Parse and Verify Token
        Jws<Claims> jws;
        try {
            jws = Jwts.parser()
                    .verifyWith(publicKeyProvider.getPublicKey())
                    .build()
                    .parseSignedClaims(token);

            // Note: JJWT strictly enforces the algorithm matching the key type natively.
            // If the public key is EdDSA, it refuses algorithms like none, HS256, RS256.
            String alg = jws.getHeader().getAlgorithm();
            if (!"EdDSA".equals(alg)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid algorithm: " + alg);
                return false;
            }
        } catch (JwtException | IllegalArgumentException e) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT token: " + e.getMessage());
            return false;
        }

        Claims claims = jws.getPayload();

        // 4. Check Roles
        if (verifyRoles != null) {
            List<?> userRoles = claims.get("roles", List.class);
            if (userRoles == null) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Missing roles in token");
                return false;
            }

            boolean hasRole = false;
            for (String requiredRole : verifyRoles.value()) {
                if (userRoles.contains(requiredRole)) {
                    hasRole = true;
                    break;
                }
            }

            if (!hasRole) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Insufficient roles");
                return false;
            }
        }

        // 5. Check Scopes
        if (verifyScope != null) {
            String userScopeRaw = claims.get("scope", String.class);
            if (userScopeRaw == null || userScopeRaw.isBlank()) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Missing scopes in token");
                return false;
            }

            List<String> userScopes = Arrays.asList(userScopeRaw.split(","));

            for (String requiredScope : verifyScope.value()) {
                if (!userScopes.contains(requiredScope.trim())) {
                    response.sendError(HttpStatus.FORBIDDEN.value(), "Missing required scope: " + requiredScope);
                    return false;
                }
            }
        }

        // Inject claims into request attributes for downstream use
        request.setAttribute("jwtClaims", claims);
        request.setAttribute("jwtSubject", claims.getSubject());

        return true;
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private <T extends java.lang.annotation.Annotation> boolean hasAnnotation(HandlerMethod method,
            Class<T> annotationClass) {
        return getAnnotation(method, annotationClass) != null;
    }

    private <T extends java.lang.annotation.Annotation> T getAnnotation(HandlerMethod method,
            Class<T> annotationClass) {
        T annotation = method.getMethodAnnotation(annotationClass);
        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(annotationClass);
        }
        return annotation;
    }
}
