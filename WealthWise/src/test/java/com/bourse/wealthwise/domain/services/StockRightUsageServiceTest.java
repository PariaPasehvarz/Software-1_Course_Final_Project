package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.action.StockRightUsage;
import com.bourse.wealthwise.domain.entity.action.CapitalRaise;
import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class StockRightUsageServiceTest {

    @Autowired private StockRightUsageService stockRightUsageService;
    @Autowired private SecurityRepository securityRepository;
    @Autowired private ActionRepository actionRepository;

    private Security stock;
    private Security right;
    private Portfolio p;

    @BeforeEach
    void setup() {
        securityRepository.clear();
        actionRepository.clear();
        stock = Security.builder().name("Foo Inc").symbol("FOO").isin("ISIN-FOO").build();
        right = Security.builder().name("Foo Right").symbol("FOO_X").isin("ISIN-FOO-X").build();
        securityRepository.addSecurity(stock);
        securityRepository.addSecurity(right);
        p = new Portfolio("P1", User.builder().firstName("A").lastName("B").build(), "Port");

        actionRepository.save(CapitalRaise.builder()
                .portfolio(p)
                .security(right)
                .volume(BigInteger.valueOf(5))
                .datetime(LocalDateTime.now().minusHours(1))
                .build());
    }

    @Test
    void saves_single_stock_right_usage_action_and_debits_cash() {
        stockRightUsageService.useRights(p, "FOO", 3, LocalDateTime.now());

        var actions = actionRepository.findAllActionsOf(p.getUuid());
        assertThat(actions.stream().filter(a -> a instanceof StockRightUsage)).hasSize(1);
        assertThat(actions).hasSize(2);
    }

    @Test
    void cannot_use_more_rights_than_owned() {
        assertThrows(IllegalArgumentException.class, () ->
            stockRightUsageService.useRights(p, "FOO", 999, LocalDateTime.now())
        );
    }
}