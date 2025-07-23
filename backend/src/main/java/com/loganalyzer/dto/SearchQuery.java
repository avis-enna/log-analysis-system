package com.loganalyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a search query for log analysis.
 * Supports Splunk-like search syntax and filtering capabilities.
 */
public class SearchQuery {
    
    @NotBlank(message = "Query string cannot be empty")
    private String query;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    @Min(value = 1, message = "Page must be at least 1")
    private int page = 1;
    
    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 10000, message = "Size cannot exceed 10000")
    private int size = 100;
    
    private List<String> sources;
    private List<String> levels;
    private List<String> hosts;
    private List<String> applications;
    private List<String> environments;
    
    private Map<String, String> filters;
    private List<SortField> sortFields;
    
    private boolean includeStackTrace = false;
    private boolean includeMetadata = false;
    private boolean highlightMatches = true;
    
    // Aggregation options
    private List<AggregationRequest> aggregations;
    
    // Search options
    private SearchMode searchMode = SearchMode.FULL_TEXT;
    private boolean caseSensitive = false;
    private boolean useRegex = false;
    private String timeZone = "UTC";
    
    // Constructors
    public SearchQuery() {}
    
    public SearchQuery(String query) {
        this.query = query;
    }
    
    public SearchQuery(String query, LocalDateTime startTime, LocalDateTime endTime) {
        this.query = query;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Enums
    public enum SearchMode {
        FULL_TEXT, EXACT_MATCH, WILDCARD, REGEX, FUZZY
    }
    
    public enum SortDirection {
        ASC, DESC
    }
    
    // Inner classes
    public static class SortField {
        private String field;
        private SortDirection direction = SortDirection.DESC;
        
        public SortField() {}
        
        public SortField(String field, SortDirection direction) {
            this.field = field;
            this.direction = direction;
        }
        
        // Getters and Setters
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        
        public SortDirection getDirection() { return direction; }
        public void setDirection(SortDirection direction) { this.direction = direction; }
    }
    
    public static class AggregationRequest {
        private String name;
        private AggregationType type;
        private String field;
        private Map<String, Object> parameters;
        
        public AggregationRequest() {}
        
        public AggregationRequest(String name, AggregationType type, String field) {
            this.name = name;
            this.type = type;
            this.field = field;
        }
        
        public enum AggregationType {
            TERMS, DATE_HISTOGRAM, HISTOGRAM, STATS, CARDINALITY, PERCENTILES
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public AggregationType getType() { return type; }
        public void setType(AggregationType type) { this.type = type; }
        
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }
    
    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    
    public List<String> getLevels() { return levels; }
    public void setLevels(List<String> levels) { this.levels = levels; }
    
    public List<String> getHosts() { return hosts; }
    public void setHosts(List<String> hosts) { this.hosts = hosts; }
    
    public List<String> getApplications() { return applications; }
    public void setApplications(List<String> applications) { this.applications = applications; }
    
    public List<String> getEnvironments() { return environments; }
    public void setEnvironments(List<String> environments) { this.environments = environments; }
    
    public Map<String, String> getFilters() { return filters; }
    public void setFilters(Map<String, String> filters) { this.filters = filters; }
    
    public List<SortField> getSortFields() { return sortFields; }
    public void setSortFields(List<SortField> sortFields) { this.sortFields = sortFields; }
    
    public boolean isIncludeStackTrace() { return includeStackTrace; }
    public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }
    
    public boolean isIncludeMetadata() { return includeMetadata; }
    public void setIncludeMetadata(boolean includeMetadata) { this.includeMetadata = includeMetadata; }
    
    public boolean isHighlightMatches() { return highlightMatches; }
    public void setHighlightMatches(boolean highlightMatches) { this.highlightMatches = highlightMatches; }
    
    public List<AggregationRequest> getAggregations() { return aggregations; }
    public void setAggregations(List<AggregationRequest> aggregations) { this.aggregations = aggregations; }
    
    public SearchMode getSearchMode() { return searchMode; }
    public void setSearchMode(SearchMode searchMode) { this.searchMode = searchMode; }
    
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    
    public boolean isUseRegex() { return useRegex; }
    public void setUseRegex(boolean useRegex) { this.useRegex = useRegex; }
    
    public String getTimeZone() { return timeZone; }
    public void setTimeZone(String timeZone) { this.timeZone = timeZone; }
    
    // Utility methods
    public boolean hasTimeRange() {
        return startTime != null && endTime != null;
    }
    
    public boolean hasFilters() {
        return (sources != null && !sources.isEmpty()) ||
               (levels != null && !levels.isEmpty()) ||
               (hosts != null && !hosts.isEmpty()) ||
               (applications != null && !applications.isEmpty()) ||
               (environments != null && !environments.isEmpty()) ||
               (filters != null && !filters.isEmpty());
    }
    
    public boolean hasAggregations() {
        return aggregations != null && !aggregations.isEmpty();
    }
    
    public boolean hasSorting() {
        return sortFields != null && !sortFields.isEmpty();
    }
    
    public int getOffset() {
        return (page - 1) * size;
    }
    
    @Override
    public String toString() {
        return String.format("SearchQuery{query='%s', startTime=%s, endTime=%s, page=%d, size=%d}", 
                           query, startTime, endTime, page, size);
    }
}
