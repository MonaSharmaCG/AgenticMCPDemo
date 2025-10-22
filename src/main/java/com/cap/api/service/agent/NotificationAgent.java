package com.cap.api.service.agent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
public class NotificationAgent {
    private static final Logger log = LoggerFactory.getLogger(NotificationAgent.class);

    @Value("${developer.dl.emails:}")
    private String developerDlEmails; // comma-separated list

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void notifyDLs(String message) {
        // In a real app, send email or message to DLs. Here, just log.
        log.info("Notifying DLs [{}]: {}", developerDlEmails, message);
        // Send email to each DL
        if (mailSender != null && developerDlEmails != null && !developerDlEmails.isBlank()) {
            for (String email : developerDlEmails.split(",")) {
                sendEmail(email.trim(), "Agent Notification", message);
            }
        } else {
            log.warn("MailSender not configured or DL emails missing. Message: {}", message);
        }
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(text);
            mailSender.send(mailMessage);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public List<String> getDlEmailList() {
        return List.of(developerDlEmails.split(","));
    }
}
