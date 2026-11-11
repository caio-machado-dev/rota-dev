import { duration, KIND_LABEL } from "@/lib/format";

const COLORS = ["var(--chart-1)", "var(--chart-2)", "var(--chart-3)", "var(--chart-4)"];

/** Distribuição do tempo por tipo de sessão: barra empilhada + lista com os números em texto. */
export function SessionMix({ mix }: { mix: { kind: string; minutes: number }[] }) {
  const total = mix.reduce((sum, m) => sum + m.minutes, 0);
  if (total === 0) return null;
  return (
    <div>
      <div className="flex h-3 overflow-hidden rounded-full bg-muted" role="img" aria-label="Distribuição do tempo por tipo de sessão">
        {mix.map((m, i) => (
          <span key={m.kind} style={{ width: `${(m.minutes / total) * 100}%`, background: COLORS[i % COLORS.length] }} />
        ))}
      </div>
      <ul className="mt-4 space-y-2">
        {mix.map((m, i) => (
          <li key={m.kind} className="flex items-center gap-2 text-small">
            <span aria-hidden className="size-2.5 rounded-full" style={{ background: COLORS[i % COLORS.length] }} />
            <span className="flex-1">{KIND_LABEL[m.kind] ?? m.kind}</span>
            <span className="num text-muted-foreground">
              {duration(m.minutes)} · {Math.round((m.minutes / total) * 100)}%
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}
