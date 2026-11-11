# Responsividade

## Breakpoints (Tailwind)
- **Mobile** (< 768px, validado em 390px): cabeçalho compacto, navegação inferior com 4 abas (alvo de 56px, safe area), botão flutuante "Registrar sessão",
  uma coluna, sheets de baixo para cima.
- **Tablet/desktop** (≥ 768px, `md`): barra lateral fixa (compactável para 64px, preferência em `localStorage`), topbar com migalhas e busca com ⌘K.
- **Largo** (≥ 1024px, `lg`): grades de duas colunas (conteúdo + painel lateral de 22rem; radar + competências; consistência + mix).
- Largura útil: `max-w-[1320px]` centralizada; validado em 1440px.

## Regras
- Toda grade com colunas em `md:`/`lg:` declara `grid-cols-1` antes: sem isso a coluna única cresce com o conteúdo e gera rolagem horizontal (corrigido na Fase 8).
- Textos longos usam `truncate`/`break-words`; tabelas de dados só em `sr-only` (dentro de um `div` que corta).
- Gráficos usam `ResponsiveContainer`; rótulos de eixo quebram em linhas no radar; heatmap rola dentro do próprio card.
- Modais: `DialogContent placement="responsive"` = bottom sheet no celular e painel lateral no desktop; confirmações ficam centralizadas.
- Alvos de toque ≥ 40px nos controles principais; `min-h-11` nas linhas de checklist.

## Verificação
Cada tela foi capturada em Chromium (Playwright) em 1440×900 e 390×844, nos dois temas, checando `scrollWidth ≤ innerWidth` (overflow horizontal zero).
