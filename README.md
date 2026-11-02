# Rota Dev

Plataforma de plano de carreira para desenvolvedores. O dev conecta GitHub, LinkedIn e currículo, e recebe um plano personalizado e acompanhável, baseado em **evidência real** do que já construiu, e não em questionário genérico.

> Status: **Fase 0 (fundação)**. Ainda não há funcionalidade de produto, só a base técnica.

## Stack

| Camada | Tecnologia |
|---|---|
| API | Java 25, Spring Boot 4.1, Spring Data JPA, Flyway, Spring Modulith |
| Banco | PostgreSQL 17 |
| Web | Next.js 16 (App Router), TypeScript, Tailwind 4, shadcn/ui |
| Infra | Docker Compose (local e VPS), GitHub Actions |

O porquê de cada escolha está nos [ADRs](docs/adr/README.md).

## Estrutura

```
api/     Spring Boot. Cada pacote em dev.rotadev.* é um módulo (ADR 0002)
web/     Next.js
infra/   Docker Compose
docs/    ADRs e glossário do domínio
```

## Pré-requisitos (macOS, Apple Silicon)

```bash
brew install --cask temurin@25      # JDK 25 (ou outro JDK 25 de sua preferência)
brew install --cask docker          # Docker Desktop: abra o app uma vez depois de instalar
brew install node@24                # Node 24
corepack enable                     # habilita o pnpm na versão fixada em web/package.json
```

Confira com `java -version` (precisa ser 25), `docker info` e `pnpm -v`.
Maven não precisa ser instalado: o projeto usa o wrapper `./mvnw`.

## Como rodar

### Tudo no Docker (o jeito mais simples)

```bash
docker compose -f infra/compose.yaml up --build
```

- Web: http://localhost:3000
- API: http://localhost:8080/actuator/health

### Desenvolvimento (com hot reload)

```bash
# 1. Banco
docker compose -f infra/compose.yaml up postgres

# 2. API (outro terminal)
cd api && ./mvnw spring-boot:run

# 3. Web (outro terminal)
cd web && pnpm install && pnpm dev
```

O front chama `/api/...` na própria origem (porta 3000), e o Next.js repassa para a API na 8080 (ADR 0005).

Alternativa sem compose: `cd api && ./mvnw spring-boot:test-run` sobe a API com um Postgres temporário via Testcontainers.

## Testes

```bash
cd api && ./mvnw verify                           # tudo do backend (Docker precisa estar rodando)
cd api && ./mvnw test -Dtest=ModularityTest       # um teste só

cd web && pnpm lint && pnpm typecheck && pnpm test
```

O CI roda os mesmos comandos em todo PR que toca `api/` ou `web/`.

## Por onde começar a ler o código

Para quem está aprendendo Spring, nesta ordem:

1. [`api/pom.xml`](api/pom.xml): dependências, cada uma com um comentário do que faz.
2. [`RotaDevApplication.java`](api/src/main/java/dev/rotadev/RotaDevApplication.java): o que `@SpringBootApplication` faz.
3. [`application.yml`](api/src/main/resources/application.yml): configuração e por que `ddl-auto: validate` e `open-in-view: false`.
4. [`V1__baseline.sql`](api/src/main/resources/db/migration/V1__baseline.sql): como o Flyway versiona o banco.
5. [`ApplicationIntegrationTest.java`](api/src/test/java/dev/rotadev/ApplicationIntegrationTest.java) e [`TestcontainersConfiguration.java`](api/src/test/java/dev/rotadev/TestcontainersConfiguration.java): um teste que sobe a aplicação inteira contra um Postgres real.
6. [`ModularityTest.java`](api/src/test/java/dev/rotadev/ModularityTest.java): **exercício `TODO(caio)`**.

## Roadmap

- [ ] **Fase 0:** fundação (monorepo, compose, CI, ADRs), em revisão no PR #1
- [ ] **Fase 1:** login com GitHub, ingestão de repositórios, upload de LinkedIn e CV, revisão do perfil
- [ ] **Fase 2:** objetivos, diagnóstico e plano via IA
- [ ] **Fase 3:** painel do dia, sessões, check-in semanal, índice de prontidão
- [ ] **Deploy:** publicação de imagens no GHCR, compose de produção e Caddy compartilhado na VPS (ADR 0013)
- [ ] **Fase 4:** prontidão para SaaS (LGPD, limites, observabilidade)

## Convenções

Ver [AGENTS.md](AGENTS.md): camadas, testes, commits e o fluxo de trabalho.
