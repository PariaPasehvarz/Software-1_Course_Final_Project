package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.PortfolioRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CapitalRaiseService {

    private final ActionRepository actionRepository;
    private final PortfolioRepository portfolioRepository;
    private final SecurityRepository securityRepository;

    public void processAnnouncement(String symbol, double perShare, LocalDateTime now) {
        Security stock = securityRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Security not found: " + symbol));

        Security right = securityRepository.findBySymbol(symbol + "_X")
                .orElseThrow(() -> new IllegalArgumentException("Right security not defined: " + symbol + "_X"));

        for (Portfolio portfolio : portfolioRepository.findAll()) {
            // count how many of the stock they hold
            long held = actionRepository.findAllActionsOf(portfolio.getUuid()).stream()
                    .filter(a -> a.getSecurity().equals(stock))
                    .mapToLong(a -> a.getVolume().longValue())
                    .sum();

            long rightsToGrant = (long) Math.floor(held * perShare);
            if (rightsToGrant > 0) {
                CapitalRaise cr = CapitalRaise.builder()
                        .portfolio(portfolio)
                        .security(right)
                        .volume(BigInteger.valueOf(rightsToGrant))
                        .datetime(now)
                        .build();

                actionRepository.save(cr);
            }
        }
    }
}
