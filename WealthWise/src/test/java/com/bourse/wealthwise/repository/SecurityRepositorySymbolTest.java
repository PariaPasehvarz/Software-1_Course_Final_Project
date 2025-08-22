package com.bourse.wealthwise.repository;

import com.bourse.wealthwise.domain.entity.security.Security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class SecurityRepositorySymbolTest {

    @Autowired
    private SecurityRepository securityRepository;

    @BeforeEach
    void reset() {
        securityRepository.clear();
    }

    @Test
    void addAndFindBySymbol() {
        Security s = Security.builder()
                .name("foolad-mobarake")
                .symbol("FOOLAD")
                .isin("1234567890")
                .build();

        securityRepository.addSecurity(s);

        Security found = securityRepository.findSecurityBySymbol("FOOLAD");
        assertThat(found).isNotNull();
        assertThat(found.getSymbol()).isEqualTo("FOOLAD");
        assertThat(found.getIsin()).isEqualTo("1234567890");
    }

    @Test
    void wrongSymbolReturnsNull() {
        Security found = securityRepository.findSecurityBySymbol("NOPE");
        assertNull(found);
    }
}
