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
