package dev.rotadev.ai.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import dev.rotadev.ai.application.Prompt;
import dev.rotadev.ai.application.PromptCatalog;

/**
 * Lê {@code prompts/<nome>/v<N>.md} e {@code prompts/<nome>/v<N>.schema.json} do classpath
 * ({@code src/main/resources}). O schema fica por versão porque mudar o formato da saída também é
 * uma versão nova do prompt. Os arquivos não mudam com a aplicação rodando, então ficam em cache.
 */
@Component
class ClasspathPromptCatalog implements PromptCatalog {

    private final Map<String, Prompt> cache = new ConcurrentHashMap<>();

    @Override
    public Prompt get(String name, int version) {
        if (!name.matches("[a-z0-9-]+")) {
            throw new IllegalArgumentException("nome de prompt inválido: " + name);
        }
        return cache.computeIfAbsent(name + "/v" + version, key -> new Prompt(name, version,
                read("prompts/" + key + ".md"), read("prompts/" + key + ".schema.json")));
    }

    private static String read(String path) {
        var resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalArgumentException("prompt não encontrado: " + path);
        }
        try (var in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
