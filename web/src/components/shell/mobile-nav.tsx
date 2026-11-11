"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { isActive, navItems } from "@/config/navigation";
import { cn } from "@/lib/utils";

/** Navegação inferior do celular: quatro abas, com área de toque de 48px e respeito à safe area. */
export function MobileNav() {
  const pathname = usePathname();
  return (
    <nav
      aria-label="Navegação principal"
      className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-4 border-t border-border bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
    >
      {navItems.map(({ href, label, icon: Icon }) => {
        const active = isActive(pathname, href);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex min-h-14 flex-col items-center justify-center gap-1 text-micro text-muted-foreground outline-none focus-visible:ring-2 focus-visible:ring-ring",
              active && "text-brand",
            )}
          >
            <Icon className="size-5" strokeWidth={1.5} aria-hidden />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
