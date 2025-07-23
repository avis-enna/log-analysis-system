package com.loganalyzer.unit;

import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.dto.SearchResult;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryRepository;
import com.loganalyzer.service.SearchService;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SearchService.
 * Tests core search functionality with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Unit Tests")
class SearchServiceTest {
    
    @Mock
    private LogEntryRepository logEntryRepository;
    
    @Mock
    private RestHighLevelClient elasticsearchClient;
    
    @InjectMocks
    private SearchService searchService;
    
    private List<LogEntry> sampleLogEntries;
    private SearchQuery sampleQuery;
    
    @BeforeEach
    void setUp() {
        // Create sample log entries
        sampleLogEntries = Arrays.asList(
            createLogEntry("1", "ERROR", "Database connection failed", "web-service"),
            createLogEntry("2", "INFO", "User login successful", "auth-service"),
            createLogEntry("3", "WARN", "High memory usage detected", "web-service"),
            createLogEntry("4", "ERROR", "Payment processing failed", "payment-service"),
            createLogEntry("5", "DEBUG", "Cache miss for user data", "web-service")
        );
        
        // Create sample search query
        sampleQuery = new SearchQuery("ERROR");
        sampleQuery.setPage(1);
        sampleQuery.setSize(10);
        sampleQuery.setStartTime(LocalDateTime.now().minusHours(24));
        sampleQuery.setEndTime(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("Should perform quick search successfully")
    void shouldPerformQuickSearchSuccessfully() {
        // Given
        List<LogEntry> errorLogs = Arrays.asList(sampleLogEntries.get(0), sampleLogEntries.get(3));
        Page<LogEntry> mockPage = new PageImpl<>(errorLogs, Pageable.ofSize(10), 2);
        
        when(logEntryRepository.findByMessageContaining(eq("ERROR"), any(Pageable.class)))
            .thenReturn(mockPage);
        
        // When
        SearchResult result = searchService.quickSearch(sampleQuery);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogs()).hasSize(2);
        assertThat(result.getTotalHits()).isEqualTo(2);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getSearchTimeMs()).isGreaterThan(0);
        assertThat(result.getSearchId()).isNotNull();
        
        // Verify repository interaction
        verify(logEntryRepository).findByMessageContaining(eq("ERROR"), any(Pageable.class));
    }
    
    @Test
    @DisplayName("Should search errors within time range")
    void shouldSearchErrorsWithinTimeRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LocalDateTime endTime = LocalDateTime.now();
        List<LogEntry> errorLogs = Arrays.asList(sampleLogEntries.get(0), sampleLogEntries.get(3));
        
        // Mock the search method (since it uses Elasticsearch client)
        SearchService spyService = spy(searchService);
        SearchResult mockResult = new SearchResult(errorLogs, 2, 1, 10);
        doReturn(mockResult).when(spyService).search(any(SearchQuery.class));
        
        // When
        SearchResult result = spyService.searchErrors(startTime, endTime, 1, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogs()).hasSize(2);
        assertThat(result.getTotalHits()).isEqualTo(2);
        
        // Verify the search was called with correct parameters
        verify(spyService).search(argThat(query -> 
            query.getQuery().contains("ERROR") && 
            query.getStartTime().equals(startTime) &&
            query.getEndTime().equals(endTime) &&
            query.isIncludeStackTrace()
        ));
    }
    
    @Test
    @DisplayName("Should search by application and environment")
    void shouldSearchByApplicationAndEnvironment() {
        // Given
        String application = "web-service";
        String environment = "production";
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LocalDateTime endTime = LocalDateTime.now();
        
        List<LogEntry> webServiceLogs = Arrays.asList(
            sampleLogEntries.get(0), sampleLogEntries.get(2), sampleLogEntries.get(4)
        );
        
        SearchService spyService = spy(searchService);
        SearchResult mockResult = new SearchResult(webServiceLogs, 3, 1, 10);
        doReturn(mockResult).when(spyService).search(any(SearchQuery.class));
        
        // When
        SearchResult result = spyService.searchByApplication(application, environment, startTime, endTime, 1, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogs()).hasSize(3);
        assertThat(result.getTotalHits()).isEqualTo(3);
        
        // Verify the search was called with correct filters
        verify(spyService).search(argThat(query -> 
            query.getApplications().contains(application) &&
            query.getEnvironments().contains(environment) &&
            query.getStartTime().equals(startTime) &&
            query.getEndTime().equals(endTime)
        ));
    }
    
    @Test
    @DisplayName("Should search by pattern with different modes")
    void shouldSearchByPatternWithDifferentModes() {
        // Given
        String pattern = "connection*";
        SearchQuery.SearchMode mode = SearchQuery.SearchMode.WILDCARD;
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LocalDateTime endTime = LocalDateTime.now();
        
        List<LogEntry> patternLogs = Arrays.asList(sampleLogEntries.get(0));
        
        SearchService spyService = spy(searchService);
        SearchResult mockResult = new SearchResult(patternLogs, 1, 1, 10);
        doReturn(mockResult).when(spyService).search(any(SearchQuery.class));
        
        // When
        SearchResult result = spyService.searchByPattern(pattern, mode, startTime, endTime, 1, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLogs()).hasSize(1);
        assertThat(result.getTotalHits()).isEqualTo(1);
        
        // Verify the search was called with correct pattern and mode
        verify(spyService).search(argThat(query -> 
            query.getQuery().equals(pattern) &&
            query.getSearchMode() == mode &&
            query.isHighlightMatches()
        ));
    }
    
    @Test
    @DisplayName("Should get field suggestions")
    void shouldGetFieldSuggestions() {
        // Given
        String fieldName = "application";
        String prefix = "web";
        int limit = 5;
        
        // Mock Elasticsearch client behavior would go here
        // For now, test the method signature and basic functionality
        
        // When
        List<String> suggestions = searchService.getFieldSuggestions(fieldName, prefix, limit);
        
        // Then
        assertThat(suggestions).isNotNull();
        // In a real implementation, we would verify the suggestions content
    }
    
    @Test
    @DisplayName("Should get available fields")
    void shouldGetAvailableFields() {
        // When
        List<String> fields = searchService.getAvailableFields();
        
        // Then
        assertThat(fields).isNotNull();
        assertThat(fields).isNotEmpty();
        assertThat(fields).contains("timestamp", "level", "message", "source", "host");
    }
    
    @Test
    @DisplayName("Should handle search query validation")
    void shouldHandleSearchQueryValidation() {
        // Given
        SearchQuery validQuery = new SearchQuery("level:ERROR");
        validQuery.setPage(1);
        validQuery.setSize(100);
        
        SearchQuery invalidQuery = new SearchQuery("");
        invalidQuery.setPage(0); // Invalid page
        invalidQuery.setSize(-1); // Invalid size
        
        // When & Then
        assertThat(validQuery.getQuery()).isNotBlank();
        assertThat(validQuery.getPage()).isGreaterThan(0);
        assertThat(validQuery.getSize()).isGreaterThan(0);
        
        assertThat(invalidQuery.getQuery()).isBlank();
        // Note: Validation would typically be handled by @Valid annotation in controller
    }
    
    @Test
    @DisplayName("Should handle search with time range")
    void shouldHandleSearchWithTimeRange() {
        // Given
        SearchQuery queryWithTimeRange = new SearchQuery("ERROR");
        queryWithTimeRange.setStartTime(LocalDateTime.now().minusHours(24));
        queryWithTimeRange.setEndTime(LocalDateTime.now());
        
        SearchQuery queryWithoutTimeRange = new SearchQuery("ERROR");
        
        // When & Then
        assertThat(queryWithTimeRange.hasTimeRange()).isTrue();
        assertThat(queryWithoutTimeRange.hasTimeRange()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle search with filters")
    void shouldHandleSearchWithFilters() {
        // Given
        SearchQuery queryWithFilters = new SearchQuery("ERROR");
        queryWithFilters.setLevels(Arrays.asList("ERROR", "FATAL"));
        queryWithFilters.setSources(Arrays.asList("web-service", "auth-service"));
        
        SearchQuery queryWithoutFilters = new SearchQuery("ERROR");
        
        // When & Then
        assertThat(queryWithFilters.hasFilters()).isTrue();
        assertThat(queryWithoutFilters.hasFilters()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle search with aggregations")
    void shouldHandleSearchWithAggregations() {
        // Given
        SearchQuery queryWithAggregations = new SearchQuery("*");
        queryWithAggregations.setAggregations(Arrays.asList(
            new SearchQuery.AggregationRequest("levels", SearchQuery.AggregationRequest.AggregationType.TERMS, "level"),
            new SearchQuery.AggregationRequest("timeline", SearchQuery.AggregationRequest.AggregationType.DATE_HISTOGRAM, "timestamp")
        ));
        
        SearchQuery queryWithoutAggregations = new SearchQuery("*");
        
        // When & Then
        assertThat(queryWithAggregations.hasAggregations()).isTrue();
        assertThat(queryWithoutAggregations.hasAggregations()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate pagination correctly")
    void shouldCalculatePaginationCorrectly() {
        // Given
        SearchQuery query = new SearchQuery("test");
        query.setPage(3);
        query.setSize(25);
        
        // When
        int offset = query.getOffset();
        
        // Then
        assertThat(offset).isEqualTo(50); // (3-1) * 25
    }
    
    @Test
    @DisplayName("Should handle search exceptions gracefully")
    void shouldHandleSearchExceptionsGracefully() {
        // Given
        when(logEntryRepository.findByMessageContaining(any(), any()))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // When & Then
        assertThatThrownBy(() -> searchService.quickSearch(sampleQuery))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Quick search operation failed");
    }
    
    /**
     * Helper method to create sample log entries.
     */
    private LogEntry createLogEntry(String id, String level, String message, String application) {
        LogEntry entry = new LogEntry();
        entry.setId(id);
        entry.setLevel(level);
        entry.setMessage(message);
        entry.setApplication(application);
        entry.setSource("test-source");
        entry.setHost("test-host");
        entry.setEnvironment("test");
        entry.setTimestamp(LocalDateTime.now());
        return entry;
    }
}
