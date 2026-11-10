import type { Metadata } from "next";

import { ProgressView } from "@/components/progress/progress-view";

export const metadata: Metadata = { title: "Progresso" };

export default function ProgressPage() {
  return (
    <section className="max-w-3xl">
      <h1 className="text-2xl font-semibold tracking-tight">Progresso</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Quão perto do cargo-alvo você está, como isso mudou a cada semana e os portões que já provou.
      </p>
      <div className="mt-8">
        <ProgressView />
      </div>
    </section>
  );
}
