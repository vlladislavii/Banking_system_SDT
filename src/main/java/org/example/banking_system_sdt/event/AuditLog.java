package org.example.banking_system_sdt.event;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AuditLog {

    private final List<BankingEvent> entries = new CopyOnWriteArrayList<>();

    public void record(BankingEvent event) {
        if (event != null) {
            entries.add(event);
        }
    }

    public List<BankingEvent> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
