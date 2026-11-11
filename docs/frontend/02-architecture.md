# Arquitetura do front-end

- **Rotas** (`web/src/app`): grupo `(auth)` (`/login`, sem shell) e `(dashboard)` (`/`, `/plano`, `/progresso`, `/perfil`, `/configuracoes`, com `AppShell`).
  Subrotas por seção chegam com cada tela (ex.: `/plano/diagnostico`).
- **Shell** (`components/shell`): `AppShell` junta `Sidebar` (fixa, compactável, mostra a prontidão), `Topbar` (migalhas, jobs, busca),
  `MobileNav` (4 abas), `CommandPalette` (⌘/Ctrl+K) e `AuthGate` (sem sessão → `/login`).
- **Atalhos:** `g a`, `g p`, `g r`, `g f` navegam; ⌘/Ctrl+K abre a paleta; nada dispara dentro de campos de texto nem com Ctrl/⌘/Alt (exceto ⌘K).
- **Dados:** `lib/api/client.ts` (HTTP, tipos do OpenAPI) → `lib/api/hooks.ts` (SWR) → componentes. Ver ADR 0017.
- **Componentes:** `ui/` (primitivos: button, card, badge, progress, skeleton, tabs, dialog, toast), `system/` (page-header, estados),
  `brand/` (logo), `theme/`, `settings/`, e pastas por tela (`now`, `plan`, `progress`, `profile`).
- **Estado de interface** no navegador: tema (`rota-theme`) e barra lateral (`rota-sidebar`) em `localStorage`, sempre com try/catch.
