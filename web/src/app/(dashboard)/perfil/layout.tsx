import { PageHeader } from "@/components/system/page-header";
import { SectionNav } from "@/components/system/section-nav";

const links = [
  { href: "/perfil", label: "Fontes" },
  { href: "/perfil/encontramos", label: "O que encontramos" },
  { href: "/perfil/dados", label: "Meus dados" },
];

export default function ProfileLayout({ children }: LayoutProps<"/perfil">) {
  return (
    <>
      <PageHeader title="Perfil" description="O que as suas fontes mostram, sempre com a evidência ao lado. É a base do diagnóstico." />
      <SectionNav links={links} label="Seções do perfil" />
      {children}
    </>
  );
}
