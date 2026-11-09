package org.example.banking_system_sdt.event;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BankingEventPublisherTest {

    @Test
    void publish_deliversEventToAllSubscribedObservers() {
        BankingEventPublisher publisher = new BankingEventPublisher();
        AtomicInteger count = new AtomicInteger(0);

        BankingEventObserver obs1 = event -> count.incrementAndGet();
        BankingEventObserver obs2 = event -> count.incrementAndGet();

        publisher.subscribe(obs1);
        publisher.subscribe(obs2);

        BankingEvent event = new BankingEvent(
                UUID.randomUUID(), EventType.ACCOUNT_OPENED, LocalDateTime.now(),
                List.of(1L), 10L, null, "Account opened"
        );
        publisher.publish(event);

        assertEquals(2, count.get());

        publisher.unsubscribe(obs1);
        publisher.publish(event);

        assertEquals(3, count.get()); // only obs2 ran
    }

    @Test
    void publish_oneObserverThrows_theOthersStillRun() {
        BankingEventPublisher publisher = new BankingEventPublisher();

        AtomicBoolean firstRan = new AtomicBoolean(false);
        AtomicBoolean thirdRan = new AtomicBoolean(false);

        BankingEventObserver firstObserver = event -> firstRan.set(true);
        BankingEventObserver failingObserver = event -> {
            throw new RuntimeException("Simulated observer crash");
        };
        BankingEventObserver thirdObserver = event -> thirdRan.set(true);

        publisher.subscribe(firstObserver);
        publisher.subscribe(failingObserver);
        publisher.subscribe(thirdObserver);

        BankingEvent event = new BankingEvent(
                UUID.randomUUID(), EventType.FEE_POLICY_CHANGED, LocalDateTime.now(),
                List.of(1L), 10L, null, "Fee policy updated"
        );

        assertDoesNotThrow(() -> publisher.publish(event));

        assertTrue(firstRan.get(), "First observer should have run");
        assertTrue(thirdRan.get(), "Third observer should have run despite second observer throwing");
    }
}
