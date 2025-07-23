package com.loganalyzer.repository;

import com.loganalyzer.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Alert entities.
 * Provides database operations for alert management.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    
    // Status-based queries
    List<Alert> findByStatus(Alert.AlertStatus status);
    
    Page<Alert> findByStatus(Alert.AlertStatus status, Pageable pageable);
    
    List<Alert> findByStatusOrderByCreatedAtDesc(Alert.AlertStatus status);
    
    // Severity-based queries
    List<Alert> findBySeverity(Alert.AlertSeverity severity);
    
    Page<Alert> findBySeverity(Alert.AlertSeverity severity, Pageable pageable);
    
    List<Alert> findBySeverityAndStatus(Alert.AlertSeverity severity, Alert.AlertStatus status);
    
    // Rule-based queries
    List<Alert> findByRuleId(Long ruleId);
    
    List<Alert> findByRuleIdAndStatus(Long ruleId, Alert.AlertStatus status);
    
    @Query("SELECT a FROM Alert a WHERE a.ruleId = :ruleId AND a.triggeredBy = :source AND a.status = 'OPEN'")
    Optional<Alert> findOpenAlertByRuleAndSource(@Param("ruleId") Long ruleId, @Param("source") String source);
    
    // Time-based queries
    List<Alert> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    List<Alert> findByCreatedAtAfter(LocalDateTime timestamp);
    
    List<Alert> findByLastOccurrenceAfter(LocalDateTime timestamp);
    
    // Combined queries
    List<Alert> findByStatusAndCreatedAtBetween(Alert.AlertStatus status, LocalDateTime start, LocalDateTime end);
    
    List<Alert> findBySeverityAndCreatedAtBetween(Alert.AlertSeverity severity, LocalDateTime start, LocalDateTime end);
    
    // Acknowledgment queries
    List<Alert> findByAcknowledgedByIsNotNull();
    
    List<Alert> findByAcknowledgedByAndAcknowledgedAtBetween(String acknowledgedBy, LocalDateTime start, LocalDateTime end);
    
    // Resolution queries
    List<Alert> findByResolvedByIsNotNull();
    
    List<Alert> findByResolvedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Trigger-based queries
    List<Alert> findByTriggeredBy(String triggeredBy);
    
    List<Alert> findByTriggeredByAndStatus(String triggeredBy, Alert.AlertStatus status);
    
    @Query("SELECT a FROM Alert a WHERE a.triggerCount > :count")
    List<Alert> findByTriggerCountGreaterThan(@Param("count") Integer count);
    
    // Count queries
    long countByStatus(Alert.AlertStatus status);
    
    long countBySeverity(Alert.AlertSeverity severity);
    
    long countByRuleId(Long ruleId);
    
    long countByTriggeredBy(String triggeredBy);
    
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = 'OPEN' AND a.severity = :severity")
    long countOpenAlertsBySeverity(@Param("severity") Alert.AlertSeverity severity);
    
    // Statistics queries
    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.status = :status GROUP BY a.severity")
    List<Object[]> countAlertsBySeverityAndStatus(@Param("status") Alert.AlertStatus status);
    
    @Query("SELECT a.ruleId, COUNT(a) FROM Alert a WHERE a.createdAt >= :since GROUP BY a.ruleId ORDER BY COUNT(a) DESC")
    List<Object[]> findTopAlertRulesSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT a.triggeredBy, COUNT(a) FROM Alert a WHERE a.createdAt >= :since GROUP BY a.triggeredBy ORDER BY COUNT(a) DESC")
    List<Object[]> findTopAlertSourcesSince(@Param("since") LocalDateTime since);
    
    // Recent alerts
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since);
    
    @Query("SELECT a FROM Alert a WHERE a.status = 'OPEN' ORDER BY a.severity DESC, a.createdAt DESC")
    List<Alert> findOpenAlertsOrderBySeverity();
    
    // Notification queries
    List<Alert> findByNotificationSentFalse();
    
    List<Alert> findByNotificationSentFalseAndSeverity(Alert.AlertSeverity severity);
    
    @Query("SELECT a FROM Alert a WHERE a.notificationSent = false AND a.notificationAttempts < 3")
    List<Alert> findPendingNotifications();
    
    // Metadata queries
    @Query("SELECT a FROM Alert a JOIN a.metadata m WHERE KEY(m) = :key AND VALUE(m) = :value")
    List<Alert> findByMetadata(@Param("key") String key, @Param("value") String value);
    
    // Tag queries
    @Query("SELECT a FROM Alert a JOIN a.tags t WHERE t = :tag")
    List<Alert> findByTag(@Param("tag") String tag);
    
    // Complex queries
    @Query("SELECT a FROM Alert a WHERE a.status IN :statuses AND a.severity IN :severities AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<Alert> findByStatusesAndSeveritiesAndDateRange(
        @Param("statuses") List<Alert.AlertStatus> statuses,
        @Param("severities") List<Alert.AlertSeverity> severities,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );
    
    // Dashboard queries
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = 'OPEN' AND a.createdAt >= :since")
    long countOpenAlertsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT AVG(CAST(a.triggerCount AS double)) FROM Alert a WHERE a.createdAt >= :since")
    Double averageTriggerCountSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT a.severity, COUNT(a) FROM Alert a WHERE a.createdAt >= :since GROUP BY a.severity")
    List<Object[]> getAlertDistributionBySeveritySince(@Param("since") LocalDateTime since);
    
    // Cleanup queries
    @Query("SELECT a FROM Alert a WHERE a.status IN ('RESOLVED', 'CLOSED') AND a.updatedAt < :cutoff")
    List<Alert> findOldResolvedAlerts(@Param("cutoff") LocalDateTime cutoff);
    
    void deleteByStatusAndUpdatedAtBefore(Alert.AlertStatus status, LocalDateTime cutoff);
    
    // Performance queries
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :start AND a.createdAt <= :end ORDER BY a.createdAt")
    List<Alert> findAlertsInTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    // Escalation queries
    @Query("SELECT a FROM Alert a WHERE a.status = 'OPEN' AND a.severity = 'CRITICAL' AND a.acknowledgedAt IS NULL AND a.createdAt < :threshold")
    List<Alert> findUnacknowledgedCriticalAlerts(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT a FROM Alert a WHERE a.status = 'ACKNOWLEDGED' AND a.acknowledgedAt < :threshold AND a.resolvedAt IS NULL")
    List<Alert> findStaleAcknowledgedAlerts(@Param("threshold") LocalDateTime threshold);
}
