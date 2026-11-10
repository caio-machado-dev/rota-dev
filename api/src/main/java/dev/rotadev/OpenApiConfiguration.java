package dev.rotadev;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Cabeçalho do contrato OpenAPI (ADR 0010). O resto (rotas, records) o springdoc lê dos controllers.
 *
 * <p>O servidor é {@code /} porque o front sempre chama a API na própria origem (ADR 0005).
 * Isso também deixa o {@code openapi.json} igual em qualquer máquina.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

    @Bean
    OpenAPI openApi(@Value("${rota.brand.name}") String brandName) {
        return new OpenAPI()
                .info(new Info().title(brandName + " API").version("v1"))
                .servers(List.of(new Server().url("/")));
    }
}
