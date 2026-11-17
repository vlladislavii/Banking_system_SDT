package org.example.banking_system_sdt.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class BankingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BankingEventPublisher.class);

    private final List<BankingEventObserver> observers = new CopyOnWriteArrayList<>();

    public BankingEventPublisher() {
    }

    @Autowired
    public BankingEventPublisher(List<BankingEventObserver> initialObservers) {
        if (initialObservers != null) {
            for (BankingEventObserver obs : initialObservers) {
                subscribe(obs);
            }
        }
    }

    public void subscribe(BankingEventObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unsubscribe(BankingEventObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    public void publish(BankingEvent event) {
        if (event == null) {
            return;
        }
        for (BankingEventObserver observer : observers) {
            try {
                observer.onEvent(event);
            } catch (Exception e) {
                log.error("Error executing observer {}: {}", observer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    public List<BankingEventObserver> getObservers() {
        return List.copyOf(observers);
    }
}
