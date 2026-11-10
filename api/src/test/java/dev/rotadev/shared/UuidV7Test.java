package dev.rotadev.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void hasVersion7AndRfcVariant() {
        UUID id = UuidV7.generate(Instant.parse("2026-11-10T12:00:00Z"));

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    void sortsByTime() {
        UUID earlier = UuidV7.generate(Instant.parse("2026-11-10T12:00:00Z"));
        UUID later = UuidV7.generate(Instant.parse("2026-11-10T12:00:01Z"));

        assertThat(earlier.toString()).isLessThan(later.toString());
    }
}
