package org.example.banking_system_sdt.notification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> listForCustomer(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @RequestParam Long customerId) {
        return notificationService.listForCustomer(callerId, customerId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @PutMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @RequestHeader(value = "X-User-Id", required = false) Long callerId,
            @PathVariable Long id) {
        notificationService.markRead(callerId, id);
    }
}
