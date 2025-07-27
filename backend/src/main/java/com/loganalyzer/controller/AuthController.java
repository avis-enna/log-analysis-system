package com.loganalyzer.controller;

import com.loganalyzer.model.Role;
import com.loganalyzer.model.User;
import com.loganalyzer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Authentication and user management controller
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                .orElse(null);
            
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("fullName", user.getFullName());
            userInfo.put("roles", user.getRoles());
            userInfo.put("isAdmin", user.isAdmin());
            userInfo.put("isDeveloper", user.isDeveloper());
            userInfo.put("isQA", user.isQA());
            userInfo.put("lastLogin", user.getLastLogin());
            userInfo.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(userInfo);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get user permissions based on roles
     */
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }
            
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                .orElse(null);
            
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            Map<String, Object> permissions = new HashMap<>();
            
            // Admin permissions
            boolean isAdmin = user.hasRole(Role.ADMIN);
            permissions.put("canManageUsers", isAdmin);
            permissions.put("canViewSystemStats", isAdmin);
            permissions.put("canAccessAdminPanel", isAdmin);
            
            // Developer permissions
            boolean isDeveloper = user.hasRole(Role.DEVELOPER) || isAdmin;
            permissions.put("canIngestLogs", isDeveloper);
            permissions.put("canAccessDeployment", isDeveloper);
            permissions.put("canViewDetailedLogs", isDeveloper);
            
            // QA permissions
            boolean isQA = user.hasRole(Role.QA) || isAdmin;
            permissions.put("canAccessTesting", isQA);
            permissions.put("canViewMonitoring", isQA);
            permissions.put("canCreateReports", isQA);
            
            // Common permissions
            boolean hasAnyRole = isAdmin || isDeveloper || isQA;
            permissions.put("canViewDashboard", hasAnyRole);
            permissions.put("canSearchLogs", hasAnyRole);
            permissions.put("canViewAlerts", hasAnyRole);
            
            permissions.put("roles", user.getRoles());
            permissions.put("username", user.getUsername());
            
            return ResponseEntity.ok(permissions);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Login endpoint (for testing - in real app you'd use proper authentication)
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username and password required"));
            }
            
            User user = userRepository.findByUsername(username)
                .orElse(null);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
            
            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
            
            if (!user.isEnabled()) {
                return ResponseEntity.status(401).body(Map.of("error", "Account disabled"));
            }
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "fullName", user.getFullName(),
                "roles", user.getRoles()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get all available roles (admin only)
     */
    @GetMapping("/roles")
    public ResponseEntity<List<Map<String, Object>>> getAllRoles() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body(null);
            }
            
            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                .orElse(null);
            
            if (user == null || !user.isAdmin()) {
                return ResponseEntity.status(403).body(null);
            }
            
            List<Map<String, Object>> roles = new ArrayList<>();
            
            for (Role role : Role.values()) {
                Map<String, Object> roleInfo = new HashMap<>();
                roleInfo.put("name", role.name());
                roleInfo.put("authority", role.getAuthority());
                roleInfo.put("description", role.getDescription());
                roles.add(roleInfo);
            }
            
            return ResponseEntity.ok(roles);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    
    /**
     * Get hardcoded test credentials (for development only)
     */
    @GetMapping("/test-credentials")
    public ResponseEntity<Map<String, Object>> getTestCredentials() {
        Map<String, Object> credentials = new HashMap<>();
        
        List<Map<String, String>> users = Arrays.asList(
            Map.of("username", "admin", "password", "admin123", "role", "ADMIN", "description", "Full administrative access"),
            Map.of("username", "dev", "password", "dev123", "role", "DEVELOPER", "description", "Developer access (logs, deployment)"),
            Map.of("username", "qa", "password", "qa123", "role", "QA", "description", "QA access (testing, monitoring)"),
            Map.of("username", "devops", "password", "devops123", "role", "ADMIN+DEVELOPER", "description", "Admin + Developer access"),
            Map.of("username", "qaread", "password", "qaread123", "role", "QA", "description", "QA read-only access")
        );
        
        credentials.put("users", users);
        credentials.put("note", "These are hardcoded test credentials for development. Use HTTP Basic Auth.");
        credentials.put("example", "curl -u admin:admin123 http://localhost:8080/api/v1/dashboard/stats");
        
        return ResponseEntity.ok(credentials);
    }
}
