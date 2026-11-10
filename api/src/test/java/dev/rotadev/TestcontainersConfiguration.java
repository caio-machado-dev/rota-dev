package dev.rotadev;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Sobe um Postgres real em Docker para os testes de integração.
 *
 * <p>{@code @ServiceConnection} faz o Spring ler host, porta, usuário e senha direto do
 * container, sem precisar configurar {@code spring.datasource.*} à mão.
 *
 * <p>A imagem é {@code postgres:17}, a mesma da produção: testar contra outra versão
 * (ou contra H2 em memória) esconde diferenças de SQL.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    }
}
