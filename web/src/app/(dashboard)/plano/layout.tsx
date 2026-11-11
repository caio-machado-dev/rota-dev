import { PageHeader } from "@/components/system/page-header";
import { SectionNav } from "@/components/system/section-nav";

const links = [
  { href: "/plano", label: "Objetivo" },
  { href: "/plano/diagnostico", label: "Diagnóstico" },
  { href: "/plano/roadmap", label: "Roadmap" },
  { href: "/plano/versoes", label: "Versões" },
];

export default function PlanLayout({ children }: LayoutProps<"/plano">) {
  return (
    <>
      <PageHeader title="Plano" description="Um plano dinâmico, que se adapta ao que você já construiu." />
      <SectionNav links={links} label="Seções do plano" />
      {children}
    </>
  );
}
