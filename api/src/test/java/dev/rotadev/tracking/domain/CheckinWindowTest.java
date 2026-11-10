package dev.rotadev.tracking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CheckinWindowTest {

    // 2026-11-09 é uma segunda-feira.
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    @Test
    void weekendOpensCurrentWeek() {
        assertThat(CheckinWindow.weekFor(MONDAY.plusDays(5))).contains(MONDAY);
        assertThat(CheckinWindow.weekFor(MONDAY.plusDays(6))).contains(MONDAY);
    }

    @Test
    void mondayAndTuesdayStillAcceptLastWeek() {
        assertThat(CheckinWindow.weekFor(MONDAY.plusWeeks(1))).contains(MONDAY);
        assertThat(CheckinWindow.weekFor(MONDAY.plusWeeks(1).plusDays(1))).contains(MONDAY);
    }

    @Test
    void midweekIsClosed() {
        assertThat(CheckinWindow.weekFor(MONDAY.plusDays(2))).isEmpty();
    }
}
