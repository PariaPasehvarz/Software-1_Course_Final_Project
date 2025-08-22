package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class RightTradingServiceTest {

    @Autowired private RightTradingService rightTradingService;
    @Autowired private ActionRepository actionRepository;
    @Autowired private SecurityRepository securityRepository;

    private Security right;
    private Portfolio p;

    @BeforeEach
    void setup() {
        actionRepository.clear();
        securityRepository.clear();
        right = Security.builder().name("Foo Right").symbol("FOO_X").isin("ISIN-FOO-X").build();
        securityRepository.addSecurity(right);
        p = new Portfolio("P1", User.builder().firstName("A").lastName("B").build(), "Port");
    }

    @Test
    void can_buy_and_sell_rights_within_owned() {
        rightTradingService.buyRights(p, right, 5, 10.0, LocalDateTime.now().minusHours(1));
        rightTradingService.sellRights(p, right, 3, 12.0, LocalDateTime.now());

        // Expect two actions saved
        assertThat(actionRepository.findAllActionsOf(p.getUuid())).hasSize(2);
    }

    @Test
    void cannot_sell_more_than_owned() {
        rightTradingService.buyRights(p, right, 2, 10.0, LocalDateTime.now().minusHours(1));
        assertThrows(IllegalArgumentException.class, () ->
            rightTradingService.sellRights(p, right, 3, 12.0, LocalDateTime.now())
        );
    }
}