package org.example.banking_system_sdt.event;

import org.springframework.stereotype.Component;

@Component
public class AuditLogObserver implements BankingEventObserver {

    private final AuditLog auditLog;

    public AuditLogObserver(AuditLog auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void onEvent(BankingEvent event) {
        if (event != null) {
            auditLog.record(event);
        }
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }
}
