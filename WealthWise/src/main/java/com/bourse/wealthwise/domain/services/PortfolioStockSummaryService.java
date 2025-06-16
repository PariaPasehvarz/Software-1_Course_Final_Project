package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.BaseAction;
import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.Sale;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.domain.entity.security.SecurityPrice;
import com.bourse.wealthwise.domain.entity.security.SecuritySummary;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.SecurityPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class PortfolioStockSummaryService {

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private SecurityPriceRepository securityPriceRepository;

    public List<SecuritySummary> getSecuritiesSummary(UUID portfolioId, LocalDate date) {
        List<BaseAction> actions = actionRepository.findAllActionsOfUntilDate(portfolioId.toString(), date.atTime(23, 59, 59));

        Map<Security, Integer> volumeMap = new HashMap<>();

        for (BaseAction action : actions) {
            if (action instanceof Buy buy) {
                volumeMap.merge(buy.getSecurity(), buy.getVolume().intValue(), Integer::sum);
            } else if (action instanceof Sale sale) {
                volumeMap.merge(sale.getSecurity(), sale.getVolume().negate().intValue(), Integer::sum);
            }
        }

        List<SecuritySummary> summaryList = new ArrayList<>();
        for (Map.Entry<Security, Integer> entry : volumeMap.entrySet()) {
            Security security = entry.getKey();
            int volume = entry.getValue();

            Optional<Double> priceOpt = securityPriceRepository.getPrice(security.getIsin(), date);
            BigDecimal price = priceOpt.map(BigDecimal::valueOf).orElse(BigDecimal.ZERO);

            summaryList.add(new SecuritySummary(security, volume, price.multiply(BigDecimal.valueOf(volume))));
        }

        summaryList.sort(Comparator.comparing(ss -> ss.getSecurity().getName()));
        return summaryList;
    }
}
