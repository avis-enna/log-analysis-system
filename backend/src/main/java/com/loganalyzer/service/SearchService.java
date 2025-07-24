package com.loganalyzer.service;

import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.dto.SearchResult;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching and analyzing log entries.
 * Uses JPA repository for local development with H2 database.
 * Provides both simple database searches and advanced query parsing.
 */
@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    /**
     * Performs a comprehensive search with advanced query parsing.
     */
    @Cacheable(value = "searchResults", key = "#query.toString()")
    public SearchResult search(SearchQuery query) {
        long startTime = System.currentTimeMillis();
        
        logger.info("Executing search: query='{}', page={}, size={}", 
                   query.getQuery(), query.getPage(), query.getSize());
        
        try {
            Pageable pageable = createPageable(query);
            
            // Parse the query and execute search
            var page = executeSearch(query, pageable);
            
            SearchResult result = new SearchResult(
                page.getContent(),
                page.getTotalElements(),
                query.getPage(),
                query.getSize()
            );
            
            result.setSearchTimeMs(System.currentTimeMillis() - startTime);
            result.setSearchId(UUID.randomUUID().toString());
            
            logger.info("Search completed: query='{}', hits={}, time={}ms", 
                       query.getQuery(), result.getTotalHits(), result.getSearchTimeMs());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Search failed for query: {}", query.getQuery(), e);
            throw new RuntimeException("Search operation failed", e);
        }
    }
    
    /**
     * Performs a quick search using simple text matching.
     */
    @Cacheable(value = "searchResults", key = "#query.toString()")
    public SearchResult quickSearch(SearchQuery query) {
        long startTime = System.currentTimeMillis();
        
        Pageable pageable = createPageable(query);
        
        try {
            var page = logEntryRepository.findByMessageContaining(query.getQuery(), pageable);
            
            SearchResult result = new SearchResult(
                page.getContent(),
                page.getTotalElements(),
                query.getPage(),
                query.getSize()
            );
            
            result.setSearchTimeMs(System.currentTimeMillis() - startTime);
            result.setSearchId(UUID.randomUUID().toString());
            
            return result;
            
        } catch (Exception e) {
            logger.error("Quick search failed for query: {}", query.getQuery(), e);
            throw new RuntimeException("Quick search operation failed", e);
        }
    }
    
    /**
     * Searches for error logs within a time range.
     */
    public SearchResult searchErrors(LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        SearchQuery query = new SearchQuery("level:ERROR OR level:FATAL");
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setSize(size);
        
        return search(query);
    }
    
    /**
     * Searches for logs by application name.
     */
    public SearchResult searchByApplication(String application, int page, int size) {
        SearchQuery query = new SearchQuery("application:" + application);
        query.setPage(page);
        query.setSize(size);
        
        return search(query);
    }
    
    /**
     * Gets aggregated statistics for search results.
     */
    public Map<String, Object> getSearchStatistics(SearchQuery query) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get total count
            long totalCount = logEntryRepository.count();
            stats.put("totalLogs", totalCount);
            
            // Get error count
            long errorCount = logEntryRepository.countByLevel("ERROR") + logEntryRepository.countByLevel("FATAL");
            stats.put("errorCount", errorCount);

            // Get warning count
            long warningCount = logEntryRepository.countByLevel("WARN");
            stats.put("warningCount", warningCount);

            // Get info count
            long infoCount = logEntryRepository.countByLevel("INFO");
            stats.put("infoCount", infoCount);
            
            // Calculate percentages
            if (totalCount > 0) {
                stats.put("errorPercentage", (double) errorCount / totalCount * 100);
                stats.put("warningPercentage", (double) warningCount / totalCount * 100);
                stats.put("infoPercentage", (double) infoCount / totalCount * 100);
            }
            
            return stats;
            
        } catch (Exception e) {
            logger.error("Failed to get search statistics", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Gets trending search terms.
     */
    public List<String> getTrendingSearches() {
        // This would typically come from a cache or analytics service
        return Arrays.asList(
            "error", "exception", "timeout", "connection", "database",
            "authentication", "authorization", "performance", "memory", "cpu"
        );
    }
    
    /**
     * Suggests search completions based on input.
     */
    public List<String> getSuggestions(String input) {
        if (input == null || input.trim().isEmpty()) {
            return getTrendingSearches();
        }
        
        String lowerInput = input.toLowerCase();
        List<String> suggestions = new ArrayList<>();
        
        // Add field suggestions
        if (lowerInput.startsWith("level:")) {
            suggestions.addAll(Arrays.asList("level:ERROR", "level:WARN", "level:INFO", "level:DEBUG"));
        } else if (lowerInput.startsWith("app")) {
            suggestions.addAll(Arrays.asList("application:web", "application:api", "application:worker"));
        } else {
            // Add common search patterns
            suggestions.addAll(Arrays.asList(
                "level:ERROR", "level:WARN", "exception", "timeout",
                "application:web", "application:api", "status:500"
            ));
        }
        
        return suggestions.stream()
            .filter(s -> s.toLowerCase().contains(lowerInput))
            .limit(10)
            .collect(Collectors.toList());
    }
    
    /**
     * Executes the actual search based on parsed query.
     */
    private org.springframework.data.domain.Page<LogEntry> executeSearch(SearchQuery query, Pageable pageable) {
        String queryText = query.getQuery();
        
        if (queryText == null || queryText.trim().isEmpty()) {
            return logEntryRepository.findAll(pageable);
        }
        
        // Simple query parsing for common patterns
        if (queryText.contains("level:")) {
            String level = extractFieldValue(queryText, "level");
            if (level != null) {
                return logEntryRepository.findByLevel(level, pageable);
            }
        }
        
        if (queryText.contains("application:")) {
            String app = extractFieldValue(queryText, "application");
            if (app != null) {
                return logEntryRepository.findByApplication(app, pageable);
            }
        }
        
        // Default to message search
        return logEntryRepository.findByMessageContaining(queryText, pageable);
    }
    
    /**
     * Searches for logs by pattern with advanced options.
     */
    public SearchResult searchByPattern(String pattern, String mode, LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        SearchQuery query = new SearchQuery(pattern);
        query.setStartTime(startTime);
        query.setEndTime(endTime);
        query.setPage(page);
        query.setSize(size);

        return search(query);
    }

    /**
     * Gets log statistics for a time range.
     */
    public Map<String, Object> getLogStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // For now, return basic statistics
            // In a real implementation, this would filter by time range
            return getSearchStatistics(new SearchQuery("*"));

        } catch (Exception e) {
            logger.error("Failed to get log statistics", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Gets field suggestions for autocomplete.
     */
    public List<String> getFieldSuggestions(String field, String prefix, int limit) {
        List<String> suggestions = new ArrayList<>();

        if ("level".equals(field)) {
            suggestions.addAll(Arrays.asList("ERROR", "WARN", "INFO", "DEBUG", "TRACE"));
        } else if ("application".equals(field)) {
            suggestions.addAll(Arrays.asList("web", "api", "worker", "scheduler", "batch"));
        } else if ("host".equals(field)) {
            suggestions.addAll(Arrays.asList("web-01", "web-02", "api-01", "api-02", "worker-01"));
        }

        return suggestions.stream()
            .filter(s -> prefix == null || s.toLowerCase().startsWith(prefix.toLowerCase()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Gets available searchable fields.
     */
    public List<String> getAvailableFields() {
        return Arrays.asList(
            "level", "application", "host", "message", "timestamp",
            "thread", "logger", "exception", "userId", "sessionId"
        );
    }

    /**
     * Creates pageable object from search query.
     */
    private Pageable createPageable(SearchQuery query) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");

        // For now, use default sorting since SearchQuery might not have sort fields
        return PageRequest.of(query.getPage(), query.getSize(), sort);
    }
    
    /**
     * Extracts field value from query string.
     */
    private String extractFieldValue(String query, String field) {
        String pattern = field + ":(\\w+)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(query);
        
        if (m.find()) {
            return m.group(1);
        }
        
        return null;
    }
}
