package com.bourse.wealthwise.messaging;

import com.bourse.wealthwise.domain.services.CapitalRaiseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("jms")
@RequiredArgsConstructor
@Slf4j
public class CapitalRaiseListener {

    private final CapitalRaiseService capitalRaiseService;

    @JmsListener(destination = "capital-raise")
    public void onMessage(String message) {
        try {
            CapitalRaiseParser.CapitalRaiseMsg m = CapitalRaiseParser.parse(message);
            capitalRaiseService.processAnnouncement(m.getSymbol(), m.getPerShare(), LocalDateTime.now());
            log.info("Processed capital raise: {}", message);
        } catch (Exception e) {
            log.warn("Invalid capital raise message: {}", message, e);
        }
    }
}
