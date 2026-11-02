/**
 * Fonte única da marca (ADR 0012). Nenhum outro arquivo escreve o nome ou as cores da marca.
 * As cores viram variáveis CSS no layout raiz (`--brand`, `--brand-foreground`).
 */
export const brand = {
  name: "Rota Dev",
  slug: "rota-dev",
  tagline: "Seu plano de carreira, baseado no que você já construiu.",
  colors: {
    primary: "oklch(0.78 0.16 165)",
    primaryForeground: "oklch(0.2 0.03 165)",
  },
} as const;
