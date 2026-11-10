package dev.rotadev.ai.infrastructure;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import dev.rotadev.ai.application.AiCallLog;
import dev.rotadev.shared.UuidV7;

@Repository
class JdbcAiCallLog implements AiCallLog {

    private final JdbcClient jdbc;

    JdbcAiCallLog(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UUID record(Entry e) {
        UUID id = UuidV7.generate(e.createdAt());
        jdbc.sql("""
                INSERT INTO ai_calls (id, user_id, prompt_name, prompt_version, model, attempt, status,
                                      input_tokens, output_tokens, cache_read_tokens, cache_write_tokens,
                                      cost_usd_micros, latency_ms, error, created_at)
                VALUES (:id, :userId, :prompt, :version, :model, :attempt, :status,
                        :input, :output, :cacheRead, :cacheWrite, :cost, :latency, :error, :createdAt)
                """)
                .param("id", id)
                .param("userId", e.userId())
                .param("prompt", e.promptName())
                .param("version", e.promptVersion())
                .param("model", e.model())
                .param("attempt", e.attempt())
                .param("status", e.status())
                .param("input", e.usage().inputTokens())
                .param("output", e.usage().outputTokens())
                .param("cacheRead", e.usage().cacheReadTokens())
                .param("cacheWrite", e.usage().cacheWriteTokens())
                .param("cost", e.costUsdMicros())
                .param("latency", e.latencyMs())
                .param("error", e.error() == null ? null : e.error().substring(0, Math.min(e.error().length(), 2000)))
                .param("createdAt", Timestamp.from(e.createdAt()))
                .update();
        return id;
    }
}
