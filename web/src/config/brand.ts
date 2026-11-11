/**
 * Fonte única da marca (ADR 0012). Nenhum outro arquivo escreve o nome ou as cores da marca.
 * As cores viram variáveis CSS no layout raiz; o `globals.css` escolhe o par certo para cada tema.
 * O verde-limão brilha no escuro mas some no fundo branco, por isso o tema claro usa um verde mais fechado.
 */
export const brand = {
  name: "Rota Dev",
  slug: "rota-dev",
  tagline: "Seu plano de carreira, baseado no que você já construiu.",
  colors: {
    dark: {
      primary: "oklch(0.88 0.2 128)",
      primaryForeground: "oklch(0.22 0.05 128)",
    },
    light: {
      primary: "oklch(0.5 0.15 135)",
      primaryForeground: "oklch(0.99 0 0)",
    },
  },
} as const;
