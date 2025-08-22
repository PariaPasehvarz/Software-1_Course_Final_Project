package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
import com.bourse.wealthwise.domain.entity.action.Sale;
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
        Security stock = securityRepository.findSecurityBySymbol(symbol);
        Security right = securityRepository.findSecurityBySymbol(symbol + "_X");
        if (stock == null || right == null) {
            throw new IllegalArgumentException("Stock or right security not found for symbol " + symbol);
        }

        for (Portfolio portfolio : portfolioRepository.getPortfolios()) {
            long currentShares = 0L;
            for (var a : actionRepository.findAllActionsOf(portfolio.getUuid())) {
                if (a instanceof Buy b && b.getSecurity().equals(stock)) {
                    currentShares += b.getVolume().longValue();
                } else if (a instanceof Sale s && s.getSecurity().equals(stock)) {
                    currentShares -= s.getVolume().longValue();
                }
            }

            long grant = (long) Math.floor(currentShares * perShare);
            if (grant <= 0) continue;

            CapitalRaise cr = CapitalRaise.builder()
                    .portfolio(portfolio)
                    .security(right)
                    .volume(BigInteger.valueOf(grant))
                    .datetime(now)
                    .build();
            actionRepository.save(cr);
        }
    }
}
