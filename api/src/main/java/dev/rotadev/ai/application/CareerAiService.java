package dev.rotadev.ai.application;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.networknt.schema.Error;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.AiRequest;
import dev.rotadev.ai.AiResult;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.ai.domain.TokenUsage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Implementa o {@link CareerAiClient} com as regras do ADR 0007:
 * <ol>
 *   <li>carrega o prompt versionado e o schema dele;</li>
 *   <li>chama o modelo pedindo saída estruturada naquele schema;</li>
 *   <li>valida a resposta contra o schema aqui também, sem confiar só no provedor;</li>
 *   <li>se o JSON vier inválido, tenta de novo até {@code rota.ai.max-attempts} vezes. Nunca
 *       "conserta" o texto na mão: ou a saída valida, ou a chamada falha;</li>
 *   <li>grava cada tentativa em {@code ai_calls}, com tokens, custo e latência.</li>
 * </ol>
 * Recusa e resposta cortada ({@code refusal}, {@code max_tokens}) não são tentadas de novo:
 * repetir a mesma pergunta daria o mesmo resultado e só gastaria tokens.
 */
@Service
@EnableConfigurationProperties(AiProperties.class)
public class CareerAiService implements CareerAiClient {

    private static final Logger log = LoggerFactory.getLogger(CareerAiService.class);

    private final PromptCatalog prompts;
    private final LlmGateway llm;
    private final AiCallLog calls;
    private final AiProperties properties;
    private final JsonMapper json;
    private final Clock clock;
    private final MeterRegistry meters;
    private final SchemaRegistry schemas = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    public CareerAiService(PromptCatalog prompts, LlmGateway llm, AiCallLog calls, AiProperties properties, JsonMapper json, Clock clock,
            MeterRegistry meters) {
        this.prompts = prompts;
        this.llm = llm;
        this.calls = calls;
        this.properties = properties;
        this.json = json;
        this.clock = clock;
        this.meters = meters;
    }

    @Override
    public AiResult run(AiRequest request) {
        Prompt prompt = prompts.get(request.prompt(), request.version());
        Schema schema = schemas.getSchema(prompt.schemaJson());
        int maxAttempts = Math.max(1, properties.maxAttempts());

        String lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long started = System.nanoTime();
            LlmGateway.LlmResponse response;
            try {
                response = llm.send(prompt.system(), request.inputJson(), prompt.schemaJson());
            } catch (AiCallFailedException e) {
                // O gateway já sabe que tentar de novo não adianta (chave ausente ou inválida, pedido
                // recusado pelo provedor): registra e falha na hora, sem o job ficar horas em retry.
                record(request, properties.activeModel(), attempt, "ERROR", TokenUsage.NONE, started, e.getMessage());
                throw e;
            } catch (RuntimeException e) {
                // Erro de rede ou da API: o SDK já tentou de novo por conta própria. Registramos e
                // deixamos o job que chamou decidir (ele tem retry com backoff, ADR 0006).
                record(request, properties.activeModel(), attempt, "ERROR", TokenUsage.NONE, started, e.getMessage());
                throw new AiCallFailedException("falha ao chamar a IA", e);
            }

            String status = switch (response.stopReason()) {
                case "refusal" -> "REFUSED";
                case "max_tokens" -> "TRUNCATED";
                default -> null;
            };
            if (status != null) {
                record(request, response.model(), attempt, status, response.usage(), started, response.stopReason());
                throw new AiCallFailedException("a IA não completou a resposta (" + response.stopReason() + ")");
            }

            List<String> errors = validate(schema, response.text());
            if (errors.isEmpty()) {
                UUID callId = record(request, response.model(), attempt, "SUCCEEDED", response.usage(), started, null);
                return new AiResult(response.text(), response.model(), callId);
            }
            lastError = String.join("; ", errors);
            log.warn("Saída inválida do prompt {} v{} (tentativa {}): {}", prompt.name(), prompt.version(), attempt, lastError);
            record(request, response.model(), attempt, "INVALID_OUTPUT", response.usage(), started, lastError);
        }
        throw new AiCallFailedException("a IA devolveu JSON inválido em " + maxAttempts + " tentativas: " + lastError);
    }

    private List<String> validate(Schema schema, String text) {
        JsonNode node;
        try {
            node = json.readTree(text);
        } catch (JacksonException e) {
            return List.of("não é JSON: " + e.getOriginalMessage());
        }
        return schema.validate(node).stream().map(Error::getMessage).toList();
    }

    private UUID record(AiRequest request, String model, int attempt, String status, TokenUsage usage, long startedNanos, String error) {
        var price = properties.pricing() == null ? null : properties.pricing().get(model);
        // Modelos ":free" do OpenRouter não cobram nada; os demais sem preço na tabela ficam sem custo (nulo).
        Long cost = price != null ? Long.valueOf(price.toPricing().costUsdMicros(usage)) : model.endsWith(":free") ? Long.valueOf(0L) : null;
        long latencyMs = (System.nanoTime() - startedNanos) / 1_000_000;
        observe(request.prompt(), model, status, cost, latencyMs);
        return calls.record(new AiCallLog.Entry(request.userId(), request.prompt(), request.version(), model, attempt, status,
                usage, cost, latencyMs, error, clock.instant()));
    }

    /**
     * Métricas para o Prometheus ({@code /actuator/prometheus}). O Micrometer é o "statsd" do Spring:
     * o código conta e mede, e o registry decide o formato. Tags com poucos valores possíveis (prompt,
     * modelo, status), nunca o usuário: uma tag por usuário criaria uma série por pessoa.
     */
    private void observe(String prompt, String model, String status, Long costMicros, long latencyMs) {
        Counter.builder("rota.ai.calls").description("Chamadas à IA, por tentativa")
                .tags("prompt", prompt, "model", model, "status", status)
                .register(meters).increment();
        Timer.builder("rota.ai.latency").tags("prompt", prompt, "model", model)
                .register(meters).record(Duration.ofMillis(latencyMs));
        if (costMicros != null) {
            DistributionSummary.builder("rota.ai.cost").baseUnit("usd_micros").description("Custo estimado por chamada")
                    .tags("prompt", prompt, "model", model)
                    .register(meters).record(costMicros);
        }
    }
}
