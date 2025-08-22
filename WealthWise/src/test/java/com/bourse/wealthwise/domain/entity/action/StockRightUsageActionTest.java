package com.bourse.wealthwise.domain.entity.action;

import com.bourse.wealthwise.domain.entity.account.User;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.domain.entity.balance.BalanceChange;
import com.bourse.wealthwise.domain.entity.security.SecurityChange;
import com.bourse.wealthwise.domain.entity.action.StockRightUsage;


import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class StockRightUsageActionTest {

    @Test
    void uses_rights_mints_stock_and_debits_cash() {
        var p = new Portfolio("P1", User.builder().firstName("A").lastName("B").build(), "Port");
        var right = Security.builder().name("Foo Right").symbol("FOO_X").isin("ISIN-FOO-X").build();
        var stock = Security.builder().name("Foo Inc").symbol("FOO").isin("ISIN-FOO").build();

        var usage = StockRightUsage.builder()
                .portfolio(p)
                .rightSecurity(right)
                .stockSecurity(stock)
                .rightsUsed(BigInteger.valueOf(5))
                .pricePerRight(BigInteger.valueOf(100))
                .datetime(LocalDateTime.now())
                .build();

        List<SecurityChange> sc = usage.getSecurityChanges();
        assertThat(sc).hasSize(2);
        assertThat(sc.get(0).getSecurity().getSymbol()).isEqualTo("FOO_X");
        assertThat(sc.get(0).getVolumeChange()).isEqualTo(BigInteger.valueOf(-5));
        assertThat(sc.get(1).getSecurity().getSymbol()).isEqualTo("FOO");
        assertThat(sc.get(1).getVolumeChange()).isEqualTo(BigInteger.valueOf(5));

        List<BalanceChange> bc = usage.getBalanceChanges();
        assertThat(bc).hasSize(1);
        assertThat(bc.get(0).getChange_amount()).isEqualTo(BigInteger.valueOf(-500));
    }
}