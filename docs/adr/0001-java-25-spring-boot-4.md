# 0001. Java 25 + Spring Boot 4.1

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
O pedido original era Java 21 + Spring Boot 3. A linha 3.x do Spring Boot (último minor: 3.5) perdeu o suporte open source em 30/06/2026, então não recebe mais correções de segurança gratuitas. O Java 25 é a versão LTS atual.

## Decisão
Java 25 (LTS) + Spring Boot 4.1.x, com Maven Wrapper (`./mvnw`). O `maven-enforcer-plugin` exige Java 25 no build.

## Alternativas descartadas
- **Boot 3.5 + Java 21:** sem suporte OSS. Começar um produto novo assim já nasce com dívida.
- **Gradle:** é bom, mas o Maven é mais comum em vagas Java e mais previsível para quem está aprendendo.

## Consequências
- A versão do Maven instalada na máquina deixa de importar: o wrapper baixa a versão certa.
- O Boot 4 traz itens para estudar: Jackson 3, Spring Security 7, Hibernate 7, starters separados por tecnologia (ex.: `spring-boot-starter-webmvc`) e null safety com JSpecify.
- Material de estudo antigo (Boot 2/3) às vezes não bate. Confira sempre a documentação da versão 4.
