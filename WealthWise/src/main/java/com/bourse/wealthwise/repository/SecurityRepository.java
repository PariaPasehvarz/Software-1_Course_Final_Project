package com.bourse.wealthwise.repository;

import com.bourse.wealthwise.domain.entity.security.Security;
import org.springframework.stereotype.Component;
import java.util.HashMap;


@Component
public class SecurityRepository {
    private final HashMap<String, Security> securityByIsin = new HashMap<>();
    private final HashMap<String, Security> securityBySymbol = new HashMap<>();

    public Security findSecurityByIsin(String isin) {
        return securityByIsin.get(isin);
    }

    public Security findSecurityBySymbol(String symbol) {
        return securityBySymbol.get(symbol);
    }

    public void addSecurity(Security security) {
        securityByIsin.put(security.getIsin(), security);
        securityBySymbol.put(security.getSymbol(), security);
    }

    public void clear() {
        securityByIsin.clear();
        securityBySymbol.clear();
    }

    public Iterable<Security> allSecurities() {
        return securityByIsin.values();
    }
}
