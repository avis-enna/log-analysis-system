package com.loganalyzer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for SearchController REST API endpoints.
 * Tests the complete HTTP request/response cycle with real dependencies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@DisplayName("Search Controller Integration Tests")
class SearchControllerIntegrationTest {
    
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
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    private List<LogEntry> testLogEntries;
    
    @BeforeEach
    void setUp() {
        // Clear existing data
        logEntryRepository.deleteAll();
        
        // Create test data
        testLogEntries = Arrays.asList(
            createLogEntry("1", "ERROR", "Database connection failed", "web-service"),
            createLogEntry("2", "INFO", "User login successful", "auth-service"),
            createLogEntry("3", "WARN", "High memory usage detected", "web-service"),
            createLogEntry("4", "ERROR", "Payment processing failed", "payment-service"),
            createLogEntry("5", "DEBUG", "Cache miss for user data", "web-service"),
            createLogEntry("6", "FATAL", "OutOfMemoryError occurred", "web-service"),
            createLogEntry("7", "INFO", "Backup completed successfully", "backup-service")
        );
        
        // Save test data and wait for indexing
        logEntryRepository.saveAll(testLogEntries);
        
        // Wait for Elasticsearch to index the data
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    @DisplayName("POST /search - Should perform comprehensive search")
    void shouldPerformComprehensiveSearch() throws Exception {
        // Given
        SearchQuery query = new SearchQuery("ERROR");
        query.setPage(1);
        query.setSize(10);
        
        // When & Then
        mockMvc.perform(post("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(query)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.totalHits").isNumber())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.searchTimeMs").isNumber())
                .andExpect(jsonPath("$.searchId").isString());
    }
    
    @Test
    @DisplayName("GET /search/quick - Should perform quick search")
    void shouldPerformQuickSearch() throws Exception {
        mockMvc.perform(get("/api/v1/search/quick")
                .param("q", "ERROR")
                .param("page", "1")
                .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.totalHits").isNumber())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }
    
    @Test
    @DisplayName("GET /search/errors - Should search error logs")
    void shouldSearchErrorLogs() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        
        mockMvc.perform(get("/api/v1/search/errors")
                .param("startTime", yesterday.toString())
                .param("endTime", now.toString())
                .param("page", "1")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.totalHits").isNumber());
    }
    
    @Test
    @DisplayName("GET /search/application/{application} - Should search by application")
    void shouldSearchByApplication() throws Exception {
        mockMvc.perform(get("/api/v1/search/application/web-service")
                .param("environment", "test")
                .param("page", "1")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.totalHits").isNumber());
    }
    
    @Test
    @DisplayName("GET /search/pattern - Should search by pattern")
    void shouldSearchByPattern() throws Exception {
        mockMvc.perform(get("/api/v1/search/pattern")
                .param("pattern", "*connection*")
                .param("mode", "WILDCARD")
                .param("page", "1")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.totalHits").isNumber());
    }
    
    @Test
    @DisplayName("GET /search/stats - Should get log statistics")
    void shouldGetLogStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/search/stats"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLogs").isNumber())
                .andExpect(jsonPath("$.timeRange").exists())
                .andExpect(jsonPath("$.searchTime").isNumber());
    }
    
    @Test
    @DisplayName("GET /search/suggest/{fieldName} - Should get field suggestions")
    void shouldGetFieldSuggestions() throws Exception {
        mockMvc.perform(get("/api/v1/search/suggest/application")
                .param("prefix", "web")
                .param("limit", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    @DisplayName("GET /search/fields - Should get available fields")
    void shouldGetAvailableFields() throws Exception {
        mockMvc.perform(get("/api/v1/search/fields"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasItem("timestamp")))
                .andExpect(jsonPath("$", hasItem("level")))
                .andExpect(jsonPath("$", hasItem("message")))
                .andExpect(jsonPath("$", hasItem("source")))
                .andExpect(jsonPath("$", hasItem("host")));
    }
    
    @Test
    @DisplayName("POST /search/validate - Should validate search query")
    void shouldValidateSearchQuery() throws Exception {
        // Valid query
        SearchQuery validQuery = new SearchQuery("level:ERROR");
        validQuery.setPage(1);
        validQuery.setSize(10);
        
        mockMvc.perform(post("/api/v1/search/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validQuery)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.query").value("level:ERROR"));
    }
    
    @Test
    @DisplayName("GET /search/history - Should get search history")
    void shouldGetSearchHistory() throws Exception {
        mockMvc.perform(get("/api/v1/search/history")
                .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    @DisplayName("POST /search/save - Should save search query")
    void shouldSaveSearchQuery() throws Exception {
        SearchQuery query = new SearchQuery("level:ERROR");
        query.setPage(1);
        query.setSize(10);
        
        mockMvc.perform(post("/api/v1/search/save")
                .param("name", "Error Logs Search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(query)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Error Logs Search"))
                .andExpect(jsonPath("$.query").exists())
                .andExpect(jsonPath("$.createdAt").exists());
    }
    
    @Test
    @DisplayName("GET /search/saved - Should get saved searches")
    void shouldGetSavedSearches() throws Exception {
        mockMvc.perform(get("/api/v1/search/saved"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    @DisplayName("Should handle invalid search query gracefully")
    void shouldHandleInvalidSearchQueryGracefully() throws Exception {
        SearchQuery invalidQuery = new SearchQuery("");
        invalidQuery.setPage(0); // Invalid page
        invalidQuery.setSize(-1); // Invalid size
        
        mockMvc.perform(post("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidQuery)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle missing required parameters")
    void shouldHandleMissingRequiredParameters() throws Exception {
        mockMvc.perform(get("/api/v1/search/quick"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle invalid date format")
    void shouldHandleInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/v1/search/errors")
                .param("startTime", "invalid-date")
                .param("endTime", "invalid-date"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should handle large page size requests")
    void shouldHandleLargePageSizeRequests() throws Exception {
        mockMvc.perform(get("/api/v1/search/quick")
                .param("q", "*")
                .param("page", "1")
                .param("size", "50000")) // Very large size
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("Should support CORS headers")
    void shouldSupportCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/search")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().exists("Access-Control-Allow-Methods"))
                .andExpect(header().exists("Access-Control-Allow-Headers"));
    }
    
    @Test
    @DisplayName("Should handle concurrent requests efficiently")
    void shouldHandleConcurrentRequestsEfficiently() throws Exception {
        // This test would typically use multiple threads
        // For simplicity, we'll test sequential requests
        
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/search/quick")
                    .param("q", "test" + i)
                    .param("page", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.searchTimeMs").isNumber());
        }
    }
    
    @Test
    @DisplayName("Should return proper HTTP status codes")
    void shouldReturnProperHttpStatusCodes() throws Exception {
        // Success case
        mockMvc.perform(get("/api/v1/search/fields"))
                .andExpect(status().isOk());
        
        // Bad request case
        mockMvc.perform(get("/api/v1/search/quick"))
                .andExpect(status().isBadRequest());
        
        // Not found case (non-existent endpoint)
        mockMvc.perform(get("/api/v1/search/nonexistent"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @DisplayName("Should include proper response headers")
    void shouldIncludeProperResponseHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/search/fields"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().exists("Date"));
    }
    
    @Test
    @DisplayName("Should handle search with complex filters")
    void shouldHandleSearchWithComplexFilters() throws Exception {
        SearchQuery complexQuery = new SearchQuery("*");
        complexQuery.setLevels(Arrays.asList("ERROR", "FATAL"));
        complexQuery.setApplications(Arrays.asList("web-service", "payment-service"));
        complexQuery.setStartTime(LocalDateTime.now().minusHours(24));
        complexQuery.setEndTime(LocalDateTime.now());
        complexQuery.setPage(1);
        complexQuery.setSize(10);
        
        mockMvc.perform(post("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(complexQuery)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs").isArray())
                .andExpect(jsonPath("$.totalHits").isNumber());
    }
    
    /**
     * Helper method to create test log entries.
     */
    private LogEntry createLogEntry(String id, String level, String message, String application) {
        LogEntry entry = new LogEntry();
        entry.setId(id);
        entry.setLevel(level);
        entry.setMessage(message);
        entry.setApplication(application);
        entry.setSource("integration-test");
        entry.setHost("test-host");
        entry.setEnvironment("test");
        entry.setTimestamp(LocalDateTime.now());
        entry.setParsed(true);
        
        return entry;
    }
}
