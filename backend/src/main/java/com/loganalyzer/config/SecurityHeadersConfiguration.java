package com.loganalyzer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Configuration for security headers to improve application security.
 */
@Configuration
public class SecurityHeadersConfiguration {
    
    /**
     * Filter to add security headers to all responses.
     */
    @Bean
    public OncePerRequestFilter securityHeadersFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                          HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                
                // Prevent clickjacking attacks
                response.setHeader("X-Frame-Options", "DENY");
                
                // Prevent MIME type sniffing
                response.setHeader("X-Content-Type-Options", "nosniff");
                
                // Enable XSS protection
                response.setHeader("X-XSS-Protection", "1; mode=block");
                
                // Enforce HTTPS in production (commented for local development)
                // response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                
                // Content Security Policy (basic policy for development)
                response.setHeader("Content-Security-Policy", 
                    "default-src 'self'; " +
                    "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                    "style-src 'self' 'unsafe-inline'; " +
                    "img-src 'self' data: https:; " +
                    "font-src 'self' https:; " +
                    "connect-src 'self' ws: wss:; " +
                    "frame-ancestors 'none'");
                
                // Referrer Policy
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                
                // Permissions Policy (formerly Feature Policy)
                response.setHeader("Permissions-Policy", 
                    "camera=(), microphone=(), geolocation=(), payment=()");
                
                // Cache control for sensitive endpoints
                String requestURI = request.getRequestURI();
                if (requestURI.contains("/api/") && !requestURI.contains("/public/")) {
                    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
                    response.setHeader("Pragma", "no-cache");
                    response.setHeader("Expires", "0");
                }
                
                filterChain.doFilter(request, response);
            }
        };
    }
}
