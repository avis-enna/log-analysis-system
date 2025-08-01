package com.loganalyzer.repository;

import com.loganalyzer.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA Repository interface for LogEntry entities.
 * Provides database-based search and query capabilities for local development.
 * This is used when Elasticsearch is not available.
 */
@Repository("logEntryJpaRepository")
public interface LogEntryJpaRepository extends JpaRepository<LogEntry, String> {
    
    // Basic queries
    Page<LogEntry> findByLevel(String level, Pageable pageable);
    
    Page<LogEntry> findBySource(String source, Pageable pageable);
    
    Page<LogEntry> findByHost(String host, Pageable pageable);
    
    Page<LogEntry> findByApplication(String application, Pageable pageable);
    
    Page<LogEntry> findByEnvironment(String environment, Pageable pageable);
    
    // Time-based queries
    Page<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<LogEntry> findByTimestampAfter(LocalDateTime timestamp, Pageable pageable);
    
    Page<LogEntry> findByTimestampBefore(LocalDateTime timestamp, Pageable pageable);
    
    // Combined queries
    Page<LogEntry> findByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<LogEntry> findBySourceAndTimestampBetween(String source, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<LogEntry> findByApplicationAndEnvironmentAndTimestampBetween(
        String application, String environment, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Full-text search queries (using SQL LIKE for H2)
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE %:message%")
    Page<LogEntry> findByMessageContaining(@Param("message") String message, Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE %:message% AND l.timestamp BETWEEN :start AND :end")
    Page<LogEntry> findByMessageContainingAndTimestampBetween(
        @Param("message") String message, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        Pageable pageable);
    
    // Multi-field search (simplified for SQL)
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE %:query% OR l.logger LIKE %:query% OR l.thread LIKE %:query%")
    Page<LogEntry> findByMultiFieldSearch(@Param("query") String query, Pageable pageable);
    
    // Error and exception queries
    @Query("SELECT l FROM LogEntry l WHERE l.level IN ('ERROR', 'FATAL')")
    Page<LogEntry> findErrorLogs(Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.stackTrace IS NOT NULL")
    Page<LogEntry> findLogsWithStackTrace(Pageable pageable);
    
    // HTTP-related queries
    @Query("SELECT l FROM LogEntry l WHERE l.httpMethod IS NOT NULL AND l.httpUrl IS NOT NULL")
    Page<LogEntry> findHttpLogs(Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.httpStatus >= 400")
    Page<LogEntry> findHttpErrorLogs(Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.responseTime >= :minResponseTime")
    Page<LogEntry> findSlowHttpRequests(@Param("minResponseTime") long minResponseTime, Pageable pageable);
    
    // Aggregation support queries
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN :start AND :end")
    List<LogEntry> findByTimestampBetweenForAggregation(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Count queries
    long countByLevel(String level);
    
    long countBySource(String source);
    
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    long countByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end);
    
    // Distinct queries with limits for performance
    @Query("SELECT DISTINCT l.source FROM LogEntry l ORDER BY l.source LIMIT 100")
    List<String> findDistinctSources();

    @Query("SELECT DISTINCT l.host FROM LogEntry l ORDER BY l.host LIMIT 100")
    List<String> findDistinctHosts();

    @Query("SELECT DISTINCT l.application FROM LogEntry l ORDER BY l.application LIMIT 100")
    List<String> findDistinctApplications();

    @Query("SELECT DISTINCT l.environment FROM LogEntry l ORDER BY l.environment LIMIT 20")
    List<String> findDistinctEnvironments();

    @Query("SELECT DISTINCT l.level FROM LogEntry l ORDER BY l.level")
    List<String> findDistinctLevels();
    
    // Recent logs
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp >= :oneHourAgo")
    Page<LogEntry> findRecentLogs(@Param("oneHourAgo") LocalDateTime oneHourAgo, Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp >= :twentyFourHoursAgo")
    Page<LogEntry> findLogsFromLast24Hours(@Param("twentyFourHoursAgo") LocalDateTime twentyFourHoursAgo, Pageable pageable);
    
    // Pattern-based queries (using SQL LIKE)
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE :pattern")
    Page<LogEntry> findByMessagePattern(@Param("pattern") String pattern, Pageable pageable);
    
    // User and session queries
    Page<LogEntry> findByUserId(String userId, Pageable pageable);
    
    Page<LogEntry> findBySessionId(String sessionId, Pageable pageable);
    
    Page<LogEntry> findByRequestId(String requestId, Pageable pageable);
    
    // Complex boolean queries
    @Query("SELECT l FROM LogEntry l WHERE l.level IN :levels AND l.timestamp BETWEEN :start AND :end")
    Page<LogEntry> findByLevelsAndTimestampBetween(
        @Param("levels") List<String> levels, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.source IN :sources AND l.timestamp BETWEEN :start AND :end")
    Page<LogEntry> findBySourcesAndTimestampBetween(
        @Param("sources") List<String> sources, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end, 
        Pageable pageable);
    
    // Performance monitoring
    @Query("SELECT l FROM LogEntry l WHERE l.processingTime >= :minProcessingTime")
    Page<LogEntry> findByMinProcessingTime(@Param("minProcessingTime") long minProcessingTime, Pageable pageable);
    
    // Simplified metadata and tag queries (for H2 compatibility)
    @Query("SELECT l FROM LogEntry l WHERE l.id IN (SELECT DISTINCT le.id FROM LogEntry le JOIN le.metadata m WHERE KEY(m) = :key AND VALUE(m) = :value)")
    Page<LogEntry> findByMetadataField(@Param("key") String key, @Param("value") String value, Pageable pageable);
    
    @Query("SELECT l FROM LogEntry l WHERE l.id IN (SELECT DISTINCT le.id FROM LogEntry le JOIN le.tags t WHERE KEY(t) = :key AND VALUE(t) = :value)")
    Page<LogEntry> findByTag(@Param("key") String key, @Param("value") String value, Pageable pageable);

    // Additional optimized queries for better performance
    @Query("SELECT l FROM LogEntry l WHERE " +
           "(:query IS NULL OR LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:level IS NULL OR l.level = :level) AND " +
           "(:source IS NULL OR l.source = :source) AND " +
           "(:startTime IS NULL OR l.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR l.timestamp <= :endTime) " +
           "ORDER BY l.timestamp DESC")
    Page<LogEntry> findByComplexQuery(
        @Param("query") String query,
        @Param("level") String level,
        @Param("source") String source,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    // Enhanced multi-field search with better performance (replaces the simpler version above)
    @Query("SELECT l FROM LogEntry l WHERE " +
           "LOWER(l.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.source) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.application) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.host) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.logger) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.thread) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY l.timestamp DESC")
    Page<LogEntry> findByEnhancedMultiFieldSearch(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Statistics queries for dashboard performance
    @Query("SELECT l.level, COUNT(l) FROM LogEntry l WHERE l.timestamp >= :since GROUP BY l.level")
    List<Object[]> getLogCountsByLevel(@Param("since") LocalDateTime since);

    @Query("SELECT l.source, COUNT(l) FROM LogEntry l WHERE l.timestamp >= :since GROUP BY l.source ORDER BY COUNT(l) DESC")
    List<Object[]> getLogCountsBySource(@Param("since") LocalDateTime since);
}
