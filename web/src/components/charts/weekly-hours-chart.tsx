"use client";

import { Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";

import { duration } from "@/lib/format";

import { ChartTooltip } from "./chart-tooltip";

const shortDate = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

/** Horas estudadas por semana contra a meta (linha tracejada). Responde: "estou cumprindo o orçamento de horas?". */
export function WeeklyHoursChart({ weeks, budgetMinutes }: { weeks: { weekStart: string; minutes: number }[]; budgetMinutes?: number | null }) {
  const data = weeks.map((w) => ({ week: shortDate.format(new Date(`${w.weekStart}T12:00:00`)), Horas: Math.round((w.minutes / 60) * 10) / 10 }));
  const budgetHours = budgetMinutes ? budgetMinutes / 60 : null;
  return (
    <figure aria-label="Horas estudadas por semana" className="h-56 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis dataKey="week" tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} interval="preserveStartEnd" />
          <YAxis domain={[0, (dataMax: number) => Math.max(Math.ceil(dataMax), Math.ceil(budgetHours ?? 0))]} tick={{ fill: "var(--muted-foreground)", fontSize: 12 }} axisLine={false} tickLine={false} allowDecimals={false} />
          <Tooltip cursor={{ fill: "var(--accent)" }} content={<ChartTooltip title={(l) => `Semana de ${l}`} format={(v) => duration(Math.round(Number(v) * 60))} />} />
          {budgetHours ? <ReferenceLine y={budgetHours} stroke="var(--viz-required)" strokeDasharray="6 4" label={{ value: "meta", fill: "var(--muted-foreground)", fontSize: 11, position: "insideTopRight" }} /> : null}
          <Bar isAnimationActive={false} dataKey="Horas" fill="var(--chart-1)" radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
      <div className="sr-only">
        <table>
        <caption>Horas por semana</caption>
        <tbody>
          {data.map((d) => (
            <tr key={d.week}>
              <td>{d.week}</td>
              <td>{d.Horas}</td>
            </tr>
          ))}
        </tbody>
      </table>
      </div>
    </figure>
  );
}
