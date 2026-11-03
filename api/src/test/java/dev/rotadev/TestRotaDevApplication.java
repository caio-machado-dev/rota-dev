package dev.rotadev;

import org.springframework.boot.SpringApplication;

/**
 * Roda a API localmente com um Postgres do Testcontainers, sem precisar de docker compose.
 * Na IDE: rode o main desta classe. No terminal: {@code ./mvnw spring-boot:test-run}.
 */
public class TestRotaDevApplication {

    public static void main(String[] args) {
        SpringApplication.from(RotaDevApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
