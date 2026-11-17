package org.example.banking_system_sdt.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditLogObserverTest {

    @Test
    void onEvent_recordsEventInAuditLog() {
        AuditLog auditLog = new AuditLog();
        AuditLogObserver observer = new AuditLogObserver(auditLog);

        BankingEvent event = new BankingEvent(
                UUID.randomUUID(), EventType.ACCOUNT_STATUS_CHANGED, LocalDateTime.now(),
                List.of(10L), 100L, null, "Account frozen"
        );

        observer.onEvent(event);

        List<BankingEvent> entries = auditLog.getEntries();
        assertEquals(1, entries.size());
        assertEquals(event, entries.get(0));
    }
}
