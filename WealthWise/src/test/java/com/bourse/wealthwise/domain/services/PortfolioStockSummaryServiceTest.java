package com.bourse.wealthwise.domain.services;

import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.action.Buy;
import com.bourse.wealthwise.domain.entity.action.Sale;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.domain.entity.security.SecuritySummary;
import com.bourse.wealthwise.domain.entity.security.SecurityType;
import com.bourse.wealthwise.repository.ActionRepository;
import com.bourse.wealthwise.repository.SecurityPriceRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PortfolioStockSummaryServiceTest {

    @Autowired
    private PortfolioStockSummaryService summaryService;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private SecurityPriceRepository securityPriceRepository;

    private final LocalDate TEST_DATE = LocalDate.of(2025, 6, 16);

    @BeforeEach
    public void clearData() {
        actionRepository.clear();
        securityPriceRepository.clear();
    }

    @Nested
    @DisplayName("✅ Basic Functionality Tests")
    class BasicTests {
        @Test
        void testSummary_withBuyAndSale() {
            Portfolio portfolio = createTestPortfolio();
            Security security = createTestSecurity("TestSecurity", "FAKE123");

            Buy buy = Buy.builder()
                    .uuid(UUID.randomUUID().toString())
                    .portfolio(portfolio)
                    .security(security)
                    .volume(BigInteger.valueOf(10))
                    .price(150)
                    .totalValue(BigInteger.valueOf(1500))
                    .datetime(LocalDateTime.of(2025, 6, 15, 10, 0))
                    .build();

            Sale sale = Sale.builder()
                    .uuid(UUID.randomUUID().toString())
                    .portfolio(portfolio)
                    .security(security)
                    .volume(BigInteger.valueOf(4))
                    .price(150)
                    .totalValue(BigInteger.valueOf(600))
                    .datetime(LocalDateTime.of(2025, 6, 15, 14, 0))
                    .build();

            actionRepository.save(buy);
            actionRepository.save(sale);
            securityPriceRepository.addPrice(security.getIsin(), TEST_DATE, 150.0);

            List<SecuritySummary> result = summaryService.getSecuritiesSummary(UUID.fromString(portfolio.getUuid()), TEST_DATE);
            assertEquals(1, result.size());

            SecuritySummary summary = result.get(0);
            assertEquals(6, summary.getVolume());
            assertEquals(6 * 150.0, summary.getValue().doubleValue());

        }
    }

    @Nested
    @DisplayName("🟡 Edge Case Tests")
    class EdgeCaseTests {

        @Test
        void testSummary_noActions() {
            UUID portfolioId = UUID.randomUUID();
            List<SecuritySummary> result = summaryService.getSecuritiesSummary(portfolioId, TEST_DATE);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void testSummary_onlyBuys() {
            Portfolio portfolio = createTestPortfolio();
            Security security = createTestSecurity("OnlyBuy", "ISIN1");

            Buy buy = Buy.builder()
                    .uuid(UUID.randomUUID().toString())
                    .portfolio(portfolio)
                    .security(security)
                    .volume(BigInteger.valueOf(5))
                    .price(100)
                    .totalValue(BigInteger.valueOf(500))
                    .datetime(LocalDateTime.of(2025, 6, 15, 10, 0))
                    .build();

            actionRepository.save(buy);
            securityPriceRepository.addPrice(security.getIsin(), TEST_DATE, 100.0);

            List<SecuritySummary> result = summaryService.getSecuritiesSummary(UUID.fromString(portfolio.getUuid()), TEST_DATE);
            assertEquals(1, result.size());
            assertEquals(5, result.get(0).getVolume());
            assertEquals(500.0, result.get(0).getValue().doubleValue());
        }

        @Test
        void testSummary_overSell() {
            Portfolio portfolio = createTestPortfolio();
            Security security = createTestSecurity("OverSell", "ISIN2");

            Buy buy = Buy.builder()
                    .uuid(UUID.randomUUID().toString())
                    .portfolio(portfolio)
                    .security(security)
                    .volume(BigInteger.valueOf(3))
                    .price(100)
                    .totalValue(BigInteger.valueOf(300))
                    .datetime(LocalDateTime.of(2025, 6, 15, 10, 0))
                    .build();

            Sale sale = Sale.builder()
                    .uuid(UUID.randomUUID().toString())
                    .portfolio(portfolio)
                    .security(security)
                    .volume(BigInteger.valueOf(5)) // overselling
                    .price(100)
                    .totalValue(BigInteger.valueOf(500))
                    .datetime(LocalDateTime.of(2025, 6, 15, 14, 0))
                    .build();

            actionRepository.save(buy);
            actionRepository.save(sale);
            securityPriceRepository.addPrice(security.getIsin(), TEST_DATE, 100.0);

            List<SecuritySummary> result = summaryService.getSecuritiesSummary(UUID.fromString(portfolio.getUuid()), TEST_DATE);
            assertEquals(1, result.size());
            assertEquals(-2, result.get(0).getVolume());
            assertEquals(-200.0, result.get(0).getValue().doubleValue());
        }
    }

    // 🔧 Helpers

    private Portfolio createTestPortfolio() {
        User user = User.builder()
                .firstName("test")
                .lastName("user")
                .uuid(UUID.randomUUID().toString())
                .build();
        return new Portfolio(UUID.randomUUID().toString(), user, "Test Portfolio");
    }

    private Security createTestSecurity(String name, String isin) {
        return Security.builder()
                .name(name)
                .symbol("SYM_" + name)
                .isin(isin)
                .securityType(SecurityType.STOCK)
                .build();
    }
}
