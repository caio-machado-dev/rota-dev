package dev.rotadev;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * Guarda do contrato (ADR 0010): o {@code api/openapi.json} versionado tem que ser igual ao que a
 * API gera agora. Mudou um controller ou um record? Regenere o arquivo com
 * {@code ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true} e comite junto. O front gera os
 * tipos TypeScript a partir desse arquivo ({@code pnpm api:types}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    private static final Path CONTRACT = Path.of("openapi.json");

    private final JsonMapper json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    @Autowired
    MockMvcTester mvc;

    @Test
    void committedContractMatchesApi() throws Exception {
        String body = mvc.get().uri("/v3/api-docs").exchange().getResponse().getContentAsString();
        JsonNode current = json.readTree(body);

        if (Boolean.getBoolean("openapi.write")) {
            Files.writeString(CONTRACT, json.writeValueAsString(current) + "\n");
        }

        assertThat(Files.exists(CONTRACT))
                .as("api/openapi.json não existe. Rode com -Dopenapi.write=true para gerar.")
                .isTrue();
        assertThat(json.readTree(Files.readString(CONTRACT)))
                .as("O contrato mudou. Rode ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true e comite api/openapi.json.")
                .isEqualTo(current);
    }
}
