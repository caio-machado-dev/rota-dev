package dev.rotadev.ai.application;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import dev.rotadev.ai.AiQuota;
import dev.rotadev.ai.AiQuotaExceededException;
import dev.rotadev.identity.Tier;
import dev.rotadev.identity.TierLimits;
import dev.rotadev.identity.UserTiers;

/**
 * Conta as gerações do mês em {@code ai_calls} (só as que deram certo: uma resposta inválida que foi
 * tentada de novo não pesa duas vezes). O mês é o do calendário em UTC, igual à fatura do provedor.
 */
@Service
class MonthlyAiQuota implements AiQuota {

    private final JdbcClient jdbc;
    private final UserTiers tiers;
    private final TierLimits limits;
    private final Clock clock;

    MonthlyAiQuota(JdbcClient jdbc, UserTiers tiers, TierLimits limits, Clock clock) {
        this.jdbc = jdbc;
        this.tiers = tiers;
        this.limits = limits;
        this.clock = clock;
    }

    @Override
    public void requireAvailable(UUID userId) {
        Tier tier = tiers.tierOf(userId);
        int limit = limits.aiGenerationsPerMonth(tier);
        Instant monthStart = YearMonth.now(clock.withZone(ZoneOffset.UTC)).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long used = jdbc.sql("SELECT count(*) FROM ai_calls WHERE user_id = :userId AND status = 'SUCCEEDED' AND created_at >= :from")
                .param("userId", userId).param("from", Timestamp.from(monthStart))
                .query(Long.class).single();
        if (used >= limit) {
            throw new AiQuotaExceededException("Você usou as " + limit + " gerações de IA do mês no plano " + tier
                    + ". O limite renova no dia 1º.");
        }
    }
}
