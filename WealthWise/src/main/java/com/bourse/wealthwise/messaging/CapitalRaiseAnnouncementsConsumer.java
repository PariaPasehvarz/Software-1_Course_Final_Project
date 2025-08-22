package com.bourse.wealthwise.messaging;

import com.bourse.wealthwise.domain.services.CapitalRaiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * A simple consumer that can be used directly in tests
 * or wired to any messaging mechanism.
 */
@Component
@RequiredArgsConstructor
public class CapitalRaiseAnnouncementsConsumer {

    private final CapitalRaiseService capitalRaiseService;

    public void consume(String message) {
        CapitalRaiseParser.CapitalRaiseMsg m = CapitalRaiseParser.parse(message);
        capitalRaiseService.processAnnouncement(
                m.getSymbol(),
                m.getPerShare(),
                LocalDateTime.now()
        );
    }
}
