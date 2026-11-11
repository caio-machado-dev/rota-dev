import { cn } from "@/lib/utils";

/** Classes dos campos de formulário: altura 36px (toque confortável), borda discreta, foco com o anel da marca. */
export const fieldClass =
  "w-full rounded-control border border-input bg-background px-3 py-2 text-body text-foreground outline-none transition-colors placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-2 focus-visible:ring-ring/40 disabled:opacity-50 aria-invalid:border-destructive";

export function Input({ className, ...props }: React.ComponentProps<"input">) {
  return <input className={cn(fieldClass, "h-9", className)} {...props} />;
}

export function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return <textarea className={cn(fieldClass, "min-h-20 resize-y", className)} {...props} />;
}

export function Select({ className, ...props }: React.ComponentProps<"select">) {
  return <select className={cn(fieldClass, "h-9", className)} {...props} />;
}

/** Rótulo + campo empilhados. `hint` aparece abaixo, em texto de apoio. */
export function Field({
  label,
  hint,
  className,
  children,
}: {
  label: string;
  hint?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <label className={cn("block", className)}>
      <span className="mb-1.5 block text-small font-medium">{label}</span>
      {children}
      {hint ? <span className="mt-1 block text-micro text-muted-foreground">{hint}</span> : null}
    </label>
  );
}
