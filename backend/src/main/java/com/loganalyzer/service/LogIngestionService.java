package com.loganalyzer.service;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for ingesting and processing log entries.
 * Handles real-time log processing, parsing, and indexing.
 */
@Service
public class LogIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogIngestionService.class);
    
    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private AlertService alertService;
    
    @Value("${log-processing.batch-size:1000}")
    private int batchSize;
    
    @Value("${log-processing.flush-interval:5000}")
    private long flushInterval;
    
    // Processing statistics
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final Map<String, AtomicLong> sourceStats = new ConcurrentHashMap<>();
    
    // Log parsing patterns
    private static final Map<String, Pattern> LOG_PATTERNS = new HashMap<>();
    
    static {
        // Common log patterns
        LOG_PATTERNS.put("APACHE_COMMON", Pattern.compile(
            "^(\\S+) \\S+ \\S+ \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+)"));
        
        LOG_PATTERNS.put("NGINX", Pattern.compile(
            "^(\\S+) - \\S+ \\[([\\d/\\w: +]+)\\] \"(\\w+) ([^\"]+) HTTP/[\\d.]+\" (\\d{3}) (\\d+) \"([^\"]+)\" \"([^\"]+)\""));
        
        LOG_PATTERNS.put("JAVA_LOG4J", Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) \\[(\\w+)\\] (\\w+) (\\S+) - (.*)"));
        
        LOG_PATTERNS.put("SYSLOG", Pattern.compile(
            "^(\\w{3} \\d{1,2} \\d{2}:\\d{2}:\\d{2}) (\\S+) (\\w+)\\[(\\d+)\\]: (.*)"));
        
        LOG_PATTERNS.put("JSON", Pattern.compile(
            "^\\{.*\\}$"));
    }
    
    /**
     * Ingests a single log entry.
     */
    @Async
    public CompletableFuture<LogEntry> ingestLog(String rawLog, String source) {
        try {
            LogEntry logEntry = parseLogEntry(rawLog, source);
            
            // Enrich the log entry
            enrichLogEntry(logEntry);
            
            // Save to Elasticsearch
            LogEntry savedEntry = logEntryRepository.save(logEntry);
            
            // Send to Kafka for real-time processing
            if (kafkaTemplate != null) {
                kafkaTemplate.send("log-events", savedEntry);
            }
            
            // Update statistics
            processedCount.incrementAndGet();
            sourceStats.computeIfAbsent(source, k -> new AtomicLong(0)).incrementAndGet();
            
            // Check for alerts
            alertService.checkLogForAlerts(savedEntry);
            
            logger.debug("Log ingested successfully: {}", savedEntry.getId());
            return CompletableFuture.completedFuture(savedEntry);
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            logger.error("Failed to ingest log from source {}: {}", source, rawLog, e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Ingests multiple log entries in batch.
     */
    @Async
    public CompletableFuture<List<LogEntry>> ingestLogsBatch(List<String> rawLogs, String source) {
        List<LogEntry> processedLogs = new ArrayList<>();
        
        try {
            for (String rawLog : rawLogs) {
                LogEntry logEntry = parseLogEntry(rawLog, source);
                enrichLogEntry(logEntry);
                processedLogs.add(logEntry);
            }
            
            // Batch save to Elasticsearch
            List<LogEntry> savedEntries = (List<LogEntry>) logEntryRepository.saveAll(processedLogs);
            
            // Send batch to Kafka
            if (kafkaTemplate != null) {
                for (LogEntry entry : savedEntries) {
                    kafkaTemplate.send("log-events", entry);
                }
            }
            
            // Update statistics
            processedCount.addAndGet(savedEntries.size());
            sourceStats.computeIfAbsent(source, k -> new AtomicLong(0)).addAndGet(savedEntries.size());
            
            // Check for alerts
            for (LogEntry entry : savedEntries) {
                alertService.checkLogForAlerts(entry);
            }
            
            logger.info("Batch ingested successfully: {} logs from source {}", savedEntries.size(), source);
            return CompletableFuture.completedFuture(savedEntries);
            
        } catch (Exception e) {
            errorCount.addAndGet(rawLogs.size());
            logger.error("Failed to ingest batch from source {}: {} logs", source, rawLogs.size(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Kafka listener for real-time log processing.
     */
    @KafkaListener(topics = "raw-logs", groupId = "log-processor")
    public void processRawLog(String rawLog) {
        try {
            // Extract source from message headers or use default
            String source = "kafka-stream";
            ingestLog(rawLog, source);
            
        } catch (Exception e) {
            logger.error("Failed to process raw log from Kafka: {}", rawLog, e);
        }
    }
    
    /**
     * Parses raw log string into LogEntry object.
     */
    private LogEntry parseLogEntry(String rawLog, String source) {
        LogEntry logEntry = new LogEntry();
        logEntry.setId(UUID.randomUUID().toString());
        logEntry.setSource(source);
        logEntry.setTimestamp(LocalDateTime.now());
        
        // Try to match against known patterns
        for (Map.Entry<String, Pattern> patternEntry : LOG_PATTERNS.entrySet()) {
            Matcher matcher = patternEntry.getValue().matcher(rawLog);
            if (matcher.matches()) {
                parseWithPattern(logEntry, matcher, patternEntry.getKey());
                logEntry.setParsed(true);
                logEntry.setOriginalFormat(patternEntry.getKey());
                return logEntry;
            }
        }
        
        // If no pattern matches, try to extract basic information
        parseGenericLog(logEntry, rawLog);
        
        return logEntry;
    }
    
    /**
     * Parses log using specific pattern.
     */
    private void parseWithPattern(LogEntry logEntry, Matcher matcher, String patternType) {
        switch (patternType) {
            case "APACHE_COMMON":
                logEntry.setHost(matcher.group(1));
                logEntry.setTimestamp(parseTimestamp(matcher.group(2)));
                logEntry.setHttpMethod(matcher.group(3));
                logEntry.setHttpUrl(matcher.group(4));
                logEntry.setHttpStatus(Integer.parseInt(matcher.group(6)));
                logEntry.setMessage(String.format("%s %s %s", matcher.group(3), matcher.group(4), matcher.group(5)));
                break;
                
            case "JAVA_LOG4J":
                logEntry.setTimestamp(parseTimestamp(matcher.group(1)));
                logEntry.setThread(matcher.group(2));
                logEntry.setLevel(matcher.group(3));
                logEntry.setLogger(matcher.group(4));
                logEntry.setMessage(matcher.group(5));
                break;
                
            case "NGINX":
                logEntry.setHost(matcher.group(1));
                logEntry.setTimestamp(parseTimestamp(matcher.group(2)));
                logEntry.setHttpMethod(matcher.group(3));
                logEntry.setHttpUrl(matcher.group(4));
                logEntry.setHttpStatus(Integer.parseInt(matcher.group(5)));
                logEntry.setMessage(String.format("%s %s", matcher.group(3), matcher.group(4)));
                break;
                
            case "SYSLOG":
                logEntry.setTimestamp(parseTimestamp(matcher.group(1)));
                logEntry.setHost(matcher.group(2));
                logEntry.setApplication(matcher.group(3));
                logEntry.setMessage(matcher.group(5));
                break;
                
            case "JSON":
                parseJsonLog(logEntry, matcher.group(0));
                break;
        }
    }
    
    /**
     * Parses generic log without specific pattern.
     */
    private void parseGenericLog(LogEntry logEntry, String rawLog) {
        logEntry.setMessage(rawLog);
        
        // Try to extract log level
        String upperLog = rawLog.toUpperCase();
        if (upperLog.contains("ERROR") || upperLog.contains("FATAL")) {
            logEntry.setLevel("ERROR");
            logEntry.setSeverity(4);
        } else if (upperLog.contains("WARN")) {
            logEntry.setLevel("WARN");
            logEntry.setSeverity(3);
        } else if (upperLog.contains("INFO")) {
            logEntry.setLevel("INFO");
            logEntry.setSeverity(2);
        } else if (upperLog.contains("DEBUG")) {
            logEntry.setLevel("DEBUG");
            logEntry.setSeverity(1);
        } else {
            logEntry.setLevel("UNKNOWN");
            logEntry.setSeverity(0);
        }
        
        // Try to extract timestamp from beginning of log
        String[] parts = rawLog.split("\\s+");
        if (parts.length > 0) {
            LocalDateTime timestamp = parseTimestamp(parts[0] + " " + (parts.length > 1 ? parts[1] : ""));
            if (timestamp != null) {
                logEntry.setTimestamp(timestamp);
            }
        }
    }
    
    /**
     * Parses JSON log format.
     */
    private void parseJsonLog(LogEntry logEntry, String jsonLog) {
        try {
            // This would use ObjectMapper in real implementation
            // For now, basic JSON parsing
            if (jsonLog.contains("\"level\"")) {
                String level = extractJsonField(jsonLog, "level");
                logEntry.setLevel(level);
            }
            
            if (jsonLog.contains("\"message\"")) {
                String message = extractJsonField(jsonLog, "message");
                logEntry.setMessage(message);
            }
            
            if (jsonLog.contains("\"timestamp\"")) {
                String timestamp = extractJsonField(jsonLog, "timestamp");
                logEntry.setTimestamp(parseTimestamp(timestamp));
            }
            
        } catch (Exception e) {
            logger.warn("Failed to parse JSON log: {}", jsonLog, e);
            logEntry.setMessage(jsonLog);
        }
    }
    
    /**
     * Extracts field value from JSON string (simplified).
     */
    private String extractJsonField(String json, String fieldName) {
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
    
    /**
     * Parses timestamp from various formats.
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        // Common timestamp formats
        String[] formats = {
            "yyyy-MM-dd HH:mm:ss,SSS",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "MMM dd HH:mm:ss",
            "dd/MMM/yyyy:HH:mm:ss Z"
        };
        
        for (String format : formats) {
            try {
                return LocalDateTime.parse(timestampStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }
        
        logger.debug("Could not parse timestamp: {}", timestampStr);
        return LocalDateTime.now();
    }
    
    /**
     * Enriches log entry with additional metadata.
     */
    private void enrichLogEntry(LogEntry logEntry) {
        // Set processing time
        logEntry.setProcessingTime(System.currentTimeMillis());
        
        // Set default values if missing
        if (logEntry.getLevel() == null) {
            logEntry.setLevel("INFO");
        }
        
        if (logEntry.getEnvironment() == null) {
            logEntry.setEnvironment("unknown");
        }
        
        if (logEntry.getHost() == null) {
            logEntry.setHost("localhost");
        }
        
        // Add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ingestionTime", LocalDateTime.now().toString());
        metadata.put("processingVersion", "1.0");
        logEntry.setMetadata(metadata);
        
        // Add tags based on content
        Set<String> tags = new HashSet<>();
        if (logEntry.isError()) {
            tags.add("error");
        }
        if (logEntry.hasStackTrace()) {
            tags.add("exception");
        }
        if (logEntry.isHttpLog()) {
            tags.add("http");
        }
        
        Map<String, String> tagMap = new HashMap<>();
        tags.forEach(tag -> tagMap.put(tag, "true"));
        logEntry.setTags(tagMap);
    }
    
    /**
     * Gets ingestion statistics.
     */
    public Map<String, Object> getIngestionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessed", processedCount.get());
        stats.put("totalErrors", errorCount.get());
        stats.put("sourceStats", sourceStats);
        stats.put("successRate", calculateSuccessRate());
        stats.put("timestamp", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * Calculates success rate.
     */
    private double calculateSuccessRate() {
        long total = processedCount.get() + errorCount.get();
        return total > 0 ? (double) processedCount.get() / total * 100 : 100.0;
    }
    
    /**
     * Resets statistics.
     */
    public void resetStats() {
        processedCount.set(0);
        errorCount.set(0);
        sourceStats.clear();
        logger.info("Ingestion statistics reset");
    }
}
