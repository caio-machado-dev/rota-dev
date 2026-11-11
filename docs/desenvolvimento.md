# Guia de desenvolvimento

Como preparar a máquina, rodar e testar o Rota Dev localmente. Convenções de código e de Git: [AGENTS.md](../AGENTS.md). Produção: [deploy.md](deploy.md).

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
cd api && ROTA_ENCRYPTION_KEY=Y2hhdmUtZGUtZGV2LWxvY2FsLW5hby11c2FyLXByb2Q= ./mvnw spring-boot:run

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

### Chave de cifra

O token do GitHub é gravado cifrado (ADR 0008), e a API não sobe sem `ROTA_ENCRYPTION_KEY`.
O `infra/compose.yaml` já traz uma chave de desenvolvimento. Rodando a API fora do Docker:

```bash
export ROTA_ENCRYPTION_KEY=$(openssl rand -base64 32)
```

Os testes usam a chave de `api/src/test/resources/application.properties`.

### IA (diagnóstico e plano)

Por padrão a IA vem do **OpenRouter com um modelo gratuito** (`nvidia/nemotron-3.5-super-vl:free`, ADR 0016):

1. Crie uma chave em https://openrouter.ai/keys (grátis, sem cartão).
2. Coloque em `infra/.env`: `OPENROUTER_API_KEY=sk-or-...` (ou exporte no terminal da API).

Limites do plano gratuito: 20 pedidos por minuto e **50 por dia**. Um diagnóstico e um plano usam
de 2 a 4 pedidos. Modelos gratuitos podem registrar o que recebem: use só com os **seus** dados.

Sem a chave, tudo sobe normalmente e só a geração de diagnóstico e plano falha, na hora e com a
mensagem "OPENROUTER_API_KEY não configurada". Os testes nunca chamam a API: o `LlmGateway` é trocado
por um falso. Cada chamada fica registrada na tabela `ai_calls`, com tokens e custo (zero nos `:free`).

Para usar o Claude: `ROTA_AI_PROVIDER=anthropic` e `ANTHROPIC_API_KEY`.

## Testes

```bash
cd api && ./mvnw verify                           # tudo do backend (Docker precisa estar rodando)
cd api && ./mvnw test -Dtest=ModularityTest       # um teste só

cd web && pnpm lint && pnpm typecheck && pnpm test
```

O CI roda os mesmos comandos em todo PR que toca `api/` ou `web/`.

### Contrato da API (ADR 0010)

O contrato OpenAPI fica versionado em `api/openapi.json`, e os tipos do front em `web/src/lib/api/schema.d.ts`.
Mudou um controller ou um record da camada `web`:

```bash
cd api && ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true   # regenera api/openapi.json
cd web && pnpm api:types                                                 # regenera os tipos TypeScript
```

Comite os dois arquivos. O `OpenApiContractTest` e o CI falham se algum deles ficar desatualizado.
Com a API rodando, o Swagger UI fica em http://localhost:8080/swagger-ui.html.

## Por onde começar a ler o código

Para quem está aprendendo Spring, nesta ordem:

1. [`api/pom.xml`](../api/pom.xml): dependências, cada uma com um comentário do que faz.
2. [`RotaDevApplication.java`](../api/src/main/java/dev/rotadev/RotaDevApplication.java): o que `@SpringBootApplication` faz.
3. [`application.yml`](../api/src/main/resources/application.yml): configuração e por que `ddl-auto: validate` e `open-in-view: false`.
4. [`V1__baseline.sql`](../api/src/main/resources/db/migration/V1__baseline.sql): como o Flyway versiona o banco.
5. [`ApplicationIntegrationTest.java`](../api/src/test/java/dev/rotadev/ApplicationIntegrationTest.java) e [`TestcontainersConfiguration.java`](../api/src/test/java/dev/rotadev/TestcontainersConfiguration.java): um teste que sobe a aplicação inteira contra um Postgres real.
6. [`ModularityTest.java`](../api/src/test/java/dev/rotadev/ModularityTest.java): **exercício `TODO(caio)`**.
7. Módulo [`identity`](../api/src/main/java/dev/rotadev/identity): o primeiro módulo real, lido de dentro para fora. `domain/` (records puros) → `application/` (`GithubSignIn`, com `@Transactional`, e a interface `UserRepository`) → `infrastructure/` (entidade JPA e a implementação do repositório) → [`V2__users.sql`](../api/src/main/resources/db/migration/V2__users.sql). Os testes ficam no mesmo pacote em `src/test`, e `GithubSignInTest` tem outro **exercício `TODO(caio)`**.
