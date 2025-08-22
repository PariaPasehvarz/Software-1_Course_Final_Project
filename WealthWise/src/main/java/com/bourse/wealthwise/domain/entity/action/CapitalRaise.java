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
 * Grants non-tradable rights to existing shareholders due to a capital raise.
 * No immediate cash impact.
 */
@SuperBuilder
@Getter
public class CapitalRaise extends BaseAction {

    private final Security security;
        private final BigInteger volume;

    private void setActionType() {
        this.actionType = ActionType.CAPITAL_RAISE;
    }

    @Override
    public List<BalanceChange> getBalanceChanges() {
        return List.of(); // no cash movement on grant
    }

    @Override
    public List<SecurityChange> getSecurityChanges() {
        return List.of(
            SecurityChange.builder()
                .uuid(UUID.randomUUID())
                .datetime(LocalDateTime.now())
                .portfolio(this.getPortfolio())
                .security(security)
                .action(this)
                .isTradable(Boolean.FALSE) // rights start as non-tradable
                .volumeChange(volume) // positive: grant
                .build()
        );
    }

    @Override
    public String accept(ActionVisitor visitor) {
        return visitor.visit(this);
    }
}