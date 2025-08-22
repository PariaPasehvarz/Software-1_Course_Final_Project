package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.Sale;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StockRightUsageService {

    private final ActionRepository actionRepository;
    private final SecurityRepository securityRepository;

    private static final int COST_PER_RIGHT = 100; // toman

    public void useRights(Portfolio portfolio, String rightSymbol, long quantity, LocalDateTime now) {
        Security right = securityRepository.findBySymbol(rightSymbol)
                .orElseThrow(() -> new IllegalArgumentException("Right not found: " + rightSymbol));

        String stockSymbol = rightSymbol.replace("_X", "");
        Security stock = securityRepository.findBySymbol(stockSymbol)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockSymbol));

        // simply record two actions: sell rights, buy stock
        Sale burnRights = Sale.builder()
                .portfolio(portfolio)
                .security(right)
                .volume(BigInteger.valueOf(quantity))
                .datetime(now)
                .build();

        Buy mintStock = Buy.builder()
                .portfolio(portfolio)
                .security(stock)
                .volume(BigInteger.valueOf(quantity))
                .datetime(now)
                .build();

        actionRepository.save(burnRights);
        actionRepository.save(mintStock);
    }
}
