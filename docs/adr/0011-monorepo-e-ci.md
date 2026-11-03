# 0011. Monorepo e CI por caminho

- **Status:** Aceito (a parte de CI foi substituída pelo [ADR 0014](0014-ci-workflow-unico-com-porteiro.md))
- **Data:** 2026-11-02

## Decisão
Um repositório com `api/`, `web/`, `infra/` e `docs/`. O GitHub Actions tem um workflow por app, disparado só quando os arquivos dele mudam:
- `api.yml`: `./mvnw verify` (testes unitários, de integração com Testcontainers e o `ModularityTest`).
- `web.yml`: lint, typecheck, testes (Vitest) e build.

A `main` só recebe merge via PR com os dois workflows verdes.

## Alternativas descartadas
- **Repositórios separados:** uma funcionalidade que mexe na API e no front vira dois PRs coordenados.

## Consequências
- Um PR por funcionalidade, cobrindo API e front juntos.
- A proteção da branch `main` é configurada no GitHub.
