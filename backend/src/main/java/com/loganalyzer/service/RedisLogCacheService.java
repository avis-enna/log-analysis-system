package com.loganalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based caching service for fast log access and real-time metrics
 */
@Service
public class RedisLogCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisLogCacheService.class);
    
    // Redis key prefixes
    private static final String LOG_KEY_PREFIX = "log:";
    private static final String RECENT_LOGS_KEY = "recent_logs";
    private static final String ERROR_LOGS_KEY = "error_logs";
    private static final String LOG_STATS_KEY = "log_stats";
    private static final String SOURCE_STATS_KEY = "source_stats:";
    private static final String HOURLY_STATS_KEY = "hourly_stats:";
    
    // Cache TTL settings
    private static final Duration LOG_CACHE_TTL = Duration.ofHours(24);
    private static final Duration STATS_CACHE_TTL = Duration.ofMinutes(5);
    private static final int MAX_RECENT_LOGS = 1000;
    private static final int MAX_ERROR_LOGS = 500;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Cache a log entry for fast access
     */
    public void cacheLog(LogEntry logEntry) {
        try {
            String logKey = LOG_KEY_PREFIX + logEntry.getId();
            String logJson = objectMapper.writeValueAsString(logEntry);
            
            // Cache the log entry
            redisTemplate.opsForValue().set(logKey, logJson, LOG_CACHE_TTL);
            
            // Add to recent logs sorted set (by timestamp)
            double timestamp = logEntry.getTimestamp().toEpochSecond(ZoneOffset.UTC);
            redisTemplate.opsForZSet().add(RECENT_LOGS_KEY, logKey, timestamp);
            
            // Maintain max size of recent logs
            redisTemplate.opsForZSet().removeRange(RECENT_LOGS_KEY, 0, -MAX_RECENT_LOGS - 1);
            
            // Cache error logs separately
            if (isErrorLevel(logEntry.getLevel())) {
                redisTemplate.opsForZSet().add(ERROR_LOGS_KEY, logKey, timestamp);
                redisTemplate.opsForZSet().removeRange(ERROR_LOGS_KEY, 0, -MAX_ERROR_LOGS - 1);
            }
            
            // Update real-time statistics
            updateRealtimeStats(logEntry);
            
            logger.debug("Cached log entry with ID: {}", logEntry.getId());
            
        } catch (Exception e) {
            logger.error("Error caching log entry: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get cached log by ID
     */
    public Optional<LogEntry> getCachedLog(Long logId) {
        try {
            String logKey = LOG_KEY_PREFIX + logId;
            String logJson = redisTemplate.opsForValue().get(logKey);
            
            if (logJson != null) {
                LogEntry logEntry = objectMapper.readValue(logJson, LogEntry.class);
                return Optional.of(logEntry);
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving cached log: {}", e.getMessage(), e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get recent logs from cache
     */
    public List<LogEntry> getRecentLogs(int limit) {
        try {
            Set<String> logKeys = redisTemplate.opsForZSet()
                .reverseRange(RECENT_LOGS_KEY, 0, limit - 1);
            
            return getLogsByKeys(logKeys);
            
        } catch (Exception e) {
            logger.error("Error retrieving recent logs from cache: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get recent error logs from cache
     */
    public List<LogEntry> getRecentErrorLogs(int limit) {
        try {
            Set<String> logKeys = redisTemplate.opsForZSet()
                .reverseRange(ERROR_LOGS_KEY, 0, limit - 1);
            
            return getLogsByKeys(logKeys);
            
        } catch (Exception e) {
            logger.error("Error retrieving recent error logs from cache: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get real-time log statistics from cache
     */
    public Map<String, Object> getRealtimeStats() {
        try {
            Map<Object, Object> statsMap = redisTemplate.opsForHash().entries(LOG_STATS_KEY);
            Map<String, Object> stats = new HashMap<>();
            
            for (Map.Entry<Object, Object> entry : statsMap.entrySet()) {
                stats.put(entry.getKey().toString(), entry.getValue());
            }
            
            // Add current timestamp
            stats.put("lastUpdate", LocalDateTime.now().toString());
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Error retrieving realtime stats from cache: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get log count by source from cache
     */
    public Map<String, Long> getLogCountsBySource() {
        try {
            Set<String> sourceKeys = redisTemplate.keys(SOURCE_STATS_KEY + "*");
            Map<String, Long> sourceCounts = new HashMap<>();
            
            for (String key : sourceKeys) {
                String source = key.substring(SOURCE_STATS_KEY.length());
                String countStr = redisTemplate.opsForValue().get(key);
                if (countStr != null) {
                    sourceCounts.put(source, Long.parseLong(countStr));
                }
            }
            
            return sourceCounts;
            
        } catch (Exception e) {
            logger.error("Error retrieving source counts from cache: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Get hourly log statistics
     */
    public List<Map<String, Object>> getHourlyStats(int hours) {
        try {
            List<Map<String, Object>> hourlyStats = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            for (int i = 0; i < hours; i++) {
                LocalDateTime hour = now.minusHours(i);
                String hourKey = HOURLY_STATS_KEY + hour.getHour();
                
                Map<Object, Object> stats = redisTemplate.opsForHash().entries(hourKey);
                Map<String, Object> hourlyData = new HashMap<>();
                hourlyData.put("hour", hour.getHour());
                hourlyData.put("timestamp", hour.toString());
                
                for (Map.Entry<Object, Object> entry : stats.entrySet()) {
                    hourlyData.put(entry.getKey().toString(), entry.getValue());
                }
                
                hourlyStats.add(hourlyData);
            }
            
            return hourlyStats;
            
        } catch (Exception e) {
            logger.error("Error retrieving hourly stats from cache: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Clear all cached logs (useful for testing)
     */
    public void clearAllLogs() {
        try {
            Set<String> logKeys = redisTemplate.keys(LOG_KEY_PREFIX + "*");
            if (!logKeys.isEmpty()) {
                redisTemplate.delete(logKeys);
            }
            
            redisTemplate.delete(RECENT_LOGS_KEY);
            redisTemplate.delete(ERROR_LOGS_KEY);
            redisTemplate.delete(LOG_STATS_KEY);
            
            Set<String> sourceKeys = redisTemplate.keys(SOURCE_STATS_KEY + "*");
            if (!sourceKeys.isEmpty()) {
                redisTemplate.delete(sourceKeys);
            }
            
            Set<String> hourlyKeys = redisTemplate.keys(HOURLY_STATS_KEY + "*");
            if (!hourlyKeys.isEmpty()) {
                redisTemplate.delete(hourlyKeys);
            }
            
            logger.info("Cleared all cached logs and statistics");
            
        } catch (Exception e) {
            logger.error("Error clearing cached logs: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Update real-time statistics
     */
    private void updateRealtimeStats(LogEntry logEntry) {
        try {
            // Update global stats
            redisTemplate.opsForHash().increment(LOG_STATS_KEY, "totalLogs", 1);
            
            if (isErrorLevel(logEntry.getLevel())) {
                redisTemplate.opsForHash().increment(LOG_STATS_KEY, "totalErrors", 1);
            } else if (isWarningLevel(logEntry.getLevel())) {
                redisTemplate.opsForHash().increment(LOG_STATS_KEY, "totalWarnings", 1);
            }
            
            // Update source stats
            String sourceKey = SOURCE_STATS_KEY + logEntry.getSource();
            redisTemplate.opsForValue().increment(sourceKey, 1);
            redisTemplate.expire(sourceKey, STATS_CACHE_TTL);
            
            // Update hourly stats
            int currentHour = LocalDateTime.now().getHour();
            String hourlyKey = HOURLY_STATS_KEY + currentHour;
            redisTemplate.opsForHash().increment(hourlyKey, "count", 1);
            
            if (isErrorLevel(logEntry.getLevel())) {
                redisTemplate.opsForHash().increment(hourlyKey, "errors", 1);
            }
            
            redisTemplate.expire(hourlyKey, Duration.ofHours(25)); // Keep for 25 hours
            
            // Set TTL for stats
            redisTemplate.expire(LOG_STATS_KEY, STATS_CACHE_TTL);
            
        } catch (Exception e) {
            logger.error("Error updating realtime stats: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get logs by their Redis keys
     */
    private List<LogEntry> getLogsByKeys(Set<String> logKeys) {
        List<LogEntry> logs = new ArrayList<>();
        
        if (logKeys != null && !logKeys.isEmpty()) {
            List<String> logJsonList = redisTemplate.opsForValue().multiGet(logKeys);
            
            for (String logJson : logJsonList) {
                if (logJson != null) {
                    try {
                        LogEntry logEntry = objectMapper.readValue(logJson, LogEntry.class);
                        logs.add(logEntry);
                    } catch (Exception e) {
                        logger.error("Error deserializing cached log: {}", e.getMessage());
                    }
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Check if log level is an error level
     */
    private boolean isErrorLevel(String level) {
        return level != null && (
            level.equalsIgnoreCase("ERROR") ||
            level.equalsIgnoreCase("FATAL") ||
            level.equalsIgnoreCase("CRITICAL")
        );
    }
    
    /**
     * Check if log level is a warning level
     */
    private boolean isWarningLevel(String level) {
        return level != null && (
            level.equalsIgnoreCase("WARN") ||
            level.equalsIgnoreCase("WARNING")
        );
    }
    
    /**
     * Health check for Redis connectivity
     */
    public boolean isRedisHealthy() {
        try {
            redisTemplate.opsForValue().set("health_check", "ping", Duration.ofSeconds(5));
            String result = redisTemplate.opsForValue().get("health_check");
            redisTemplate.delete("health_check");
            return "ping".equals(result);
        } catch (Exception e) {
            logger.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }
}
