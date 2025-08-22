package com.bourse.wealthwise.messaging;

public class CapitalRaiseParser {

    public static class CapitalRaiseMsg {
        private final String symbol;
        private final double perShare;
        public CapitalRaiseMsg(String symbol, double perShare) {
            this.symbol = symbol;
            this.perShare = perShare;
        }
        public String getSymbol() { return symbol; }
        public double getPerShare() { return perShare; }
    }

    /**
     * Parses messages in the exact format: "CAPITAL_RAISE <SYMBOL> <PER_SHARE>"
     * e.g., "CAPITAL_RAISE FOOLAD 0.25"
     * @throws IllegalArgumentException if the format is invalid
     */
    public static CapitalRaiseMsg parse(String msg) {
        if (msg == null) throw new IllegalArgumentException("Message is null");
        String[] parts = msg.trim().split("\\s+");
        if (parts.length != 3) throw new IllegalArgumentException("Expected 3 parts");
        if (!"CAPITAL_RAISE".equals(parts[0])) throw new IllegalArgumentException("Unknown verb: " + parts[0]);
        String symbol = parts[1];
        double perShare;
        try {
            perShare = Double.parseDouble(parts[2]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Per-share must be a number");
        }
        return new CapitalRaiseMsg(symbol, perShare);
    }
}