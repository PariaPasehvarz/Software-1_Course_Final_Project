package com.bourse.wealthwise.domain.entity.action;

import com.bourse.wealthwise.domain.entity.action.utils.ActionVisitor;
import com.bourse.wealthwise.domain.entity.balance.BalanceChange;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import com.bourse.wealthwise.domain.entity.security.Security;
import com.bourse.wealthwise.domain.entity.security.SecurityChange;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uses stock rights to acquire the underlying stock at a fixed price.
 * Side effects:
 *  - rights decrease
 *  - stock increases
 *  - cash decreases (debit)
 */
@SuperBuilder
@Getter
public class StockRightUsage extends BaseAction {

    private final Security rightSecurity;
    private final Security stockSecurity;
    private final BigInteger rightsUsed;
    private final BigInteger pricePerRight; // e.g., 100

    private void setActionType() {
        this.actionType = ActionType.STOCK_RIGHT_USAGE;
    }

    @Override
    public List<BalanceChange> getBalanceChanges() {
        BigInteger totalCost = rightsUsed.multiply(pricePerRight);
        return List.of(
            BalanceChange.builder()
                .uuid(UUID.randomUUID())
                .datetime(LocalDateTime.now())
                .portfolio(this.getPortfolio())
                .change_amount(totalCost.negate()) // debit
                .action(this)
                .build()
        );
    }

    @Override
    public List<SecurityChange> getSecurityChanges() {
        return List.of(
            // consume rights
            SecurityChange.builder()
                .uuid(UUID.randomUUID())
                .datetime(LocalDateTime.now())
                .portfolio(this.getPortfolio())
                .security(rightSecurity)
                .action(this)
                .isTradable(Boolean.TRUE)
                .volumeChange(rightsUsed.negate())
                .build(),
            // receive stock
            SecurityChange.builder()
                .uuid(UUID.randomUUID())
                .datetime(LocalDateTime.now())
                .portfolio(this.getPortfolio())
                .security(stockSecurity)
                .action(this)
                .isTradable(Boolean.TRUE)
                .volumeChange(rightsUsed)
                .build()
        );
    }

    @Override
    public String accept(ActionVisitor visitor) {
        return visitor.visit(this);
    }
}