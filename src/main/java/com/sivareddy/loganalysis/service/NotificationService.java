package com.sivareddy.loganalysis.service;

import com.sivareddy.loganalysis.model.Alert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for sending notifications
 * 
 * @author Siva Reddy
 */
@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${alert.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${alert.recipients:admin@company.com}")
    private String recipients;
    
    @Value("${spring.application.name:Log Analysis System}")
    private String applicationName;
    
    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Send alert notification via email
     */
    @Async
    public void sendAlertNotification(Alert alert) {
        if (!emailEnabled) {
            logger.debug("Email notifications are disabled");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(getRecipientArray());
            message.setSubject(String.format("[%s] %s Alert: %s", applicationName, alert.getSeverity(), alert.getType()));
            message.setText(buildAlertEmailContent(alert));
            
            mailSender.send(message);
            logger.info("Alert notification sent for alert: {}", alert.getId());
            
        } catch (Exception e) {
            logger.error("Failed to send alert notification for alert: {}", alert.getId(), e);
        }
    }
    
    /**
     * Send system status notification
     */
    @Async
    public void sendSystemStatusNotification(String subject, String message) {
        if (!emailEnabled) {
            logger.debug("Email notifications are disabled");
            return;
        }
        
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(getRecipientArray());
            mailMessage.setSubject(String.format("[%s] %s", applicationName, subject));
            mailMessage.setText(message);
            
            mailSender.send(mailMessage);
            logger.info("System status notification sent: {}", subject);
            
        } catch (Exception e) {
            logger.error("Failed to send system status notification: {}", subject, e);
        }
    }
    
    /**
     * Send batch of alerts notification
     */
    @Async
    public void sendBatchAlertNotification(List<Alert> alerts) {
        if (!emailEnabled || alerts.isEmpty()) {
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(getRecipientArray());
            message.setSubject(String.format("[%s] Batch Alert Summary - %d alerts", applicationName, alerts.size()));
            message.setText(buildBatchAlertEmailContent(alerts));
            
            mailSender.send(message);
            logger.info("Batch alert notification sent for {} alerts", alerts.size());
            
        } catch (Exception e) {
            logger.error("Failed to send batch alert notification", e);
        }
    }
    
    /**
     * Build email content for a single alert
     */
    private String buildAlertEmailContent(Alert alert) {
        StringBuilder content = new StringBuilder();
        content.append("Alert Details:\n");
        content.append("=================\n\n");
        content.append("ID: ").append(alert.getId()).append("\n");
        content.append("Type: ").append(alert.getType()).append("\n");
        content.append("Severity: ").append(alert.getSeverity()).append("\n");
        content.append("Message: ").append(alert.getMessage()).append("\n");
        
        if (alert.getDescription() != null) {
            content.append("Description: ").append(alert.getDescription()).append("\n");
        }
        
        if (alert.getSourceSystem() != null) {
            content.append("Source System: ").append(alert.getSourceSystem()).append("\n");
        }
        
        if (alert.getSourceIp() != null) {
            content.append("Source IP: ").append(alert.getSourceIp()).append("\n");
        }
        
        content.append("Created At: ").append(alert.getCreatedAt()).append("\n");
        
        if (alert.getAcknowledgmentRequired()) {
            content.append("\n⚠️  This alert requires acknowledgment.\n");
        }
        
        content.append("\nPlease investigate and take appropriate action.\n");
        
        return content.toString();
    }
    
    /**
     * Build email content for batch alerts
     */
    private String buildBatchAlertEmailContent(List<Alert> alerts) {
        StringBuilder content = new StringBuilder();
        content.append("Alert Summary:\n");
        content.append("================\n\n");
        content.append("Total Alerts: ").append(alerts.size()).append("\n\n");
        
        // Group by severity
        long critical = alerts.stream().filter(a -> "CRITICAL".equals(a.getSeverity())).count();
        long high = alerts.stream().filter(a -> "HIGH".equals(a.getSeverity())).count();
        long medium = alerts.stream().filter(a -> "MEDIUM".equals(a.getSeverity())).count();
        long low = alerts.stream().filter(a -> "LOW".equals(a.getSeverity())).count();
        
        content.append("By Severity:\n");
        content.append("  Critical: ").append(critical).append("\n");
        content.append("  High: ").append(high).append("\n");
        content.append("  Medium: ").append(medium).append("\n");
        content.append("  Low: ").append(low).append("\n\n");
        
        content.append("Recent Alerts:\n");
        content.append("==============\n");
        
        for (Alert alert : alerts.subList(0, Math.min(alerts.size(), 10))) {
            content.append(String.format("- [%s] %s: %s\n", 
                alert.getSeverity(), alert.getType(), alert.getMessage()));
        }
        
        if (alerts.size() > 10) {
            content.append(String.format("... and %d more alerts\n", alerts.size() - 10));
        }
        
        content.append("\nPlease check the dashboard for full details.\n");
        
        return content.toString();
    }
    
    /**
     * Get recipient array from comma-separated string
     */
    private String[] getRecipientArray() {
        return Arrays.stream(recipients.split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
    }
}
