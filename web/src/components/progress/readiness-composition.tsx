import { Progress } from "@/components/ui/progress";
import type { Readiness } from "@/lib/api/client";
import { cn } from "@/lib/utils";

/** Pesos do índice, como definidos no produto (docs/glossario.md). */
export const READINESS_WEIGHTS = [
  { key: "skills", label: "Habilidades", weight: 40 },
  { key: "gates", label: "Portões", weight: 25 },
  { key: "study", label: "Estudo", weight: 20 },
  { key: "habit", label: "Hábito", weight: 15 },
] as const;

/** Mostra de onde vem a nota: cada componente, seu peso e quantos pontos contribui para o total. */
export function ReadinessComposition({ current, className }: { current: Readiness["current"]; className?: string }) {
  return (
    <ul className={cn("space-y-3", className)} aria-label="Composição do índice">
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
  );
}
