# 0014. CI com workflow único e check porteiro

- **Status:** Aceito
- **Data:** 2026-11-03
- **Substitui:** a parte de CI do [ADR 0011](0011-monorepo-e-ci.md)

## Contexto
O ADR 0011 criou um workflow por app (`api.yml`, `web.yml`), disparado só quando os arquivos do app mudam. Com as branches protegidas, queremos que o GitHub **exija** CI verde para o merge. Só que um check obrigatório que nunca roda (ex.: num PR que só mexe em `docs/`) fica pendente para sempre e trava o merge.

## Decisão
Um único workflow, `ci.yml`, que roda em todo PR e em todo push para `develop`/`main`:
- `changes`: descobre, com `git diff`, se o PR tocou `api/`, `web/` ou o próprio workflow.
- `api` e `web`: rodam só se a pasta deles mudou.
- `ci-ok` (**porteiro**): roda sempre e só passa se nenhum job falhou ou foi cancelado (pulado conta como ok). É o **único check obrigatório** em `develop` e `main`.

Em push para `develop`/`main` (que só acontece via merge), tudo roda.

## Alternativas descartadas
- **Manter dois workflows e não exigir checks:** a regra "só entra com CI verde" vira só convenção.
- **Exigir `api` e `web` como obrigatórios:** trava PRs que não tocam essas pastas.
- **Action de terceiros para detectar mudanças (ex.: `dorny/paths-filter`):** resolve, mas `git diff` em 10 linhas de shell faz o mesmo sem acrescentar dependência externa no pipeline.

## Consequências
- PR só de documentação passa em segundos (só `changes` e `ci-ok` rodam).
- Adicionar um app novo ao monorepo = um job novo + uma linha no `changes` + incluí-lo no `needs` do `ci-ok`.
- Renomear o job `ci-ok` exige atualizar a proteção das branches.
