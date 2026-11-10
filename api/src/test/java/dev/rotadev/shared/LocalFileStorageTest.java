package dev.rotadev.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    /** {@code @TempDir}: o JUnit cria uma pasta temporária e apaga no fim, como o {@code tmp_path} do pytest. */
    @TempDir
    Path base;

    @Test
    void savesUnderUserFolderAndReadsBack() throws IOException {
        var storage = new LocalFileStorage(base.toString());
        UUID user = UUID.randomUUID();

        String key = storage.save(user, new ByteArrayInputStream("conteúdo".getBytes()));

        assertThat(key).startsWith(user + "/");
        try (var in = storage.open(key)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("conteúdo");
        }
    }

    @Test
    void deleteAllForRemovesOnlyThatUser() throws IOException {
        var storage = new LocalFileStorage(base.toString());
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        storage.save(alice, new ByteArrayInputStream(new byte[] {1}));
        String bobKey = storage.save(bob, new ByteArrayInputStream(new byte[] {2}));

        storage.deleteAllFor(alice);

        assertThat(Files.exists(base.resolve(alice.toString()))).isFalse();
        assertThat(Files.exists(base.resolve(bobKey))).isTrue();
    }

    @Test
    void refusesKeysOutsideBaseFolder() {
        var storage = new LocalFileStorage(base.toString());

        assertThatThrownBy(() -> storage.open("../fora.txt")).isInstanceOf(IllegalArgumentException.class);
    }
}
