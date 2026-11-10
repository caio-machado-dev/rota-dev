package dev.rotadev.readiness.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReadinessScoreTest {

    @Test
    void weightsTheFourParts() {
        // Habilidades 50%, portões 50%, estudo 100%, hábito 50%:
        // 40×0,5 + 25×0,5 + 20×1 + 15×0,5 = 20 + 12,5 + 20 + 7,5 = 60.
        var score = ReadinessScore.of(new ReadinessInputs(9, 18, 2, 4, 2400, 600, 10));

        assertThat(score).isEqualTo(new ReadinessScore(60, 50, 50, 100, 50));
    }

    @Test
    void overachievingDoesNotPassOneHundred() {
        var score = ReadinessScore.of(new ReadinessInputs(30, 18, 9, 4, 99_999, 600, 28));

        assertThat(score.score()).isEqualTo(100);
    }

    @Test
    void nothingToMeasureCountsAsZero() {
        assertThat(ReadinessScore.of(new ReadinessInputs(0, 0, 0, 0, 500, 0, 3)).score()).isEqualTo(2);
    }
}
