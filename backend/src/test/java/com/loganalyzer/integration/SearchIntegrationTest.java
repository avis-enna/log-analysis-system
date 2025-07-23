package com.loganalyzer.integration;

import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.dto.SearchResult;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryRepository;
import com.loganalyzer.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for search functionality using TestContainers.
 * Tests the complete search pipeline with real Elasticsearch instance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Search Integration Tests")
class SearchIntegrationTest {
    
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
                    .asCompatibleSubstituteFor("elasticsearch"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.host", elasticsearch::getHost);
        registry.add("elasticsearch.port", () -> elasticsearch.getMappedPort(9200));
        registry.add("elasticsearch.username", () -> "");
        registry.add("elasticsearch.password", () -> "");
    }
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    private List<LogEntry> testLogEntries;
    
    @BeforeEach
    void setUp() {
        // Clear existing data
        logEntryRepository.deleteAll();
        
        // Create test data
        testLogEntries = Arrays.asList(
            createLogEntry("1", "ERROR", "Database connection timeout", "web-service", "prod-server-1"),
            createLogEntry("2", "INFO", "User authentication successful", "auth-service", "auth-server-1"),
            createLogEntry("3", "WARN", "High memory usage: 85%", "web-service", "prod-server-2"),
            createLogEntry("4", "ERROR", "Payment processing failed for order #12345", "payment-service", "payment-server-1"),
            createLogEntry("5", "DEBUG", "Cache hit for user profile data", "web-service", "prod-server-1"),
            createLogEntry("6", "FATAL", "OutOfMemoryError in thread pool", "web-service", "prod-server-3"),
            createLogEntry("7", "INFO", "Scheduled backup completed successfully", "backup-service", "backup-server-1"),
            createLogEntry("8", "ERROR", "HTTP 500 error on /api/users endpoint", "api-gateway", "gateway-server-1"),
            createLogEntry("9", "WARN", "SSL certificate expires in 30 days", "web-service", "prod-server-1"),
            createLogEntry("10", "INFO", "Application startup completed in 2.3 seconds", "web-service", "prod-server-1")
        );
        
        // Save test data
        logEntryRepository.saveAll(testLogEntries);
        
        // Wait for Elasticsearch to index the data
        await().untilAsserted(() -> {
            assertThat(logEntryRepository.count()).isEqualTo(10);
        });
    }
    
    @Test
    @DisplayName("Should perform full-text search across all fields")
    void shouldPerformFullTextSearch() {
        // Given
        SearchQuery query = new SearchQuery("database");
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.quickSearch(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getTotalHits()).isGreaterThan(0);
        assertThat(result.getSearchTimeMs()).isGreaterThan(0);
        
        // Verify that results contain the search term
        result.getLogs().forEach(log -> 
            assertThat(log.getMessage().toLowerCase()).contains("database"));
    }
    
    @Test
    @DisplayName("Should filter logs by level")
    void shouldFilterLogsByLevel() {
        // Given
        SearchQuery query = new SearchQuery("*");
        query.setLevels(Arrays.asList("ERROR", "FATAL"));
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(4); // 3 ERROR + 1 FATAL
        
        // Verify all results are ERROR or FATAL level
        result.getLogs().forEach(log -> 
            assertThat(log.getLevel()).isIn("ERROR", "FATAL"));
    }
    
    @Test
    @DisplayName("Should filter logs by application")
    void shouldFilterLogsByApplication() {
        // Given
        SearchQuery query = new SearchQuery("*");
        query.setApplications(Arrays.asList("web-service"));
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(5); // 5 web-service logs
        
        // Verify all results are from web-service
        result.getLogs().forEach(log -> 
            assertThat(log.getApplication()).isEqualTo("web-service"));
    }
    
    @Test
    @DisplayName("Should filter logs by time range")
    void shouldFilterLogsByTimeRange() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        SearchQuery query = new SearchQuery("*");
        query.setStartTime(oneHourAgo);
        query.setEndTime(now);
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        
        // Verify all results are within time range
        result.getLogs().forEach(log -> {
            assertThat(log.getTimestamp()).isAfter(oneHourAgo);
            assertThat(log.getTimestamp()).isBefore(now);
        });
    }
    
    @Test
    @DisplayName("Should perform combined filtering")
    void shouldPerformCombinedFiltering() {
        // Given
        SearchQuery query = new SearchQuery("*");
        query.setLevels(Arrays.asList("ERROR"));
        query.setApplications(Arrays.asList("web-service"));
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(1); // Only 1 ERROR from web-service
        
        LogEntry log = result.getLogs().get(0);
        assertThat(log.getLevel()).isEqualTo("ERROR");
        assertThat(log.getApplication()).isEqualTo("web-service");
    }
    
    @Test
    @DisplayName("Should search with wildcard patterns")
    void shouldSearchWithWildcardPatterns() {
        // Given
        SearchQuery query = new SearchQuery("*connection*");
        query.setSearchMode(SearchQuery.SearchMode.WILDCARD);
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.searchByPattern(
            "*connection*", 
            SearchQuery.SearchMode.WILDCARD,
            LocalDateTime.now().minusHours(24),
            LocalDateTime.now(),
            1, 10
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        
        // Verify results contain "connection"
        result.getLogs().forEach(log -> 
            assertThat(log.getMessage().toLowerCase()).contains("connection"));
    }
    
    @Test
    @DisplayName("Should search for error logs")
    void shouldSearchForErrorLogs() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LocalDateTime endTime = LocalDateTime.now();
        
        // When
        SearchResult result = searchService.searchErrors(startTime, endTime, 1, 10);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(4); // 3 ERROR + 1 FATAL
        
        // Verify all results are error level
        result.getLogs().forEach(log -> 
            assertThat(log.getLevel()).isIn("ERROR", "FATAL"));
    }
    
    @Test
    @DisplayName("Should get log statistics")
    void shouldGetLogStatistics() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(24);
        LocalDateTime endTime = LocalDateTime.now();
        
        // When
        Map<String, Object> stats = searchService.getLogStatistics(startTime, endTime);
        
        // Then
        assertThat(stats).isNotNull();
        assertThat(stats).containsKey("totalLogs");
        assertThat(stats).containsKey("timeRange");
        assertThat(stats).containsKey("searchTime");
        
        assertThat((Long) stats.get("totalLogs")).isEqualTo(10);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> timeRange = (Map<String, Object>) stats.get("timeRange");
        assertThat(timeRange).containsKey("start");
        assertThat(timeRange).containsKey("end");
    }
    
    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        // Given
        SearchQuery query = new SearchQuery("*");
        query.setPage(1);
        query.setSize(5);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(5);
        assertThat(result.getResultCount()).isLessThanOrEqualTo(5);
        assertThat(result.getTotalPages()).isGreaterThanOrEqualTo(2); // 10 logs / 5 per page
        assertThat(result.hasNextPage()).isTrue();
        assertThat(result.hasPreviousPage()).isFalse();
    }
    
    @Test
    @DisplayName("Should handle empty search results")
    void shouldHandleEmptySearchResults() {
        // Given
        SearchQuery query = new SearchQuery("nonexistent_term_xyz123");
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.quickSearch(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isFalse();
        assertThat(result.getTotalHits()).isEqualTo(0);
        assertThat(result.getLogs()).isEmpty();
        assertThat(result.getTotalPages()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should perform case-insensitive search")
    void shouldPerformCaseInsensitiveSearch() {
        // Given
        SearchQuery query1 = new SearchQuery("ERROR");
        SearchQuery query2 = new SearchQuery("error");
        SearchQuery query3 = new SearchQuery("Error");
        
        // When
        SearchResult result1 = searchService.quickSearch(query1);
        SearchResult result2 = searchService.quickSearch(query2);
        SearchResult result3 = searchService.quickSearch(query3);
        
        // Then
        assertThat(result1.getTotalHits()).isEqualTo(result2.getTotalHits());
        assertThat(result2.getTotalHits()).isEqualTo(result3.getTotalHits());
    }
    
    @Test
    @DisplayName("Should search by host")
    void shouldSearchByHost() {
        // Given
        SearchQuery query = new SearchQuery("*");
        query.setHosts(Arrays.asList("prod-server-1"));
        query.setPage(1);
        query.setSize(10);
        
        // When
        SearchResult result = searchService.search(query);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getTotalHits()).isEqualTo(4); // 4 logs from prod-server-1
        
        // Verify all results are from the specified host
        result.getLogs().forEach(log -> 
            assertThat(log.getHost()).isEqualTo("prod-server-1"));
    }
    
    /**
     * Helper method to create test log entries.
     */
    private LogEntry createLogEntry(String id, String level, String message, String application, String host) {
        LogEntry entry = new LogEntry();
        entry.setId(id);
        entry.setLevel(level);
        entry.setMessage(message);
        entry.setApplication(application);
        entry.setHost(host);
        entry.setSource("integration-test");
        entry.setEnvironment("test");
        entry.setTimestamp(LocalDateTime.now());
        entry.setParsed(true);
        
        // Set severity based on level
        switch (level) {
            case "FATAL":
                entry.setSeverity(5);
                break;
            case "ERROR":
                entry.setSeverity(4);
                break;
            case "WARN":
                entry.setSeverity(3);
                break;
            case "INFO":
                entry.setSeverity(2);
                break;
            case "DEBUG":
                entry.setSeverity(1);
                break;
            default:
                entry.setSeverity(0);
        }
        
        return entry;
    }
}
