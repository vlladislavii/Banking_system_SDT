package org.example.banking_system_sdt.notification;

import org.example.banking_system_sdt.event.EventType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long recipientId,
        EventType type,
        String message,
        LocalDateTime createdAt,
        boolean read
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getRecipientId(),
                n.getType(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isRead()
        );
    }
}
