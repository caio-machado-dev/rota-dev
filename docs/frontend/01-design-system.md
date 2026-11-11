# Design system do Rota Dev

Fonte dos valores: `web/src/app/globals.css` (tokens) e `web/src/config/brand.ts` (nome e cor da marca, ADR 0012).
Regra: componentes usam **tokens semânticos** (`bg-card`, `text-muted-foreground`, `rounded-card`), nunca cor, raio ou sombra soltos.

## Identidade
Direção "Obsidian": grafite com um toque de verde (matiz 128), verde-limão como única cor de marca, branco suave.
Escuro é o padrão; o claro usa um verde mais fechado porque o limão some no branco.

| Tema | `--brand` | Texto sobre a marca |
|---|---|---|
| Escuro | `oklch(0.88 0.2 128)` | `oklch(0.22 0.05 128)` |
| Claro | `oklch(0.5 0.15 135)` | `oklch(0.99 0 0)` |

Logotipo: um "R" desenhado como trajeto (`web/src/components/brand/logo.tsx`: `LogoMark` e `Logo`), com ponto de partida na cor da marca.
Usa `currentColor`, então serve em fundo escuro, claro e na versão monocromática (`mono`). Favicon: `web/src/app/icon.svg` (SVG estático, com as
cores copiadas de `brand.ts`; se a marca mudar, atualize os dois).

## Cores
- **Escalas** (`primary`, `neutral` 50–950; `success`, `warning`, `destructive`, `info` 50–900): classes `bg-primary-500`, `text-info-300`...
  São matéria-prima; em componentes prefira os semânticos.
- **Semânticos:** `background`, `foreground`, `surface`, `surface-raised`, `surface-overlay`, `card`, `popover`, `muted`, `accent`,
  `secondary`, `border`, `input`, `ring`, `primary`, `destructive`, `success`, `warning`, `info`, `sidebar*`, `chart-1..6`
  (cada cor de status tem o par `-foreground`).
- **Gráficos:** `viz-current` e `viz-required` (atual × exigido) foram validados com o validador de paleta; `chart-1..6` para séries extras.
  Cor nunca é a única forma de informação: gráficos trazem os números em texto.
- Profundidade no escuro: `background` < `surface` < `surface-raised` < `surface-overlay`. Evite caixa dentro de caixa.

## Tipografia
Geist (interface) e Geist Mono (identificadores, métricas; com moderação). Números com a classe `num` (tabular-nums).

| Classe | Tamanho | Uso |
|---|---|---|
| `text-display` | 72px | número de destaque (prontidão) |
| `text-h1` / `text-h2` / `text-h3` | 30 / 22 / 17px | títulos |
| `text-body` | 15px | texto corrido |
| `text-small` | 13px | apoio |
| `text-micro` | 12px | rótulos, legendas |

## Espaço, raio, sombra, movimento
- Espaço: escala de 4px do Tailwind (`1`=4px ... `16`=64px). Use 4, 8, 12, 16, 24, 32, 48, 64.
- Raios: `rounded-control` 6px, `rounded-card` 10px, `rounded-modal` 14px, status em `rounded-full`.
- Sombras: `shadow-surface`, `shadow-raised`, `shadow-overlay` (sutis; a hierarquia vem das superfícies).
- Movimento: `duration-150/200/250` e `ease-out-soft`; só para feedback (menus, progresso, skeleton). `prefers-reduced-motion`
  zera as transições e animações globalmente.
- Ícones: Lucide, traço 1.5 (`strokeWidth={1.5}`), 16 ou 20px.

## Temas
`ThemeProvider` + `ThemeToggle` (`src/components/theme/`): escuro, claro ou sistema, guardado em `localStorage` (`rota-theme`).
`themeScript` roda no `<head>` e aplica a classe `dark` antes da pintura (sem piscar). Sem preferência, vale o escuro.

## Componentes-base
`ui/`: `Button`, `Card` (+Header/Title/Description), `Badge` (tons neutral, brand, success, warning, danger, info), `Skeleton`, `Progress`.
`system/`: `PageHeader`, `EmptyState` (sempre com ação útil), `ErrorState` (alerta + tentar de novo).

## Pendências desta fase
Tabs, tooltip, dialog/sheet, toast e paleta de comandos (entram com o shell, Fase 3); a troca do verde nos componentes antigos
(`text-brand`/`bg-brand` continuam funcionando e já mudam de cor com o tema).
