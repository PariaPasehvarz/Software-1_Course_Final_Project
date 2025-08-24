package com.bourse.wealthwise.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Thin JMS adapter that listens on a queue and delegates to the domain consumer.
 */
@Component
@ConditionalOnProperty(value = "app.messaging.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CapitalRaiseJmsAdapter {

    public static final String QUEUE = "capital.raise.announcements";

    private final CapitalRaiseAnnouncementsConsumer consumer;

    @JmsListener(destination = QUEUE)
    public void onMessage(String payload) {
        consumer.consume(payload); // reuse your existing parsing + service workflow
    }
}
