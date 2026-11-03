import type { Metadata } from "next";

import { Placeholder } from "@/components/shell/placeholder";

export const metadata: Metadata = { title: "Perfil" };

export default function ProfilePage() {
  return (
    <Placeholder title="Perfil" phase="Fase 1">
      Fontes conectadas (GitHub, LinkedIn, currículo) e revisão do perfil extraído.
    </Placeholder>
  );
}
