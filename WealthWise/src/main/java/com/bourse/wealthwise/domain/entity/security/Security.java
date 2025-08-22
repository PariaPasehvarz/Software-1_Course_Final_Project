package com.bourse.wealthwise.domain.entity.security;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

@Getter
@ToString
@Builder
public class Security {

    private String name ;

    private String symbol;


    public String getName() {
        return name;
    }

    @Builder.Default
    private SecurityType securityType = SecurityType.STOCK;

    private String isin;

    public String getIsin() {
        return isin;
    }

    public void setIsin(String testIsin) {
        isin = testIsin;
    }

    public void setName(String _name) {
        name = _name;
    }
}
// Ali's Edit, to be deleted if everything worked well
//import java.util.Objects;
//
//public final class Security {
//    private final String symbol; // e.g. FOOLAD, FOOLAD_X
//    private final SecurityType type;
//
//    public Security(String symbol, SecurityType type) {
//        this.symbol = Objects.requireNonNull(symbol).toUpperCase();
//        this.type = Objects.requireNonNull(type);
//    }
//
//    public String getSymbol() { return symbol; }
//    public SecurityType getType() { return type; }
//
//    @Override public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Security)) return false;
//        Security that = (Security) o;
//        return symbol.equals(that.symbol) && type == that.type;
//    }
//
//    @Override public int hashCode() { return Objects.hash(symbol, type); }
//    @Override public String toString() { return symbol + "(" + type + ")"; }
//}
