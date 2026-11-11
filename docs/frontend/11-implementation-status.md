# Status da implementação do redesign

Legenda: Não iniciado · Em andamento · Implementado (funciona com a API) · Testado (testes automatizados + captura visual).

| Área | Status | Observação |
|---|---|---|
| Auditoria | Implementado | `docs/01-auditoria-tecnica.md` |
| Design system (tokens, tema, logo, base) | Testado | PRs #35, #36 |
| Shell (sidebar, topbar, mobile, paleta, atalhos, guarda) | Testado | testes de integração + captura (desktop, mobile, claro/escuro) |
| `/login` | Implementado | só GitHub; capturado |
| `/configuracoes` | Implementado | tema e resumo da conta; notificações não existem no backend |
| Agora, Plano, Progresso, Perfil | Não iniciado | painéis antigos funcionando dentro do novo shell |
| Onboarding | Não iniciado | |

Validação visual: Chromium (Playwright) com respostas da API simuladas **só no navegador de teste**; nada disso entra no produto.
