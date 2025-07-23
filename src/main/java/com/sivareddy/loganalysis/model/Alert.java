package com.sivareddy.loganalysis.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class representing an alert
 * 
 * @author Siva Reddy
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_type", columnList = "type"),
    @Index(name = "idx_alert_severity", columnList = "severity"),
    @Index(name = "idx_alert_resolved", columnList = "resolved"),
    @Index(name = "idx_alert_created", columnList = "created_at")
})
public class Alert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String type;
    
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String severity;
    
    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Size(max = 2000)
    @Column(length = 2000)
    private String description;
    
    @Size(max = 100)
    @Column(name = "source_system", length = 100)
    private String sourceSystem;
    
    @Size(max = 45)
    @Column(name = "source_ip", length = 45)
    private String sourceIp;
    
    @Column(name = "log_entry_id")
    private Long logEntryId;
    
    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;
    
    @Size(max = 100)
    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;
    
    @Column(name = "resolved_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
    
    @Size(max = 500)
    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;
    
    @Column(name = "acknowledgment_required")
    private Boolean acknowledgmentRequired = false;
    
    @Column(name = "acknowledged")
    private Boolean acknowledged = false;
    
    @Size(max = 100)
    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;
    
    @Column(name = "acknowledged_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acknowledgedAt;
    
    @Column(name = "notification_sent")
    private Boolean notificationSent = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public Alert() {}
    
    // Constructor with required fields
    public Alert(String type, String severity, String message) {
        this.type = type;
        this.severity = severity;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    
    public Long getLogEntryId() { return logEntryId; }
    public void setLogEntryId(Long logEntryId) { this.logEntryId = logEntryId; }
    
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    
    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    
    public Boolean getAcknowledgmentRequired() { return acknowledgmentRequired; }
    public void setAcknowledgmentRequired(Boolean acknowledgmentRequired) { this.acknowledgmentRequired = acknowledgmentRequired; }
    
    public Boolean getAcknowledged() { return acknowledged; }
    public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }
    
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }
    
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    
    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", severity='" + severity + '\'' +
                ", message='" + message + '\'' +
                ", resolved=" + resolved +
                ", createdAt=" + createdAt +
                '}';
    }
}
