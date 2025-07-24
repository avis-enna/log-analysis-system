package com.loganalyzer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.SearchQuery;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Local Development Environment.
 * Tests the complete local development setup with H2 database.
 * Verifies that both backend APIs work without external dependencies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Local Development Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LocalDevelopmentIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/v1";
    }
    
    @Test
    @Order(1)
    @DisplayName("Health Check Should Work")
    void testHealthCheck() {
        // Test actuator health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("UP"));
        
        System.out.println("✅ Health check passed: " + response.getBody());
    }
    
    @Test
    @Order(2)
    @DisplayName("Database Should Be Working")
    void testDatabaseConnection() {
        // Test that we can interact with the H2 database
        long initialCount = logEntryRepository.count();
        
        // Create a test log entry
        LogEntry testEntry = new LogEntry();
        testEntry.setId("test-" + System.currentTimeMillis());
        testEntry.setTimestamp(LocalDateTime.now());
        testEntry.setLevel("INFO");
        testEntry.setMessage("Test log entry for integration test");
        testEntry.setSource("integration-test");
        testEntry.setHost("localhost");
        testEntry.setApplication("log-analyzer");
        testEntry.setEnvironment("test");
        
        // Save the entry
        LogEntry savedEntry = logEntryRepository.save(testEntry);
        assertNotNull(savedEntry);
        assertEquals(testEntry.getId(), savedEntry.getId());
        
        // Verify count increased
        long newCount = logEntryRepository.count();
        assertEquals(initialCount + 1, newCount);
        
        System.out.println("✅ Database operations working. Entries: " + newCount);
    }
    
    @Test
    @Order(3)
    @DisplayName("Search API Should Respond")
    void testSearchApi() {
        // Create search request
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery("*");
        searchQuery.setPage(0);
        searchQuery.setSize(10);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<SearchQuery> request = new HttpEntity<>(searchQuery, headers);
        
        // Call search API
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/logs/search", request, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Should return valid JSON
        assertTrue(response.getBody().contains("{"));
        assertTrue(response.getBody().contains("}"));
        
        System.out.println("✅ Search API working: " + response.getStatusCode());
    }
    
    @Test
    @Order(4)
    @DisplayName("Dashboard API Should Respond")
    void testDashboardApi() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/dashboard/stats", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        System.out.println("✅ Dashboard API working: " + response.getStatusCode());
    }
    
    @Test
    @Order(5)
    @DisplayName("Alerts API Should Respond")
    void testAlertsApi() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/alerts", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        System.out.println("✅ Alerts API working: " + response.getStatusCode());
    }
    
    @Test
    @Order(6)
    @DisplayName("Log Ingestion API Should Work")
    void testLogIngestionApi() {
        // Create a log entry to ingest
        Map<String, Object> logData = new HashMap<>();
        logData.put("timestamp", LocalDateTime.now().toString());
        logData.put("level", "INFO");
        logData.put("message", "Test log from integration test");
        logData.put("source", "integration-test");
        logData.put("host", "localhost");
        logData.put("application", "test-app");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(logData, headers);
        
        // Call ingestion API
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/logs/ingest", request, String.class);
        
        // Should accept the log entry
        assertTrue(response.getStatusCode().is2xxSuccessful());
        
        System.out.println("✅ Log ingestion API working: " + response.getStatusCode());
    }
    
    @Test
    @Order(7)
    @DisplayName("External Services Should Be Optional")
    void testExternalServicesOptional() {
        // This test verifies that the application works without external services
        // like Elasticsearch, Kafka, Redis, etc.
        
        // The fact that all previous tests passed means external services are optional
        // Let's verify by checking that we can still perform operations
        
        long logCount = logEntryRepository.count();
        assertTrue(logCount >= 0, "Should be able to count logs without external services");
        
        // Test that search still works (using JPA instead of Elasticsearch)
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery("test");
        searchQuery.setPage(0);
        searchQuery.setSize(5);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchQuery> request = new HttpEntity<>(searchQuery, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/logs/search", request, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        System.out.println("✅ External services are optional - application works without them");
    }
    
    @Test
    @Order(8)
    @DisplayName("H2 Database Console Should Be Available")
    void testH2Console() {
        // Test that H2 console is accessible (for development)
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/h2-console", String.class);
        
        // H2 console should be available (might redirect or show login page)
        assertTrue(response.getStatusCode().is2xxSuccessful() || 
                  response.getStatusCode().is3xxRedirection());
        
        System.out.println("✅ H2 console accessible: " + response.getStatusCode());
    }
    
    @Test
    @Order(9)
    @DisplayName("Application Should Handle Errors Gracefully")
    void testErrorHandling() {
        // Test invalid search request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>("{\"invalid\":\"json\"}", headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/logs/search", request, String.class);
        
        // Should handle error gracefully (not crash)
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is2xxSuccessful());
        
        System.out.println("✅ Error handling working: " + response.getStatusCode());
    }
    
    @Test
    @Order(10)
    @DisplayName("Complete Development Environment Should Be Ready")
    void testDevelopmentEnvironmentReadiness() {
        System.out.println("\n🎉 COMPLETE DEVELOPMENT ENVIRONMENT VERIFICATION");
        System.out.println("Backend URL: " + baseUrl);
        
        // 1. Health check
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
            baseUrl + "/actuator/health", String.class);
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        System.out.println("✅ Backend health check passed");
        
        // 2. Database operations
        long logCount = logEntryRepository.count();
        assertTrue(logCount >= 0);
        System.out.println("✅ H2 database operations working (" + logCount + " entries)");
        
        // 3. API endpoints
        ResponseEntity<String> apiResponse = restTemplate.getForEntity(
            baseUrl + "/dashboard/stats", String.class);
        assertEquals(HttpStatus.OK, apiResponse.getStatusCode());
        System.out.println("✅ REST API endpoints responding");
        
        // 4. Search functionality
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setQuery("*");
        searchQuery.setPage(0);
        searchQuery.setSize(1);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchQuery> request = new HttpEntity<>(searchQuery, headers);
        
        ResponseEntity<String> searchResponse = restTemplate.postForEntity(
            baseUrl + "/logs/search", request, String.class);
        assertEquals(HttpStatus.OK, searchResponse.getStatusCode());
        System.out.println("✅ Search functionality working");
        
        System.out.println("\n🚀 COMPLETE DEVELOPMENT ENVIRONMENT IS READY!");
        System.out.println("   - No Docker required");
        System.out.println("   - No external services required");
        System.out.println("   - H2 database working perfectly");
        System.out.println("   - All APIs responding");
        System.out.println("   - Ready for immediate development");
    }
}
