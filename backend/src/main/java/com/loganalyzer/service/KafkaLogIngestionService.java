package com.loganalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.LogEntryDTO;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka-based log ingestion service for high-throughput log processing
 */
@Service
public class KafkaLogIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaLogIngestionService.class);
    private static final String LOG_TOPIC = "logs";
    private static final String PROCESSED_LOG_TOPIC = "processed-logs";
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    @Autowired
    private AlertGenerationService alertGenerationService;
    
    @Autowired
    private RedisLogCacheService redisLogCacheService;
    
    /**
     * Publish log to Kafka topic for processing
     */
    public void publishLog(LogEntryDTO logEntryDTO) {
        try {
            String logJson = objectMapper.writeValueAsString(logEntryDTO);
            
            CompletableFuture<SendResult<String, String>> future = 
                kafkaTemplate.send(LOG_TOPIC, logEntryDTO.getSource(), logJson);
                
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Log published successfully to topic {} with offset {}",
                        LOG_TOPIC, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish log to Kafka: {}", ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error serializing log for Kafka: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Consume logs from Kafka topic and process them
     */
    @KafkaListener(topics = LOG_TOPIC, groupId = "log-processor-group")
    public void consumeLog(ConsumerRecord<String, String> record) {
        try {
            logger.debug("Consuming log from Kafka: partition={}, offset={}, key={}",
                record.partition(), record.offset(), record.key());
            
            LogEntryDTO logEntryDTO = objectMapper.readValue(record.value(), LogEntryDTO.class);
            
            // Process the log entry
            LogEntry processedLog = processLogEntry(logEntryDTO);
            
            // Cache in Redis for real-time access
            redisLogCacheService.cacheLog(processedLog);
            
            // Generate alerts if needed
            alertGenerationService.analyzeLogAndGenerateAlerts(processedLog);
            
            // Publish to processed logs topic for downstream consumers
            publishProcessedLog(processedLog);
            
            logger.debug("Successfully processed log entry with ID: {}", processedLog.getId());
            
        } catch (Exception e) {
            logger.error("Error processing log from Kafka: {}", e.getMessage(), e);
            // In production, you might want to send to a dead letter topic
        }
    }
    
    /**
     * Process and persist log entry
     */
    private LogEntry processLogEntry(LogEntryDTO logEntryDTO) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage(logEntryDTO.getMessage());
        logEntry.setLevel(logEntryDTO.getLevel());
        logEntry.setSource(logEntryDTO.getSource());
        logEntry.setApplication(logEntryDTO.getApplication());
        logEntry.setEnvironment(logEntryDTO.getEnvironment());
        logEntry.setCategory(logEntryDTO.getCategory());
        logEntry.setTimestamp(logEntryDTO.getTimestamp() != null ? 
            logEntryDTO.getTimestamp() : LocalDateTime.now());
        
        // Enrich log entry with metadata
        enrichLogEntry(logEntry);
        
        // Save to database
        return logEntryRepository.save(logEntry);
    }
    
    /**
     * Enrich log entry with additional metadata
     */
    private void enrichLogEntry(LogEntry logEntry) {
        // Add processing timestamp
        logEntry.setProcessedAt(LocalDateTime.now());
        
        // Set default values if missing
        if (logEntry.getApplication() == null || logEntry.getApplication().trim().isEmpty()) {
            logEntry.setApplication("unknown");
        }
        
        if (logEntry.getEnvironment() == null || logEntry.getEnvironment().trim().isEmpty()) {
            logEntry.setEnvironment("production"); // default to production
        }
        
        if (logEntry.getCategory() == null || logEntry.getCategory().trim().isEmpty()) {
            logEntry.setCategory(inferCategory(logEntry.getMessage(), logEntry.getLevel()));
        }
        
        // Add severity score based on level and content
        logEntry.setSeverityScore(calculateSeverityScore(logEntry));
    }
    
    /**
     * Infer category based on log message and level
     */
    private String inferCategory(String message, String level) {
        if (message == null) return "general";
        
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("security") || lowerMessage.contains("auth") || 
            lowerMessage.contains("login") || lowerMessage.contains("unauthorized")) {
            return "security";
        }
        
        if (lowerMessage.contains("deploy") || lowerMessage.contains("startup") || 
            lowerMessage.contains("shutdown") || lowerMessage.contains("restart")) {
            return "deployment";
        }
        
        if (lowerMessage.contains("performance") || lowerMessage.contains("slow") || 
            lowerMessage.contains("timeout") || lowerMessage.contains("response time")) {
            return "performance";
        }
        
        if (lowerMessage.contains("database") || lowerMessage.contains("sql") || 
            lowerMessage.contains("connection")) {
            return "database";
        }
        
        if (lowerMessage.contains("network") || lowerMessage.contains("http") || 
            lowerMessage.contains("api")) {
            return "network";
        }
        
        return "application";
    }
    
    /**
     * Calculate severity score based on log level and content
     */
    private int calculateSeverityScore(LogEntry logEntry) {
        int baseScore;
        switch (logEntry.getLevel().toUpperCase()) {
            case "CRITICAL":
            case "FATAL":
                baseScore = 90;
                break;
            case "ERROR":
                baseScore = 70;
                break;
            case "WARN":
            case "WARNING":
                baseScore = 50;
                break;
            case "INFO":
                baseScore = 30;
                break;
            case "DEBUG":
                baseScore = 10;
                break;
            default:
                baseScore = 20;
                break;
        }
        
        // Increase score based on keywords
        String message = logEntry.getMessage().toLowerCase();
        if (message.contains("failed") || message.contains("exception") || 
            message.contains("error")) {
            baseScore += 10;
        }
        
        if (message.contains("timeout") || message.contains("connection") || 
            message.contains("unavailable")) {
            baseScore += 15;
        }
        
        if (message.contains("security") || message.contains("unauthorized") || 
            message.contains("breach")) {
            baseScore += 20;
        }
        
        return Math.min(baseScore, 100); // Cap at 100
    }
    
    /**
     * Publish processed log to downstream topic
     */
    private void publishProcessedLog(LogEntry logEntry) {
        try {
            String processedLogJson = objectMapper.writeValueAsString(logEntry);
            
            kafkaTemplate.send(PROCESSED_LOG_TOPIC, 
                logEntry.getSource() + "-" + logEntry.getId(), processedLogJson);
                
        } catch (Exception e) {
            logger.error("Error publishing processed log to Kafka: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Health check for Kafka connectivity
     */
    public boolean isKafkaHealthy() {
        try {
            // Send a test message to verify connectivity
            kafkaTemplate.send("health-check", "test", "ping").get();
            return true;
        } catch (Exception e) {
            logger.warn("Kafka health check failed: {}", e.getMessage());
            return false;
        }
    }
}
