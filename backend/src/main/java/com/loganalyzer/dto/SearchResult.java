package com.loganalyzer.dto;

import com.loganalyzer.model.LogEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a search query.
 * Contains the matching log entries, aggregations, and metadata about the search.
 */
public class SearchResult {
    
    private List<LogEntry> logs;
    private long totalHits;
    private int page;
    private int size;
    private int totalPages;
    private long searchTimeMs;
    private String searchId;
    private LocalDateTime timestamp;
    
    // Aggregation results
    private Map<String, AggregationResult> aggregations;
    
    // Search metadata
    private SearchMetadata metadata;
    
    // Highlighting information
    private Map<String, List<String>> highlights;
    
    // Constructors
    public SearchResult() {
        this.timestamp = LocalDateTime.now();
    }
    
    public SearchResult(List<LogEntry> logs, long totalHits, int page, int size) {
        this();
        this.logs = logs;
        this.totalHits = totalHits;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) totalHits / size);
    }
    
    // Inner classes
    public static class AggregationResult {
        private String name;
        private String type;
        private List<Bucket> buckets;
        private Map<String, Object> stats;
        
        public AggregationResult() {}
        
        public AggregationResult(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        public static class Bucket {
            private String key;
            private long docCount;
            private Map<String, Object> metrics;
            
            public Bucket() {}
            
            public Bucket(String key, long docCount) {
                this.key = key;
                this.docCount = docCount;
            }
            
            // Getters and Setters
            public String getKey() { return key; }
            public void setKey(String key) { this.key = key; }
            
            public long getDocCount() { return docCount; }
            public void setDocCount(long docCount) { this.docCount = docCount; }
            
            public Map<String, Object> getMetrics() { return metrics; }
            public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public List<Bucket> getBuckets() { return buckets; }
        public void setBuckets(List<Bucket> buckets) { this.buckets = buckets; }
        
        public Map<String, Object> getStats() { return stats; }
        public void setStats(Map<String, Object> stats) { this.stats = stats; }
    }
    
    public static class SearchMetadata {
        private String query;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<String> appliedFilters;
        private String sortBy;
        private boolean timedOut;
        private int shardsTotal;
        private int shardsSuccessful;
        private int shardsFailed;
        
        public SearchMetadata() {}
        
        // Getters and Setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public List<String> getAppliedFilters() { return appliedFilters; }
        public void setAppliedFilters(List<String> appliedFilters) { this.appliedFilters = appliedFilters; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public boolean isTimedOut() { return timedOut; }
        public void setTimedOut(boolean timedOut) { this.timedOut = timedOut; }
        
        public int getShardsTotal() { return shardsTotal; }
        public void setShardsTotal(int shardsTotal) { this.shardsTotal = shardsTotal; }
        
        public int getShardsSuccessful() { return shardsSuccessful; }
        public void setShardsSuccessful(int shardsSuccessful) { this.shardsSuccessful = shardsSuccessful; }
        
        public int getShardsFailed() { return shardsFailed; }
        public void setShardsFailed(int shardsFailed) { this.shardsFailed = shardsFailed; }
    }
    
    // Getters and Setters
    public List<LogEntry> getLogs() { return logs; }
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }
    
    public long getTotalHits() { return totalHits; }
    public void setTotalHits(long totalHits) { 
        this.totalHits = totalHits;
        this.totalPages = (int) Math.ceil((double) totalHits / size);
    }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { 
        this.size = size;
        this.totalPages = (int) Math.ceil((double) totalHits / size);
    }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    
    public long getSearchTimeMs() { return searchTimeMs; }
    public void setSearchTimeMs(long searchTimeMs) { this.searchTimeMs = searchTimeMs; }
    
    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Map<String, AggregationResult> getAggregations() { return aggregations; }
    public void setAggregations(Map<String, AggregationResult> aggregations) { this.aggregations = aggregations; }
    
    public SearchMetadata getMetadata() { return metadata; }
    public void setMetadata(SearchMetadata metadata) { this.metadata = metadata; }
    
    public Map<String, List<String>> getHighlights() { return highlights; }
    public void setHighlights(Map<String, List<String>> highlights) { this.highlights = highlights; }
    
    // Utility methods
    public boolean hasResults() {
        return logs != null && !logs.isEmpty();
    }
    
    public boolean hasAggregations() {
        return aggregations != null && !aggregations.isEmpty();
    }
    
    public boolean hasHighlights() {
        return highlights != null && !highlights.isEmpty();
    }
    
    public boolean hasNextPage() {
        return page < totalPages;
    }
    
    public boolean hasPreviousPage() {
        return page > 1;
    }
    
    public int getResultCount() {
        return logs != null ? logs.size() : 0;
    }
    
    public double getSearchTimeSeconds() {
        return searchTimeMs / 1000.0;
    }
    
    @Override
    public String toString() {
        return String.format("SearchResult{totalHits=%d, page=%d, size=%d, searchTimeMs=%d, resultCount=%d}", 
                           totalHits, page, size, searchTimeMs, getResultCount());
    }
}
