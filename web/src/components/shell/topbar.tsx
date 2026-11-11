"use client";

import { ChevronRight, Loader2, Search } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";

import { LogoMark } from "@/components/brand/logo";
import { Badge } from "@/components/ui/badge";
import { brand } from "@/config/brand";
import { breadcrumb } from "@/config/navigation";
import { useJobs } from "@/lib/api/hooks";

/** Mostra o que está sendo gerado em segundo plano (diagnóstico, plano). Some quando não há nada rodando. */
export function JobIndicator() {
  const jobs = useJobs();
  if (jobs.length === 0) return null;
  const labels = jobs.map((job) => job.label).join(", ");
  return (
    <Badge tone="brand" role="status" title={labels}>
      <Loader2 className="animate-spin" strokeWidth={1.5} aria-hidden />
      <span className="hidden sm:inline">{labels}</span>
      <span className="sr-only sm:hidden">{labels}</span>
      <span aria-hidden className="sm:hidden">{jobs.length}</span>
    </Badge>
  );
}

/** Topo: migalhas de pão, busca/paleta (⌘K) e jobs. No celular vira um cabeçalho compacto. */
export function Topbar({ onOpenPalette }: { onOpenPalette: () => void }) {
  const crumbs = breadcrumb(usePathname());
  const current = crumbs[crumbs.length - 1];

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-border bg-background/90 px-4 backdrop-blur md:px-8">
      <Link href="/" aria-label="Ir para Agora" className="md:hidden">
        <LogoMark className="size-7" />
      </Link>

      <nav aria-label="Você está em" className="min-w-0 flex-1">
        <ol className="flex items-center gap-1.5 text-small">
          {crumbs.map((crumb, index) => {
            const last = index === crumbs.length - 1;
            return (
              <li key={crumb.href} className={last ? "min-w-0" : "hidden items-center gap-1.5 md:flex"}>
                {last ? (
                  <span aria-current="page" className="truncate font-medium text-foreground">
                    {crumb.label}
                  </span>
                ) : (
                  <>
                    <Link href={crumb.href} className="text-muted-foreground hover:text-foreground">
                      {crumb.label}
                    </Link>
                    <ChevronRight className="size-3.5 text-muted-foreground" strokeWidth={1.5} aria-hidden />
                  </>
                )}
              </li>
            );
          })}
        </ol>
        <span className="sr-only">{current.label}</span>
      </nav>

      <JobIndicator />

      <button
        type="button"
        onClick={onOpenPalette}
        aria-label="Buscar e comandos"
        className="flex h-8 items-center gap-2 rounded-control border border-input bg-surface px-2.5 text-small text-muted-foreground transition-colors outline-none hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring md:w-64"
      >
        <Search className="size-4" strokeWidth={1.5} aria-hidden />
        <span className="hidden flex-1 text-left md:inline">Buscar no {brand.name}…</span>
        <kbd className="num hidden rounded-[4px] border border-border px-1.5 text-micro md:inline">⌘K</kbd>
      </button>
    </header>
  );
}
