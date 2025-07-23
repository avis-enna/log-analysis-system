package com.loganalyzer.repository;

import com.loganalyzer.model.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for LogEntry entities.
 * Provides Elasticsearch-based search and query capabilities.
 */
@Repository
public interface LogEntryRepository extends ElasticsearchRepository<LogEntry, String> {
    
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
    
    // Full-text search queries
    @Query("{\"bool\": {\"must\": [{\"match\": {\"message\": \"?0\"}}]}}")
    Page<LogEntry> findByMessageContaining(String message, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"match\": {\"message\": \"?0\"}}, {\"range\": {\"timestamp\": {\"gte\": \"?1\", \"lte\": \"?2\"}}}]}}")
    Page<LogEntry> findByMessageContainingAndTimestampBetween(String message, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Multi-field search
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"message^2\", \"logger\", \"thread\", \"stackTrace\"]}}")
    Page<LogEntry> findByMultiFieldSearch(String query, Pageable pageable);
    
    // Error and exception queries
    @Query("{\"bool\": {\"should\": [{\"match\": {\"level\": \"ERROR\"}}, {\"match\": {\"level\": \"FATAL\"}}]}}")
    Page<LogEntry> findErrorLogs(Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"exists\": {\"field\": \"stackTrace\"}}]}}")
    Page<LogEntry> findLogsWithStackTrace(Pageable pageable);
    
    // HTTP-related queries
    @Query("{\"bool\": {\"must\": [{\"exists\": {\"field\": \"httpMethod\"}}, {\"exists\": {\"field\": \"httpUrl\"}}]}}")
    Page<LogEntry> findHttpLogs(Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"httpStatus\": {\"gte\": 400}}}]}}")
    Page<LogEntry> findHttpErrorLogs(Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"responseTime\": {\"gte\": ?0}}}]}}")
    Page<LogEntry> findSlowHttpRequests(long minResponseTime, Pageable pageable);
    
    // Aggregation support queries
    @Query("{\"bool\": {\"must\": [{\"range\": {\"timestamp\": {\"gte\": \"?0\", \"lte\": \"?1\"}}}]}}")
    List<LogEntry> findByTimestampBetweenForAggregation(LocalDateTime start, LocalDateTime end);
    
    // Count queries
    long countByLevel(String level);
    
    long countBySource(String source);
    
    long countByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    long countByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end);
    
    // Distinct queries
    @Query("{\"aggs\": {\"distinct_sources\": {\"terms\": {\"field\": \"source.keyword\", \"size\": 1000}}}}")
    List<String> findDistinctSources();
    
    @Query("{\"aggs\": {\"distinct_hosts\": {\"terms\": {\"field\": \"host.keyword\", \"size\": 1000}}}}")
    List<String> findDistinctHosts();
    
    @Query("{\"aggs\": {\"distinct_applications\": {\"terms\": {\"field\": \"application.keyword\", \"size\": 1000}}}}")
    List<String> findDistinctApplications();
    
    @Query("{\"aggs\": {\"distinct_environments\": {\"terms\": {\"field\": \"environment.keyword\", \"size\": 1000}}}}")
    List<String> findDistinctEnvironments();
    
    // Recent logs
    @Query("{\"bool\": {\"must\": [{\"range\": {\"timestamp\": {\"gte\": \"now-1h\"}}}]}}")
    Page<LogEntry> findRecentLogs(Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"range\": {\"timestamp\": {\"gte\": \"now-24h\"}}}]}}")
    Page<LogEntry> findLogsFromLast24Hours(Pageable pageable);
    
    // Pattern-based queries
    @Query("{\"bool\": {\"must\": [{\"wildcard\": {\"message.keyword\": \"*?0*\"}}]}}")
    Page<LogEntry> findByMessagePattern(String pattern, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"regexp\": {\"message.keyword\": \"?0\"}}]}}")
    Page<LogEntry> findByMessageRegex(String regex, Pageable pageable);
    
    // User and session queries
    Page<LogEntry> findByUserId(String userId, Pageable pageable);
    
    Page<LogEntry> findBySessionId(String sessionId, Pageable pageable);
    
    Page<LogEntry> findByRequestId(String requestId, Pageable pageable);
    
    // Complex boolean queries
    @Query("{\"bool\": {\"must\": [{\"terms\": {\"level.keyword\": ?0}}], \"filter\": [{\"range\": {\"timestamp\": {\"gte\": \"?1\", \"lte\": \"?2\"}}}]}}")
    Page<LogEntry> findByLevelsAndTimestampBetween(List<String> levels, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    @Query("{\"bool\": {\"must\": [{\"terms\": {\"source.keyword\": ?0}}], \"filter\": [{\"range\": {\"timestamp\": {\"gte\": \"?1\", \"lte\": \"?2\"}}}]}}")
    Page<LogEntry> findBySourcesAndTimestampBetween(List<String> sources, LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Performance monitoring
    @Query("{\"bool\": {\"must\": [{\"range\": {\"processingTime\": {\"gte\": ?0}}}]}}")
    Page<LogEntry> findByMinProcessingTime(long minProcessingTime, Pageable pageable);
    
    // Metadata queries
    @Query("{\"bool\": {\"must\": [{\"nested\": {\"path\": \"metadata\", \"query\": {\"bool\": {\"must\": [{\"match\": {\"metadata.?0\": \"?1\"}}]}}}}]}}")
    Page<LogEntry> findByMetadataField(String key, String value, Pageable pageable);
    
    // Tag-based queries
    @Query("{\"bool\": {\"must\": [{\"nested\": {\"path\": \"tags\", \"query\": {\"bool\": {\"must\": [{\"match\": {\"tags.?0\": \"?1\"}}]}}}}]}}")
    Page<LogEntry> findByTag(String key, String value, Pageable pageable);
}
