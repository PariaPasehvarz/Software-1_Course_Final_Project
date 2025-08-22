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
