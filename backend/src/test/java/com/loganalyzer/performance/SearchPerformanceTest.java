package com.loganalyzer.performance;

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
import org.springframework.util.StopWatch;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for the search functionality.
 * Tests system performance under various load conditions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("Search Performance Tests")
class SearchPerformanceTest {
    
    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
                    .asCompatibleSubstituteFor("elasticsearch"))
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g"); // Increased memory for performance tests
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.host", elasticsearch::getHost);
        registry.add("elasticsearch.port", () -> elasticsearch.getMappedPort(9200));
    }
    
    @Autowired
    private SearchService searchService;
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    private static final int LARGE_DATASET_SIZE = 10000;
    private static final int CONCURRENT_USERS = 50;
    private static final int REQUESTS_PER_USER = 10;
    
    private final Random random = new Random();
    private final String[] logLevels = {"DEBUG", "INFO", "WARN", "ERROR", "FATAL"};
    private final String[] applications = {"web-service", "auth-service", "payment-service", "notification-service", "analytics-service"};
    private final String[] hosts = {"server-1", "server-2", "server-3", "server-4", "server-5"};
    private final String[] environments = {"development", "staging", "production"};
    
    @BeforeEach
    void setUp() {
        // Clear existing data
        logEntryRepository.deleteAll();
        
        // Create large dataset for performance testing
        createLargeDataset();
    }
    
    @Test
    @DisplayName("Should handle single search request within performance threshold")
    void shouldHandleSingleSearchWithinThreshold() {
        // Given
        SearchQuery query = new SearchQuery("ERROR");
        query.setPage(1);
        query.setSize(100);
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        SearchResult result = searchService.quickSearch(query);
        
        stopWatch.stop();
        long executionTime = stopWatch.getTotalTimeMillis();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(executionTime).isLessThan(500); // Should complete within 500ms
        
        System.out.printf("Single search completed in %d ms%n", executionTime);
    }
    
    @Test
    @DisplayName("Should handle concurrent search requests efficiently")
    void shouldHandleConcurrentSearchRequestsEfficiently() throws InterruptedException, ExecutionException {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        List<Future<SearchResult>> futures = new ArrayList<>();
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // Submit concurrent search requests
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            Future<SearchResult> future = executor.submit(() -> {
                SearchQuery query = createRandomSearchQuery();
                return searchService.quickSearch(query);
            });
            futures.add(future);
        }
        
        // Wait for all requests to complete
        List<SearchResult> results = new ArrayList<>();
        for (Future<SearchResult> future : futures) {
            results.add(future.get(30, TimeUnit.SECONDS)); // 30 second timeout
        }
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        
        executor.shutdown();
        
        // Then
        assertThat(results).hasSize(CONCURRENT_USERS);
        assertThat(results).allMatch(result -> result != null);
        assertThat(totalTime).isLessThan(10000); // Should complete within 10 seconds
        
        double averageTime = (double) totalTime / CONCURRENT_USERS;
        System.out.printf("Concurrent searches: %d requests completed in %d ms (avg: %.2f ms per request)%n", 
                         CONCURRENT_USERS, totalTime, averageTime);
    }
    
    @Test
    @DisplayName("Should handle high-frequency search requests")
    void shouldHandleHighFrequencySearchRequests() throws InterruptedException, ExecutionException {
        // Given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        List<Future<Long>> futures = new ArrayList<>();
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        // Each user performs multiple requests
        for (int user = 0; user < CONCURRENT_USERS; user++) {
            Future<Long> future = executor.submit(() -> {
                long userStartTime = System.currentTimeMillis();
                
                for (int request = 0; request < REQUESTS_PER_USER; request++) {
                    SearchQuery query = createRandomSearchQuery();
                    SearchResult result = searchService.quickSearch(query);
                    assertThat(result).isNotNull();
                }
                
                return System.currentTimeMillis() - userStartTime;
            });
            futures.add(future);
        }
        
        // Wait for all users to complete
        List<Long> userTimes = new ArrayList<>();
        for (Future<Long> future : futures) {
            userTimes.add(future.get(60, TimeUnit.SECONDS)); // 60 second timeout
        }
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        
        executor.shutdown();
        
        // Then
        int totalRequests = CONCURRENT_USERS * REQUESTS_PER_USER;
        double requestsPerSecond = (double) totalRequests / (totalTime / 1000.0);
        double averageUserTime = userTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        
        assertThat(requestsPerSecond).isGreaterThan(10); // Should handle at least 10 requests per second
        
        System.out.printf("High-frequency test: %d requests in %d ms (%.2f req/sec, avg user time: %.2f ms)%n", 
                         totalRequests, totalTime, requestsPerSecond, averageUserTime);
    }
    
    @Test
    @DisplayName("Should handle large result sets efficiently")
    void shouldHandleLargeResultSetsEfficiently() {
        // Given
        SearchQuery query = new SearchQuery("*"); // Match all logs
        query.setPage(1);
        query.setSize(1000); // Large page size
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        SearchResult result = searchService.quickSearch(query);
        
        stopWatch.stop();
        long executionTime = stopWatch.getTotalTimeMillis();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasResults()).isTrue();
        assertThat(result.getResultCount()).isLessThanOrEqualTo(1000);
        assertThat(executionTime).isLessThan(2000); // Should complete within 2 seconds
        
        System.out.printf("Large result set (%d results) retrieved in %d ms%n", 
                         result.getResultCount(), executionTime);
    }
    
    @Test
    @DisplayName("Should handle complex queries with filters efficiently")
    void shouldHandleComplexQueriesWithFiltersEfficiently() {
        // Given
        SearchQuery complexQuery = new SearchQuery("ERROR OR FATAL");
        complexQuery.setLevels(List.of("ERROR", "FATAL"));
        complexQuery.setApplications(List.of("web-service", "payment-service"));
        complexQuery.setHosts(List.of("server-1", "server-2"));
        complexQuery.setStartTime(LocalDateTime.now().minusHours(24));
        complexQuery.setEndTime(LocalDateTime.now());
        complexQuery.setPage(1);
        complexQuery.setSize(100);
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        SearchResult result = searchService.search(complexQuery);
        
        stopWatch.stop();
        long executionTime = stopWatch.getTotalTimeMillis();
        
        // Then
        assertThat(result).isNotNull();
        assertThat(executionTime).isLessThan(1000); // Should complete within 1 second
        
        System.out.printf("Complex query with filters completed in %d ms%n", executionTime);
    }
    
    @Test
    @DisplayName("Should handle pagination efficiently")
    void shouldHandlePaginationEfficiently() {
        // Given
        SearchQuery query = new SearchQuery("*");
        query.setSize(50);
        
        List<Long> pageTimes = new ArrayList<>();
        
        // When - Test first 10 pages
        for (int page = 1; page <= 10; page++) {
            query.setPage(page);
            
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            
            SearchResult result = searchService.quickSearch(query);
            
            stopWatch.stop();
            long executionTime = stopWatch.getTotalTimeMillis();
            pageTimes.add(executionTime);
            
            assertThat(result).isNotNull();
            assertThat(result.getPage()).isEqualTo(page);
        }
        
        // Then
        double averagePageTime = pageTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxPageTime = pageTimes.stream().mapToLong(Long::longValue).max().orElse(0);
        
        assertThat(averagePageTime).isLessThan(300); // Average should be under 300ms
        assertThat(maxPageTime).isLessThan(500); // Max should be under 500ms
        
        System.out.printf("Pagination test: avg %.2f ms, max %d ms%n", averagePageTime, maxPageTime);
    }
    
    @Test
    @DisplayName("Should maintain performance under memory pressure")
    void shouldMaintainPerformanceUnderMemoryPressure() {
        // Given
        List<SearchResult> results = new ArrayList<>();
        
        // When - Perform many searches to create memory pressure
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        for (int i = 0; i < 100; i++) {
            SearchQuery query = createRandomSearchQuery();
            SearchResult result = searchService.quickSearch(query);
            results.add(result);
            
            // Force garbage collection periodically
            if (i % 20 == 0) {
                System.gc();
            }
        }
        
        stopWatch.stop();
        long totalTime = stopWatch.getTotalTimeMillis();
        
        // Then
        assertThat(results).hasSize(100);
        assertThat(results).allMatch(result -> result != null);
        
        double averageTime = (double) totalTime / 100;
        assertThat(averageTime).isLessThan(500); // Should maintain performance
        
        System.out.printf("Memory pressure test: 100 searches in %d ms (avg: %.2f ms)%n", 
                         totalTime, averageTime);
    }
    
    @Test
    @DisplayName("Should handle search timeout gracefully")
    void shouldHandleSearchTimeoutGracefully() {
        // Given
        SearchQuery timeoutQuery = new SearchQuery("*");
        timeoutQuery.setSize(10000); // Very large result set
        
        // When
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        try {
            SearchResult result = searchService.quickSearch(timeoutQuery);
            stopWatch.stop();
            
            // Then
            assertThat(result).isNotNull();
            assertThat(stopWatch.getTotalTimeMillis()).isLessThan(30000); // Should not exceed 30 seconds
            
        } catch (Exception e) {
            stopWatch.stop();
            // Timeout exceptions are acceptable for this test
            System.out.printf("Search timed out after %d ms (expected behavior)%n", 
                             stopWatch.getTotalTimeMillis());
        }
    }
    
    /**
     * Creates a large dataset for performance testing.
     */
    private void createLargeDataset() {
        System.out.println("Creating large dataset for performance testing...");
        
        List<LogEntry> logEntries = IntStream.range(0, LARGE_DATASET_SIZE)
                .parallel()
                .mapToObj(this::createRandomLogEntry)
                .toList();
        
        // Save in batches to avoid memory issues
        int batchSize = 1000;
        for (int i = 0; i < logEntries.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, logEntries.size());
            List<LogEntry> batch = logEntries.subList(i, endIndex);
            logEntryRepository.saveAll(batch);
        }
        
        System.out.printf("Created %d log entries for performance testing%n", LARGE_DATASET_SIZE);
    }
    
    /**
     * Creates a random log entry for testing.
     */
    private LogEntry createRandomLogEntry(int index) {
        LogEntry entry = new LogEntry();
        entry.setId(String.valueOf(index));
        entry.setLevel(logLevels[random.nextInt(logLevels.length)]);
        entry.setMessage(generateRandomMessage());
        entry.setApplication(applications[random.nextInt(applications.length)]);
        entry.setHost(hosts[random.nextInt(hosts.length)]);
        entry.setEnvironment(environments[random.nextInt(environments.length)]);
        entry.setSource("performance-test");
        entry.setTimestamp(LocalDateTime.now().minusMinutes(random.nextInt(1440))); // Random time in last 24 hours
        entry.setParsed(true);
        
        return entry;
    }
    
    /**
     * Generates a random log message.
     */
    private String generateRandomMessage() {
        String[] messageTemplates = {
            "User %d logged in successfully",
            "Database query executed in %d ms",
            "HTTP request to %s completed with status %d",
            "Cache miss for key: user_%d",
            "Payment processing failed for transaction %d",
            "Memory usage: %d%% of available heap",
            "Connection timeout to external service",
            "Scheduled task completed successfully",
            "Invalid input received: %s",
            "System health check passed"
        };
        
        String template = messageTemplates[random.nextInt(messageTemplates.length)];
        return String.format(template, random.nextInt(10000), random.nextInt(1000), 
                           "/api/endpoint", random.nextInt(600), "invalid_data");
    }
    
    /**
     * Creates a random search query for testing.
     */
    private SearchQuery createRandomSearchQuery() {
        String[] queryTerms = {"ERROR", "SUCCESS", "timeout", "user", "database", "*"};
        
        SearchQuery query = new SearchQuery(queryTerms[random.nextInt(queryTerms.length)]);
        query.setPage(1);
        query.setSize(random.nextInt(100) + 10); // 10-110 results
        
        // Randomly add filters
        if (random.nextBoolean()) {
            query.setLevels(List.of(logLevels[random.nextInt(logLevels.length)]));
        }
        
        if (random.nextBoolean()) {
            query.setApplications(List.of(applications[random.nextInt(applications.length)]));
        }
        
        return query;
    }
}
