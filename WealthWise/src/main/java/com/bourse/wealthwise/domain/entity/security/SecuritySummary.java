package com.bourse.wealthwise.domain.entity.security;

import java.math.BigDecimal;

public class SecuritySummary {
    private Security security;
    private int volume;
    private BigDecimal value;

    public SecuritySummary(Security security, int volume, BigDecimal value) {
        this.security = security;
        this.volume = volume;
        this.value = value;
    }

    public Security getSecurity() {
        return security;
    }

    public int getVolume() {
        return volume;
    }

    public BigDecimal getValue() {
        return value;
    }
}
