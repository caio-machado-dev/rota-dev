import { cn } from "@/lib/utils";

/** Barra de progresso acessível (`role="progressbar"`). `value` e `max` em unidades reais, como minutos. */
export function Progress({
  value,
  max = 100,
  label,
  className,
}: {
  value: number;
  max?: number;
  /** Nome lido por leitores de tela, por exemplo "Meta semanal". */
  label: string;
  className?: string;
}) {
  const ratio = max > 0 ? Math.min(1, Math.max(0, value / max)) : 0;
  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={max}
      aria-valuenow={Math.min(value, max)}
      className={cn("h-1.5 w-full overflow-hidden rounded-full bg-muted", className)}
    >
      <div
        className="h-full rounded-full bg-brand transition-[width] duration-200 ease-out-soft"
        style={{ width: `${ratio * 100}%` }}
      />
    </div>
  );
}
