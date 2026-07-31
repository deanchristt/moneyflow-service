package com.moneyflow.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends notifications via email. Opt-in: only active when
 * {@code moneyflow.notifications.email.enabled=true} and SMTP is configured
 * (spring.mail.*). Registered alongside {@link LoggingNotificationSender}, so
 * enabling it adds an email channel without removing the log channel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "moneyflow.notifications.email", name = "enabled", havingValue = "true")
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${moneyflow.notifications.email.from:no-reply@moneyflow.local}")
    private String from;

    @Override
    public void send(String recipient, String subject, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(recipient);
            mail.setSubject(subject);
            mail.setText(message);
            mailSender.send(mail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
        }
    }
}
