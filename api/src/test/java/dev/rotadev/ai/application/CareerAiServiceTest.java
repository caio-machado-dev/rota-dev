package dev.rotadev.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.AiRequest;
import dev.rotadev.ai.domain.TokenUsage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.json.JsonMapper;

/**
 * Teste de unidade do serviço, sem Spring e sem rede: o {@link LlmGateway} devolve respostas
 * enfileiradas e o {@link AiCallLog} só guarda numa lista.
 */
class CareerAiServiceTest {

    private static final String SCHEMA = """
            {"type": "object", "properties": {"answer": {"type": "string"}}, "required": ["answer"], "additionalProperties": false}
            """;
    private static final TokenUsage USAGE = new TokenUsage(1_000, 500, 0, 0);

    private final Deque<LlmGateway.LlmResponse> responses = new ArrayDeque<>();
    private final List<AiCallLog.Entry> logged = new ArrayList<>();
    /** Registry em memória: o teste lê os contadores direto, sem Prometheus. */
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final AiProperties properties = new AiProperties("chave", "claude-opus-5-5", "high", 16000, 2,
            Map.of("claude-opus-5-5", new AiProperties.Price(new BigDecimal("4"), new BigDecimal("20"), new BigDecimal("0.2"), new BigDecimal("5"))));

    private final CareerAiService service = new CareerAiService(
            (name, version) -> new Prompt(name, version, "sistema", SCHEMA),
            (system, user, schema) -> responses.removeFirst(),
            entry -> {
                logged.add(entry);
                return UUID.randomUUID();
            },
            properties,
            JsonMapper.builder().build(),
            Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC),
            meters);

    private final AiRequest request = new AiRequest(UUID.randomUUID(), "diagnosis", 1, "{}");

    @Test
    void returnsValidatedJsonAndRecordsCost() {
        responses.add(new LlmGateway.LlmResponse("{\"answer\": \"ok\"}", "end_turn", "claude-opus-5-5", USAGE));

        var result = service.run(request);

        assertThat(result.json()).contains("ok");
        assertThat(logged).singleElement().satisfies(e -> {
            assertThat(e.status()).isEqualTo("SUCCEEDED");
            // 1000 × $4/M + 500 × $20/M = $0,004 + $0,01 = 14.000 micro-dólares
            assertThat(e.costUsdMicros()).isEqualTo(14_000L);
        });
        assertThat(meters.get("rota.ai.calls").tags("status", "SUCCEEDED", "prompt", "diagnosis").counter().count()).isEqualTo(1.0);
        assertThat(meters.get("rota.ai.cost").summary().totalAmount()).isEqualTo(14_000.0);
    }

    @Test
    void retriesOnceWhenOutputBreaksTheSchema() {
        responses.add(new LlmGateway.LlmResponse("{\"resposta\": 1}", "end_turn", "claude-opus-5-5", USAGE));
        responses.add(new LlmGateway.LlmResponse("{\"answer\": \"agora sim\"}", "end_turn", "claude-opus-5-5", USAGE));

        assertThat(service.run(request).json()).contains("agora sim");
        assertThat(logged).extracting(AiCallLog.Entry::status).containsExactly("INVALID_OUTPUT", "SUCCEEDED");
    }

    @Test
    void failsAfterMaxAttemptsWithoutFixingTheText() {
        responses.add(new LlmGateway.LlmResponse("não é json", "end_turn", "claude-opus-5-5", USAGE));
        responses.add(new LlmGateway.LlmResponse("{}", "end_turn", "claude-opus-5-5", USAGE));

        assertThatThrownBy(() -> service.run(request)).isInstanceOf(AiCallFailedException.class).hasMessageContaining("2 tentativas");
        assertThat(logged).hasSize(2);
    }

    @Test
    void refusalIsNotRetried() {
        responses.add(new LlmGateway.LlmResponse("", "refusal", "claude-opus-5-5", USAGE));

        assertThatThrownBy(() -> service.run(request)).isInstanceOf(AiCallFailedException.class).hasMessageContaining("refusal");
        assertThat(logged).extracting(AiCallLog.Entry::status).containsExactly("REFUSED");
    }

    @Test
    void unknownModelIsRecordedWithoutCost() {
        responses.add(new LlmGateway.LlmResponse("{\"answer\": \"ok\"}", "end_turn", "modelo-de-fallback", USAGE));

        service.run(request);

        assertThat(logged.getFirst().costUsdMicros()).isNull();
        assertThat(logged.getFirst().model()).isEqualTo("modelo-de-fallback");
    }
}
