package dev.rotadev.ai.infrastructure;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.application.AiProperties;
import dev.rotadev.ai.application.LlmGateway;
import dev.rotadev.ai.domain.TokenUsage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link LlmGateway} pelo OpenRouter (ADR 0016), que dá acesso a vários modelos, inclusive gratuitos,
 * pela mesma API de chat completions. Ativo com {@code rota.ai.provider=openrouter}, o padrão.
 *
 * <ul>
 *   <li><b>Saída estruturada:</b> {@code response_format} com o JSON Schema do prompt e {@code strict: true}.
 *       {@code provider.require_parameters: true} faz o OpenRouter só escolher provedores que aceitam
 *       esse parâmetro. Mesmo assim o {@code CareerAiService} valida de novo, porque a documentação
 *       avisa que a garantia varia por provedor.</li>
 *   <li><b>Erros:</b> chave ausente ou recusada (400, 401, 402, 403) falha de vez, sem retry. Limite
 *       de taxa (429) e falha do provedor (5xx) sobem como erro comum, e o job tenta de novo mais tarde.</li>
 *   <li><b>Tempo:</b> modelos gratuitos podem demorar minutos; o tempo de leitura é de 5 minutos.</li>
 * </ul>
 *
 * <p>Usa HTTP direto pelo {@link RestClient}: o OpenRouter não tem SDK Java oficial, e a API é pequena.
 */
@Component
@ConditionalOnProperty(name = "rota.ai.provider", havingValue = "openrouter", matchIfMissing = true)
class OpenRouterLlmGateway implements LlmGateway {

    /** Status em que tentar de novo não resolve: pedido inválido, chave errada, sem crédito, proibido. */
    private static final Set<Integer> PERMANENT = Set.of(400, 401, 402, 403);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final AiProperties properties;
    private final JsonMapper json;
    private final RestClient http;

    /**
     * Construtor que o Spring usa. Com dois construtores na classe, o {@code @Autowired} diz qual é o dele.
     * Monta o cliente HTTP com os tempos de conexão e de leitura e repassa para o outro.
     */
    @Autowired
    OpenRouterLlmGateway(AiProperties properties, JsonMapper json, RestClient.Builder builder) {
        this(properties, json, builder
                .requestFactory(withTimeouts())
                .baseUrl(properties.openrouter().baseUrl())
                // Identifica o app no OpenRouter (opcional, recomendado pela documentação).
                .defaultHeader("X-Title", "rota-dev")
                .build());
    }

    /** Recebe o cliente pronto. Os testes usam este, com um cliente ligado ao {@code MockRestServiceServer}. */
    OpenRouterLlmGateway(AiProperties properties, JsonMapper json, RestClient http) {
        this.properties = properties;
        this.json = json;
        this.http = http;
    }

    private static JdkClientHttpRequestFactory withTimeouts() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofMinutes(5));
        return factory;
    }

    @Override
    public LlmResponse send(String system, String userContent, String schemaJson) {
        String apiKey = properties.openrouter().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiCallFailedException("OPENROUTER_API_KEY não configurada (veja docs/desenvolvimento.md)");
        }

        ChatResponse response = http.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body(system, userContent, schemaJson))
                .retrieve()
                .onStatus(status -> PERMANENT.contains(status.value()), (request, resp) -> {
                    throw new AiCallFailedException("o OpenRouter recusou o pedido (" + statusText(resp.getStatusCode()) + "): "
                            + new String(resp.getBody().readAllBytes()).replaceAll("\\s+", " "));
                })
                .body(ChatResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            // Às vezes o provedor responde 200 sem conteúdo. Vale tentar de novo.
            throw new IllegalStateException("resposta do OpenRouter sem choices");
        }
        Choice choice = response.choices().getFirst();
        Usage usage = response.usage() == null ? new Usage(0, 0, null) : response.usage();
        long cached = usage.promptTokensDetails() == null ? 0 : usage.promptTokensDetails().cachedTokens();
        return new LlmResponse(
                choice.message() == null || choice.message().content() == null ? "" : choice.message().content(),
                stopReason(choice.finishReason()),
                response.model() == null ? properties.openrouter().model() : response.model(),
                new TokenUsage(usage.promptTokens() - cached, usage.completionTokens(), cached, 0));
    }

    /** Monta o corpo do pedido. Separado de {@link #send} para o teste conferir sem rede. */
    Map<String, Object> body(String system, String userContent, String schemaJson) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.openrouter().model());
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", userContent)));
        body.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of("name", "output", "strict", true, "schema", json.readValue(schemaJson, MAP))));
        body.put("provider", Map.of("require_parameters", true));
        body.put("max_tokens", properties.maxTokens());
        return body;
    }

    /** Traduz o {@code finish_reason} do OpenRouter para o vocabulário que o {@code CareerAiService} entende. */
    static String stopReason(String finishReason) {
        if (finishReason == null) {
            return "end_turn";
        }
        return switch (finishReason) {
            case "length" -> "max_tokens";
            case "content_filter" -> "refusal";
            default -> "end_turn";
        };
    }

    private static String statusText(HttpStatusCode status) {
        return "HTTP " + status.value();
    }

    record ChatResponse(String model, List<Choice> choices, Usage usage) {
    }

    record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    record Message(String content) {
    }

    record Usage(
            @JsonProperty("prompt_tokens") long promptTokens,
            @JsonProperty("completion_tokens") long completionTokens,
            @JsonProperty("prompt_tokens_details") PromptDetails promptTokensDetails) {
    }

    record PromptDetails(@JsonProperty("cached_tokens") long cachedTokens) {
    }
}
