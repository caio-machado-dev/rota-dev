package dev.rotadev.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.application.AiProperties;
import tools.jackson.databind.json.JsonMapper;

/** O pedido e a resposta do OpenRouter, sem rede: o {@link MockRestServiceServer} faz o papel da API. */
class OpenRouterLlmGatewayTest {

    private static final String SCHEMA = """
            {"type": "object", "properties": {"answer": {"type": "string"}}, "required": ["answer"], "additionalProperties": false}
            """;
    private static final String MODEL = "nvidia/nemotron-3.5-super-vl:free";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private OpenRouterLlmGateway gateway(String key) {
        var props = new AiProperties("openrouter", "high", 16000, 2, Map.of(), new AiProperties.Anthropic("", "claude-opus-5-5"),
                new AiProperties.OpenRouter(key, MODEL, "https://openrouter.test/api/v1"));
        return new OpenRouterLlmGateway(props, JsonMapper.builder().build(), builder.baseUrl(props.openrouter().baseUrl()).build());
    }

    @Test
    void sendsStructuredOutputRequestAndReadsTheAnswer() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-or-teste"))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.required[0]").value("answer"))
                .andExpect(jsonPath("$.provider.require_parameters").value(true))
                .andRespond(withSuccess("""
                        {"id": "gen-1", "model": "nvidia/nemotron-3.5-super-vl:free",
                         "choices": [{"message": {"role": "assistant", "content": "{\\"answer\\": \\"oi\\"}"}, "finish_reason": "stop"}],
                         "usage": {"prompt_tokens": 1200, "completion_tokens": 300, "prompt_tokens_details": {"cached_tokens": 200}}}
                        """, MediaType.APPLICATION_JSON));

        var response = gateway("sk-or-teste").send("sistema", "{\"x\": 1}", SCHEMA);

        assertThat(response.text()).isEqualTo("{\"answer\": \"oi\"}");
        assertThat(response.stopReason()).isEqualTo("end_turn");
        assertThat(response.model()).isEqualTo(MODEL);
        assertThat(response.usage().inputTokens()).isEqualTo(1000);
        assertThat(response.usage().cacheReadTokens()).isEqualTo(200);
        assertThat(response.usage().outputTokens()).isEqualTo(300);
        server.verify();
    }

    @Test
    void truncatedAnswerBecomesMaxTokens() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withSuccess("""
                        {"choices": [{"message": {"content": "{\\"ans"}, "finish_reason": "length"}], "usage": {"prompt_tokens": 1, "completion_tokens": 1}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(gateway("sk").send("s", "{}", SCHEMA).stopReason()).isEqualTo("max_tokens");
    }

    /** Chave errada: falha definitiva, sem retry, com a mensagem do OpenRouter. */
    @Test
    void invalidKeyIsPermanentFailure() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\": {\"code\": 401, \"message\": \"No auth credentials found\"}}"));

        assertThatThrownBy(() -> gateway("sk-errada").send("s", "{}", SCHEMA))
                .isInstanceOfSatisfying(AiCallFailedException.class, e -> {
                    assertThat(e.retryable()).isFalse();
                    assertThat(e.getMessage()).contains("HTTP 401", "No auth credentials found");
                });
    }

    /** Limite de taxa do plano gratuito: erro comum, e o job tenta de novo mais tarde. */
    @Test
    void rateLimitIsRetryable() {
        server.expect(requestTo("https://openrouter.test/api/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("{\"error\": {\"code\": 429}}"));

        assertThatThrownBy(() -> gateway("sk").send("s", "{}", SCHEMA))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class)
                .isNotInstanceOf(AiCallFailedException.class);
    }

    @Test
    void missingKeyFailsBeforeAnyRequest() {
        assertThatThrownBy(() -> gateway("").send("s", "{}", SCHEMA))
                .isInstanceOfSatisfying(AiCallFailedException.class, e -> assertThat(e.retryable()).isFalse())
                .hasMessageContaining("OPENROUTER_API_KEY");
        server.verify();
    }

    @Test
    void finishReasonMapping() {
        assertThat(OpenRouterLlmGateway.stopReason("content_filter")).isEqualTo("refusal");
        assertThat(OpenRouterLlmGateway.stopReason(null)).isEqualTo("end_turn");
    }
}
