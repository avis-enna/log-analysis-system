package com.loganalyzer.controller;

import com.loganalyzer.dto.LogEntryDTO;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import com.loganalyzer.service.LogIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Logs controller for log entry operations
 */
@RestController
@RequestMapping("/logs")
@CrossOrigin(origins = "*")
public class LogsController {

    @Autowired
    private LogEntryJpaRepository logEntryRepository;

    @Autowired
    private LogIngestionService logIngestionService;

    /**
     * Get paginated logs
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String query) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> logPage;
            
            if (query != null && !query.trim().isEmpty()) {
                // Use search functionality if query is provided
                logPage = logEntryRepository.searchByQuery(query.trim(), pageable);
            } else {
                // Get all logs if no search query
                logPage = logEntryRepository.findAll(pageable);
            }
            
            List<LogEntryDTO> logDTOs = logPage.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logDTOs);
            response.put("totalElements", logPage.getTotalElements());
            response.put("totalPages", logPage.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("hasNext", logPage.hasNext());
            response.put("hasPrevious", logPage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get log by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogEntryDTO> getLogById(@PathVariable String id) {
        try {
            return logEntryRepository.findById(id)
                    .map(this::convertToDTO)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convert LogEntry to DTO
     */
    private LogEntryDTO convertToDTO(LogEntry logEntry) {
        LogEntryDTO dto = new LogEntryDTO();
        dto.setId(logEntry.getId());
        dto.setTimestamp(logEntry.getTimestamp());
        dto.setLevel(logEntry.getLevel());
        dto.setMessage(logEntry.getMessage());
        dto.setSource(logEntry.getSource());
        dto.setApplication(logEntry.getApplication());
        dto.setHost(logEntry.getHost());
        dto.setLogger(logEntry.getLogger());
        dto.setThread(logEntry.getThread());
        dto.setUserId(logEntry.getUserId());
        dto.setSessionId(logEntry.getSessionId());
        dto.setRequestId(logEntry.getRequestId());
        dto.setStackTrace(logEntry.getStackTrace());
        dto.setHttpMethod(logEntry.getHttpMethod());
        dto.setHttpUrl(logEntry.getHttpUrl());
        dto.setHttpStatus(logEntry.getHttpStatus());
        dto.setResponseTime(logEntry.getResponseTime());
        dto.setProcessingTime(logEntry.getProcessingTime());
        dto.setEnvironment(logEntry.getEnvironment());
        dto.setCategory(logEntry.getCategory());
        dto.setSeverity(logEntry.getSeverity());
        dto.setParsed(logEntry.getParsed());
        dto.setOriginalFormat(logEntry.getOriginalFormat());
        return dto;
    }

    /**
     * Ingest a single log entry
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingestLog(
            @RequestBody Map<String, String> request) {
        try {
            String rawLog = request.get("log");
            String source = request.getOrDefault("source", "manual");
            
            if (rawLog == null || rawLog.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Log content is required"));
            }
            
            CompletableFuture<LogEntry> future = logIngestionService.ingestLog(rawLog, source);
            LogEntry logEntry = future.get(); // Wait for completion
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Log ingested successfully");
            response.put("logId", logEntry.getId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to ingest log: " + e.getMessage()));
        }
    }

    /**
     * Ingest multiple log entries
     */
    @PostMapping("/ingest/batch")
    public ResponseEntity<Map<String, Object>> ingestLogsBatch(
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> rawLogs = (List<String>) request.get("logs");
            String source = (String) request.getOrDefault("source", "manual");
            
            if (rawLogs == null || rawLogs.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Logs array is required"));
            }
            
            CompletableFuture<List<LogEntry>> future = logIngestionService.ingestLogsBatch(rawLogs, source);
            List<LogEntry> logEntries = future.get(); // Wait for completion
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Logs ingested successfully");
            response.put("count", logEntries.size());
            response.put("logIds", logEntries.stream().map(LogEntry::getId).collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to ingest logs: " + e.getMessage()));
        }
    }

    /**
     * Generate sample log data for testing
     */
    @PostMapping("/generate-sample")
    public ResponseEntity<Map<String, Object>> generateSampleLogs(
            @RequestParam(defaultValue = "100") int count) {
        try {
            List<String> sampleLogs = generateSampleLogData(count);
            
            CompletableFuture<List<LogEntry>> future = logIngestionService.ingestLogsBatch(sampleLogs, "sample-generator");
            List<LogEntry> logEntries = future.get(); // Wait for completion
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sample logs generated successfully");
            response.put("count", logEntries.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to generate sample logs: " + e.getMessage()));
        }
    }

    private List<String> generateSampleLogData(int count) {
        String[] levels = {"INFO", "WARNING", "ERROR", "DEBUG", "CRITICAL"};
        String[] sources = {"nginx", "application", "database", "security", "firewall", "auth-service"};
        String[] hosts = {"web-01", "web-02", "db-master", "db-slave", "cache-01", "lb-01"};
        String[] messages = {
            "User login successful",
            "Database connection established",
            "Cache cleared successfully", 
            "Service restart completed",
            "Authentication failed for user admin",
            "High memory usage detected: 85%",
            "Slow database query: 2500ms",
            "SSL certificate expires in 30 days",
            "Rate limit exceeded for IP 192.168.1.100",
            "SQL injection attempt detected",
            "File upload completed successfully",
            "Backup process initiated",
            "Configuration reloaded",
            "Health check passed",
            "Network unreachable error",
            "Payment processing failed",
            "Session timeout occurred",
            "Disk space running low: 90% used"
        };
        
        List<String> logs = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < count; i++) {
            String level = levels[random.nextInt(levels.length)];
            String source = sources[random.nextInt(sources.length)];
            String host = hosts[random.nextInt(hosts.length)];
            String message = messages[random.nextInt(messages.length)];
            
            // Generate log in different formats
            String log;
            switch (random.nextInt(3)) {
                case 0: // Apache format
                    log = String.format("192.168.1.%d - - [%s] \"GET /api/test HTTP/1.1\" %d %d \"%s\" \"Mozilla/5.0\"",
                        random.nextInt(255),
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss +0000")),
                        level.equals("ERROR") ? 500 : 200,
                        random.nextInt(5000),
                        message);
                    break;
                case 1: // Application log format
                    log = String.format("%s [%s] %s %s.%s - %s",
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS")),
                        Thread.currentThread().getName(),
                        level,
                        source,
                        host,
                        message);
                    break;
                default: // Syslog format
                    log = String.format("%s %s %s[%d]: %s - %s",
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd HH:mm:ss")),
                        host,
                        source,
                        random.nextInt(9999),
                        level,
                        message);
                    break;
            }
            
            logs.add(log);
        }
        
        return logs;
    }
    
    /**
     * Upload log file
     */
    @PostMapping("/upload/file")
    public ResponseEntity<Map<String, Object>> uploadLogFile(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            String content = new String(file.getBytes());
            String[] lines = content.split("\n");
            
            // Process each line as a log entry
            int processed = 0;
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    try {
                        CompletableFuture.runAsync(() -> {
                            try {
                                logIngestionService.ingestLog(line.trim(), "file-upload");
                            } catch (Exception e) {
                                // Log error but continue processing
                            }
                        });
                        processed++;
                    } catch (Exception e) {
                        // Skip problematic lines
                    }
                }
            }
            
            response.put("message", "File uploaded successfully");
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("linesProcessed", processed);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Failed to process file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Upload log text directly
     */
    @PostMapping("/upload/text")
    public ResponseEntity<Map<String, Object>> uploadLogText(@RequestParam("text") String text) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (text == null || text.trim().isEmpty()) {
                response.put("error", "Text is empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            String[] lines = text.split("\n");
            int processed = 0;
            
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    try {
                        CompletableFuture.runAsync(() -> {
                            try {
                                logIngestionService.ingestLog(line.trim(), "text-upload");
                            } catch (Exception e) {
                                // Log error but continue processing
                            }
                        });
                        processed++;
                    } catch (Exception e) {
                        // Skip problematic lines
                    }
                }
            }
            
            response.put("message", "Text uploaded successfully");
            response.put("linesProcessed", processed);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Failed to process text: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
