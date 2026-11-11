"use client";

import { useState } from "react";
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import type { Readiness } from "@/lib/api/client";
import { cn } from "@/lib/utils";

import { ChartTooltip } from "./chart-tooltip";

const SERIES = [
  { key: "score", label: "Índice", color: "var(--chart-1)" },
  { key: "skills", label: "Habilidades", color: "var(--chart-2)" },
  { key: "gates", label: "Portões", color: "var(--chart-3)" },
  { key: "study", label: "Estudo", color: "var(--chart-4)" },
  { key: "habit", label: "Hábito", color: "var(--chart-5)" },
] as const;
type SeriesKey = (typeof SERIES)[number]["key"];

const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

/** Evolução semanal do índice e de cada componente (habilidades, portões, estudo, hábito). Responde: "estou melhorando, e em quê?". */
export function ReadinessTrend({ history }: { history: Readiness["history"] }) {
  const [active, setActive] = useState<SeriesKey[]>(["score"]);
  const data = [...history]
    .sort((a, b) => a.weekStart.localeCompare(b.weekStart))
    .map((p) => ({ week: shortDate.format(new Date(`${p.weekStart}T12:00:00`)), weekStart: p.weekStart, ...p.value }));

  if (data.length < 2) return <p className="py-10 text-center text-small text-muted-foreground">A linha do tempo aparece a partir do segundo check-in.</p>;

  const toggle = (key: SeriesKey) => setActive((cur) => (cur.includes(key) ? (cur.length > 1 ? cur.filter((k) => k !== key) : cur) : [...cur, key]));

  return (
    <div>
      <div role="group" aria-label="Séries do gráfico" className="mb-3 flex flex-wrap gap-2">
        {SERIES.map((s) => (
          <button
            key={s.key}
            type="button"
            aria-pressed={active.includes(s.key)}
            onClick={() => toggle(s.key)}
            className={cn("flex items-center gap-1.5 rounded-full border border-input px-2.5 py-1 text-micro transition-colors hover:bg-accent", active.includes(s.key) && "bg-accent")}
          >
            <span aria-hidden className="size-2 rounded-full" style={{ background: s.color }} />
            {s.label}
          </button>
        ))}
      </div>
      <figure aria-label="Evolução semanal do índice de prontidão" className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
            <CartesianGrid stroke="var(--border)" vertical={false} />
            <XAxis dataKey="week" tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
            <YAxis domain={[0, 100]} ticks={[0, 50, 100]} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
            <Tooltip content={<ChartTooltip title={(l) => `Semana de ${l}`} />} />
            {SERIES.filter((s) => active.includes(s.key)).map((s) => (
              <Line isAnimationActive={false} key={s.key} type="monotone" name={s.label} dataKey={s.key} stroke={s.color} strokeWidth={2} dot={{ r: 3 }} activeDot={{ r: 5 }} />
            ))}
          </LineChart>
        </ResponsiveContainer>
        <div className="sr-only">
        <table>
          <caption>Índice por semana</caption>
          <thead>
            <tr>
              <th>Semana</th>
              {SERIES.map((s) => (
                <th key={s.key}>{s.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((p) => (
              <tr key={p.weekStart}>
                <td>{p.weekStart}</td>
                {SERIES.map((s) => (
                  <td key={s.key}>{p[s.key]}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      </figure>
    </div>
  );
}
