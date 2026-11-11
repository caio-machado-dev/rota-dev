"use client";

import { PanelLeftClose, PanelLeftOpen } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";

import { Logo, LogoMark } from "@/components/brand/logo";
import { Progress } from "@/components/ui/progress";
import { brand } from "@/config/brand";
import { isActive, navItems, settingsItem, type NavItem } from "@/config/navigation";
import { useReadiness } from "@/lib/api/hooks";
import { cn } from "@/lib/utils";

import { UserMenu } from "./user-menu";

const KEY = "rota-sidebar";

function NavLink({ item, collapsed, active }: { item: NavItem; collapsed: boolean; active: boolean }) {
  const { href, label, icon: Icon } = item;
  return (
    <Link
      href={href}
      title={collapsed ? label : undefined}
      aria-label={collapsed ? label : undefined}
      aria-current={active ? "page" : undefined}
      className={cn(
        "relative flex items-center gap-3 rounded-control px-3 py-2 text-small text-muted-foreground transition-colors duration-150 outline-none hover:bg-sidebar-accent hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring",
        collapsed && "justify-center px-0",
        active && "bg-sidebar-accent text-foreground",
      )}
    >
      {active ? <span aria-hidden className="absolute inset-y-1.5 left-0 w-0.5 rounded-full bg-brand" /> : null}
      <Icon className={cn("size-5 shrink-0", active && "text-brand")} strokeWidth={1.5} aria-hidden />
      {collapsed ? null : label}
    </Link>
  );
}

/** Mostra a prontidão atual em qualquer página. Some se ainda não há dado (sem sessão, por exemplo). */
function ReadinessIndicator({ collapsed }: { collapsed: boolean }) {
  const { data } = useReadiness();
  if (!data) return null;
  const score = data.current.score;
  if (collapsed) {
    return (
      <Link href="/progresso" title="Prontidão" aria-label={`Prontidão ${score} de 100`} className="num mx-auto block text-small font-semibold text-brand">
        {score}
      </Link>
    );
  }
  return (
    <Link href="/progresso" className="block rounded-card border border-border p-3 transition-colors hover:bg-sidebar-accent">
      <div className="flex items-baseline justify-between">
        <span className="text-micro text-muted-foreground">Prontidão</span>
        <span className="num text-h3 font-semibold">
          {score}
          <span className="text-micro font-normal text-muted-foreground">/100</span>
        </span>
      </div>
      <Progress label="Índice de prontidão" value={score} className="mt-2" />
    </Link>
  );
}

/** Barra lateral fixa (a partir de `md`), compactável. A preferência fica no navegador. */
export function Sidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    try {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- o storage só existe no navegador.
      setCollapsed(localStorage.getItem(KEY) === "1");
    } catch {
      /* sem storage: começa expandida */
    }
  }, []);

  function toggle() {
    setCollapsed((current) => {
      try {
        localStorage.setItem(KEY, current ? "0" : "1");
      } catch {
        /* vale só nesta visita */
      }
      return !current;
    });
  }

  return (
    <aside
      className={cn(
        "sticky top-0 hidden h-dvh shrink-0 flex-col border-r border-sidebar-border bg-sidebar py-5 transition-[width] duration-200 ease-out-soft md:flex",
        collapsed ? "w-16 px-2" : "w-60 px-3",
      )}
    >
      <Link href="/" aria-label={`${brand.name}, ir para Agora`} className={cn("mb-8 rounded-control outline-none focus-visible:ring-2 focus-visible:ring-ring", collapsed ? "mx-auto" : "px-3")}>
        {collapsed ? <LogoMark className="size-7" /> : <Logo />}
      </Link>

      <nav aria-label="Navegação principal" className="flex flex-col gap-1">
        {navItems.map((item) => (
          <NavLink key={item.href} item={item} collapsed={collapsed} active={isActive(pathname, item.href)} />
        ))}
      </nav>

      <div className="mt-auto flex flex-col gap-3">
        <ReadinessIndicator collapsed={collapsed} />
        <NavLink item={settingsItem} collapsed={collapsed} active={isActive(pathname, settingsItem.href)} />
        <div className="border-t border-sidebar-border pt-3">
          <UserMenu compact={collapsed} />
        </div>
        <button
          type="button"
          onClick={toggle}
          aria-label={collapsed ? "Expandir barra lateral" : "Recolher barra lateral"}
          className="flex items-center gap-3 rounded-control px-3 py-2 text-small text-muted-foreground transition-colors outline-none hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring"
        >
          {collapsed ? <PanelLeftOpen className="mx-auto size-5" strokeWidth={1.5} aria-hidden /> : <PanelLeftClose className="size-5" strokeWidth={1.5} aria-hidden />}
          {collapsed ? null : "Recolher"}
        </button>
      </div>
    </aside>
  );
}
