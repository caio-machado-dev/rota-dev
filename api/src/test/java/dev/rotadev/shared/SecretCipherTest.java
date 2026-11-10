package dev.rotadev.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class SecretCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("outra-chave-de-32-bytes-12345678".getBytes());

    private final SecretCipher cipher = new SecretCipher(KEY);

    @Test
    void roundTrips() {
        String stored = cipher.encrypt("gho_token-secreto");

        assertThat(stored).startsWith("v1:").doesNotContain("gho_token-secreto");
        assertThat(cipher.decrypt(stored)).isEqualTo("gho_token-secreto");
    }

    @Test
    void sameInputGivesDifferentCiphertext() {
        assertThat(cipher.encrypt("igual")).isNotEqualTo(cipher.encrypt("igual"));
    }

    @Test
    void detectsTampering() {
        byte[] bytes = Base64.getDecoder().decode(cipher.encrypt("gho_token").substring("v1:".length()));
        bytes[bytes.length - 1] ^= 1; // troca um bit da tag de autenticação
        String tampered = "v1:" + Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String stored = cipher.encrypt("gho_token");

        assertThatThrownBy(() -> new SecretCipher(OTHER_KEY).decrypt(stored)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsMissingOrShortKey() {
        assertThatThrownBy(() -> new SecretCipher("")).hasMessageContaining("ROTA_ENCRYPTION_KEY");
        assertThatThrownBy(() -> new SecretCipher(Base64.getEncoder().encodeToString(new byte[16])))
                .hasMessageContaining("32 bytes");
    }
}
