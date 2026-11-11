# 01. Auditoria técnica do front-end (Fase 1 do redesign)

Data: 2026-11-10. Escopo: `web/`, e o contrato da API em `api/openapi.json` (só leitura; o backend não muda).
Linha de base: `pnpm typecheck` passa. Lint e testes **não foram rodados** nesta fase (ainda sem mudança de código).

## 1. Arquitetura atual
- Next.js App Router, páginas finas (`src/app/**/page.tsx`) que montam **um painel client por seção**
  (`NowPanel`, `PlanView`, `ProgressView`, `ProfileReview`...). Cada painel busca os próprios dados
  em `useEffect` + `useState`, sem cache compartilhado nem biblioteca de dados.
- Chamadas HTTP centralizadas em `src/lib/api/client.ts` (`apiFetch` com CSRF, `ApiError`, uma função por endpoint).
  Tipos vêm do OpenAPI gerado (`schema.d.ts`, ADR 0010). Isso já cumpre "não espalhar HTTP pelas telas"; **preservar**.
- Sessão fica no backend (cookie, mesma origem, rewrite `/api/*`, ADR 0005). Não há `/login` no front:
  o painel recebe 401 e mostra um link para `LOGIN_URL`.
- Marca em `src/config/brand.ts` (ADR 0012). O redesign precisa respeitar: nome, slug e cores só ali.

## 2. Tecnologias
| Item | Encontrado | Prompt pede | Nota |
|---|---|---|---|
| Next.js | 16.4.3 (`cacheComponents`, `partialPrefetching`) | 16 | ok |
| React | 19.3.0 | 19 | ok |
| Tailwind | 4 via `@tailwindcss/turbopack` | 4 | ok |
| shadcn/ui | estilo `base-nova`, sobre **@base-ui/react** (não Radix) | shadcn | só `button.tsx` instalado |
| Recharts | 3.10.1 | sim | usado em 2 lugares (linha e radar) |
| Ícones | lucide-react | sim | ok |
| Testes | Vitest 5 + Testing Library + jsdom | | 1 teste por componente |
| Estado/dados | nenhum (useState/useEffect) | | ver problemas |
| Fontes | Geist e Geist Mono (`next/font`) | sim | ok |
Não falta dependência estrutural. Candidatas novas (decisão abaixo): paleta de comandos (`cmdk`), dados (`swr` ou `@tanstack/react-query`).

## 3. Estrutura atual (`web/src`, 28 arquivos fora lockfile)
```
app/ layout.tsx page.tsx(Agora) plano/ progresso/ perfil/   globals.css
components/ now/ plan/ profile/ progress/ shell/ ui/button.tsx
config/ brand.ts navigation.ts      lib/ api/{client.ts,schema.d.ts} utils.ts
```
Rotas existentes: `/`, `/plano`, `/progresso`, `/perfil`. **Não existem** `/login`, `/onboarding`, `/configuracoes`
nem sub-rotas (as subdivisões hoje são seções empilhadas numa página só).

## 4. Mapeamento backend → tela
Legenda: ✅ existe e já tem UI · 🟡 existe, UI a refazer · ❌ não existe no backend.

| Funcionalidade do prompt | Endpoint | Tela alvo | Status |
|---|---|---|---|
| Usuário, fuso, tier | `GET /api/me` | shell, Meus dados | ✅ |
| Fontes (GitHub) e sync (job 202) | `GET /sources`, `POST /sources/github/sync` | Perfil › Fontes | 🟡 |
| Upload LinkedIn/currículo | `GET/POST /sources/uploads` (status, formato, tamanho) | Perfil › Fontes | 🟡 (sem % de progresso: só `status`) |
| Perfil, snapshots, skills, evidências | `GET /profile`, `/profile/snapshots/{id}` | Perfil › O que encontramos | 🟡 |
| Objetivo | `GET/PUT /goal` | Plano › Objetivo | 🟡 |
| Diagnóstico (eixos, lacunas, evidências citadas) | `POST /diagnosis` (202), `GET /diagnosis/latest`, `/{id}` | Plano › Diagnóstico | 🟡 |
| Plano: trimestres, semanas, portões, projeto-âncora, recursos | `POST /plan` (202), `GET /plan/current`, `/latest` | Plano › Roadmap | 🟡 |
| Versões do plano | `GET /plan/versions`, `/versions/{id}` | Plano › Versões | 🟡 |
| Agora: semana do plano, sessões, hábito, orçamento | `GET /now` | Agora | 🟡 |
| Registrar/apagar sessão | `POST/DELETE /tracking/sessions`, `GET ?from&to` | Sessão global, Agora | 🟡 |
| Hábito anti-vibe (3 passos, por dia) | `PUT /tracking/review-habit` | Agora | 🟡 |
| Template semanal de estudo | `GET/PUT /tracking/template` | Agora, Configurações | ✅ |
| Prontidão atual + histórico semanal (4 componentes) | `GET /readiness` | Agora, Progresso | 🟡 |
| Check-in | `GET/POST /checkin`, `GET /checkin/history` | Progresso › Check-in | 🟡 |
| Portões conquistados (com URL de evidência) | `GET/POST /milestones` | Progresso › Portões | 🟡 |
| Reajustes (aceitar/recusar) | `GET /adjustments`, `POST /{id}/accept\|reject` | Progresso › Reajustes | 🟡 |
| Exportar e excluir conta (LGPD) | `GET /account/export`, `DELETE /account` | Perfil › Meus dados | ✅ |

### Lacunas do backend (o prompt pede, a API não tem)
Regra do prompt: documentar e propor alternativa, sem simular. Proposta:

| Pedido | Situação | Alternativa proposta |
|---|---|---|
| Restaurar versão do plano | sem endpoint | não oferecer o botão; só consultar |
| Diff entre versões | sem endpoint, mas `GET /plan/versions` traz o conteúdo | calcular o diff **no cliente** (função pura, testável) |
| Confirmar/corrigir/remover/adicionar skill; contestar nota | sem endpoint | exibir só leitura; registrar como pendência de backend |
| Streak e histórico do anti-vibe | só existe o hábito de hoje (`habitToday`) | mostrar o dia; sem streak até haver `GET` de hábitos |
| Contribuições GitHub na semana | só `commits` digitado no check-in | usar commits do check-in; rotular a origem |
| Heatmap de consistência | `GET /tracking/sessions?from&to` dá dias com minutos | **viável**; sem dado de hábito por dia |
| Notificações, central de jobs | sem endpoint | jobs derivados do `status` de diagnóstico/plano/upload (já pollados); notificações fora |
| Etapas reais do processamento do diagnóstico | só `status` (e `error`) | mostrar estados reais (na fila/pronto/falhou); **sem** etapas inventadas |
| Preferência de notificações, integrações LinkedIn/dados manuais como "conexão" | LinkedIn e currículo são **uploads**, não conexões | modelar como upload |
| Progresso de upload (%) | não há | só estado e erro |
| Busca global de skills/evidências | sem endpoint | filtro local sobre `GET /profile` |
| Login por outro método | só GitHub OAuth | só GitHub |

## 5. Componentes reaproveitáveis
- **Manter:** `client.ts` inteiro, tipos gerados, `navigation.ts`, `brand.ts`, `utils.ts`, lógica dos painéis (estados 401/erro/vazio já existem e têm testes).
- **Reescrever a apresentação:** `AppShell`, os 6 painéis, `Button` (variantes), gráficos.
- **Criar:** biblioteca de UI própria (card, badge, skeleton, dialog/sheet, toast, tabs, progress, tooltip), tokens, logo, gauge, heatmap, sparkline, paleta de comandos.

## 6. Problemas encontrados
1. **Painéis gigantes** misturam busca de dados, regra e JSX (`progress-view` 329 linhas, `now-panel` 281, `profile-review` 251). Contraria "componentes com responsabilidade clara".
2. **Sem camada de dados**: `useEffect` repetido, sem cache, sem revalidação. O polling de jobs (diagnóstico, plano, upload) é refeito à mão em cada painel.
3. **Mesmo 401 tratado em cada painel** (link "Entre com GitHub" repetido); falta uma guarda única e a rota `/login`.
4. **Tema**: o `html` tem `className="dark"` fixo; o claro existe no CSS mas **não há alternância**. `--sidebar-primary` no escuro é azul-roxo (resto do template shadcn), fora da marca.
5. **Paleta**: tokens são o neutro padrão do shadcn; faltam escalas, `success/warning/info`, `surface-*`, `chart-6` (hoje chart-1..5 só em cinza). Só `--brand` é próprio; verde-água `oklch(.78 .16 165)`.
6. **Larguras**: páginas em `max-w-3xl` (≈768px), o contrário do que o prompt pede (1200–1320px).
7. **Sem logo/favicon** (marca é texto mono). Sem skeletons (só "Carregando…").
8. **Estilos repetidos** em strings (`fieldClass` em `now-panel`), sem componentes de formulário.
9. Mocks: não há fixtures; qualquer demo deve ficar isolada (pasta própria, fora de produção).
10. `AGENTS.md` do `web/` avisa que este Next tem mudanças incompatíveis: **ler `node_modules/next/dist/docs/` antes de mexer em rotas, layouts e cache** (`cacheComponents` está ligado).

## 7. Riscos
- `cacheComponents` + páginas client: rotas novas com route groups e `loading.tsx` precisam seguir os docs da versão.
- Remover dado/fluxo existente por redesign (regra 4 do prompt): cada painel tem testes; manter os testes verdes a cada PR e **portar** os cenários para os componentes novos.
- Volume: o prompt equivale a vários PRs grandes. Pelo `AGENTS.md`, PRs de até ~400 linhas e parada por fase; uma "Fase 4 inteira" já são vários PRs.
- Memória da máquina: nada de rodar suíte inteira nem Playwright em paralelo com testes (regras do CLAUDE.md global).
- Capturas visuais com Playwright são possíveis (MCP instalado), mas o login é GitHub OAuth: para validar telas autenticadas é preciso API local + sessão, ou fixtures isoladas.

## 8. Plano de execução proposto (cada item = 1 ou mais PRs pequenos para `develop`)
- **F2 Design system:** tokens (cores, tipografia, raios, tema claro/escuro com alternância), logo/favicon, 8–10 componentes-base. Docs `docs/frontend/01`.
- **F3 Shell:** sidebar compactável, topbar, navegação mobile, `CommandPalette`, atalhos, guarda de 401 + `/login`.
- **F4 Agora**, **F5 Plano**, **F6 Progresso**, **F7 Perfil**, **F8 Onboarding**, **F9 Refinamento**, **F10 Docs finais**. Cada tela: extrair hook de dados + componentes de apresentação, manter os testes.
- Docs `docs/frontend/*` nascem junto de cada fase (não no fim), com `IMPLEMENTATION_STATUS.md` honesto.

## 9. Decisões que preciso de você antes da Fase 2
1. **Direção de cor.** A imagem de referência é preto + verde-limão; hoje a marca é verde-água (165°). Mudar `brand.ts` para o verde-limão (~hue 125) ou manter o verde-água e só refinar? (Recomendo: verde-limão da imagem como primária, via `brand.ts`.)
2. **Camada de dados.** Adicionar `swr` (leve, ~4 KB) para cache, revalidação e polling de jobs, ou seguir com hooks próprios? (Recomendo SWR: tira a repetição do problema 2. Como você vem de Python: é como um cache de requests com `refetch` automático.)
3. **Rotas.** Criar `/login`, `/onboarding`, `/configuracoes` e sub-rotas (`/plano/diagnostico`, `/progresso/checkin`...) com route groups, ou manter seções numa página com abas? (Recomendo sub-rotas para as 4 seções principais e as 3 novas; mantém `/`, `/plano`, `/progresso`, `/perfil` intactas.)
4. **Paleta de comandos.** Adicionar `cmdk` ou construir com `@base-ui/react`? (Recomendo `cmdk`.)
5. **Onboarding sem estado no servidor:** o wizard será derivado do que já existe (fontes? perfil? objetivo? diagnóstico?). Aceitável?
6. **Ritmo.** O prompt pede tudo de uma vez; o `AGENTS.md` pede parada por fase. Sugiro seguir o `AGENTS.md`: pausa para sua revisão ao fim de cada fase.
