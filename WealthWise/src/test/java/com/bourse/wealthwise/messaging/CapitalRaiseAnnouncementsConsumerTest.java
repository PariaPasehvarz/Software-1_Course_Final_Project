package com.bourse.wealthwise.messaging;

import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CapitalRaiseAnnouncementsConsumerTest {

    @Autowired private CapitalRaiseAnnouncementsConsumer consumer;
    @Autowired private ActionRepository actionRepository;
    @Autowired private SecurityRepository securityRepository;
    @Autowired private PortfolioRepository portfolioRepository;

    private Portfolio portfolio;
    private Security stock;
    private Security right;

    @BeforeEach
    void setup() {
        actionRepository.clear();
        securityRepository.clear();

        stock = Security.builder().name("Foo Inc").symbol("FOO").isin("ISIN-FOO").build();
        right = Security.builder().name("Foo Right").symbol("FOO_X").isin("ISIN-FOO-X").build();
        securityRepository.addSecurity(stock);
        securityRepository.addSecurity(right);

        portfolio = new Portfolio("P1", User.builder().firstName("A").lastName("B").build(), "Port");
        portfolioRepository.save(portfolio);

        // give portfolio 10 stock
        actionRepository.save(Buy.builder()
                .portfolio(portfolio)
                .security(stock)
                .volume(BigInteger.valueOf(10))
                .datetime(LocalDateTime.now().minusDays(1))
                .build());
    }

    @Test
    void consumes_and_triggers_capital_raise() {
        consumer.consume("CAPITAL_RAISE FOO 0.25");

        var actions = actionRepository.findAllActionsOf(portfolio.getUuid());
        assertThat(actions.stream().filter(a -> a instanceof CapitalRaise)).hasSize(1);

        CapitalRaise cr = (CapitalRaise) actions.stream()
                .filter(a -> a instanceof CapitalRaise)
                .findFirst().get();

        // floor(10 * 0.25) = 2 rights
        assertThat(cr.getVolume()).isEqualTo(BigInteger.valueOf(2));
    }
}
