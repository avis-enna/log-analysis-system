package com.loganalyzer.exception;

/**
 * Exception thrown when log ingestion operations fail.
 */
public class LogIngestionException extends RuntimeException {
    
    private final String source;
    private final String errorCode;
    
    public LogIngestionException(String message) {
        super(message);
        this.source = null;
        this.errorCode = "INGESTION_ERROR";
    }
    
    public LogIngestionException(String message, String source) {
        super(message);
        this.source = source;
        this.errorCode = "INGESTION_ERROR";
    }
    
    public LogIngestionException(String message, String source, String errorCode) {
        super(message);
        this.source = source;
        this.errorCode = errorCode;
    }
    
    public LogIngestionException(String message, Throwable cause) {
        super(message, cause);
        this.source = null;
        this.errorCode = "INGESTION_ERROR";
    }
    
    public LogIngestionException(String message, String source, Throwable cause) {
        super(message, cause);
        this.source = source;
        this.errorCode = "INGESTION_ERROR";
    }
    
    public String getSource() {
        return source;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
