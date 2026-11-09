package org.example.banking_system_sdt.event;

import org.example.banking_system_sdt.customer.CustomerService;
import org.example.banking_system_sdt.exception.ForbiddenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditLog auditLog;
    private final CustomerService customerService;

    public AuditLogController(AuditLog auditLog, CustomerService customerService) {
        this.auditLog = auditLog;
        this.customerService = customerService;
    }

    @GetMapping
    public List<BankingEvent> getEntries(@RequestHeader(value = "X-User-Id", required = false) Long callerId) {
        if (!customerService.isAdmin(callerId)) {
            throw new ForbiddenException("Only administrators can view the audit log");
        }
        return auditLog.getEntries();
    }
}
