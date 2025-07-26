package com.loganalyzer.repository;

import com.loganalyzer.model.LogEntry;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Local in-memory implementation of LogEntryRepository for development and testing.
 * This replaces the Elasticsearch-based repository when running in 'local' profile.
 */
@Repository
@Profile("local")
public class LocalLogEntryRepository implements LogEntryRepositoryInterface {
    
    private final Map<String, LogEntry> logEntries = new ConcurrentHashMap<>();
    private long idCounter = 1;
    
    @Override
    public Page<LogEntry> findByLevel(String level, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getLevel().equalsIgnoreCase(level))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findBySource(String source, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getSource().contains(source))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByHost(String host, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getHost() != null && entry.getHost().contains(host))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByApplication(String application, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getApplication() != null && entry.getApplication().contains(application))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByEnvironment(String environment, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getEnvironment() != null && entry.getEnvironment().contains(environment))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getTimestamp().isAfter(start) && entry.getTimestamp().isBefore(end))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByMessageContaining(String message, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getMessage().toLowerCase().contains(message.toLowerCase()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByLevelAndTimestampBetween(String level, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> entry.getLevel().equalsIgnoreCase(level) &&
                           entry.getTimestamp().isAfter(start) && 
                           entry.getTimestamp().isBefore(end))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findByLevelInOrderByTimestampDesc(List<String> levels, Pageable pageable) {
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> levels.contains(entry.getLevel().toUpperCase()))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public List<LogEntry> findByLevelOrderByTimestampDesc(String level, Pageable pageable) {
        return logEntries.values().stream()
            .filter(entry -> entry.getLevel().equalsIgnoreCase(level))
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .collect(Collectors.toList());
    }
    
    @Override
    public long countByLevel(String level) {
        return logEntries.values().stream()
            .filter(entry -> entry.getLevel().equalsIgnoreCase(level))
            .count();
    }
    
    @Override
    public long countByTimestampBetween(LocalDateTime start, LocalDateTime end) {
        return logEntries.values().stream()
            .filter(entry -> entry.getTimestamp().isAfter(start) && entry.getTimestamp().isBefore(end))
            .count();
    }
    
    @Override
    public Page<LogEntry> searchByQuery(String query, Pageable pageable) {
        // Simple text search across message, source, host, and application fields
        String lowerQuery = query.toLowerCase();
        List<LogEntry> filtered = logEntries.values().stream()
            .filter(entry -> 
                entry.getMessage().toLowerCase().contains(lowerQuery) ||
                entry.getSource().toLowerCase().contains(lowerQuery) ||
                (entry.getHost() != null && entry.getHost().toLowerCase().contains(lowerQuery)) ||
                (entry.getApplication() != null && entry.getApplication().toLowerCase().contains(lowerQuery))
            )
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(filtered, pageable);
    }
    
    @Override
    public Page<LogEntry> findAllPaged(Pageable pageable) {
        List<LogEntry> all = logEntries.values().stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .collect(Collectors.toList());
        
        return createPage(all, pageable);
    }
    
    // Basic CRUD operations
    @Override
    public <S extends LogEntry> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(String.valueOf(idCounter++));
        }
        logEntries.put(entity.getId(), entity);
        return entity;
    }
    
    @Override
    public <S extends LogEntry> Iterable<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }
    
    @Override
    public Optional<LogEntry> findById(String id) {
        return Optional.ofNullable(logEntries.get(id));
    }
    
    @Override
    public boolean existsById(String id) {
        return logEntries.containsKey(id);
    }
    
    @Override
    public Iterable<LogEntry> findAll() {
        return logEntries.values();
    }
    
    @Override
    public Iterable<LogEntry> findAllById(Iterable<String> ids) {
        List<LogEntry> result = new ArrayList<>();
        for (String id : ids) {
            LogEntry entry = logEntries.get(id);
            if (entry != null) {
                result.add(entry);
            }
        }
        return result;
    }
    
    @Override
    public long count() {
        return logEntries.size();
    }
    
    @Override
    public void deleteById(String id) {
        logEntries.remove(id);
    }
    
    @Override
    public void delete(LogEntry entity) {
        logEntries.remove(entity.getId());
    }
    
    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        for (String id : ids) {
            logEntries.remove(id);
        }
    }
    
    @Override
    public void deleteAll(Iterable<? extends LogEntry> entities) {
        for (LogEntry entity : entities) {
            logEntries.remove(entity.getId());
        }
    }
    
    @Override
    public void deleteAll() {
        logEntries.clear();
    }
    
    // Helper method to create paginated results
    private Page<LogEntry> createPage(List<LogEntry> entries, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), entries.size());
        
        if (start > entries.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, entries.size());
        }
        
        List<LogEntry> pageContent = entries.subList(start, end);
        return new PageImpl<>(pageContent, pageable, entries.size());
    }
    
    // Initialize with some sample data
    public void initializeSampleData() {
        if (logEntries.isEmpty()) {
            LogEntry sample1 = new LogEntry();
            sample1.setId("1");
            sample1.setTimestamp(LocalDateTime.now().minusMinutes(5));
            sample1.setLevel("INFO");
            sample1.setMessage("Application started successfully");
            sample1.setSource("com.loganalyzer.Application");
            sample1.setHost("localhost");
            sample1.setApplication("log-analyzer");
            sample1.setEnvironment("local");
            save(sample1);
            
            LogEntry sample2 = new LogEntry();
            sample2.setId("2");
            sample2.setTimestamp(LocalDateTime.now().minusMinutes(3));
            sample2.setLevel("ERROR");
            sample2.setMessage("Database connection failed");
            sample2.setSource("com.loganalyzer.database.ConnectionManager");
            sample2.setHost("localhost");
            sample2.setApplication("log-analyzer");
            sample2.setEnvironment("local");
            save(sample2);
            
            LogEntry sample3 = new LogEntry();
            sample3.setId("3");
            sample3.setTimestamp(LocalDateTime.now().minusMinutes(1));
            sample3.setLevel("WARN");
            sample3.setMessage("High memory usage detected");
            sample3.setSource("com.loganalyzer.monitoring.MemoryMonitor");
            sample3.setHost("localhost");
            sample3.setApplication("log-analyzer");
            sample3.setEnvironment("local");
            save(sample3);
        }
    }
}
