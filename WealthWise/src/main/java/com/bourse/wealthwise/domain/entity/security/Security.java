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
