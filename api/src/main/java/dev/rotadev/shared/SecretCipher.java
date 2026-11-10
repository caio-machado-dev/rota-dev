package dev.rotadev.shared;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cifra segredos (tokens OAuth) antes de irem para o banco (ADR 0008).
 *
 * <p>AES-256 em modo GCM: além de esconder o conteúdo, o GCM detecta adulteração (se um byte do
 * texto cifrado mudar, a decifragem falha). Cada valor ganha um IV (número usado uma vez) aleatório
 * de 12 bytes, então cifrar o mesmo token duas vezes dá resultados diferentes.
 *
 * <p>Formato gravado: {@code v1:<base64(iv + texto cifrado + tag)>}. O prefixo de versão permite
 * trocar a chave no futuro: valores novos saem com {@code v2:}, os antigos continuam legíveis com a
 * chave {@code v1} e são recifrados aos poucos.
 */
@Component
public class SecretCipher {

    private static final String VERSION_PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(@Value("${rota.encryption.key:}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "ROTA_ENCRYPTION_KEY não definida. Gere uma com: openssl rand -base64 32 (veja docs/desenvolvimento.md)");
        }
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        if (raw.length != 32) {
            throw new IllegalStateException("ROTA_ENCRYPTION_KEY deve ter 32 bytes (256 bits) em base64, tem " + raw.length);
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return VERSION_PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("falha ao cifrar", e);
        }
    }

    public String decrypt(String stored) {
        if (!stored.startsWith(VERSION_PREFIX)) {
            throw new IllegalArgumentException("versão de cifra desconhecida");
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(VERSION_PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Chave errada ou valor adulterado. A mensagem não inclui o valor, que é segredo.
            throw new IllegalStateException("falha ao decifrar", e);
        }
    }
}
