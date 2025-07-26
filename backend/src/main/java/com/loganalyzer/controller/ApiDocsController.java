package com.loganalyzer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

/**
 * API Documentation controller
 */
@RestController
@RequestMapping("/api/v1/docs")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000"})
public class ApiDocsController {
    
    /**
     * Get API documentation
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getApiDocs() {
        Map<String, Object> docs = new HashMap<>();
        
        docs.put("title", "Log Analysis System API");
        docs.put("version", "1.0.0");
        docs.put("description", "Enterprise Log Analysis and Search System");
        
        Map<String, Object> endpoints = new HashMap<>();
        
        // Dashboard endpoints
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("GET /api/v1/dashboard/stats", "Get dashboard statistics");
        dashboard.put("GET /api/v1/dashboard/realtime", "Get real-time metrics");
        dashboard.put("GET /api/v1/dashboard/volume", "Get log volume over time");
        dashboard.put("GET /api/v1/dashboard/top-sources", "Get top log sources");
        dashboard.put("GET /api/v1/dashboard/error-trends", "Get error trends");
        endpoints.put("Dashboard", dashboard);
        
        // Log endpoints
        Map<String, Object> logs = new HashMap<>();
        logs.put("GET /api/v1/logs", "Get paginated logs");
        logs.put("GET /api/v1/logs/{id}", "Get specific log entry");
        logs.put("POST /api/v1/logs/ingest", "Ingest single log entry");
        logs.put("POST /api/v1/logs/ingest/batch", "Ingest multiple log entries");
        logs.put("POST /api/v1/logs/generate-sample", "Generate sample logs for testing");
        endpoints.put("Logs", logs);
        
        // Search endpoints
        Map<String, Object> search = new HashMap<>();
        search.put("GET /api/v1/logs-search/search", "Simple log search (working)");
        search.put("GET /api/v1/logs-search/search/level/{level}", "Search by log level");
        search.put("POST /api/v1/search", "Advanced search (may have issues)");
        search.put("GET /api/v1/search/quick", "Quick text search (may have issues)");
        endpoints.put("Search", search);
        
        // Upload endpoints
        Map<String, Object> upload = new HashMap<>();
        upload.put("POST /api/v1/logs/upload/file", "Upload log file");
        upload.put("POST /api/v1/logs/upload/text", "Upload log text");
        endpoints.put("Upload", upload);
        
        // Documentation
        Map<String, Object> docsEndpoints = new HashMap<>();
        docsEndpoints.put("GET /api/v1/docs", "Get API documentation");
        docsEndpoints.put("GET /swagger-ui.html", "Swagger UI (if available)");
        endpoints.put("Documentation", docsEndpoints);
        
        docs.put("endpoints", endpoints);
        
        // Example usage
        Map<String, Object> examples = new HashMap<>();
        examples.put("Search logs", "GET /api/v1/logs-search/search?q=ERROR&page=0&size=10");
        examples.put("Get dashboard stats", "GET /api/v1/dashboard/stats");
        examples.put("Upload text logs", "POST /api/v1/logs/upload/text with text=your_log_data");
        examples.put("Generate sample data", "POST /api/v1/logs/generate-sample?count=50");
        docs.put("examples", examples);
        
        return ResponseEntity.ok(docs);
    }
    
    /**
     * Get system status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "RUNNING");
        status.put("message", "Log Analysis System is operational");
        status.put("timestamp", System.currentTimeMillis());
        status.put("version", "1.0.0");
        
        Map<String, Object> features = new HashMap<>();
        features.put("Dashboard", "✅ Operational");
        features.put("Log Storage", "✅ H2 Database Active");
        features.put("Search", "⚠️ Simple search working, advanced search has issues");
        features.put("Upload", "✅ File and text upload available");
        features.put("Real-time", "✅ WebSocket broadcasting active");
        status.put("features", features);
        
        return ResponseEntity.ok(status);
    }
}
