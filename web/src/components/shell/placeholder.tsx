/** Página provisória: marca onde cada funcionalidade vai entrar. */
export function Placeholder({ title, phase, children }: { title: string; phase: string; children?: React.ReactNode }) {
  return (
    <section className="max-w-2xl">
      <p className="font-mono text-xs uppercase tracking-widest text-muted-foreground">{phase}</p>
      <h1 className="mt-2 text-2xl font-semibold tracking-tight">{title}</h1>
      <div className="mt-3 text-sm leading-relaxed text-muted-foreground">{children}</div>
    </section>
  );
}
