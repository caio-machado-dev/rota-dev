# Integração com a API

Nenhum endpoint, contrato ou regra de negócio do backend foi alterado. Tipos vêm do OpenAPI gerado (`pnpm api:types`, ADR 0010).
Camadas: `lib/api/client.ts` (HTTP e erros) → `lib/api/hooks.ts` (SWR, ADR 0017) → componentes.

## Transporte e autenticação
- Caminhos relativos `/api/...`; o Next faz rewrite para a API (ADR 0005). Sessão por cookie, mesma origem, sem CORS.
- `apiFetch` adiciona `X-XSRF-TOKEN` (cookie `XSRF-TOKEN`) em métodos que alteram dados.
- Login: link `/api/oauth2/authorization/github` (navegação inteira). Escopos: `read:user`, `user:email`. Logout: `POST /api/logout`.
- 401: `AuthGate` (shell) manda para `/login`; `RedirectIfSignedIn` faz o caminho inverso. 404 em "recurso opcional" vira `null` (objetivo, diagnóstico, plano).
- Erros: `ApiError(status, mensagem do ProblemDetail)`; telas mostram a mensagem da API e preservam o que foi digitado.

## Endpoints usados
| Recurso | Endpoint | Hook / função | Telas |
|---|---|---|---|
| Usuário | `GET /api/me` | `useMe` | shell, Agora, Configurações, Meus dados |
| Hoje | `GET /api/now` | `useNow` | Agora, Roadmap, Progresso |
| Prontidão | `GET /api/readiness` | `useReadiness` | sidebar, Agora, Progresso |
| Objetivo | `GET/PUT /api/goal` | `useGoal`, `saveGoal` | Plano, onboarding |
| Diagnóstico | `GET /api/diagnosis/latest`, `POST /api/diagnosis` (202) | `useLatestDiagnosis`, `requestDiagnosis` | Plano, Progresso, onboarding |
| Plano | `GET /api/plan/latest`, `/current`, `/versions`, `/versions/{id}`; `POST /api/plan` (202) | `useLatestPlan`, `useCurrentPlan`, `usePlanVersions`, `usePlanVersion` | Plano, Agora, Portões, Reajustes |
| Sessões | `GET /api/tracking/sessions?from&to`, `POST`, `DELETE /{id}` | `useSessions`, `logSession`, `deleteSession` | Agora, Progresso |
| Hábito anti-vibe | `PUT /api/tracking/review-habit` | `saveHabit` | Agora |
| Check-in | `GET/POST /api/checkin`, `GET /api/checkin/history` | `useCheckinStatus`, `useCheckinHistory`, `submitCheckin` | Progresso, Agora |
| Portões | `GET/POST /api/milestones` | `useMilestones`, `achieveMilestone` | Progresso, Agora, Roadmap |
| Reajustes | `GET /api/adjustments`, `POST /{id}/accept\|reject` | `useAdjustments`, `decideAdjustment` | Progresso, Agora |
| Fontes | `GET /api/sources`, `POST /api/sources/github/sync` (202) | `useSources`, `syncGithub` | Perfil, onboarding |
| Arquivos | `GET/POST /api/sources/uploads` (multipart) | `useUploads`, `uploadDocument` | Perfil, onboarding |
| Perfil | `GET /api/profile` | `useProfile` | Perfil, Roadmap, paleta, onboarding |
| Conta | `GET /api/account/export`, `DELETE /api/account` | `EXPORT_URL`, `deleteAccount` | Meus dados |

Não usados: `GET /api/profile/snapshots/{id}`, `GET /api/diagnosis/{id}`, `GET/PUT /api/tracking/template` (hoje só lido via `/api/now`).

## Jobs assíncronos
Não existe endpoint de jobs. Diagnóstico e plano são consultados com `refreshInterval` de 3 s enquanto `status === "PENDING"`; arquivos enquanto `RECEIVED`;
a leitura do GitHub é acompanhada por `lastSyncedAt` (até 90 s). O `JobIndicator` da topbar deriva dos status de diagnóstico e plano.

## Incompatibilidades e lacunas (documentadas, não simuladas)
Ver `docs/01-auditoria-tecnica.md` §4 e `04-pages.md`: sem restaurar versão do plano, sem contestar nota, sem confirmar/corrigir/remover habilidade, sem dados
manuais, sem streak do protocolo, sem tarefa do plano "concluída", sem notificações, sem % de upload, sem "impacto esperado" em reajustes.
