# Architecture Decision Records (ADRs)

Cada arquivo registra **uma** decisão: o contexto, o que foi decidido, o que foi descartado e o que isso implica.
ADR aceito não se edita. Para mudar de ideia, crie um ADR novo que **substitui** o antigo e marque o antigo como `Substituído por NNNN`.

| # | Decisão | Status |
|---|---|---|
| [0001](0001-java-25-spring-boot-4.md) | Java 25 + Spring Boot 4.1 | Aceito |
| [0002](0002-monolito-modular-spring-modulith.md) | Monólito modular com Spring Modulith | Aceito |
| [0003](0003-api-expoe-apenas-dtos.md) | API expõe só records/DTOs | Aceito |
| [0004](0004-multi-tenant-por-linha.md) | Multi-tenant por `user_id` | Aceito |
| [0005](0005-sessao-no-backend-mesma-origem.md) | Sessão no backend, mesma origem | Aceito |
| [0006](0006-jobs-no-postgres.md) | Fila de jobs no Postgres | Aceito |
| [0007](0007-ia-atras-de-interface.md) | IA atrás de `CareerAiClient` | Aceito |
| [0008](0008-segredos-em-repouso.md) | Segredos e arquivos em repouso | Aceito |
| [0009](0009-convencoes-de-dados.md) | Convenções de dados (IDs, datas, dinheiro) | Aceito |
| [0010](0010-contrato-openapi.md) | Contrato API↔web via OpenAPI | Aceito |
| [0011](0011-monorepo-e-ci.md) | Monorepo e CI por caminho | Aceito |
| [0012](0012-marca-centralizada.md) | Marca centralizada | Aceito |
| [0013](0013-deploy-vps-compose.md) | Deploy na VPS com Docker Compose | Aceito |

Modelo para novos ADRs: [0000-modelo.md](0000-modelo.md).
