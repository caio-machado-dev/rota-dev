import Link from "next/link";

import { Sparkline } from "@/components/charts/sparkline";
import { Badge } from "@/components/ui/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ReadinessComposition } from "@/components/progress/readiness-composition";
import type { Readiness } from "@/lib/api/client";
import { readinessBand, readinessDelta } from "@/lib/format";

/** O índice de prontidão com a origem da nota à vista: quanto cada componente contribui. */
export function ReadinessCard({ readiness }: { readiness: Readiness }) {
  const { current, history } = readiness;
  const delta = readinessDelta(history);
  const trend = [...history].sort((a, b) => a.weekStart.localeCompare(b.weekStart)).map((p) => p.value.score);

  return (
    <Card>
      <CardHeader>
        <div>
          <CardTitle>Prontidão</CardTitle>
          <CardDescription>{readinessBand(current.score)}</CardDescription>
        </div>
        {delta !== null ? (
          <Badge tone={delta > 0 ? "success" : delta < 0 ? "warning" : "neutral"} aria-label={`Variação desde o último ponto: ${delta}`}>
            {delta > 0 ? "+" : ""}
            {delta} pts
          </Badge>
        ) : null}
      </CardHeader>

      <div className="flex items-end justify-between gap-4">
        <p className="num text-display font-semibold leading-none tracking-tight">
          {current.score}
          <span className="ml-1 text-h3 font-normal text-muted-foreground">/100</span>
        </p>
        <Sparkline values={trend} label="Histórico semanal da prontidão" />
      </div>

      <ReadinessComposition current={current} className="mt-6" />
      <Link href="/progresso" className="mt-4 inline-block text-small text-brand hover:underline">
        Ver evolução completa
      </Link>
    </Card>
  );
}
