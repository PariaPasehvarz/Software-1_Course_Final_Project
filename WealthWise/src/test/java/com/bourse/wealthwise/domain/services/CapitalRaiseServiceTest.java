package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.PortfolioRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CapitalRaiseServiceTest {

    @Autowired private CapitalRaiseService capitalRaiseService;
    @Autowired private SecurityRepository securityRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private ActionRepository actionRepository;

    @BeforeEach
    void setup() {
        securityRepository.clear();
        actionRepository.clear();
    }

    @Test
    void grants_floor_of_held_times_ratio() {
        // Given a stock with 10 shares in a portfolio
        Security stock = Security.builder().name("Foo Inc").symbol("FOO").isin("ISIN-FOO").build();
        Security right = Security.builder().name("Foo Right").symbol("FOO_X").isin("ISIN-FOO-X").build();
        securityRepository.addSecurity(stock);
        securityRepository.addSecurity(right);

        Portfolio p = new Portfolio("P1", User.builder().firstName("A").lastName("B").build(), "Test");
        portfolioRepository.save(p);

        Buy buy = Buy.builder()
                .portfolio(p)
                .security(stock)
                .volume(BigInteger.valueOf(10))
                .datetime(LocalDateTime.now().minusDays(1))
                .build();
        actionRepository.save(buy);

        // When
        capitalRaiseService.processAnnouncement("FOO", 0.3, LocalDateTime.now());

        // Then: expect floor(10 * 0.3) = 3 rights granted via a CapitalRaise action
        List<CapitalRaise> crs = actionRepository.findAllActionsOf(p.getUuid()).stream()
                .filter(a -> a instanceof CapitalRaise)
                .map(a -> (CapitalRaise)a)
                .collect(Collectors.toList());

        assertThat(crs).hasSize(1);
        CapitalRaise cr = crs.get(0);
        assertThat(cr.getSecurity().getSymbol()).isEqualTo("FOO_X");
        assertThat(cr.getVolume()).isEqualTo(BigInteger.valueOf(3));
    }
}