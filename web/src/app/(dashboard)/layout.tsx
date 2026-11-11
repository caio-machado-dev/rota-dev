import { AppShell } from "@/components/shell/app-shell";

/** Todas as telas internas (Agora, Plano, Progresso, Perfil, Configurações) dividem o mesmo shell. */
export default function DashboardLayout({ children }: LayoutProps<"/">) {
  return <AppShell>{children}</AppShell>;
}
