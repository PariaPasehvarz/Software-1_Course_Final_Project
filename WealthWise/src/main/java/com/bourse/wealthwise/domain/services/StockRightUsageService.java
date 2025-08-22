package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.BaseAction;
import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
import com.bourse.wealthwise.domain.entity.action.Sale;
import com.bourse.wealthwise.domain.entity.action.StockRightUsage;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StockRightUsageService {

    private static final BigInteger PRICE_PER_RIGHT = BigInteger.valueOf(100);

    private final ActionRepository actionRepository;
    private final SecurityRepository securityRepository;

    public void useRights(Portfolio portfolio, String stockSymbol, long quantity, LocalDateTime now) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        Objects.requireNonNull(portfolio, "portfolio is required");
        Objects.requireNonNull(stockSymbol, "stockSymbol is required");
        Objects.requireNonNull(now, "now is required");

        Security stock = securityRepository.findSecurityBySymbol(stockSymbol);
        Security right = securityRepository.findSecurityBySymbol(stockSymbol + "_X");
        if (stock == null || right == null) {
            throw new IllegalArgumentException("Stock or right not found for " + stockSymbol);
        }

        long ownedRights = rightsOwnedUpTo(portfolio, right, now);
        if (quantity > ownedRights) throw new IllegalArgumentException("Not enough rights to use");

        StockRightUsage sru = StockRightUsage.builder()
                .portfolio(portfolio)
                .rightSecurity(right)
                .stockSecurity(stock)
                .rightsUsed(BigInteger.valueOf(quantity))
                .pricePerRight(PRICE_PER_RIGHT)
                .datetime(now)
                .build();
        actionRepository.save(sru);
    }

    private long rightsOwnedUpTo(Portfolio portfolio, Security right, LocalDateTime until) {
        List<BaseAction> actions = actionRepository.findAllActionsOfUntilDate(portfolio.getUuid(), until);
        long total = 0L;
        for (var a : actions) {
            if (a instanceof Buy b && b.getSecurity().equals(right)) {
                total += b.getVolume().longValue();
            } else if (a instanceof Sale s && s.getSecurity().equals(right)) {
                total -= s.getVolume().longValue();
            } else if (a instanceof CapitalRaise cr && cr.getSecurity().equals(right)) {
                total += cr.getVolume().longValue();
            } else if (a instanceof StockRightUsage su && su.getRightSecurity().equals(right)) {
                total -= su.getRightsUsed().longValue();
            }
        }
        return total;
    }
}
