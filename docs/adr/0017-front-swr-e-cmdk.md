# 0017. Front-end: SWR para dados e cmdk para a paleta de comandos

- **Status:** Aceito
- **Data:** 2026-11-10

## Contexto
Cada painel do `web/` buscava os próprios dados com `useEffect`, sem cache compartilhado, com o 401 e o polling de jobs repetidos à mão.

## Decisão
- **SWR** (`web/src/lib/api/hooks.ts`): um hook por recurso, sobre as funções de `client.ts`. O HTTP continua só em `client.ts`.
  Diagnóstico e plano fazem polling a cada 3 s enquanto `PENDING`.
- **cmdk** para a paleta de comandos (⌘/Ctrl + K), com atalhos em `components/shell/shortcuts.ts`.
- **clsx + tailwind-merge** (com os nomes do design system) em `cn()`, no lugar do pacote `cn`, que apagava classes de tamanho próprias.
- Guarda de sessão única (`AuthGate`) e rota `/login`; rotas agrupadas em `(auth)` e `(dashboard)`.

## Consequências
Três dependências pequenas. Telas novas usam os hooks; os painéis antigos migram junto com o redesign de cada tela.
