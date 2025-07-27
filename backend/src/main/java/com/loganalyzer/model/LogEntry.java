package com.loganalyzer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a log entry in the system.
 * This is the core entity that stores individual log messages with metadata.
 *
 * Stored in Elasticsearch for fast full-text search and aggregations.
 * Also supports JPA for local development with H2 database.
 * Indexed with optimized mappings for search performance.
 */
@Entity
@Table(name = "log_entries", indexes = {
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_level", columnList = "level"),
    @Index(name = "idx_log_source", columnList = "source"),
    @Index(name = "idx_log_application", columnList = "application"),
    @Index(name = "idx_log_host", columnList = "host")
})
@Document(indexName = "logs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

    @jakarta.persistence.Id
    @Column(name = "id", length = 255)
    private String id;
    
    @Column(name = "timestamp", nullable = false)
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    @Column(name = "level", length = 50)
    @Field(type = FieldType.Keyword)
    private String level;

    @Column(name = "message", columnDefinition = "TEXT")
    @Field(type = FieldType.Text, analyzer = "standard")
    private String message;

    @Column(name = "source", length = 255)
    @Field(type = FieldType.Keyword)
    private String source;

    @Column(name = "host", length = 255)
    @Field(type = FieldType.Keyword)
    private String host;

    @Column(name = "application", length = 255)
    @Field(type = FieldType.Keyword)
    private String application;

    @Column(name = "environment", length = 100)
    @Field(type = FieldType.Keyword)
    private String environment;

    @Column(name = "logger", length = 500)
    @Field(type = FieldType.Keyword)
    private String logger;
    
    @Column(name = "thread", length = 255)
    @Field(type = FieldType.Keyword)
    private String thread;

    @Column(columnDefinition = "TEXT")
    @Field(type = FieldType.Text)
    private String stackTrace;

    @ElementCollection
    @CollectionTable(name = "log_entry_metadata", joinColumns = @JoinColumn(name = "log_entry_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 1000)
    @Field(type = FieldType.Object)
    private Map<String, String> metadata;

    @ElementCollection
    @CollectionTable(name = "log_entry_tags", joinColumns = @JoinColumn(name = "log_entry_id"))
    @MapKeyColumn(name = "tags_key")
    @Column(name = "tag")
    @Field(type = FieldType.Object)
    private Map<String, String> tags;

    @Column(name = "processing_time")
    @Field(type = FieldType.Long)
    private Long processingTime;

    @Column(name = "parsed")
    @Field(type = FieldType.Boolean)
    private Boolean parsed;

    @Column(name = "original_format", length = 100)
    @Field(type = FieldType.Keyword)
    private String originalFormat;

    @Column(name = "severity")
    @Field(type = FieldType.Integer)
    private Integer severity;
    
    @Column(name = "processed_at")
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime processedAt;

    @Column(name = "category", length = 100)
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Column(name = "user_id", length = 255)
    @Field(type = FieldType.Text)
    private String userId;

    @Column(name = "session_id", length = 255)
    @Field(type = FieldType.Text)
    private String sessionId;

    @Column(name = "request_id", length = 255)
    @Field(type = FieldType.Text)
    private String requestId;

    @Column(name = "http_method", length = 10)
    @Field(type = FieldType.Keyword)
    private String httpMethod;

    @Column(name = "http_url", length = 2000)
    @Field(type = FieldType.Text)
    private String httpUrl;

    @Column(name = "http_status")
    @Field(type = FieldType.Integer)
    private Integer httpStatus;

    @Column(name = "response_time")
    @Field(type = FieldType.Long)
    private Long responseTime;
    
    // Constructors
    public LogEntry() {
        this.timestamp = LocalDateTime.now();
        this.parsed = false;
        this.severity = 0;
    }
    
    public LogEntry(String message, String level, String source) {
        this();
        this.message = message;
        this.level = level;
        this.source = source;
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
    
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    
    public Map<String, String> getTags() { return tags; }
    public void setTags(Map<String, String> tags) { this.tags = tags; }
    
    public Long getProcessingTime() { return processingTime; }
    public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
    
    public Boolean getParsed() { return parsed; }
    public void setParsed(Boolean parsed) { this.parsed = parsed; }
    
    public String getOriginalFormat() { return originalFormat; }
    public void setOriginalFormat(String originalFormat) { this.originalFormat = originalFormat; }
    
    public Integer getSeverity() { return severity; }
    public void setSeverity(Integer severity) { this.severity = severity; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    // Convenience method for Kafka service compatibility
    public int getSeverityScore() { return severity != null ? severity : 0; }
    public void setSeverityScore(int severityScore) { this.severity = severityScore; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    
    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
    
    public String getHttpUrl() { return httpUrl; }
    public void setHttpUrl(String httpUrl) { this.httpUrl = httpUrl; }
    
    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
    
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    
    // Utility methods
    public boolean isError() {
        return "ERROR".equalsIgnoreCase(level) || "FATAL".equalsIgnoreCase(level);
    }
    
    public boolean isWarning() {
        return "WARN".equalsIgnoreCase(level) || "WARNING".equalsIgnoreCase(level);
    }
    
    public boolean hasStackTrace() {
        return stackTrace != null && !stackTrace.trim().isEmpty();
    }
    
    public boolean isHttpLog() {
        return httpMethod != null && httpUrl != null;
    }

    /**
     * Adds metadata to the log entry.
     */
    public void addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * Adds a tag to the log entry.
     */
    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new java.util.HashMap<>();
        }
        this.tags.put(tag, "true");
    }

    @Override
    public String toString() {
        return String.format("LogEntry{id='%s', timestamp=%s, level='%s', message='%s', source='%s'}",
                           id, timestamp, level, message, source);
    }
}
