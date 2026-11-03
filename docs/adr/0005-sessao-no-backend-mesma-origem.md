# 0005. Sessão no backend, front e API na mesma origem

- **Status:** Aceito
- **Data:** 2026-11-02

## Contexto
O login é com GitHub (OAuth). O front é Next.js e a API é Spring. Precisamos decidir quem guarda a sessão e como o navegador prova quem é.

## Decisão
- A API (Spring Security, módulo OAuth2 Client) faz o fluxo OAuth com o GitHub e cria uma **sessão no servidor**, guardada no Postgres (Spring Session JDBC).
- O navegador recebe só um cookie `HttpOnly`, `Secure` e `SameSite=Lax` com o id da sessão.
- O Next.js faz **rewrite** de `/api/*` para a API, então navegador, front e API ficam na mesma origem.
- Proteção contra CSRF com o token em cookie do Spring Security.

## Alternativas descartadas
- **JWT entregue ao front:** se ficar no `localStorage`, fica exposto a XSS. Revogar e renovar exige infraestrutura extra (refresh token, lista de bloqueio).
- **Auth.js (NextAuth) no front:** a lógica de identidade iria para o lado TypeScript, e o objetivo do projeto é dominar Spring Security.

## Consequências
- Sem CORS para configurar.
- Logout e revogação são imediatos (apagar a sessão).
- Server Components do Next.js que chamam a API precisam repassar o cookie da requisição.
- Implementado na Fase 1.
