package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
import com.bourse.wealthwise.domain.entity.action.Sale;
import com.bourse.wealthwise.domain.entity.action.StockRightUsage;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RightTradingService {

    private final ActionRepository actionRepository;

    public void buyRights(Portfolio portfolio, Security right, long quantity, double price, LocalDateTime when) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        Buy buy = Buy.builder()
                .portfolio(portfolio)
                .security(right)
                .volume(BigInteger.valueOf(quantity))
                .datetime(when)
                .build();
        actionRepository.save(buy);
    }

    public void sellRights(Portfolio portfolio, Security right, long quantity, double price, LocalDateTime when) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        long owned = rightsOwnedUpTo(portfolio, right, when);
        if (quantity > owned) throw new IllegalArgumentException("Not enough rights to sell");
        Sale sale = Sale.builder()
                .portfolio(portfolio)
                .security(right)
                .volume(BigInteger.valueOf(quantity))
                .datetime(when)
                .build();
        actionRepository.save(sale);
    }

    private long rightsOwnedUpTo(Portfolio portfolio, Security right, LocalDateTime until) {
        List<com.bourse.wealthwise.domain.entity.action.BaseAction> actions =
                actionRepository.findAllActionsOfUntilDate(portfolio.getUuid(), until);
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