import { Check } from "lucide-react";

import { cn } from "@/lib/utils";

/** Indicador das etapas do onboarding. A etapa atual leva `aria-current="step"`; as concluídas ganham o visto. */
export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex items-center gap-2" aria-label="Etapas do onboarding">
      {steps.map((label, index) => {
        const done = index < current;
        const active = index === current;
        return (
          <li key={label} aria-current={active ? "step" : undefined} className="flex flex-1 items-center gap-2 last:flex-none">
            <span
              className={cn(
                "num grid size-7 shrink-0 place-items-center rounded-full border text-small font-medium transition-colors duration-200",
                done && "border-brand bg-brand text-brand-foreground",
                active && "border-brand text-brand",
                !done && !active && "border-border text-muted-foreground",
              )}
            >
              {done ? <Check className="size-4" strokeWidth={2.5} aria-hidden /> : index + 1}
            </span>
            <span className={cn("hidden text-small sm:inline", active ? "font-medium text-foreground" : "text-muted-foreground")}>{label}</span>
            {index < steps.length - 1 ? <span aria-hidden className={cn("h-px flex-1 transition-colors duration-200", done ? "bg-brand" : "bg-border")} /> : null}
          </li>
        );
      })}
    </ol>
  );
}
