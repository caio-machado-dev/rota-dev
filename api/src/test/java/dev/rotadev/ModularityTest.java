package dev.rotadev;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Verifica as fronteiras entre os módulos (ADR 0002).
 *
 * <p>Este teste não sobe o Spring nem o banco: o Modulith só analisa as classes compiladas.
 * Por isso ele roda em milissegundos.
 */
class ModularityTest {

    // TODO(caio): implementar a verificação de módulos e remover o @Disabled.
    //
    // O que fazer:
    //   1. Crie um campo `ApplicationModules modules = ApplicationModules.of(RotaDevApplication.class);`
    //      (import org.springframework.modulith.core.ApplicationModules).
    //   2. Neste método, chame `modules.verify();`. Ele lança exceção se um módulo usar classe
    //      interna de outro ou se houver dependência circular.
    //   3. Bônus: crie um segundo teste que imprime `modules` no console (System.out.println)
    //      para ver como o Modulith enxerga o projeto.
    //
    // Critério de pronto: `./mvnw test -Dtest=ModularityTest` passa sem o @Disabled.
    // Hoje ainda não há módulos, então ele passa vazio; a partir da Fase 1 ele começa a proteger.
    // Referência: https://docs.spring.io/spring-modulith/reference/verification.html
    @Disabled("TODO(caio): exercício da Fase 0")
    @Test
    void verifiesModularStructure() {
    }
}
