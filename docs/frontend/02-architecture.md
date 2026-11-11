# Arquitetura do front-end

## Rotas (`web/src/app`)
```
(auth)/       login/   onboarding/                       (sem shell)
(dashboard)/  page.tsx (Agora)  template.tsx (fade)  layout.tsx (AppShell)
              plano/{,diagnostico,roadmap,versoes}   progresso/{,checkin,portoes,reajustes}
              perfil/{,encontramos,dados}            configuracoes/
layout.tsx (tema, toasts)  globals.css (tokens)  icon.svg
```
Cada seção tem `layout.tsx` com `PageHeader` + `SectionNav`; as páginas são finas e montam um componente `*Section`/`*Dashboard`.

## Camadas
1. `lib/api/client.ts`: HTTP, CSRF, `ApiError`, uma função por endpoint, tipos do OpenAPI.
2. `lib/api/hooks.ts`: SWR (um hook por recurso, polling de jobs). ADR 0017.
3. `lib/format.ts`, `lib/domain/*`: regras puras e testadas.
4. `components/*`: apresentação + telas; estado de interface local (`useState`) e contextos pequenos (`SessionProvider`, `ThemeProvider`, `ToastProvider`).

## Shell (`components/shell`)
`AppShell` = `Sidebar` + `Topbar` + `MobileNav` + botão flutuante + `SessionSheet` + `CommandPalette` + `AuthGate` + atalhos (`shortcuts.ts`).
Preferências do navegador: tema (`rota-theme`) e barra lateral (`rota-sidebar`) em `localStorage`, sempre com try/catch.

## Decisões
- Sem `features/` por enquanto: pastas por tela dentro de `components/` bastam para o tamanho atual (a referência do prompt era sugestão).
- Dados fictícios: nenhum no código de produto. Fixtures de validação visual ficam fora do repositório.
- Backend inalterado; lacunas listadas em `docs/01-auditoria-tecnica.md` e `04-pages.md`.
- `AGENTS.md` do `web/`: Next 16 com `cacheComponents`; páginas client com `useSearchParams` ficam sob `Suspense`.
