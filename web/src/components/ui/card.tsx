import { cn } from "@/lib/utils";

/** Superfície base. Uma borda discreta e profundidade pela cor, sem sombra pesada. */
export function Card({ className, ...props }: React.ComponentProps<"section">) {
  return (
    <section
      data-slot="card"
      className={cn("rounded-card border border-border bg-card p-5 text-card-foreground shadow-surface", className)}
      {...props}
    />
  );
}

export function CardHeader({ className, ...props }: React.ComponentProps<"header">) {
  return <header className={cn("mb-4 flex items-start justify-between gap-3", className)} {...props} />;
}

export function CardTitle({ className, ...props }: React.ComponentProps<"h2">) {
  return <h2 className={cn("text-h3 font-semibold tracking-tight", className)} {...props} />;
}

export function CardDescription({ className, ...props }: React.ComponentProps<"p">) {
  return <p className={cn("text-small text-muted-foreground", className)} {...props} />;
}
