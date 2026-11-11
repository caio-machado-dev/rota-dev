/** Tooltip padrão dos gráficos (Recharts): superfície do popover, números alinhados. `format` ajusta cada valor. */
export function ChartTooltip({
  active,
  payload,
  label,
  format = (v) => String(v),
  title,
}: {
  active?: boolean;
  payload?: { name?: string; value?: number | string; color?: string }[];
  label?: string | number;
  format?: (value: number | string, name?: string) => string;
  title?: (label: string | number) => string;
}) {
  if (!active || !payload?.length) return null;
  return (
    <div className="rounded-card border border-border bg-popover px-3 py-2 text-small shadow-raised">
      <p className="font-medium">{title && label !== undefined ? title(label) : label}</p>
      {payload.map((p) => (
        <p key={p.name} className="num flex items-center gap-2 text-muted-foreground">
          <span aria-hidden className="size-2 rounded-full" style={{ background: p.color }} />
          {p.name}: {format(p.value ?? 0, p.name)}
        </p>
      ))}
    </div>
  );
}
