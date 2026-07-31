package com.moneyflow.notification;

/**
 * Strategy for delivering a notification to a user. Implementations are picked up
 * as Spring beans; add e.g. an email/push sender by contributing another bean.
 */
public interface NotificationSender {

    void send(String recipient, String subject, String message);
}
