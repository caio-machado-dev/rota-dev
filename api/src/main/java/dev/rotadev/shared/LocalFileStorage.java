package dev.rotadev.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@link FileStorage} numa pasta do disco: {@code <base>/<userId>/<uuid>}.
 *
 * <p>O nome original do arquivo nunca vira caminho no disco (fica só no banco). Assim um nome como
 * {@code ../../etc/passwd} não tem como escapar da pasta base.
 */
@Component
class LocalFileStorage implements FileStorage {

    private final Path base;

    LocalFileStorage(@Value("${rota.storage.path:./data/uploads}") String basePath) {
        this.base = Path.of(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new UncheckedIOException("não consegui criar a pasta de uploads " + base, e);
        }
    }

    @Override
    public String save(UUID userId, InputStream content) throws IOException {
        String key = userId + "/" + UuidV7.generate(Instant.now());
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return key;
    }

    @Override
    public InputStream open(String key) throws IOException {
        return Files.newInputStream(resolve(key));
    }

    @Override
    public void delete(String key) throws IOException {
        Files.deleteIfExists(resolve(key));
    }

    @Override
    public void deleteAllFor(UUID userId) throws IOException {
        Path dir = resolve(userId.toString());
        if (!Files.exists(dir)) {
            return;
        }
        // Apaga de dentro para fora: arquivos primeiro, a pasta por último.
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    /** Defesa extra: qualquer chave que saia da pasta base é recusada. */
    private Path resolve(String key) {
        Path path = base.resolve(key).normalize();
        if (!path.startsWith(base)) {
            throw new IllegalArgumentException("chave fora da pasta de uploads");
        }
        return path;
    }
}
