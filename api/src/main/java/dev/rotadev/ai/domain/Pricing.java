package dev.rotadev.ai.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Preço de um modelo em dólares por milhão de tokens (a unidade da tabela da Anthropic).
 *
 * <p>{@link BigDecimal} na conta e {@code long} no resultado: {@code double} acumula erro de
 * arredondamento, e dinheiro no projeto é sempre inteiro (ADR 0009).
 */
public record Pricing(BigDecimal inputPerMtok, BigDecimal outputPerMtok, BigDecimal cacheReadPerMtok, BigDecimal cacheWritePerMtok) {

    /** Custo estimado em milionésimos de dólar (1 USD = 1.000.000). */
    public long costUsdMicros(TokenUsage usage) {
        // tokens × (USD por milhão de tokens) = USD × 10^6 = micro-USD. A divisão por um milhão
        // da tabela cancela com a multiplicação para micro-dólares.
        BigDecimal micros = BigDecimal.valueOf(usage.inputTokens()).multiply(inputPerMtok)
                .add(BigDecimal.valueOf(usage.outputTokens()).multiply(outputPerMtok))
                .add(BigDecimal.valueOf(usage.cacheReadTokens()).multiply(cacheReadPerMtok))
                .add(BigDecimal.valueOf(usage.cacheWriteTokens()).multiply(cacheWritePerMtok));
        return micros.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
