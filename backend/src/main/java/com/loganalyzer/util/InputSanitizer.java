package com.loganalyzer.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent security vulnerabilities.
 */
@Component
public class InputSanitizer {
    
    // Patterns for detecting potentially malicious input
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript|onload|onerror)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|</script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LOG_INJECTION_PATTERN = Pattern.compile(
        "[\r\n\t]",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Sanitizes search query input to prevent injection attacks.
     */
    public String sanitizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }
        
        // Remove null bytes
        query = query.replace("\0", "");
        
        // Remove or escape potentially dangerous characters
        query = query.replace("'", "\\'")
                    .replace("\"", "\\\"")
                    .replace(";", "\\;");
        
        // Limit length to prevent DoS
        if (query.length() > 1000) {
            query = query.substring(0, 1000);
        }
        
        return query.trim();
    }
    
    /**
     * Sanitizes log message content to prevent log injection.
     */
    public String sanitizeLogMessage(String message) {
        if (message == null) {
            return null;
        }
        
        // Remove line breaks and tabs that could be used for log injection
        message = LOG_INJECTION_PATTERN.matcher(message).replaceAll(" ");
        
        // Remove null bytes
        message = message.replace("\0", "");
        
        // Limit length
        if (message.length() > 10000) {
            message = message.substring(0, 10000) + "... [truncated]";
        }
        
        return message.trim();
    }
    
    /**
     * Sanitizes field names to ensure they're safe for database queries.
     */
    public String sanitizeFieldName(String fieldName) {
        if (fieldName == null) {
            return null;
        }
        
        // Only allow alphanumeric characters, underscores, and dots
        fieldName = fieldName.replaceAll("[^a-zA-Z0-9_.]", "");
        
        // Limit length
        if (fieldName.length() > 50) {
            fieldName = fieldName.substring(0, 50);
        }
        
        return fieldName;
    }
    
    /**
     * Validates that input doesn't contain SQL injection patterns.
     */
    public boolean containsSqlInjection(String input) {
        if (input == null) {
            return false;
        }
        
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }
    
    /**
     * Validates that input doesn't contain XSS patterns.
     */
    public boolean containsXss(String input) {
        if (input == null) {
            return false;
        }
        
        return XSS_PATTERN.matcher(input).find();
    }
    
    /**
     * Comprehensive input validation for search queries.
     */
    public void validateSearchInput(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }
        
        if (containsSqlInjection(query)) {
            throw new IllegalArgumentException("Search query contains potentially malicious SQL patterns");
        }
        
        if (containsXss(query)) {
            throw new IllegalArgumentException("Search query contains potentially malicious script patterns");
        }
        
        if (query.length() > 1000) {
            throw new IllegalArgumentException("Search query is too long (maximum 1000 characters)");
        }
    }
    
    /**
     * Sanitizes and validates log entry data.
     */
    public void validateLogEntry(String level, String message, String source) {
        if (level != null && !isValidLogLevel(level)) {
            throw new IllegalArgumentException("Invalid log level: " + level);
        }
        
        if (message != null && message.length() > 10000) {
            throw new IllegalArgumentException("Log message is too long (maximum 10000 characters)");
        }
        
        if (source != null && (source.length() > 100 || containsXss(source))) {
            throw new IllegalArgumentException("Invalid log source");
        }
    }
    
    private boolean isValidLogLevel(String level) {
        return level.matches("^(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)$");
    }
}
