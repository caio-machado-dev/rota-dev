import { CalendarCheck, ChartNoAxesCombined, Map, Settings, UserRound, type LucideIcon } from "lucide-react";

export type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
  /** Segunda tecla do atalho "g + tecla". */
  shortcut?: string;
};

/** Seções principais. A mesma lista alimenta a barra lateral (desktop), a inferior (celular) e a paleta de comandos. */
export const navItems: NavItem[] = [
  { href: "/", label: "Agora", icon: CalendarCheck, shortcut: "a" },
  { href: "/plano", label: "Plano", icon: Map, shortcut: "p" },
  { href: "/progresso", label: "Progresso", icon: ChartNoAxesCombined, shortcut: "r" },
  { href: "/perfil", label: "Perfil", icon: UserRound, shortcut: "f" },
];

export const settingsItem: NavItem = { href: "/configuracoes", label: "Configurações", icon: Settings };

/** Nomes das rotas para o breadcrumb (primeiro nível e subrotas). */
const segmentLabels: Record<string, string> = {
  plano: "Plano",
  progresso: "Progresso",
  perfil: "Perfil",
  configuracoes: "Configurações",
  diagnostico: "Diagnóstico",
  roadmap: "Roadmap",
  versoes: "Versões",
  checkin: "Check-in",
  portoes: "Portões",
  reajustes: "Reajustes",
  encontramos: "O que encontramos",
  dados: "Meus dados",
};

export type Crumb = { href: string; label: string };

/** Migalhas de pão a partir da rota. A raiz é "Agora". */
export function breadcrumb(pathname: string): Crumb[] {
  const segments = pathname.split("/").filter(Boolean);
  if (segments.length === 0) return [{ href: "/", label: "Agora" }];
  return segments.map((segment, index) => ({
    href: "/" + segments.slice(0, index + 1).join("/"),
    label: segmentLabels[segment] ?? segment,
  }));
}

/** "/" só é ativo na raiz; as demais seções também ficam ativas nas subpáginas. */
export function isActive(pathname: string, href: string): boolean {
  return href === "/" ? pathname === "/" : pathname === href || pathname.startsWith(`${href}/`);
}
