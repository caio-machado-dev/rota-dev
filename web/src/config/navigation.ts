import { CalendarCheck, ChartNoAxesCombined, Map, UserRound, type LucideIcon } from "lucide-react";

export type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
};

/** Seções principais. A mesma lista alimenta a barra lateral (desktop) e a inferior (celular). */
export const navItems: NavItem[] = [
  { href: "/", label: "Agora", icon: CalendarCheck },
  { href: "/plano", label: "Plano", icon: Map },
  { href: "/progresso", label: "Progresso", icon: ChartNoAxesCombined },
  { href: "/perfil", label: "Perfil", icon: UserRound },
];

/** "/" só é ativo na raiz; as demais seções também ficam ativas nas subpáginas. */
export function isActive(pathname: string, href: string): boolean {
  return href === "/" ? pathname === "/" : pathname === href || pathname.startsWith(`${href}/`);
}
