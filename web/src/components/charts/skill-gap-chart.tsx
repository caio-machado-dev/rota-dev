import type { Diagnosis } from "@/lib/api/client";

type Gap = Diagnosis["gaps"][number];

/**
 * Lacunas por prioridade, em barras de 0 a 5 (quanto falta para o exigido). Cada linha traz o número em
 * texto: a barra reforça, não substitui. Responde "o que devo atacar primeiro?".
 */
export function SkillGapChart({ gaps }: { gaps: Gap[] }) {
  return (
    <ol className="space-y-4" aria-label="Lacunas por prioridade">
      {[...gaps]
        .sort((a, b) => a.priority - b.priority)
        .map((g) => (
          <li key={g.axisKey}>
            <div className="mb-1 flex items-baseline justify-between gap-3 text-small">
              <span>
                <span className="num text-muted-foreground">{g.priority}. </span>
                <span className="font-medium">{g.axisName}</span>
              </span>
              <span className="num text-muted-foreground">
                faltam {g.size} {g.size === 1 ? "ponto" : "pontos"}
              </span>
            </div>
            <div className="h-1.5 rounded-full bg-muted" role="img" aria-label={`${g.size} de 5 pontos de distância`}>
              <div className="h-full rounded-full bg-viz-required transition-[width] duration-200" style={{ width: `${Math.min(100, (g.size / 5) * 100)}%` }} />
            </div>
            <p className="mt-1 text-small text-muted-foreground">{g.description}</p>
          </li>
        ))}
    </ol>
  );
}
