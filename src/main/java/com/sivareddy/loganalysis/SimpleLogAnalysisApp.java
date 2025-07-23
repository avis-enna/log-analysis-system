package com.sivareddy.loganalysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Log Analysis System Demo
 * 
 * @author Siva Reddy
 */
@SpringBootApplication
public class LogAnalysisSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalysisSystemApplication.class, args);
    }
}

@Controller
class DashboardController {
    
    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("title", "Log Analysis System - Dashboard");
        return "dashboard";
    }
}

@RestController
class ApiController {
    
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "Log Analysis System");
        health.put("version", "1.0.0");
        return health;
    }
    
    @GetMapping("/api/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalLogs", 15420);
        stats.put("errorLogs", 85);
        stats.put("warningLogs", 234);
        stats.put("infoLogs", 15101);
        stats.put("unresolvedAlerts", 12);
        stats.put("criticalAlerts", 3);
        stats.put("sources", new String[]{"nginx", "application", "database", "security"});
        stats.put("uptime", "Running");
        stats.put("lastUpdated", LocalDateTime.now().toString());
        return stats;
    }
}
