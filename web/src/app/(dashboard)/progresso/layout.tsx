import { PageHeader } from "@/components/system/page-header";
import { SectionNav } from "@/components/system/section-nav";

const links = [
  { href: "/progresso", label: "Visão geral" },
  { href: "/progresso/checkin", label: "Check-in" },
  { href: "/progresso/portoes", label: "Portões" },
  { href: "/progresso/reajustes", label: "Reajustes" },
];

export default function ProgressLayout({ children }: LayoutProps<"/progresso">) {
  return (
    <>
      <PageHeader title="Progresso" description="Quão perto do cargo-alvo você está e o que mudou a cada semana." />
      <SectionNav links={links} label="Seções do progresso" />
      {children}
    </>
  );
}
