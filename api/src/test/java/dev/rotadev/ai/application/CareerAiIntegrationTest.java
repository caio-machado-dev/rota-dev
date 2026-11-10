package dev.rotadev.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import dev.rotadev.TestcontainersConfiguration;
import dev.rotadev.ai.AiRequest;
import dev.rotadev.ai.CareerAiClient;
import dev.rotadev.ai.domain.TokenUsage;
import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;

/** Tudo de verdade (prompt no classpath, Postgres, configuração), menos a chamada ao provedor. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CareerAiIntegrationTest {

    @MockitoBean
    LlmGateway llm;

    @Autowired
    CareerAiClient ai;

    @Autowired
    GithubSignIn signIn;

    @Autowired
    JdbcClient jdbc;

    @Test
    void loadsPromptFromClasspathAndRecordsTheCall() {
        UUID userId = signIn.signIn(new GithubProfile(10001L, "ai-user", null, null, null)).id();
        when(llm.send(anyString(), anyString(), anyString())).thenReturn(
                new LlmGateway.LlmResponse("{\"answer\": \"oi\"}", "end_turn", "claude-opus-5-5", new TokenUsage(2_000, 1_000, 0, 0)));

        var result = ai.run(new AiRequest(userId, "test-echo", 1, "{\"pergunta\": \"tudo bem?\"}"));

        var row = jdbc.sql("SELECT prompt_name, prompt_version, status, cost_usd_micros FROM ai_calls WHERE id = :id")
                .param("id", result.callId()).query().singleRow();
        assertThat(row).containsEntry("prompt_name", "test-echo").containsEntry("status", "SUCCEEDED")
                .containsEntry("cost_usd_micros", 28_000L);
    }
}
