package com.sivareddy.loganalysis.service;

import com.sivareddy.loganalysis.model.LogEntry;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for detecting patterns in log entries
 * 
 * @author Siva Reddy
 */
@Service
public class PatternDetectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternDetectionService.class);
    
    // Security threat patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i).*(union|select|insert|delete|update|drop|create|alter|exec|script|javascript|<script|eval\\(|alert\\(|document\\.|window\\.).*"
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i).*(script|javascript|vbscript|onload|onerror|onclick|onmouseover|alert\\(|document\\.|window\\.).*"
    );
    
    private static final Pattern DIRECTORY_TRAVERSAL_PATTERN = Pattern.compile(
        ".*(\\.\\./|\\.\\\\|\\.\\./\\.\\./|\\\\\\.\\.\\\\).*"
    );
    
    private static final Pattern BRUTE_FORCE_PATTERN = Pattern.compile(
        "(?i).*(failed|invalid|incorrect|denied|unauthorized|forbidden|login|authentication|password).*"
    );
    
    private static final Pattern SUSPICIOUS_USER_AGENT_PATTERN = Pattern.compile(
        "(?i).*(bot|crawler|spider|scraper|scanner|nikto|sqlmap|nmap|masscan|hydra|gobuster|dirb|wfuzz).*"
    );
    
    // Performance patterns
    private static final Pattern SLOW_QUERY_PATTERN = Pattern.compile(
        "(?i).*(slow|timeout|deadlock|lock|blocked|waiting).*"
    );
    
    private static final Pattern MEMORY_ISSUE_PATTERN = Pattern.compile(
        "(?i).*(out of memory|memory|heap|garbage|gc|oom|outofmemoryerror).*"
    );
    
    // Error patterns
    private static final Pattern CRITICAL_ERROR_PATTERN = Pattern.compile(
        "(?i).*(fatal|critical|severe|exception|error|failure|crash|abort|panic).*"
    );
    
    /**
     * Check if a log entry represents a security threat
     */
    public boolean isSecurityThreat(LogEntry logEntry) {
        if (logEntry == null || logEntry.getMessage() == null) {
            return false;
        }
        
        String message = logEntry.getMessage();
        String userAgent = logEntry.getUserAgent();
        
        // Check for SQL injection attempts
        if (SQL_INJECTION_PATTERN.matcher(message).matches()) {
            logger.warn("SQL injection pattern detected in log entry: {}", logEntry.getId());
            return true;
        }
        
        // Check for XSS attempts
        if (XSS_PATTERN.matcher(message).matches()) {
            logger.warn("XSS pattern detected in log entry: {}", logEntry.getId());
            return true;
        }
        
        // Check for directory traversal attempts
        if (DIRECTORY_TRAVERSAL_PATTERN.matcher(message).matches()) {
            logger.warn("Directory traversal pattern detected in log entry: {}", logEntry.getId());
            return true;
        }
        
        // Check for suspicious user agents
        if (userAgent != null && SUSPICIOUS_USER_AGENT_PATTERN.matcher(userAgent).matches()) {
            logger.warn("Suspicious user agent detected in log entry: {}", logEntry.getId());
            return true;
        }
        
        // Check for error status codes that might indicate attacks
        if (logEntry.getStatusCode() != null) {
            int statusCode = logEntry.getStatusCode();
            if (statusCode == 401 || statusCode == 403 || statusCode == 404) {
                // Multiple 4xx errors from same IP might indicate scanning
                return isRepeatedFailureFromSameIP(logEntry);
            }
        }
        
        return false;
    }
    
    /**
     * Check if a log entry represents a failed login attempt
     */
    public boolean isFailedLoginAttempt(LogEntry logEntry) {
        if (logEntry == null || logEntry.getMessage() == null) {
            return false;
        }
        
        String message = logEntry.getMessage().toLowerCase();
        
        return BRUTE_FORCE_PATTERN.matcher(message).matches() &&
               (message.contains("login") || message.contains("authentication") || 
                message.contains("password") || message.contains("credential"));
    }
    
    /**
     * Check if a log entry represents a performance issue
     */
    public boolean isPerformanceIssue(LogEntry logEntry) {
        if (logEntry == null) {
            return false;
        }
        
        // Check response time
        if (logEntry.getResponseTime() != null && logEntry.getResponseTime() > 5000) {
            return true;
        }
        
        // Check for slow query patterns
        if (logEntry.getMessage() != null && SLOW_QUERY_PATTERN.matcher(logEntry.getMessage()).matches()) {
            return true;
        }
        
        // Check for memory issues
        if (logEntry.getMessage() != null && MEMORY_ISSUE_PATTERN.matcher(logEntry.getMessage()).matches()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a log entry represents a critical error
     */
    public boolean isCriticalError(LogEntry logEntry) {
        if (logEntry == null || logEntry.getMessage() == null) {
            return false;
        }
        
        return CRITICAL_ERROR_PATTERN.matcher(logEntry.getMessage()).matches() ||
               "ERROR".equals(logEntry.getLevel()) || 
               "FATAL".equals(logEntry.getLevel()) ||
               "CRITICAL".equals(logEntry.getLevel());
    }
    
    /**
     * Check if there are repeated failures from the same IP
     */
    private boolean isRepeatedFailureFromSameIP(LogEntry logEntry) {
        // This is a simplified check - in a real implementation, you would
        // query the database for recent failures from the same IP
        if (logEntry.getIpAddress() == null) {
            return false;
        }
        
        // For now, just flag any 4xx error as potentially suspicious
        return logEntry.getStatusCode() != null && logEntry.getStatusCode() >= 400 && logEntry.getStatusCode() < 500;
    }
    
    /**
     * Analyze log entry and return threat level
     */
    public ThreatLevel analyzeThreatLevel(LogEntry logEntry) {
        if (isCriticalError(logEntry)) {
            return ThreatLevel.CRITICAL;
        } else if (isSecurityThreat(logEntry)) {
            return ThreatLevel.HIGH;
        } else if (isPerformanceIssue(logEntry) || isFailedLoginAttempt(logEntry)) {
            return ThreatLevel.MEDIUM;
        } else if ("WARNING".equals(logEntry.getLevel())) {
            return ThreatLevel.LOW;
        } else {
            return ThreatLevel.NONE;
        }
    }
    
    /**
     * Enum for threat levels
     */
    public enum ThreatLevel {
        NONE, LOW, MEDIUM, HIGH, CRITICAL
    }
}
