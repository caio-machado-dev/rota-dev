package dev.rotadev;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import dev.rotadev.identity.application.GithubSignIn;
import dev.rotadev.identity.domain.GithubProfile;
import dev.rotadev.jobs.JobQueue;
import dev.rotadev.jobs.application.JobWorker;

/**
 * /actuator/prometheus responde sem login e traz as métricas próprias com a tag da aplicação.
 *
 * <p>{@code @AutoConfigureMetrics}: por padrão o Spring Boot desliga a exportação de métricas nos testes
 * (para não mandar dados de teste a um coletor de verdade). Aqui queremos o endpoint ligado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMetrics
@Import(TestcontainersConfiguration.class)
class ObservabilityTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JobQueue queue;

    @Autowired
    JobWorker worker;

    @Autowired
    GithubSignIn signIn;

    @Test
    void prometheusExposesJobMetrics() {
        var user = signIn.signIn(new GithubProfile(19001L, "obs-user", null, null, null));
        queue.enqueue(user.id(), "tipo.inexistente", "{}", "obs:" + user.id());
        worker.runOnce(10);

        assertThat(mvc.get().uri("/actuator/prometheus")).hasStatusOk().body().asString()
                .contains("rota_jobs_executed_total{")
                .contains("outcome=\"failed\"")
                .contains("application=\"rota-dev-api\"")
                .contains("jvm_memory_used_bytes");
    }
}
