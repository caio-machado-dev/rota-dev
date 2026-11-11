"use client";

import { GitCommitHorizontal } from "lucide-react";
import { useState } from "react";

import { EmptyState, ErrorState } from "@/components/system/states";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { usePlanVersions } from "@/lib/api/hooks";
import { cn } from "@/lib/utils";

import { PlanDiff } from "./plan-diff-view";

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" });

/**
 * Aba "Versões": histórico no estilo `git log` (mais nova primeiro, com o motivo de cada versão) e comparação
 * entre duas versões, calculada no cliente. Restaurar uma versão antiga não existe na API, então não é oferecido.
 */
export function VersionsSection() {
  const { data: versions, error, isLoading, mutate } = usePlanVersions();
  const [selected, setSelected] = useState<string[]>([]);

  if (isLoading) return <Skeleton className="h-64" />;
  if (error) return <ErrorState message="Não consegui carregar o histórico." onRetry={() => void mutate()} />;
  if (!versions || versions.length === 0) {
    return (
      <Card>
        <EmptyState title="Nenhuma versão ainda" description="Cada plano gerado e cada reajuste aceito vira uma versão, com o motivo registrado." />
      </Card>
    );
  }

  const ordered = [...versions].sort((a, b) => b.version - a.version);
  const toggle = (id: string) => setSelected((cur) => (cur.includes(id) ? cur.filter((x) => x !== id) : [...cur.slice(-1), id]));
  const pair = selected.length === 2 ? selected.map((id) => versions.find((v) => v.id === id)!).sort((a, b) => a.version - b.version) : null;

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader>
          <div>
            <CardTitle>Histórico</CardTitle>
            <CardDescription>Marque duas versões para comparar o que mudou.</CardDescription>
          </div>
          <Button variant="outline" size="sm" disabled={selected.length === 0} onClick={() => setSelected([])}>
            Limpar seleção
          </Button>
        </CardHeader>
        <ol className="relative">
          {ordered.map((v, index) => {
            const checked = selected.includes(v.id);
            return (
              <li key={v.id} className="relative flex gap-4 pb-6 last:pb-0">
                {index < ordered.length - 1 ? <span aria-hidden className="absolute top-6 bottom-0 left-[11px] w-px bg-border" /> : null}
                <GitCommitHorizontal className={cn("relative z-10 mt-0.5 size-6 shrink-0 bg-card", index === 0 ? "text-brand" : "text-muted-foreground")} strokeWidth={1.5} aria-hidden />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="num font-mono text-body font-medium">v{v.version}</span>
                    <span className="font-mono text-micro text-muted-foreground">{v.id.slice(0, 7)}</span>
                    {index === 0 ? <Badge tone="brand">atual</Badge> : null}
                    {v.status !== "READY" ? <Badge tone={v.status === "FAILED" ? "danger" : "info"}>{v.status === "FAILED" ? "falhou" : "gerando"}</Badge> : null}
                  </div>
                  <p className="mt-0.5 text-body">{v.reason}</p>
                  <p className="text-small text-muted-foreground">{dateFormat.format(new Date(v.createdAt))}</p>
                </div>
                <label className="flex items-start gap-2 text-small text-muted-foreground">
                  <input
                    type="checkbox"
                    aria-label={`Comparar v${v.version}`}
                    checked={checked}
                    disabled={v.status !== "READY"}
                    onChange={() => toggle(v.id)}
                    className="mt-0.5 size-4 accent-[var(--brand)]"
                  />
                  Comparar
                </label>
              </li>
            );
          })}
        </ol>
      </Card>

      {pair ? (
        <Card>
          <CardHeader>
            <div>
              <CardTitle>
                Diferenças: v{pair[0].version} → v{pair[1].version}
              </CardTitle>
              <CardDescription>
                <span className="text-success">+</span> entrou na versão mais nova · <span className="text-destructive">−</span> saiu
              </CardDescription>
            </div>
          </CardHeader>
          <PlanDiff fromId={pair[0].id} toId={pair[1].id} />
        </Card>
      ) : null}
    </div>
  );
}
