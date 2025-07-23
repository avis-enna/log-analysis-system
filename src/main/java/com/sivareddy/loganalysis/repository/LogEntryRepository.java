package com.sivareddy.loganalysis.repository;

import com.sivareddy.loganalysis.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for LogEntry entities
 * 
 * @author Siva Reddy
 */
@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    /**
     * Find log entries by level
     */
    Page<LogEntry> findByLevel(String level, Pageable pageable);
    
    /**
     * Find log entries by source
     */
    Page<LogEntry> findBySource(String source, Pageable pageable);
    
    /**
     * Find log entries by level and source
     */
    Page<LogEntry> findByLevelAndSource(String level, String source, Pageable pageable);
    
    /**
     * Find log entries within a time range
     */
    Page<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * Find log entries by IP address
     */
    Page<LogEntry> findByIpAddress(String ipAddress, Pageable pageable);
    
    /**
     * Search log entries by message content
     */
    @Query("SELECT l FROM LogEntry l WHERE l.message LIKE %:searchTerm%")
    Page<LogEntry> findByMessageContaining(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find unprocessed log entries
     */
    List<LogEntry> findByProcessedFalse();
    
    /**
     * Count log entries by level within time range
     */
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.level = :level AND l.timestamp >= :since")
    Long countByLevelSince(@Param("level") String level, @Param("since") LocalDateTime since);
    
    /**
     * Count log entries by source within time range
     */
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.source = :source AND l.timestamp >= :since")
    Long countBySourceSince(@Param("source") String source, @Param("since") LocalDateTime since);
    
    /**
     * Get distinct sources
     */
    @Query("SELECT DISTINCT l.source FROM LogEntry l")
    List<String> findDistinctSources();
    
    /**
     * Get distinct levels
     */
    @Query("SELECT DISTINCT l.level FROM LogEntry l")
    List<String> findDistinctLevels();
    
    /**
     * Get log entries with high response times
     */
    @Query("SELECT l FROM LogEntry l WHERE l.responseTime > :threshold AND l.timestamp >= :since")
    List<LogEntry> findSlowRequests(@Param("threshold") Long threshold, @Param("since") LocalDateTime since);
    
    /**
     * Get log entries with error status codes
     */
    @Query("SELECT l FROM LogEntry l WHERE l.statusCode >= 400 AND l.timestamp >= :since")
    List<LogEntry> findErrorRequests(@Param("since") LocalDateTime since);
    
    /**
     * Get log entries by IP address with failed attempts
     */
    @Query("SELECT l FROM LogEntry l WHERE l.ipAddress = :ipAddress AND l.message LIKE '%failed%' AND l.timestamp >= :since")
    List<LogEntry> findFailedAttemptsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);
    
    /**
     * Get hourly log counts for timeline chart
     */
    @Query(value = "SELECT DATE_TRUNC('hour', timestamp) as hour, level, COUNT(*) as count " +
                   "FROM log_entries " +
                   "WHERE timestamp >= :since " +
                   "GROUP BY hour, level " +
                   "ORDER BY hour", 
           nativeQuery = true)
    List<Object[]> getHourlyLogCounts(@Param("since") LocalDateTime since);
    
    /**
     * Get top IP addresses by request count
     */
    @Query(value = "SELECT ip_address, COUNT(*) as count " +
                   "FROM log_entries " +
                   "WHERE timestamp >= :since AND ip_address IS NOT NULL " +
                   "GROUP BY ip_address " +
                   "ORDER BY count DESC " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> getTopIpAddresses(@Param("since") LocalDateTime since, @Param("limit") int limit);
    
    /**
     * Get log statistics by source
     */
    @Query(value = "SELECT source, " +
                   "COUNT(*) as total_count, " +
                   "COUNT(CASE WHEN level = 'ERROR' THEN 1 END) as error_count, " +
                   "COUNT(CASE WHEN level = 'WARNING' THEN 1 END) as warning_count, " +
                   "COUNT(CASE WHEN level = 'INFO' THEN 1 END) as info_count " +
                   "FROM log_entries " +
                   "WHERE timestamp >= :since " +
                   "GROUP BY source " +
                   "ORDER BY total_count DESC", 
           nativeQuery = true)
    List<Object[]> getLogStatsBySource(@Param("since") LocalDateTime since);
}
