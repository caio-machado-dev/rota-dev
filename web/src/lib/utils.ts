import { clsx, type ClassValue } from "clsx";
import { extendTailwindMerge } from "tailwind-merge";

/**
 * Junta classes e resolve conflitos do Tailwind (a última vence). O merge precisa conhecer os nomes
 * do nosso design system: sem isso, `text-small` seria tratado como cor e apagado por `text-muted-foreground`.
 */
const twMerge = extendTailwindMerge({
  extend: {
    theme: {
      text: ["display", "h1", "h2", "h3", "body", "small", "micro"],
      radius: ["control", "card", "modal"],
      shadow: ["surface", "raised", "overlay"],
    },
  },
});

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
