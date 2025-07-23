package com.loganalyzer.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents an alert in the system.
 * Alerts are triggered when certain conditions are met in the log data.
 * 
 * Stored in PostgreSQL for ACID compliance and complex queries.
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_created", columnList = "createdAt"),
    @Index(name = "idx_alert_rule", columnList = "ruleId")
})
@EntityListeners(AuditingEntityListener.class)
public class Alert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertStatus status;
    
    @NotNull
    @Column(name = "rule_id", nullable = false)
    private Long ruleId;
    
    @Column(name = "rule_name")
    private String ruleName;
    
    @Column(name = "triggered_by")
    private String triggeredBy;
    
    @Column(name = "trigger_count")
    private Integer triggerCount = 1;
    
    @Column(name = "first_occurrence")
    private LocalDateTime firstOccurrence;
    
    @Column(name = "last_occurrence")
    private LocalDateTime lastOccurrence;
    
    @Column(name = "acknowledged_by")
    private String acknowledgedBy;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "resolved_by")
    private String resolvedBy;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    @ElementCollection
    @CollectionTable(name = "alert_metadata", joinColumns = @JoinColumn(name = "alert_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    @ElementCollection
    @CollectionTable(name = "alert_tags", joinColumns = @JoinColumn(name = "alert_id"))
    @Column(name = "tag")
    private java.util.Set<String> tags;
    
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;
    
    @Column(name = "notification_attempts")
    private Integer notificationAttempts = 0;
    
    @Column(name = "last_notification_attempt")
    private LocalDateTime lastNotificationAttempt;
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Alert() {
        this.status = AlertStatus.OPEN;
        this.firstOccurrence = LocalDateTime.now();
        this.lastOccurrence = LocalDateTime.now();
    }
    
    public Alert(String title, AlertSeverity severity, Long ruleId) {
        this();
        this.title = title;
        this.severity = severity;
        this.ruleId = ruleId;
    }
    
    // Enums
    public enum AlertSeverity {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);
        
        private final int level;
        
        AlertSeverity(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    public enum AlertStatus {
        OPEN, ACKNOWLEDGED, RESOLVED, SUPPRESSED, CLOSED
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public AlertSeverity getSeverity() { return severity; }
    public void setSeverity(AlertSeverity severity) { this.severity = severity; }
    
    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }
    
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    
    public Integer getTriggerCount() { return triggerCount; }
    public void setTriggerCount(Integer triggerCount) { this.triggerCount = triggerCount; }
    
    public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
    public void setFirstOccurrence(LocalDateTime firstOccurrence) { this.firstOccurrence = firstOccurrence; }
    
    public LocalDateTime getLastOccurrence() { return lastOccurrence; }
    public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }
    
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public java.util.Set<String> getTags() { return tags; }
    public void setTags(java.util.Set<String> tags) { this.tags = tags; }
    
    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }
    
    public Integer getNotificationAttempts() { return notificationAttempts; }
    public void setNotificationAttempts(Integer notificationAttempts) { this.notificationAttempts = notificationAttempts; }
    
    public LocalDateTime getLastNotificationAttempt() { return lastNotificationAttempt; }
    public void setLastNotificationAttempt(LocalDateTime lastNotificationAttempt) { this.lastNotificationAttempt = lastNotificationAttempt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // Utility methods
    public void acknowledge(String acknowledgedBy) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = LocalDateTime.now();
    }
    
    public void resolve(String resolvedBy, String notes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
    }
    
    public void incrementTriggerCount() {
        this.triggerCount++;
        this.lastOccurrence = LocalDateTime.now();
    }
    
    public boolean isOpen() {
        return status == AlertStatus.OPEN;
    }
    
    public boolean isResolved() {
        return status == AlertStatus.RESOLVED || status == AlertStatus.CLOSED;
    }
    
    public boolean isCritical() {
        return severity == AlertSeverity.CRITICAL;
    }
    
    @Override
    public String toString() {
        return String.format("Alert{id=%d, title='%s', severity=%s, status=%s, ruleId=%d}", 
                           id, title, severity, status, ruleId);
    }
}
