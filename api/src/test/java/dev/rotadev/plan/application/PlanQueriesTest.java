package dev.rotadev.plan.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.rotadev.plan.domain.PlanContent;

/** Em que semana do plano a pessoa está hoje? A semana 1 começa na data de início do plano. */
class PlanQueriesTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 11, 9);

    private final PlanStore store = mock(PlanStore.class);
    private final PlanQueries queries = new PlanQueries(store);

    @Test
    void findsTheWeekContainingToday() {
        when(store.current(USER)).thenReturn(Optional.of(plan()));

        assertThat(queries.weekOf(USER, START).orElseThrow().index()).isEqualTo(1);
        assertThat(queries.weekOf(USER, START.plusDays(6)).orElseThrow().index()).isEqualTo(1);
        assertThat(queries.weekOf(USER, START.plusDays(7)).orElseThrow().theme()).isEqualTo("Testes");
    }

    @Test
    void emptyBeforeStartOrAfterDetailedWeeks() {
        when(store.current(USER)).thenReturn(Optional.of(plan()));

        assertThat(queries.weekOf(USER, START.minusDays(1))).isEmpty();
        assertThat(queries.weekOf(USER, START.plusWeeks(5))).isEmpty();
    }

    private static PlanStore.StoredPlan plan() {
        var weeks = List.of(
                new PlanContent.Week(1, "Base", List.of(new PlanContent.Task("Revisar diff", "REVIEW", 60))),
                new PlanContent.Week(2, "Testes", List.of()));
        var content = new PlanContent("", new PlanContent.AnchorProject("", "", List.of(), List.of()),
                List.of(new PlanContent.Quarter(1, "T1", "", List.of(), weeks)), List.of());
        return new PlanStore.StoredPlan(UUID.randomUUID(), 1, "READY", "Primeira versão", UUID.randomUUID(), START, content, null, Instant.now(), null);
    }
}
