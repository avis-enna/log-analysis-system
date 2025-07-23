package com.sivareddy.loganalysis.service;

import com.sivareddy.loganalysis.model.LogEntry;
import com.sivareddy.loganalysis.model.Alert;
import com.sivareddy.loganalysis.repository.LogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for processing log files and entries
 * 
 * @author Siva Reddy
 */
@Service
@Transactional
public class LogProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogProcessingService.class);
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    @Autowired
    private AlertService alertService;
    
    @Autowired
    private PatternDetectionService patternDetectionService;
    
    // Common log patterns
    private static final Pattern APACHE_LOG_PATTERN = Pattern.compile(
        "^(\\S+) \\S+ \\S+ \\[([^\\]]+)\\] \"(\\S+) (\\S+) (\\S+)\" (\\d+) (\\S+) \"([^\"]*)\" \"([^\"]*)\"$"
    );
    
    private static final Pattern JAVA_LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) \\[(\\w+)\\] (\\w+) (\\S+) - (.+)$"
    );
    
    private static final Pattern JSON_LOG_PATTERN = Pattern.compile(
        "^\\{.*\"timestamp\"\\s*:\\s*\"([^\"]+)\".*\"level\"\\s*:\\s*\"([^\"]+)\".*\"message\"\\s*:\\s*\"([^\"]+)\".*\\}$"
    );
    
    private static final DateTimeFormatter APACHE_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z");
    private static final DateTimeFormatter JAVA_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    /**
     * Process a single log file
     */
    public CompletableFuture<Integer> processLogFile(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting to process log file: {}", filePath);
                
                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) {
                    logger.error("File does not exist or is not a file: {}", filePath);
                    return 0;
                }
                
                List<LogEntry> logEntries = new ArrayList<>();
                int processedLines = 0;
                
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        LogEntry logEntry = parseLogLine(line, file.getName());
                        if (logEntry != null) {
                            logEntries.add(logEntry);
                            processedLines++;
                            
                            // Process in batches to avoid memory issues
                            if (logEntries.size() >= 1000) {
                                saveBatch(logEntries);
                                logEntries.clear();
                            }
                        }
                    }
                    
                    // Process remaining entries
                    if (!logEntries.isEmpty()) {
                        saveBatch(logEntries);
                    }
                }
                
                logger.info("Successfully processed {} lines from file: {}", processedLines, filePath);
                return processedLines;
                
            } catch (IOException e) {
                logger.error("Error processing log file: {}", filePath, e);
                return 0;
            }
        });
    }
    
    /**
     * Parse a single log line based on detected format
     */
    private LogEntry parseLogLine(String line, String source) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try Apache/Nginx format first
            Matcher apacheMatcher = APACHE_LOG_PATTERN.matcher(line);
            if (apacheMatcher.matches()) {
                return parseApacheLogLine(apacheMatcher, source);
            }
            
            // Try Java application log format
            Matcher javaMatcher = JAVA_LOG_PATTERN.matcher(line);
            if (javaMatcher.matches()) {
                return parseJavaLogLine(javaMatcher, source);
            }
            
            // Try JSON format
            Matcher jsonMatcher = JSON_LOG_PATTERN.matcher(line);
            if (jsonMatcher.matches()) {
                return parseJsonLogLine(jsonMatcher, source);
            }
            
            // If no pattern matches, create a generic entry
            return createGenericLogEntry(line, source);
            
        } catch (Exception e) {
            logger.warn("Failed to parse log line: {}", line, e);
            return createGenericLogEntry(line, source);
        }
    }
    
    /**
     * Parse Apache/Nginx log line
     */
    private LogEntry parseApacheLogLine(Matcher matcher, String source) {
        LogEntry logEntry = new LogEntry();
        
        String ipAddress = matcher.group(1);
        String timestamp = matcher.group(2);
        String method = matcher.group(3);
        String url = matcher.group(4);
        String protocol = matcher.group(5);
        String statusCode = matcher.group(6);
        String size = matcher.group(7);
        String referer = matcher.group(8);
        String userAgent = matcher.group(9);
        
        logEntry.setIpAddress(ipAddress);
        logEntry.setTimestamp(parseTimestamp(timestamp, APACHE_DATE_FORMAT));
        logEntry.setSource(source);
        logEntry.setMessage(String.format("%s %s %s", method, url, protocol));
        logEntry.setUserAgent(userAgent);
        logEntry.setStatusCode(Integer.parseInt(statusCode));
        
        // Determine log level based on status code
        int status = Integer.parseInt(statusCode);
        if (status >= 500) {
            logEntry.setLevel("ERROR");
        } else if (status >= 400) {
            logEntry.setLevel("WARNING");
        } else {
            logEntry.setLevel("INFO");
        }
        
        return logEntry;
    }
    
    /**
     * Parse Java application log line
     */
    private LogEntry parseJavaLogLine(Matcher matcher, String source) {
        LogEntry logEntry = new LogEntry();
        
        String timestamp = matcher.group(1);
        String thread = matcher.group(2);
        String level = matcher.group(3);
        String logger = matcher.group(4);
        String message = matcher.group(5);
        
        logEntry.setTimestamp(parseTimestamp(timestamp, JAVA_DATE_FORMAT));
        logEntry.setLevel(level);
        logEntry.setSource(source);
        logEntry.setMessage(message);
        logEntry.setThreadName(thread);
        logEntry.setLoggerName(logger);
        
        return logEntry;
    }
    
    /**
     * Parse JSON log line
     */
    private LogEntry parseJsonLogLine(Matcher matcher, String source) {
        LogEntry logEntry = new LogEntry();
        
        String timestamp = matcher.group(1);
        String level = matcher.group(2);
        String message = matcher.group(3);
        
        logEntry.setTimestamp(parseTimestamp(timestamp, ISO_DATE_FORMAT));
        logEntry.setLevel(level);
        logEntry.setSource(source);
        logEntry.setMessage(message);
        
        return logEntry;
    }
    
    /**
     * Create a generic log entry for unparseable lines
     */
    private LogEntry createGenericLogEntry(String line, String source) {
        LogEntry logEntry = new LogEntry();
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setLevel("INFO");
        logEntry.setSource(source);
        logEntry.setMessage(line);
        return logEntry;
    }
    
    /**
     * Parse timestamp from string
     */
    private LocalDateTime parseTimestamp(String timestampStr, DateTimeFormatter formatter) {
        try {
            return LocalDateTime.parse(timestampStr, formatter);
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp: {}", timestampStr, e);
            return LocalDateTime.now();
        }
    }
    
    /**
     * Save a batch of log entries
     */
    private void saveBatch(List<LogEntry> logEntries) {
        try {
            logEntryRepository.saveAll(logEntries);
            
            // Process each entry for pattern detection and alerts
            for (LogEntry entry : logEntries) {
                processLogEntryForAlerts(entry);
            }
            
        } catch (Exception e) {
            logger.error("Error saving batch of log entries", e);
        }
    }
    
    /**
     * Process log entry for potential alerts
     */
    private void processLogEntryForAlerts(LogEntry logEntry) {
        try {
            // Check for error patterns
            if ("ERROR".equals(logEntry.getLevel()) || "CRITICAL".equals(logEntry.getLevel())) {
                alertService.createAlert(
                    "ERROR_DETECTED",
                    "HIGH",
                    String.format("Error detected in %s: %s", logEntry.getSource(), logEntry.getMessage()),
                    logEntry.getSource(),
                    logEntry.getIpAddress(),
                    logEntry.getId()
                );
            }
            
            // Check for security patterns
            if (patternDetectionService.isSecurityThreat(logEntry)) {
                alertService.createAlert(
                    "SECURITY_THREAT",
                    "CRITICAL",
                    String.format("Security threat detected from IP %s: %s", logEntry.getIpAddress(), logEntry.getMessage()),
                    logEntry.getSource(),
                    logEntry.getIpAddress(),
                    logEntry.getId()
                );
            }
            
            // Check for performance issues
            if (logEntry.getResponseTime() != null && logEntry.getResponseTime() > 5000) {
                alertService.createAlert(
                    "PERFORMANCE_ISSUE",
                    "MEDIUM",
                    String.format("Slow response time detected: %dms", logEntry.getResponseTime()),
                    logEntry.getSource(),
                    logEntry.getIpAddress(),
                    logEntry.getId()
                );
            }
            
            // Check for failed login attempts
            if (patternDetectionService.isFailedLoginAttempt(logEntry)) {
                alertService.createAlert(
                    "FAILED_LOGIN",
                    "MEDIUM",
                    String.format("Failed login attempt from IP %s", logEntry.getIpAddress()),
                    logEntry.getSource(),
                    logEntry.getIpAddress(),
                    logEntry.getId()
                );
            }
            
        } catch (Exception e) {
            logger.error("Error processing log entry for alerts: {}", logEntry.getId(), e);
        }
    }
    
    /**
     * Get processing statistics
     */
    public ProcessingStats getProcessingStats() {
        ProcessingStats stats = new ProcessingStats();
        
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        
        stats.setTotalLogsProcessed(logEntryRepository.count());
        stats.setLogsLast24Hours(logEntryRepository.countByLevelSince("INFO", last24Hours) +
                                 logEntryRepository.countByLevelSince("WARNING", last24Hours) +
                                 logEntryRepository.countByLevelSince("ERROR", last24Hours));
        stats.setErrorLogsLast24Hours(logEntryRepository.countByLevelSince("ERROR", last24Hours));
        stats.setWarningLogsLast24Hours(logEntryRepository.countByLevelSince("WARNING", last24Hours));
        
        return stats;
    }
    
    /**
     * Inner class for processing statistics
     */
    public static class ProcessingStats {
        private Long totalLogsProcessed;
        private Long logsLast24Hours;
        private Long errorLogsLast24Hours;
        private Long warningLogsLast24Hours;
        
        // Getters and setters
        public Long getTotalLogsProcessed() { return totalLogsProcessed; }
        public void setTotalLogsProcessed(Long totalLogsProcessed) { this.totalLogsProcessed = totalLogsProcessed; }
        
        public Long getLogsLast24Hours() { return logsLast24Hours; }
        public void setLogsLast24Hours(Long logsLast24Hours) { this.logsLast24Hours = logsLast24Hours; }
        
        public Long getErrorLogsLast24Hours() { return errorLogsLast24Hours; }
        public void setErrorLogsLast24Hours(Long errorLogsLast24Hours) { this.errorLogsLast24Hours = errorLogsLast24Hours; }
        
        public Long getWarningLogsLast24Hours() { return warningLogsLast24Hours; }
        public void setWarningLogsLast24Hours(Long warningLogsLast24Hours) { this.warningLogsLast24Hours = warningLogsLast24Hours; }
    }
}
