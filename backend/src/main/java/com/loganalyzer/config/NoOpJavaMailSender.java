package com.loganalyzer.config;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.internet.MimeMessage;

/**
 * No-op implementation of JavaMailSender for local development.
 * All methods either log the operation or do nothing.
 */
public class NoOpJavaMailSender implements JavaMailSender {
    
    private static final Logger logger = LoggerFactory.getLogger(NoOpJavaMailSender.class);

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        logger.info("Mock email would be sent to: {} - Subject: {}", 
                   simpleMessage.getTo() != null ? String.join(", ", simpleMessage.getTo()) : "unknown",
                   simpleMessage.getSubject());
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        for (SimpleMailMessage message : simpleMessages) {
            send(message);
        }
    }

    @Override
    public MimeMessage createMimeMessage() {
        // Return null since we won't actually use MimeMessage in local mode
        return null;
    }

    @Override
    public MimeMessage createMimeMessage(InputStream contentStream) throws MailException {
        // Return null since we won't actually use MimeMessage in local mode
        return null;
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        logger.info("Mock MimeMessage email would be sent");
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        logger.info("Mock MimeMessage emails would be sent (count: {})", mimeMessages.length);
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        logger.info("Mock MimeMessagePreparator email would be sent");
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        logger.info("Mock MimeMessagePreparator emails would be sent (count: {})", mimeMessagePreparators.length);
    }
}
