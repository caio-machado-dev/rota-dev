"use client";

import { Plus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";

import { SessionProvider, useSessionUi } from "@/components/sessions/session-context";
import { SessionSheet } from "@/components/sessions/session-sheet";
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
  return (
    <SessionProvider>
      <ShellFrame>{children}</ShellFrame>
    </SessionProvider>
  );
}

function ShellFrame({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { openSession } = useSessionUi();
  const [paletteOpen, setPaletteOpen] = useState(false);

  const togglePalette = useCallback(() => setPaletteOpen((open) => !open), []);
  const onSequence = useCallback(
    (second: string) => {
      const target = navItems.find((item) => item.shortcut === second);
      if (target) router.push(target.href);
    },
    [router],
  );
  const onKey = useCallback((key: string) => key === "n" && openSession(), [openSession]);
  useGlobalShortcuts({ onTogglePalette: togglePalette, onSequence, onKey });

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
      <button
        type="button"
        onClick={() => openSession()}
        aria-label="Registrar sessão"
        className="fixed right-4 bottom-[calc(4.5rem+env(safe-area-inset-bottom))] z-40 grid size-12 place-items-center rounded-full bg-primary text-primary-foreground shadow-raised outline-none transition-transform duration-150 active:scale-95 focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background md:hidden"
      >
        <Plus className="size-6" strokeWidth={1.5} aria-hidden />
      </button>
      <SessionSheet />
      <CommandPalette open={paletteOpen} onOpenChange={setPaletteOpen} onRegisterSession={() => openSession()} />
    </div>
  );
}
