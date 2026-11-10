package dev.rotadev;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio único da aplicação (ADR 0009).
 *
 * <p>O código pede um {@link Clock} no construtor em vez de chamar {@code Instant.now()}.
 * Em produção ele é o relógio do sistema em UTC; nos testes, um {@code Clock.fixed(...)}.
 * É o mesmo efeito do {@code freezegun} no Python, só que sem mágica: o relógio é uma dependência.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
