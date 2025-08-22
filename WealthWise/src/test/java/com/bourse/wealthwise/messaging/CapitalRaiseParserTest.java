package com.bourse.wealthwise.messaging;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CapitalRaiseParserTest {

    @Test
    void parsesHappyPath() {
        var msg = CapitalRaiseParser.parse("CAPITAL_RAISE FOOLAD 0.25");
        assertEquals("FOOLAD", msg.getSymbol());
        assertEquals(0.25, msg.getPerShare(), 1e-9);
    }

    @Test
    void rejectsWrongVerb() {
        assertThrows(IllegalArgumentException.class, () ->
            CapitalRaiseParser.parse("RAISE FOOLAD 0.25"));
    }

    @Test
    void rejectsBadNumber() {
        assertThrows(IllegalArgumentException.class, () ->
            CapitalRaiseParser.parse("CAPITAL_RAISE FOOLAD notANumber"));
    }

    @Test
    void rejectsWrongArity() {
        assertThrows(IllegalArgumentException.class, () ->
            CapitalRaiseParser.parse("CAPITAL_RAISE ONLY_TWO"));
    }
}