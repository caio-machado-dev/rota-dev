package dev.rotadev.ai.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.OutputConfig;

import dev.rotadev.ai.AiCallFailedException;
import dev.rotadev.ai.application.AiProperties;
import tools.jackson.databind.json.JsonMapper;

/** Confere a requisição montada para o SDK, sem rede. */
class AnthropicLlmGatewayTest {

    private static final String SCHEMA = """
            {"type": "object", "properties": {"answer": {"type": "string"}}, "required": ["answer"], "additionalProperties": false}
            """;

    @Test
    void buildsStructuredOutputRequestWithEffortAndDefaultFallback() {
        var gateway = new AnthropicLlmGateway(props("k"), JsonMapper.builder().build());

        var params = gateway.params("sistema", "{\"x\": 1}", SCHEMA);

        assertThat(params.model().toString()).isEqualTo("claude-opus-5-5");
        assertThat(params.maxTokens()).isEqualTo(16000L);
        var output = params.outputConfig().orElseThrow();
        assertThat(output.effort()).contains(OutputConfig.Effort.HIGH);
        assertThat(output.format().orElseThrow().schema()._additionalProperties()).containsKeys("type", "properties", "required");
        assertThat(params._additionalBodyProperties()).containsEntry("fallbacks", JsonValue.from("default"));
        assertThat(params._additionalHeaders().values("anthropic-beta")).containsExactly("server-side-fallback-2026-07-01");
    }

    @Test
    void missingApiKeyFailsOnlyWhenCalled() {
        var gateway = new AnthropicLlmGateway(props(""), JsonMapper.builder().build());

        assertThatThrownBy(() -> gateway.send("s", "{}", SCHEMA)).isInstanceOf(AiCallFailedException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }

    private static AiProperties props(String key) {
        return new AiProperties("anthropic", "high", 16000, 2, Map.of(), new AiProperties.Anthropic(key, "claude-opus-5-5"),
                new AiProperties.OpenRouter("", "x:free", "https://openrouter.test"));
    }
}
