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
