"use client";

import { useMemo } from "react";

import { Skeleton } from "@/components/ui/skeleton";
import { usePlanVersion } from "@/lib/api/hooks";
import { diffPlans } from "@/lib/domain/plan-diff";

/** Diferença entre duas versões do plano (por id), com linhas removidas em vermelho e novas em verde. Calculada no cliente. */
export function PlanDiff({ fromId, toId }: { fromId: string; toId: string }) {
  const { data: from } = usePlanVersion(fromId);
  const { data: to } = usePlanVersion(toId);
  const diff = useMemo(() => (from?.content && to?.content ? diffPlans(from.content, to.content) : null), [from, to]);

  if (!from || !to) return <Skeleton className="h-32" />;
  if (!diff) return <p className="text-small text-muted-foreground">Uma das versões não tem conteúdo para comparar (falhou ou ainda está sendo gerada).</p>;
  if (diff.length === 0) return <p className="text-small text-muted-foreground">As duas versões têm o mesmo conteúdo.</p>;

  return (
    <div className="space-y-5" aria-label={`Diferenças de v${from.version} para v${to.version}`}>
      {diff.map((section) => (
        <section key={section.section}>
          <h4 className="mb-1.5 text-small font-medium text-muted-foreground">{section.section}</h4>
          <ul className="overflow-hidden rounded-control border border-border font-mono text-small">
            {section.removed.map((line) => (
              <li key={`-${line}`} className="flex gap-2 bg-destructive/10 px-3 py-1">
                <span aria-label="removido" className="text-destructive">−</span>
                <span className="min-w-0 break-words">{line}</span>
              </li>
            ))}
            {section.added.map((line) => (
              <li key={`+${line}`} className="flex gap-2 bg-success/10 px-3 py-1">
                <span aria-label="adicionado" className="text-success">+</span>
                <span className="min-w-0 break-words">{line}</span>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
