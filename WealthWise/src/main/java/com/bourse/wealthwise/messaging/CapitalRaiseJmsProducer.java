package com.bourse.wealthwise.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.messaging.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CapitalRaiseJmsProducer {
    private final JmsTemplate jmsTemplate;

    public void publish(String payload) {
        jmsTemplate.convertAndSend(CapitalRaiseJmsAdapter.QUEUE, payload);
    }
}
