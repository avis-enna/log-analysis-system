package com.loganalyzer.controller;

import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.scheduling.annotation.Scheduled;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * WebSocket controller for real-time data streaming
 * Handles dashboard metrics, log updates, and system health
 */
@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private LogIngestionService logIngestionService;
    
    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    /**
     * Handle subscription requests from clients
     */
    @MessageMapping("/subscribe")
    @SendTo("/topic/subscribed")
    public Map<String, Object> handleSubscription(Map<String, Object> message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "subscribed");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("message", "Successfully subscribed to real-time updates");
        return response;
    }

    /**
     * Send real-time dashboard metrics every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    public void sendDashboardMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // Generate real-time metrics
            metrics.put("timestamp", LocalDateTime.now().toString());
            metrics.put("logsPerSecond", random.nextInt(50) + 10);
            metrics.put("errorsPerMinute", random.nextInt(20) + 5);
            metrics.put("cpuUsage", Math.round((random.nextDouble() * 30 + 40) * 10.0) / 10.0);
            metrics.put("memoryUsage", Math.round((random.nextDouble() * 20 + 60) * 10.0) / 10.0);
            metrics.put("diskUsage", Math.round((random.nextDouble() * 15 + 35) * 10.0) / 10.0);
            metrics.put("networkIO", random.nextInt(100000) + 50000);
            metrics.put("activeConnections", random.nextInt(100) + 20);

            messagingTemplate.convertAndSend("/topic/dashboard/metrics", metrics);
        } catch (Exception e) {
            System.err.println("Error sending dashboard metrics: " + e.getMessage());
        }
    }

    /**
     * Send real-time log entries every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    public void sendLogUpdates() {
        try {
            Map<String, Object> logUpdate = new HashMap<>();
            logUpdate.put("timestamp", LocalDateTime.now().toString());
            logUpdate.put("newLogsCount", random.nextInt(10) + 1);
            logUpdate.put("totalLogs", logEntryRepository.count());
            
            messagingTemplate.convertAndSend("/topic/logs/updates", logUpdate);
        } catch (Exception e) {
            System.err.println("Error sending log updates: " + e.getMessage());
        }
    }

    /**
     * Send system health updates every 15 seconds
     */
    @Scheduled(fixedRate = 15000)
    public void sendSystemHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("timestamp", LocalDateTime.now().toString());
            health.put("database", "healthy");
            health.put("api", "healthy");
            health.put("memory", random.nextDouble() > 0.9 ? "warning" : "healthy");
            health.put("disk", random.nextDouble() > 0.95 ? "warning" : "healthy");
            
            messagingTemplate.convertAndSend("/topic/system/health", health);
        } catch (Exception e) {
            System.err.println("Error sending system health: " + e.getMessage());
        }
    }

    /**
     * Send alert notifications
     */
    @Scheduled(fixedRate = 30000)
    public void sendAlertUpdates() {
        try {
            // Only send alert if random condition is met (simulate real alerts)
            if (random.nextDouble() > 0.7) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("timestamp", LocalDateTime.now().toString());
                alert.put("type", "warning");
                alert.put("title", "High Error Rate Detected");
                alert.put("message", "Error rate exceeded threshold in the last 5 minutes");
                alert.put("severity", "medium");
                
                messagingTemplate.convertAndSend("/topic/alerts/new", alert);
            }
        } catch (Exception e) {
            System.err.println("Error sending alert updates: " + e.getMessage());
        }
    }
}
