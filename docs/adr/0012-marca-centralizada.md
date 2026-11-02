# 0012. Marca centralizada

- **Status:** Aceito
- **Data:** 2026-11-02

## Decisão
Nome, slug, descrição e cores da marca vivem em `web/src/config/brand.ts` (as cores viram variáveis CSS) e, no backend, em `rota.brand.*` no `application.yml`. Nenhum outro arquivo escreve "Rota Dev" literalmente.

## Consequências
Trocar a marca mexe em dois arquivos.
