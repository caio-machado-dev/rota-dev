# Testes

## Estratégia
- **Unitários (lógica pura):** `lib/format`, `lib/domain/consistency`, `plan-diff`, `profile`, `config/navigation`.
- **Componentes/telas:** Testing Library + Vitest (jsdom). Cada tela é testada como o usuário a usa (papéis e rótulos), com a API simulada por
  `src/test/helpers.tsx` (`stubApi` registra as escritas; `renderScreen` isola o cache SWR). Cobrem: estados de carga/erro/vazio, escrita com corpo
  correto, confirmações, preservação de dados em erro, estados de job.
- **Visual e acessibilidade (manual/ferramenta):** capturas em Chromium (Playwright) em 1440 e 390 px, claro e escuro; `axe-core` em todas as rotas
  (ver `07-accessibility.md`). Os scripts ficam fora do repositório (usam fixtures só para validação visual).
- **E2E com backend real:** não há. Cenário recomendado como próximo passo: Playwright contra `docker compose` com um usuário de teste.

## Execução (máquina de 16 GB)
`NODE_OPTIONS=--max-old-space-size=2048 em-fila pnpm exec vitest run --maxWorkers=2 <arquivo>`, um arquivo por vez. Também: `pnpm typecheck`, `pnpm lint`, `pnpm build`.
Última rodada: 22 arquivos de teste, 85 casos; typecheck, lint e build verdes.

## Cenários críticos cobertos
Login/guarda de sessão · navegação e atalhos (`g`+tecla, ⌘K, ignorar campos) · dashboard (semana, ritmo, prontidão com pesos) · registro de sessão a partir de
tarefa · protocolo anti-vibe salva ao marcar · objetivo (PUT, erro preservando campos) · diagnóstico (seleção de eixo, "Não verificado", job) · roadmap (semana
atual) · versões (git log e diff) · check-in (fechado, aberto, erro, concluído) · portões (registrar evidência) · reajuste (confirmação, recusa, antes/depois) ·
fontes (upload válido/inválido) · habilidades (filtros, `?q=`, comparação) · exclusão de conta em dois passos · onboarding de ponta a ponta · alternância de tema.

## Lacunas
Sem testes de regressão visual automatizados, sem E2E com API real, sem teste de leitor de tela real.
