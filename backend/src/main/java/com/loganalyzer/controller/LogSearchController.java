package com.loganalyzer.controller;

import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple search controller without caching to fix hanging issues
 */
@RestController
@RequestMapping("/api/v1/logs-search")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000"})
public class LogSearchController {
    
    @Autowired
    private LogEntryJpaRepository logEntryRepository;
    
    /**
     * Simple search endpoint that works
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchLogs(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            
            Page<LogEntry> logs;
            if (q.trim().isEmpty()) {
                logs = logEntryRepository.findAll(pageable);
            } else {
                logs = logEntryRepository.findByMessageContaining(q, pageable);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            response.put("hasNext", logs.hasNext());
            response.put("hasPrevious", logs.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Search by level
     */
    @GetMapping("/search/level/{level}")
    public ResponseEntity<Map<String, Object>> searchByLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<LogEntry> logs = logEntryRepository.findByLevel(level.toUpperCase(), pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs.getContent());
            response.put("totalElements", logs.getTotalElements());
            response.put("totalPages", logs.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
