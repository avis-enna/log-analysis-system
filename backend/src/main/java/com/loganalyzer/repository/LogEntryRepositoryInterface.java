package com.loganalyzer.repository;

import com.loganalyzer.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Common interface for LogEntry repositories.
 * This allows both Elasticsearch and local implementations to be used interchangeably.
 */
public interface LogEntryRepositoryInterface extends CrudRepository<LogEntry, String> {
    
    // Basic queries
    Page<LogEntry> findByLevel(String level, Pageable pageable);
    
    Page<LogEntry> findBySource(String source, Pageable pageable);
    
    Page<LogEntry> findByHost(String host, Pageable pageable);
    
    Page<LogEntry> findByApplication(String application, Pageable pageable);
    
    Page<LogEntry> findByEnvironment(String environment, Pageable pageable);
    
    // Time-based queries
    Page<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Text search
    Page<LogEntry> findByMessageContaining(String message, Pageable pageable);
    
    // Combined queries
    Page<LogEntry> findByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Analytics queries  
    Page<LogEntry> findByLevelInOrderByTimestampDesc(List<String> levels, Pageable pageable);
    
    List<LogEntry> findByLevelOrderByTimestampDesc(String level, Pageable pageable);
    
    // Count queries
    long countByLevel(String level);
    
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    // Search functionality
    @Query("SELECT l FROM LogEntry l WHERE " +
           "LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.source) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.application) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.host) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.logger) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.thread) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY l.timestamp DESC")
    Page<LogEntry> searchByQuery(@Param("query") String query, Pageable pageable);
    
    // Paginated findAll method
    @Query("SELECT l FROM LogEntry l ORDER BY l.timestamp DESC")
    Page<LogEntry> findAllPaged(Pageable pageable);
}
