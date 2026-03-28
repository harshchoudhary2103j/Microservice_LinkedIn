package com.harsh.linkedin.api_gateway.filters;

import com.harsh.linkedin.api_gateway.JwtService;
import com.harsh.linkedin.api_gateway.clients.LogoutClient;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthenticationFilter implements Filter {
    private final LogoutClient logoutClient;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/users/auth/login",
            "/api/v1/users/auth/signup"
    );

    private final JwtService jwtService;

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        log.info("Incoming request: {}", request.getRequestURI());

        if (PUBLIC_PATHS.stream().anyMatch(request.getRequestURI()::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String tokenHeader = request.getHeader("Authorization");

        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            log.error("Authorization token header not found");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }

        String token = tokenHeader.split("Bearer ")[1];

        try {

            Boolean isBlackListed = logoutClient.isTokenBlacklisted(token);
            if(Boolean.TRUE.equals(isBlackListed)){
                log.warn("Blocked request: Token has been blacklisted (user logged out)");
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
            String userId = jwtService.getUserIdFromToken(token);

            HttpServletRequest mutatedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) return userId;
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name))
                        return Collections.enumeration(List.of(userId));
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    names.add("X-User-Id");
                    return Collections.enumeration(names);
                }
            };

            chain.doFilter(mutatedRequest, response);

        } catch (JwtException e) {
            log.error("JWT Exception: {}", e.getLocalizedMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }
}