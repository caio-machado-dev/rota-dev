import type { Metadata } from "next";

import { DiagnosisPanel } from "@/components/plan/diagnosis-panel";
import { GoalForm } from "@/components/plan/goal-form";

export const metadata: Metadata = { title: "Plano" };

export default function PlanPage() {
  return (
    <section className="max-w-3xl">
      <h1 className="text-2xl font-semibold tracking-tight">Plano</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        Comece pelo objetivo: aonde você quer chegar, até quando e com quantas horas por semana.
      </p>
      <h2 className="mt-8 text-sm font-semibold">Objetivo de carreira</h2>
      <div className="mt-3">
        <GoalForm />
      </div>
      <h2 className="mt-12 text-sm font-semibold">Diagnóstico</h2>
      <p className="mt-1 text-sm text-muted-foreground">
        Onde você está hoje em cada competência do cargo-alvo, sempre com a evidência que sustenta a nota.
      </p>
      <div className="mt-4">
        <DiagnosisPanel />
      </div>
    </section>
  );
}
