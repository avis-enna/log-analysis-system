package com.loganalyzer.exception;

/**
 * Exception thrown when search operations fail.
 */
public class SearchException extends RuntimeException {
    
    private final String searchQuery;
    private final String errorCode;
    
    public SearchException(String message) {
        super(message);
        this.searchQuery = null;
        this.errorCode = "SEARCH_ERROR";
    }
    
    public SearchException(String message, String searchQuery) {
        super(message);
        this.searchQuery = searchQuery;
        this.errorCode = "SEARCH_ERROR";
    }
    
    public SearchException(String message, String searchQuery, String errorCode) {
        super(message);
        this.searchQuery = searchQuery;
        this.errorCode = errorCode;
    }
    
    public SearchException(String message, Throwable cause) {
        super(message, cause);
        this.searchQuery = null;
        this.errorCode = "SEARCH_ERROR";
    }
    
    public SearchException(String message, String searchQuery, Throwable cause) {
        super(message, cause);
        this.searchQuery = searchQuery;
        this.errorCode = "SEARCH_ERROR";
    }
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
