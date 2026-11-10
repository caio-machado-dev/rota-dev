package dev.rotadev.tracking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ReviewHabitTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    @Test
    void completeOnlyWithAllFourSteps() {
        assertThat(new ReviewHabit(DAY, true, true, true, "Entendi o retry do worker").complete()).isTrue();
        assertThat(new ReviewHabit(DAY, true, true, true, "  ").complete()).isFalse();
        assertThat(new ReviewHabit(DAY, true, false, true, "nota").complete()).isFalse();
    }

    @Test
    void countsStepsDone() {
        assertThat(new ReviewHabit(DAY, true, false, true, null).stepsDone()).isEqualTo(2);
    }
}
