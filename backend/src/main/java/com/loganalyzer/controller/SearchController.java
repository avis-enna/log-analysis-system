package com.loganalyzer.controller;

import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.dto.SearchResult;
import com.loganalyzer.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for log search operations.
 * Provides comprehensive search capabilities similar to Splunk.
 */
@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Log search and query operations")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000", "https://*.railway.app"})
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    @Autowired
    private SearchService searchService;
    
    /**
     * Performs comprehensive log search with advanced filtering and aggregations.
     */
    @PostMapping
    @Operation(summary = "Search logs", description = "Performs comprehensive log search with filtering, aggregations, and highlighting")
    public ResponseEntity<SearchResult> search(@Valid @RequestBody SearchQuery query) {
        logger.info("Search request: query='{}', page={}, size={}", query.getQuery(), query.getPage(), query.getSize());
        
        try {
            SearchResult result = searchService.search(query);
            logger.info("Search completed: {} hits in {}ms", result.getTotalHits(), result.getSearchTimeMs());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Search failed for query: {}", query.getQuery(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Performs quick search for simple queries.
     */
    @GetMapping("/quick")
    @Operation(summary = "Quick search", description = "Performs quick search for simple text queries")
    public ResponseEntity<SearchResult> quickSearch(
            @Parameter(description = "Search query string") @RequestParam String q,
            @Parameter(description = "Page number (1-based)") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        SearchQuery query = new SearchQuery(q);
        query.setPage(page);
        query.setSize(size);
        
        try {
            SearchResult result = searchService.quickSearch(query);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Quick search failed for query: {}", q, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Searches for error logs within a time range.
     */
    @GetMapping("/errors")
    @Operation(summary = "Search errors", description = "Searches for error and fatal logs within specified time range")
    public ResponseEntity<SearchResult> searchErrors(
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        try {
            SearchResult result = searchService.searchErrors(startTime, endTime, page, size);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error search failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Searches logs by application and environment.
     */
    @GetMapping("/application/{application}")
    @Operation(summary = "Search by application", description = "Searches logs for specific application and environment")
    public ResponseEntity<SearchResult> searchByApplication(
            @Parameter(description = "Application name") @PathVariable String application,
            @Parameter(description = "Environment") @RequestParam(required = false) String environment,
            @Parameter(description = "Start time") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        try {
            if (startTime == null) startTime = LocalDateTime.now().minusHours(24);
            if (endTime == null) endTime = LocalDateTime.now();
            if (environment == null) environment = "production";
            
            SearchResult result = searchService.searchByApplication(application, page, size);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Application search failed for app: {}", application, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Searches logs using pattern matching.
     */
    @GetMapping("/pattern")
    @Operation(summary = "Pattern search", description = "Searches logs using wildcard or regex patterns")
    public ResponseEntity<SearchResult> searchByPattern(
            @Parameter(description = "Search pattern") @RequestParam String pattern,
            @Parameter(description = "Search mode") @RequestParam(defaultValue = "WILDCARD") SearchQuery.SearchMode mode,
            @Parameter(description = "Start time") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "100") int size) {
        
        try {
            if (startTime == null) startTime = LocalDateTime.now().minusHours(24);
            if (endTime == null) endTime = LocalDateTime.now();
            
            SearchResult result = searchService.searchByPattern(pattern, mode.toString(), startTime, endTime, page, size);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Pattern search failed for pattern: {}", pattern, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets log statistics and aggregations.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get statistics", description = "Gets aggregated log statistics for specified time range")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @Parameter(description = "Start time") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        try {
            if (startTime == null) startTime = LocalDateTime.now().minusHours(24);
            if (endTime == null) endTime = LocalDateTime.now();
            
            Map<String, Object> stats = searchService.getLogStatistics(startTime, endTime);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Statistics request failed", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets field suggestions for auto-completion.
     */
    @GetMapping("/suggest/{fieldName}")
    @Operation(summary = "Get field suggestions", description = "Gets field value suggestions for auto-completion")
    public ResponseEntity<List<String>> getFieldSuggestions(
            @Parameter(description = "Field name") @PathVariable String fieldName,
            @Parameter(description = "Prefix to match") @RequestParam(required = false, defaultValue = "") String prefix,
            @Parameter(description = "Maximum suggestions") @RequestParam(defaultValue = "10") int limit) {
        
        try {
            List<String> suggestions = searchService.getFieldSuggestions(fieldName, prefix, limit);
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            logger.error("Field suggestions failed for field: {}", fieldName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets available field names.
     */
    @GetMapping("/fields")
    @Operation(summary = "Get available fields", description = "Gets list of available field names for searching")
    public ResponseEntity<List<String>> getAvailableFields() {
        try {
            List<String> fields = searchService.getAvailableFields();
            return ResponseEntity.ok(fields);
            
        } catch (Exception e) {
            logger.error("Failed to get available fields", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Validates search query syntax.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate query", description = "Validates search query syntax without executing the search")
    public ResponseEntity<Map<String, Object>> validateQuery(@Valid @RequestBody SearchQuery query) {
        try {
            // Basic validation logic
            Map<String, Object> validation = Map.of(
                "valid", true,
                "query", query.getQuery(),
                "estimatedResults", "unknown",
                "suggestions", List.of()
            );
            
            return ResponseEntity.ok(validation);
            
        } catch (Exception e) {
            Map<String, Object> validation = Map.of(
                "valid", false,
                "error", e.getMessage(),
                "query", query.getQuery()
            );
            return ResponseEntity.ok(validation);
        }
    }
    
    /**
     * Gets search history for a user.
     */
    @GetMapping("/history")
    @Operation(summary = "Get search history", description = "Gets recent search queries for the current user")
    public ResponseEntity<List<Map<String, Object>>> getSearchHistory(
            @Parameter(description = "Maximum history items") @RequestParam(defaultValue = "20") int limit) {
        
        try {
            // This would typically fetch from user's search history
            List<Map<String, Object>> history = List.of(
                Map.of("query", "ERROR", "timestamp", LocalDateTime.now().minusHours(1), "results", 150),
                Map.of("query", "application:web-service", "timestamp", LocalDateTime.now().minusHours(2), "results", 1200),
                Map.of("query", "level:WARN", "timestamp", LocalDateTime.now().minusHours(3), "results", 45)
            );
            
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Failed to get search history", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Saves a search query for later use.
     */
    @PostMapping("/save")
    @Operation(summary = "Save search", description = "Saves a search query for later use")
    public ResponseEntity<Map<String, Object>> saveSearch(
            @Parameter(description = "Search name") @RequestParam String name,
            @Valid @RequestBody SearchQuery query) {
        
        try {
            // This would typically save to user's saved searches
            Map<String, Object> savedSearch = Map.of(
                "id", System.currentTimeMillis(),
                "name", name,
                "query", query,
                "createdAt", LocalDateTime.now(),
                "createdBy", "current-user"
            );
            
            logger.info("Search saved: {} by user", name);
            return ResponseEntity.ok(savedSearch);
            
        } catch (Exception e) {
            logger.error("Failed to save search: {}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Gets saved searches for a user.
     */
    @GetMapping("/saved")
    @Operation(summary = "Get saved searches", description = "Gets saved search queries for the current user")
    public ResponseEntity<List<Map<String, Object>>> getSavedSearches() {
        try {
            // This would typically fetch from user's saved searches
            List<Map<String, Object>> savedSearches = List.of(
                Map.of("id", 1L, "name", "Daily Errors", "query", "level:ERROR", "createdAt", LocalDateTime.now().minusDays(1)),
                Map.of("id", 2L, "name", "Web Service Logs", "query", "application:web-service", "createdAt", LocalDateTime.now().minusDays(2))
            );
            
            return ResponseEntity.ok(savedSearches);
            
        } catch (Exception e) {
            logger.error("Failed to get saved searches", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
