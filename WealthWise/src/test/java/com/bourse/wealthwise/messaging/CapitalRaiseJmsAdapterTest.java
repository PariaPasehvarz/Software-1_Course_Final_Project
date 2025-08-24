package com.bourse.wealthwise.messaging;

import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.PortfolioRepository;
import com.bourse.wealthwise.repository.SecurityRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "app.messaging.enabled=true",
        "spring.artemis.mode=embedded",
        "spring.jms.listener.auto-startup=true",
        "spring.jms.pub-sub-domain=false"
})
class CapitalRaiseJmsAdapterTest {

    @Autowired private CapitalRaiseJmsProducer producer;
    @Autowired private SecurityRepository securityRepository;
    @Autowired private PortfolioRepository portfolioRepository;
    @Autowired private ActionRepository actionRepository;

    private Portfolio portfolio;
    private Security foo;

    @BeforeEach
    void setUp() {
        // reset in-memory stores
        securityRepository.clear();
        actionRepository.clear();

        // --- Seed required securities (no price field on your builder) ---
        foo = Security.builder()
                .name("Foo Inc.")
                .symbol("FOO")
                .isin("FOO-ISIN")
                .build();

        Security fooRight = Security.builder()
                .name("Foo Inc. Right")
                .symbol("FOO_X")   // rights symbol convention: <symbol>_X
                .isin("FOO_X-ISIN")
                .build();

        securityRepository.addSecurity(foo);
        securityRepository.addSecurity(fooRight);

        // --- Portfolio & initial position ---
        User manager = User.builder()
                .firstName("Test")
                .lastName("User")
                .uuid("u-1")
                .build();

        portfolio = new Portfolio(
                UUID.randomUUID().toString(),   // Portfolio expects String id
                manager,
                "Test Portfolio"
        );
        portfolioRepository.save(portfolio);

        // Own 10 shares of FOO so capital raise will grant rights
        actionRepository.save(Buy.builder()
                .uuid(UUID.randomUUID().toString())
                .portfolio(portfolio)
                .security(foo)
                .volume(BigInteger.valueOf(10))
                .price(10) // <-- Buy action in your model has an int price; this is fine
                .datetime(LocalDateTime.now())
                .build());
    }

    @Test
    void end_to_end_over_jms() {
        // publish a capital raise announcement over JMS
        producer.publish("CAPITAL_RAISE FOO 0.25");

        // verify a CapitalRaise action shows up for this portfolio
        Awaitility.await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            var actions = actionRepository.findAllActionsOf(portfolio.getUuid());
            boolean hasCapitalRaise = actions.stream()
                    .anyMatch(a -> a.getClass().getSimpleName().equalsIgnoreCase("CapitalRaise"));
            assertThat(hasCapitalRaise).isTrue();
        });
    }
}
