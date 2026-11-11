import { cn } from "@/lib/utils";

/** Placeholder de carga com o formato do conteúdo real. Pulsa; `prefers-reduced-motion` desliga. */
export function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return <div aria-hidden data-slot="skeleton" className={cn("animate-pulse rounded-control bg-muted", className)} {...props} />;
}
