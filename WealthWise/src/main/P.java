// =========================
// Project structure (suggested)
// =========================
// src/main/java/com/wealthwise/rights/
//   domain/
//     SecurityType.java
//     Security.java
//     HoldingSnapshot.java
//     SecurityChange.java
//     ActionType.java
//   repo/
//     PortfolioReadModel.java
//     SecurityChangeRepository.java
//   service/
//     PortfolioQueryService.java
//     CapitalRaiseService.java
//     RightTradingService.java
//     StockRightUsageService.java
//     RightSymbolPolicy.java
//   messaging/
//     CapitalRaiseListener.java
//   exception/
//     DomainException.java
//     NotEnoughRightsException.java
//     UnknownSecurityException.java
//
// src/test/java/com/wealthwise/rights/
//   CapitalRaiseServiceTest.java
//   RightTradingServiceTest.java
//   StockRightUsageServiceTest.java
//
// Notes:
// - This module is intentionally persistence‑agnostic: SecurityChangeRepository is an interface with an
//   in‑memory test implementation so you can plug your own JPA/Mongo/EventStore later.
// - Event Sourcing: portfolio state is derived from the event log (SecurityChange list).
// - Right symbol mapping uses a simple policy: right of symbol "FOOLAD" becomes "FOOLAD_X".
// - Money/cash handling is NOT implemented here (you can connect to your wallet/cash module). We only enforce
//   domain constraints for quantities; where price/cash is needed, methods accept a price parameter but ignore
//   the balance.

// =========================
// domain/SecurityType.java
// =========================
package com.wealthwise.rights.domain;

public enum SecurityType {
    STOCK,
    STOCK_RIGHT
}

// =========================
// domain/Security.java
// =========================
package com.wealthwise.rights.domain;

import java.util.Objects;

public final class Security {
    private final String symbol; // e.g. FOOLAD, FOOLAD_X
    private final SecurityType type;

    public Security(String symbol, SecurityType type) {
        this.symbol = Objects.requireNonNull(symbol).toUpperCase();
        this.type = Objects.requireNonNull(type);
    }

    public String getSymbol() { return symbol; }
    public SecurityType getType() { return type; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Security)) return false;
        Security that = (Security) o;
        return symbol.equals(that.symbol) && type == that.type;
    }

    @Override public int hashCode() { return Objects.hash(symbol, type); }
    @Override public String toString() { return symbol + "(" + type + ")"; }
}

// =========================
// domain/HoldingSnapshot.java (read model per portfolio for a moment in time)
// =========================
package com.wealthwise.rights.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HoldingSnapshot {
    private final String portfolioId;
    private final Map<String, Long> volumesBySymbol; // symbol -> volume (>= 0)

    public HoldingSnapshot(String portfolioId, Map<String, Long> volumesBySymbol) {
        this.portfolioId = portfolioId;
        this.volumesBySymbol = new HashMap<>(volumesBySymbol);
    }

    public String getPortfolioId() { return portfolioId; }
    public long volumeOf(String symbol) { return volumesBySymbol.getOrDefault(symbol.toUpperCase(), 0L); }
    public Map<String, Long> asMap() { return Collections.unmodifiableMap(volumesBySymbol); }
}

// =========================
// domain/ActionType.java
// =========================
package com.wealthwise.rights.domain;

public enum ActionType {
    CAPITAL_RAISE,
    BUY_RIGHT,
    SELL_RIGHT,
    RIGHT_USAGE,
    ADJUSTMENT // generic manual adjustments if needed
}

// =========================
// domain/SecurityChange.java (event)
// =========================
package com.wealthwise.rights.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class SecurityChange {
    private final String id; // event id
    private final String portfolioId;
    private final String symbol;         // e.g. FOOLAD or FOOLAD_X
    private final long delta;             // can be negative
    private final LocalDateTime at;       // event time
    private final ActionType actionType;  // provenance
    private final String actionRef;       // optional external reference (order id, message id, etc.)

    public SecurityChange(String portfolioId,
                          String symbol,
                          long delta,
                          LocalDateTime at,
                          ActionType actionType,
                          String actionRef) {
        if (delta == 0) throw new IllegalArgumentException("delta cannot be zero");
        this.id = UUID.randomUUID().toString();
        this.portfolioId = Objects.requireNonNull(portfolioId);
        this.symbol = Objects.requireNonNull(symbol).toUpperCase();
        this.delta = delta;
        this.at = at == null ? LocalDateTime.now() : at;
        this.actionType = Objects.requireNonNull(actionType);
        this.actionRef = actionRef;
    }

    public String getId() { return id; }
    public String getPortfolioId() { return portfolioId; }
    public String getSymbol() { return symbol; }
    public long getDelta() { return delta; }
    public LocalDateTime getAt() { return at; }
    public ActionType getActionType() { return actionType; }
    public String getActionRef() { return actionRef; }
}

// =========================
// exception/DomainException.java
// =========================
package com.wealthwise.rights.exception;

public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
}

// =========================
// exception/NotEnoughRightsException.java
// =========================
package com.wealthwise.rights.exception;

public class NotEnoughRightsException extends DomainException {
    public NotEnoughRightsException(String symbol, long wanted, long available) {
        super("Not enough rights to sell/use: " + symbol + ", wanted=" + wanted + ", available=" + available);
    }
}

// =========================
// exception/UnknownSecurityException.java
// =========================
package com.wealthwise.rights.exception;

public class UnknownSecurityException extends DomainException {
    public UnknownSecurityException(String symbol) { super("Unknown security: " + symbol); }
}

// =========================
// repo/SecurityChangeRepository.java (event store abstraction)
// =========================
package com.wealthwise.rights.repo;

import com.wealthwise.rights.domain.SecurityChange;
import java.time.LocalDateTime;
import java.util.List;

public interface SecurityChangeRepository {
    void append(SecurityChange event);
    void appendAll(List<SecurityChange> events);
    List<SecurityChange> findByPortfolio(String portfolioId);
    List<SecurityChange> findByPortfolioUpTo(String portfolioId, LocalDateTime upTo);
    List<String> listPortfolioIds();
}

// =========================
// repo/PortfolioReadModel.java (rebuild holdings from events)
// =========================
package com.wealthwise.rights.repo;

import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioReadModel {

    public HoldingSnapshot rebuild(String portfolioId, List<SecurityChange> events) {
        Map<String, Long> volumes = new HashMap<>();
        for (SecurityChange e : events) {
            if (!e.getPortfolioId().equals(portfolioId)) continue;
            volumes.merge(e.getSymbol(), e.getDelta(), Long::sum);
        }
        // remove zeros/negatives from the snapshot map (but allow 0 in logic if you prefer)
        volumes.entrySet().removeIf(en -> en.getValue() == 0);
        return new HoldingSnapshot(portfolioId, volumes);
    }

    public HoldingSnapshot rebuildUpTo(String portfolioId, List<SecurityChange> events, LocalDateTime upTo) {
        Map<String, Long> volumes = new HashMap<>();
        for (SecurityChange e : events) {
            if (!e.getPortfolioId().equals(portfolioId)) continue;
            if (e.getAt().isAfter(upTo)) continue;
            volumes.merge(e.getSymbol(), e.getDelta(), Long::sum);
        }
        volumes.entrySet().removeIf(en -> en.getValue() == 0);
        return new HoldingSnapshot(portfolioId, volumes);
    }
}

// =========================
// service/RightSymbolPolicy.java
// =========================
package com.wealthwise.rights.service;

public class RightSymbolPolicy {
    public String rightOf(String stockSymbol) { return stockSymbol.toUpperCase() + "_X"; }
    public String stockOf(String rightSymbol) {
        if (rightSymbol.toUpperCase().endsWith("_X")) return rightSymbol.substring(0, rightSymbol.length() - 2);
        return rightSymbol; // best effort fallback
    }
}

// =========================
// service/PortfolioQueryService.java
// =========================
package com.wealthwise.rights.service;

import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.repo.PortfolioReadModel;
import com.wealthwise.rights.repo.SecurityChangeRepository;

public class PortfolioQueryService {
    private final SecurityChangeRepository repo;
    private final PortfolioReadModel readModel = new PortfolioReadModel();

    public PortfolioQueryService(SecurityChangeRepository repo) {
        this.repo = repo;
    }

    public HoldingSnapshot currentHoldings(String portfolioId) {
        return readModel.rebuild(portfolioId, repo.findByPortfolio(portfolioId));
    }
}

// =========================
// service/CapitalRaiseService.java
// =========================
package com.wealthwise.rights.service;

import com.wealthwise.rights.domain.ActionType;
import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.repo.SecurityChangeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumes a capital raise announcement and allocates rights to all portfolios that hold the stock.
 * Message format: "CAPITAL_RAISE <SYMBOL> <AMOUNT_PER_SHARE>"
 */
public class CapitalRaiseService {
    private final SecurityChangeRepository repo;
    private final PortfolioQueryService queryService;
    private final RightSymbolPolicy policy = new RightSymbolPolicy();

    public CapitalRaiseService(SecurityChangeRepository repo) {
        this.repo = repo;
        this.queryService = new PortfolioQueryService(repo);
    }

    public void processAnnouncement(String message, String messageId) {
        String[] parts = message.trim().split("\\s+");
        if (parts.length != 3 || !"CAPITAL_RAISE".equalsIgnoreCase(parts[0])) {
            throw new IllegalArgumentException("Invalid capital raise message: " + message);
        }
        String symbol = parts[1].toUpperCase();
        double perShare = Double.parseDouble(parts[2]);
        String rightSymbol = policy.rightOf(symbol);

        List<SecurityChange> batch = new ArrayList<>();
        for (String pid : repo.listPortfolioIds()) {
            HoldingSnapshot snap = queryService.currentHoldings(pid);
            long held = snap.volumeOf(symbol);
            if (held <= 0) continue;
            long rightsToGrant = (long) Math.floor(held * perShare); // round down per spec
            if (rightsToGrant == 0) continue;
            batch.add(new SecurityChange(pid, rightSymbol, rightsToGrant, LocalDateTime.now(),
                    ActionType.CAPITAL_RAISE, messageId));
        }
        if (!batch.isEmpty()) repo.appendAll(batch);
    }
}

// =========================
// service/RightTradingService.java (buy/sell rights)
// =========================
package com.wealthwise.rights.service;

import com.wealthwise.rights.domain.ActionType;
import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.exception.NotEnoughRightsException;
import com.wealthwise.rights.repo.SecurityChangeRepository;

import java.time.LocalDateTime;

public class RightTradingService {
    private final SecurityChangeRepository repo;
    private final PortfolioQueryService queryService;

    public RightTradingService(SecurityChangeRepository repo) {
        this.repo = repo;
        this.queryService = new PortfolioQueryService(repo);
    }

    public void buyRights(String portfolioId, String rightSymbol, long quantity, double pricePerRight, String orderId) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        // Cash handling is out of scope; assume external wallet check succeeded.
        repo.append(new SecurityChange(portfolioId, rightSymbol.toUpperCase(), quantity, LocalDateTime.now(),
                ActionType.BUY_RIGHT, orderId));
    }

    public void sellRights(String portfolioId, String rightSymbol, long quantity, double pricePerRight, String orderId) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        HoldingSnapshot snap = queryService.currentHoldings(portfolioId);
        long have = snap.volumeOf(rightSymbol);
        if (have < quantity) throw new NotEnoughRightsException(rightSymbol, quantity, have);
        repo.append(new SecurityChange(portfolioId, rightSymbol.toUpperCase(), -quantity, LocalDateTime.now(),
                ActionType.SELL_RIGHT, orderId));
    }
}

// =========================
// service/StockRightUsageService.java (convert rights to stock)
// =========================
package com.wealthwise.rights.service;

import com.wealthwise.rights.domain.ActionType;
import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.exception.NotEnoughRightsException;
import com.wealthwise.rights.repo.SecurityChangeRepository;

import java.time.LocalDateTime;
import java.util.Arrays;

public class StockRightUsageService {
    private final SecurityChangeRepository repo;
    private final PortfolioQueryService queryService;
    private final RightSymbolPolicy policy = new RightSymbolPolicy();

    public static final int COST_PER_RIGHT = 100; // toman per spec

    public StockRightUsageService(SecurityChangeRepository repo) {
        this.repo = repo;
        this.queryService = new PortfolioQueryService(repo);
    }

    /**
     * Convert rights to ordinary shares (1:1) by paying the required cash externally.
     * Creates two events: (-rights) and (+stock).
     */
    public void useRights(String portfolioId, String rightSymbol, long quantity, String requestId) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
        HoldingSnapshot snap = queryService.currentHoldings(portfolioId);
        long have = snap.volumeOf(rightSymbol);
        if (have < quantity) throw new NotEnoughRightsException(rightSymbol, quantity, have);

        String stockSymbol = policy.stockOf(rightSymbol);
        LocalDateTime now = LocalDateTime.now();
        SecurityChange burnRights = new SecurityChange(portfolioId, rightSymbol.toUpperCase(), -quantity, now,
                ActionType.RIGHT_USAGE, requestId);
        SecurityChange mintStock = new SecurityChange(portfolioId, stockSymbol.toUpperCase(), quantity, now,
                ActionType.RIGHT_USAGE, requestId);
        repo.appendAll(Arrays.asList(burnRights, mintStock));
    }
}

// =========================
// messaging/CapitalRaiseListener.java (JMS example for Artemis)
// =========================
package com.wealthwise.rights.messaging;

import com.wealthwise.rights.service.CapitalRaiseService;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class CapitalRaiseListener {
    private final CapitalRaiseService capitalRaiseService;

    public CapitalRaiseListener(CapitalRaiseService capitalRaiseService) {
        this.capitalRaiseService = capitalRaiseService;
    }

    // Queue/topic name is example; align with your broker config
    @JmsListener(destination = "capital-raise")
    public void onMessage(String message, @Header(name = "messageId", required = false) String messageId) {
        capitalRaiseService.processAnnouncement(message, messageId == null ? "N/A" : messageId);
    }
}

// =========================
// TEST UTIL: In-memory repository for events
// =========================
package com.wealthwise.rights;

import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.repo.SecurityChangeRepository;

import java.time.LocalDateTime;
import java.util.*;
        import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemorySecurityChangeRepository implements SecurityChangeRepository {
    private final Map<String, List<SecurityChange>> byPortfolio = new ConcurrentHashMap<>();

    @Override public void append(SecurityChange event) {
        byPortfolio.computeIfAbsent(event.getPortfolioId(), k -> new ArrayList<>()).add(event);
    }

    @Override public void appendAll(List<SecurityChange> events) {
        for (SecurityChange e : events) append(e);
    }

    @Override public List<SecurityChange> findByPortfolio(String portfolioId) {
        return new ArrayList<>(byPortfolio.getOrDefault(portfolioId, List.of()));
    }

    @Override public List<SecurityChange> findByPortfolioUpTo(String portfolioId, LocalDateTime upTo) {
        return findByPortfolio(portfolioId).stream().filter(e -> !e.getAt().isAfter(upTo)).collect(Collectors.toList());
    }

    @Override public List<String> listPortfolioIds() { return new ArrayList<>(byPortfolio.keySet()); }
}

// =========================
// TESTS: CapitalRaiseServiceTest.java
// =========================
package com.wealthwise.rights;

import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.domain.ActionType;
import com.wealthwise.rights.service.CapitalRaiseService;
import com.wealthwise.rights.service.PortfolioQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class CapitalRaiseServiceTest {
    InMemorySecurityChangeRepository repo;
    CapitalRaiseService service;

    @BeforeEach void setup() {
        repo = new InMemorySecurityChangeRepository();
        service = new CapitalRaiseService(repo);
        // Seed holdings: P1 has 100 FOOLAD & 100 B, P2 has 200 FOOLAD, P3 has 500 B
        repo.append(new SecurityChange("P1", "FOOLAD", 100, null, ActionType.ADJUSTMENT, "seed"));
        repo.append(new SecurityChange("P1", "B", 100, null, ActionType.ADJUSTMENT, "seed"));
        repo.append(new SecurityChange("P2", "FOOLAD", 200, null, ActionType.ADJUSTMENT, "seed"));
        repo.append(new SecurityChange("P3", "B", 500, null, ActionType.ADJUSTMENT, "seed"));
    }

    @Test void grants_rights_proportionally_and_rounds_down() {
        service.processAnnouncement("CAPITAL_RAISE FOOLAD 0.5", "m1");
        PortfolioQueryService q = new PortfolioQueryService(repo);
        HoldingSnapshot p1 = q.currentHoldings("P1");
        HoldingSnapshot p2 = q.currentHoldings("P2");
        HoldingSnapshot p3 = q.currentHoldings("P3");
        assertEquals(100, p1.volumeOf("FOOLAD"));
        assertEquals(50, p1.volumeOf("FOOLAD_X"));
        assertEquals(200, p2.volumeOf("FOOLAD"));
        assertEquals(100, p2.volumeOf("FOOLAD_X"));
        assertEquals(0, p3.volumeOf("FOOLAD_X"));
    }
}

// =========================
// TESTS: RightTradingServiceTest.java
// =========================
package com.wealthwise.rights;

import com.wealthwise.rights.domain.ActionType;
import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.exception.NotEnoughRightsException;
import com.wealthwise.rights.service.PortfolioQueryService;
import com.wealthwise.rights.service.RightTradingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RightTradingServiceTest {
    InMemorySecurityChangeRepository repo;
    RightTradingService svc;

    @BeforeEach void setup() {
        repo = new InMemorySecurityChangeRepository();
        svc = new RightTradingService(repo);
        repo.append(new SecurityChange("P1", "FOOLAD_X", 120, null, ActionType.ADJUSTMENT, "seed"));
    }

    @Test void buy_adds_rights() {
        svc.buyRights("P1", "FOOLLAD_X".replace("LL", "L"), 30, 10.0, "o1"); // 30
        HoldingSnapshot s = new PortfolioQueryService(repo).currentHoldings("P1");
        assertEquals(150, s.volumeOf("FOOLAD_X"));
    }

    @Test void sell_reduces_rights() {
        svc.sellRights("P1", "FOOLAD_X", 20, 10.0, "o2");
        HoldingSnapshot s = new PortfolioQueryService(repo).currentHoldings("P1");
        assertEquals(100, s.volumeOf("FOOLAD_X"));
    }

    @Test void cannot_sell_more_than_owned() {
        assertThrows(NotEnoughRightsException.class, () ->
                svc.sellRights("P1", "FOOLAD_X", 121, 10.0, "o3"));
    }
}

// =========================
// TESTS: StockRightUsageServiceTest.java
// =========================
package com.wealthwise.rights;

import com.wealthwise.rights.domain.ActionType;
import com.wealthwise.rights.domain.HoldingSnapshot;
import com.wealthwise.rights.domain.SecurityChange;
import com.wealthwise.rights.exception.NotEnoughRightsException;
import com.wealthwise.rights.service.PortfolioQueryService;
import com.wealthwise.rights.service.StockRightUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StockRightUsageServiceTest {
    InMemorySecurityChangeRepository repo;
    StockRightUsageService svc;

    @BeforeEach void setup() {
        repo = new InMemorySecurityChangeRepository();
        svc = new StockRightUsageService(repo);
        repo.append(new SecurityChange("P1", "FOOLAD", 100, null, ActionType.ADJUSTMENT, "seed"));
        repo.append(new SecurityChange("P1", "FOOLAD_X", 60, null, ActionType.ADJUSTMENT, "seed"));
    }

    @Test void converts_rights_to_stock_one_to_one() {
        svc.useRights("P1", "FOOLAD_X", 40, "r1");
        HoldingSnapshot s = new PortfolioQueryService(repo).currentHoldings("P1");
        assertEquals(20, s.volumeOf("FOOLAD_X"));
        assertEquals(140, s.volumeOf("FOOLAD"));
    }

    @Test void cannot_use_more_than_owned() {
        assertThrows(NotEnoughRightsException.class, () -> svc.useRights("P1", "FOOLAD_X", 100, "r2"));
    }
}

// =========================
// OPTIONAL: Spring configuration wiring (if you want to auto-configure beans)
// =========================
package com.wealthwise.rights;

import com.wealthwise.rights.repo.SecurityChangeRepository;
import com.wealthwise.rights.service.CapitalRaiseService;
import com.wealthwise.rights.service.RightTradingService;
import com.wealthwise.rights.service.StockRightUsageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RightsModuleConfig {
    @Bean public CapitalRaiseService capitalRaiseService(SecurityChangeRepository repo) { return new CapitalRaiseService(repo); }
    @Bean public RightTradingService rightTradingService(SecurityChangeRepository repo) { return new RightTradingService(repo); }
    @Bean public StockRightUsageService stockRightUsageService(SecurityChangeRepository repo) { return new StockRightUsageService(repo); }
}
