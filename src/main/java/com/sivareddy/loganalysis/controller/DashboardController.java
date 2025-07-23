package com.sivareddy.loganalysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Web controller for dashboard views
 * 
 * @author Siva Reddy
 */
@Controller
@RequestMapping("/")
public class DashboardController {
    
    /**
     * Main dashboard view
     */
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("title", "Log Analysis System - Dashboard");
        return "dashboard";
    }
    
    /**
     * Logs view
     */
    @GetMapping("/logs")
    public String logs(Model model) {
        model.addAttribute("title", "Log Analysis System - Logs");
        return "logs";
    }
    
    /**
     * Alerts view
     */
    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("title", "Log Analysis System - Alerts");
        return "alerts";
    }
    
    /**
     * Analytics view
     */
    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("title", "Log Analysis System - Analytics");
        return "analytics";
    }
}
