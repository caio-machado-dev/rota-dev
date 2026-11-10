"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { brand } from "@/config/brand";
import { isActive, navItems } from "@/config/navigation";
import { cn } from "@/lib/utils";

import { UserMenu } from "./user-menu";

/**
 * Estrutura de navegação: barra lateral fixa a partir de `md` (desktop) e barra inferior
 * no celular. É um Client Component porque precisa da rota atual (`usePathname`).
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex min-h-dvh">
      <aside className="hidden w-60 shrink-0 flex-col border-r border-border bg-sidebar px-3 py-6 md:flex">
        <Link href="/" className="mb-8 px-3 font-mono text-sm font-semibold tracking-tight text-brand">
          {brand.name}
        </Link>
        <nav aria-label="Navegação principal" className="flex flex-col gap-1">
          {navItems.map(({ href, label, icon: Icon }) => {
            const active = isActive(pathname, href);
            return (
              <Link
                key={href}
                href={href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex items-center gap-3 rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-sidebar-accent hover:text-foreground",
                  active && "bg-sidebar-accent text-foreground",
                )}
              >
                <Icon className={cn("size-4", active && "text-brand")} aria-hidden />
                {label}
              </Link>
            );
          })}
        </nav>
        <div className="mt-auto border-t border-border pt-4">
          <UserMenu />
        </div>
      </aside>

      <main className="flex-1 px-5 pb-24 pt-8 md:px-10 md:pb-10">
        {/* No celular não há barra lateral: o menu do usuário fica no topo. */}
        <div className="mb-6 flex justify-end md:hidden">
          <UserMenu />
        </div>
        {children}
      </main>

      <nav
        aria-label="Navegação principal"
        className="fixed inset-x-0 bottom-0 grid grid-cols-4 border-t border-border bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
      >
        {navItems.map(({ href, label, icon: Icon }) => {
          const active = isActive(pathname, href);
          return (
            <Link
              key={href}
              href={href}
              aria-current={active ? "page" : undefined}
              className={cn(
                "flex flex-col items-center gap-1 py-2.5 text-[11px] text-muted-foreground",
                active && "text-brand",
              )}
            >
              <Icon className="size-5" aria-hidden />
              {label}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
