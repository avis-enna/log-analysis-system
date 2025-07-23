package com.loganalyzer.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a log entry in the system.
 * This is the core entity that stores individual log messages with metadata.
 * 
 * Stored in Elasticsearch for fast full-text search and aggregations.
 * Indexed with optimized mappings for search performance.
 */
@Document(indexName = "logs")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;
    
    @Field(type = FieldType.Keyword)
    private String level;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String message;
    
    @Field(type = FieldType.Keyword)
    private String source;
    
    @Field(type = FieldType.Keyword)
    private String host;
    
    @Field(type = FieldType.Keyword)
    private String application;
    
    @Field(type = FieldType.Keyword)
    private String environment;
    
    @Field(type = FieldType.Keyword)
    private String logger;
    
    @Field(type = FieldType.Keyword)
    private String thread;
    
    @Field(type = FieldType.Text)
    private String stackTrace;
    
    @Field(type = FieldType.Object)
    private Map<String, Object> metadata;
    
    @Field(type = FieldType.Object)
    private Map<String, String> tags;
    
    @Field(type = FieldType.Long)
    private Long processingTime;
    
    @Field(type = FieldType.Boolean)
    private Boolean parsed;
    
    @Field(type = FieldType.Keyword)
    private String originalFormat;
    
    @Field(type = FieldType.Integer)
    private Integer severity;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Text)
    private String userId;
    
    @Field(type = FieldType.Text)
    private String sessionId;
    
    @Field(type = FieldType.Text)
    private String requestId;
    
    @Field(type = FieldType.Keyword)
    private String httpMethod;
    
    @Field(type = FieldType.Text)
    private String httpUrl;
    
    @Field(type = FieldType.Integer)
    private Integer httpStatus;
    
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
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
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
    
    @Override
    public String toString() {
        return String.format("LogEntry{id='%s', timestamp=%s, level='%s', message='%s', source='%s'}", 
                           id, timestamp, level, message, source);
    }
}
