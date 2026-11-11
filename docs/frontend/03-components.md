# Componentes

Todos em `web/src/components/`. Regra: componente de apresentação não faz HTTP; telas (`*Section`, `*Dashboard`) usam os hooks de `lib/api/hooks.ts`
e passam dados por props. Cores, raios e tipografia vêm dos tokens (`docs/frontend/01-design-system.md`).

## Primitivos (`ui/`)
| Componente | Finalidade | Props principais | Notas |
|---|---|---|---|
| `Button` | ação | `variant` (default, outline, secondary, ghost, destructive, link), `size` (xs, sm, default, lg, icon, icon-xs, icon-sm, icon-lg) | base-ui + cva |
| `Card`, `CardHeader`, `CardTitle`, `CardDescription` | superfície base | `className`, props de `section` | `aria-labelledby` + `id` no título quando for região |
| `Badge` | status em pílula | `tone`: neutral, brand, success, warning, danger, info | texto sempre diz o status; cor reforça |
| `Progress` | barra acessível | `value`, `max`, `label` (obrigatório) | `role="progressbar"` |
| `Skeleton` | carga com a forma do conteúdo | `className` | `aria-hidden`, pulsa |
| `Input`, `Textarea`, `Select`, `Field` | campos | `Field` recebe `label`, `hint` | altura 36px, foco com anel da marca |
| `Dialog`, `DialogContent`, `ConfirmDialog` | modal / sheet | `placement`: center, right, bottom, responsive; `ConfirmDialog`: `title`, `description`, `confirmLabel`, `destructive`, `busy`, `confirmDisabled`, `onConfirm` | foco preso, Esc fecha (base-ui); título obrigatório |
| `Tabs*` | abas de painel | wrappers de base-ui | usadas no Roadmap |
| `ToastProvider`, `useToast` | feedback | `toast.success(msg)`, `toast.error(msg)` | região `aria-live` |
| `Tooltip` | rótulo de ícone | `label`, `side` | só reforço; nome acessível fica no elemento |

## Sistema (`system/`)
`PageHeader` (título h1, descrição, ações) · `SectionNav` (subseções por rota, `aria-current`) · `EmptyState` (título, descrição, ação útil) ·
`ErrorState` (`message`, `onRetry`, `role="alert"`) · `JobBanner` (status real de um job: `PENDING` ou `FAILED`).

## Marca e tema
`Logo` / `LogoMark` (`mono`, `showName`) · `GithubMark` · `ThemeProvider` / `useTheme` / `ThemeToggle` (escuro, claro, sistema).

## Shell (`shell/`)
`AppShell` (junta tudo) · `Sidebar` (compactável, prontidão) · `Topbar` (migalhas, `JobIndicator`, busca) · `MobileNav` · `CommandPalette` · `AuthGate` ·
`RedirectIfSignedIn` · `UserMenu` · `shortcuts.ts` (`useGlobalShortcuts`).

## Gráficos (`charts/`)
Todos têm alternativa textual (valores em texto ou tabela `sr-only`) e tooltip/estado vazio onde cabe.
| Componente | Pergunta que responde |
|---|---|
| `ReadinessGauge` | "quão pronto estou?" |
| `ReadinessTrend` | "estou melhorando, e em quê?" (índice + 4 componentes) |
| `Sparkline` | "para onde vai?" (SVG puro) |
| `SkillRadar` | "onde estou contra o cargo?" (clique no eixo seleciona) |
| `SkillGapChart` | "o que ataco primeiro?" |
| `GoalComparison` | "quanto falta em cada competência?" |
| `ConsistencyHeatmap` | "estudo com regularidade?" |
| `WeeklyHoursChart` | "cumpro o orçamento de horas?" |
| `SessionMix` | "como divido o tempo?" |

## Domínio por tela
- **`now/`**: `NowDashboard`, `NowHeader`, `WeekPlan`, `AntiVibe`, `SessionList`, `ReadinessCard`, `SidePanel`.
- **`sessions/`**: `SessionProvider`/`useSessionUi`, `SessionSheet`, `SessionForm` (manual + cronômetro), `useStopwatch`.
- **`plan/`**: `GoalSection`/`GoalForm`/`CareerGoalCard`, `DiagnosisSection`/`SkillDetail`, `RoadmapSection`, `VersionsSection`/`PlanDiff`.
- **`progress/`**: `ProgressOverview`/`ReadinessComposition`, `CheckinSection`, `GatesSection`, `AdjustmentsSection`.
- **`profile/`**: `SourcesSection` (`GithubCard`, `UploadCard`), `FoundSection`, `AccountSection`.
- **`evidence/`**: `EvidenceChip`, `Unverified`, `FileUploader` (validação antes do envio).
- **`onboarding/`**: `OnboardingWizard`, `Stepper`. **`settings/`**: `SettingsPanel`.

## Regras de lógica
Cálculos puros ficam em `lib/format.ts` e `lib/domain/*` (`consistency`, `plan-diff`, `profile`) com testes próprios: ritmo da semana, variação do índice,
heatmap/horas/mix, diff de versões, validação de upload, comparação de leituras.
