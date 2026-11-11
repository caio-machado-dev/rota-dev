"use client";

import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";

import { navItems } from "@/config/navigation";

import { AuthGate } from "./auth-gate";
import { CommandPalette } from "./command-palette";
import { MobileNav } from "./mobile-nav";
import { Sidebar } from "./sidebar";
import { useGlobalShortcuts } from "./shortcuts";
import { Topbar } from "./topbar";

/**
 * Estrutura das telas internas: barra lateral fixa e compactável (desktop), topbar, navegação
 * inferior (celular), paleta de comandos e atalhos de teclado. Largura útil de até 1320px.
 */
export function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [paletteOpen, setPaletteOpen] = useState(false);

  const togglePalette = useCallback(() => setPaletteOpen((open) => !open), []);
  const onSequence = useCallback(
    (second: string) => {
      const target = navItems.find((item) => item.shortcut === second);
      if (target) router.push(target.href);
    },
    [router],
  );
  useGlobalShortcuts({ onTogglePalette: togglePalette, onSequence });

  return (
    <div className="flex min-h-dvh">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onOpenPalette={() => setPaletteOpen(true)} />
        <main className="mx-auto w-full max-w-[1320px] flex-1 px-4 pb-24 pt-6 md:px-8 md:pb-10 md:pt-8">
          <AuthGate>{children}</AuthGate>
        </main>
      </div>
      <MobileNav />
      <CommandPalette open={paletteOpen} onOpenChange={setPaletteOpen} />
    </div>
  );
}
