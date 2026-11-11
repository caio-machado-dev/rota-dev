# Acessibilidade (WCAG 2.2 AA como referência)

## Verificação automática
`axe-core` (tags wcag2a, wcag2aa, wcag21aa, wcag22aa, best-practice) em 15 rotas × tema escuro/claro × desktop/mobile, com respostas da API simuladas só no
navegador de teste: **0 violações** na última execução. Pendente de verificação manual: leitor de tela real (VoiceOver/NVDA), diálogos abertos e estados de erro no axe.

## Decisões
- **Contraste:** tokens de status no tema claro escurecidos (`success` 0.41, `warning` 0.44, `info` 0.43 em OKLCH) para ≥ 4.5:1 em texto sobre fundo tingido.
  Texto secundário (`muted-foreground`) ≥ 4.5:1 nos dois temas. Cores de série dos gráficos nunca carregam texto.
- **Cor nunca é a única informação:** badges trazem texto; "exigido" é tracejado no radar; heatmap tem `title` por dia; gauge tem o número no centro.
- **Teclado:** foco visível (anel da marca) em todos os controles; link "Pular para o conteúdo"; atalhos sem Ctrl/⌘ não disparam em campos de texto;
  listas de competências e portões são botões/links reais; diálogos prendem o foco e devolvem ao gatilho (base-ui).
- **ARIA e semântica:** `header`, `nav` (com `aria-label` distinto), `main#conteudo`, `aside`; `aria-current="page"` na navegação e `"step"` na etapa atual;
  `role="status"`/`aria-live="polite"` em jobs e toasts; `role="alert"` em erros; `role="progressbar"` com `aria-valuenow/max`; `radiogroup` para
  tema, energia e tipo de sessão.
- **Gráficos:** `figure` com `aria-label` + tabela oculta (`sr-only`) com os mesmos números; sparkline e gauge com `role="img"` descritivo.
- **Formulários:** todo campo tem rótulo visível; erros em `role="alert"` e valores preservados; ação destrutiva exige diálogo e login digitado.
- **Movimento:** transições de 150–250 ms; `prefers-reduced-motion` reduz animações e transições a ~0 globalmente (`globals.css`).
