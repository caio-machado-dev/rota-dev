package dev.rotadev.identity.application;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import dev.rotadev.identity.Tier;
import dev.rotadev.identity.TierLimits;

/**
 * {@code rota.limits.*} do application.yml. Ficam na configuração para mudar sem mexer em código.
 * Um nível que não aparece na configuração tem limite 0.
 */
@ConfigurationProperties("rota.limits")
public record LimitsProperties(Map<Tier, Integer> aiGenerationsPerMonth, Map<Tier, Integer> uploads) implements TierLimits {

    @Override
    public int aiGenerationsPerMonth(Tier tier) {
        return aiGenerationsPerMonth.getOrDefault(tier, 0);
    }

    @Override
    public int uploads(Tier tier) {
        return uploads.getOrDefault(tier, 0);
    }

    /** Registra o record como bean: o Spring preenche os campos a partir do application.yml. */
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(LimitsProperties.class)
    static class Registration {
    }
}
