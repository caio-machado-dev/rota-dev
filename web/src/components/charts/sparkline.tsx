import { cn } from "@/lib/utils";

/**
 * Mini gráfico de tendência em SVG puro. Responde "para onde isso vem indo?" sem eixos.
 * `label` descreve a série para leitores de tela (a cor nunca é a única informação: o valor
 * atual deve estar em texto ao lado). Com menos de 2 pontos não desenha nada.
 */
export function Sparkline({
  values,
  label,
  className,
  width = 120,
  height = 36,
}: {
  values: number[];
  label: string;
  className?: string;
  width?: number;
  height?: number;
}) {
  if (values.length < 2) return null;
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const pad = 3;
  const points = values.map((v, i) => {
    const x = pad + (i / (values.length - 1)) * (width - pad * 2);
    const y = height - pad - ((v - min) / span) * (height - pad * 2);
    return [x, y] as const;
  });
  const path = points.map(([x, y], i) => `${i === 0 ? "M" : "L"}${x.toFixed(1)} ${y.toFixed(1)}`).join(" ");
  const [lx, ly] = points[points.length - 1];

  return (
    <svg
      role="img"
      aria-label={`${label}: de ${values[0]} para ${values[values.length - 1]}`}
      viewBox={`0 0 ${width} ${height}`}
      className={cn("h-9 w-30 text-brand", className)}
    >
      <path d={path} fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={lx} cy={ly} r="2.5" fill="currentColor" />
    </svg>
  );
}
