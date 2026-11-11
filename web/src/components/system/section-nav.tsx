"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { cn } from "@/lib/utils";

export type SectionLink = { href: string; label: string };

/** Navegação entre as subseções de uma página (Plano, Progresso, Perfil). É navegação de rota, não abas de painel. */
export function SectionNav({ links, label }: { links: SectionLink[]; label: string }) {
  const pathname = usePathname();
  return (
    <nav aria-label={label} className="-mt-2 mb-8 flex gap-1 overflow-x-auto border-b border-border">
      {links.map(({ href, label: text }, index) => {
        const active = index === 0 ? pathname === href : pathname === href || pathname.startsWith(`${href}/`);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "-mb-px shrink-0 border-b-2 border-transparent px-3 py-2.5 text-small font-medium text-muted-foreground transition-colors duration-150 outline-none hover:text-foreground focus-visible:ring-2 focus-visible:ring-ring",
              active && "border-brand text-foreground",
            )}
          >
            {text}
          </Link>
        );
      })}
    </nav>
  );
}
