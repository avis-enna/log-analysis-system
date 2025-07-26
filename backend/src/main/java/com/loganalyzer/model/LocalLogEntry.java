package com.loganalyzer.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * JPA entity for log entries used in local testing with H2 database.
 * This is a simplified version of LogEntry for local development without Elasticsearch.
 */
@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_level", columnList = "level"),
    @Index(name = "idx_log_application", columnList = "application"),
    @Index(name = "idx_log_host", columnList = "host")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalLogEntry {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    @Column(length = 20)
    private String level;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(length = 100)
    private String source;
    
    @Column(length = 100)
    private String host;
    
    @Column(length = 100)
    private String application;
    
    @Column(length = 50)
    private String environment;
    
    @Column(length = 200)
    private String logger;
    
    @Column(length = 100)
    private String thread;
    
    @Column(length = 500)
    private String traceId;
    
    @Column(length = 500)
    private String spanId;
    
    @Column(columnDefinition = "TEXT")
    private String stackTrace;
    
    @ElementCollection
    @CollectionTable(name = "log_entry_tags", joinColumns = @JoinColumn(name = "log_entry_id"))
    @Column(name = "tag")
    private Map<String, String> tags = new HashMap<>();
    
    @ElementCollection
    @CollectionTable(name = "log_entry_metadata", joinColumns = @JoinColumn(name = "log_entry_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata = new HashMap<>();
    
    // Constructors
    public LocalLogEntry() {}
    
    public LocalLogEntry(String id, LocalDateTime timestamp, String level, String message) {
        this.id = id;
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    
    public String getApplication() { return application; }
    public void setApplication(String application) { this.application = application; }
    
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    
    public String getLogger() { return logger; }
    public void setLogger(String logger) { this.logger = logger; }
    
    public String getThread() { return thread; }
    public void setThread(String thread) { this.thread = thread; }
    
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    
    public String getSpanId() { return spanId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    
    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    @Override
    public String toString() {
        return "LocalLogEntry{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", application='" + application + '\'' +
                ", host='" + host + '\'' +
                '}';
    }
}
