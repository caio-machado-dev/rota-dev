package dev.rotadev;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API.
 *
 * <p>{@code @SpringBootApplication} junta três coisas:
 * <ul>
 *   <li><b>component scan</b>: procura classes anotadas ({@code @Service}, {@code @RestController}...)
 *       neste pacote e nos subpacotes. Por isso esta classe fica na raiz, {@code dev.rotadev}.</li>
 *   <li><b>auto-configuration</b>: olha as dependências do pom e configura sozinho o que der
 *       (achou o driver do Postgres e o JPA? cria o pool de conexões e o EntityManager).</li>
 *   <li><b>configuration</b>: esta classe também pode declarar beans com {@code @Bean}.</li>
 * </ul>
 *
 * <p>Para o Spring Modulith, cada pacote direto abaixo deste ({@code dev.rotadev.identity},
 * {@code dev.rotadev.sources}...) é um módulo (ADR 0002).
 */
@SpringBootApplication
public class RotaDevApplication {

    public static void main(String[] args) {
        SpringApplication.run(RotaDevApplication.class, args);
    }
}
