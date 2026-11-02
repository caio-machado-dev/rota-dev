# 0010. Contrato API↔web via OpenAPI

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
Tipos TypeScript escritos à mão divergem da API em silêncio.

## Decisão
A API publica seu contrato OpenAPI (springdoc). O front gera os tipos com `openapi-typescript` (`pnpm api:types`). O CI falha se os tipos versionados não baterem com o contrato atual.

## Alternativas descartadas
- **Tipos escritos à mão:** divergem.
- **GraphQL:** mais uma tecnologia, sem necessidade.

## Consequências
- Mudança que quebra o contrato aparece no build do front, e não em produção.
- Entra na Fase 1, junto do primeiro endpoint real.
