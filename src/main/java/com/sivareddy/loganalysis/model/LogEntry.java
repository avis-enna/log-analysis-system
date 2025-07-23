package com.sivareddy.loganalysis.model;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity class representing a log entry
 * 
 * @author Siva Reddy
 */
@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_level", columnList = "level"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_ip_address", columnList = "ip_address")
})
public class LogEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String level;
    
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String source;
    
    @NotBlank
    @Size(max = 5000)
    @Column(nullable = false, length = 5000)
    private String message;
    
    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Size(max = 100)
    @Column(name = "request_id", length = 100)
    private String requestId;
    
    @Size(max = 50)
    @Column(name = "session_id", length = 50)
    private String sessionId;
    
    @Size(max = 100)
    @Column(name = "thread_name", length = 100)
    private String threadName;
    
    @Size(max = 200)
    @Column(name = "logger_name", length = 200)
    private String loggerName;
    
    @Column(name = "response_time")
    private Long responseTime;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Column(name = "processed", nullable = false)
    private Boolean processed = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor
    public LogEntry() {}
    
    // Constructor with required fields
    public LogEntry(LocalDateTime timestamp, String level, String source, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.source = source;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getThreadName() { return threadName; }
    public void setThreadName(String threadName) { this.threadName = threadName; }
    
    public String getLoggerName() { return loggerName; }
    public void setLoggerName(String loggerName) { this.loggerName = loggerName; }
    
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    
    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", level='" + level + '\'' +
                ", source='" + source + '\'' +
                ", message='" + message + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
