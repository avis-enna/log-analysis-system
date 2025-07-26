package com.loganalyzer.service;

import com.loganalyzer.dto.SearchResult;
import com.loganalyzer.model.LogEntry;
import com.loganalyzer.repository.LogEntryJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple search service using JPA queries instead of Elasticsearch.
 * Provides basic log search functionality with simplified features.
 */
@Service
public class SimpleSearchService {

    @Autowired
    private LogEntryJpaRepository logEntryRepository;

    /**
     * Search logs with basic text matching
     */
    public SearchResult searchLogs(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        Page<LogEntry> logPage;
        if (query == null || query.trim().isEmpty()) {
            logPage = logEntryRepository.findAll(pageable);
        } else {
            logPage = logEntryRepository.findByMessageContaining(query.trim(), pageable);
        }
        
        return new SearchResult(
            logPage.getContent(),
            logPage.getTotalElements(),
            logPage.getNumber(),
            logPage.getSize()
        );
    }

    /**
     * Search logs by level
     */
    public SearchResult searchByLevel(String level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntry> logPage = logEntryRepository.findByLevel(level, pageable);
        
        return new SearchResult(
            logPage.getContent(),
            logPage.getTotalElements(),
            logPage.getNumber(),
            logPage.getSize()
        );
    }

    /**
     * Search logs by time range
     */
    public SearchResult searchByTimeRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntry> logPage = logEntryRepository.findByTimestampBetween(start, end, pageable);
        
        return new SearchResult(
            logPage.getContent(),
            logPage.getTotalElements(),
            logPage.getNumber(),
            logPage.getSize()
        );
    }

    /**
     * Get basic aggregations
     */
    public Map<String, Object> getLogAggregations() {
        Map<String, Object> aggregations = new HashMap<>();
        
        // Simple counts by level
        List<String> levels = List.of("ERROR", "WARN", "INFO", "DEBUG");
        Map<String, Long> levelCounts = new HashMap<>();
        
        for (String level : levels) {
            long count = logEntryRepository.countByLevel(level);
            levelCounts.put(level, count);
        }
        
        aggregations.put("levelCounts", levelCounts);
        aggregations.put("totalLogs", logEntryRepository.count());
        
        return aggregations;
    }

    /**
     * Find error logs
     */
    public SearchResult findErrorLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntry> logPage = logEntryRepository.findErrorLogs(pageable);
        
        return new SearchResult(
            logPage.getContent(),
            logPage.getTotalElements(),
            logPage.getNumber(),
            logPage.getSize()
        );
    }

    /**
     * Get recent logs (last hour)
     */
    public SearchResult getRecentLogs(int page, int size) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<LogEntry> logPage = logEntryRepository.findRecentLogs(oneHourAgo, pageable);
        
        return new SearchResult(
            logPage.getContent(),
            logPage.getTotalElements(),
            logPage.getNumber(),
            logPage.getSize()
        );
    }
}
