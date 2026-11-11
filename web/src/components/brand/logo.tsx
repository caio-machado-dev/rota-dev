import { brand } from "@/config/brand";
import { cn } from "@/lib/utils";

/**
 * Símbolo do Rota Dev: um "R" desenhado como trajeto. Parte de um ponto (onde você está),
 * sobe pelo traço e fecha a curva na direção do próximo marco. Usa `currentColor`, então serve
 * em fundo escuro, claro e na versão monocromática. O ponto de partida leva a cor da marca,
 * exceto em `mono`.
 */
export function LogoMark({ className, mono = false }: { className?: string; mono?: boolean }) {
  return (
    <svg viewBox="0 0 32 32" fill="none" aria-hidden className={cn("size-6", className)}>
      <path
        d="M9 24V8h7.5a5 5 0 0 1 0 10H9m7.5 0L23 24"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="9" cy="24" r="2.75" className={mono ? "fill-current" : "fill-brand"} />
    </svg>
  );
}

/** Logotipo horizontal: símbolo + nome (o nome vem de `brand.ts`). */
export function Logo({ className, mono = false, showName = true }: { className?: string; mono?: boolean; showName?: boolean }) {
  return (
    <span className={cn("inline-flex items-center gap-2 text-foreground", className)}>
      <LogoMark mono={mono} />
      {showName ? <span className="text-h3 font-semibold tracking-tight">{brand.name}</span> : <span className="sr-only">{brand.name}</span>}
    </span>
  );
}
