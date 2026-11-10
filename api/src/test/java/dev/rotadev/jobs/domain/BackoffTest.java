package dev.rotadev.jobs.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class BackoffTest {

    private final Backoff backoff = new Backoff(Duration.ofSeconds(30), Duration.ofMinutes(5));

    @Test
    void doublesAfterEachFailure() {
        assertThat(backoff.delayAfter(1)).isEqualTo(Duration.ofSeconds(30));
        assertThat(backoff.delayAfter(2)).isEqualTo(Duration.ofMinutes(1));
        assertThat(backoff.delayAfter(3)).isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void stopsAtMax() {
        assertThat(backoff.delayAfter(5)).isEqualTo(Duration.ofMinutes(5));
        assertThat(backoff.delayAfter(1_000)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void rejectsAttemptZero() {
        assertThatThrownBy(() -> backoff.delayAfter(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
