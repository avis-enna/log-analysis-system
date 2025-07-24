package com.loganalyzer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Configuration for request/response logging and tracing.
 */
@Configuration
public class LoggingConfiguration {
    
    /**
     * Request logging filter that adds correlation IDs and logs request details.
     */
    @Bean
    public OncePerRequestFilter requestLoggingFilter() {
        return new OncePerRequestFilter() {
            private final Logger logger = LoggerFactory.getLogger("REQUEST_LOG");
            
            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                          HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                
                long startTime = System.currentTimeMillis();
                String correlationId = UUID.randomUUID().toString().substring(0, 8);
                
                // Add correlation ID to MDC for all log messages in this request
                MDC.put("correlationId", correlationId);
                MDC.put("requestMethod", request.getMethod());
                MDC.put("requestUri", request.getRequestURI());
                
                try {
                    // Log incoming request
                    logger.info("Incoming request: {} {} from {} [{}]", 
                        request.getMethod(), 
                        request.getRequestURI(), 
                        getClientIpAddress(request),
                        correlationId);
                    
                    // Process the request
                    filterChain.doFilter(request, response);
                    
                    // Log response
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("Request completed: {} {} -> {} in {}ms [{}]", 
                        request.getMethod(), 
                        request.getRequestURI(), 
                        response.getStatus(),
                        duration,
                        correlationId);
                    
                    // Log slow requests
                    if (duration > 1000) {
                        logger.warn("Slow request detected: {} {} took {}ms [{}]", 
                            request.getMethod(), 
                            request.getRequestURI(), 
                            duration,
                            correlationId);
                    }
                    
                } finally {
                    // Clean up MDC
                    MDC.clear();
                }
            }
            
            private String getClientIpAddress(HttpServletRequest request) {
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        };
    }
}
