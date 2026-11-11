import { ExternalLink } from "lucide-react";

import { cn } from "@/lib/utils";

/** Evidência que sustenta uma nota: link quando há URL; texto simples quando a leitura de origem não tem. */
export function EvidenceChip({ title, url, className }: { title?: string | null; url?: string | null; className?: string }) {
  const base = "inline-flex max-w-full items-center gap-1 rounded-full border border-border px-2.5 py-0.5 text-micro";
  if (url) {
    return (
      <a href={url} target="_blank" rel="noreferrer" className={cn(base, "text-foreground hover:bg-accent", className)}>
        <span className="truncate">{title ?? "evidência"}</span>
        <ExternalLink className="size-3 shrink-0 text-muted-foreground" strokeWidth={1.5} aria-label="abre em nova aba" />
      </a>
    );
  }
  return <span className={cn(base, "text-muted-foreground", className)}>{title ?? "evidência de uma leitura anterior"}</span>;
}

/** Item sem evidência: a regra do produto é dizer "Não verificado", nunca presumir. */
export function Unverified({ className }: { className?: string }) {
  return (
    <span className={cn("inline-flex items-center rounded-full border border-warning/30 bg-warning/10 px-2.5 py-0.5 text-micro text-warning", className)}>
      Não verificado
    </span>
  );
}
