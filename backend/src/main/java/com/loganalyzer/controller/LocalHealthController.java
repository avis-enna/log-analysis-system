package com.loganalyzer.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic health controller for local testing.
 */
@RestController
@RequestMapping("/health")
@Profile("local")
public class LocalHealthController {
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("mode", "local");
        health.put("message", "Backend is running in local development mode");
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("application", "Log Analysis System");
        info.put("version", "1.0.0");
        info.put("profile", "local");
        info.put("database", "H2 In-Memory");
        info.put("features", new String[]{"Basic API", "Health Checks", "Local Development"});
        
        return ResponseEntity.ok(info);
    }
}
