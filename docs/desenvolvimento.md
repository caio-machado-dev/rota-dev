# Guia de desenvolvimento

Como preparar a máquina, rodar e testar o Rota Dev localmente. Convenções de código e de Git: [AGENTS.md](../AGENTS.md).

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

### Login com GitHub

1. Em https://github.com/settings/developers, crie uma **OAuth App** com:
   - Homepage URL: `http://localhost:3000`
   - Authorization callback URL: `http://localhost:3000/api/login/oauth2/code/github`
2. Copie `infra/.env.example` para `infra/.env` e preencha `GITHUB_CLIENT_ID` e `GITHUB_CLIENT_SECRET`.
   Rodando a API fora do Docker, exporte as duas variáveis no terminal antes do `./mvnw spring-boot:run`.
3. Abra http://localhost:3000/api/oauth2/authorization/github. Depois do login, `GET /api/me` devolve o usuário.

Sem as variáveis a API sobe normalmente, mas o GitHub recusa o login.

## Testes

```bash
cd api && ./mvnw verify                           # tudo do backend (Docker precisa estar rodando)
cd api && ./mvnw test -Dtest=ModularityTest       # um teste só

cd web && pnpm lint && pnpm typecheck && pnpm test
```

O CI roda os mesmos comandos em todo PR que toca `api/` ou `web/`.

## Por onde começar a ler o código

Para quem está aprendendo Spring, nesta ordem:

1. [`api/pom.xml`](../api/pom.xml): dependências, cada uma com um comentário do que faz.
2. [`RotaDevApplication.java`](../api/src/main/java/dev/rotadev/RotaDevApplication.java): o que `@SpringBootApplication` faz.
3. [`application.yml`](../api/src/main/resources/application.yml): configuração e por que `ddl-auto: validate` e `open-in-view: false`.
4. [`V1__baseline.sql`](../api/src/main/resources/db/migration/V1__baseline.sql): como o Flyway versiona o banco.
5. [`ApplicationIntegrationTest.java`](../api/src/test/java/dev/rotadev/ApplicationIntegrationTest.java) e [`TestcontainersConfiguration.java`](../api/src/test/java/dev/rotadev/TestcontainersConfiguration.java): um teste que sobe a aplicação inteira contra um Postgres real.
6. [`ModularityTest.java`](../api/src/test/java/dev/rotadev/ModularityTest.java): **exercício `TODO(caio)`**.
7. Módulo [`identity`](../api/src/main/java/dev/rotadev/identity): o primeiro módulo real, lido de dentro para fora. `domain/` (records puros) → `application/` (`GithubSignIn`, com `@Transactional`, e a interface `UserRepository`) → `infrastructure/` (entidade JPA e a implementação do repositório) → [`V2__users.sql`](../api/src/main/resources/db/migration/V2__users.sql). Os testes ficam no mesmo pacote em `src/test`, e `GithubSignInTest` tem outro **exercício `TODO(caio)`**.
