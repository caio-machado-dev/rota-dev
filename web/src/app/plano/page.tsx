import type { Metadata } from "next";

import { Placeholder } from "@/components/shell/placeholder";

export const metadata: Metadata = { title: "Plano" };

export default function PlanPage() {
  return (
    <Placeholder title="Plano" phase="Fase 2">
      Roadmap em trimestres e semanas, portões e projeto-âncora, gerados a partir do seu diagnóstico.
    </Placeholder>
  );
}
