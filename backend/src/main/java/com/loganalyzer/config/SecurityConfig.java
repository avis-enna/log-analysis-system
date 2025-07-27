package com.loganalyzer.config;

import com.loganalyzer.model.Role;
import com.loganalyzer.model.User;
import com.loganalyzer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Security configuration with role-based access control
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Autowired
    private UserRepository userRepository;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                
                // Admin only endpoints
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                
                // Developer endpoints
                .requestMatchers("/api/v1/logs/ingest").hasAnyRole("ADMIN", "DEVELOPER")
                .requestMatchers("/api/v1/deployment/**").hasAnyRole("ADMIN", "DEVELOPER")
                
                // QA endpoints
                .requestMatchers("/api/v1/testing/**").hasAnyRole("ADMIN", "QA")
                .requestMatchers("/api/v1/monitoring/**").hasAnyRole("ADMIN", "QA")
                
                // Common authenticated endpoints
                .requestMatchers("/api/v1/dashboard/**").hasAnyRole("ADMIN", "DEVELOPER", "QA")
                .requestMatchers("/api/v1/logs/search").hasAnyRole("ADMIN", "DEVELOPER", "QA")
                .requestMatchers("/api/v1/alerts/**").hasAnyRole("ADMIN", "DEVELOPER", "QA")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {}); // Enable HTTP Basic authentication for API access
        
        return http.build();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
            
            List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toList());
            
            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
        };
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Initialize hardcoded users on application startup
     */
    @Bean
    public CommandLineRunner initializeUsers() {
        return args -> {
            PasswordEncoder encoder = passwordEncoder();
            
            // Create admin user
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User(
                    "admin",
                    encoder.encode("admin123"),
                    "admin@loganalyzer.com",
                    "System Administrator",
                    Set.of(Role.ADMIN)
                );
                userRepository.save(admin);
                System.out.println("Created admin user: admin / admin123");
            }
            
            // Create developer user
            if (!userRepository.existsByUsername("dev")) {
                User developer = new User(
                    "dev",
                    encoder.encode("dev123"),
                    "developer@loganalyzer.com",
                    "Lead Developer",
                    Set.of(Role.DEVELOPER)
                );
                userRepository.save(developer);
                System.out.println("Created developer user: dev / dev123");
            }
            
            // Create QA user
            if (!userRepository.existsByUsername("qa")) {
                User qa = new User(
                    "qa",
                    encoder.encode("qa123"),
                    "qa@loganalyzer.com",
                    "QA Engineer",
                    Set.of(Role.QA)
                );
                userRepository.save(qa);
                System.out.println("Created QA user: qa / qa123");
            }
            
            // Create multi-role user (admin + developer)
            if (!userRepository.existsByUsername("devops")) {
                User devops = new User(
                    "devops",
                    encoder.encode("devops123"),
                    "devops@loganalyzer.com",
                    "DevOps Engineer",
                    Set.of(Role.ADMIN, Role.DEVELOPER)
                );
                userRepository.save(devops);
                System.out.println("Created DevOps user: devops / devops123");
            }
            
            // Create read-only QA user
            if (!userRepository.existsByUsername("qaread")) {
                User qaRead = new User(
                    "qaread",
                    encoder.encode("qaread123"),
                    "qaread@loganalyzer.com",
                    "QA Read-Only",
                    Set.of(Role.QA)
                );
                userRepository.save(qaRead);
                System.out.println("Created QA read-only user: qaread / qaread123");
            }
            
            System.out.println("\n=== Available Users ===");
            System.out.println("admin / admin123 - Full administrative access");
            System.out.println("dev / dev123 - Developer access (logs, deployment)");
            System.out.println("qa / qa123 - QA access (testing, monitoring)");
            System.out.println("devops / devops123 - Admin + Developer access");
            System.out.println("qaread / qaread123 - QA read-only access");
            System.out.println("========================\n");
        };
    }
}
