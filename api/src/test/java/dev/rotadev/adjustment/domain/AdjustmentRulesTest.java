package dev.rotadev.adjustment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class AdjustmentRulesTest {

    private static final LocalDate W2 = LocalDate.of(2026, 11, 9);
    private static final LocalDate W1 = W2.minusWeeks(1);
    private static final int BUDGET = 600;

    @Test
    void hardWeekComesFirst() {
        var proposal = AdjustmentRules.propose(List.of(new WeekResult(W2, 50, 0, 2, "Mudança de casa")), BUDGET);

        assertThat(proposal).get().extracting(Proposal::kind).isEqualTo(AdjustmentKind.LIGHTER_WEEK);
        assertThat(proposal.get().reason()).contains("Mudança de casa");
    }

    @Test
    void twoLowWeeksReduceLoadToRealAverage() {
        var proposal = AdjustmentRules.propose(List.of(new WeekResult(W2, 300, 3, 4, null), new WeekResult(W1, 240, 3, 4, null)), BUDGET);

        assertThat(proposal).get().satisfies(p -> {
            assertThat(p.kind()).isEqualTo(AdjustmentKind.REDUCE_LOAD);
            assertThat(p.guidance()).contains("5 h por semana");
        });
    }

    @Test
    void oneLowWeekIsNotEnough() {
        var proposal = AdjustmentRules.propose(List.of(new WeekResult(W2, 100, 3, 4, null), new WeekResult(W1, 600, 3, 4, null)), BUDGET);

        assertThat(proposal).isEmpty();
    }

    @Test
    void twoHighWeeksSuggestAnticipating() {
        var proposal = AdjustmentRules.propose(List.of(new WeekResult(W2, 800, 3, 5, null), new WeekResult(W1, 760, 3, 5, null)), BUDGET);

        assertThat(proposal).get().extracting(Proposal::kind).isEqualTo(AdjustmentKind.INCREASE_LOAD);
    }

    @Test
    void fewReviewDaysAskForMoreReview() {
        var proposal = AdjustmentRules.propose(List.of(new WeekResult(W2, 600, 1, 4, null)), BUDGET);

        assertThat(proposal).get().extracting(Proposal::kind).isEqualTo(AdjustmentKind.MORE_REVIEW);
    }

    @Test
    void goodWeekNeedsNothing() {
        assertThat(AdjustmentRules.propose(List.of(new WeekResult(W2, 600, 4, 4, null)), BUDGET)).isEmpty();
    }
}
