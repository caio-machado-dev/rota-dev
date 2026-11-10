package dev.rotadev.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PricingTest {

    private final Pricing opus55 = new Pricing(new BigDecimal("4.00"), new BigDecimal("20.00"), new BigDecimal("0.20"), new BigDecimal("5.00"));

    @Test
    void costsInMicroDollars() {
        // 10 mil tokens de entrada a $4/M = $0,04; 2 mil de saída a $20/M = $0,04. Total $0,08.
        assertThat(opus55.costUsdMicros(new TokenUsage(10_000, 2_000, 0, 0))).isEqualTo(80_000L);
    }

    @Test
    void cacheReadsAreCheaper() {
        assertThat(opus55.costUsdMicros(new TokenUsage(0, 0, 10_000, 0))).isEqualTo(2_000L);
    }
}
