package com.loganalyzer.controller;

import com.loganalyzer.dto.LogEntryDTO;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.service.KafkaLogIngestionService;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.RedisLogCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced log ingestion controller with Kafka, Redis, and role-based access
 */
@RestController
@RequestMapping("/logs")
@CrossOrigin(origins = "*")
public class EnhancedLogController {
    
    @Autowired
    private LogIngestionService logIngestionService;
    
    @Autowired
    private KafkaLogIngestionService kafkaLogIngestionService;
    
    @Autowired
    private RedisLogCacheService redisLogCacheService;
    
    /**
     * Ingest single log entry via Kafka (Developer/Admin access)
     */
    @PostMapping("/ingest/kafka")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> ingestLogViaKafka(@RequestBody LogEntryDTO logEntryDTO) {
        try {
            // Publish to Kafka for processing
            kafkaLogIngestionService.publishLog(logEntryDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "accepted");
            response.put("message", "Log published to Kafka for processing");
            response.put("source", logEntryDTO.getSource());
            response.put("level", logEntryDTO.getLevel());
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Ingest single log entry directly (Developer/Admin access)
     */
    @PostMapping("/ingest/direct")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> ingestLogDirect(@RequestBody Map<String, String> logData) {
        try {
            String rawLog = logData.get("message");
            String source = logData.getOrDefault("source", "api");
            
            if (rawLog == null || rawLog.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Log message is required"));
            }
            
            logIngestionService.ingestLog(rawLog, source);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Log ingested successfully");
            response.put("source", source);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get cached log by ID (All authenticated users)
     */
    @GetMapping("/cached/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasRole('QA')")
    public ResponseEntity<Map<String, Object>> getCachedLog(@PathVariable Long id) {
        try {
            Optional<LogEntry> logEntry = redisLogCacheService.getCachedLog(id);
            
            if (logEntry.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "status", "found",
                    "log", logEntry.get(),
                    "source", "cache"
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get recent logs from cache (All authenticated users)
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasRole('QA')")
    public ResponseEntity<Map<String, Object>> getRecentLogs(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            List<LogEntry> recentLogs = redisLogCacheService.getRecentLogs(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", recentLogs);
            response.put("count", recentLogs.size());
            response.put("source", "cache");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get recent error logs from cache (All authenticated users)
     */
    @GetMapping("/recent/errors") 
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasRole('QA')")
    public ResponseEntity<Map<String, Object>> getRecentErrorLogs(
            @RequestParam(defaultValue = "25") int limit) {
        try {
            List<LogEntry> errorLogs = redisLogCacheService.getRecentErrorLogs(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", errorLogs);
            response.put("count", errorLogs.size());
            response.put("source", "cache");
            response.put("type", "errors");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get real-time statistics from cache (All authenticated users)
     */
    @GetMapping("/stats/realtime")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasRole('QA')")
    public ResponseEntity<Map<String, Object>> getRealtimeStats() {
        try {
            Map<String, Object> stats = redisLogCacheService.getRealtimeStats();
            stats.put("source", "redis-cache");
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get log counts by source (All authenticated users)
     */
    @GetMapping("/stats/sources")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasRole('QA')")
    public ResponseEntity<Map<String, Object>> getSourceStats() {
        try {
            Map<String, Long> sourceCounts = redisLogCacheService.getLogCountsBySource();
            
            Map<String, Object> response = new HashMap<>();
            response.put("sources", sourceCounts);
            response.put("totalSources", sourceCounts.size());
            response.put("source", "cache");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get hourly statistics (All authenticated users)
     */
    @GetMapping("/stats/hourly")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER') or hasRole('QA')")
    public ResponseEntity<Map<String, Object>> getHourlyStats(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<Map<String, Object>> hourlyStats = redisLogCacheService.getHourlyStats(hours);
            
            Map<String, Object> response = new HashMap<>();
            response.put("statistics", hourlyStats);
            response.put("hours", hours);
            response.put("source", "cache");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Health check for services (Admin only)
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getServiceHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // Check Kafka health
            boolean kafkaHealthy = kafkaLogIngestionService.isKafkaHealthy();
            health.put("kafka", Map.of("status", kafkaHealthy ? "UP" : "DOWN"));
            
            // Check Redis health
            boolean redisHealthy = redisLogCacheService.isRedisHealthy();
            health.put("redis", Map.of("status", redisHealthy ? "UP" : "DOWN"));
            
            // Overall status
            boolean overallHealthy = kafkaHealthy && redisHealthy;
            health.put("overall", Map.of("status", overallHealthy ? "UP" : "DOWN"));
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Clear all cached logs (Admin only)
     */
    @DeleteMapping("/cache/clear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache() {
        try {
            redisLogCacheService.clearAllLogs();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All cached logs cleared"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Bulk ingest logs via Kafka (Developer/Admin access)
     */
    @PostMapping("/ingest/kafka/bulk")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEVELOPER')")
    public ResponseEntity<Map<String, Object>> bulkIngestViaKafka(@RequestBody List<LogEntryDTO> logEntries) {
        try {
            int published = 0;
            for (LogEntryDTO logEntry : logEntries) {
                kafkaLogIngestionService.publishLog(logEntry);
                published++;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "accepted");
            response.put("message", "Logs published to Kafka for processing");
            response.put("count", published);
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
