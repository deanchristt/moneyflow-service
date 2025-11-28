package com.moneyflow.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default notification sender that writes to the application log. Always active so
 * the system has at least one delivery channel; real channels (email, push) can be
 * added as additional {@link NotificationSender} beans.
 */
@Slf4j
@Component
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(String recipient, String subject, String message) {
        log.warn("[NOTIFICATION] to={} | {} | {}", recipient, subject, message);
    }
}
