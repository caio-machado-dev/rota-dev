"use client";

import { Legend, PolarAngleAxis, PolarGrid, PolarRadiusAxis, Radar, RadarChart, ResponsiveContainer, Tooltip } from "recharts";

import type { Diagnosis } from "@/lib/api/client";

type Axis = Diagnosis["axes"][number];

/** Quebra o nome do eixo em linhas de até ~12 caracteres, para não cortar na tela do celular. */
function wrap(text: string, max = 12): string[] {
  const lines: string[] = [];
  for (const word of text.split(" ")) {
    const last = lines.at(-1);
    if (last !== undefined && `${last} ${word}`.length <= max) lines[lines.length - 1] = `${last} ${word}`;
    else lines.push(word);
  }
  return lines;
}

type TickProps = { x?: number | string; y?: number | string; textAnchor?: "start" | "middle" | "end" | "inherit"; payload?: { value: string } };

function ChartTooltip({ active, payload, label }: { active?: boolean; payload?: { name: string; value: number }[]; label?: string }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-card border border-border bg-popover px-3 py-2 text-small shadow-raised">
      <p className="font-medium">{label}</p>
      {payload.map((p) => (
        <p key={p.name} className="num text-muted-foreground">
          {p.name}: {p.value} de 5
        </p>
      ))}
    </div>
  );
}

/**
 * Radar de competências: nota atual × exigida pelo cargo-alvo (0 a 5). O eixo selecionado fica em destaque;
 * clicar no nome do eixo seleciona. A lista de competências ao lado é a alternativa por teclado e por texto,
 * e a tabela oculta traz os mesmos números para leitores de tela. "Exigido" é tracejado: não depende só da cor.
 */
export function SkillRadar({ axes, selectedKey, onSelect }: { axes: Axis[]; selectedKey: string | null; onSelect: (key: string) => void }) {
  const data = axes.map((a) => ({ axis: a.name, Atual: a.score, Exigido: a.required }));
  const keyByName = new Map(axes.map((a) => [a.name, a.key]));
  const selectedName = axes.find((a) => a.key === selectedKey)?.name;

  function AxisLabel({ x = 0, y = 0, textAnchor = "middle", payload }: TickProps) {
    const name = payload?.value ?? "";
    const lines = wrap(name);
    const offset = Number(y) < 150 ? -(lines.length - 1) * 14 : 0;
    const selected = name === selectedName;
    return (
      <text
        x={x}
        y={Number(y) + offset}
        textAnchor={textAnchor}
        fill={selected ? "var(--foreground)" : "var(--muted-foreground)"}
        fontSize={12}
        fontWeight={selected ? 600 : 400}
        className="cursor-pointer"
        onClick={() => {
          const key = keyByName.get(name);
          if (key) onSelect(key);
        }}
      >
        {lines.map((line, i) => (
          <tspan key={i} x={x} dy={i === 0 ? 0 : 14}>
            {line}
          </tspan>
        ))}
      </text>
    );
  }

  return (
    <figure aria-label="Radar de competências: nota atual e nota exigida, de 0 a 5" className="h-80 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <RadarChart data={data} outerRadius="62%">
          <PolarGrid stroke="var(--border)" />
          <PolarAngleAxis dataKey="axis" tick={AxisLabel} />
          <PolarRadiusAxis domain={[0, 5]} tickCount={6} tick={false} axisLine={false} />
          <Radar name="Exigido" dataKey="Exigido" stroke="var(--viz-required)" strokeWidth={2} strokeDasharray="6 4" fill="none" />
          <Radar name="Atual" dataKey="Atual" stroke="var(--viz-current)" strokeWidth={2} fill="var(--viz-current)" fillOpacity={0.3} />
          <Legend wrapperStyle={{ fontSize: 12 }} formatter={(value: string) => <span style={{ color: "var(--muted-foreground)" }}>{value}</span>} />
          <Tooltip content={<ChartTooltip />} />
        </RadarChart>
      </ResponsiveContainer>
      <table className="sr-only">
        <caption>Notas por competência</caption>
        <thead>
          <tr>
            <th>Competência</th>
            <th>Atual</th>
            <th>Exigido</th>
          </tr>
        </thead>
        <tbody>
          {axes.map((a) => (
            <tr key={a.key}>
              <td>{a.name}</td>
              <td>{a.score}</td>
              <td>{a.required}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}
