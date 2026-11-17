package org.example.banking_system_sdt.event;

public interface BankingEventObserver {
    void onEvent(BankingEvent event);
}
