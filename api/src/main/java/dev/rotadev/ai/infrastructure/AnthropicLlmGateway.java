package dev.rotadev.ai.infrastructure;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.TextBlock;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.application.AiProperties;
import dev.rotadev.ai.application.LlmGateway;
import dev.rotadev.ai.domain.TokenUsage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link LlmGateway} com o SDK oficial da Anthropic ({@code com.anthropic:anthropic-java}).
 * Ativo com {@code rota.ai.provider=anthropic} (ADR 0016).
 *
 * <ul>
 *   <li><b>Saída estruturada:</b> {@code output_config.format} com o JSON Schema do prompt. O modelo
 *       é obrigado a responder nesse formato; o {@code CareerAiService} valida de novo mesmo assim.</li>
 *   <li><b>Esforço:</b> {@code output_config.effort} (padrão {@code high} na configuração). No Claude
 *       Opus 5.5 o raciocínio está sempre ligado e o esforço é o único controle; sem ele, o padrão do
 *       modelo seria {@code medium}.</li>
 *   <li><b>Fallback em recusa:</b> {@code fallbacks: "default"} com o cabeçalho beta
 *       {@code server-side-fallback-2026-07-01}. Se os filtros de segurança recusarem a pergunta, a
 *       própria API refaz com o modelo indicado para aquele tipo de recusa. O SDK 3.3.0 ainda não tem
 *       método tipado para isso, então o campo vai como propriedade extra do corpo.</li>
 * </ul>
 *
 * <p>O cliente HTTP só é criado na primeira chamada: a API sobe e roda os testes sem
 * {@code ANTHROPIC_API_KEY}, e só o que usa IA falha, com mensagem clara.
 */
@Component
@ConditionalOnProperty(name = "rota.ai.provider", havingValue = "anthropic")
class AnthropicLlmGateway implements LlmGateway {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final AiProperties properties;
    private final JsonMapper json;
    private volatile AnthropicClient client;

    AnthropicLlmGateway(AiProperties properties, JsonMapper json) {
        this.properties = properties;
        this.json = json;
    }

    @Override
    public LlmResponse send(String system, String userContent, String schemaJson) {
        Message message = client().messages().create(params(system, userContent, schemaJson));

        String text = message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
        var usage = message.usage();
        return new LlmResponse(
                text,
                message.stopReason().map(Object::toString).orElse("end_turn"),
                message.model().toString(),
                new TokenUsage(usage.inputTokens(), usage.outputTokens(),
                        usage.cacheReadInputTokens().orElse(0L), usage.cacheCreationInputTokens().orElse(0L)));
    }

    /** Separado de {@link #send} para o teste conferir a requisição sem chamar a API. */
    MessageCreateParams params(String system, String userContent, String schemaJson) {
        var schema = JsonOutputFormat.Schema.builder();
        json.readValue(schemaJson, MAP).forEach((key, value) -> schema.putAdditionalProperty(key, JsonValue.from(value)));

        MessageCreateParams params = MessageCreateParams.builder()
                .model(properties.anthropic().model())
                .maxTokens(properties.maxTokens())
                .system(system)
                .addUserMessage(userContent)
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.of(properties.effort()))
                        .format(JsonOutputFormat.builder().schema(schema.build()).build())
                        .build())
                .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
                .build();
        return params;
    }

    private AnthropicClient client() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    if (properties.anthropic().apiKey() == null || properties.anthropic().apiKey().isBlank()) {
                        throw new AiCallFailedException("ANTHROPIC_API_KEY não configurada (veja docs/desenvolvimento.md)");
                    }
                    client = AnthropicOkHttpClient.builder().apiKey(properties.anthropic().apiKey()).build();
                }
            }
        }
        return client;
    }
}
