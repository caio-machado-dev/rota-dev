# Status da implementação do redesign

Legenda: **Testado** = funciona com a API (contratos reais) + testes automatizados + captura visual + axe sem violações.
Pendências listadas por área; nada abaixo é layout estático.

| Área | Status | Observação |
|---|---|---|
| Auditoria | Implementado | `docs/01-auditoria-tecnica.md` |
| Design system (tokens, temas, logo, base) | Testado | claro e escuro; `01-design-system.md` |
| Shell (sidebar, topbar, mobile, paleta, atalhos, guarda) | Testado | `g`+tecla, ⌘K, `n`, `c`; skip link |
| `/login` | Testado | só GitHub (único método da API) |
| `/configuracoes` | Testado | tema e conta; **sem notificações** (API) |
| Agora | Testado | **sem** concluir tarefa do plano, streak do anti-vibe, commits do GitHub na semana (API) |
| Plano: objetivo, diagnóstico, roadmap, versões | Testado | **sem** restaurar versão, autor, contestar nota (API); diff calculado no cliente |
| Progresso: visão geral, check-in, portões, reajustes | Testado | **sem** impacto esperado de reajuste (API) |
| Perfil: fontes, o que encontramos, meus dados | Testado | **sem** editar habilidades nem dados manuais (API); sem % de upload |
| Onboarding (4 etapas) | Testado | sem etapas de processamento detalhadas (API só tem status) |
| Documentação (`docs/frontend/01` a `11`) | Implementado | |

## Validação final
- `pnpm typecheck`, `pnpm lint`, `pnpm build`: verdes. Testes: 22 arquivos, 85 casos, todos passando (rodados um arquivo por vez).
- Capturas em 1440×900 e 390×844, claro e escuro, sem overflow horizontal. `axe-core`: 0 violações em 15 rotas × 2 temas × 2 tamanhos.

## Pendências reais
1. Revisão visual humana e ajuste fino de copy e proporções (nada foi validado por uma pessoa ainda).
2. E2E com backend real e leitor de tela real (VoiceOver/NVDA).
3. Funcionalidades que dependem de backend: ver `docs/01-auditoria-tecnica.md` §4.
4. Notificações, streak do anti-vibe, tarefa concluída no plano, restaurar versão, editar habilidades: aguardam endpoints.
5. Tema do `Cache disabled` do Next em modo dev aparece nas capturas; não existe em produção.
