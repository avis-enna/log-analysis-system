package com.sivareddy.loganalysis.service;

import com.sivareddy.loganalysis.model.Alert;
import com.sivareddy.loganalysis.repository.AlertRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for managing alerts
 * 
 * @author Siva Reddy
 */
@Service
@Transactional
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    @Autowired
    private AlertRepository alertRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Create a new alert
     */
    public Alert createAlert(String type, String severity, String message, String sourceSystem, String sourceIp, Long logEntryId) {
        try {
            Alert alert = new Alert();
            alert.setType(type);
            alert.setSeverity(severity);
            alert.setMessage(message);
            alert.setSourceSystem(sourceSystem);
            alert.setSourceIp(sourceIp);
            alert.setLogEntryId(logEntryId);
            
            // Set acknowledgment requirement based on severity
            if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
                alert.setAcknowledgmentRequired(true);
            }
            
            Alert savedAlert = alertRepository.save(alert);
            
            // Send notification for high-severity alerts
            if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
                notificationService.sendAlertNotification(savedAlert);
            }
            
            logger.info("Created alert: {} - {} - {}", type, severity, message);
            return savedAlert;
            
        } catch (Exception e) {
            logger.error("Error creating alert", e);
            throw new RuntimeException("Failed to create alert", e);
        }
    }
    
    /**
     * Resolve an alert
     */
    public Alert resolveAlert(Long alertId, String resolvedBy, String resolutionNotes) {
        try {
            Optional<Alert> optionalAlert = alertRepository.findById(alertId);
            if (optionalAlert.isPresent()) {
                Alert alert = optionalAlert.get();
                alert.setResolved(true);
                alert.setResolvedBy(resolvedBy);
                alert.setResolvedAt(LocalDateTime.now());
                alert.setResolutionNotes(resolutionNotes);
                
                Alert savedAlert = alertRepository.save(alert);
                logger.info("Resolved alert: {}", alertId);
                return savedAlert;
            } else {
                throw new RuntimeException("Alert not found: " + alertId);
            }
        } catch (Exception e) {
            logger.error("Error resolving alert: {}", alertId, e);
            throw new RuntimeException("Failed to resolve alert", e);
        }
    }
    
    /**
     * Acknowledge an alert
     */
    public Alert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        try {
            Optional<Alert> optionalAlert = alertRepository.findById(alertId);
            if (optionalAlert.isPresent()) {
                Alert alert = optionalAlert.get();
                alert.setAcknowledged(true);
                alert.setAcknowledgedBy(acknowledgedBy);
                alert.setAcknowledgedAt(LocalDateTime.now());
                
                Alert savedAlert = alertRepository.save(alert);
                logger.info("Acknowledged alert: {}", alertId);
                return savedAlert;
            } else {
                throw new RuntimeException("Alert not found: " + alertId);
            }
        } catch (Exception e) {
            logger.error("Error acknowledging alert: {}", alertId, e);
            throw new RuntimeException("Failed to acknowledge alert", e);
        }
    }
    
    /**
     * Get all unresolved alerts
     */
    public List<Alert> getUnresolvedAlerts() {
        return alertRepository.findByResolvedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
    
    /**
     * Get alerts that need acknowledgment
     */
    public List<Alert> getAlertsRequiringAcknowledgment() {
        return alertRepository.findByAcknowledgmentRequiredTrueAndAcknowledgedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent();
    }
    
    /**
     * Get recent high-severity alerts
     */
    public List<Alert> getRecentHighSeverityAlerts(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return alertRepository.findRecentHighSeverityAlerts(since);
    }
    
    /**
     * Get alert statistics
     */
    public AlertStatistics getAlertStatistics() {
        AlertStatistics stats = new AlertStatistics();
        
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        
        stats.setTotalAlerts(alertRepository.count());
        stats.setUnresolvedAlerts(alertRepository.countUnresolvedAlerts());
        stats.setCriticalAlerts(alertRepository.countBySeveritySince("CRITICAL", last24Hours));
        stats.setHighAlerts(alertRepository.countBySeveritySince("HIGH", last24Hours));
        stats.setMediumAlerts(alertRepository.countBySeveritySince("MEDIUM", last24Hours));
        stats.setLowAlerts(alertRepository.countBySeveritySince("LOW", last24Hours));
        
        return stats;
    }
    
    /**
     * Inner class for alert statistics
     */
    public static class AlertStatistics {
        private Long totalAlerts;
        private Long unresolvedAlerts;
        private Long criticalAlerts;
        private Long highAlerts;
        private Long mediumAlerts;
        private Long lowAlerts;
        
        // Getters and setters
        public Long getTotalAlerts() { return totalAlerts; }
        public void setTotalAlerts(Long totalAlerts) { this.totalAlerts = totalAlerts; }
        
        public Long getUnresolvedAlerts() { return unresolvedAlerts; }
        public void setUnresolvedAlerts(Long unresolvedAlerts) { this.unresolvedAlerts = unresolvedAlerts; }
        
        public Long getCriticalAlerts() { return criticalAlerts; }
        public void setCriticalAlerts(Long criticalAlerts) { this.criticalAlerts = criticalAlerts; }
        
        public Long getHighAlerts() { return highAlerts; }
        public void setHighAlerts(Long highAlerts) { this.highAlerts = highAlerts; }
        
        public Long getMediumAlerts() { return mediumAlerts; }
        public void setMediumAlerts(Long mediumAlerts) { this.mediumAlerts = mediumAlerts; }
        
        public Long getLowAlerts() { return lowAlerts; }
        public void setLowAlerts(Long lowAlerts) { this.lowAlerts = lowAlerts; }
    }
}
