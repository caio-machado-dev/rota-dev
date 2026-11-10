package dev.rotadev.shared;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Gera UUID versão 7 (RFC 9562): os primeiros 48 bits são o instante em milissegundos e o resto é
 * aleatório. Ordena por tempo, o que é bom para índice, e não revela contagem (ADR 0009).
 *
 * <p>As entidades JPA usam o gerador do Hibernate ({@code @UuidGenerator}); esta classe serve para
 * as tabelas gravadas com SQL direto (JdbcClient), como a de jobs.
 *
 * <p>O módulo {@code shared} guarda só utilitários sem regra de negócio, que qualquer módulo usa.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    public static UUID generate(Instant now) {
        long millis = now.toEpochMilli();
        long randomHigh = RANDOM.nextLong();
        long randomLow = RANDOM.nextLong();
        // 48 bits de tempo | versão 7 (4 bits) | 12 bits aleatórios
        long mostSignificant = (millis << 16) | 0x7000L | (randomHigh & 0x0FFFL);
        // variante 10 (2 bits) | 62 bits aleatórios
        long leastSignificant = (randomLow & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificant, leastSignificant);
    }
}
