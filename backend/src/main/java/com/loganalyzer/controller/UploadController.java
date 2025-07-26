package com.loganalyzer.controller;

import com.loganalyzer.service.LogIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Simple upload controller for log files and text
 */
@RestController
@RequestMapping("/api/v1/upload")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000"})
public class UploadController {
    
    @Autowired
    private LogIngestionService logIngestionService;
    
    /**
     * Upload log file
     */
    @PostMapping("/logs")
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
    @PostMapping("/text")
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
    
    /**
     * Get upload status/info
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getUploadStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ready");
        status.put("message", "Upload service is operational");
        status.put("supportedFormats", new String[]{"text/plain", "application/json", "text/csv"});
        status.put("maxFileSize", "10MB");
        
        return ResponseEntity.ok(status);
    }
}
