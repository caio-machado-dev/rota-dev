import type { HeatCell } from "@/lib/domain/consistency";
import { duration } from "@/lib/format";
import { cn } from "@/lib/utils";

const LEVEL_CLASS = ["bg-muted", "bg-brand/25", "bg-brand/50", "bg-brand/75", "bg-brand"] as const;
const WEEKDAYS = ["Seg", "", "Qua", "", "Sex", "", "Dom"];
const dayFormat = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "numeric", month: "long" });

/**
 * Heatmap de consistência: uma coluna por semana, uma linha por dia (segunda a domingo). Cada célula
 * descreve seu dia por texto (`title` e `aria-label`); a intensidade só reforça. Responde: "estou estudando com regularidade?".
 */
export function ConsistencyHeatmap({ grid }: { grid: (HeatCell | null)[][] }) {
  const cells = grid.flat().filter((c): c is HeatCell => c !== null);
  const active = cells.filter((c) => c.minutes > 0).length;
  return (
    <div>
      <div className="flex gap-2 overflow-x-auto pb-1">
        <div aria-hidden className="grid grid-rows-7 gap-1 pt-0 text-micro text-muted-foreground">
          {WEEKDAYS.map((label, i) => (
            <span key={i} className="flex h-4 items-center">
              {label}
            </span>
          ))}
        </div>
        <div role="img" aria-label={`Consistência: ${active} dias com estudo nas últimas ${grid.length} semanas`} className="flex gap-1">
          {grid.map((week, w) => (
            <div key={w} className="grid grid-rows-7 gap-1">
              {week.map((cell, d) =>
                cell ? (
                  <span
                    key={d}
                    title={`${dayFormat.format(new Date(`${cell.day}T12:00:00`))}: ${cell.minutes > 0 ? duration(cell.minutes) : "sem estudo"}`}
                    className={cn("size-4 rounded-[3px]", LEVEL_CLASS[cell.level])}
                  />
                ) : (
                  <span key={d} className="size-4" />
                ),
              )}
            </div>
          ))}
        </div>
      </div>
      <div className="mt-3 flex items-center justify-between gap-3 text-micro text-muted-foreground">
        <span className="num">
          {active} de {cells.length} dias com estudo
        </span>
        <span className="flex items-center gap-1" aria-hidden>
          menos
          {LEVEL_CLASS.map((c) => (
            <span key={c} className={cn("size-3 rounded-[2px]", c)} />
          ))}
          mais
        </span>
      </div>
    </div>
  );
}
