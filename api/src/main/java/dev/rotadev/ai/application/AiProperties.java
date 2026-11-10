package dev.rotadev.ai.application;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import dev.rotadev.ai.domain.Pricing;

/**
 * Configuração {@code rota.ai.*} do application.yml, ligada a um record.
 *
 * <p>{@code @ConfigurationProperties} é o jeito tipado de ler configuração no Spring, parecido com
 * um {@code BaseSettings} do pydantic-settings: o Boot preenche os campos pelos nomes e falha na
 * subida se um tipo não bater.
 *
 * @param pricing preço por modelo, em USD por milhão de tokens; a chave é o id do modelo
 */
@ConfigurationProperties("rota.ai")
public record AiProperties(String apiKey, String model, String effort, long maxTokens, int maxAttempts, Map<String, Price> pricing) {

    public record Price(BigDecimal input, BigDecimal output, BigDecimal cacheRead, BigDecimal cacheWrite) {

        Pricing toPricing() {
            return new Pricing(input, output, cacheRead, cacheWrite);
        }
    }
}
