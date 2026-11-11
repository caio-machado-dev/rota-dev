import Link from "next/link";

import { Sparkline } from "@/components/charts/sparkline";
import { Badge } from "@/components/ui/badge";
import { Card, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import type { Readiness } from "@/lib/api/client";
import { readinessBand, readinessDelta } from "@/lib/format";

/** Pesos do índice, como definidos no produto (docs/glossario.md). */
export const READINESS_WEIGHTS = [
  { key: "skills", label: "Habilidades", weight: 40 },
  { key: "gates", label: "Portões", weight: 25 },
  { key: "study", label: "Estudo", weight: 20 },
  { key: "habit", label: "Hábito", weight: 15 },
] as const;

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

      <ul className="mt-6 space-y-3" aria-label="Composição do índice">
        {READINESS_WEIGHTS.map(({ key, label, weight }) => {
          const value = current[key];
          return (
            <li key={key}>
              <div className="mb-1 flex justify-between text-small">
                <span>
                  {label} <span className="text-muted-foreground">· peso {weight}%</span>
                </span>
                <span className="num text-muted-foreground">
                  {value} → {((value * weight) / 100).toFixed(1)} pts
                </span>
              </div>
              <Progress label={`${label}: ${value} de 100`} value={value} />
            </li>
          );
        })}
      </ul>
      <Link href="/progresso" className="mt-4 inline-block text-small text-brand hover:underline">
        Ver evolução completa
      </Link>
    </Card>
  );
}
