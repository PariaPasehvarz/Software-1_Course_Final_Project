package com.bourse.wealthwise.domain.entity.security;

import com.bourse.wealthwise.domain.entity.action.BaseAction;
import com.bourse.wealthwise.domain.entity.portfolio.Portfolio;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a change in holdings of a security within a portfolio,
 * caused by an action (Buy, Sale, CapitalRaise, RightUsage, etc).
 */
@Getter
@Builder
public class SecurityChange {

    private final UUID uuid;                 // unique event id
    private final LocalDateTime datetime;    // when the change happened

    private final Portfolio portfolio;       // portfolio this change belongs to
    private final Security security;         // the affected security
    private final BaseAction action;         // action that triggered the change

    private final Boolean isTradable;        // true = can be traded, false = expired/non-tradable
    private final BigInteger volumeChange;   // positive or negative

    // Convenience factory for new changes
    public static SecurityChange of(Portfolio portfolio,
                                    Security security,
                                    BaseAction action,
                                    BigInteger volumeChange,
                                    boolean isTradable) {
        return SecurityChange.builder()
                .uuid(UUID.randomUUID())
                .datetime(LocalDateTime.now())
                .portfolio(portfolio)
                .security(security)
                .action(action)
                .isTradable(isTradable)
                .volumeChange(volumeChange)
                .build();
    }
}
