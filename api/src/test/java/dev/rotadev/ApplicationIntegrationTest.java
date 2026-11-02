package dev.rotadev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Teste de fumaça: a aplicação inteira sobe contra um Postgres real?
 *
 * <ul>
 *   <li>{@code @SpringBootTest} sobe o contexto completo, como em produção.</li>
 *   <li>{@code @Import(TestcontainersConfiguration.class)} troca o banco pelo container de teste.</li>
 *   <li>{@code @AutoConfigureMockMvc} permite fazer requisições HTTP sem abrir porta de rede.</li>
 * </ul>
 *
 * <p>Os campos usam {@code @Autowired} porque o JUnit é quem cria a classe de teste.
 * No código de produção, a injeção é sempre pelo construtor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApplicationIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void healthEndpointReportsUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void flywayAppliedBaselineMigration() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success",
                Integer.class);

        assertThat(applied).isEqualTo(1);
    }
}
