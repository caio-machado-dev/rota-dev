import type { Metadata } from "next";

import { Placeholder } from "@/components/shell/placeholder";

export const metadata: Metadata = { title: "Progresso" };

export default function ProgressPage() {
  return (
    <Placeholder title="Progresso" phase="Fase 3">
      Índice de prontidão, radar de habilidades, horas por semana e linha do tempo.
    </Placeholder>
  );
}
