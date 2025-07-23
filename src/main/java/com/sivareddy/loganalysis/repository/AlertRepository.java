package com.sivareddy.loganalysis.repository;

import com.sivareddy.loganalysis.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Alert entities
 * 
 * @author Siva Reddy
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    /**
     * Find alerts by type
     */
    Page<Alert> findByType(String type, Pageable pageable);
    
    /**
     * Find alerts by severity
     */
    Page<Alert> findBySeverity(String severity, Pageable pageable);
    
    /**
     * Find unresolved alerts
     */
    Page<Alert> findByResolvedFalse(Pageable pageable);
    
    /**
     * Find resolved alerts
     */
    Page<Alert> findByResolvedTrue(Pageable pageable);
    
    /**
     * Find alerts that require acknowledgment
     */
    Page<Alert> findByAcknowledgmentRequiredTrueAndAcknowledgedFalse(Pageable pageable);
    
    /**
     * Find alerts within time range
     */
    Page<Alert> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * Find alerts by source system
     */
    Page<Alert> findBySourceSystem(String sourceSystem, Pageable pageable);
    
    /**
     * Find alerts by source IP
     */
    Page<Alert> findBySourceIp(String sourceIp, Pageable pageable);
    
    /**
     * Count unresolved alerts
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.resolved = false")
    Long countUnresolvedAlerts();
    
    /**
     * Count alerts by severity
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.severity = :severity AND a.createdAt >= :since")
    Long countBySeveritySince(@Param("severity") String severity, @Param("since") LocalDateTime since);
    
    /**
     * Count alerts by type
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.type = :type AND a.createdAt >= :since")
    Long countByTypeSince(@Param("type") String type, @Param("since") LocalDateTime since);
    
    /**
     * Find recent high-severity alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.severity IN ('HIGH', 'CRITICAL') AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentHighSeverityAlerts(@Param("since") LocalDateTime since);
    
    /**
     * Find alerts that haven't been notified
     */
    @Query("SELECT a FROM Alert a WHERE a.notificationSent = false")
    List<Alert> findAlertsToNotify();
    
    /**
     * Get alert statistics by type
     */
    @Query(value = "SELECT type, " +
                   "COUNT(*) as total_count, " +
                   "COUNT(CASE WHEN resolved = true THEN 1 END) as resolved_count, " +
                   "COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical_count, " +
                   "COUNT(CASE WHEN severity = 'HIGH' THEN 1 END) as high_count " +
                   "FROM alerts " +
                   "WHERE created_at >= :since " +
                   "GROUP BY type " +
                   "ORDER BY total_count DESC", 
           nativeQuery = true)
    List<Object[]> getAlertStatsByType(@Param("since") LocalDateTime since);
    
    /**
     * Get hourly alert counts for timeline chart
     */
    @Query(value = "SELECT DATE_TRUNC('hour', created_at) as hour, severity, COUNT(*) as count " +
                   "FROM alerts " +
                   "WHERE created_at >= :since " +
                   "GROUP BY hour, severity " +
                   "ORDER BY hour", 
           nativeQuery = true)
    List<Object[]> getHourlyAlertCounts(@Param("since") LocalDateTime since);
    
    /**
     * Get top source systems by alert count
     */
    @Query(value = "SELECT source_system, COUNT(*) as count " +
                   "FROM alerts " +
                   "WHERE created_at >= :since AND source_system IS NOT NULL " +
                   "GROUP BY source_system " +
                   "ORDER BY count DESC " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<Object[]> getTopSourceSystems(@Param("since") LocalDateTime since, @Param("limit") int limit);
}
