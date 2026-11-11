"use client";

import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import type { Diagnosis } from "@/lib/api/client";

import { ChartTooltip } from "./chart-tooltip";

/** Nota atual × exigida por competência (0 a 5). Responde: "quão longe estou do cargo-alvo em cada área?". */
export function GoalComparison({ axes }: { axes: Diagnosis["axes"] }) {
  const data = axes.map((a) => ({ axis: a.name, Atual: a.score, Exigido: a.required }));
  return (
    <figure aria-label="Nota atual e nota exigida por competência" style={{ height: 40 + axes.length * 44 }} className="w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical" margin={{ top: 0, right: 8, bottom: 0, left: 0 }} barGap={2}>
          <CartesianGrid stroke="var(--border)" horizontal={false} />
          <XAxis type="number" domain={[0, 5]} ticks={[0, 1, 2, 3, 4, 5]} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
          <YAxis type="category" dataKey="axis" width={130} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} />
          <Tooltip cursor={{ fill: "var(--accent)" }} content={<ChartTooltip format={(v) => `${v} de 5`} />} />
          <Legend wrapperStyle={{ fontSize: 12 }} formatter={(v: string) => <span style={{ color: "var(--muted-foreground)" }}>{v}</span>} />
          <Bar isAnimationActive={false} dataKey="Atual" fill="var(--viz-current)" radius={[0, 3, 3, 0]} barSize={10} />
          <Bar isAnimationActive={false} dataKey="Exigido" fill="var(--viz-required)" radius={[0, 3, 3, 0]} barSize={10} />
        </BarChart>
      </ResponsiveContainer>
      <div className="sr-only">
        <table>
        <caption>Notas por competência</caption>
        <tbody>
          {data.map((d) => (
            <tr key={d.axis}>
              <td>{d.axis}</td>
              <td>{d.Atual}</td>
              <td>{d.Exigido}</td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </figure>
  );
}
