package com.loganalyzer.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache configuration for the log analyzer application.
 * Provides in-memory caching for frequently accessed data.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {
    
    /**
     * Cache manager for local development and testing.
     * Uses in-memory concurrent maps for simplicity.
     */
    @Bean
    @Profile({"local", "test", "default"})
    public CacheManager localCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Pre-configure cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "searchResults",      // Search query results
            "logStatistics",      // Log statistics and aggregations
            "fieldSuggestions",   // Field value suggestions
            "availableFields",    // Available field names
            "recentLogs",         // Recent log entries
            "alertCounts",        // Alert count statistics
            "dashboardData",      // Dashboard widget data
            "userPreferences"     // User preferences and settings
        ));
        
        // Allow dynamic cache creation
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
    
    /**
     * Cache configuration properties
     */
    public static class CacheNames {
        public static final String SEARCH_RESULTS = "searchResults";
        public static final String LOG_STATISTICS = "logStatistics";
        public static final String FIELD_SUGGESTIONS = "fieldSuggestions";
        public static final String AVAILABLE_FIELDS = "availableFields";
        public static final String RECENT_LOGS = "recentLogs";
        public static final String ALERT_COUNTS = "alertCounts";
        public static final String DASHBOARD_DATA = "dashboardData";
        public static final String USER_PREFERENCES = "userPreferences";
    }
    
    /**
     * Cache TTL configuration (in seconds)
     */
    public static class CacheTTL {
        public static final int SEARCH_RESULTS = 300;      // 5 minutes
        public static final int LOG_STATISTICS = 60;       // 1 minute
        public static final int FIELD_SUGGESTIONS = 1800;  // 30 minutes
        public static final int AVAILABLE_FIELDS = 3600;   // 1 hour
        public static final int RECENT_LOGS = 30;          // 30 seconds
        public static final int ALERT_COUNTS = 60;         // 1 minute
        public static final int DASHBOARD_DATA = 60;       // 1 minute
        public static final int USER_PREFERENCES = 3600;   // 1 hour
    }
}
