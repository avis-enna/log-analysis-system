package com.loganalyzer.service;

import com.loganalyzer.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending alert notifications via various channels.
 * Supports email, webhook, and other notification methods.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate;
    
    @Value("${alerting.notification.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${alerting.notification.email.smtp.from:noreply@loganalyzer.com}")
    private String fromEmail;
    
    @Value("${alerting.notification.webhook.enabled:true}")
    private boolean webhookEnabled;
    
    @Value("${alerting.notification.webhook.timeout:5000}")
    private int webhookTimeout;
    
    @Value("${alerting.notification.webhook.retry-attempts:3}")
    private int retryAttempts;
    
    public NotificationService(@Autowired(required = false) JavaMailSender mailSender, RestTemplate restTemplate) {
        this.mailSender = mailSender;
        this.restTemplate = restTemplate;
    }
    
    /**
     * Sends alert notification via all configured channels.
     */
    @Async
    public CompletableFuture<Void> sendAlertNotification(Alert alert) {
        try {
            // Send email notification
            if (emailEnabled) {
                sendEmailNotification(alert);
            }
            
            // Send webhook notification
            if (webhookEnabled) {
                sendWebhookNotification(alert);
            }
            
            // Send Slack notification (if configured)
            sendSlackNotification(alert);
            
            // Send Teams notification (if configured)
            sendTeamsNotification(alert);
            
            logger.info("Alert notifications sent for alert: {}", alert.getId());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            logger.error("Failed to send alert notifications for alert: {}", alert.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Sends email notification for alert.
     */
    private void sendEmailNotification(Alert alert) {
        if (mailSender == null) {
            logger.debug("Email notification skipped - JavaMailSender not available");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(getEmailRecipients(alert));
            message.setSubject(formatEmailSubject(alert));
            message.setText(formatEmailBody(alert));

            mailSender.send(message);
            logger.debug("Email notification sent for alert: {}", alert.getId());

        } catch (Exception e) {
            logger.error("Failed to send email notification for alert: {}", alert.getId(), e);
        }
    }
    
    /**
     * Sends webhook notification for alert.
     */
    private void sendWebhookNotification(Alert alert) {
        String webhookUrl = getWebhookUrl(alert);
        if (webhookUrl == null) {
            return;
        }
        
        try {
            Map<String, Object> payload = createWebhookPayload(alert);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Webhook notification sent for alert: {}", alert.getId());
            } else {
                logger.warn("Webhook notification failed with status: {} for alert: {}", 
                          response.getStatusCode(), alert.getId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to send webhook notification for alert: {}", alert.getId(), e);
        }
    }
    
    /**
     * Sends Slack notification for alert.
     */
    private void sendSlackNotification(Alert alert) {
        String slackWebhookUrl = getSlackWebhookUrl(alert);
        if (slackWebhookUrl == null) {
            return;
        }
        
        try {
            Map<String, Object> payload = createSlackPayload(alert);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(slackWebhookUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Slack notification sent for alert: {}", alert.getId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to send Slack notification for alert: {}", alert.getId(), e);
        }
    }
    
    /**
     * Sends Microsoft Teams notification for alert.
     */
    private void sendTeamsNotification(Alert alert) {
        String teamsWebhookUrl = getTeamsWebhookUrl(alert);
        if (teamsWebhookUrl == null) {
            return;
        }
        
        try {
            Map<String, Object> payload = createTeamsPayload(alert);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(teamsWebhookUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.debug("Teams notification sent for alert: {}", alert.getId());
            }
            
        } catch (Exception e) {
            logger.error("Failed to send Teams notification for alert: {}", alert.getId(), e);
        }
    }
    
    /**
     * Gets email recipients for alert based on severity and metadata.
     */
    private String[] getEmailRecipients(Alert alert) {
        // This would typically be configured per alert rule or severity
        switch (alert.getSeverity()) {
            case CRITICAL:
                return new String[]{"oncall@company.com", "devops@company.com"};
            case HIGH:
                return new String[]{"devops@company.com"};
            case MEDIUM:
                return new String[]{"monitoring@company.com"};
            case LOW:
            default:
                return new String[]{"logs@company.com"};
        }
    }
    
    /**
     * Formats email subject for alert.
     */
    private String formatEmailSubject(Alert alert) {
        return String.format("[%s] %s - %s", 
                           alert.getSeverity().name(), 
                           alert.getTitle(),
                           alert.getTriggeredBy());
    }
    
    /**
     * Formats email body for alert.
     */
    private String formatEmailBody(Alert alert) {
        StringBuilder body = new StringBuilder();
        body.append("Alert Details:\n");
        body.append("=============\n\n");
        body.append("Title: ").append(alert.getTitle()).append("\n");
        body.append("Severity: ").append(alert.getSeverity()).append("\n");
        body.append("Status: ").append(alert.getStatus()).append("\n");
        body.append("Rule: ").append(alert.getRuleName()).append("\n");
        body.append("Triggered By: ").append(alert.getTriggeredBy()).append("\n");
        body.append("First Occurrence: ").append(alert.getFirstOccurrence()).append("\n");
        body.append("Trigger Count: ").append(alert.getTriggerCount()).append("\n\n");
        
        body.append("Description:\n");
        body.append(alert.getDescription()).append("\n\n");
        
        if (alert.getMetadata() != null && !alert.getMetadata().isEmpty()) {
            body.append("Metadata:\n");
            alert.getMetadata().forEach((key, value) -> 
                body.append("- ").append(key).append(": ").append(value).append("\n"));
            body.append("\n");
        }
        
        body.append("View in Dashboard: http://your-dashboard-url/alerts/").append(alert.getId()).append("\n");
        
        return body.toString();
    }
    
    /**
     * Gets webhook URL for alert.
     */
    private String getWebhookUrl(Alert alert) {
        // This would be configured per alert rule or globally
        return System.getenv("ALERT_WEBHOOK_URL");
    }
    
    /**
     * Creates webhook payload for alert.
     */
    private Map<String, Object> createWebhookPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("alertId", alert.getId());
        payload.put("title", alert.getTitle());
        payload.put("description", alert.getDescription());
        payload.put("severity", alert.getSeverity().name());
        payload.put("status", alert.getStatus().name());
        payload.put("ruleName", alert.getRuleName());
        payload.put("triggeredBy", alert.getTriggeredBy());
        payload.put("triggerCount", alert.getTriggerCount());
        payload.put("firstOccurrence", alert.getFirstOccurrence().toString());
        payload.put("lastOccurrence", alert.getLastOccurrence().toString());
        payload.put("metadata", alert.getMetadata());
        payload.put("tags", alert.getTags());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        return payload;
    }
    
    /**
     * Gets Slack webhook URL for alert.
     */
    private String getSlackWebhookUrl(Alert alert) {
        return System.getenv("SLACK_WEBHOOK_URL");
    }
    
    /**
     * Creates Slack payload for alert.
     */
    private Map<String, Object> createSlackPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        
        String color = getSeverityColor(alert.getSeverity());
        String text = String.format("*%s Alert: %s*\n%s", 
                                   alert.getSeverity().name(), 
                                   alert.getTitle(),
                                   alert.getDescription());
        
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color);
        attachment.put("text", text);
        attachment.put("ts", System.currentTimeMillis() / 1000);
        
        // Add fields
        Map<String, Object>[] fields = new Map[4];
        fields[0] = Map.of("title", "Severity", "value", alert.getSeverity().name(), "short", true);
        fields[1] = Map.of("title", "Source", "value", alert.getTriggeredBy(), "short", true);
        fields[2] = Map.of("title", "Rule", "value", alert.getRuleName(), "short", true);
        fields[3] = Map.of("title", "Count", "value", alert.getTriggerCount().toString(), "short", true);
        attachment.put("fields", fields);
        
        payload.put("attachments", new Object[]{attachment});
        
        return payload;
    }
    
    /**
     * Gets Teams webhook URL for alert.
     */
    private String getTeamsWebhookUrl(Alert alert) {
        return System.getenv("TEAMS_WEBHOOK_URL");
    }
    
    /**
     * Creates Teams payload for alert.
     */
    private Map<String, Object> createTeamsPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("@type", "MessageCard");
        payload.put("@context", "http://schema.org/extensions");
        payload.put("themeColor", getSeverityColor(alert.getSeverity()));
        payload.put("summary", alert.getTitle());
        payload.put("title", String.format("%s Alert", alert.getSeverity().name()));
        payload.put("text", alert.getDescription());
        
        // Add sections
        Map<String, Object> section = new HashMap<>();
        section.put("activityTitle", alert.getTitle());
        section.put("activitySubtitle", String.format("Triggered by: %s", alert.getTriggeredBy()));
        
        Map<String, Object>[] facts = new Map[3];
        facts[0] = Map.of("name", "Severity", "value", alert.getSeverity().name());
        facts[1] = Map.of("name", "Rule", "value", alert.getRuleName());
        facts[2] = Map.of("name", "Trigger Count", "value", alert.getTriggerCount().toString());
        section.put("facts", facts);
        
        payload.put("sections", new Object[]{section});
        
        return payload;
    }
    
    /**
     * Gets color code for alert severity.
     */
    private String getSeverityColor(Alert.AlertSeverity severity) {
        switch (severity) {
            case CRITICAL:
                return "#FF0000"; // Red
            case HIGH:
                return "#FF8C00"; // Orange
            case MEDIUM:
                return "#FFD700"; // Yellow
            case LOW:
            default:
                return "#00CED1"; // Blue
        }
    }
    
    /**
     * Sends test notification to verify configuration.
     */
    public void sendTestNotification(String channel, String recipient) {
        Alert testAlert = new Alert();
        testAlert.setId(999L);
        testAlert.setTitle("Test Alert");
        testAlert.setDescription("This is a test alert to verify notification configuration.");
        testAlert.setSeverity(Alert.AlertSeverity.LOW);
        testAlert.setStatus(Alert.AlertStatus.OPEN);
        testAlert.setRuleName("test-rule");
        testAlert.setTriggeredBy("test-system");
        testAlert.setTriggerCount(1);
        testAlert.setFirstOccurrence(LocalDateTime.now());
        testAlert.setLastOccurrence(LocalDateTime.now());
        
        switch (channel.toLowerCase()) {
            case "email":
                if (emailEnabled) {
                    sendEmailNotification(testAlert);
                }
                break;
            case "webhook":
                if (webhookEnabled) {
                    sendWebhookNotification(testAlert);
                }
                break;
            case "slack":
                sendSlackNotification(testAlert);
                break;
            case "teams":
                sendTeamsNotification(testAlert);
                break;
            default:
                logger.warn("Unknown notification channel: {}", channel);
        }
    }
}
